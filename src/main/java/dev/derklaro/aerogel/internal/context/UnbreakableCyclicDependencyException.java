/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2025 Pasqual K. and contributors
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

package dev.derklaro.aerogel.internal.context;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.internal.PassThroughException;
import java.util.Arrays;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * Exception thrown when a cyclic dependency gets detected during construction.
 *
 * @author Pasqual Koschmieder
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
final class UnbreakableCyclicDependencyException extends PassThroughException {

  /**
   * The package name to filter from the exception stack trace.
   */
  private static final String PACKAGE_PREFIX = Injector.class.getPackageName();

  public UnbreakableCyclicDependencyException(@NotNull String message) {
    super(message);

    // filter and set the stack trace of the exception
    StackTraceElement[] currentThreadStack = new RuntimeException().getStackTrace();
    for (int index = 0; index < currentThreadStack.length; index++) {
      StackTraceElement traceElement = currentThreadStack[index];
      if (!traceElement.getClassName().startsWith(PACKAGE_PREFIX)) {
        StackTraceElement[] relevantStack = Arrays.copyOfRange(currentThreadStack, index, currentThreadStack.length);
        this.setStackTrace(relevantStack);
        break;
      }
    }
  }
}
