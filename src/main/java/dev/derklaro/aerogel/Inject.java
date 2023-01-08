/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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
 * Represents the main injection marker during the runtime for {@link Injector}s. This annotation can be added to the
 * following types to indicate a construction request regardless of its access modifier (for example: public, private,
 * protected).
 *
 * <ul>
 *   <li>
 *     Constructors: every instance an {@link Injector} makes must have either a no-args constructor or a constructor
 *     marked as {@code @Inject} to indicate a member injection request to that constructor. A constructor annotated as
 *     {@code @Inject} will be used before checking for no-args constructors.
 *   </li>
 *   <li>
 *     Methods/Fields: every method or field which is not abstract (for method) or not final (for fields) can be annotated
 *     with {@link Inject}. This will cause an injector to inject the annotated field/method when construing an instance
 *     of the target class. All these methods and fields will be available for direct injection using a
 *     {@link MemberInjector} as well.
 *   </li>
 * </ul>
 *
 * <p>If the annotation is placed on a method the invocation order can be influenced by using {@link Order}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@API(status = API.Status.STABLE, since = "1.0")
public @interface Inject {

  /**
   * If true the injector will only do the injection when all types can be collected which are required to proceed and
   * will not throw an exception if one of them is missing. Fields will still have the same value as before and methods
   * will not get called. The only exception are constructors. If an instance can not be constructed because one binding
   * is missing, it will still result in an exception because the invocation can not proceed and there is no way to
   * provide the requested instance.
   *
   * @return if the value to inject is not required.
   */
  boolean optional() default false;
}
