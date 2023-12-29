package dev.gigaherz.rhinolib.util;

import dev.gigaherz.rhinolib.Context;
import dev.gigaherz.rhinolib.Scriptable;

@FunctionalInterface
public interface CustomJavaToJsWrapper {
	Scriptable convertJavaToJs(Context cx, Scriptable scope, Class<?> staticType);
}
