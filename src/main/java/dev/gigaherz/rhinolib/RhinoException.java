/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */


package dev.gigaherz.rhinolib;

import java.io.CharArrayWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class of exceptions thrown by the JavaScript engine.
 */
public abstract class RhinoException extends RuntimeException {
	private static final Pattern JAVA_STACK_PATTERN = Pattern.compile("_c_(.*)_\\d+");
	@Serial
	private static final long serialVersionUID = 1883500631321581169L;

	static String formatStackTrace(ScriptStackElement[] stack, String message) {
		StringBuilder buffer = new StringBuilder();
		String lineSeparator = System.lineSeparator();

		for (ScriptStackElement elem : stack) {
			elem.renderJavaStyle(buffer);
			buffer.append(lineSeparator);
		}
		return buffer.toString();
	}

	Object interpreterStackInfo;
	int[] interpreterLineData;
	private String sourceName;
	private int lineNumber;
	private String lineSource;
	private int columnNumber;

	RhinoException(Context cx) {
		Evaluator e = Context.createInterpreter();
		if (e != null) {
			e.captureStackInfo(cx, this);
		}
	}

	RhinoException(Context cx, String details) {
		super(details);
		Evaluator e = Context.createInterpreter();
		if (e != null) {
			e.captureStackInfo(cx, this);
		}
	}

	@Override
	public final String getMessage() {
		String details = details();
		if (sourceName == null || lineNumber <= 0) {
			return details;
		}
		StringBuilder buf = new StringBuilder(details);
		buf.append(" (");
		buf.append(sourceName);
		if (lineNumber > 0) {
			buf.append('#');
			buf.append(lineNumber);
		}
		buf.append(')');
		return buf.toString();
	}

	public String details() {
		return super.getMessage();
	}

	/**
	 * Get the uri of the script source containing the error, or null
	 * if that information is not available.
	 */
	public final String sourceName() {
		return sourceName;
	}

	/**
	 * Initialize the uri of the script source containing the error.
	 *
	 * @param sourceName the uri of the script source responsible for the error.
	 *                   It should not be <code>null</code>.
	 * @throws IllegalStateException if the method is called more then once.
	 */
	public final void initSourceName(String sourceName) {
		if (sourceName == null) {
			throw new IllegalArgumentException();
		}
		if (this.sourceName != null) {
			throw new IllegalStateException();
		}
		this.sourceName = sourceName;
	}

	/**
	 * Returns the line number of the statement causing the error,
	 * or zero if not available.
	 */
	public final int lineNumber() {
		return lineNumber;
	}

	/**
	 * Initialize the line number of the script statement causing the error.
	 *
	 * @param lineNumber the line number in the script source.
	 *                   It should be positive number.
	 * @throws IllegalStateException if the method is called more then once.
	 */
	public final void initLineNumber(int lineNumber) {
		if (lineNumber <= 0) {
			throw new IllegalArgumentException(String.valueOf(lineNumber));
		}
		if (this.lineNumber > 0) {
			throw new IllegalStateException();
		}
		this.lineNumber = lineNumber;
	}

	/**
	 * The column number of the location of the error, or zero if unknown.
	 */
	public final int columnNumber() {
		return columnNumber;
	}

	/**
	 * Initialize the column number of the script statement causing the error.
	 *
	 * @param columnNumber the column number in the script source.
	 *                     It should be positive number.
	 * @throws IllegalStateException if the method is called more then once.
	 */
	public final void initColumnNumber(int columnNumber) {
		if (columnNumber <= 0) {
			throw new IllegalArgumentException(String.valueOf(columnNumber));
		}
		if (this.columnNumber > 0) {
			throw new IllegalStateException();
		}
		this.columnNumber = columnNumber;
	}

	/**
	 * The source text of the line causing the error, or null if unknown.
	 */
	public final String lineSource() {
		return lineSource;
	}

	/**
	 * Initialize the text of the source line containing the error.
	 *
	 * @param lineSource the text of the source line responsible for the error.
	 *                   It should not be <code>null</code>.
	 * @throws IllegalStateException if the method is called more then once.
	 */
	public final void initLineSource(String lineSource) {
		if (lineSource == null) {
			throw new IllegalArgumentException();
		}
		if (this.lineSource != null) {
			throw new IllegalStateException();
		}
		this.lineSource = lineSource;
	}

	final void recordErrorOrigin(String sourceName, int lineNumber, String lineSource, int columnNumber) {
		// XXX: for compatibility allow for now -1 to mean 0
		if (lineNumber == -1) {
			lineNumber = 0;
		}

		if (sourceName != null) {
			initSourceName(sourceName);
		}
		if (lineNumber != 0) {
			initLineNumber(lineNumber);
		}
		if (lineSource != null) {
			initLineSource(lineSource);
		}
		if (columnNumber != 0) {
			initColumnNumber(columnNumber);
		}
	}

