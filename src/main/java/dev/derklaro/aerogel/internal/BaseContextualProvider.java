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

package dev.derklaro.aerogel.internal;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.InjectionContext;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.internal.context.holder.InjectionContextHolder;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract implementation of a contextual provider which simplifies the use of providers for downstream api
 * consumers. This provider implementation ensures that:
 * <ol>
 *   <li>calls to the {@link #get()} method are always executed within an injection context.
 *   <li>no calls to construct done are made when this provider is wrapped by a downstream provider.
 * </ol>
 *
 * @param <T> the type constructed by this provider.
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0", consumers = "dev.derklaro.aerogel.internal.*")
public abstract class BaseContextualProvider<T> implements ContextualProvider<T> {

  protected final Injector injector;
  protected boolean hasUpstreamProvider;

  /**
   * Constructs a new base contextual provider instance.
   *
   * @param injector the injector associated with this provider.
   */
  protected BaseContextualProvider(@NotNull Injector injector) {
    this.injector = injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector injector() {
    return this.injector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull ContextualProvider<T> asUpstreamContext() {
    this.hasUpstreamProvider = true;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable T get() throws AerogelException {
    // enter the injection context
    InjectionContext context = InjectionContextHolder.enter();

    try {
      // call get to method which takes a context
      T result = this.get(context);

      // leave the current context and ensure that the construct completed if we were the last one to leave
      if (InjectionContextHolder.leave()) {
        context.ensureComplete();
      }

      // return the constructed value
      return result;
    } catch (Exception exception) {
      // force leave the current context and rethrow the exception
      InjectionContextHolder.forceLeave();
      throw AerogelException.forException(exception);
    }
  }

  /**
   * Calls the construct done method on the given injection context unless this provider is wrapped by a downstream
   * provider.
   *
   * @param context           the context to call the construct done method on.
   * @param elements          the elements handled by this provider.
   * @param constructed       the constructed value of this provider.
   * @param allowMemberInject if member injection should be done.
   * @throws NullPointerException if the given context or elements array is null.
   */
  protected void callConstructDone(
    @NotNull InjectionContext context,
    @NotNull Element[] elements,
    @Nullable Object constructed,
    boolean allowMemberInject,
    boolean allowStore
  ) {
    if (!this.hasUpstreamProvider) {
      context.constructDone(elements, this.injector, constructed, allowMemberInject, allowStore);
    }
  }
}
