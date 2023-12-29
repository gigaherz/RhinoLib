package dev.gigaherz.rhinolib.util;

import dev.gigaherz.rhinolib.Context;
import dev.gigaherz.rhinolib.IdEnumerationIterator;
import dev.gigaherz.rhinolib.JavaScriptException;

import java.util.Iterator;
import java.util.function.Consumer;

public record JavaIteratorWrapper(Iterator<?> parent) implements IdEnumerationIterator
{
	@Override
	public boolean enumerationIteratorHasNext(Context cx, Consumer<Object> callback) {
		if (parent.hasNext()) {
			callback.accept(parent.next());
			return true;
		}

		return false;
	}

	@Override
	public boolean enumerationIteratorNext(Context cx, Consumer<Object> callback) throws JavaScriptException
    {
		if (parent.hasNext()) {
			callback.accept(parent.next());
			return true;
		}

		return false;
	}
}
