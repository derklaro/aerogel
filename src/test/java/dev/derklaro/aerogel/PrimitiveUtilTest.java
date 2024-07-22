/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2024 Pasqual K. and contributors
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

import dev.derklaro.aerogel.internal.util.PrimitiveUtil;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PrimitiveUtilTest {

  @Test
  void testDefaultBoolean() {
    Assertions.assertFalse(PrimitiveUtil.defaultValue(boolean.class));
  }

  @Test
  void testDefaultChar() {
    Assertions.assertEquals('\0', PrimitiveUtil.defaultValue(char.class));
  }

  @Test
  void testDefaultByte() {
    Assertions.assertEquals((byte) 0, PrimitiveUtil.defaultValue(byte.class));
  }

  @Test
  void testDefaultShort() {
    Assertions.assertEquals((short) 0, PrimitiveUtil.defaultValue(short.class));
  }

  @Test
  void testDefaultInt() {
    Assertions.assertEquals(0, PrimitiveUtil.defaultValue(int.class));
  }

  @Test
  void testDefaultLong() {
    Assertions.assertEquals(0L, PrimitiveUtil.defaultValue(long.class));
  }

  @Test
  void testDefaultFloat() {
    Assertions.assertEquals(0f, PrimitiveUtil.defaultValue(float.class));
  }

  @Test
  void testDefaultDouble() {
    Assertions.assertEquals(0d, PrimitiveUtil.defaultValue(double.class));
  }

  @Test
  void testNonPrimitiveTypeThrowsException() {
    Assertions.assertThrows(AssertionError.class, () -> PrimitiveUtil.defaultValue(Object.class));
    Assertions.assertThrows(AssertionError.class, () -> PrimitiveUtil.defaultValue(String.class));
    Assertions.assertThrows(AssertionError.class, () -> PrimitiveUtil.defaultValue(Collection.class));
  }
}
