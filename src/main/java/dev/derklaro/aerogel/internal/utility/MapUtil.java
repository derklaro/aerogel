/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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

package dev.derklaro.aerogel.internal.utility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A small utility to provide pre-configured map instances.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel")
public final class MapUtil {

  private MapUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new concurrent map instance with an initial capacity of 16, a load factor of 0.9 (to ensure dense
   * packaging which improves memory usage) and 1 shard (1 shard can handle multiple concurrent calls, there is no need
   * to create more shards).
   *
   * @param <K> the type of keys which are stored in the map.
   * @param <V> the type of values which are stored in the map.
   * @return a new concurrent map as described above.
   */
  public static @NotNull <K, V> ConcurrentMap<K, V> newConcurrentMap() {
    return new ConcurrentHashMap<>(16, 0.9f, 1);
  }

  /**
   * Constructs a new map with the given expected size, which prevents resizing. The created map is passed to the given
   * decorator and then wrapped to be unmodifiable.
   *
   * @param elementCount the expected size of the map.
   * @param decorator    the decorator to put the needed entries into the map before it's made unmodifiable.
   * @param <K>          the type of keys which are stored in the map.
   * @param <V>          the type of values which are stored in the map.
   * @return an unmodifiable map with the added entries from the given decorator.
   */
  @Unmodifiable
  public static @NotNull <K, V> Map<K, V> staticMap(int elementCount, @NotNull Consumer<Map<K, V>> decorator) {
    Map<K, V> map = new HashMap<>(elementCount, 1.0f);
    decorator.accept(map);
    return Collections.unmodifiableMap(map);
  }
}
