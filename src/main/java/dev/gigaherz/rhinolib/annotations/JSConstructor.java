/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.gigaherz.rhinolib.annotations;

import dev.gigaherz.rhinolib.Context;
import dev.gigaherz.rhinolib.Scriptable;
import dev.gigaherz.rhinolib.ScriptableObject;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that marks a Java method as JavaScript constructor. This can
 * be used as an alternative to the <code>jsConstructor</code> naming convention described in
 * {@link ScriptableObject#defineClass(Scriptable, Class, boolean, boolean, Context)}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface JSConstructor {
}
