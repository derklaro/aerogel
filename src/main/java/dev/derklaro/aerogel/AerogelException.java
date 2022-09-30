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

package dev.derklaro.aerogel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The core exception wrapping any message or other exception thrown when working with aerogel.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class AerogelException extends RuntimeException {

  private final boolean allowStackTrace;

  /**
   * Creates a new aerogel exception instance.
   *
   * @param message         the message of the exception.
   * @param throwable       the cause of the exception.
   * @param allowStackTrace if a stack trace should be appended when throwing the exception.
   */
  private AerogelException(String message, Throwable throwable, boolean allowStackTrace) {
    super(message, throwable);
    this.allowStackTrace = allowStackTrace;
  }

  /**
   * Creates a new aerogel exception with the given message and enabled stack trace.
   *
   * @param message the message of the exception.
   * @return the created exception instance.
   */
  public static @NotNull AerogelException forMessage(@NotNull String message) {
    return of(message, null, true);
  }

  /**
   * Creates a new aerogel exception with the given message and disabled stack trace.
   *
   * @param message the message of the exception.
   * @return the created exception instance.
   */
  public static @NotNull AerogelException forMessageWithoutStack(@NotNull String message) {
    return of(message, null, false);
  }

  /**
   * Creates a new aerogel exception with the given cause and enabled stack trace.
   *
   * @param exception the cause of the exception.
   * @return the created exception instance.
   */
  public static @NotNull AerogelException forException(@NotNull Throwable exception) {
    return of(null, exception, true);
  }

  /**
   * Creates a new aerogel exception with the given message and cause. Stacktrace is enabled.
   *
   * @param message   the message of the exception.
   * @param exception the cause of the exception.
   * @return the created exception instance.
   */
  public static @NotNull AerogelException forMessagedException(@NotNull String message, @NotNull Throwable exception) {
    return of(message, exception, true);
  }

  /**
   * Creates a new aerogel exception instance.
   *
   * @param message         the message of the exception.
   * @param throwable       the cause of the exception.
   * @param allowStackTrace if a stack trace should be appended when throwing the exception.
   * @return the created exception instance.
   */
  public static @NotNull AerogelException of(
    @Nullable String message,
    @Nullable Throwable throwable,
    boolean allowStackTrace
  ) {
    return new AerogelException(message, throwable, allowStackTrace);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Throwable initCause(Throwable cause) {
    return this.allowStackTrace ? super.initCause(cause) : this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this.allowStackTrace ? super.fillInStackTrace() : this;
  }
}
