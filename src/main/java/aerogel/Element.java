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

import aerogel.internal.DefaultElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Wraps a type which has annotation based attributes.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public interface Element {

  /**
   * Creates a new element for the given {@code type}.
   *
   * @param type the type of the element.
   * @return a new element for the given {@code type}.
   * @throws NullPointerException if {@code type} is null.
   */
  static @NotNull Element get(@NotNull Type type) {
    Objects.requireNonNull(type, "A type is required to construct an element");
    return new DefaultElement(type);
  }

  /**
   * Get the name of this element.
   *
   * @return the name of this element or {@code null} if this element is not named.
   */
  @Nullable String requiredName();

  /**
   * Get the type of this element.
   *
   * @return the type of this element.
   */
  @NotNull Type componentType();

  /**
   * Get an unmodifiable view of all required annotations of this element.
   *
   * @return all required annotations of this element.
   */
  @UnmodifiableView
  @NotNull Collection<AnnotationComparer> annotationComparer();

  /**
   * Sets the required name of this element.
   *
   * @param name the new required name of this element or {@code null} if no name should be required.
   * @return the same instance as used to call the method, for chaining.
   */
  @NotNull Element requireName(@Nullable String name);

  /**
   * Adds the given annotations as required annotations.
   *
   * @param annotations the annotations to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if an element of {@code annotations} is null.
   */
  @NotNull Element requireAnnotations(@NotNull Annotation... annotations);

  /**
   * Adds the given annotation types as required annotations.
   *
   * @param annotationTypes the annotation types to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if an element of {@code annotationTypes} is null.
   */
  @NotNull Element requireAnnotations(@NotNull Class<?>... annotationTypes);

  /**
   * {@inheritDoc}
   */
  @Override
  @NotNull String toString();

  /**
   * {@inheritDoc}
   */
  @Override
  int hashCode();

  /**
   * {@inheritDoc}
   */
  @Override
  boolean equals(@NotNull Object other);
}
