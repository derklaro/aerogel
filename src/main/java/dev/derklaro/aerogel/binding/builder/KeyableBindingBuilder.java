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

package dev.derklaro.aerogel.binding.builder;

import dev.derklaro.aerogel.binding.key.BindingKey;
import io.leangen.geantyref.TypeToken;
import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A builder steps that allows to define additional binding keys that will be supported by the final binding as well.
 * This can for example be useful to multiple keys to the same singleton binding.
 *
 * @param <T> the type of values handled by this binding that is being built.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface KeyableBindingBuilder<T> extends QualifiableBindingBuilder<T> {

  /**
   * Adds the type as one of the supported types of the constructed binding. This method can be called as many times as
   * needed to represent all types that are constructable by the final binding.
   *
   * @param type the type to add as a supported type to the final binding.
   * @return this builder, for chaining or finishing the binding process.
   * @throws IllegalArgumentException if the given type is already marked as a target of this builder.
   */
  @NotNull
  KeyableBindingBuilder<T> andBind(@NotNull Type type);

  /**
   * Adds the type as one of the supported types of the constructed binding. This method can be called as many times as
   * needed to represent all types that are constructable by the final binding.
   *
   * @param type the type to add as a supported type to the final binding.
   * @return this builder, for chaining or finishing the binding process.
   * @throws IllegalArgumentException if the given type is already marked as a target of this builder.
   */
  @NotNull
  KeyableBindingBuilder<T> andBind(@NotNull Class<? extends T> type);

  /**
   * Adds the type represented by the given type token as one of the supported types of the constructed binding. This
   * method can be called as many times as needed to represent all types that are constructable by the final binding.
   *
   * @param typeToken the type token holding the type to add as a supported type to the final binding.
   * @return this builder, for chaining or finishing the binding process.
   * @throws IllegalArgumentException if the given type is already marked as a target of this builder.
   */
  @NotNull
  KeyableBindingBuilder<T> andBind(@NotNull TypeToken<? extends T> typeToken);

  /**
   * Adds the key as one of the supported keys of the constructed binding. This method can be called as many times as
   * needed to represent all keys that are constructable by the final binding.
   *
   * @param bindingKey the binding key to add to the as a supported key to the final binding.
   * @return this builder, for chaining or finishing the binding process.
   * @throws IllegalArgumentException if the given key is already marked as a target of this builder.
   */
  @NotNull
  KeyableBindingBuilder<T> andBind(@NotNull BindingKey<? extends T> bindingKey);
}
