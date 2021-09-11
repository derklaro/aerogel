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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public final class InjectionClassData {

  private final Class<?> type;
  private final Constructor<?> constructor;
  private final Collection<MethodHandle> allMethods;
  private final Collection<MethodHandle> injectFields;
  private final Collection<MethodHandle> injectMethods;

  public InjectionClassData(
    Class<?> type,
    Constructor<?> injectConstructor,
    Collection<MethodHandle> allMethods,
    Collection<MethodHandle> injectFields,
    Collection<MethodHandle> injectMethods
  ) {
    this.type = type;
    this.constructor = injectConstructor;
    this.allMethods = allMethods;
    this.injectFields = injectFields;
    this.injectMethods = injectMethods;
  }

  public @NotNull Class<?> type() {
    return this.type;
  }

  public @NotNull Constructor<?> injectConstructor() {
    return this.constructor;
  }

  public @NotNull Collection<MethodHandle> allMethods() {
    return this.allMethods;
  }

  public @NotNull Collection<MethodHandle> injectFields() {
    return this.injectFields;
  }

  public @NotNull Collection<MethodHandle> injectMethods() {
    return this.injectMethods;
  }
}
