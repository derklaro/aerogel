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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Provider;
import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A request to construct an instance of a specified binding associated with a specified key.
 *
 * @param <T> the type of values being constructed by this request.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface InjectionRequest<T> {

  /**
   * Get the key that was used to construct this request.
   *
   * @return the key that was used to construct this request.
   */
  @NotNull
  BindingKey<T> key();

  /**
   * Get the binding that is associated with the provided key.
   *
   * @return the binding that is associated with the provided key.
   */
  @NotNull
  InstalledBinding<T> binding();

  /**
   * Overrides the given type with the given value in this specific injection request.
   *
   * @param type  the type to override.
   * @param value the value to override the type with.
   * @param <V>   the generic type that is being overridden.
   * @return a new builder with the given override applied.
   */
  @NotNull
  @Contract(value = "_, _ -> new", pure = true)
  <V> InjectionRequest<T> override(@NotNull Type type, @Nullable V value);

  /**
   * Overrides the given type with the given value in this specific injection request.
   *
   * @param type  the type to override.
   * @param value the value to override the type with.
   * @param <V>   the generic type that is being overridden.
   * @return a new builder with the given override applied.
   */
  @NotNull
  @Contract(value = "_, _ -> new", pure = true)
  <V> InjectionRequest<T> override(@NotNull Class<? extends V> type, @Nullable V value);

  /**
   * Overrides the given type with the given value in this specific injection request.
   *
   * @param type  the type to override.
   * @param value the value to override the type with.
   * @param <V>   the generic type that is being overridden.
   * @return a new builder with the given override applied.
   */
  @NotNull
  @Contract(value = "_, _ -> new", pure = true)
  <V> InjectionRequest<T> override(@NotNull TypeToken<? extends V> type, @Nullable V value);

  /**
   * Overrides the given binding key with the given value in this specific injection request.
   *
   * @param key   the binding key to override.
   * @param value the value to override the binding key with.
   * @param <V>   the generic type that is being overridden.
   * @return a new builder with the given override applied.
   */
  @NotNull
  @Contract(value = "_, _ -> new", pure = true)
  <V> InjectionRequest<T> override(@NotNull BindingKey<? extends V> key, @Nullable V value);

  /**
   * Overrides the given type with the given value provider in this specific injection request.
   *
   * @param type          the type to override.
   * @param valueProvider the provider to call to obtain the override for the given type.
   * @param <V>           the generic type that is being overridden.
   * @return a new builder with the given override applied.
   */
  @NotNull
  @Contract(value = "_, _ -> new", pure = true)
  <V> InjectionRequest<T> override(@NotNull Type type, @NotNull Provider<? extends V> valueProvider);

  /**
   * Overrides the given type with the given value provider in this specific injection request.
   *
   * @param type          the type to override.
   * @param valueProvider the provider to call to obtain the override for the given type.
   * @param <V>           the generic type that is being overridden.
   * @return a new builder with the given override applied.
   */
  @NotNull
  @Contract(value = "_, _ -> new", pure = true)
  <V> InjectionRequest<T> override(@NotNull Class<? extends V> type, @NotNull Provider<? extends V> valueProvider);

  /**
   * Overrides the given type with the given value provider in this specific injection request.
   *
   * @param type          the type to override.
   * @param valueProvider the provider to call to obtain the override for the given type.
   * @param <V>           the generic type that is being overridden.
   * @return a new builder with the given override applied.
   */
  @NotNull
  @Contract(value = "_, _ -> new", pure = true)
  <V> InjectionRequest<T> override(@NotNull TypeToken<? extends V> type, @NotNull Provider<? extends V> valueProvider);

  /**
   * Overrides the given binding key with the given value provider in this specific injection request.
   *
   * @param key           the binding key to override.
   * @param valueProvider the provider to call to obtain the override for the given type.
   * @param <V>           the generic type that is being overridden.
   * @return a new builder with the given override applied.
   */
  @NotNull
  @Contract(value = "_, _ -> new", pure = true)
  <V> InjectionRequest<T> override(@NotNull BindingKey<? extends V> key, @NotNull Provider<? extends V> valueProvider);

  /**
   * Constructs an instance of the associated binding with the settings applied in this injection request.
   *
   * @return an instance of the associated binding with the settings applied in this injection request.
   */
  T construct();

  /**
   * Constructs a provider for instances of the associated binding with the settings applied in this injection request.
   *
   * @return a provider for instances of the associated binding with the settings applied in this injection request.
   */
  @NotNull
  Provider<T> createProvider();
}
