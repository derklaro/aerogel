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

package dev.derklaro.aerogel.registry;

import java.util.Optional;
import java.util.function.Predicate;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * The base implementation of a frozen registry that implements all shared methods between registry types.
 *
 * @param <K> the type of keys that can be used to access the values in the registry.
 * @param <V> the type of values stored in this registry.
 * @param <R> the type of registry being implemented.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
abstract class BaseFrozenRegistryImpl<K, V, R extends Registry<K, V>> implements Registry<K, V> {

  protected final R delegate;

  /**
   * Constructs a new base implementation of a frozen registry, using the given registry as the delegate for read
   * calls.
   *
   * @param delegate the original registry that contains the values to read from. Will not be written to.
   */
  protected BaseFrozenRegistryImpl(@NotNull R delegate) {
    this.delegate = delegate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<Registry<K, V>> parent() {
    return this.delegate.parent();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<V> get(@NotNull K key) {
    return this.delegate.get(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<V> getDirect(@NotNull K key) {
    return this.delegate.getDirect(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterByKey(@NotNull K key) {
    throw new UnsupportedOperationException("registry is frozen");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterByValue(@NotNull V value) {
    throw new UnsupportedOperationException("registry is frozen");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(@NotNull Predicate<V> filter) {
    throw new UnsupportedOperationException("registry is frozen");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int entryCount() {
    return this.delegate.entryCount();
  }
}
