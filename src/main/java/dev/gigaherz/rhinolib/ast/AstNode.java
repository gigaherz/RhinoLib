/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib.ast;

import dev.gigaherz.rhinolib.Kit;
import dev.gigaherz.rhinolib.Node;
import dev.gigaherz.rhinolib.Token;

import java.util.Comparator;

/**
 * Base class for AST node types.  The goal of the AST is to represent the
 * physical source code, to make it useful for code-processing tools such
 * as IDEs or pretty-printers.  The parser must not rewrite the parse tree
 * when producing this representation. <p>
 * <p>
 * The {@code AstNode} hierarchy sits atop the older {@link Node} class,
 * which was designed for code generation.  The {@code Node} class is a
 * flexible, weakly-typed class suitable for creating and rewriting code
 * trees, but using it requires you to remember the exact ordering of the
 * child nodes, which are kept in a linked list.  The {@code AstNode}
 * hierarchy is a strongly-typed facade with named accessors for children
 * and common properties, but under the hood it's still using a linked list
 * of child nodes.  It isn't a very good idea to use the child list directly
 * unless you know exactly what you're doing.</p>
 * <p>
 * Note that {@code AstNode} records additional information, including
 * the node's position, length, and parent node.  Also, some {@code AstNode}
 * subclasses record some of their child nodes in instance members, since
 * they are not needed for code generation.  In a nutshell, only the code
 * generator should be mixing and matching {@code AstNode} and {@code Node}
 * objects.<p>
 * <p>
 * All offset fields in all subclasses of AstNode are relative to their
 * parent.  For things like paren, bracket and keyword positions, the
 * position is relative to the current node.  The node start position is
 * relative to the parent node. <p>
 * <p>
 * During the actual parsing, node positions are absolute; adding the node to
 * its parent fixes up the offsets to be relative.  By the time you see the AST
 * (e.g. using the {@code Visitor} interface), the offsets are relative. <p>
 * <p>
 * {@code AstNode} objects have property lists accessible via the
 * {@link #getProp} and {@link #putProp} methods.  The property lists are
 * integer-keyed with arbitrary {@code Object} values.  For the most part the
 * parser generating the AST avoids using properties, preferring fields for
 * elements that are always set.  Property lists are intended for user-defined
 * annotations to the tree.  The Rhino code generator acts as a client and
 * uses node properties extensively.  You are welcome to use the property-list
 * API for anything your client needs.<p>
 * <p>
 * This hierarchy does not have separate branches for expressions and
 * statements, as the distinction in JavaScript is not as clear-cut as in
 * Java or C++.
 */
public abstract class AstNode extends Node implements Comparable<AstNode> {
	public static class PositionComparator implements Comparator<AstNode> {
		/**
		 * Sorts nodes by (relative) start position.  The start positions are
		 * relative to their parent, so this comparator is only meaningful for
		 * comparing siblings.
		 */
		@Override
		public int compare(AstNode n1, AstNode n2) {
			return n1.position - n2.position;
		}
	}

	/**
	 * @see Kit#codeBug
	 */
	public static RuntimeException codeBug() throws RuntimeException {
		throw Kit.codeBug();
	}

	protected int position = -1;
	protected int length = 1;
	protected AstNode parent;
	/*
	 * Holds comments that are on same line as of actual statement e.g.
	 * For a for loop
	 *      1) for(var i=0; i<10; i++) //test comment { }
	 *      2) for(var i=0; i<10; i++)
	 *          //test comment
	 *          //test comment 2
	 *          { }
	 * For If Statement
	 *      1) if (x == 2) //test if comment
	 *             a = 3 + 4; //then comment
	 * and so on
	 */
	protected AstNode inlineComment;

	public AstNode() {
		super(Token.ERROR);
	}

	/**
	 * Constructs a new AstNode
	 *
	 * @param pos the start position
	 */
	public AstNode(int pos) {
		this();
		position = pos;
	}

	/**
	 * Constructs a new AstNode
	 *
	 * @param pos the start position
	 * @param len the number of characters spanned by the node in the source
	 *            text
	 */
	public AstNode(int pos, int len) {
		this();
		position = pos;
		length = len;
	}

