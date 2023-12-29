/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib;

import dev.gigaherz.rhinolib.util.Deletable;
import dev.gigaherz.rhinolib.util.JavaIteratorWrapper;
import dev.gigaherz.rhinolib.util.wrap.TypeWrapperFactory;
import dev.gigaherz.rhinolib.util.wrap.TypeWrappers;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class reflects non-Array Java objects into the JavaScript environment.  It
 * reflect fields directly, and uses NativeJavaMethod objects to reflect (possibly
 * overloaded) methods.<p>
 *
 * @author Mike Shaver
 * @see NativeJavaArray
 * @see NativeJavaPackage
 * @see NativeJavaClass
 */

public class NativeJavaObject implements Scriptable, SymbolScriptable, Wrapper {
	static final byte CONVERSION_TRIVIAL = 1;
	static final byte CONVERSION_NONTRIVIAL = 0;
	static final byte CONVERSION_NONE = 99;
	private static final Object COERCED_INTERFACE_KEY = "Coerced Interface";
	private static final int JSTYPE_UNDEFINED = 0; // undefined type
	private static final int JSTYPE_NULL = 1; // null
	private static final int JSTYPE_BOOLEAN = 2; // boolean
	private static final int JSTYPE_NUMBER = 3; // number
	private static final int JSTYPE_STRING = 4; // string
	private static final int JSTYPE_JAVA_CLASS = 5; // JavaClass
	private static final int JSTYPE_JAVA_OBJECT = 6; // JavaObject
	private static final int JSTYPE_JAVA_ARRAY = 7; // JavaArray
	private static final int JSTYPE_OBJECT = 8; // Scriptable

	/**
	 * Determine whether we can/should convert between the given type and the
	 * desired one.  This should be superceded by a conversion-cost calculation
	 * function, but for now I'll hide behind precedent.
	 */
	public static boolean canConvert(Context cx, Object fromObj, Class<?> to) {
		return getConversionWeight(cx, fromObj, to) < CONVERSION_NONE;
	}

