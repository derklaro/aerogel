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

package dev.derklaro.aerogel.internal.proxy;

import dev.derklaro.aerogel.internal.unsafe.UnsafeMemberAccess;
import dev.derklaro.aerogel.internal.utility.NullMask;
import dev.derklaro.aerogel.internal.utility.Preconditions;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A util for generating runtime proxies for interfaces instead of using reflection.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal")
public final class InjectionTimeProxy {

  private InjectionTimeProxy() {
    throw new UnsupportedOperationException();
  }

  /**
   * Makes a runtime proxy for the given interface class.
   *
   * @param interfaceClass the class to generate the proxy for.
   * @param <T>            the type of the class modeled.
   * @return the created proxy instance for the class.
   */
  public static @NotNull <T> ProxyMapping makeProxy(@NotNull Class<T> interfaceClass) {
    // construct the proxy
    DelegatingInvocationHandler handler = new DelegatingInvocationHandler();
    Object proxy = Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, handler);

    // return a mapping of the proxy to the handler
    return new ProxyMapping(proxy, handler);
  }

  /**
   * Checks if the given instance is a proxy.
   *
   * @param candidate the candidate to check.
   * @return true if the given candidate is a proxy, false otherwise.
   */
  public static boolean isProxy(@NotNull Object candidate) {
    return Proxy.isProxyClass(candidate.getClass());
  }

  /**
   * Represents an invocation handler which can have a delegate set to forward all calls to. If no delegate is availabe
   * the calls will fail with an exception.
   *
   * @author Pasqual K.
   * @since 2.0
   */
  @API(status = API.Status.INTERNAL, since = "2.0")
  static final class DelegatingInvocationHandler implements InvocationHandler {

    // no need to make this volatile nor lock based nor atomic, there should never
    // be two threads which are writing to this simultaneously
    Object delegate;

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
      Preconditions.checkArgument(delegate != null, "Proxy was used before a delegate is available");

      // ensure that we can call the underlying method
      Method accessibleMethod = UnsafeMemberAccess.forceMakeAccessible(method);

      try {
        // call the delegate method with the given arguments
        Object unmaskedDelegate = NullMask.unmask(delegate);
        return accessibleMethod.invoke(unmaskedDelegate, args);
      } catch (InvocationTargetException invocationException) {
        // rethrow the underlying exception
        throw invocationException.getTargetException();
      }
    }
  }
}
