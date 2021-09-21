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

import aerogel.internal.ImmediateProvider;
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
public interface Provider<T> {

  /**
   * Get an immediate provider of the given element - the {@link #get()} method will always return the provided
   * element.
   *
   * @param provided the element which should be returned by a call to {@link #get()}.
   * @param <C>      the type of the {@code provided} argument.
   * @return a provider returning {@code provided} for every {@link #get()} call.
   */
  static @NotNull <C> Provider<C> of(@Nullable C provided) {
    return new ImmediateProvider<>(provided);
  }

  /**
   * Provides a full constructed instance of the {@code T} type. This method delegates every {@link Throwable} caused
   * during the object instantiation to the caller.
   *
   * @return a fully constructed instance of the type {@code T}.
   * @throws AerogelException if any exception occurs during the construction of the underlying type.
   */
  @Nullable T get() throws AerogelException;
}
