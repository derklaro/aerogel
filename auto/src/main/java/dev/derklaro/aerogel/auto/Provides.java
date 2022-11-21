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

package dev.derklaro.aerogel.auto;

import dev.derklaro.aerogel.ProvidedBy;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * Defines which type the annotated class is bound to. It will be used when type lookups are made to find the class
 * implementing or extending the required abstract instance. It works like {@link ProvidedBy} in the opposite
 * direction.
 *
 * <p>All given provided classes must be super interfaces/classes of the annotated class. If that is not the case,
 * injecting a class from the annotated class based on the binding will result in a class cast exception.
 *
 * <p>Note that providing instances for primitive and array types is not supported by this annotation. Due to the
 * limitations of annotations, support for generics (like {@code Collection<String>}) is not possible.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@API(status = API.Status.STABLE, since = "1.0")
public @interface Provides {

  /**
   * Defines which class this instance should be bound to. The given type must be super interface or class of the
   * annotated class in order for this annotation to work correctly.
   *
   * @return the type to which the annotated class should get bound.
   */
  @NotNull Class<?>[] value();
}
