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
import java.util.Map;
import java.util.Objects;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Wraps a type which has annotation based attributes.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public interface Element {

  /**
   * Creates a new element for the given type, with no other requirements specified initially.
   *
   * @param type the type of the element.
   * @return a new element for the given type.
   * @throws NullPointerException if the given type is null.
   */
  @Contract(pure = true)
  static @NotNull Element forType(@NotNull Type type) {
    Objects.requireNonNull(type, "type");
    return new DefaultElement(type);
  }

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
  @NotNull Collection<AnnotationPredicate> requiredAnnotations();

  /**
   * Adds the given annotations as required annotations.
   *
   * @param annotation the annotations to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given annotation is null.
   */
  @NotNull Element requireAnnotation(@NotNull Annotation annotation);

  /**
   * Constructs a proxy for the given annotation type and requires it. This method only works when all values of the
   * given annotation type are optional (defaulted). With this in mind, the annotation added to the annotated element is
   * required to have all values set to the default as well.
   * <p>
   * If a value of the annotation should have a different type, use {@link #requireAnnotation(Class, Map)} instead.
   *
   * @param annotationType the type of annotation to require.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given annotation type is null.
   * @throws AerogelException     if the given annotation has a non-defaulted method.
   * @since 2.0
   */
  @API(status = API.Status.STABLE, since = "2.0")
  @NotNull Element requireAnnotation(@NotNull Class<? extends Annotation> annotationType);

  /**
   * Constructs a proxy for the given annotation type and requires it. The construction process requires the caller to
   * give a value in the overridden values map for all methods which are not optional (defaulted) in the given
   * annotation type. Overridden values for defaulted values can be passed as well, but are optional.
   *
   * @param annotationType the annotation types to add.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given annotation type or overridden value map is null.
   * @throws AerogelException     if the given annotation has a non-defaulted method which has no overridden value.
   * @since 2.0
   */
  @API(status = API.Status.STABLE, since = "2.0")
  @NotNull Element requireAnnotation(
    @NotNull Class<? extends Annotation> annotationType,
    @NotNull Map<String, Object> overriddenMethodValues);

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
