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

package aerogel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AerogelException extends RuntimeException {

  private final boolean allowStackTrace;

  private AerogelException(String message, Throwable throwable, boolean allowStackTrace) {
    super(message, throwable);
    this.allowStackTrace = allowStackTrace;
  }

  public static @NotNull AerogelException forMessage(@NotNull String message) {
    return of(message, null, true);
  }

  public static @NotNull AerogelException forMessageWithoutStack(@NotNull String message) {
    return of(message, null, false);
  }

  public static @NotNull AerogelException forException(@NotNull Throwable exception) {
    return of(null, exception, true);
  }

  public static @NotNull AerogelException forMessagedException(@NotNull String message, @NotNull Throwable exception) {
    return of(message, exception, true);
  }

  public static @NotNull AerogelException of(
    @Nullable String message,
    @Nullable Throwable throwable,
    boolean allowStackTrace
  ) {
    return new AerogelException(message, throwable, allowStackTrace);
  }

  @Override
  public synchronized Throwable initCause(Throwable cause) {
    return this.allowStackTrace ? super.initCause(cause) : this;
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this.allowStackTrace ? super.fillInStackTrace() : this;
  }
}
