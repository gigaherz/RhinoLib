package dev.gigaherz.rhinolib.util;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface CustomJavaToJsWrapperProvider<T> {
	CustomJavaToJsWrapperProvider<?> NONE = object -> null;

	@Nullable
	CustomJavaToJsWrapper create(T object);
}
