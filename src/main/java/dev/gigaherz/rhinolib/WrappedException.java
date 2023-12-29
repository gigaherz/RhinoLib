/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib;

import java.io.Serial;

/**
 * A wrapper for runtime exceptions.
 * <p>
 * Used by the JavaScript runtime to wrap and propagate exceptions that occur
 * during runtime.
 *
 * @author Norris Boyd
 */
public class WrappedException extends EvaluatorException {
	@Serial
	private static final long serialVersionUID = -1551979216966520648L;
	private final Throwable exception;

	/**
	 * @see Context#throwAsScriptRuntimeEx(Throwable, Context)
	 */
	public WrappedException(Context cx, Throwable exception) {
		super(cx, "Wrapped " + exception);
		this.exception = exception;
		this.initCause(exception);

		int[] linep = {0};
		String sourceName = Context.getSourcePositionFromStack(cx, linep);
		int lineNumber = linep[0];
		if (sourceName != null) {
			initSourceName(sourceName);
		}
		if (lineNumber != 0) {
			initLineNumber(lineNumber);
		}
	}

	/**
	 * Get the wrapped exception.
	 *
	 * @return the exception that was presented as a argument to the
	 * constructor when this object was created
	 */
	public Throwable getWrappedException() {
		return exception;
	}
}
