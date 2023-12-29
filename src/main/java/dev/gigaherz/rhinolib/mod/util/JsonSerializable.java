package dev.gigaherz.rhinolib.mod.util;

import com.google.gson.JsonElement;
import dev.gigaherz.rhinolib.util.RemapForJS;

/**
 * @author LatvianModder
 */
public interface JsonSerializable {
	@RemapForJS("toJson")
	JsonElement toJsonJS();
}