	private String generateStackTrace() {
		// Get stable reference to work properly with concurrent access
		CharArrayWriter writer = new CharArrayWriter();
		super.printStackTrace(new PrintWriter(writer));
		String origStackTrace = writer.toString();
		Evaluator e = Context.createInterpreter();
		if (e != null) {
			return e.getPatchedStack(this, origStackTrace);
		}
		return null;
	}

	/**
	 * Get a string representing the script stack of this exception.
	 * If optimization is enabled, this includes java stack elements
	 * whose source and method names suggest they have been generated
	 * by the Rhino script compiler.
	 *
	 * @return a script stack dump
	 * @since 1.6R6
	 */
	public String getScriptStackTrace() {
		return getScriptStackTrace(NativeError.DEFAULT_STACK_LIMIT, null);
	}

	/**
	 * Get a string representing the script stack of this exception.
	 * If optimization is enabled, this includes java stack elements
	 * whose source and method names suggest they have been generated
	 * by the Rhino script compiler.
	 * The optional "limit" parameter limits the number of stack frames returned.
	 * The "functionName" parameter will exclude any stack frames "below" the
	 * specified function on the stack.
	 *
	 * @param limit        the number of stack frames returned
	 * @param functionName the name of a function on the stack -- frames below it will be ignored
	 * @return a script stack dump
	 * @since 1.8.0
	 */
	public String getScriptStackTrace(int limit, String functionName) {
		ScriptStackElement[] stack = getScriptStack(limit, functionName);
		return formatStackTrace(stack, details());
	}

	/**
	 * Get the script stack of this exception as an array of
	 * {@link ScriptStackElement}s.
	 * If optimization is enabled, this includes java stack elements
	 * whose source and method names suggest they have been generated
	 * by the Rhino script compiler.
	 *
	 * @return the script stack for this exception
	 * @since 1.7R3
	 */
	public ScriptStackElement[] getScriptStack() {
		return getScriptStack(-1, null);
	}

	/**
	 * Get the script stack of this exception as an array of
	 * {@link ScriptStackElement}s.
	 * If optimization is enabled, this includes java stack elements
	 * whose source and method names suggest they have been generated
	 * by the Rhino script compiler.
	 *
	 * @param limit        the number of stack frames returned, or -1 for unlimited
	 * @param hideFunction the name of a function on the stack -- frames below it will be ignored, or null
	 * @return the script stack for this exception
	 * @since 1.8.0
	 */
	public ScriptStackElement[] getScriptStack(int limit, String hideFunction) {
		List<ScriptStackElement> list = new ArrayList<>();
		ScriptStackElement[][] interpreterStack = null;
		if (interpreterStackInfo != null) {
			Evaluator interpreter = Context.createInterpreter();
			if (interpreter instanceof Interpreter) {
				interpreterStack = ((Interpreter) interpreter).getScriptStackElements(this);
			}
		}

		int interpreterStackIndex = 0;
		StackTraceElement[] stack = getStackTrace();
		int count = 0;
		boolean printStarted = (hideFunction == null);

		// Pattern to recover function name from java method name -
		// see Codegen.getBodyMethodName()
		// kudos to Marc Guillemot for coming up with this
		for (StackTraceElement e : stack) {
			String fileName = e.getFileName();
			if (e.getMethodName().startsWith("_c_") && e.getLineNumber() > -1 && fileName != null && !fileName.endsWith(".java")) {
				String methodName = e.getMethodName();
				Matcher match = JAVA_STACK_PATTERN.matcher(methodName);
				// the method representing the main script is always "_c_script_0" -
				// at least we hope so
				methodName = !"_c_script_0".equals(methodName) && match.find() ? match.group(1) : null;

				if (!printStarted && hideFunction.equals(methodName)) {
					printStarted = true;
				} else if (printStarted && ((limit < 0) || (count < limit))) {
					list.add(new ScriptStackElement(fileName, methodName, e.getLineNumber()));
					count++;
				}

			} else if ("dev.gigaherz.rhinolib.Interpreter".equals(e.getClassName()) && "interpretLoop".equals(e.getMethodName()) && interpreterStack != null && interpreterStack.length > interpreterStackIndex) {

				for (ScriptStackElement elem : interpreterStack[interpreterStackIndex++]) {
					if (!printStarted && hideFunction.equals(elem.functionName)) {
						printStarted = true;
					} else if (printStarted && ((limit < 0) || (count < limit))) {
						list.add(elem);
						count++;
					}
				}
			}
		}
		return list.toArray(new ScriptStackElement[list.size()]);
	}

	@Override
	public void printStackTrace(PrintWriter s) {
		if (interpreterStackInfo == null) {
			super.printStackTrace(s);
		} else {
			s.print(generateStackTrace());
		}
	}

	@Override
	public void printStackTrace(PrintStream s) {
		if (interpreterStackInfo == null) {
			super.printStackTrace(s);
		} else {
			s.print(generateStackTrace());
		}
	}
}
