/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib.ast;

import dev.gigaherz.rhinolib.Token;

/**
 * Return statement.  Node type is {@link Token#RETURN}.
 *
 * <pre><i>ReturnStatement</i> :
 *      <b>return</b> [<i>no LineTerminator here</i>] [Expression] ;</pre>
 */
public class ReturnStatement extends AstNode {

	private AstNode returnValue;

	{
		type = Token.RETURN;
	}

	public ReturnStatement() {
	}

	public ReturnStatement(int pos) {
		super(pos);
	}

	public ReturnStatement(int pos, int len) {
		super(pos, len);
	}

	public ReturnStatement(int pos, int len, AstNode returnValue) {
		super(pos, len);
		setReturnValue(returnValue);
	}

	/**
	 * Returns return value, {@code null} if return value is void
	 */
	public AstNode getReturnValue() {
		return returnValue;
	}

	/**
	 * Sets return value expression, and sets its parent to this node.
	 * Can be {@code null}.
	 */
	public void setReturnValue(AstNode returnValue) {
		this.returnValue = returnValue;
		if (returnValue != null) {
			returnValue.setParent(this);
		}
	}
}
