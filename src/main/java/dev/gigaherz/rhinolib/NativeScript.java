/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib;

/**
 * The JavaScript Script object.
 * <p>
 * Note that the C version of the engine uses XDR as the format used
 * by freeze and thaw. Since this depends on the internal format of
 * structures in the C runtime, we cannot duplicate it.
 * <p>
 * Since we cannot replace 'this' as a result of the compile method,
 * will forward requests to execute to the nonnull 'script' field.
 *
 * @author Norris Boyd
 * @since 1.3
 */

class NativeScript extends BaseFunction {
	private static final Object SCRIPT_TAG = "Script";
	private static final int Id_constructor = 1;
	private static final int Id_toString = 2;
	private static final int Id_compile = 3;
	private static final int Id_exec = 4;
	private static final int MAX_PROTOTYPE_ID = 4;

	static void init(Scriptable scope, boolean sealed, Context cx) {
		NativeScript obj = new NativeScript(null);
		obj.exportAsJSClass(MAX_PROTOTYPE_ID, scope, sealed, cx);
	}

	private static NativeScript realThis(Scriptable thisObj, IdFunctionObject f, Context cx) {
		if (!(thisObj instanceof NativeScript)) {
			throw incompatibleCallError(f, cx);
		}
		return (NativeScript) thisObj;
	}

	private static Script compile(Context cx, String source) {
		int[] linep = {0};
		String filename = Context.getSourcePositionFromStack(cx, linep);
		if (filename == null) {
			filename = "<Script object>";
			linep[0] = 1;
		}
		ErrorReporter reporter;
		reporter = DefaultErrorReporter.forEval(cx.getErrorReporter());
		return cx.compileString(source, null, reporter, filename, linep[0], null);
	}

	private Script script;

	private NativeScript(Script script) {
		this.script = script;
	}

	/**
	 * Returns the name of this JavaScript class, "Script".
	 */
	@Override
	public String getClassName() {
		return "Script";
	}

	// #string_id_map#

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (script != null) {
			return script.exec(cx, scope);
		}
		return Undefined.instance;
	}

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		throw Context.reportRuntimeError0("msg.script.is.not.constructor", cx);
	}

	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public int getArity() {
		return 0;
	}

	@Override
	protected void initPrototypeId(int id, Context cx) {
		String s;
		int arity;
		switch (id) {
			case Id_constructor -> {
				arity = 1;
				s = "constructor";
			}
			case Id_toString -> {
				arity = 0;
				s = "toString";
			}
			case Id_exec -> {
				arity = 0;
				s = "exec";
			}
			case Id_compile -> {
				arity = 1;
				s = "compile";
			}
			default -> throw new IllegalArgumentException(String.valueOf(id));
		}
		initPrototypeMethod(SCRIPT_TAG, id, s, arity, cx);
	}

	@Override
	public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (!f.hasTag(SCRIPT_TAG)) {
			return super.execIdCall(f, cx, scope, thisObj, args);
		}
		int id = f.methodId();
		switch (id) {
			case Id_constructor -> {
				String source = (args.length == 0) ? "" : ScriptRuntime.toString(cx, args[0]);
				Script script = compile(cx, source);
				NativeScript nscript = new NativeScript(script);
				ScriptRuntime.setObjectProtoAndParent(cx, scope, nscript);
				return nscript;
			}
			case Id_toString -> {
				return "not_supported";
			}
			case Id_exec -> {
				throw Context.reportRuntimeError1("msg.cant.call.indirect", "exec", cx);
			}
			case Id_compile -> {
				NativeScript real = realThis(thisObj, f, cx);
				String source = ScriptRuntime.toString(cx, args, 0);
				real.script = compile(cx, source);
				return real;
			}
		}
		throw new IllegalArgumentException(String.valueOf(id));
	}

	// #/string_id_map#

	@Override
	protected int findPrototypeId(String s) {
		return switch (s) {
			case "constructor" -> Id_constructor;
			case "toString" -> Id_toString;
			case "compile" -> Id_compile;
			case "exec" -> Id_exec;
			default -> 0;
		};
	}
}

