/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel.internal.utility;

import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ToStringHelper {

  private final StringBuilder stringBuilder;

  public ToStringHelper(@NotNull Object instance) {
    this.stringBuilder = new StringBuilder(instance.getClass().getSimpleName()).append("(");
  }

  public static @NotNull ToStringHelper from(@NotNull Object instance) {
    return new ToStringHelper(instance);
  }

  public @NotNull ToStringHelper putCollection(@NotNull String name, @Nullable Collection<?> value) {
    // check if the collection is empty or null - use an empty array separator then
    if (value == null || value.isEmpty()) {
      return this.putField(name, "[]");
    } else {
      // join the elements of the array together
      StringBuilder builder = new StringBuilder().append('[');
      for (Object entry : value) {
        // skip null objects - we don't need them in the toString value
        if (entry != null) {
          builder.append(entry).append(", ");
        }
      }
      // check if there were more than 1 non-null element
      if (builder.length() == 1) {
        // no elements added to the builder - treat as an empty collection
        return this.putField(name, "[]");
      } else {
        // remove the last ', ' and add the closing ']'
        return this.putField(name, builder.delete(builder.length() - 2, builder.length()).append(']').toString());
      }
    }
  }

  public @NotNull ToStringHelper putField(@NotNull String name, @Nullable Object value) {
    this.stringBuilder.append(name).append('=').append(value).append(", ");
    return this;
  }

  public @NotNull String toString() {
    // remove the last comma if there is one
    int lastFieldIndex = this.stringBuilder.lastIndexOf(",");
    if (lastFieldIndex != -1) {
      this.stringBuilder.delete(lastFieldIndex, this.stringBuilder.length());
    }
    // complete the toString generation
    return this.stringBuilder.append(")").toString();
  }
}
