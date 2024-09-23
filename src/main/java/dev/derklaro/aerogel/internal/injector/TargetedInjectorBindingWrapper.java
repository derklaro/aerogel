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

package dev.derklaro.aerogel.internal.injector;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.ScopeApplier;
import dev.derklaro.aerogel.binding.BindingOptions;
import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

final class TargetedInjectorBindingWrapper<T> implements UninstalledBinding<T> {

  private final UninstalledBinding<T> delegate;
  private final TargetedInjectorImpl originalInjector;

  public TargetedInjectorBindingWrapper(
    @NotNull UninstalledBinding<T> delegate,
    @NotNull TargetedInjectorImpl originalInjector
  ) {
    this.delegate = delegate;
    this.originalInjector = originalInjector;
  }

  @Override
  public @NotNull BindingKey<? extends T> mainKey() {
    return this.delegate.mainKey();
  }

  @Override
  public @NotNull @Unmodifiable List<BindingKey<? extends T>> keys() {
    return this.delegate.keys();
  }

  @Override
  public boolean supportsKey(@NotNull BindingKey<?> key) {
    return this.delegate.supportsKey(key);
  }

  @Override
  public @NotNull BindingOptions options() {
    return this.delegate.options();
  }

  @Override
  public @NotNull Optional<ScopeApplier> scope() {
    return this.delegate.scope();
  }

  @Override
  public @NotNull UninstalledBinding<T> withScope(@Nullable ScopeApplier scope) {
    UninstalledBinding<T> delegateWithNewScope = this.delegate.withScope(scope);
    return new TargetedInjectorBindingWrapper<>(delegateWithNewScope, this.originalInjector);
  }

  @Override
  public @NotNull InstalledBinding<T> prepareForInstallation(@NotNull Injector injector) {
    InstalledBinding<T> delegateInstalled = this.delegate.prepareForInstallation(injector);
    return new InstalledWrapper<>(this.originalInjector, delegateInstalled);
  }

  static final class InstalledWrapper<T> implements InstalledBinding<T> {

    final TargetedInjectorImpl originalInjector;
    private final InstalledBinding<T> delegate;

    public InstalledWrapper(@NotNull TargetedInjectorImpl originalInjector, @NotNull InstalledBinding<T> delegate) {
      this.originalInjector = originalInjector;
      this.delegate = delegate;
    }

    @Override
    public @NotNull BindingKey<? extends T> mainKey() {
      return this.delegate.mainKey();
    }

    @Override
    public @NotNull @Unmodifiable List<BindingKey<? extends T>> keys() {
      return this.delegate.keys();
    }

    @Override
    public boolean supportsKey(@NotNull BindingKey<?> key) {
      return this.delegate.supportsKey(key);
    }

    @Override
    public @NotNull Injector installedInjector() {
      return this.delegate.installedInjector();
    }

    @Override
    public @NotNull BindingOptions options() {
      return this.delegate.options();
    }

    @Override
    public @NotNull Optional<ScopeApplier> scope() {
      return this.delegate.scope();
    }

    @Override
    public @NotNull ProviderWithContext<T> providerWithContext() {
      return this.delegate.providerWithContext();
    }

    @Override
    public @NotNull UninstalledBinding<T> asUninstalled() {
      return this.delegate.asUninstalled();
    }
  }

  static final class DynamicWrapper implements DynamicBinding {

    final TargetedInjectorImpl originalInjector;
    private final DynamicBinding delegate;

    public DynamicWrapper(@NotNull TargetedInjectorImpl originalInjector, @NotNull DynamicBinding delegate) {
      this.originalInjector = originalInjector;
      this.delegate = delegate;
    }

    @Override
    public boolean supports(@NotNull BindingKey<?> key) {
      return this.delegate.supports(key);
    }

    @Override
    public @NotNull <T> Optional<UninstalledBinding<T>> tryMatch(@NotNull BindingKey<T> key) {
      return this.delegate.tryMatch(key);
    }
  }
}
