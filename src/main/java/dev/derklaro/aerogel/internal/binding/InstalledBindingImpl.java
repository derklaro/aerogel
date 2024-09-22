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

package dev.derklaro.aerogel.internal.binding;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.ScopeApplier;
import dev.derklaro.aerogel.binding.BindingOptions;
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

final class InstalledBindingImpl<T> implements InstalledBinding<T> {

  private final Injector injector;
  private final UninstalledBinding<T> source;

  private final ProviderWithContext<T> providerWithContext;

  public InstalledBindingImpl(
    @NotNull Injector injector,
    @NotNull UninstalledBinding<T> source,
    @NotNull ProviderWithContext<T> providerWithContext
  ) {
    this.source = source;
    this.injector = injector;
    this.providerWithContext = providerWithContext;
  }

  @Override
  public @NotNull BindingKey<? extends T> mainKey() {
    return this.source.mainKey();
  }

  @Override
  public @NotNull @Unmodifiable List<BindingKey<? extends T>> keys() {
    return this.source.keys();
  }

  @Override
  public boolean supportsKey(@NotNull BindingKey<?> key) {
    return this.source.supportsKey(key);
  }

  @Override
  public @NotNull Injector installedInjector() {
    return this.injector;
  }

  @Override
  public @NotNull BindingOptions options() {
    return this.source.options();
  }

  @Override
  public @NotNull Optional<ScopeApplier> scope() {
    return this.source.scope();
  }

  @Override
  public @NotNull ProviderWithContext<T> providerWithContext() {
    return this.providerWithContext;
  }

  @Override
  public @NotNull UninstalledBinding<T> asUninstalled() {
    return this.source;
  }
}
