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

package dev.derklaro.aerogel.internal.provider;

import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import jakarta.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CascadingProviderFactory<T> implements ProviderFactory<T> {

  private final BindingKey<? extends T> cascadedTo;

  private CascadingProviderFactory(@NotNull BindingKey<? extends T> cascadedTo) {
    this.cascadedTo = cascadedTo;
  }

  public static @NotNull <T> CascadingProviderFactory<T> cascadeTo(@NotNull BindingKey<? extends T> cascadedTo) {
    return new CascadingProviderFactory<>(cascadedTo);
  }

  @Override
  public @NotNull ProviderWithContext<T> constructProvider() {
    return new CascadedProvider<>(this.cascadedTo);
  }

  private static final class CascadedProvider<T> implements ProviderWithContext<T> {

    private final BindingKey<? extends T> cascadedTo;

    public CascadedProvider(@NotNull BindingKey<? extends T> cascadedTo) {
      this.cascadedTo = cascadedTo;
    }

    @Override
    public @Nullable T get(@NotNull InjectionContext context) {
      Provider<? extends T> provider = context.injector().provider(this.cascadedTo);
      return provider.get();
    }

    @Override
    public @NotNull String toString() {
      return "Cascaded(" + this.cascadedTo + ")";
    }
  }
}
