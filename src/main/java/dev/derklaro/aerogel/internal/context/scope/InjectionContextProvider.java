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

package dev.derklaro.aerogel.internal.context.scope;

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.context.InjectionContext;
import dev.derklaro.aerogel.context.InjectionContextScope;
import dev.derklaro.aerogel.internal.context.LazyContextualProvider;
import java.lang.reflect.Type;
import java.util.List;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A provider for injection context scopes.
 * <p>
 * Note: while this class is marked as internal, you can override it and implement your own unsupported scope handling.
 * To do this, just create your own context provider implementation and register it into the service provider
 * interface.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0", consumers = "dev.derklaro.aerogel.*")
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
   * Removes the current context scope, if possible. If the underlying scope handling does not allow for removal of the
   * current context, this method does nothing and just returns false.
   *
   * @return true if the underlying context existed and was removed, false otherwise.
   */
  default boolean removeContextScope() {
    return this.removeContextScope(null);
  }

  /**
   * Removes the current context scope, if its wrapped context matches the given one and removal is possible. If the
   * underlying scope handling does not allow for removal of the current context, this method does nothing and just
   * returns false.
   *
   * @param expectedContext the context that is expected, can be null to indicate that no context is expected.
   * @return true if the underlying context existed and was removed, false otherwise.
   */
  boolean removeContextScope(@Nullable InjectionContext expectedContext);

  /**
   * Get the current injection context scope, if any is bound in the current context.
   *
   * @return the current injection context scope, if any is bound in the current context.
   */
  @Nullable InjectionContextScope currentScope();

  /**
   * Enters a new subcontext or a new root context in the current execution scope.
   *
   * @param callingBinding            the binding to create the context for.
   * @param constructingType          the type that gets constructed by the new context.
   * @param overriddenDirectInstances the overridden instances which are directly available.
   * @return a subcontext or new root context for the given parameters.
   */
  @NotNull InjectionContextScope enterContextScope(
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType,
    @NotNull List<LazyContextualProvider> overriddenDirectInstances);
}
