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

import dev.derklaro.aerogel.internal.codegen.InjectionTimeProxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InjectionTimeProxyTest {

  @Test
  void testInjectionTimeProxyCreate() {
    // create a proxy and set the delegate
    B proxy = InjectionTimeProxy.makeProxy(B.class);
    Assertions.assertInstanceOf(InjectionTimeProxy.InjectionTimeProxied.class, proxy).setDelegate(new ABImpl());

    String hello = Assertions.assertDoesNotThrow(proxy::a);
    Assertions.assertEquals("Hello", hello);

    String world = Assertions.assertDoesNotThrow(proxy::b);
    Assertions.assertEquals("World", world);

    String intString = Assertions.assertDoesNotThrow(() -> proxy.toString(1234));
    Assertions.assertEquals("1234", intString);
  }

  public interface A {

    String toString(int a);

    CharSequence a();
  }

  public interface B extends A {

    @Override
    String a();

    String b();
  }

  public static final class ABImpl implements B {

    @Override
    public String toString(int a) {
      return Integer.toString(a);
    }

    @Override
    public String a() {
      return "Hello";
    }

    @Override
    public String b() {
      return "World";
    }
  }
}
