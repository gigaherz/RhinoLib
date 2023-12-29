/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.gigaherz.rhinolib;

import java.util.Objects;

public class IdFunctionObject extends BaseFunction {
	static boolean equalObjectGraphs(Context cx, IdFunctionObject f1, IdFunctionObject f2, EqualObjectGraphs eq) {
		return f1.methodId == f2.methodId && f1.hasTag(f2.tag) && eq.equalGraphs(cx, f1.idcall, f2.idcall);
	}

	private final IdFunctionCall idcall;
	private final Object tag;
	private final int methodId;
	private final int arity;
	private boolean useCallAsConstructor;
	private String functionName;

	public IdFunctionObject(IdFunctionCall idcall, Object tag, int id, int arity) {
		if (arity < 0) {
			throw new IllegalArgumentException();
		}

		this.idcall = idcall;
		this.tag = tag;
		this.methodId = id;
		this.arity = arity;
	}

	public IdFunctionObject(IdFunctionCall idcall, Object tag, int id, String name, int arity, Scriptable scope) {
		super(scope, null);

		if (arity < 0) {
			throw new IllegalArgumentException();
		}
		if (name == null) {
			throw new IllegalArgumentException();
		}

		this.idcall = idcall;
		this.tag = tag;
		this.methodId = id;
		this.arity = arity;
		this.functionName = name;
	}

	public void initFunction(String name, Scriptable scope) {
		if (name == null) {
			throw new IllegalArgumentException();
		}
		if (scope == null) {
			throw new IllegalArgumentException();
		}
		this.functionName = name;
		setParentScope(scope);
	}

	public final boolean hasTag(Object tag) {
		return Objects.equals(tag, this.tag);
	}

	public Object getTag() {
		return tag;
	}

	public final int methodId() {
		return methodId;
	}

	public final void markAsConstructor(Scriptable prototypeProperty) {
		useCallAsConstructor = true;
		setImmunePrototypeProperty(prototypeProperty);
	}

	public final void addAsProperty(Scriptable target, Context cx) {
		defineProperty(target, functionName, this, DONTENUM, cx);
	}

	public void exportAsScopeProperty(Context cx) {
		addAsProperty(getParentScope(), cx);
	}

	@Override
	public Scriptable getPrototype(Context cx) {
		// Lazy initialization of prototype: for native functions this
		// may not be called at all
		Scriptable proto = super.getPrototype(cx);
		if (proto == null) {
			proto = getFunctionPrototype(getParentScope(), cx);
			setPrototype(proto);
		}
		return proto;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		return idcall.execIdCall(this, cx, scope, thisObj, args);
	}

	@Override
	public Scriptable createObject(Context cx, Scriptable scope) {
		if (useCallAsConstructor) {
			return null;
		}
		// Throw error if not explicitly coded to be used as constructor,
		// to satisfy ECMAScript standard (see bugzilla 202019).
		// To follow current (2003-05-01) SpiderMonkey behavior, change it to:
		// return super.createObject(cx, scope);
		throw ScriptRuntime.typeError1(cx, "msg.not.ctor", functionName);
	}

	@Override
	public int getArity() {
		return arity;
	}

	@Override
	public int getLength() {
		return getArity();
	}

	@Override
	public String getFunctionName() {
		return (functionName == null) ? "" : functionName;
	}

	public final RuntimeException unknown() {
		// It is program error to call id-like methods for unknown function
		return new IllegalArgumentException("BAD FUNCTION ID=" + methodId + " MASTER=" + idcall);
	}
}
