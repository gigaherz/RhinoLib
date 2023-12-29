/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib.ast;

import dev.gigaherz.rhinolib.Token;

/**
 * Do statement.  Node type is {@link Token#DO}.
 *
 * <pre><i>DoLoop</i>:
 * <b>do</b> Statement <b>while</b> <b>(</b> Expression <b>)</b> <b>;</b></pre>
 */
public class DoLoop extends Loop {

	private AstNode condition;
	private int whilePosition = -1;

	{
		type = Token.DO;
	}

	public DoLoop() {
	}

	public DoLoop(int pos) {
		super(pos);
	}

	public DoLoop(int pos, int len) {
		super(pos, len);
	}

	/**
	 * Returns loop condition
	 */
	public AstNode getCondition() {
		return condition;
	}

	/**
	 * Sets loop condition, and sets its parent to this node.
	 *
	 * @throws IllegalArgumentException if condition is null
	 */
	public void setCondition(AstNode condition) {
		assertNotNull(condition);
		this.condition = condition;
		condition.setParent(this);
	}

	/**
	 * Returns source position of "while" keyword
	 */
	public int getWhilePosition() {
		return whilePosition;
	}

	/**
	 * Sets source position of "while" keyword
	 */
	public void setWhilePosition(int whilePosition) {
		this.whilePosition = whilePosition;
	}
}
