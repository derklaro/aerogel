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

package dev.derklaro.aerogel.scopedvalue;

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextScope;
import dev.derklaro.aerogel.context.LazyContextualProvider;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import java.lang.reflect.Type;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An injection context provider that uses scoped values.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
public final class ScopedValueInjectionContextProvider implements InjectionContextProvider {

  private final ScopedValue<InjectionContextScope> scopeScopedValue = ScopedValue.newInstance();

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable InjectionContextScope currentScope() {
    return this.scopeScopedValue.orElse(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull InjectionContextScope enterContextScope(
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType,
    @NotNull List<LazyContextualProvider> overrides,
    @Nullable Element associatedElement
  ) {
    InjectionContextScope currentScope = this.scopeScopedValue.orElse(null);
    if (currentScope != null) {
      if (currentScope.context().obsolete()) {
        // the current root context is obsolete, copy the necessary information from it into a new root context
        dev.derklaro.aerogel.context.InjectionContext context = currentScope.context().copyAsRoot(
          callingBinding,
          constructingType,
          overrides,
          associatedElement,
          this);
        return new ScopedValueInjectionContextScope(context, this.scopeScopedValue);
      } else {
        // we're already in an existing root context, enter a subcontext of that one
        dev.derklaro.aerogel.context.InjectionContext subcontext = currentScope.context().enterSubcontext(constructingType, callingBinding, null);
        return new ScopedValueInjectionContextScope(subcontext, this.scopeScopedValue);
      }
    } else {
      // no context yet, construct a new root context
      dev.derklaro.aerogel.context.InjectionContext context = new InjectionContext(
        callingBinding,
        constructingType,
        overrides,
        this);
      return new ScopedValueInjectionContextScope(context, this.scopeScopedValue);
    }
  }
}
