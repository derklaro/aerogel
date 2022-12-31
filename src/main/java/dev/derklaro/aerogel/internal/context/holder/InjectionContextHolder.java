/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 Pasqual K. and contributors
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

package dev.derklaro.aerogel.internal.context.holder;

import dev.derklaro.aerogel.InjectionContext;
import java.util.function.Supplier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A holder for an injection context based on the current calling thread.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal")
public final class InjectionContextHolder {

  private static final ThreadLocal<ContextInfo> CONTEXT_HOLDER = new ThreadLocal<>();
  private static final Supplier<InjectionContext> DEFAULT_CONTEXT_SUPPLIER = () -> InjectionContext.builder().build();

  private InjectionContextHolder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Enters the given injection context, creating a new empty one if the current thread has no context available yet.
   *
   * @return the claimed injection context for the current thread.
   */
  public static @NotNull InjectionContext enter() {
    return enter(DEFAULT_CONTEXT_SUPPLIER);
  }

  /**
   * Enters the given injection context, creating a new empty one if the current thread has no context available yet.
   *
   * @param defaultSupplier the factory to create a new injection context if needed.
   * @return the claimed injection context for the current thread.
   */
  public static @NotNull InjectionContext enter(@NotNull Supplier<InjectionContext> defaultSupplier) {
    // check if a context is already present, use that one in that case
    ContextInfo contextInfo = CONTEXT_HOLDER.get();
    if (contextInfo == null) {
      // create and set a new context
      contextInfo = new ContextInfo(defaultSupplier.get());
      CONTEXT_HOLDER.set(contextInfo);
    }

    return contextInfo.claim().context();
  }

  /**
   * Leaves the current context (releases it by one) and removes the context from the thread if no other locations have
   * claimed the context.
   *
   * @return true if this call removed the last claim (and the context for the current thread), false otherwise.
   */
  public static boolean leave() {
    // check if the context is present and release it by one. If there are no more claims the
    // context is removed completely
    ContextInfo contextInfo = CONTEXT_HOLDER.get();
    if (contextInfo != null && contextInfo.release()) {
      CONTEXT_HOLDER.remove();
      return true;
    } else {
      // context not removed
      return false;
    }
  }

  /**
   * Force removes the context from the current thread.
   */
  public static void forceLeave() {
    CONTEXT_HOLDER.remove();
  }
}
