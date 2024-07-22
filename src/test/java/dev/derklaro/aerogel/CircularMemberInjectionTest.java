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

import dev.derklaro.aerogel.binding.UninstalledBinding;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CircularMemberInjectionTest {

  @Test
  void testCircularMemberInjection() {
    Injector injector = Injector.newInjector();

    UninstalledBinding<String> testStringBinding = injector.createBindingBuilder()
      .bind(String.class)
      .qualifiedWithName("test")
      .toInstance("Hello World");
    injector.installBinding(testStringBinding);

    SomeClass instance = Assertions.assertDoesNotThrow(() -> injector.instance(SomeClass.class));
    Assertions.assertNotNull(instance);
    Assertions.assertEquals("Hello World", instance.helloWorld);
    Assertions.assertNotNull(instance.someOtherClass);
    Assertions.assertSame(instance, instance.someOtherClass.someClass);
  }

  @Singleton
  private static final class SomeClass {

    private final String helloWorld;
    private SomeOtherClass someOtherClass;

    @Inject
    public SomeClass(@Named("test") String helloWorld) {
      this.helloWorld = helloWorld;
    }

    @Inject
    public void constructDone(SomeOtherClass otherClass) {
      this.someOtherClass = otherClass;
    }
  }

  @Singleton
  private static final class SomeOtherClass {

    private final SomeClass someClass;

    @Inject
    public SomeOtherClass(SomeClass someClass) {
      this.someClass = someClass;
    }
  }
}
