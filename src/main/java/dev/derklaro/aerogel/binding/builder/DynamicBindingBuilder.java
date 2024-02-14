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

import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.dynamic.DynamicBinding;
import dev.derklaro.aerogel.binding.dynamic.InjectableElement;
import dev.derklaro.aerogel.binding.dynamic.InjectableElementType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A builder for a dynamic binding, that provides some options to match target elements. The matching process can be
 * customized by providing custom filters to {@link #matchElement(Predicate)}. Each filter option that is requested on
 * this builder is chained using a logical AND operation. Note that only filter requests of fields and parameters are
 * made to the root builder directly by default, all other element types can only be parents of these.
 * <p>
 * An example to filter a field element with the type {@code PaymentType} and the qualifier annotation {@code @Cash}:
 * <pre>
 * {@code
 * builder
 *   .matchElementType(StandardInjectableElementType.FIELD)
 *   .exactRawType(PaymentType.class)
 *   .annotationPresent(Cash.class)
 * }
 * </pre>
 * Deeper filtering options are also possible:
 * <pre>
 * {@code
 * builder
 *   .matchElementType(StandardInjectableElementType.FIELD)
 *   .matchType(type -> type instanceof Class clazz && clazz == PaymentType.class)
 *   .matchAnnotation(Cash.class, anno -> anno.cashType() == CashType.NOTE)
 * }
 * </pre>
 * And fully customized filtering, even chained:
 * <pre>
 * {@code
 * builder
 *   .matchElement(element -> element.rawType() == PaymentType.class)
 *   .matchElement(element -> element.annotation(Cash.class).isPresent())
 * }
 * </pre>
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface DynamicBindingBuilder<B extends DynamicBindingBuilder<B>> {

  /**
   * Ensures that the type of element that is targeted by this builder has the given type. Note that only fields and
   * parameters are passed to root-level injection requests by default. All other element types can only be parents of
   * these.
   *
   * @param elementType the type of element that must be requested for injection.
   * @return this builder, for chaining.
   * @see dev.derklaro.aerogel.binding.dynamic.StandardInjectableElementType
   */
  @NotNull
  B matchElementType(@NotNull InjectableElementType elementType);

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
  B annotationPresent(@NotNull Class<? extends Annotation> annotationType);

  /**
   * Ensures that at least one annotation that matches the given filter is present on the element that should be
   * injected. Every annotation can be matched by this filter, however, it is recommended to only check for qualifier
   * annotations using this method.
   *
   * @param filter the filter that at least one annotation on the target element must match.
   * @return this builder, for chaining.
   */
  @NotNull
  B matchAnnotation(@NotNull Predicate<? extends Annotation> filter);

  /**
   * Ensures that an annotation with the given type is present on the element that is targeted by this builder, and that
   * the annotation instance matches the given filter predicate. The given annotation type must be a qualifier
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
  <A extends Annotation> B matchAnnotation(
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
  B exactRawType(@NotNull Class<?> type);

  /**
   * Ensures that the given type is a super or the same type of the raw type of the element that is targeted by this
   * builder. This check can be expressed as: {@code elementType instanceof givenType}.
   *
   * @param type the type to check for being a super or the same class of the injectable element.
   * @return this builder, for chaining.
   */
  @NotNull
  B superRawType(@NotNull Class<?> type);

  /**
   * Ensures that the element that is targeted by this builder has a raw type that matches the given filter.
   *
   * @param filter the filter for the raw type of the element that is targeted by this builder.
   * @return this builder, for chaining.
   */
  @NotNull
  B matchRawType(@NotNull Predicate<Class<?>> filter);

  /**
   * Ensures that the element that is targeted by this builder has a type that matches the given filter.
   *
   * @param filter the filter for the type of the element that is targeted by this builder.
   * @return this builder, for chaining.
   */
  @NotNull
  B matchType(@NotNull Predicate<Type> filter);

  /**
   * Ensures that the element that is targeted by this builder has the exact name provided to the function. The name
   * checking is done case-sensitive.
   *
   * @param name the exact name that the element that is targeted by this builder must have.
   * @return this builder, for chaining.
   */
  @NotNull
  B matchName(@NotNull String name);

  /**
   * Ensures that the element that is targeted by this builder has a name that matches the given pattern.
   *
   * @param namePattern the pattern that the name of the element targeted by this builder must match.
   * @return this builder, for chaining.
   */
  @NotNull
  B matchName(@NotNull Pattern namePattern);

  /**
   * Ensures that the name of the element targeted by this builder matches the given filter.
   *
   * @param filter the filter to apply to the name of the element.
   * @return this builder, for chaining.
   */
  @NotNull
  B matchName(@NotNull Predicate<String> filter);

  /**
   * Appends the given filter with a logical AND operation to the other filters for the element.
   *
   * @param filter the filter to apply to the element.
   * @return this builder, for chaining.
   */
  @NotNull
  B matchElement(@NotNull Predicate<InjectableElement> filter);

  /**
   * Get a new builder to match the parent element of the element targeted by this builder. When the filter from the
   * builder is applied, it verifies that the target element has a parent and that the parent matches the constructed
   * filter.
   *
   * @return a new builder to build a matcher for the parent element of the target element.
   * @see Parent#apply()
   */
  @NotNull
  Parent<B> matchParent();

  /**
   * A dynamic binding builder to match the root element (the direct target) of the injection. This builder can provide
   * the final binding after all filters were applied. Each dynamic binding needs at least one filter to be applied,
   * prior to building. The root binding builder can be re-used after building without making changes to filters in
   * existing bindings.
   *
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  @API(status = API.Status.STABLE, since = "3.0")
  interface Root extends DynamicBindingBuilder<Root> {

    /**
     * Constructs a new dynamic binding that checks if all applied filters are met. If this is the case the given
     * binding will be called to construct the binding to return from the invocation. This method can only be called
     * after at least one filter was set in the builder. Each invocation will create a new binding instance.
     * Modifications to this builder after invoking the method will not reflect into the returned binding.
     *
     * @param binding the binding callback to invoke to construct a binding in case the provided filters are matching.
     * @return a new dynamic binding that calls the given binding if the provided filters are matching.
     * @throws IllegalStateException if no filters were applied to this builder yet.
     */
    @NotNull
    DynamicBinding toBinding(@NotNull DynamicBinding binding);

    /**
     * Constructs a new dynamic binding that check if all applied filters are met. If this is the case the given
     * function will be called to construct the binding to return from the invocation. The scopeable builder provided as
     * the function input has access to the same configuration as obtainable from the root binding builder that provided
     * this builder. The providing function can return null to indicate that no binding should be constructed, even if
     * the given filters match. This method can only be called after at least one filter was set in the builder. Each
     * invocation will create a new binding instance. Modifications to this builder after invoking the method will not
     * reflect into the returned binding.
     *
     * @param bindingProvider the binding factory to invoke in case the provided filters are matching.
     * @return a new dynamic binding that calls the given binding factory if the provided filters are matching.
     * @throws IllegalStateException if no filters were applied to this builder yet.
     */
    @NotNull
    DynamicBinding toBindingProvider(
      @NotNull Function<ScopeableBindingBuilder<?>, UninstalledBinding<?>> bindingProvider);

    /**
     * Constructs a new dynamic binding that check if all applied filters are met. If this is the case the given
     * function will be called to construct the binding to return from the invocation. The injectable element provided
     * as the function input is root element that gets injected. The scopeable builder provided as the function input
     * has access to the same configuration as obtainable from the root binding builder that provided this builder. The
     * providing function can return null to indicate that no binding should be constructed, even if the given filters
     * match. This method can only be called after at least one filter was set in the builder. Each invocation will
     * create a new binding instance. Modifications to this builder after invoking the method will not reflect into the
     * returned binding.
     *
     * @param bindingProvider the binding factory to invoke in case the provided filters are matching.
     * @return a new dynamic binding that calls the given binding factory if the provided filters are matching.
     * @throws IllegalStateException if no filters were applied to this builder yet.
     */
    @NotNull
    DynamicBinding toBindingProvider(
      @NotNull BiFunction<InjectableElement, ScopeableBindingBuilder<?>, UninstalledBinding<?>> bindingProvider);
  }

  /**
   * A builder to apply element matcher options for parent elements. The match options are the same as available for the
   * root element, except that they need to be applied to the child after all options were set.
   *
   * @param <C> the type of the child builder.
   * @author Pasqual Koschmieder
   * @see DynamicBindingBuilder#matchParent()
   * @since 3.0
   */
  @API(status = API.Status.STABLE, since = "3.0")
  interface Parent<C extends DynamicBindingBuilder<C>> extends DynamicBindingBuilder<Parent<C>> {

    /**
     * Applies the filters for the parent element to the child matcher and returns to the child builder. This method
     * does not apply anything to the child builder if no matching methods were called prior to invocation. The method
     * can only be invoked once on a builder instance.
     *
     * @return the child builder after applying the match options of this builder.
     * @throws IllegalStateException if the method is invoked more than once.
     */
    @NotNull
    C apply();
  }
}