	/**
	 * Derive a ranking based on how "natural" the conversion is.
	 * The special value CONVERSION_NONE means no conversion is possible,
	 * and CONVERSION_NONTRIVIAL signals that more type conformance testing
	 * is required.
	 * Based on
	 * <a href="http://www.mozilla.org/js/liveconnect/lc3_method_overloading.html">
	 * "preferred method conversions" from Live Connect 3</a>
	 */
	static int getConversionWeight(Context cx, Object fromObj, Class<?> to) {
		if (cx.hasTypeWrappers() && cx.getTypeWrappers().getWrapperFactory(to, fromObj) != null) {
			return CONVERSION_NONTRIVIAL;
		}

		int fromCode = getJSTypeCode(fromObj);

		switch (fromCode) {

			case JSTYPE_UNDEFINED:
				if (to == ScriptRuntime.StringClass || to == ScriptRuntime.ObjectClass) {
					return 1;
				}
				break;

			case JSTYPE_NULL:
				if (!to.isPrimitive()) {
					return 1;
				}
				break;

			case JSTYPE_BOOLEAN:
				// "boolean" is #1
				if (to == Boolean.TYPE) {
					return 1;
				} else if (to == ScriptRuntime.BooleanClass) {
					return 2;
				} else if (to == ScriptRuntime.ObjectClass) {
					return 3;
				} else if (to == ScriptRuntime.StringClass) {
					return 4;
				}
				break;

			case JSTYPE_NUMBER:
				if (to.isPrimitive()) {
					if (to == Double.TYPE) {
						return 1;
					} else if (to != Boolean.TYPE) {
						return 1 + getSizeRank(to);
					}
				} else {
					if (to == ScriptRuntime.StringClass) {
						// native numbers are #1-8
						return 9;
					} else if (to == ScriptRuntime.ObjectClass) {
						return 10;
					} else if (ScriptRuntime.NumberClass.isAssignableFrom(to)) {
						// "double" is #1
						return 2;
					}
				}
				break;

			case JSTYPE_STRING:
				if (to == ScriptRuntime.StringClass) {
					return 1;
				} else if (to.isInstance(fromObj)) {
					return 2;
				} else if (to.isPrimitive()) {
					if (to == Character.TYPE) {
						return 3;
					} else if (to != Boolean.TYPE) {
						return 4;
					}
				}
				break;

			case JSTYPE_JAVA_CLASS:
				if (to == ScriptRuntime.ClassClass) {
					return 1;
				} else if (to == ScriptRuntime.ObjectClass) {
					return 3;
				} else if (to == ScriptRuntime.StringClass) {
					return 4;
				}
				break;

			case JSTYPE_JAVA_OBJECT:
			case JSTYPE_JAVA_ARRAY:
				Object javaObj = fromObj;
				if (javaObj instanceof Wrapper) {
					javaObj = ((Wrapper) javaObj).unwrap();
				}
				if (to.isInstance(javaObj)) {
					return CONVERSION_NONTRIVIAL;
				}
				if (to == ScriptRuntime.StringClass) {
					return 2;
				} else if (to.isPrimitive() && to != Boolean.TYPE) {
					return (fromCode == JSTYPE_JAVA_ARRAY) ? CONVERSION_NONE : 2 + getSizeRank(to);
				}
				break;

			case JSTYPE_OBJECT:
				// Other objects takes #1-#3 spots
				if (to != ScriptRuntime.ObjectClass && to.isInstance(fromObj)) {
					// No conversion required, but don't apply for java.lang.Object
					return 1;
				}
				if (to.isArray()) {
					if (fromObj instanceof NativeArray) {
						// This is a native array conversion to a java array
						// Array conversions are all equal, and preferable to object
						// and string conversion, per LC3.
						return 2;
					}
				} else if (to == ScriptRuntime.ObjectClass) {
					return 3;
				} else if (to == ScriptRuntime.StringClass) {
					return 4;
				} else if (to == ScriptRuntime.DateClass) {
					if (fromObj instanceof NativeDate) {
						// This is a native date to java date conversion
						return 1;
					}
				} else if (to.isInterface()) {

					if (fromObj instanceof NativeFunction) {
						// See comments in createInterfaceAdapter
						return 1;
					}
					if (fromObj instanceof NativeObject) {
						return 2;
					}
					return 12;
				} else if (to.isPrimitive() && to != Boolean.TYPE) {
					return 4 + getSizeRank(to);
				}
				break;
		}

		return CONVERSION_NONE;
	}

	static int getSizeRank(Class<?> aType) {
		if (aType == Double.TYPE) {
			return 1;
		} else if (aType == Float.TYPE) {
			return 2;
		} else if (aType == Long.TYPE) {
			return 3;
		} else if (aType == Integer.TYPE) {
			return 4;
		} else if (aType == Short.TYPE) {
			return 5;
		} else if (aType == Character.TYPE) {
			return 6;
		} else if (aType == Byte.TYPE) {
			return 7;
		} else if (aType == Boolean.TYPE) {
			return CONVERSION_NONE;
		} else {
			return 8;
		}
	}

	private static int getJSTypeCode(Object value) {
		if (value == null) {
			return JSTYPE_NULL;
		} else if (value == Undefined.instance) {
			return JSTYPE_UNDEFINED;
		} else if (value instanceof CharSequence) {
			return JSTYPE_STRING;
		} else if (value instanceof Number) {
			return JSTYPE_NUMBER;
		} else if (value instanceof Boolean) {
			return JSTYPE_BOOLEAN;
		} else if (value instanceof Scriptable) {
			if (value instanceof NativeJavaClass) {
				return JSTYPE_JAVA_CLASS;
			} else if (value instanceof NativeJavaArray) {
				return JSTYPE_JAVA_ARRAY;
			} else if (value instanceof Wrapper) {
				return JSTYPE_JAVA_OBJECT;
			} else {
				return JSTYPE_OBJECT;
			}
		} else if (value instanceof Class) {
			return JSTYPE_JAVA_CLASS;
		} else {
			Class<?> valueClass = value.getClass();
			if (valueClass.isArray()) {
				return JSTYPE_JAVA_ARRAY;
			}
			return JSTYPE_JAVA_OBJECT;
		}
	}

