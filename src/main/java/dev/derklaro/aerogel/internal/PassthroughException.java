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

package dev.derklaro.aerogel.internal;

import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an exception which, when caught, should be rethrown as-is without further wrapping. This exception type
 * has never a stacktrace available.
 *
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0")
public class PassthroughException extends RuntimeException {

  /**
   * Constructs a new instance of this exception, protected to ensure that only overrides are possible.
   */
  protected PassthroughException() {
  }

  /**
   * Constructs a new instance of this exception, protected to ensure that only overrides are possible.
   *
   * @param cause the cause which should be available for later retrieval.
   */
  protected PassthroughException(@NotNull Throwable cause) {
    super(cause);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Throwable fillInStackTrace() {
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Throwable initCause(@Nullable Throwable cause) {
    return this;
  }
}
