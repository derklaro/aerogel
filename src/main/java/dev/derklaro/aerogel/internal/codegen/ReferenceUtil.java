/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 Pasqual K. and contributors
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

package dev.derklaro.aerogel.internal.codegen;

import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A util to work with references in generated source code.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@SuppressWarnings("unused") // called from generated source code
public final class ReferenceUtil {

  private static final Object NULL = new Object();

  private ReferenceUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Stores the given value in the given reference, if the given reference has no value present yet. If a value is
   * present, the existing value will get wrapped and returned, with member injection disabled.
   *
   * @param reference the reference to store the given value in.
   * @param value     the value to store in the reference.
   * @return an instance create result containing the old or given value.
   */
  public static @NotNull InstanceCreateResult storeAndPack(
    @NotNull AtomicReference<Object> reference,
    @Nullable Object value
  ) {
    // set the value if no value is already present
    Object masked = mask(value);
    if (reference.compareAndSet(null, masked)) {
      return new InstanceCreateResult(value, true);
    }

    // a value is already present
    Object storedValue = reference.get();
    return new InstanceCreateResult(unmask(storedValue), false);
  }

  /**
   * Unpacks the given masked value and creates a new instance create result which has member injection disabled.
   *
   * @param masked the masked value to unmask.
   * @return an instance create result containing the unmasked form of the given value.
   */
  public static @NotNull InstanceCreateResult unmaskAndPack(@NotNull Object masked) {
    Object unmasked = unmask(masked);
    return new InstanceCreateResult(unmasked, false);
  }

  /**
   * Masks the given value if null, returns the same value otherwise.
   *
   * @param value the value to mask.
   * @return a mask if the given value is null, the given value otherwise.
   */
  private static @NotNull Object mask(@Nullable Object value) {
    return value != null ? value : NULL;
  }

  /**
   * Unmasks the given value if the masked value is marked as null, returns the same value otherwise.
   *
   * @param masked the value to unmask.
   * @return the unmasked value, or the given value if not masked.
   */
  private static @Nullable Object unmask(@NotNull Object masked) {
    return masked == NULL ? null : masked;
  }
}
