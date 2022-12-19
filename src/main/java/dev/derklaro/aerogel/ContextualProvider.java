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

import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a provider which can get the underlying instance, using the given injection context when called in a
 * contextual manner.
 *
 * <p>In difference to a normal provider, a contextual provider is bound to a specific injector.
 *
 * @param <T> the type returned from this provider.
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public interface ContextualProvider<T> extends Provider<T> {

  /**
   * Get the underlying injector of this provider.
   *
   * @return the underlying injector of this provider.
   */
  @NotNull Injector injector();

  /**
   * Indicates to this context that it got wrapped in a downstream context. The method is free to either do nothing,
   * change the internal state or return a new provider which should be used by the downstream provider to call this
   * provider.
   *
   * @return this context wrapped for downstream use.
   */
  @NotNull ContextualProvider<T> asUpstreamContext();

  /**
   * Provides a full constructed instance of the {@code T} type. This method delegates every {@link Throwable} caused
   * during the object instantiation to the caller.
   *
   * @param context the context to use for instance lookups.
   * @return the constructed instance of this provider, might be null.
   * @throws NullPointerException if the given context is null.
   * @throws AerogelException     if any exception occurs during the construction of the underlying type.
   */
  @Nullable T get(@NotNull InjectionContext context) throws AerogelException;
}
