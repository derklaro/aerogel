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

/**
 * Binds the annotated method as a factory method for the return type of the method. Any type from which the return type
 * extends or implements are bound to that factory as well. Factory methods are not preferred over actual bindings
 * supplied to the associated {@link Injector}. Arguments supplied to the factory methods are taken from the associated
 * {@link Injector}. Please note that in order for this annotation to work the annotation scanning must get enabled when
 * building an injector instance.
 *
 * @author Pasqual K.
 * @since 1.0
 */
// @todo: find a good way to implement this
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Factory {

  /**
   * If {@code true} all super types of the method's return type are bound to that factory method as well. For example,
   * if class {@code Abc} extends from {@code Abcd} and the factory method looks something like the following:
   * <code>
   * {@literal @}Factory public static Abc newAbc(DateTime now) { return new Abc(now); }
   * </code>
   * any instance of {@code Abcd} requested via the {@link Injector} will be taken generated from the factory method as
   * long as there is no specific binding for the type {@code Abcd} in the factory. This setting defaults for simplicity
   * reasons to {@code false}.
   *
   * @return if the super types of the method return type should be included when constructing via the factory method.
   */
  boolean includeSuper() default false;
}