	/**
	 * Returns relative position in parent
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Sets relative position in parent
	 */
	public void setPosition(int position) {
		this.position = position;
	}

	/**
	 * Returns the absolute document position of the node.
	 * Computes it by adding the node's relative position
	 * to the relative positions of all its parents.
	 */
	public int getAbsolutePosition() {
		int pos = position;
		AstNode parent = this.parent;
		while (parent != null) {
			pos += parent.getPosition();
			parent = parent.getParent();
		}
		return pos;
	}

	/**
	 * Returns node length
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Sets node length
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * Sets the node start and end positions.
	 * Computes the length as ({@code end} - {@code position}).
	 */
	public void setBounds(int position, int end) {
		setPosition(position);
		setLength(end - position);
	}

	/**
	 * Make this node's position relative to a parent.
	 * Typically only used by the parser when constructing the node.
	 *
	 * @param parentPosition the absolute parent position; the
	 *                       current node position is assumed to be absolute and is
	 *                       decremented by parentPosition.
	 */
	public void setRelative(int parentPosition) {
		this.position -= parentPosition;
	}

	/**
	 * Returns the node parent, or {@code null} if it has none
	 */
	public AstNode getParent() {
		return parent;
	}

	/**
	 * Sets the node parent.  This method automatically adjusts the
	 * current node's start position to be relative to the new parent.
	 *
	 * @param parent the new parent. Can be {@code null}.
	 */
	public void setParent(AstNode parent) {
		if (parent == this.parent) {
			return;
		}

		// Convert position back to absolute.
		if (this.parent != null) {
			setRelative(-this.parent.getAbsolutePosition());
		}

		this.parent = parent;
		if (parent != null) {
			setRelative(parent.getAbsolutePosition());
		}
	}

	/**
	 * Adds a child or function to the end of the block.
	 * Sets the parent of the child to this node, and fixes up
	 * the start position of the child to be relative to this node.
	 * Sets the length of this node to include the new child.
	 *
	 * @param kid the child
	 * @throws IllegalArgumentException if kid is {@code null}
	 */
	public void addChild(AstNode kid) {
		assertNotNull(kid);
		int end = kid.getPosition() + kid.getLength();
		setLength(end - this.getPosition());
		addChildToBack(kid);
		kid.setParent(this);
	}

	/**
	 * Returns the root of the tree containing this node.
	 *
	 * @return the {@link AstRoot} at the root of this node's parent
	 * chain, or {@code null} if the topmost parent is not an {@code AstRoot}.
	 */
	public AstRoot getAstRoot() {
		AstNode parent = this;  // this node could be the AstRoot
		while (parent != null && !(parent instanceof AstRoot)) {
			parent = parent.getParent();
		}
		return (AstRoot) parent;
	}

	/**
	 * Returns a short, descriptive name for the node, such as
	 * "ArrayComprehension".
	 */
	public String shortName() {
		String classname = getClass().getName();
		int last = classname.lastIndexOf(".");
		return classname.substring(last + 1);
	}

	// subclasses with potential side effects should override this
	@Override
	public boolean hasSideEffects() {
		return switch (getType()) {         // Avoid cascaded error messages
			case Token.ASSIGN, Token.ASSIGN_ADD, Token.ASSIGN_BITAND, Token.ASSIGN_BITOR, Token.ASSIGN_BITXOR, Token.ASSIGN_DIV, Token.ASSIGN_LSH, Token.ASSIGN_MOD, Token.ASSIGN_MUL, Token.ASSIGN_RSH, Token.ASSIGN_SUB, Token.ASSIGN_URSH, Token.BLOCK, Token.BREAK, Token.CALL, Token.CATCH, Token.CATCH_SCOPE, Token.CONST, Token.CONTINUE, Token.DEC, Token.DELPROP, Token.DEL_REF, Token.DO, Token.ELSE, Token.ENTERWITH, Token.ERROR, Token.EXPORT, Token.EXPR_RESULT, Token.FINALLY, Token.FUNCTION, Token.FOR, Token.GOTO, Token.IF, Token.IFEQ, Token.IFNE, Token.IMPORT, Token.INC, Token.JSR, Token.LABEL, Token.LEAVEWITH, Token.LET, Token.LETEXPR, Token.LOCAL_BLOCK, Token.LOOP, Token.NEW, Token.REF_CALL, Token.RETHROW, Token.RETURN, Token.RETURN_RESULT, Token.SEMI, Token.SETELEM, Token.SETELEM_OP, Token.SETNAME, Token.SETPROP, Token.SETPROP_OP, Token.SETVAR, Token.SET_REF, Token.SET_REF_OP, Token.SWITCH, Token.TARGET, Token.THROW, Token.TRY, Token.VAR, Token.WHILE, Token.WITH, Token.WITHEXPR, Token.YIELD, Token.YIELD_STAR ->
					true;
			default -> false;
		};
	}

