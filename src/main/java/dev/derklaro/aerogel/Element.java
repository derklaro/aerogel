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

import dev.derklaro.aerogel.internal.DefaultElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import org.apiguardian.api.API;
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
  static @NotNull Element forType(@NotNull Type type) {
    Objects.requireNonNull(type, "type");
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
  @NotNull Collection<AnnotationPredicate<?>> requiredAnnotations();

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
   * @param annotation the annotations to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given annotation is null.
   */
  @NotNull Element requireAnnotation(@NotNull Annotation annotation);

  /**
   * Adds the given annotation types as required annotations.
   *
   * @param annotationType the annotation types to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given annotation type is null.
   */
  @NotNull Element requireAnnotation(@NotNull Class<? extends Annotation> annotationType);

  /**
   * Adds the given annotation predicates as a requirement for this element.
   *
   * @param predicate the predicate to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given predicate is null.
   */
  @NotNull Element requireAnnotation(@NotNull AnnotationPredicate<?> predicate);

  /**
   * Get if this element has special requirements.
   *
   * @return true if this element has special requirements, false otherwise.
   * @since 2.0
   */
  @API(status = API.Status.STABLE, since = "2.0")
  boolean hasSpecialRequirements();

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
