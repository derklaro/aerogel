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

package dev.derklaro.aerogel.internal.util;

import dev.derklaro.aerogel.internal.reflect.TypeUtil;
import java.util.Collection;
import java.util.StringJoiner;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A small utility class to easily build a {@code toString} method response for any class.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel")
public final class ToStringHelper {

  /**
   * The prefix prepend to the final toString value.
   */
  private final String prefix;
  /**
   * The string builder to write the information of the class to.
   */
  private final StringJoiner stringJoiner;

  /**
   * Constructs a new builder based on the given {@code instance}.
   *
   * @param instance the instance for which the toString method result will be created.
   */
  private ToStringHelper(@NotNull Object instance) {
    this.prefix = TypeUtil.toPrettyString(instance.getClass());
    this.stringJoiner = new StringJoiner(", ", "(", ")");
  }

  /**
   * Code style flow helper for creating a new builder based on the given {@code instance}.
   *
   * @param instance the instance for which the toString method result will be created.
   * @return the constructed builder.
   */
  public static @NotNull ToStringHelper create(@NotNull Object instance) {
    return new ToStringHelper(instance);
  }

  /**
   * Puts a collection into this builder, all entries of the collection will be stringified and added to this builder.
   *
   * @param name  the name of the collection's origin field.
   * @param value the actual collection which should get written into the builder.
   * @return the same instance as used to call this method, for chaining.
   */
  public @NotNull ToStringHelper putCollection(@NotNull String name, @Nullable Collection<?> value) {
    // check if the collection is empty or null - use an empty array separator then
    if (value == null || value.isEmpty()) {
      return this.putField(name, "[]");
    } else {
      // join the elements of the array together
      StringJoiner joiner = new StringJoiner(", ", "[", "]");
      for (Object entry : value) {
        // skip null objects - we don't need them in the toString value
        if (entry != null) {
          joiner.add(entry.toString());
        }
      }

      // put the joined value
      return this.putField(name, joiner.toString());
    }
  }

  /**
   * Puts the object stringified into this builder.
   *
   * @param name  the name of the object's origin field.
   * @param value the object to put into this builder.
   * @return the same instance as used to call this method, for chaining.
   */
  public @NotNull ToStringHelper putField(@NotNull String name, @Nullable Object value) {
    String toStringValue = String.format("%s=%s", name, value);
    this.stringJoiner.add(toStringValue);

    return this;
  }

  /**
   * Completes the operation and builds a full {@code toString} method result from this builder. A builder can be
   * re-used after a call to this method.
   *
   * @return the created {@code toString} method result based on this builder.
   */
  public @NotNull String toString() {
    return this.prefix + this.stringJoiner.toString();
  }
}
