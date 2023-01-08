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

import dev.derklaro.aerogel.internal.reflect.TypeUtil;
import io.leangen.geantyref.TypeFactory;
import java.lang.reflect.Type;
import java.util.Collection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeUtilTest {

  @Test
  void testRawClassTypeExtraction() {
    Type classType = TypeUtil.rawType(String.class);
    Assertions.assertSame(String.class, classType);
  }

  @Test
  void testArrayTypeExtraction() {
    Type arrayType = TypeFactory.arrayOf(String.class);
    Type rawType = TypeUtil.rawType(arrayType);
    Assertions.assertSame(String[].class, rawType);
  }

  @Test
  void testRawGenericTypeExtraction() {
    Type genericType = TypeFactory.parameterizedClass(Collection.class, String.class);
    Type rawType = TypeUtil.rawType(genericType);
    Assertions.assertSame(Collection.class, rawType);
  }

  @Test
  void testRawGenericArrayTypeExtraction() {
    Type genericType = TypeFactory.parameterizedClass(Collection.class, String.class);
    Type arrayType = TypeFactory.arrayOf(genericType);
    Type rawType = TypeUtil.rawType(arrayType);
    Assertions.assertSame(Collection[].class, rawType);
  }
}
