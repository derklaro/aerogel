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

package dev.derklaro.aerogel.internal.invoke;

import java.util.Objects;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An exception which indicates that the value of something was already constructed and there is no further need to
 * resolve (or construct) other values. The constructed value is available via {@link #constructedValue()}.
 *
 * <p>This exception will neither accept a cause nor init the stack trace, as this exception should just be used for
 * indication and never for actual tracing.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
public final class ConstructedValueException extends RuntimeException {

  private final Object constructed;

  /**
   * Constructs a new constructed value exception instance.
   *
   * @param constructed the value that was constructed.
   * @throws NullPointerException if the given constructed value is null.
   */
  public ConstructedValueException(@NotNull Object constructed) {
    this.constructed = Objects.requireNonNull(constructed, "constructed");
  }

  /**
   * Get the value that was constructed and the cause why this exception was thrown.
   *
   * @return the value that was constructed.
   */
  public @NotNull Object constructedValue() {
    return this.constructed;
  }

  /**
   * This method is a no-op as this exception is only a marker and should not get used for tracing.
   *
   * @param cause the cause to initialize, ignored for this exception implementation.
   * @return this exception with no changes applied.
   */
  @Override
  public @NotNull Throwable initCause(@Nullable Throwable cause) {
    return this;
  }

  /**
   * This method is a no-op as this exception is only a marker and should not get used for tracing.
   *
   * @return this exception with no changes applied.
   */
  @Override
  public @NotNull Throwable fillInStackTrace() {
    return this;
  }
}
