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

package dev.derklaro.aerogel.context;

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProviderHolder;
import java.lang.reflect.Type;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A provider for injection context scopes.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface InjectionContextProvider {

  /**
   * Gets the singleton global injection context provider for all injectors.
   *
   * @return the injection context provider for all injectors.
   */
  static @NotNull InjectionContextProvider provider() {
    return InjectionContextProviderHolder.getContextProvider();
  }

  /**
   * Get the current injection context scope, if any is bound in the current context.
   *
   * @return the current injection context scope, if any is bound in the current context.
   */
  @Nullable InjectionContextScope currentScope();

  /**
   * Enters a new subcontext or a new root context in the current execution scope.
   *
   * @param callingBinding    the binding to create the context for.
   * @param constructingType  the type that gets constructed by the new context.
   * @param overrides         the overridden instances which are directly available.
   * @param associatedElement the element for which the context is needed, null if unknown.
   * @return a subcontext or new root context for the given parameters.
   */
  @NotNull InjectionContextScope enterContextScope(
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType,
    @NotNull List<LazyContextualProvider> overrides,
    @Nullable Element associatedElement);
}
