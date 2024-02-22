/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2024 Pasqual K. and contributors
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

package dev.derklaro.aerogel.binding.builder;

import java.lang.annotation.Annotation;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A builder steps that allows to define a qualifier annotation that is required on target elements. This step returns a
 * new builder step that allows for scoping, as only a single qualifier is allowed per binding. Qualifier annotations
 * can be marker annotations or have properties, in which case the property values of the target injectable element must
 * match as well. If no annotation implementation is available but properties should be required, an annotation instance
 * can be built using {@link #buildQualifier(Class)}.
 * <p>
 * Example usage to apply a qualifier to the builder:
 * <pre>
 * {@code
 * root
 *   .bind(PaymentService.class)
 *   .qualifiedWith(Paypal.class)
 *   // continue with scoping and targeting
 * }
 * </pre>
 * Or by applying a custom annotation with properties:
 * <pre>
 * {@code
 * root
 *   .bind(PaymentService.class)
 *   .buildQualifier(PaymentServiceType.class)
 *   .property(PaymentServiceType::value)
 *   .returns(ServiceType.PAYPAL)
 *   .require()
 *   // continue with scoping and targeting
 * }
 * </pre>
 *
 * @param <T> the type of values handled by this binding that is being built.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface QualifiableBindingBuilder<T> extends ScopeableBindingBuilder<T> {

  /**
   * Requires the binding that is being build to be annotated with the {@link jakarta.inject.Named} qualifier annotation
   * and the name to match the given name.
   *
   * @param name the value to require from the named qualifier annotation.
   * @return the next binding step that allows for scoping the target binding.
   * @see jakarta.inject.Named
   */
  @NotNull
  ScopeableBindingBuilder<T> qualifiedWithName(@NotNull String name);

  /**
   * Requires the given annotation instance in this builder. If the underlying annotation type has no properties (marker
   * annotation), only the annotation type will be required. If properties are present, the properties must also match
   * on the element that is being injected.
   *
   * @param qualifierAnnotation the qualifier annotation instance to require.
   * @return the next binding step that allows for scoping the target binding.
   * @throws IllegalArgumentException if the annotation type is not retained at runtime or is not a qualifier.
   * @see #qualifiedWith(Class)
   */
  @NotNull
  ScopeableBindingBuilder<T> qualifiedWith(@NotNull Annotation qualifierAnnotation);

  /**
   * Requires the given annotation type in this builder. If the given annotation type has properties that are not
   * defaulted, this method cannot be used to require the raw annotation type. The values pf the property methods must
   * be present. This is due to the fact that qualifier annotation properties are part of the binding hash, meaning that
   * without properties the injectable elements with properties would never match the built binding.
   *
   * @param qualifierAnnotationType the annotation type to require.
   * @return the next binding step that allows for scoping the target binding.
   * @throws IllegalArgumentException if the given annotation type has non-defaulted properties, is not retained at
   *                                  runtime or is not a qualifier annotation.
   * @see #qualifiedWith(Annotation)
   */
  @NotNull
  ScopeableBindingBuilder<T> qualifiedWith(@NotNull Class<? extends Annotation> qualifierAnnotationType);

  /**
   * Starts the build process for a non-marker qualifier annotation if no implementation is available. Upon finishing
   * the qualifier build, the scoping and targeting process can be continued as usual.
   *
   * @param qualifierAnnotationType the annotation type to start the building process for.
   * @param <A>                     the annotation type.
   * @return a new annotation builder for the given annotation type.
   * @throws IllegalArgumentException if the given annotation type is a marker annotation, is not retained at runtime or
   *                                  is not a qualifier annotation.
   */
  @NotNull
  <A extends Annotation> BindingAnnotationBuilder<A, T> buildQualifier(@NotNull Class<A> qualifierAnnotationType);
}
