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

package dev.derklaro.aerogel.internal.binding.annotation;

import dev.derklaro.aerogel.internal.util.PrimitiveUtil;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BuilderAnnotationProxy<A extends Annotation> implements InvocationHandler {

  private A proxyInstance;
  private String lastAccessedProperty;

  private BuilderAnnotationProxy() {
  }

  @SuppressWarnings("unchecked")
  public static @NotNull <A extends Annotation> BuilderAnnotationProxy<A> make(@NotNull Class<A> annotationType) {
    ClassLoader classLoader = annotationType.getClassLoader();
    BuilderAnnotationProxy<A> handler = new BuilderAnnotationProxy<>();
    handler.proxyInstance = (A) Proxy.newProxyInstance(classLoader, new Class[]{annotationType}, handler);
    return handler;
  }

  @Override
  public @Nullable Object invoke(
    @NotNull Object proxy,
    @NotNull Method method,
    @NotNull Object[] args
  ) throws Throwable {
    this.lastAccessedProperty = method.getName();

    // for primitives a default value must be returned, everything else can be null
    Class<?> rt = method.getReturnType();
    if (rt.isPrimitive()) {
      return PrimitiveUtil.defaultValue(rt);
    } else {
      return null;
    }
  }

  public @NotNull A proxyInstance() {
    return this.proxyInstance;
  }

  public @Nullable String getAndRemoveLastAccessedProperty() {
    String prop = this.lastAccessedProperty;
    this.lastAccessedProperty = null;
    return prop;
  }
}
