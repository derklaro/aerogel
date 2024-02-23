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

import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextScope;
import jakarta.inject.Provider;
import java.util.Map;
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
  public @Nullable InjectionContextScope currentScope() {
    return this.scopeThreadLocal.get();
  }

  @Override
  public @NotNull InjectionContextScope enterContextScope(
    @NotNull InstalledBinding<?> binding,
    @NotNull Map<BindingKey<?>, Provider<?>> overrides
  ) {
    InjectionContextScope currentScope = this.scopeThreadLocal.get();
    if (currentScope != null) {
      InjectionContext currentContext = currentScope.context();
      if (currentScope.context().obsolete()) {
        // the current root context is obsolete, copy the necessary information from it into a new root context
        InjectionContext newContext = currentContext.copyAsRoot(binding, overrides, this);
        return new ThreadLocalInjectionContextScope(newContext, this.scopeThreadLocal);
      } else {
        // we're already in an existing root context, enter a subcontext of that one
        InjectionContext subcontext = currentContext.enterSubcontext(binding);
        return new ThreadLocalInjectionContextScope(subcontext, this.scopeThreadLocal);
      }
    } else {
      // no context yet, construct a new root context
      InjectionContext newContext = new InjectionContext(binding, overrides, this);
      return new ThreadLocalInjectionContextScope(newContext, this.scopeThreadLocal);
    }
  }
}
