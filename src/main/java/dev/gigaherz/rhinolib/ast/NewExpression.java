/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib.ast;

import dev.gigaherz.rhinolib.Token;

/**
 * New expression. Node type is {@link Token#NEW}.
 *
 * <pre><i>NewExpression</i> :
 *      MemberExpression
 *      <b>new</b> NewExpression</pre>
 * <p>
 * This node is a subtype of {@link FunctionCall}, mostly for internal code
 * sharing.  Structurally a {@code NewExpression} node is very similar to a
 * {@code FunctionCall}, so it made a certain amount of sense.
 */
public class NewExpression extends FunctionCall {

	private ObjectLiteral initializer;

	{
		type = Token.NEW;
	}

	public NewExpression() {
	}

	public NewExpression(int pos) {
		super(pos);
	}

	public NewExpression(int pos, int len) {
		super(pos, len);
	}

	/**
	 * Returns initializer object, if any.
	 *
	 * @return extra initializer object-literal expression, or {@code null} if
	 * not specified.
	 */
	public ObjectLiteral getInitializer() {
		return initializer;
	}

	/**
	 * Sets initializer object.  Rhino supports an experimental syntax
	 * of the form {@code new expr [ ( arglist ) ] [initializer]},
	 * in which initializer is an object literal that is used to set
	 * additional properties on the newly-created {@code expr} object.
	 *
	 * @param initializer extra initializer object.
	 *                    Can be {@code null}.
	 */
	public void setInitializer(ObjectLiteral initializer) {
		this.initializer = initializer;
		if (initializer != null) {
			initializer.setParent(this);
		}
	}
}
