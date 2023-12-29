/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib.ast;

import dev.gigaherz.rhinolib.Token;

/**
 * For-in or for-each-in or for-of statement.  Node type is {@link Token#FOR}.
 *
 * <pre><b>for</b> [<b>each</b>] ( LeftHandSideExpression <b>in</b> Expression ) Statement</pre>
 * <pre><b>for</b> [<b>each</b>] ( <b>var</b> VariableDeclarationNoIn <b>in</b> Expression ) Statement</pre>
 * <pre><b>for</b> ( LeftHandSideExpression <b>of</b> Expression ) Statement</pre>
 * <pre><b>for</b> ( ForDeclaration <b>of</b> Expression ) Statement</pre>
 */
public class ForInLoop extends Loop {

	protected AstNode iterator;
	protected AstNode iteratedObject;
	protected int inPosition = -1;
	protected int eachPosition = -1;
	protected boolean isForEach;
	protected boolean isForOf;

	{
		type = Token.FOR;
	}

	public ForInLoop() {
	}

	public ForInLoop(int pos) {
		super(pos);
	}

	public ForInLoop(int pos, int len) {
		super(pos, len);
	}

	/**
	 * Returns loop iterator expression
	 */
	public AstNode getIterator() {
		return iterator;
	}

	/**
	 * Sets loop iterator expression:  the part before the "in" or "of" keyword.
	 * Also sets its parent to this node.
	 *
	 * @throws IllegalArgumentException if {@code iterator} is {@code null}
	 */
	public void setIterator(AstNode iterator) {
		assertNotNull(iterator);
		this.iterator = iterator;
		iterator.setParent(this);
	}

	/**
	 * Returns object being iterated over
	 */
	public AstNode getIteratedObject() {
		return iteratedObject;
	}

	/**
	 * Sets object being iterated over, and sets its parent to this node.
	 *
	 * @throws IllegalArgumentException if {@code object} is {@code null}
	 */
	public void setIteratedObject(AstNode object) {
		assertNotNull(object);
		this.iteratedObject = object;
		object.setParent(this);
	}

	/**
	 * Returns whether the loop is a for-each loop
	 */
	public boolean isForEach() {
		return isForEach;
	}

	/**
	 * Sets whether the loop is a for-each loop
	 */
	public void setIsForEach(boolean isForEach) {
		this.isForEach = isForEach;
	}

	/**
	 * Returns whether the loop is a for-of loop
	 */
	public boolean isForOf() {
		return isForOf;
	}

	/**
	 * Sets whether the loop is a for-each loop
	 */
	public void setIsForOf(boolean isForOf) {
		this.isForOf = isForOf;
	}

	/**
	 * Returns position of "in" or "of" keyword
	 */
	public int getInPosition() {
		return inPosition;
	}

	/**
	 * Sets position of "in" or "of" keyword
	 *
	 * @param inPosition position of "in" or "of" keyword,
	 *                   or -1 if not present (e.g. in presence of a syntax error)
	 */
	public void setInPosition(int inPosition) {
		this.inPosition = inPosition;
	}

	/**
	 * Returns position of "each" keyword
	 */
	public int getEachPosition() {
		return eachPosition;
	}

	/**
	 * Sets position of "each" keyword
	 *
	 * @param eachPosition position of "each" keyword,
	 *                     or -1 if not present.
	 */
	public void setEachPosition(int eachPosition) {
		this.eachPosition = eachPosition;
	}
}
