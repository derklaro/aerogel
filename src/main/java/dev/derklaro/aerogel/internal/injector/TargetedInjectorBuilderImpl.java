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
import dev.derklaro.aerogel.TargetedInjectorBuilder;
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.registry.Registry;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class TargetedInjectorBuilderImpl implements TargetedInjectorBuilder {

  private final AtomicReference<TargetedInjectorImpl> injectorRef;
  private final Registry.WithKeyMapping<BindingKey<?>, InstalledBinding<?>> bindingRegistry;

  /* trusted */
  TargetedInjectorBuilderImpl(
    @NotNull Injector parent,
    @NotNull Injector nonTargetedInjector,
    @NotNull InjectorOptions injectorOptions
  ) {
    this.bindingRegistry = parent.bindingRegistry().createChildRegistry();

    TargetedInjectorImpl injector = new TargetedInjectorImpl(
      parent,
      nonTargetedInjector,
      injectorOptions,
      this.bindingRegistry);
    this.injectorRef = new AtomicReference<>(injector);
  }

  private static @NotNull TargetedInjectorImpl validateInjectorPresence(@Nullable TargetedInjectorImpl maybeInjector) {
    if (maybeInjector == null) {
      throw new IllegalStateException("specific injector builder already targeted.");
    }
    return maybeInjector;
  }

  @Override
  public @NotNull <T> TargetedInjectorBuilder installBinding(@NotNull UninstalledBinding<T> binding) {
    TargetedInjectorImpl refInjector = this.injectorRef.get();
    TargetedInjectorImpl injector = validateInjectorPresence(refInjector);

    InstalledBinding<?> installedBinding = binding.prepareForInstallation(injector);
    this.bindingRegistry.register(binding.key(), installedBinding);
    return this;
  }

  @Override
  public @NotNull Injector build() {
    TargetedInjectorImpl refInjector = this.injectorRef.getAndSet(null);
    return validateInjectorPresence(refInjector);
  }
}
