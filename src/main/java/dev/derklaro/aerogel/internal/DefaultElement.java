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

package dev.derklaro.aerogel.internal;

import dev.derklaro.aerogel.AnnotationPredicate;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.internal.annotation.AnnotationFactory;
import dev.derklaro.aerogel.internal.annotation.DefaultAnnotationPredicate;
import dev.derklaro.aerogel.internal.utility.ToStringHelper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of an element. See {@link Element#forType(Type)}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal")
public final class DefaultElement implements Element {

  private final Type componentType;
  private final List<AnnotationPredicate> annotationPredicates;

  /**
   * Constructs a new default element type instance.
   *
   * @param componentType the type of the element.
   */
  public DefaultElement(@NotNull Type componentType) {
    this.componentType = componentType;
    this.annotationPredicates = Collections.emptyList();
  }

  /**
   * Constructs a new default element type instance.
   *
   * @param componentType        the type of the element.
   * @param annotationPredicates the required annotations of the element.
   */
  private DefaultElement(@NotNull Type componentType, @NotNull List<AnnotationPredicate> annotationPredicates) {
    this.componentType = componentType;
    this.annotationPredicates = Collections.unmodifiableList(annotationPredicates);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Type componentType() {
    return this.componentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @UnmodifiableView
  public @NotNull Collection<AnnotationPredicate> requiredAnnotations() {
    return this.annotationPredicates;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element requireAnnotation(@NotNull Annotation annotation) {
    Objects.requireNonNull(annotation, "annotation");

    // construct the predicate for the given annotation
    AnnotationPredicate predicate = DefaultAnnotationPredicate.forAnnotation(annotation);

    // register the new predicate in a copy of the current predicates
    List<AnnotationPredicate> annotationPredicates = new LinkedList<>(this.annotationPredicates);
    annotationPredicates.add(predicate);

    // construct and return a new element
    return new DefaultElement(this.componentType, annotationPredicates);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element requireAnnotation(@NotNull Class<? extends Annotation> annotationType) {
    return this.requireAnnotation(annotationType, Collections.emptyMap());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Element requireAnnotation(
    @NotNull Class<? extends Annotation> annotationType,
    @NotNull Map<String, Object> overriddenMethodValues
  ) {
    Objects.requireNonNull(annotationType, "annotationType");
    Objects.requireNonNull(overriddenMethodValues, "overriddenMethodValues");

    // construct and require the annotation
    Annotation proxied = AnnotationFactory.generateAnnotation(annotationType, overriddenMethodValues);
    return this.requireAnnotation(proxied);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasSpecialRequirements() {
    return !this.annotationPredicates.isEmpty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String toString() {
    return ToStringHelper.create(this)
      .putField("componentType", this.componentType)
      .putCollection("requiredAnnotations", this.annotationPredicates)
      .toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.componentType, this.annotationPredicates);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@NotNull Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultElement)) {
      return false;
    }

    // compare the common fields - type & name
    DefaultElement that = (DefaultElement) o;
    if (!this.componentType.equals(that.componentType)) {
      return false;
    }

    // compare the annotation strategies
    // save an iterator if they are either both empty
    if (this.annotationPredicates.isEmpty() && that.annotationPredicates.isEmpty()) {
      return true;
    }
    // or not the same size
    if (this.annotationPredicates.size() != that.annotationPredicates.size()) {
      return false;
    }
    // check every comparer
    for (int i = 0; i < this.annotationPredicates.size(); i++) {
      if (!this.annotationPredicates.get(i).equals(that.annotationPredicates.get(i))) {
        return false;
      }
    }
    // every annotation comparer equals as well
    return true;
  }
}
