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

package dev.derklaro.aerogel.assertion;

import dev.derklaro.aerogel.internal.codegen.InjectionTimeProxy;
import org.junit.jupiter.api.Assertions;

public class InjectAssert {

  public static void assertSameProxySafe(Object left, Object right) {
    // assert we have left and right present
    Assertions.assertNotNull(left);
    Assertions.assertNotNull(right);

    // check if none of the given values is a proxy, we can shortcut to == then
    if (!(left instanceof InjectionTimeProxy.InjectionTimeProxied)
      && !(right instanceof InjectionTimeProxy.InjectionTimeProxied)) {
      Assertions.assertSame(left, right);
      return;
    }

    // compare the hash, a proxy must give the same hash information based of the delegate
    // this might throw an exception if the delegate is not set or null, therefore the wrap in doesNotThrow
    int leftHash = Assertions.assertDoesNotThrow(left::hashCode);
    int rightHash = Assertions.assertDoesNotThrow(right::hashCode);

    // compare the hash
    Assertions.assertEquals(leftHash, rightHash);
  }

  public static void assertNotSameProxySafe(Object left, Object right) {
    // assert we have left and right present
    Assertions.assertNotNull(left);
    Assertions.assertNotNull(right);

    // check if none of the given values is a proxy, we can shortcut to == then
    if (!(left instanceof InjectionTimeProxy.InjectionTimeProxied)
      && !(right instanceof InjectionTimeProxy.InjectionTimeProxied)) {
      Assertions.assertNotSame(left, right);
      return;
    }

    // compare the hash, a proxy must give the same hash information based of the delegate
    // this might throw an exception if the delegate is not set or null, therefore the wrap in doesNotThrow
    int leftHash = Assertions.assertDoesNotThrow(left::hashCode);
    int rightHash = Assertions.assertDoesNotThrow(right::hashCode);

    // compare the hash
    Assertions.assertNotEquals(leftHash, rightHash);
  }
}
