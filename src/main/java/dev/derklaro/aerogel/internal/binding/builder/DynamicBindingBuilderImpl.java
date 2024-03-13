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

package dev.derklaro.aerogel.internal.binding.builder;

import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.DynamicBindingBuilder;
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.binding.builder.ScopeableBindingBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

final class DynamicBindingBuilderImpl implements DynamicBindingBuilder {

  private final RootBindingBuilder rootBindingBuilder;

  public DynamicBindingBuilderImpl(@NotNull RootBindingBuilder rootBindingBuilder) {
    this.rootBindingBuilder = rootBindingBuilder;
  }

  @Override
  public @NotNull DynamicBindingBuilder annotationPresent(@NotNull Class<? extends Annotation> annotationType) {
    return null;
  }

  @Override
  public @NotNull DynamicBindingBuilder matchAnnotation(@NotNull Predicate<? extends Annotation> filter) {
    return null;
  }

  @Override
  public @NotNull <A extends Annotation> DynamicBindingBuilder matchAnnotation(@NotNull Class<A> annotationType,
    @NotNull Predicate<A> filter) {
    return null;
  }

  @Override
  public @NotNull DynamicBindingBuilder exactRawType(@NotNull Class<?> type) {
    return null;
  }

  @Override
  public @NotNull DynamicBindingBuilder superRawType(@NotNull Class<?> type) {
    return null;
  }

  @Override
  public @NotNull DynamicBindingBuilder matchRawType(@NotNull Predicate<Class<?>> filter) {
    return null;
  }

  @Override
  public @NotNull DynamicBindingBuilder matchType(@NotNull Predicate<Type> filter) {
    return null;
  }

  @Override
  public @NotNull DynamicBinding toBinding(@NotNull DynamicBinding binding) {
    return null;
  }

  @Override
  public @NotNull DynamicBinding toKeyedBindingProvider(
    @NotNull Function<ScopeableBindingBuilder<?>, UninstalledBinding<?>> bindingProvider) {
    return null;
  }

  @Override
  public @NotNull DynamicBinding toBindingProvider(
    @NotNull Function<RootBindingBuilder, UninstalledBinding<?>> bindingProvider) {
    return null;
  }
}