	/**
	 * Type-munging for field setting and method invocation.
	 * Conforms to LC3 specification
	 */
	static Object coerceTypeImpl(@Nullable TypeWrappers typeWrappers, Class<?> type, Object value, Context cx) {
		if (value == null || value.getClass() == type) {
			return value;
		}

		Object unwrappedValue = Wrapper.unwrapped(value);
		TypeWrapperFactory<?> typeWrapper = typeWrappers == null ? null : typeWrappers.getWrapperFactory(type, unwrappedValue);

		if (typeWrapper != null) {
			return typeWrapper.wrap(cx, unwrappedValue);
		}

		switch (getJSTypeCode(value)) {
			case JSTYPE_NULL -> {
				// raise error if type.isPrimitive()
				if (type.isPrimitive()) {
					return reportConversionError(value, type, cx);
				}
				return null;
			}
			case JSTYPE_UNDEFINED -> {
				if (type == ScriptRuntime.StringClass || type == ScriptRuntime.ObjectClass) {
					return "undefined";
				}
				return reportConversionError("undefined", type, value, cx);
			}
			case JSTYPE_BOOLEAN -> {
				// Under LC3, only JS Booleans can be coerced into a Boolean value
				if (type == Boolean.TYPE || type == ScriptRuntime.BooleanClass || type == ScriptRuntime.ObjectClass) {
					return value;
				} else if (type == ScriptRuntime.StringClass) {
					return value.toString();
				} else {
					return reportConversionError(value, type, cx);
				}
			}
			case JSTYPE_NUMBER -> {
				if (type == ScriptRuntime.StringClass) {
					return ScriptRuntime.toString(cx, value);
				} else if (type == ScriptRuntime.ObjectClass) {
					/*
					if (cx.hasFeature(Context.FEATURE_INTEGER_WITHOUT_DECIMAL_PLACE)) {
						//to process numbers like 2.0 as 2 without decimal place
						long roundedValue = Math.round(toDouble(value));
						if (roundedValue == toDouble(value)) {
							return coerceToNumber(Long.TYPE, value);
						}
					}
					 */
					return coerceToNumber(Double.TYPE, value, cx);
				} else if ((type.isPrimitive() && type != Boolean.TYPE) || ScriptRuntime.NumberClass.isAssignableFrom(type)) {
					return coerceToNumber(type, value, cx);
				} else {
					return reportConversionError(value, type, cx);
				}
			}
			case JSTYPE_STRING -> {
				if (type == ScriptRuntime.StringClass || type.isInstance(value)) {
					return value.toString();
				} else if (type == Character.TYPE || type == ScriptRuntime.CharacterClass) {
					// Special case for converting a single char string to a
					// character
					// Placed here because it applies *only* to JS strings,
					// not other JS objects converted to strings
					if (((CharSequence) value).length() == 1) {
						return ((CharSequence) value).charAt(0);
					}
					return coerceToNumber(type, value, cx);
				} else if ((type.isPrimitive() && type != Boolean.TYPE) || ScriptRuntime.NumberClass.isAssignableFrom(type)) {
					return coerceToNumber(type, value, cx);
				} else {
					return reportConversionError(value, type, cx);
				}
			}
			case JSTYPE_JAVA_CLASS -> {
				if (type == ScriptRuntime.ClassClass || type == ScriptRuntime.ObjectClass) {
					return unwrappedValue;
				} else if (type == ScriptRuntime.StringClass) {
					return unwrappedValue.toString();
				} else {
					return reportConversionError(unwrappedValue, type, cx);
				}
			}
			case JSTYPE_JAVA_OBJECT, JSTYPE_JAVA_ARRAY -> {
				if (type.isPrimitive()) {
					if (type == Boolean.TYPE) {
						return reportConversionError(unwrappedValue, type, cx);
					}
					return coerceToNumber(type, unwrappedValue, cx);
				}
				if (type == ScriptRuntime.StringClass) {
					return unwrappedValue.toString();
				}
				if (type.isInstance(unwrappedValue)) {
					return unwrappedValue;
				}
				return reportConversionError(unwrappedValue, type, cx);
			}
			case JSTYPE_OBJECT -> {
				if (type == ScriptRuntime.StringClass) {
					return ScriptRuntime.toString(cx, value);
				} else if (type.isPrimitive()) {
					if (type == Boolean.TYPE) {
						return reportConversionError(value, type, cx);
					}
					return coerceToNumber(type, value, cx);
				} else if (type.isInstance(value)) {
					return value;
				} else if (type == ScriptRuntime.DateClass && value instanceof NativeDate) {
					double time = ((NativeDate) value).getJSTimeValue();
					// XXX: This will replace NaN by 0
					return new Date((long) time);
				} else if (type.isArray() && value instanceof NativeArray array) {
					// Make a new java array, and coerce the JS array components
					// to the target (component) type.
					long length = array.getLength();
					Class<?> arrayType = type.getComponentType();
					Object Result = Array.newInstance(arrayType, (int) length);
					for (int i = 0; i < length; ++i) {
						try {
							Array.set(Result, i, coerceTypeImpl(typeWrappers, arrayType, array.get(cx, i, array), cx));
						} catch (EvaluatorException ee) {
							return reportConversionError(value, type, cx);
						}
					}

					return Result;
				} else if (value instanceof Wrapper) {
					if (type.isInstance(unwrappedValue)) {
						return unwrappedValue;
					}
					return reportConversionError(unwrappedValue, type, cx);
				} else if (type.isInterface() && (value instanceof NativeObject || value instanceof NativeFunction || value instanceof ArrowFunction)) {
					// Try to use function/object as implementation of Java interface.
					return createInterfaceAdapter(cx, type, (ScriptableObject) value);
				} else {
					return reportConversionError(value, type, cx);
				}
			}
		}


		return value;
	}

