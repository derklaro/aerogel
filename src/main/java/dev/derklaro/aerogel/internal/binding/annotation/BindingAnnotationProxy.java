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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class BindingAnnotationProxy implements InvocationHandler {

  private final Class<? extends Annotation> annotationType;
  private final Map<String, Supplier<?>> propertyValueFactories;

  private transient String toString;
  private transient Integer hashCode;

  private BindingAnnotationProxy(
    @NotNull Class<? extends Annotation> annotationType,
    @NotNull Map<String, Supplier<?>> propertyValueFactories
  ) {
    this.annotationType = annotationType;
    this.propertyValueFactories = propertyValueFactories;
  }

  @SuppressWarnings("unchecked")
  public static @NotNull <A extends Annotation> A makeProxy(
    @NotNull Class<A> annotationType,
    @NotNull Map<String, Supplier<?>> propertyValueFactories
  ) {
    ClassLoader classLoader = annotationType.getClassLoader();
    BindingAnnotationProxy handler = new BindingAnnotationProxy(annotationType, propertyValueFactories);
    return (A) Proxy.newProxyInstance(classLoader, new Class[]{annotationType}, handler);
  }

  @Override
  public @NotNull Object invoke(
    @NotNull Object proxy,
    @NotNull Method method,
    @NotNull Object[] args
  ) throws Throwable {
    String methodName = method.getName();
    switch (methodName) {
      case "annotationType":
        return this.annotationType;
      case "toString":
        return this.annotationToString();
      case "hashCode":
        return this.annotationHashCode();
      case "equals":
        return this.annotationEquals(args[0]);
      default:
        Supplier<?> valueSupplier = this.propertyValueFactories.get(methodName);
        return valueSupplier.get();
    }
  }

  private @NotNull Integer annotationHashCode() {
    Integer result = this.hashCode;
    if (result == null) {
      result = 0;
      for (Map.Entry<String, Supplier<?>> memberEntry : this.propertyValueFactories.entrySet()) {
        // get the hash code of the value
        Object suppliedValue = memberEntry.getValue().get();
        int valueHash = Arrays.deepHashCode(new Object[]{suppliedValue});

        // get the hash code of the key & construct the member hash code as defined in java.lang.Annotation docs
        int keyHash = memberEntry.getKey().hashCode();
        result += (127 * keyHash) ^ (valueHash - 31);
      }
      this.hashCode = result;
    }

    return result;
  }

  private @NotNull String annotationToString() {
    String toString = this.toString;
    if (toString == null) {
      String typePrefix = "@" + this.annotationType.getName();
      StringJoiner toStringJoiner = new StringJoiner(", ", typePrefix + "(", ")");
      for (Map.Entry<String, Supplier<?>> memberEntry : this.propertyValueFactories.entrySet()) {
        // compute the toString of the return value
        Object value = memberEntry.getValue().get();
        String arrayValToString = Arrays.deepToString(new Object[]{value});
        String actualValToString = arrayValToString.substring(1, arrayValToString.length() - 1);

        // prepend the property name to the value toString
        String propertyName = memberEntry.getKey();
        String fullToStringEntry = propertyName + '=' + actualValToString;
        toStringJoiner.add(fullToStringEntry);
      }

      toString = toStringJoiner.toString();
      this.toString = toString;
    }

    return toString;
  }

  private boolean annotationEquals(@Nullable Object other) throws Exception {
    // check if it's event the same instance
    if (!this.annotationType.isInstance(other)) {
      return false;
    }

    for (Map.Entry<String, Supplier<?>> memberEntry : this.propertyValueFactories.entrySet()) {
      // get the value on the other object
      String methodName = memberEntry.getKey();
      Method methodOnOther = this.annotationType.getMethod(methodName);
      Object valueOnOther = methodOnOther.invoke(other);

      // get the value on the current object & compare
      Object valueOnCurrent = memberEntry.getValue().get();
      if (!Arrays.deepEquals(new Object[]{valueOnCurrent}, new Object[]{valueOnOther})) {
        return false;
      }
    }

    return true;
  }
}
