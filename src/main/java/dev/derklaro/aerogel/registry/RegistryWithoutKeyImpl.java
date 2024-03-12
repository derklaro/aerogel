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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * The default implementation of a registry that only registers values without a key mapping.
 *
 * @param <K> the type of keys that can be used to access the values in the registry.
 * @param <V> the type of values stored in this registry.
 * @author Pasqual Koschmieder
 * @see Registry#createRegistryWithoutKeys(BiPredicate)
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
final class RegistryWithoutKeyImpl<K, V> implements Registry.WithoutKeyMapping<K, V> {

  private final Map<V, Boolean> backingMap;
  private final BiPredicate<K, V> keyTester;
  private final Registry.WithoutKeyMapping<K, V> parent;

  /**
   * Constructs a new, empty root registry.
   *
   * @param keyTester the tester to check if a given key belongs to a registered value.
   */
  public RegistryWithoutKeyImpl(@NotNull BiPredicate<K, V> keyTester) {
    this(keyTester, null);
  }

  /**
   * Constructs a new, empty registry that uses the given registry as the parent.
   *
   * @param keyTester the tester to check if a given key belongs to a registered value.
   * @param parent    the parent registry of this registry.
   */
  private RegistryWithoutKeyImpl(
    @NotNull BiPredicate<K, V> keyTester,
    @Nullable Registry.WithoutKeyMapping<K, V> parent
  ) {
    this(keyTester, MapUtil.newConcurrentMap(), parent);
  }

  /**
   * Creates a new registry that uses the given map as the backing map and the given registry as the parent.
   *
   * @param keyTester  the tester to check if a given key belongs to a registered value.
   * @param backingMap the backing map containing the entries of this registry.
   * @param parent     the parent registry of this registry.
   */
  private RegistryWithoutKeyImpl(
    @NotNull BiPredicate<K, V> keyTester,
    @NotNull Map<V, Boolean> backingMap,
    @Nullable Registry.WithoutKeyMapping<K, V> parent
  ) {
    this.keyTester = keyTester;
    this.backingMap = backingMap;
    this.parent = parent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void register(@NotNull V value) {
    Boolean prev = this.backingMap.putIfAbsent(value, Boolean.TRUE);
    if (prev != null) {
      throw new IllegalArgumentException("Registry did already contain value " + value);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull @Unmodifiable Collection<V> entries() {
    Set<V> keys = this.backingMap.keySet();
    return Collections.unmodifiableCollection(new HashSet<>(keys));
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
    V mapping = this.getDirectNullable(key);
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
    V mapping = this.getDirectNullable(key);
    return Optional.ofNullable(mapping);
  }

  /**
   * Tries to resolve a value that matches the given key. If no such value is found, null is returned.
   *
   * @param key the key to find an associated value for.
   * @return the value that matches the given key, null if no matching value is present.
   */
  private @Nullable V getDirectNullable(@NotNull K key) {
    for (V entry : this.backingMap.keySet()) {
      if (this.keyTester.test(key, entry)) {
        return entry;
      }
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterByKey(@NotNull K key) {
    this.backingMap.keySet().removeIf(value -> this.keyTester.test(key, value));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterByValue(@NotNull V value) {
    this.backingMap.remove(value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregister(@NotNull Predicate<V> filter) {
    this.backingMap.keySet().removeIf(filter);
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
  public @NotNull Registry.WithoutKeyMapping<K, V> copy() {
    Map<V, Boolean> newBackingMap = MapUtil.newConcurrentMap();
    newBackingMap.putAll(this.backingMap);
    return new RegistryWithoutKeyImpl<>(this.keyTester, newBackingMap, this.parent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Registry.WithoutKeyMapping<K, V> freeze() {
    return new FrozenImpl<>(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Registry.WithoutKeyMapping<K, V> createChildRegistry() {
    return new RegistryWithoutKeyImpl<>(this.keyTester, this);
  }

  /**
   * An implementation of {@link Registry.WithoutKeyMapping} that does not support any modifications.
   *
   * @param <K> the type of keys that can be used to access the values in the registry.
   * @param <V> the type of values stored in this registry.
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  private static final class FrozenImpl<K, V>
    extends BaseFrozenRegistryImpl<K, V, Registry.WithoutKeyMapping<K, V>>
    implements Registry.WithoutKeyMapping<K, V> {

    /**
     * Constructs a new frozen registry implementation of {@link Registry.WithoutKeyMapping}.
     *
     * @param delegate the original registry that contains the values to read from. Will not be written to.
     */
    private FrozenImpl(@NotNull Registry.WithoutKeyMapping<K, V> delegate) {
      super(delegate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(@NotNull V value) {
      throw new UnsupportedOperationException("registry is frozen");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull @Unmodifiable Collection<V> entries() {
      return this.delegate.entries();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Registry.WithoutKeyMapping<K, V> copy() {
      return new FrozenImpl<>(this.delegate.copy());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Registry.WithoutKeyMapping<K, V> freeze() {
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Registry.WithoutKeyMapping<K, V> createChildRegistry() {
      return this.delegate.createChildRegistry();
    }
  }
}