	public static Object createInterfaceAdapter(Context cx, Class<?> type, ScriptableObject so) {
		// XXX: Currently only instances of ScriptableObject are
		// supported since the resulting interface proxies should
		// be reused next time conversion is made and generic
		// Callable has no storage for it. Weak references can
		// address it but for now use this restriction.

		Object key = Kit.makeHashKeyFromPair(COERCED_INTERFACE_KEY, type);
		Object old = so.getAssociatedValue(key);
		if (old != null) {
			// Function was already wrapped
			return old;
		}
		Object glue = InterfaceAdapter.create(cx, type, so);
		// Store for later retrieval
		glue = so.associateValue(key, glue);
		return glue;
	}

	private static Object coerceToNumber(Class<?> type, Object value, Context cx) {
		Class<?> valueClass = value.getClass();

		// Character
		if (type == Character.TYPE || type == ScriptRuntime.CharacterClass) {
			if (valueClass == ScriptRuntime.CharacterClass) {
				return value;
			}
			return (char) toInteger(value, ScriptRuntime.CharacterClass, Character.MIN_VALUE, Character.MAX_VALUE, cx);
		}

		// Double, Float
		if (type == ScriptRuntime.ObjectClass || type == ScriptRuntime.DoubleClass || type == Double.TYPE) {
			return valueClass == ScriptRuntime.DoubleClass ? value : Double.valueOf(toDouble(value, cx));
		}

		if (type == ScriptRuntime.FloatClass || type == Float.TYPE) {
			if (valueClass == ScriptRuntime.FloatClass) {
				return value;
			}
			double number = toDouble(value, cx);
			if (Double.isInfinite(number) || Double.isNaN(number) || number == 0.0) {
				return (float) number;
			}

			double absNumber = Math.abs(number);
			if (absNumber < Float.MIN_VALUE) {
				return (number > 0.0) ? +0.0f : -0.0f;
			} else if (absNumber > Float.MAX_VALUE) {
				return (number > 0.0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
			} else {
				return (float) number;
			}
		}

		// Integer, Long, Short, Byte
		if (type == ScriptRuntime.IntegerClass || type == Integer.TYPE) {
			if (valueClass == ScriptRuntime.IntegerClass) {
				return value;
			}
			return (int) toInteger(value, ScriptRuntime.IntegerClass, Integer.MIN_VALUE, Integer.MAX_VALUE, cx);
		}

		if (type == ScriptRuntime.LongClass || type == Long.TYPE) {
			if (valueClass == ScriptRuntime.LongClass) {
				return value;
			}
			/* Long values cannot be expressed exactly in doubles.
			 * We thus use the largest and smallest double value that
			 * has a value expressible as a long value. We build these
			 * numerical values from their hexidecimal representations
			 * to avoid any problems caused by attempting to parse a
			 * decimal representation.
			 */
			final double max = Double.longBitsToDouble(0x43dfffffffffffffL);
			final double min = Double.longBitsToDouble(0xc3e0000000000000L);
			return toInteger(value, ScriptRuntime.LongClass, min, max, cx);
		}

		if (type == ScriptRuntime.ShortClass || type == Short.TYPE) {
			if (valueClass == ScriptRuntime.ShortClass) {
				return value;
			}
			return (short) toInteger(value, ScriptRuntime.ShortClass, Short.MIN_VALUE, Short.MAX_VALUE, cx);
		}

		if (type == ScriptRuntime.ByteClass || type == Byte.TYPE) {
			if (valueClass == ScriptRuntime.ByteClass) {
				return value;
			}
			return (byte) toInteger(value, ScriptRuntime.ByteClass, Byte.MIN_VALUE, Byte.MAX_VALUE, cx);
		}

		return toDouble(value, cx);
	}

	private static double toDouble(Object value, Context cx) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else if (value instanceof String) {
			return ScriptRuntime.toNumber(cx, (String) value);
		} else if (value instanceof Scriptable) {
			if (value instanceof Wrapper) {
				// XXX: optimize tail-recursion?
				return toDouble(((Wrapper) value).unwrap(), cx);
			}
			return ScriptRuntime.toNumber(cx, value);
		} else {
			Method meth;
			try {
				meth = value.getClass().getMethod("doubleValue", (Class[]) null);
			} catch (NoSuchMethodException e) {
				meth = null;
			} catch (SecurityException e) {
				meth = null;
			}
			if (meth != null) {
				try {
					return ((Number) meth.invoke(value, (Object[]) null)).doubleValue();
				} catch (IllegalAccessException e) {
					// XXX: ignore, or error message?
					reportConversionError(value, Double.TYPE, cx);
				} catch (InvocationTargetException e) {
					// XXX: ignore, or error message?
					reportConversionError(value, Double.TYPE, cx);
				}
			}
			return ScriptRuntime.toNumber(cx, value.toString());
		}
	}

