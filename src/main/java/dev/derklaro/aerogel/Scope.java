/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 Pasqual K. and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package dev.derklaro.aerogel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apiguardian.api.API;

/**
 * This annotation identifies an annotation which represents a scope. When an injector creates an instance of a class
 * the resulting instance can be optionally scoped. When a later call to create an instance is done in the same scope,
 * the injector is required to return the same instance again.
 *
 * <p>There are two scopes present by default:
 * <ol>
 *   <li>The default scope: in this scope an instance is created (for example to inject a parameter) and the injector
 *   will forget about the instance instantly.
 *   <li>Singleton (indicated by &#064;Singleton): the instance for the class will be created only once in the scope of
 *   the current injector and children.
 * </ol>
 *
 * <p>A scope annotation...
 * <ol>
 *   <li>must use a runtime retention in runtime.
 *   <li>should have no attributes (as the injector will not use them, nor provide them to scope providers).
 *   <li>must not be inherited and must be directly applied to the target element.
 *   <li>can target a method (for factory) or class.
 * </ol>
 *
 * <p>If the scope annotation is missing on an annotation, the injector will not detect the given annotation as a scope
 * and ignore the added annotation.
 *
 * @author Pasqual K.
 * @see Singleton
 * @since 2.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@API(status = API.Status.STABLE, since = "2.0")
public @interface Scope {

}
