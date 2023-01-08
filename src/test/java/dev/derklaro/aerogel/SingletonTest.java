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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.util.Qualifiers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingletonTest {

  private static int calls = 0;

  @Name("intValue")
  private static int intValue() {
    return calls++;
  }

  @Name("holder")
  private static StringHolder factoryStringHolder() {
    return new StringHolder();
  }

  @AfterEach
  void resetCalls() {
    calls = 0;
  }

  @Test
  void testConstructingSingleton() {
    Injector injector = Injector.newInjector();
    injector.install(BindingBuilder.create().toConstructing(StringHolder.class));
    injector.install(BindingBuilder.create().toFactory(SingletonTest.class, "intValue"));

    StringHolder value = injector.instance(StringHolder.class);
    Assertions.assertNotNull(value);
    Assertions.assertEquals("test", value.test);

    StringHolder value2 = injector.instance(StringHolder.class);
    Assertions.assertNotNull(value2);
    Assertions.assertEquals("test", value2.test);

    Assertions.assertSame(value, value2);
    Assertions.assertEquals(1, calls);
    Assertions.assertEquals(value.intValue, value2.intValue);
  }

  @Test
  void testFactoryMethodSingleton() {
    Injector injector = Injector.newInjector();
    injector.install(BindingBuilder.create().toFactory(SingletonTest.class, "intValue"));
    injector.install(BindingBuilder.create().toFactory(SingletonTest.class, "factoryStringHolder"));

    Element element = Element.forType(StringHolder.class).requireAnnotation(Qualifiers.named("holder"));

    StringHolder holderA = injector.instance(element);
    Assertions.assertNotNull(holderA);

    StringHolder holderB = injector.instance(element);
    Assertions.assertNotNull(holderB);

    Assertions.assertSame(holderA, holderB);
    Assertions.assertEquals(1, calls);
    Assertions.assertEquals(holderA.intValue, holderB.intValue);
  }

  @Singleton
  private static final class StringHolder {

    private final String test = "test";

    @Inject
    @Name("intValue")
    private int intValue; // ensures that member injection is only done once for singletons
  }
}
