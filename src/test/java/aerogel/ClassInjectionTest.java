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

public class ClassInjectionTest {

  @Test
  void testClassInject() {
    Injector injector = Injector.newInjector();
    injector.install(Bindings.fixed(Element.get(String.class), "test"));

    InjectableClass injectableClass = injector.instance(InjectableClass.class);

    Assertions.assertNotNull(injectableClass);
    Assertions.assertEquals("test", injectableClass.test);
  }

  @Test
  void testNamedClassInject() {
    Injector injector = Injector.newInjector();
    injector.install(Bindings.fixed(Element.get(String.class), "test"));
    injector.install(Bindings.fixed(Element.get(String.class).requireName("testing"), "test1234"));

    InjectableNamedClass injectableClass = injector.instance(InjectableNamedClass.class);

    Assertions.assertNotNull(injectableClass);
    Assertions.assertEquals("test", injectableClass.test);
    Assertions.assertEquals("test1234", injectableClass.namedString);
  }

  private static final class InjectableClass {

    private final String test;

    @Inject
    public InjectableClass(String test) {
      this.test = test;
    }
  }

  private static final class InjectableNamedClass {

    private final String test;
    private final String namedString;

    @Inject
    public InjectableNamedClass(String test, @Name("testing") String namedString) {
      this.test = test;
      this.namedString = namedString;
    }
  }
}