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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BindingsTest {

  @Test
  void testFixedBinding() {
    Injector injector = Injector.newInjector();
    injector.install(BindingBuilder.create().bind(String.class).toInstance("Test"));

    String test = injector.instance(String.class);
    String test1 = injector.instance(String.class);

    Assertions.assertNotNull(test);
    Assertions.assertEquals("Test", test);

    Assertions.assertNotNull(test1);
    Assertions.assertEquals("Test", test1);

    Assertions.assertEquals(System.identityHashCode(test), System.identityHashCode(test1));
  }

  @Test
  void testConstructingBinding() {
    Injector injector = Injector.newInjector();
    injector.install(BindingBuilder.create().toConstructing(StringHolder.class));

    StringHolder value = injector.instance(StringHolder.class);
    Assertions.assertNotNull(value);
    Assertions.assertEquals("Test", value.test);

    StringHolder value2 = injector.instance(StringHolder.class);
    Assertions.assertNotNull(value2);
    Assertions.assertNotEquals(value.hashCode(), value2.hashCode());
  }

  @Test
  void testInvalidConstructingBinding() {
    Assertions.assertThrows(
      AerogelException.class,
      () -> Injector.newInjector().install(BindingBuilder.create().toConstructing(NotConstructable.class)));
  }

  @Test
  void testFactoryBinding() {
    Injector injector = Injector.newInjector();
    injector.install(BindingBuilder.create().toFactory(FactoryMethodHolder.class, "constructStringHolder"));

    StringHolder holder = injector.instance(StringHolder.class);
    Assertions.assertNotNull(holder);
    Assertions.assertEquals("Factory", holder.test);
  }

  @Test
  void testInvalidFactoryBinding() {
    Assertions.assertThrows(
      AerogelException.class,
      () -> Injector.newInjector()
        .install(BindingBuilder.create().toFactory(FactoryMethodHolder.class, "constructVoid")));
    Assertions.assertThrows(
      AerogelException.class,
      () -> Injector.newInjector()
        .install(BindingBuilder.create().toFactory(FactoryMethodHolder.class, "instanceConstruct")));
  }

  private interface NotConstructable {

  }

  private static final class StringHolder {

    private final String test;

    public StringHolder() {
      this.test = "Test";
    }

    public StringHolder(String test) {
      this.test = test;
    }
  }

  private static final class FactoryMethodHolder {

    public static StringHolder constructStringHolder() {
      return new StringHolder("Factory");
    }

    public static void constructVoid() {
    }

    public void instanceConstruct() {
    }
  }
}