	/**
	 * Bounces an IllegalArgumentException up if arg is {@code null}.
	 *
	 * @param arg any method argument
	 * @throws IllegalArgumentException if the argument is {@code null}
	 */
	protected void assertNotNull(Object arg) {
		if (arg == null) {
			throw new IllegalArgumentException("arg cannot be null");
		}
	}

	// TODO(stevey):  think of a way to have polymorphic toString
	// methods while keeping the ability to use Node.toString for
	// dumping the IR with Token.printTrees.  Most likely:  change
	// Node.toString to be Node.dumpTree and change callers to use that.
	// For now, need original toString, to compare output to old Rhino's.

	//     @Override
	//     public String toString() {
	//         return this.getClass().getName() + ": " +
	//             Token.typeToName(getType());
	//     }

	/**
	 * Returns the innermost enclosing function, or {@code null} if not in a
	 * function.  Begins the search with this node's parent.
	 *
	 * @return the {@link FunctionNode} enclosing this node, else {@code null}
	 */
	public FunctionNode getEnclosingFunction() {
		AstNode parent = this.getParent();
		while (parent != null && !(parent instanceof FunctionNode)) {
			parent = parent.getParent();
		}
		return (FunctionNode) parent;
	}

	/**
	 * Returns the innermost enclosing {@link Scope} node, or {@code null}
	 * if we're not nested in a scope.  Begins the search with this node's parent.
	 * Note that this is not the same as the defining scope for a {@link Name}.
	 *
	 * @return the {@link Scope} enclosing this node, else {@code null}
	 */
	public Scope getEnclosingScope() {
		AstNode parent = this.getParent();
		while (parent != null && !(parent instanceof Scope)) {
			parent = parent.getParent();
		}
		return (Scope) parent;
	}

	/**
	 * Permits AST nodes to be sorted based on start position and length.
	 * This makes it easy to sort Comment and Error nodes into a set of
	 * other AST nodes:  just put them all into a {@link java.util.SortedSet},
	 * for instance.
	 *
	 * @param other another node
	 * @return -1 if this node's start position is less than {@code other}'s
	 * start position.  If tied, -1 if this node's length is less than
	 * {@code other}'s length.  If the lengths are equal, sorts abitrarily
	 * on hashcode unless the nodes are the same per {@link #equals}.
	 */
	@Override
	public int compareTo(AstNode other) {
		if (this.equals(other)) {
			return 0;
		}
		int abs1 = this.getAbsolutePosition();
		int abs2 = other.getAbsolutePosition();
		if (abs1 < abs2) {
			return -1;
		}
		if (abs2 < abs1) {
			return 1;
		}
		int len1 = this.getLength();
		int len2 = other.getLength();
		if (len1 < len2) {
			return -1;
		}
		if (len2 < len1) {
			return 1;
		}
		return this.hashCode() - other.hashCode();
	}

	/**
	 * Returns the depth of this node.  The root is depth 0, its
	 * children are depth 1, and so on.
	 *
	 * @return the node depth in the tree
	 */
	public int depth() {
		return parent == null ? 0 : 1 + parent.depth();
	}

	/**
	 * Return the line number recorded for this node.
	 * If no line number was recorded, searches the parent chain.
	 *
	 * @return the nearest line number, or -1 if none was found
	 */
	@Override
	public int getLineno() {
		if (lineno != -1) {
			return lineno;
		}
		if (parent != null) {
			return parent.getLineno();
		}
		return -1;
	}

	public AstNode getInlineComment() {
		return inlineComment;
	}

	public void setInlineComment(AstNode inlineComment) {
		this.inlineComment = inlineComment;
	}
}
