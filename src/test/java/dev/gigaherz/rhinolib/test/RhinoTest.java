package dev.gigaherz.rhinolib.test;

import dev.gigaherz.rhinolib.Context;
import dev.gigaherz.rhinolib.NativeObject;
import dev.gigaherz.rhinolib.Scriptable;
import org.junit.jupiter.api.Assertions;

public class RhinoTest {
	public final String testName;
	public final Context context;
	public final Scriptable rootScope;
	public boolean shareScope;
	public TestConsole console;

	public RhinoTest(String n) {
		testName = n;
		context = Context.enter();
		console = new TestConsole(context);

		rootScope = context.initStandardObjects();
		context.addToScope(rootScope, "console", console);
	}

	public RhinoTest shareScope() {
		shareScope = true;
		return this;
	}

	public void test(String name, String script, String match) {
		try {
			Scriptable scope;

			if (shareScope) {
				scope = rootScope;
			} else {
				scope = new NativeObject(context);
				scope.setParentScope(rootScope);
			}

			context.evaluateString(scope, script, testName + "/" + name, 1, null);
		} catch (Exception ex) {
			ex.printStackTrace();
			console.info("Error: " + ex.getMessage());
			// ex.printStackTrace();
		}

		Assertions.assertEquals(match.trim(), console.getConsoleOutput().trim());
	}
}
