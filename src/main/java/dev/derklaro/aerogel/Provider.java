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

import dev.derklaro.aerogel.internal.ImmediateProvider;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a provider as declared by {@link jakarta.inject.Provider}. Any type which is injectable through a normal
 * {@link Injector} instance can be injected wrapped into a {@link Provider} or {@link jakarta.inject.Provider}. Every
 * {@link BindingHolder} represents a {@link Provider} because it supports (as any {@link Provider} should)...
 *
 * <ul>
 *   <li>... creating any instance lazily instead of directly.</li>
 *   <li>... preventing circular dependencies by creating a lazy instance on non-proxyable types.</li>
 *   <li>... creating multiple instances of the type independently (as long as the type is not a singleton).</li>
 * </ul>
 *
 * <p>You can (as described) do this:
 *
 * <pre>
 *   public class Company {
 *     &#064;Inject
 *     public Company(Provider&lt;EmployeeDatabase&gt; database) {
 *        EmployeeDatabase databaseA = database.get();
 *        EmployeeDatabase databaseB = database.get();
 *     }
 *   }
 * </pre>
 *
 * <p>instead of:
 *
 * <pre>
 *   public class Company {
 *     &#064;Inject
 *     public Company(EmployeeDatabase databaseA, EmployeeDatabase databaseB) {
 *
 *     }
 *   }
 * </pre>
 *
 * @param <T> represents the actual type which should get injected.
 * @author Pasqual K.
 * @since 1.0
 */
@FunctionalInterface
public interface Provider<T> extends Supplier<T> {

  /**
   * Get an immediate provider of the given element - the {@link #get()} method will always return the provided
   * element.
   *
   * @param provided the element which should be returned by a call to {@link #get()}.
   * @param <C>      the type of the {@code provided} argument.
   * @return a provider returning {@code provided} for every {@link #get()} call.
   */
  static @NotNull <C> Provider<C> immediate(@Nullable C provided) {
    return new ImmediateProvider<>(provided);
  }

  /**
   * Provides a full constructed instance of the {@code T} type. This method delegates every {@link Throwable} caused
   * during the object instantiation to the caller.
   *
   * @return a fully constructed instance of the type {@code T}.
   * @throws AerogelException if any exception occurs during the construction of the underlying type.
   */
  @Override
  @Nullable T get() throws AerogelException;

  /**
   * Tries to get the value of this provider, returns null if getting the value results in an exception.
   *
   * @return the value of this provider or null if getting the value results in an exception.
   * @see #getOrElse(Object)
   * @since 1.2.0
   */
  default @Nullable T getOrNull() {
    return this.getOrElse(null);
  }

  /**
   * Tries to get the value of this provider, return {@code defaultValue} if getting the value results in an exception.
   *
   * @param defaultValue the default value to get if this provider cannot provide a value.
   * @return the value provided by this provider or {@code defaultValue} if this provider is unable to provide a value.
   * @since 1.2.0
   */
  default T getOrElse(@Nullable T defaultValue) {
    try {
      return this.get();
    } catch (AerogelException exception) {
      return defaultValue;
    }
  }

  /**
   * Get the value of this provider into an optional. The optional is only present if a value of this provider is
   * present and not null.
   *
   * @return an optional wrapping the current value of this provider.
   * @see #getOrNull()
   * @since 1.2.0
   */
  default @NotNull Optional<T> getAsOptional() {
    return Optional.ofNullable(this.getOrNull());
  }

  /**
   * Returns a new {@link Provider} whose value is the value of this provider or the given {@code defaultValue} if not
   * present. The resulting provider is synced to this provider meaning that every time get is called on the created
   * provider it queries the value of this provider and determines if the value of this or the given
   * {@code defaultValue} should be used.
   *
   * @param defaultValue the default value to return if the current provider has no value.
   * @return the new provider.
   * @since 1.2.0
   */
  default @NotNull Provider<T> orElse(@Nullable T defaultValue) {
    return () -> this.getOrElse(defaultValue);
  }

