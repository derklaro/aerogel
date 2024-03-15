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

package dev.derklaro.aerogel.internal.context.scope;

import dev.derklaro.aerogel.internal.context.InjectionContext;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * A scope of an injection context returned by a context provider.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
public interface InjectionContextScope {

  /**
   * The context that is wrapped by this context scope.
   *
   * @return the wrapped injection context.
   */
  @NotNull
  InjectionContext context();

  /**
   * Executes the given operation using the injection context of this scope. This method overrides the current scoped
   * context for the lifetime of the operation and resets it to the previous value afterward.
   *
   * @param operation the operation to execute.
   * @param <T>       the type of value returned by the given operation.
   * @return the value returned by the given operation.
   * @throws NullPointerException if the given operation is null.
   */
  @UnknownNullability
  <T> T executeScoped(@NotNull Supplier<T> operation);
}
