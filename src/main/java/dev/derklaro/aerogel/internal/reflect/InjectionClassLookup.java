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

package dev.derklaro.aerogel.internal.reflect;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.internal.jakarta.JakartaBridge;
import dev.derklaro.aerogel.Inject;
import java.lang.reflect.Constructor;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class to find an injectable constructor in a class.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class InjectionClassLookup {

  private InjectionClassLookup() {
    throw new UnsupportedOperationException();
  }

  /**
   * Finds the only injectable constructor of the class. An injectable constructor must be unique and annotated as
   * {@link Inject}. If no constructor in the class is annotated the no-args constructor will be used when
   * available.
   *
   * @param clazz the class to find an injectable constructor in.
   * @return the injectable constructor of the class.
   * @throws AerogelException if zero or more than one injectable constructor was found in the given class.
   */
  public static @NotNull Constructor<?> findInjectableConstructor(@NotNull Class<?> clazz) {
    // prevents copy of the constructors array
    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    // the target constructor we should use for injection
    Constructor<?> injectionPoint = null;
    // loop over the constructors - look for the constructors which are annotated as @Inject
    for (Constructor<?> constructor : constructors) {
      // check if the constructor is annotated as @Inject
      if (JakartaBridge.isInjectable(constructor)) {
        // check if we already found a constructor - only one is allowed per class
        if (injectionPoint != null) {
          throw AerogelException.forMessage(
            "Class " + clazz.getName() + " declared multiple constructors which are annotated as @Inject.");
        }
        // the constructor is our injection point
        injectionPoint = constructor;
      }
    }
    // check if we found an injectable constructor
    if (injectionPoint == null) {
      // no constructor was found yet - check for a constructor with 0 arguments
      for (Constructor<?> constructor : constructors) {
        if (constructor.getParameterCount() == 0) {
          // there might only be one constructor without a parameter - assign and break
          injectionPoint = constructor;
          break;
        }
      }
    }
    // check if we now have an injection point - throw an exception if not
    if (injectionPoint == null) {
      throw AerogelException.forMessage("No injectable constructor in class " + clazz.getName());
    }
    // the constructor we filtered out is our injection point
    return injectionPoint;
  }
}
