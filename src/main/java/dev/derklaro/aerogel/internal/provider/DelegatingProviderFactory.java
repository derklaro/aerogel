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
import dev.derklaro.aerogel.internal.context.InjectionContext;
import jakarta.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DelegatingProviderFactory<T> implements ProviderFactory<T> {

  private final Provider<? extends T> delegate;

  private DelegatingProviderFactory(@NotNull Provider<? extends T> delegate) {
    this.delegate = delegate;
  }

  public static @NotNull <T> DelegatingProviderFactory<T> toProvider(@NotNull Provider<? extends T> delegate) {
    return new DelegatingProviderFactory<>(delegate);
  }

  @Override
  public @NotNull ProviderWithContext<T> constructProvider() {
    return new DelegatingProvider<>(this.delegate);
  }

  private static final class DelegatingProvider<T> implements ProviderWithContext<T> {

    private final Provider<? extends T> delegate;

    public DelegatingProvider(@NotNull Provider<? extends T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public @Nullable T get(@NotNull InjectionContext context) {
      return this.delegate.get();
    }

    @Override
    public @NotNull String toString() {
      return "Delegating(" + this.delegate + ")";
    }
  }
}
