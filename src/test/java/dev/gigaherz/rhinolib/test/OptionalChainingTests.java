package dev.gigaherz.rhinolib.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@SuppressWarnings("unused")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OptionalChainingTests {
	public static final RhinoTest TEST = new RhinoTest("optionalChaining").shareScope();

	@Test
	@Order(1)
	public void init() {
		TEST.test("init", """
				let a = { b: { c: 'd' } }
				let e = { f: {} }
				let h = null
				""", "");
	}

	@Test
	@Order(2)
	public void shouldError() {
		TEST.test("shouldError", """
				console.info(a.b.c)
				console.info(e.f.g)
				console.info(h.i.j)
				""", """
				d
				undefined
				Error: TypeError: Cannot read property "i" from null (optionalChaining/shouldError#3)
				""");
	}

	@Test
	@Order(2)
	public void shouldntError() {
		TEST.test("shouldntError", """
				console.info(a?.b?.c)
				console.info(e?.f?.g)
				console.info(h?.i?.j)
				""", """
				d
				undefined
				undefined
				""");
	}
}
