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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.apiguardian.api.API;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A mapping between a key and a value. Registries can exist in two forms: with and without an explicit key mapping. If
 * a registry has an explicit key mapping, one key is associated with one value. In case no explicit key mapping exists,
 * only a value is put into the registry which can be accessed by providing a key that matches the value.
 * <p>
 * Each registry can have a parent. If a get operation is triggered and the value is not directly present in the current
 * registry, a lookup will be done in the parent registry until the root registry is reached.
 * <p>
 * All registry implementations must be thread-safe.
 *
 * @param <K> the type of keys that can be used to access the values in the registry.
 * @param <V> the type of values stored in this registry.
 * @author Pasqual Koschmieder
 * @see WithKeyMapping
 * @see WithoutKeyMapping
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface Registry<K, V> {

  /**
   * Creates a new root registry that uses key bindings to resolve values.
   *
   * @param <K> the type of keys.
   * @param <V> the type of values.
   * @return a new root key-based registry.
   */
  @NotNull
  @Contract(value = " -> new", pure = true)
  static <K, V> Registry.WithKeyMapping<K, V> createRegistryWithKeys() {
    return new RegistryWithKeyImpl<>();
  }

  /**
   * Creates a new root registry that uses no key bindings when registering values to it. The given key tester is used
   * to check if a given key matches a registered value when an access operation via key is done.
   *
   * @param keyTester the tester to check if a given key belongs to a registered value.
   * @param <K>       the type of keys.
   * @param <V>       the type of values.
   * @return a new root value-based registry.
   */
  @NotNull
  @Contract(value = "_ -> new", pure = true)
  static <K, V> Registry.WithoutKeyMapping<K, V> createRegistryWithoutKeys(@NotNull BiPredicate<K, V> keyTester) {
    return new RegistryWithoutKeyImpl<>(keyTester);
  }

  /**
   * Get the optional parent registry. Empty if this registry is a root registry.
   *
   * @return the optional parent registry.
   */
  @NotNull
  Optional<Registry<K, V>> parent();

  /**
   * Get a value that matches the given key. If no direct mapping is present in this registry, the key will be looked up
   * in the parent registry. This process is done until a mapping was found or the root registry is reached.
   *
   * @param key the key to get the mapped value of.
   * @return the value mapped to the given key, an empty optional if no mapping exists.
   * @see #getDirect(Object)
   */
  @NotNull
  Optional<V> get(@NotNull K key);

  /**
   * Get a value that matches the given key in this registry only. If no mapping is present in this registry, an empty
   * optional is returned instead of a lookup in the parent registry.
   *
   * @param key the key to get the mapped value of.
   * @return the value mapped to the given key, an empty optional if no mapping exists in this registry.
   * @see #get(Object)
   */
  @NotNull
  Optional<V> getDirect(@NotNull K key);

  /**
   * Unregisters the values that are mapped or match the given key from this registry.
   *
   * @param key the key to remove the values of.
   * @throws UnsupportedOperationException if this registry is frozen.
   */
  void unregisterByKey(@NotNull K key);

  /**
   * Unregisters the given value from this registry, removing all keys that are associated with the given value.
   *
   * @param value the value to unregister from this registry.
   * @throws UnsupportedOperationException if this registry is frozen.
   */
  void unregisterByValue(@NotNull V value);

  /**
   * Unregisters all entries from this registry that match the given filter.
   *
   * @param filter the filter for the values to unregister.
   * @throws UnsupportedOperationException if this registry is frozen.
   */
  void unregister(@NotNull Predicate<V> filter);

  /**
   * Get the amount of entries that are registered in thís registry.
   *
   * @return the amount of entries that are registered in thís registry.
   */
  int entryCount();

  /**
   * Copies the registry entries into a new registry which has the same attributes as this registry. If this registry is
   * frozen, the copied registry will be frozen as well.
   *
   * @return a new registry with the same properties and a copy of the values of this registry.
   */
  @NotNull
  @CheckReturnValue
  Registry<K, V> copy();

  /**
   * Freezes the current state of the registry, disallowing any modifications to it. This method does nothing if the
   * current registry is already frozen. Note: changes made to this registry after freezing will reflect into the frozen
   * registry.
   *
   * @return a new, frozen registry containing the same elements as this registry.
   */
  @NotNull
  @CheckReturnValue
  Registry<K, V> freeze();

  /**
   * Creates a new, empty child registry that uses this registry as the parent registry. If this registry is frozen, the
   * returned registry will not be frozen.
   *
   * @return a new, empty child registry that uses this registry as the parent registry.
   */
  @NotNull
  @CheckReturnValue
  Registry<K, V> createChildRegistry();

  /**
   * A type of registry that maps keys to values. Values can only be registered with a key associated to them. Keys can
   * only be registered once in this type of registry.
   *
   * @param <K> the type of keys that can be used to access the values in the registry.
   * @param <V> the type of values stored in this registry.
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  @API(status = API.Status.STABLE, since = "3.0")
  interface WithKeyMapping<K, V> extends Registry<K, V> {

    /**
     * Registers the given key-value mapping into this registry.
     *
     * @param key   the key to associate with the given value.
     * @param value the value to associated with the given key.
     * @throws UnsupportedOperationException if this registry is frozen.
     */
    void register(@NotNull K key, @NotNull V value);

    /**
     * Get a snapshot of the entries currently mapped in this registry. Changes made to the registry after getting the
     * snapshot do not reflect into the returned map.
     *
     * @return a snapshot of the entries currently mapped in this registry.
     */
    @NotNull
    @Unmodifiable
    Map<K, V> entries();

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    Registry.WithKeyMapping<K, V> copy();

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    Registry.WithKeyMapping<K, V> freeze();

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    Registry.WithKeyMapping<K, V> createChildRegistry();
  }

  /**
   * A type of registry that does not require a direct key-value mapping when registering a value into the registry.
   * When getting a value based on a key from this registry, the registry needs to somehow decide if a registered value
   * matches the given key. Values can only be registered once into this registry.
   *
   * @param <K> the type of keys that can be used to access the values in the registry.
   * @param <V> the type of values stored in this registry.
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  @API(status = API.Status.STABLE, since = "3.0")
  interface WithoutKeyMapping<K, V> extends Registry<K, V> {

    /**
     * Registers the given value into this registry.
     *
     * @param value the value to register.
     * @throws UnsupportedOperationException if this registry is frozen.
     */
    void register(@NotNull V value);

    /**
     * Get a snapshot of the values that are currently registered in this registry. Changes made to the registry after
     * getting the snapshot do not reflect into the returned collection.
     *
     * @return a snapshot of the values that are currently registered in this registry.
     */
    @NotNull
    @Unmodifiable
    Collection<V> entries();

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    Registry.WithoutKeyMapping<K, V> copy();

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    Registry.WithoutKeyMapping<K, V> freeze();

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    Registry.WithoutKeyMapping<K, V> createChildRegistry();
  }
}
