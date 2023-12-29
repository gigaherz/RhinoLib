package dev.gigaherz.rhinolib.mod.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class RhinoProperties {
	public static final RhinoProperties INSTANCE = new RhinoProperties();

	public static Path getGameDir() {
		throw new AssertionError();
	}

	public static boolean isDev() {
		throw new AssertionError();
	}

	public static InputStream openResource(String path) throws Exception {
		throw new AssertionError();
	}

	private final Properties properties;
	// public boolean forceLocalMappings;
	private boolean writeProperties;

	RhinoProperties() {
		properties = new Properties();

		try {
			var propertiesFile = getGameDir().resolve("rhino.local.properties");
			writeProperties = false;

			if (Files.exists(propertiesFile)) {
				try (Reader reader = Files.newBufferedReader(propertiesFile)) {
					properties.load(reader);
				}
			} else {
				writeProperties = true;
			}

			// forceLocalMappings = get("forceLocalMappings", false);

			if (writeProperties) {
				try (Writer writer = Files.newBufferedWriter(propertiesFile)) {
					properties.store(writer, "Local properties for Rhino, please do not push this to version control if you don't know what you're doing!");
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		LOGGER.info("Rhino properties loaded.");
	}

	private void remove(String key) {
		var s = properties.getProperty(key);

		if (s != null) {
			properties.remove(key);
			writeProperties = true;
		}
	}

	private String get(String key, String def) {
		var s = properties.getProperty(key);

		if (s == null) {
			properties.setProperty(key, def);
			writeProperties = true;
			return def;
		}

		return s;
	}

	private boolean get(String key, boolean def) {
		return get(key, def ? "true" : "false").equals("true");
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("RhinoProperties");
}
