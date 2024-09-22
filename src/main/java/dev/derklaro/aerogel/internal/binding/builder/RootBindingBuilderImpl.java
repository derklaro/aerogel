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

import dev.derklaro.aerogel.ScopeApplier;
import dev.derklaro.aerogel.binding.builder.DynamicBindingBuilder;
import dev.derklaro.aerogel.binding.builder.KeyableBindingBuilder;
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.binding.BindingOptionsImpl;
import dev.derklaro.aerogel.registry.Registry;
import io.leangen.geantyref.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class RootBindingBuilderImpl implements RootBindingBuilder {

  private final BindingOptionsImpl standardBindingOptions;
  private final Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry;

  public RootBindingBuilderImpl(
    @NotNull BindingOptionsImpl standardBindingOptions,
    @NotNull Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopes
  ) {
    this.standardBindingOptions = standardBindingOptions;
    this.scopeRegistry = scopes;
  }

  @Override
  public @NotNull DynamicBindingBuilder bindDynamically() {
    return new DynamicBindingBuilderImpl(this);
  }

  @Override
  public @NotNull <T> KeyableBindingBuilder<T> bind(@NotNull Type type) {
    BindingKey<T> key = BindingKey.of(type);
    return new ConcreteBindingBuilderImpl<>(List.of(key), this.standardBindingOptions, this.scopeRegistry);
  }

  @Override
  public @NotNull <T> KeyableBindingBuilder<T> bind(@NotNull Class<T> type) {
    BindingKey<T> key = BindingKey.of(type);
    return new ConcreteBindingBuilderImpl<>(List.of(key), this.standardBindingOptions, this.scopeRegistry);
  }

  @Override
  public @NotNull <T> KeyableBindingBuilder<T> bind(@NotNull TypeToken<T> typeToken) {
    BindingKey<T> key = BindingKey.of(typeToken);
    return new ConcreteBindingBuilderImpl<>(List.of(key), this.standardBindingOptions, this.scopeRegistry);
  }

  @Override
  public @NotNull <T> KeyableBindingBuilder<T> bind(@NotNull BindingKey<T> bindingKey) {
    return new ConcreteBindingBuilderImpl<>(List.of(bindingKey), this.standardBindingOptions, this.scopeRegistry);
  }
}
