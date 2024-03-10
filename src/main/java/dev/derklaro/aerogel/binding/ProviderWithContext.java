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

package dev.derklaro.aerogel.binding;

import dev.derklaro.aerogel.internal.context.InjectionContext;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides instances of {@code T} while also taking the current injection context into account. Mainly used for
 * internal instance construction, external users should use {@link jakarta.inject.Provider}s from the target binding
 * instead. Note: unlike providers, this type cannot be injected.
 *
 * @param <T> the type being provided by this provider.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@FunctionalInterface
@API(status = API.Status.MAINTAINED, since = "3.0")
public interface ProviderWithContext<T> {

  /**
   * Provides a fully constructed value of {@code T} with respect to the given injection context.
   *
   * @param context the current injection context.
   * @return a fully constructed value of {@code T}.
   */
  @Nullable
  T get(@NotNull InjectionContext context);
}
