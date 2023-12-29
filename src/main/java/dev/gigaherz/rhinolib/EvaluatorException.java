/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */


package dev.gigaherz.rhinolib;

import java.io.Serial;

/**
 * The class of exceptions thrown by the JavaScript engine.
 */
public class EvaluatorException extends RhinoException {
	@Serial
	private static final long serialVersionUID = -8743165779676009808L;

	public EvaluatorException(Context cx, String detail) {
		super(cx, detail);
	}

	/**
	 * Create an exception with the specified detail message.
	 * <p>
	 * Errors internal to the JavaScript engine will simply throw a
	 * RuntimeException.
	 *
	 * @param detail     the error message
	 * @param sourceName the name of the source reponsible for the error
	 * @param lineNumber the line number of the source
	 */
	public EvaluatorException(Context cx, String detail, String sourceName, int lineNumber) {
		this(cx, detail, sourceName, lineNumber, null, 0);
	}

	/**
	 * Create an exception with the specified detail message.
	 * <p>
	 * Errors internal to the JavaScript engine will simply throw a
	 * RuntimeException.
	 *
	 * @param detail       the error message
	 * @param sourceName   the name of the source responsible for the error
	 * @param lineNumber   the line number of the source
	 * @param columnNumber the columnNumber of the source (may be zero if
	 *                     unknown)
	 * @param lineSource   the source of the line containing the error (may be
	 *                     null if unknown)
	 */
	public EvaluatorException(Context cx, String detail, String sourceName, int lineNumber, String lineSource, int columnNumber) {
		super(cx, detail);
		recordErrorOrigin(sourceName, lineNumber, lineSource, columnNumber);
	}
}
