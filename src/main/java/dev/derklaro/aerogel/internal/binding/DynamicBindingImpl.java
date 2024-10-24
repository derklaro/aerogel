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

import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

public final class DynamicBindingImpl implements DynamicBinding {

  private final Predicate<BindingKey<?>> filter;
  private final Function<BindingKey<?>, UninstalledBinding<?>> bindingFactory;

  public DynamicBindingImpl(
    @NotNull Predicate<BindingKey<?>> filter,
    @NotNull Function<BindingKey<?>, UninstalledBinding<?>> bindingFactory
  ) {
    this.filter = filter;
    this.bindingFactory = bindingFactory;
  }

  @Override
  public boolean supports(@NotNull BindingKey<?> key) {
    return this.filter.test(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull <T> Optional<UninstalledBinding<T>> tryMatch(@NotNull BindingKey<T> key) {
    if (this.filter.test(key)) {
      UninstalledBinding<T> binding = (UninstalledBinding<T>) this.bindingFactory.apply(key);
      return Optional.ofNullable(binding);
    } else {
      return Optional.empty();
    }
  }
}
