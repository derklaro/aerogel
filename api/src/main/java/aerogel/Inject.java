/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jetbrains.annotations.NotNull;

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
 *     Methods: every method which gets called by an {@link Invoker} can be annotated with {@code @Inject}. It will
 *     indicate that the method requests the injection of types from the associated {@link Injector}. Passed instances
 *     to the invoker are preferred over instances provided by the {@link Injector}.
 *   </li>
 *   <li>
 *     Fields: every field in a class can be initialized by using {@link FieldInjector#inject(Object)}. Any field
 *     annotated with {@code @Inject} in the passed instance will get it's value rewritten to the one provided by the
 *     associated {@link Injector}. Passed instances to the invoker are preferred over instances provided by the {@link Injector}.
 *   </li>
 * </ul>
 *
 * @author Pasqual K.
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
public @interface Inject {

  /**
   * The name to use when injecting an instance. The name can be set when providing an instance to an {@link Injector},
   * or using any other annotation marking an instance as injectable such as {@link Provides} or {@link ProvidedBy}. If
   * an instance has no name set the default value will be the name of the member to inject, for example the name of the
   * field which is annotated by this annotation. If no name can be resolved or no binding with the name exists the
   * injection will fail if the name is explicitly supplied, if not the injector will fall back to use an instance of
   * the type annotated, for example the field type.
   *
   * @return the name of the injected instance to use, falls back to no name by default.
   */
  @NotNull String name() default "";

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
