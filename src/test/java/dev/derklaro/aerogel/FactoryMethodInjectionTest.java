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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.binding.BindingBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FactoryMethodInjectionTest {

  @Singleton
  private static Object constructObject() {
    return new Object();
  }

  private static SomeClass constructSomeClass() {
    return new SomeClass();
  }

  private static SomeSingletonClass constructSomeSingletonClass() {
    return new SomeSingletonClass();
  }

  @Test
  void testFactoryMethodDirectScopesAreApplied() {
    Injector injector = Injector.newInjector();
    injector.install(BindingBuilder.create().toFactory(FactoryMethodInjectionTest.class, "constructObject"));

    // get the instance two times
    Object valueA = injector.instance(Object.class);
    Object valueB = injector.instance(Object.class);

    // must be the same
    Assertions.assertNotNull(valueA);
    Assertions.assertSame(valueA, valueB);
  }

  @Test
  void testFactoryMethodRespectsSingletonReturnType() {
    Injector injector = Injector.newInjector();
    injector.install(
      BindingBuilder.create().toFactory(FactoryMethodInjectionTest.class, "constructSomeSingletonClass"));

    // get the instance two times
    SomeSingletonClass valueA = injector.instance(SomeSingletonClass.class);
    SomeSingletonClass valueB = injector.instance(SomeSingletonClass.class);

    // must be the same
    Assertions.assertNotNull(valueA);
    Assertions.assertSame(valueA, valueB);
  }

  @Test
  void testFactoryMethodRespectsNonSingleton() {
    Injector injector = Injector.newInjector();
    injector.install(
      BindingBuilder.create().toFactory(FactoryMethodInjectionTest.class, "constructSomeClass"));

    // get the instance two times
    SomeClass valueA = injector.instance(SomeClass.class);
    SomeClass valueB = injector.instance(SomeClass.class);

    // must be the same
    Assertions.assertNotNull(valueA);
    Assertions.assertNotNull(valueB);
    Assertions.assertNotSame(valueA, valueB);
  }

  private static final class SomeClass {

  }

  @Singleton
  private static final class SomeSingletonClass {

  }
}
