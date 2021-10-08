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

package aerogel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingletonTest {

  @Name("holder")
  private static StringHolder factoryStringHolder() {
    return new StringHolder();
  }

  @Test
  void testConstructingSingleton() {
    Injector injector = Injector.newInjector();
    injector.install(Bindings.constructing(Element.get(StringHolder.class)));

    StringHolder value = injector.instance(StringHolder.class);
    Assertions.assertNotNull(value);
    Assertions.assertEquals("test", value.test);

    StringHolder value2 = injector.instance(StringHolder.class);
    Assertions.assertNotNull(value2);
    Assertions.assertEquals("test", value2.test);

    Assertions.assertSame(value, value2);
  }

  @Test
  void testFactoryMethodSingleton() throws NoSuchMethodException {
    Injector injector = Injector.newInjector();
    injector.install(Bindings.factory(SingletonTest.class.getDeclaredMethod("factoryStringHolder")));

    StringHolder holderA = injector.instance(Element.get(StringHolder.class).requireName("holder"));
    Assertions.assertNotNull(holderA);

    StringHolder holderB = injector.instance(Element.get(StringHolder.class).requireName("holder"));
    Assertions.assertNotNull(holderB);

    Assertions.assertSame(holderA, holderB);
  }

  @Singleton
  private static final class StringHolder {

    private final String test = "test";
  }
}
