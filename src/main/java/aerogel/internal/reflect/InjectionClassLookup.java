/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel.internal.reflect;

import aerogel.internal.jakarta.JakartaBridge;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class InjectionClassLookup {

  private InjectionClassLookup() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull InjectionClassData lookup(@NotNull Class<?> clazz) {
    // lookup the injectable constructor
    Constructor<?> injectableConstructor = null;
    for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
      // check for the presence of an @Inject annotation
      if (JakartaBridge.isInjectable(constructor)) {
        // this constructor is the one the injection should be done on
        injectableConstructor = constructor;
        break;
      } else if (constructor.getParameterCount() == 0) {
        // this might be a good constructor but there might be a directly annotated one - no break
        injectableConstructor = constructor;
      }
    }
    // check if a constructor was found
    Objects.requireNonNull(injectableConstructor, "No injectable constructor in class " + clazz);
    // extract all methods
    Collection<MethodHandle> allMethods = ReflectionUtils.collectMembers(
      clazz,
      $ -> true,
      Class::getDeclaredMethods,
      method -> {
        method.setAccessible(true);
        return MethodHandles.lookup().unreflect(method);
      });
    // extract all methods which are annotated as @Inject
    Collection<MethodHandle> injectMethods = ReflectionUtils.collectMembers(
      clazz,
      JakartaBridge::isInjectable,
      Class::getDeclaredMethods,
      method -> {
        method.setAccessible(true);
        return MethodHandles.lookup().unreflect(method);
      });
    // extract all fields which are annotated as @Inject
    Collection<MethodHandle> injectFields = ReflectionUtils.collectMembers(
      clazz,
      JakartaBridge::isInjectable,
      Class::getDeclaredFields,
      field -> {
        field.setAccessible(true);
        return MethodHandles.lookup().unreflectSetter(field);
      });
    // create the resulting class data
    return new InjectionClassData(
      clazz,
      injectableConstructor,
      allMethods,
      injectFields,
      injectMethods);
  }
}
