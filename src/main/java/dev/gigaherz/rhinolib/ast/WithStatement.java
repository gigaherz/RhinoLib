/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib.ast;

import dev.gigaherz.rhinolib.Token;

/**
 * With statement.  Node type is {@link Token#WITH}.
 *
 * <pre><i>WithStatement</i> :
 *      <b>with</b> ( Expression ) Statement ;</pre>
 */
public class WithStatement extends AstNode {

	private AstNode expression;
	private AstNode statement;
	private int lp = -1;
	private int rp = -1;

	{
		type = Token.WITH;
	}

	public WithStatement() {
	}

	public WithStatement(int pos) {
		super(pos);
	}

	public WithStatement(int pos, int len) {
		super(pos, len);
	}

	/**
	 * Returns object expression
	 */
	public AstNode getExpression() {
		return expression;
	}

	/**
	 * Sets object expression (and its parent link)
	 *
	 * @throws IllegalArgumentException} if expression is {@code null}
	 */
	public void setExpression(AstNode expression) {
		assertNotNull(expression);
		this.expression = expression;
		expression.setParent(this);
	}

	/**
	 * Returns the statement or block
	 */
	public AstNode getStatement() {
		return statement;
	}

	/**
	 * Sets the statement (and sets its parent link)
	 *
	 * @throws IllegalArgumentException} if statement is {@code null}
	 */
	public void setStatement(AstNode statement) {
		assertNotNull(statement);
		this.statement = statement;
		statement.setParent(this);
	}

	/**
	 * Returns left paren offset
	 */
	public int getLp() {
		return lp;
	}

	/**
	 * Sets left paren offset
	 */
	public void setLp(int lp) {
		this.lp = lp;
	}

	/**
	 * Returns right paren offset
	 */
	public int getRp() {
		return rp;
	}

	/**
	 * Sets right paren offset
	 */
	public void setRp(int rp) {
		this.rp = rp;
	}

	/**
	 * Sets both paren positions
	 */
	public void setParens(int lp, int rp) {
		this.lp = lp;
		this.rp = rp;
	}
}
