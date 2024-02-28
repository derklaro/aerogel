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

import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A builder for a dynamic binding, that provides some options to match target elements. Each filter option that is
 * requested on this builder is chained using a logical AND operation. Note that only filter requests of fields and
 * parameters are made to the root builder directly by default.
 * <p>
 * An example to filter a field element with the type {@code PaymentType} and the qualifier annotation {@code @Cash}:
 * <pre>
 * {@code
 * builder
 *   .exactRawType(PaymentType.class)
 *   .annotationPresent(Cash.class)
 * }
 * </pre>
 * Deeper filtering options are also possible:
 * <pre>
 * {@code
 * builder
 *   .matchType(type -> type instanceof Class clazz && clazz == PaymentType.class)
 *   .matchAnnotation(Cash.class, anno -> anno.cashType() == CashType.NOTE)
 * }
 * </pre>
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface DynamicBindingBuilder {

  /**
   * Ensures that the given annotation is present on the element that is targeted by this builder. The given annotation
   * type must be a qualifier annotation and retained at runtime. Use {@link #matchAnnotation(Class, Predicate)} to
   * filter based on presence and properties of the annotation.
   *
   * @param annotationType the annotation type to check for.
   * @return this builder, for chaining.
   * @throws IllegalArgumentException if the given annotation is not a qualifier annotation or not retained at runtime.
   * @see #matchAnnotation(Class, Predicate)
   */
  @NotNull
  DynamicBindingBuilder annotationPresent(@NotNull Class<? extends Annotation> annotationType);

  /**
   * Ensures that the qualifier annotation present on the element that should be injected matches the given filter. This
   * method also adds the requirement for a qualifier annotation to be present. Note: the given filter is not called if
   * the annotation present on the element is a marker annotation. In these cases no annotation instance is retained,
   * and therefore the filter will never pass. Use {@link #annotationPresent(Class)} for marker annotation instead.
   *
   * @param filter the filter for the required qualifier annotation on the target element.
   * @return this builder, for chaining.
   */
  @NotNull
  DynamicBindingBuilder matchAnnotation(@NotNull Predicate<? extends Annotation> filter);

  /**
   * Ensures that a qualifier annotation with the given type is present on the element that is targeted by this builder,
   * and that the annotation instance matches the given filter predicate. The given annotation type must be a qualifier
   * annotation and retained at runtime. For marker annotations use {@link #annotationPresent(Class)} instead.
   *
   * @param annotationType the type of the annotation to filter.
   * @param filter         the filter to apply to the annotation.
   * @param <A>            the type of the annotation.
   * @return this builder, for chaining.
   * @throws IllegalArgumentException if the given annotation is not a qualifier annotation or not retained at runtime.
   * @see #annotationPresent(Class)
   */
  @NotNull
  <A extends Annotation> DynamicBindingBuilder matchAnnotation(
    @NotNull Class<A> annotationType,
    @NotNull Predicate<A> filter);

  /**
   * Ensures that the raw type of the element that is targeted by this builder is exactly the given class. The
   * comparison is executed using {@code equals()}.
   *
   * @param type the exact raw type that the element to inject must have.
   * @return this builder, for chaining.
   */
  @NotNull
  DynamicBindingBuilder exactRawType(@NotNull Class<?> type);

  /**
   * Ensures that the given type is a super or the same type of the raw type of the element that is targeted by this
   * builder. This check can be expressed as: {@code elementType instanceof givenType}.
   *
   * @param type the type to check for being a super or the same class of the injectable element.
   * @return this builder, for chaining.
   */
  @NotNull
  DynamicBindingBuilder superRawType(@NotNull Class<?> type);

  /**
   * Ensures that the element that is targeted by this builder has a raw type that matches the given filter.
   *
   * @param filter the filter for the raw type of the element that is targeted by this builder.
   * @return this builder, for chaining.
   */
  @NotNull
  DynamicBindingBuilder matchRawType(@NotNull Predicate<Class<?>> filter);

  /**
   * Ensures that the element that is targeted by this builder has a type that matches the given filter.
   *
   * @param filter the filter for the type of the element that is targeted by this builder.
   * @return this builder, for chaining.
   */
  @NotNull
  DynamicBindingBuilder matchType(@NotNull Predicate<Type> filter);

  /**
   * Constructs a new dynamic binding that checks if all applied filters are met. If this is the case the given binding
   * will be called to construct the binding to return from the invocation. This method can only be called after at
   * least one filter was set in the builder. Each invocation will create a new binding instance. Modifications to this
   * builder after invoking the method will not reflect into the returned binding.
   *
   * @param binding the binding callback to invoke to construct a binding in case the provided filters are matching.
   * @return a new dynamic binding that calls the given binding if the provided filters are matching.
   * @throws IllegalStateException if no filters were applied to this builder yet.
   */
  @NotNull
  DynamicBinding toBinding(@NotNull DynamicBinding binding);

  /**
   * Constructs a new dynamic binding that check if all applied filters are met. If this is the case the given function
   * will be called to construct the binding to return from the invocation. The scopeable builder provided as the
   * function input has access to the same configuration as obtainable from the root binding builder that provided this
   * builder. The providing function can return null to indicate that no binding should be constructed, even if the
   * given filters match. This method can only be called after at least one filter was set in the builder. Each
   * invocation will create a new binding instance. Modifications to this builder after invoking the method will not
   * reflect into the returned binding.
   *
   * @param bindingProvider the binding factory to invoke in case the provided filters are matching.
   * @return a new dynamic binding that calls the given binding factory if the provided filters are matching.
   * @throws IllegalStateException if no filters were applied to this builder yet.
   */
  @NotNull
  DynamicBinding toKeyedBindingProvider(
    @NotNull Function<ScopeableBindingBuilder<?>, UninstalledBinding<?>> bindingProvider);

  /**
   * Constructs a new dynamic binding that check if all applied filters are met. If this is the case the given function
   * will be called to construct the binding to return from the invocation. The root binding builder provided as the
   * function input is the same root binding builder that was used to construct this binding builder. The providing
   * function can return null to indicate that no binding should be constructed, even if the given filters match. This
   * method can only be called after at least one filter was set in the builder. Each invocation will create a new
   * binding instance. Modifications to this builder after invoking the method will not reflect into the returned
   * binding.
   *
   * @param bindingProvider the binding factory to invoke in case the provided filters are matching.
   * @return a new dynamic binding that calls the given binding factory if the provided filters are matching.
   * @throws IllegalStateException if no filters were applied to this builder yet.
   */
  @NotNull
  DynamicBinding toBindingProvider(@NotNull Function<RootBindingBuilder, UninstalledBinding<?>> bindingProvider);
}
