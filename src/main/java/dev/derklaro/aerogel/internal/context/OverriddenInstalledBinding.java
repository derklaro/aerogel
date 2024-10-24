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

package dev.derklaro.aerogel.internal.context;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.ScopeApplier;
import dev.derklaro.aerogel.binding.BindingOptions;
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.binding.BindingOptionsImpl;
import dev.derklaro.aerogel.internal.provider.DelegatingProviderFactory;
import jakarta.inject.Provider;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

final class OverriddenInstalledBinding implements InstalledBinding<Object> {

  private static final BindingOptions EMPTY_BINDING_OPTIONS = new BindingOptionsImpl(null);

  private final Injector injector;
  private final BindingKey<?> targetKey;
  private final ProviderWithContext<?> provider;

  public OverriddenInstalledBinding(
    @NotNull Injector injector,
    @NotNull BindingKey<?> targetKey,
    @NotNull Provider<?> provider
  ) {
    this.injector = injector;
    this.targetKey = targetKey;
    this.provider = DelegatingProviderFactory.toProvider(provider).constructProvider();
  }

  @Override
  public @NotNull BindingKey<?> mainKey() {
    return this.targetKey;
  }

  @Override
  public @NotNull @Unmodifiable List<BindingKey<?>> keys() {
    return List.of(this.targetKey);
  }

  @Override
  public boolean supportsKey(@NotNull BindingKey<?> key) {
    return this.targetKey.equals(key);
  }

  @Override
  public @NotNull Injector installedInjector() {
    return this.injector;
  }

  @Override
  public @NotNull BindingOptions options() {
    return EMPTY_BINDING_OPTIONS;
  }

  @Override
  public @NotNull Optional<ScopeApplier> scope() {
    return Optional.empty();
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull ProviderWithContext<Object> providerWithContext() {
    return (ProviderWithContext<Object>) this.provider;
  }

  @Override
  public @NotNull UninstalledBinding<Object> asUninstalled() {
    throw new UnsupportedOperationException("not supported on this binding");
  }
}
