/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib;

public class CompilerEnvirons {
	private ErrorReporter errorReporter;

	public CompilerEnvirons() {
		errorReporter = DefaultErrorReporter.instance;
	}

	public void initFromContext(Context cx) {
		setErrorReporter(cx.getErrorReporter());
	}

	public final ErrorReporter getErrorReporter() {
		return errorReporter;
	}

	public void setErrorReporter(ErrorReporter errorReporter) {
		if (errorReporter == null) {
			throw new IllegalArgumentException();
		}
		this.errorReporter = errorReporter;
	}

	public final boolean isStrictMode() {
		return false;
	}
}
