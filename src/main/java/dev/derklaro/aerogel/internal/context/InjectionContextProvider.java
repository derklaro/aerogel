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

package dev.derklaro.aerogel.internal.context;

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the injection context for the given thread and provides some convince method to enter a new root context or
 * create a subcontext of the current root context.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
public final class InjectionContextProvider {

  private static final ThreadLocal<DefaultInjectionContext> CURRENT_ROOT_CONTEXT = new ThreadLocal<>();

  private InjectionContextProvider() {
    throw new UnsupportedOperationException();
  }

  /**
   * Enters a new root context or a subcontext if a root context is already present on the current thread.
   *
   * @param callingBinding   the binding to create the context for.
   * @param constructingType the type that gets constructed by the new context.
   * @return a new root context or a subcontext of the current thread-local root context.
   */
  public static @NotNull DefaultInjectionContext enterRootContext(
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType
  ) {
    return enterRootContext(callingBinding, constructingType, Collections.emptyMap());
  }

  /**
   * Enters a new root context or a subcontext if a root context is already present on the current thread.
   *
   * @param callingBinding            the binding to create the context for.
   * @param constructingType          the type that gets constructed by the new context.
   * @param overriddenDirectInstances the overridden instances which are directly available.
   * @return a new root context or a subcontext of the current thread-local root context.
   */
  static @NotNull DefaultInjectionContext enterRootContext(
    @NotNull ContextualProvider<?> callingBinding,
    @NotNull Type constructingType,
    @NotNull Map<Element, LazyContextualProvider> overriddenDirectInstances
  ) {
    // check if a context is already present, enter a sub context in that case
    DefaultInjectionContext currentRootContext = CURRENT_ROOT_CONTEXT.get();
    if (currentRootContext != null) {
      return currentRootContext.enterSubcontext(callingBinding, constructingType);
    }

    // no context yet, construct one and set it as the root
    currentRootContext = new DefaultInjectionContext(callingBinding, constructingType, overriddenDirectInstances);
    CURRENT_ROOT_CONTEXT.set(currentRootContext);
    return currentRootContext;
  }

  /**
   * Removes the current thread local root context without doing any further checks.
   */
  public static void removeRootContext() {
    CURRENT_ROOT_CONTEXT.remove();
  }

  /**
   * Removes the root context of the current thread if the current root context is the same context as given to the
   * method (as defined by {@code ==}).
   *
   * @param expectedContext the context that should be the current root context.
   */
  public static void removeRootContext(@NotNull InjectionContext expectedContext) {
    InjectionContext currentContext = CURRENT_ROOT_CONTEXT.get();
    if (currentContext == expectedContext) {
      CURRENT_ROOT_CONTEXT.remove();
    }
  }

  /**
   * Get the current root context which is used on the current thread. If no context is used this method returns null.
   *
   * @return the current root context used on the caller thread, null if none.
   */
  public static @Nullable InjectionContext currentRootContext() {
    return CURRENT_ROOT_CONTEXT.get();
  }
}
