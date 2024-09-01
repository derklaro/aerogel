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
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextScope;
import jakarta.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ContextualBindingResolver {

  private final Injector targetInjector;

  public ContextualBindingResolver(@NotNull Injector targetInjector) {
    this.targetInjector = targetInjector;
  }

  public @NotNull <T> Provider<T> constructProvider(@NotNull InstalledBinding<T> binding) {
    return () -> this.resolveInstance(binding);
  }

  @SuppressWarnings("unchecked")
  public @Nullable <T> T resolveInstance(@NotNull InstalledBinding<T> binding) {
    InjectionContextProvider provider = InjectionContextProvider.provider();
    InjectionContextScope scope = provider.enterContextScope(this.targetInjector, binding);
    InjectionContext context = scope.context();
    return scope.executeScoped(() -> {
      try {
        return (T) context.resolveInstance();
      } finally {
        if (context.root()) {
          context.finishConstruction();
        }
      }
    });
  }
}
