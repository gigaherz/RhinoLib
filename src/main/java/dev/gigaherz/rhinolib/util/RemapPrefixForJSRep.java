package dev.gigaherz.rhinolib.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows you to change field or method name on class scale with prefix
 *
 * @author LatvianModder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RemapPrefixForJSRep {
	RemapPrefixForJS[] value();
}
