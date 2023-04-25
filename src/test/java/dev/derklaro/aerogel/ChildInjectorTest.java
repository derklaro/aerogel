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

public class ChildInjectorTest {

  @Test
  void testChildInjectorConstruction() {
    Injector injector = Injector.newInjector();
    injector.install(BindingBuilder.create().toConstructing(StringHolder.class));

    StringHolder value = injector.instance(StringHolder.class);
    Assertions.assertNotNull(value);
    Assertions.assertEquals("test", value.test);

    Injector child = injector.newChildInjector();
    Assertions.assertNull(child.fastBinding(Element.forType(StringHolder.class)));

    StringHolder value2 = child.instance(StringHolder.class);
    Assertions.assertNotNull(value2);
    Assertions.assertEquals("test", value2.test);
    Assertions.assertSame(value, value2);
  }

  @Test
  void testChildInjectorInheritance() {
    Injector injector = Injector.newInjector();
    Injector childInjector = injector.newChildInjector();

    Assertions.assertTrue(injector.bindings().isEmpty());
    Assertions.assertTrue(childInjector.bindings().isEmpty());

    Assertions.assertSame(injector, injector.instance(Injector.class));
    Assertions.assertSame(childInjector, childInjector.instance(Injector.class));

    injector.install(BindingBuilder.create().toInstance("Hello World!"));

    Assertions.assertEquals(1, injector.allBindings().size());
    Assertions.assertEquals(1, childInjector.allBindings().size());

    Assertions.assertFalse(injector.bindings().isEmpty());
    Assertions.assertTrue(childInjector.bindings().isEmpty());

    Assertions.assertEquals("Hello World!", injector.instance(String.class));
    Assertions.assertEquals("Hello World!", childInjector.instance(String.class));

    childInjector.install(BindingBuilder.create().bind(int.class).toInstance(1234));

    Assertions.assertThrows(AerogelException.class, () -> injector.instance(int.class));
    Assertions.assertEquals(1234, childInjector.instance(int.class));
  }

  @Singleton
  private static final class StringHolder {

    private final String test = "test";
  }
}