  /**
   * Returns a new {@link Provider} whose value is the value of this provider or the value of the given {@code fallback}
   * provider if not present. The resulting provider is synced to this provider meaning that every time get is called on
   * the created provider it queries the value of this provider and determines if the value of this or the given
   * {@code fallback} provider should be used.
   *
   * @param fallback the provider to query the value from if the current provider has no value.
   * @return the new provider.
   * @since 1.2.0
   */
  default @NotNull Provider<T> orElse(@NotNull Provider<? extends T> fallback) {
    return () -> {
      T currentValue = this.getOrNull();
      return currentValue == null ? fallback.getOrNull() : currentValue;
    };
  }

  /**
   * Returns a new {@link Provider} whose value is the value of this provider or the value of the given {@code supplier}
   * if not present. The resulting provider is synced to this provider meaning that every time get is called on the
   * created provider it queries the value of this provider and determines if the value of this or the result of the
   * given {@code supplier} should be used.
   *
   * @param supplier the supplier that produces a value in case this provider has no value.
   * @return the new provider.
   * @since 1.2.0
   */
  default @NotNull Provider<T> orElseGet(@NotNull Supplier<? extends T> supplier) {
    return () -> {
      T currentValue = this.getOrNull();
      return currentValue == null ? supplier.get() : currentValue;
    };
  }

  /**
   * Returns a new {@link Provider} whose value is the value of this provider applied to given {@code mapper}. The
   * resulting provider is synced to this provider meaning that every time get is called on the created provider it
   * queries the value of this provider and applies it to the given {@code mapper}. If the value of the current (this)
   * provider is null the mapper function will not be called and the new provider will return {@code null}.
   *
   * @param mapper the mapper this provider's value. May return null in which case the new provider has no value.
   * @param <R>    the element type of the new provider.
   * @return the new provider.
   * @since 1.2.0
   */
  default @NotNull <R> Provider<R> map(@NotNull Function<? super T, ? extends R> mapper) {
    return () -> {
      T currentValue = this.getOrNull();
      return currentValue == null ? null : mapper.apply(currentValue);
    };
  }

  /**
   * Returns a new {@link Provider} whose value is the value of this provider applied to the given {@code mapper}. On
   * the mapped provider the function {@link #getOrNull()} will be called to determine the result. The resulting
   * provider is synced to this provider meaning that every time get is called on the created provider it queries the
   * value of this provider and applies it to the given {@code mapper}. If the value of the current (this) provider is
   * null the mapper function will not be called and the new provider will return {@code null}. If the mapper function
   * returns {@code null} when computing the provider the new provider will return {@code null}.
   *
   * @param mapper the flat mapper this provider's value. May return null in which case the new provider has no value.
   * @param <R>    the element type of the new provider.
   * @return the new provider.
   * @since 1.2.0
   */
  default @NotNull <R> Provider<R> flatMap(@NotNull Function<? super T, ? extends Provider<? extends R>> mapper) {
    return () -> {
      T currentValue = this.getOrNull();
      // no current value - no need for the mapper lookup
      if (currentValue == null) {
        return null;
      }
      // compute a provider from the function and get the return value of it if the computed provider is present
      Provider<? extends R> provider = mapper.apply(currentValue);
      return provider == null ? null : provider.getOrNull();
    };
  }

  /**
   * Returns a new {@link Provider} which value will be combined result of this and the given {@code second} provider.
   * The resulting provider is synced to this provider meaning that every time get is called on the created provider it
   * queries the value of this provider, the value of the {@code second} provider and applies it to the given
   * {@code combiner}. If the value of the current (this) or the {@code second} provider is null the mapper combiner
   * will not be called and the new provider will return {@code null}.
   *
   * @param second   the provider to combine this provider with.
   * @param combiner the combiner for this and the second provider's values.
   * @param <R>      the element type of the second provider.
   * @param <S>      the element type of the new provider.
   * @return the new provider.
   * @since 1.2.0
   */
  default @NotNull <R, S> Provider<S> combine(@NotNull Provider<R> second, @NotNull BiFunction<T, R, S> combiner) {
    return () -> {
      // query the current provider's value
      T currentValue = this.getOrNull();
      if (currentValue == null) {
        return null;
      }
      // query the second provider's value
      R secondValue = second.getOrNull();
      // combine both results if the second result is present
      return secondValue == null ? null : combiner.apply(currentValue, secondValue);
    };
  }
}
