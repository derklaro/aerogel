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

package dev.derklaro.aerogel.internal.context.scope.threadlocal;

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.context.InjectionContext;
import dev.derklaro.aerogel.context.InjectionContextScope;
import dev.derklaro.aerogel.internal.context.DefaultInjectionContext;
import dev.derklaro.aerogel.internal.context.LazyContextualProvider;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import java.lang.reflect.Type;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of the injection context provider which uses thread-locals to control scopes.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0", consumers = "dev.derklaro.aerogel.internal.context.scope")
public final class ThreadLocalInjectionContextProvider implements InjectionContextProvider {

  private final ThreadLocal<InjectionContextScope> scopeThreadLocal = new ThreadLocal<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeContextScope(@Nullable InjectionContext expectedContext) {
    // just remove the local context if there is no context we're expecting
    if (expectedContext == null) {
      this.scopeThreadLocal.remove();
      return true;
    }

    // check if there is a current context, if not we can just return
    InjectionContextScope currentScope = this.scopeThreadLocal.get();
    if (currentScope == null) {
      return true;
    }

    // if the current context is the expected, remove the context
    if (currentScope.context() == expectedContext) {
      this.scopeThreadLocal.remove();
      return true;
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InjectionContextScope currentScope() {
    return this.scopeThreadLocal.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContextScope enterContextScope(
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType,
    @NotNull List<LazyContextualProvider> overriddenDirectInstances
  ) {
    InjectionContextScope currentScope = this.scopeThreadLocal.get();
    if (currentScope != null && !currentScope.context().obsolete()) {
      // we're already in a root context, enter a subcontext of that one
      InjectionContext subcontext = currentScope.context().enterSubcontext(constructingType, callingBinding, null);
      return new ThreadLocalInjectionContextScope(subcontext, this.scopeThreadLocal);
    } else {
      // no context yet, construct a new root context
      InjectionContext context = new DefaultInjectionContext(
        callingBinding,
        constructingType,
        overriddenDirectInstances,
        this);
      return new ThreadLocalInjectionContextScope(context, this.scopeThreadLocal);
    }
  }
}
