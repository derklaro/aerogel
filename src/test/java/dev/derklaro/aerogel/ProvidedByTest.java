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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProvidedByTest {

  @Test
  void testProvidedByDetection() {
    JustAnotherInterface value = Assertions.assertDoesNotThrow(
      () -> Injector.newInjector().instance(JustAnotherInterface.class));

    Assertions.assertNotNull(value);
    Assertions.assertEquals("Hello world :)", value.helloWorld());
    Assertions.assertEquals("Goodbye everyone", value.helloPeople());
  }

  @ProvidedBy(JustAnotherInterfaceImpl.class)
  public interface JustAnotherInterface {

    default String helloWorld() {
      return "Hello world :)";
    }

    default String helloPeople() {
      return "Hello people";
    }
  }

  public static final class JustAnotherInterfaceImpl implements JustAnotherInterface {

    @Override
    public String helloPeople() {
      return "Goodbye everyone";
    }
  }
}
