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

package dev.derklaro.aerogel.internal.context.scope.threadlocal;

import dev.derklaro.aerogel.internal.context.InjectionContext;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextScope;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * An injection context scope that uses thread-locals to set the context of the current scope.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
final class ThreadLocalInjectionContextScope implements InjectionContextScope {

  private final InjectionContext context;
  private final ThreadLocal<InjectionContextScope> scopeThreadLocal;

  /**
   * Constructs a new thread local injection context scope.
   *
   * @param context          the context that is wrapped in this scope.
   * @param scopeThreadLocal the thread local that is used by the context provider.
   */
  public ThreadLocalInjectionContextScope(
    @NotNull InjectionContext context,
    @NotNull ThreadLocal<InjectionContextScope> scopeThreadLocal
  ) {
    this.context = context;
    this.scopeThreadLocal = scopeThreadLocal;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContext context() {
    return this.context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T executeScoped(@NotNull Supplier<T> operation) {
    InjectionContextScope currentScope = this.scopeThreadLocal.get();
    if (currentScope == null || currentScope.context().obsolete()) {
      // there is either no context currently bound or the current bound snapshot is obsolete
      // we're using our own context for further actions
      return this.forceExecuteScoped(operation, currentScope);
    } else {
      // the current scope is still valid, use that one
      return operation.get();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> @UnknownNullability T forceExecuteScoped(@NotNull Supplier<T> operation) {
    InjectionContextScope currentScope = this.scopeThreadLocal.get();
    return this.forceExecuteScoped(operation, currentScope);
  }

  /**
   * Overrides the current thread local with this scope, resetting the thread-local to the old scope. If the old scope
   * is null, then the value is removed from the thread-local.
   *
   * @param operation the operation to execute after the thread-local was set to this scope.
   * @param previous  the current scope that should be reset to after executing the given operation.
   * @param <T>       the type of result returned by the given operation.
   * @return the result of the given operation.
   */
  private <T> @Nullable T forceExecuteScoped(
    @NotNull Supplier<T> operation,
    @Nullable InjectionContextScope previous
  ) {
    try {
      // update the thread local to use this scope, then call the given operation
      this.scopeThreadLocal.set(this);
      return operation.get();
    } finally {
      // reset to the previous scope if present, else remove the mapping to the current scope
      if (previous != null) {
        this.scopeThreadLocal.set(previous);
      } else {
        this.scopeThreadLocal.remove();
      }
    }
  }
}
