package dev.gigaherz.rhinolib.mod.util;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface ChangeListener<T> {
	void onChanged(T o);
}