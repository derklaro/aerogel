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
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A builder for type-safe, future-proof qualifier annotation implementations. The return value of each property method
 * can be defined by calling the {@link #property(Function)} method, and invoking the target property method in lambda
 * form:
 * <pre>
 * {@code .property(Named::name)}
 * </pre>
 * In the returned builder step, the return value can be provided based on the type of the invoked method.
 * <p>
 * The builder instance can be obtained via {@link QualifiableBindingBuilder#buildQualifier(Class)}.
 *
 * @param <A> the type of the annotation to build.
 * @param <T> the binding value type constructed by the parent binding builder.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface BindingAnnotationBuilder<A extends Annotation, T> {

  /**
   * Starts the bind process of a single property on the target annotation type. The following properties cannot be
   * bound by this builder method:
   * <ul>
   *   <li>{@code toString()}, {@code equals()} and {@code hashCode()}
   *   <li>{@code annotationType()}
   * </ul>
   * <p>
   * The return value and other options can be defined in the returned second builder step, based on the return type of
   * the annotation property method invocation.
   * <p>
   * Example for defining the {@code name()} method on an annotation called {@code Named}:
   * <pre>
   * {@code .property(Named::name)}
   * </pre>
   *
   * @param accessor the function being invoked to retrieve the property method to define.
   * @param <R>      the return type of the annotation method invocation.
   * @return a second step builder for defining options and the return value of the property method invocation.
   * @throws IllegalStateException    if no method was invoked after the given function call.
   * @throws IllegalArgumentException if an invalid or already bound method was invoked after the given function call.
   */
  @NotNull
  <R> DefineReturn<A, T, R> property(@NotNull Function<A, R> accessor);

  /**
   * Finalizes the build and requires the qualifier annotation constructed by this builder in the parent binding
   * builder. A call to this method will verify that all <strong>required</strong> property methods have a return value
   * defined. If a defaulted property method does not have a value defined at the time calling this method, the default
   * value of the method will be used instead.
   *
   * @return the original binding builder with the qualifier annotation constructed by this builder required.
   */
  @NotNull
  ScopeableBindingBuilder<T> require();

  /**
   * The second step of an annotation property build process. The return value and other options can be defined in this
   * step. When the return value is supplied to any of the corresponding methods, the original annotation builder is
   * returned and further properties can be defined.
   * <p>
   * An example for the {@code Named} annotation:
   * <pre>
   * {@code
   * public @interface Named {
   *   String name();
   *   int order() default 5;
   * }
   * }
   * </pre>
   * can be built using the following code snippet:
   * <pre>
   * {@code
   * builder
   *   .property(Named::name).returns("Hello World")
   *   .property(Named::order).orDefault().returnLazySupply(() -> someComputationOrNull())}
   * </pre>
   * This code snippet binds the {@code Named.name()} property method to the value {@code Hello World} and the
   * {@code Named.order()} to a lazy supplier computation. If the computation throws an exception or returns null, no
   * error is thrown but the default value of the property method is returned instead.
   *
   * @param <A> the type of the annotation to build.
   * @param <T> the binding value type constructed by the parent binding builder.
   * @param <R> the return type of the annotation method invocation.
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  @API(status = API.Status.STABLE, since = "3.0")
  interface DefineReturn<A extends Annotation, T, R> {

    /**
     * Instructs the builder to return the default value of an annotation property method if the provided return value
     * evaluates to null or the computation throws an exception. If not set an exception is thrown in these cases.
     * <p>
     * Note: this method can only be called if the annotation property method has a default value. If no default value
     * is known, an exception is thrown upon invocation.
     *
     * @return this builder, for chaining.
     * @throws IllegalStateException if the target annotation property method has no default value present.
     */
    @NotNull
    DefineReturn<A, T, R> orDefault();

    /**
     * Defines the return value of the target property method. Can only be null if the target property method has a
     * default value and {@link #orDefault()} was called prior to the invocation.
     *
     * @param returnValue the value to return from the method invocation.
     * @param <X>         the type of the return value.
     * @return the source annotation builder to chain multiple property calls.
     * @throws IllegalArgumentException if the given return value is null and the defaulting setting is not activated.
     * @see #orDefault()
     */
    @NotNull
    <X extends R> BindingAnnotationBuilder<A, T> returns(@Nullable X returnValue);

    /**
     * Defines the return value of the target property method to be a lazy computation using the given callable. The
     * return value is calculated on first property access, and then memoized, meaning that the given callable will only
     * be executed once. The callable can only return null or throw an exception if the target property method has a
     * default value and {@link #orDefault()} was called prior to the invocation. In all other cases an exception is
     * propagated in these cases.
     *
     * @param returnValueProvider the function to call to get the return value of the property method.
     * @param <X>                 the type of the return value.
     * @return the source annotation builder to chain multiple property calls.
     * @see #orDefault()
     */
    @NotNull
    <X extends R> BindingAnnotationBuilder<A, T> returnLazyCall(@NotNull Callable<X> returnValueProvider);

    /**
     * Defines the return value of the target property method to be a lazy computation using the given supplier. The
     * return value is calculated on first property access, and then memoized, meaning that the given supplier will only
     * be executed once. The supplier can only return null or throw an exception if the target property method has a
     * default value and {@link #orDefault()} was called prior to the invocation. In all other cases an exception is
     * propagated in these cases.
     *
     * @param returnValueProvider the function to call to get the return value of the property method.
     * @param <X>                 the type of the return value.
     * @return the source annotation builder to chain multiple property calls.
     * @see #orDefault()
     */
    @NotNull
    <X extends R> BindingAnnotationBuilder<A, T> returnLazySupply(@NotNull Supplier<X> returnValueProvider);
  }
}
