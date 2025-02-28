/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2025 Pasqual K. and contributors
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

import dev.derklaro.aerogel.InjectionRequest;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextScope;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Provider;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class InjectionRequestImpl<T> implements InjectionRequest<T> {

  @SuppressWarnings("unchecked")
  private static final Map.Entry<BindingKey<?>, Provider<?>>[] EMPTY_OVERRIDES = new Map.Entry[0];

  private final Injector injector;
  private final BindingKey<T> key;
  private final InstalledBinding<T> binding;
  private final Map.Entry<BindingKey<?>, Provider<?>>[] overrides;

  // local cache for the mapping between the overrides entry array and the overrides map
  // note that this should only be accessed createOverridesMap and not be modified
  private Map<BindingKey<?>, Provider<?>> localOverridesCache;

  public InjectionRequestImpl(
    @NotNull Injector injector,
    @NotNull BindingKey<T> key,
    @NotNull InstalledBinding<T> binding
  ) {
    this(injector, key, binding, EMPTY_OVERRIDES);
  }

  private InjectionRequestImpl(
    @NotNull Injector injector,
    @NotNull BindingKey<T> key,
    @NotNull InstalledBinding<T> binding,
    @NotNull Map.Entry<BindingKey<?>, Provider<?>>[] overrides
  ) {
    this.injector = injector;
    this.key = key;
    this.binding = binding;
    this.overrides = overrides;
  }

  @Override
  public @NotNull BindingKey<T> key() {
    return this.key;
  }

  @Override
  public @NotNull InstalledBinding<T> binding() {
    return this.binding;
  }

  @Override
  public @NotNull <V> InjectionRequest<T> override(@NotNull Type type, @Nullable V value) {
    Provider<? extends V> valueProvider = () -> value;
    return this.overrideProvider(type, valueProvider);
  }

  @Override
  public @NotNull <V> InjectionRequest<T> override(@NotNull Class<V> type, @Nullable V value) {
    Provider<? extends V> valueProvider = () -> value;
    return this.overrideProvider(type, valueProvider);
  }

  @Override
  public @NotNull <V> InjectionRequest<T> override(@NotNull TypeToken<V> key, @Nullable V value) {
    Provider<? extends V> valueProvider = () -> value;
    return this.overrideProvider(key, valueProvider);
  }

  @Override
  public @NotNull <V> InjectionRequest<T> override(@NotNull BindingKey<V> key, @Nullable V value) {
    Provider<? extends V> valueProvider = () -> value;
    return this.overrideProvider(key, valueProvider);
  }

  @Override
  public @NotNull <V> InjectionRequest<T> overrideProvider(
    @NotNull Type type,
    @NotNull Provider<? extends V> valueProvider
  ) {
    BindingKey<V> bindingKey = BindingKey.of(type);
    return this.overrideProvider(bindingKey, valueProvider);
  }

  @Override
  public @NotNull <V> InjectionRequest<T> overrideProvider(
    @NotNull Class<V> type,
    @NotNull Provider<? extends V> valueProvider
  ) {
    BindingKey<V> bindingKey = BindingKey.of(type);
    return this.overrideProvider(bindingKey, valueProvider);
  }

  @Override
  public @NotNull <V> InjectionRequest<T> overrideProvider(
    @NotNull TypeToken<V> type,
    @NotNull Provider<? extends V> valueProvider
  ) {
    BindingKey<V> bindingKey = BindingKey.of(type);
    return this.overrideProvider(bindingKey, valueProvider);
  }

  @Override
  public @NotNull <V> InjectionRequest<T> overrideProvider(
    @NotNull BindingKey<V> key,
    @NotNull Provider<? extends V> valueProvider
  ) {
    int currentOverridesLength = this.overrides.length;
    Map.Entry<BindingKey<?>, Provider<?>>[] overrides = Arrays.copyOf(this.overrides, currentOverridesLength + 1);
    overrides[currentOverridesLength] = Map.entry(key, valueProvider);
    return new InjectionRequestImpl<>(this.injector, this.key, this.binding, overrides);
  }

  @Override
  @SuppressWarnings("unchecked")
  public T construct() {
    Map<BindingKey<?>, Provider<?>> overrides = this.createOverridesMap();
    InjectionContextProvider provider = InjectionContextProvider.provider();
    InjectionContextScope scope = provider.enterContextScope(this.injector, this.key, this.binding, overrides);
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

  @Override
  public @NotNull Provider<T> createProvider() {
    return this::construct;
  }

  /**
   * Get or construct the local override cache for this injection request implementation. The map should only be
   * initialized once, but that is not a strict requirement during concurrent calls.
   *
   * @return the overrides that were added to this injection request.
   */
  private @NotNull Map<BindingKey<?>, Provider<?>> createOverridesMap() {
    Map<BindingKey<?>, Provider<?>> localOverrides = this.localOverridesCache;
    if (localOverrides != null) {
      return localOverrides;
    }

    Map.Entry<BindingKey<?>, Provider<?>>[] overrides = this.overrides;
    if (overrides.length == 0) {
      return Map.of();
    }

    if (overrides.length == 1) {
      Map.Entry<BindingKey<?>, Provider<?>> override = overrides[0];
      this.localOverridesCache = localOverrides = Map.of(override.getKey(), override.getValue());
      return localOverrides;
    }

    localOverrides = new HashMap<>();
    for (int index = this.overrides.length - 1; index >= 0; index--) {
      Map.Entry<BindingKey<?>, Provider<?>> entry = this.overrides[index];
      localOverrides.putIfAbsent(entry.getKey(), entry.getValue());
    }

    this.localOverridesCache = localOverrides;
    return localOverrides;
  }
}
