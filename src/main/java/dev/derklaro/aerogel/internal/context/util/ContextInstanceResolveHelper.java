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

package dev.derklaro.aerogel.internal.context.util;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.binding.BindingHolder;
import dev.derklaro.aerogel.internal.PassthroughException;
import dev.derklaro.aerogel.internal.context.InjectionContextProvider;
import java.lang.reflect.Type;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A small helper class to make working with instances resolves of contexts easier.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
public final class ContextInstanceResolveHelper {

  private ContextInstanceResolveHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Resolves the instances of the requested type using the provider provided by the given binding holder. If an
   * injection context is present on the current thread, a subcontext of the root is created to resolve the value, in
   * all other cases a new context is created and set.
   *
   * @param element       the requested element for which the provider should provide the value.
   * @param bindingHolder the binding that is associated with the given requested type.
   * @return the resolved instance of the given type.
   * @throws AerogelException if an exception occurs while resolving the instance.
   */
  public static @Nullable Object resolveInstance(@NotNull Element element, @NotNull BindingHolder bindingHolder) {
    return resolveInstance(element.componentType(), bindingHolder.provider(element));
  }

  /**
   * Resolves the instances of the requested type using the given provider. If an injection context is present on the
   * current thread, a subcontext of the root is created to resolve the value, in all other cases a new context is
   * created and set.
   *
   * @param requestedType the type requested for injection.
   * @param provider      the provider that is associated with the given requested type.
   * @return the resolved instance of the given type.
   * @throws AerogelException if an exception occurs while resolving the instance.
   */
  public static @Nullable Object resolveInstance(@NotNull Type requestedType, @NotNull ContextualProvider<?> provider) {
    // enter the injection context
    InjectionContext context = InjectionContextProvider.enterRootContext(provider, requestedType);
    return resolveInstanceAndRemoveContext(context);
  }

  /**
   * Resolves the value of the given injection context. If the given context is a root context, the construction is
   * finished (that ensures that no more circular proxies without a delegate are present and that member injection
   * requests are executed). If the given context is a root context and the current thread local context, the context is
   * removed from the current thread after construction finished.
   *
   * @param context the context to resolve the value of.
   * @return the resolved value of the context.
   * @throws AerogelException if an exception occurs while resolving the instance.
   */
  public static @Nullable Object resolveInstanceAndRemoveContext(@NotNull InjectionContext context) {
    try {
      // call get to method which takes a context
      Object result = context.resolveInstance();

      // leave the current context if we entered the root context
      // in any other case, just leave the finish call up to the root creator
      if (context.rootContext()) {
        context.finishConstruction();
      }

      // return the constructed value
      return result;
    } catch (Exception exception) {
      // force remove the root context, leave the context open in any other case
      // some downstream providers might handle the thrown exception gracefully and
      // resume the construction which just failed
      if (context.rootContext()) {
        InjectionContextProvider.removeRootContext(context);
      }

      // don't re-wrap wrapped or passthrough exceptions
      PassthroughException.rethrow(exception);

      // rethrow the exception, wrapped
      throw AerogelException.forException(exception);
    }
  }
}