	private static long toInteger(Object value, Class<?> type, double min, double max, Context cx) {
		double d = toDouble(value, cx);

		if (Double.isInfinite(d) || Double.isNaN(d)) {
			// Convert to string first, for more readable message
			reportConversionError(ScriptRuntime.toString(cx, value), type, cx);
		}

		if (d > 0.0) {
			d = Math.floor(d);
		} else {
			d = Math.ceil(d);
		}

		if (d < min || d > max) {
			// Convert to string first, for more readable message
			reportConversionError(ScriptRuntime.toString(cx, value), type, cx);
		}
		return (long) d;
	}

	static Object reportConversionError(Object value, Class<?> type, Context cx) {
		return reportConversionError(value, type, value, cx);
	}

	static Object reportConversionError(Object value, Class<?> type, Object stringValue, Context cx) {
		// It uses String.valueOf(value), not value.toString() since
		// value can be null, bug 282447.
		throw Context.reportRuntimeError2("msg.conversion.not.allowed", String.valueOf(stringValue), JavaMembers.javaSignature(type), cx);
	}

	/**
	 * The prototype of this object.
	 */
	protected Scriptable prototype;
	/**
	 * The parent scope of this object.
	 */
	protected Scriptable parent;
	protected transient Object javaObject;
	protected transient Class<?> staticType;
	protected transient JavaMembers members;
	protected transient Map<String, FieldAndMethods> fieldAndMethods;
	protected transient Map<String, Object> customMembers;
	protected transient boolean isAdapter;

