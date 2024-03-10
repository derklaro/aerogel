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

package dev.derklaro.aerogel.internal.context;

import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.internal.util.NullMask;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A constructed proxy for a specific binding which can be delegated to a concrete implementation once constructed. This
 * is used to break up circular referenced during injection.
 *
 * @author Pasqual Koschmieder
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
final class InjectionTimeProxy {

  public final Object proxy;
  public final InstalledBinding<?> binding;

  private final DelegatingInvocationHandler invocationHandler;

  private Runnable removeListener;

  /**
   * Constructs a new injection time proxy instance. For internal use only. Instances should only be constructed using
   * {@link #make(Class, Runnable, InstalledBinding)}.
   *
   * @param proxy             the created proxy instance.
   * @param removeListener    the callback to execute when the delegate gets set.
   * @param binding           the binding for which the proxy gets created.
   * @param invocationHandler the invocation handler for the proxy instance.
   */
  private InjectionTimeProxy(
    @NotNull Object proxy,
    @NotNull Runnable removeListener,
    @NotNull InstalledBinding<?> binding,
    @NotNull DelegatingInvocationHandler invocationHandler
  ) {
    this.proxy = proxy;
    this.binding = binding;

    this.removeListener = removeListener;
    this.invocationHandler = invocationHandler;
  }

  /**
   * Creates a proxy instance for the given interface type.
   *
   * @param interfaceClass the interface type to proxy.
   * @param removeListener the listener to execute when the delegate instance is present.
   * @param binding        the binding to which the proxy belongs.
   * @return a wrapper around the constructed proxy instance.
   */
  public static @NotNull InjectionTimeProxy make(
    @NotNull Class<?> interfaceClass,
    @NotNull Runnable removeListener,
    @NotNull InstalledBinding<?> binding
  ) {
    DelegatingInvocationHandler handler = new DelegatingInvocationHandler();
    Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, handler);
    return new InjectionTimeProxy(proxy, removeListener, binding, handler);
  }

  /**
   * Get if the underlying invocation handler has no concrete target to call yet.
   *
   * @return true if the underlying invocation handler has no concrete target to call yet, false otherwise.
   */
  public boolean undelegated() {
    return this.invocationHandler.delegate == null;
  }

  /**
   * Sets the delegate of this proxy mapping stored in this mapping unless the delegate is already present.
   *
   * @param delegate the delegate to use for the created proxy.
   */
  public void setDelegate(@Nullable Object delegate) {
    if (this.invocationHandler.delegate == null) {
      this.invocationHandler.delegate = NullMask.mask(delegate);
    }
  }

  /**
   * Executes the remove listener passed during construction and removes the listener instance. Subsequent calls to this
   * method will not do anything.
   */
  public void executeRemoveListener() {
    Runnable removeListener = this.removeListener;
    if (removeListener != null) {
      this.removeListener = null;
      removeListener.run();
    }
  }

  /**
   * The invocation handler for proxy instances which can be delegated to a concrete implementation of the class, once
   * the construction was possible.
   *
   * @author Pasqual Koschmieder
   * @since 2.0
   */
  @API(status = API.Status.INTERNAL, since = "2.0")
  static final class DelegatingInvocationHandler implements InvocationHandler {

    // should only be written to once
    private Object delegate;

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable Object invoke(
      @NotNull Object proxy,
      @NotNull Method method,
      @NotNull Object[] args
    ) throws Throwable {
      // ensure that the proxy has a delegate
      Object delegate = this.delegate;
      if (delegate == null) {
        throw new IllegalStateException("injection proxy access before construction completion");
      }

      // ensure the visibility in case the method is defined in an interface that
      // is not exposed to us directly. This might however fail, in case the access
      // permissions for modules are not set up properly...
      method.setAccessible(true);

      try {
        // call the delegate method with the given arguments
        Object unmaskedDelegate = NullMask.unmask(delegate);
        return method.invoke(unmaskedDelegate, args);
      } catch (InvocationTargetException invocationException) {
        // rethrow the underlying exception
        throw invocationException.getTargetException();
      }
    }
  }
}
