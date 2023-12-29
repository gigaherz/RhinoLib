package dev.gigaherz.rhinolib.util.wrap;

import dev.gigaherz.rhinolib.Context;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface TypeWrapperFactory<T> {
	interface Simple<T> extends TypeWrapperFactory<T> {
		T wrapSimple(Object o);

		@Override
		default T wrap(Context cx, Object o) {
			return wrapSimple(o);
		}
	}

	T wrap(Context cx, Object o);
}
