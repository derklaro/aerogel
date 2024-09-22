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
import dev.derklaro.aerogel.internal.provider.ProviderFactory;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public final class UninstalledBindingImpl<T> implements UninstalledBinding<T> {

  private final List<BindingKey<? extends T>> bindingKeys;
  private final ScopeApplier scopeApplier;
  private final BindingOptions bindingOptions;

  private final ProviderFactory<T> providerFactory;

  public UninstalledBindingImpl(
    @NotNull List<BindingKey<? extends T>> bindingKeys,
    @Nullable ScopeApplier scopeApplier,
    @NotNull BindingOptions bindingOptions,
    @NotNull ProviderFactory<T> providerFactory
  ) {
    this.bindingKeys = bindingKeys;
    this.scopeApplier = scopeApplier;
    this.bindingOptions = bindingOptions;
    this.providerFactory = providerFactory;
  }

  @Override
  public @NotNull BindingKey<? extends T> mainKey() {
    return this.bindingKeys.get(0);
  }

  @Override
  public @NotNull @Unmodifiable List<BindingKey<? extends T>> keys() {
    return this.bindingKeys;
  }

  @Override
  public boolean supportsKey(@NotNull BindingKey<?> key) {
    return this.bindingKeys.contains(key);
  }

  @Override
  public @NotNull BindingOptions options() {
    return this.bindingOptions;
  }

  @Override
  public @NotNull Optional<ScopeApplier> scope() {
    return Optional.ofNullable(this.scopeApplier);
  }

  @Override
  public @NotNull UninstalledBinding<T> withScope(@Nullable ScopeApplier scope) {
    return new UninstalledBindingImpl<>(this.bindingKeys, scope, this.bindingOptions, this.providerFactory);
  }

  @Override
  public @NotNull InstalledBinding<T> prepareForInstallation(@NotNull Injector injector) {
    ProviderWithContext<T> provider = this.providerFactory.constructProvider();
    if (this.scopeApplier != null) {
      provider = this.scopeApplier.applyScope(this.bindingKeys, provider);
    }

    return new InstalledBindingImpl<>(injector, this, provider);
  }
}
