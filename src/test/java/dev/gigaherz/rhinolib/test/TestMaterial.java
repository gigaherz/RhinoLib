package dev.gigaherz.rhinolib.test;

import java.util.HashMap;
import java.util.Map;

public record TestMaterial(String name) {
	public static final Map<String, TestMaterial> MATERIALS = new HashMap<>();

	public static TestMaterial get(Object o) {
		return MATERIALS.computeIfAbsent(String.valueOf(o), TestMaterial::new);
	}
}