	public NativeJavaObject() {
	}

	public NativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType, Context cx) {
		this(scope, javaObject, staticType, false, cx);
	}

	public NativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType, boolean isAdapter, Context cx) {
		this.parent = scope;
		this.javaObject = javaObject;
		this.staticType = staticType;
		this.isAdapter = isAdapter;
		initMembers(cx, scope);
	}

	protected void initMembers(Context cx, Scriptable scope) {
		Class<?> dynamicType;
		if (javaObject != null) {
			dynamicType = javaObject.getClass();
		} else {
			dynamicType = staticType;
		}
		members = JavaMembers.lookupClass(cx, scope, dynamicType, staticType, isAdapter);
		fieldAndMethods = members.getFieldAndMethodsObjects(this, javaObject, false, cx);
		customMembers = null;
	}

	protected void addCustomMember(String name, Object fm) {
		if (customMembers == null) {
			customMembers = new HashMap<>();
		}

		customMembers.put(name, fm);
	}

	protected void addCustomFunction(String name, CustomFunction.Func func, Class<?>... argTypes) {
		addCustomMember(name, new CustomFunction(name, func, argTypes));
	}

	protected void addCustomFunction(String name, CustomFunction.NoArgFunc func) {
		addCustomFunction(name, func, CustomFunction.NO_ARGS);
	}

	public void addCustomProperty(String name, CustomProperty getter) {
		addCustomMember(name, getter);
	}

	@Override
	public boolean has(Context cx, String name, Scriptable start) {
		return members.has(name, false) || customMembers != null && customMembers.containsKey(name);
	}

	@Override
	public boolean has(Context cx, int index, Scriptable start) {
		return false;
	}

	@Override
	public boolean has(Context cx, Symbol key, Scriptable start) {
		return javaObject instanceof Iterable<?> && SymbolKey.ITERATOR.equals(key);
	}

	@Override
	public Object get(Context cx, String name, Scriptable start) {
		if (fieldAndMethods != null) {
			Object result = fieldAndMethods.get(name);
			if (result != null) {
				return result;
			}
		}

		if (customMembers != null) {
			Object result = customMembers.get(name);

			if (result != null) {
				if (result instanceof CustomProperty) {
					Object r = ((CustomProperty) result).get(cx);

					if (r == null) {
						return Undefined.instance;
					}

					Object r1 = cx.getWrapFactory().wrap(cx, this, r, r.getClass());

					if (r1 instanceof Scriptable) {
						return ((Scriptable) r1).getDefaultValue(cx, null);
					}

					return r1;
				}

				return result;
			}
		}

		// TODO: passing 'this' as the scope is bogus since it has
		//  no parent scope
		return members.get(this, name, javaObject, false, cx);
	}

	@Override
	public Object get(Context cx, Symbol key, Scriptable start) {
		if (javaObject instanceof Iterable<?> itr && SymbolKey.ITERATOR.equals(key)) {
			return new JavaIteratorWrapper(itr.iterator());
		}

		// Native Java objects have no Symbol members
		return NOT_FOUND;
	}

	@Override
	public Object get(Context cx, int index, Scriptable start) {
		throw members.reportMemberNotFound(Integer.toString(index), cx);
	}

	@Override
	public void put(Context cx, String name, Scriptable start, Object value) {
		// We could be asked to modify the value of a property in the
		// prototype. Since we can't add a property to a Java object,
		// we modify it in the prototype rather than copy it down.
		if (prototype == null || members.has(name, false)) {
			members.put(this, name, javaObject, value, false, cx);
		} else {
			prototype.put(cx, name, prototype, value);
		}
	}

	@Override
	public void put(Context cx, Symbol symbol, Scriptable start, Object value) {
		// We could be asked to modify the value of a property in the
		// prototype. Since we can't add a property to a Java object,
		// we modify it in the prototype rather than copy it down.
		String name = symbol.toString();
		if (prototype == null || members.has(name, false)) {
			members.put(this, name, javaObject, value, false, cx);
		} else if (prototype instanceof SymbolScriptable) {
			((SymbolScriptable) prototype).put(cx, symbol, prototype, value);
		}
	}

	@Override
	public void put(Context cx, int index, Scriptable start, Object value) {
		throw members.reportMemberNotFound(Integer.toString(index), cx);
	}

	@Override
	public boolean hasInstance(Context cx, Scriptable value) {
		// This is an instance of a Java class, so always return false
		return false;
	}

	@Override
	public void delete(Context cx, String name) {
		if (fieldAndMethods != null) {
			Object result = fieldAndMethods.get(name);
			if (result != null) {
				Deletable.deleteObject(result);
				return;
			}
		}

		if (customMembers != null) {
			Object result = customMembers.get(name);
			if (result != null) {
				Deletable.deleteObject(result);
				return;
			}
		}

		Deletable.deleteObject(members.get(this, name, javaObject, false, cx));
	}

	@Override
	public void delete(Context cx, Symbol key) {
	}

	@Override
	public void delete(Context cx, int index) {
	}

	@Override
	public Scriptable getPrototype(Context cx) {
		if (prototype == null && javaObject instanceof String) {
			return TopLevel.getBuiltinPrototype(ScriptableObject.getTopLevelScope(parent), TopLevel.Builtins.String, cx);
		}
		return prototype;
	}

	/**
	 * Sets the prototype of the object.
	 */
	@Override
	public void setPrototype(Scriptable m) {
		prototype = m;
	}

	/**
	 * Returns the parent (enclosing) scope of the object.
	 */
	@Override
	public Scriptable getParentScope() {
		return parent;
	}

	/**
	 * Sets the parent (enclosing) scope of the object.
	 */
	@Override
	public void setParentScope(Scriptable m) {
		parent = m;
	}

	@Override
	public Object[] getIds(Context cx) {
		if (customMembers != null) {
			Object[] c = customMembers.keySet().toArray(ScriptRuntime.EMPTY_OBJECTS);
			Object[] m = members.getIds(false);
			Object[] result = new Object[c.length + m.length];
			System.arraycopy(c, 0, result, 0, c.length);
			System.arraycopy(m, 0, result, c.length, m.length);
			return result;
		}

		return members.getIds(false);
	}

	@Override
	public Object unwrap() {
		return javaObject;
	}

	@Override
	public String getClassName() {
		return "JavaObject";
	}

	@Override
	public Object getDefaultValue(Context cx, Class<?> hint) {
		Object value;
		if (hint == null) {
			if (javaObject instanceof Boolean) {
				hint = ScriptRuntime.BooleanClass;
			}
			if (javaObject instanceof Number) {
				hint = ScriptRuntime.NumberClass;
			}
		}
		if (hint == null || hint == ScriptRuntime.StringClass) {
			value = javaObject.toString();
		} else {
			String converterName;
			if (hint == ScriptRuntime.BooleanClass) {
				converterName = "booleanValue";
			} else if (hint == ScriptRuntime.NumberClass) {
				converterName = "doubleValue";
			} else {
				throw Context.reportRuntimeError0("msg.default.value", cx);
			}
			Object converterObject = get(cx, converterName, this);
			if (converterObject instanceof Function f) {
				value = f.call(cx, f.getParentScope(), this, ScriptRuntime.EMPTY_OBJECTS);
			} else {
				if (hint == ScriptRuntime.NumberClass && javaObject instanceof Boolean) {
					boolean b = (Boolean) javaObject;
					value = b ? ScriptRuntime.wrapNumber(1.0) : ScriptRuntime.zeroObj;
				} else {
					value = javaObject.toString();
				}
			}
		}
		return value;
	}
}
