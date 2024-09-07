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

import dev.derklaro.aerogel.internal.util.MapUtil;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The default implementation of a registry that uses key-value mappings.
 *
 * @param <K> the type of keys that can be used to access the values in the registry.
 * @param <V> the type of values stored in this registry.
 * @author Pasqual Koschmieder
 * @see Registry#createRegistryWithKeys()
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
final class RegistryWithKeyImpl<K, V> implements Registry.WithKeyMapping<K, V> {

  private final Map<K, V> backingMap;
  private final Registry.WithKeyMapping<K, V> parent;

  /**
   * Creates a new, empty root registry.
   */
  public RegistryWithKeyImpl() {
    this(null);
  }

  /**
   * Creates a new, empty registry that uses the given registry as the parent.
   *
   * @param parent the parent registry of this registry.
   */
  private RegistryWithKeyImpl(@Nullable Registry.WithKeyMapping<K, V> parent) {
    this(MapUtil.newConcurrentMap(), parent);
  }

  /**
   * Creates a new registry that uses the given map as the backing map and the given registry as the parent.
   *
   * @param backingMap the backing map containing the entries of this registry.
   * @param parent     the parent registry of this registry.
   */
  private RegistryWithKeyImpl(@NotNull Map<K, V> backingMap, @Nullable Registry.WithKeyMapping<K, V> parent) {
    this.backingMap = backingMap;
    this.parent = parent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(@NotNull K key, @NotNull V value) {
    V previous = this.backingMap.putIfAbsent(key, value);
    if (previous != null) {
      throw new IllegalArgumentException("Key " + key + " already registered in registry");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull @Unmodifiable Map<K, V> entries() {
    Map<K, V> copy = new HashMap<>(this.backingMap);
    return Collections.unmodifiableMap(copy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<Registry<K, V>> parent() {
    return Optional.ofNullable(this.parent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<V> get(@NotNull K key) {
    // check current registry first
    V mapping = this.backingMap.get(key);
    if (mapping != null) {
      return Optional.of(mapping);
    }

    // try parent, if present
    if (this.parent != null) {
      return this.parent.get(key);
    }

    return Optional.empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Optional<V> getDirect(@NotNull K key) {
    V mapping = this.backingMap.get(key);
    return Optional.ofNullable(mapping);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterByKey(@NotNull K key) {
    this.backingMap.remove(key);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterByValue(@NotNull V value) {
    this.backingMap.values().removeIf(value::equals);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(@NotNull Predicate<V> filter) {
    this.backingMap.values().removeIf(filter);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int entryCount() {
    return this.backingMap.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Registry.WithKeyMapping<K, V> copy() {
    Map<K, V> newBackingMap = MapUtil.newConcurrentMap();
    newBackingMap.putAll(this.backingMap);
    return new RegistryWithKeyImpl<>(newBackingMap, this.parent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Registry.WithKeyMapping<K, V> freeze() {
    return new FrozenImpl<>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Registry.WithKeyMapping<K, V> createChildRegistry() {
    return new RegistryWithKeyImpl<>(this);
  }

  /**
   * An implementation of {@link Registry.WithKeyMapping} that does not support any modifications.
   *
   * @param <K> the type of keys that can be used to access the values in the registry.
   * @param <V> the type of values stored in this registry.
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  @API(status = API.Status.INTERNAL, since = "3.0")
  private static final class FrozenImpl<K, V>
    extends BaseFrozenRegistryImpl<K, V, Registry.WithKeyMapping<K, V>>
    implements Registry.WithKeyMapping<K, V> {

    /**
     * Constructs a new frozen registry implementation of {@link Registry.WithKeyMapping}.
     *
     * @param delegate the original registry that contains the values to read from. Will not be written to.
     */
    private FrozenImpl(@NotNull Registry.WithKeyMapping<K, V> delegate) {
      super(delegate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(@NotNull K key, @NotNull V value) {
      throw new UnsupportedOperationException("registry is frozen");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull @Unmodifiable Map<K, V> entries() {
      return this.delegate.entries();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Registry.WithKeyMapping<K, V> copy() {
      return new FrozenImpl<>(this.delegate.copy());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Registry.WithKeyMapping<K, V> freeze() {
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Registry.WithKeyMapping<K, V> createChildRegistry() {
      return this.delegate.createChildRegistry();
    }
  }
}
