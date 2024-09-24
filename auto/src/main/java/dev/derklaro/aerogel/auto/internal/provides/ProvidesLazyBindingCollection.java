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

package dev.derklaro.aerogel.auto.internal.provides;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.auto.LazyBindingCollection;
import dev.derklaro.aerogel.auto.internal.CombinedLazyBindingCollection;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.KeyableBindingBuilder;
import java.util.List;
import org.jetbrains.annotations.NotNull;

final class ProvidesLazyBindingCollection implements LazyBindingCollection {

  private final Class<?> implementation;
  private final Class<?>[] providedTypes;

  public ProvidesLazyBindingCollection(@NotNull Class<?> implementation, @NotNull Class<?>[] providedTypes) {
    this.implementation = implementation;
    this.providedTypes = providedTypes;
  }

  @Override
  public void installBindings(@NotNull Injector injector) {
    UninstalledBinding<?> binding = this.constructBinding(injector);
    injector.installBinding(binding);
  }

  @Override
  public @NotNull List<UninstalledBinding<?>> constructBindings(@NotNull Injector injector) {
    UninstalledBinding<?> binding = this.constructBinding(injector);
    return List.of(binding);
  }

  @Override
  public @NotNull LazyBindingCollection combine(@NotNull LazyBindingCollection other) {
    return new CombinedLazyBindingCollection(this, other);
  }

  @SuppressWarnings("unchecked")
  private @NotNull UninstalledBinding<?> constructBinding(@NotNull Injector injector) {
    // bind the main key first
    Class<Object> mainType = (Class<Object>) this.providedTypes[0];
    KeyableBindingBuilder<Object> bindingBuilder = injector.createBindingBuilder().bind(mainType);

    // check if there are additional keys that need to be bound as well
    int providedTypeCount = this.providedTypes.length;
    if (providedTypeCount > 1) {
      for (int index = 1; index < providedTypeCount; index++) {
        Class<Object> providedType = (Class<Object>) this.providedTypes[index];
        bindingBuilder = bindingBuilder.andBind(providedType);
      }
    }

    // target the builder to the final implementing class
    return bindingBuilder.toConstructingClass(this.implementation);
  }
}