package dev.gigaherz.rhinolib.util;

import dev.gigaherz.rhinolib.Context;
import dev.gigaherz.rhinolib.Scriptable;

@FunctionalInterface
public interface ValueUnwrapper {
	ValueUnwrapper DEFAULT = (cx, scope, value) -> cx.getWrapFactory().wrap(cx, scope, value, value.getClass());

	Object unwrap(Context cx, Scriptable scope, Object value);
}
