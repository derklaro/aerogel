/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2025 Pasqual K. and contributors
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
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.lang.annotation.Annotation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InjectionRequestTest {

  @Test
  void testConstructorInjectionOverride() {
    Injector injector = Injector.newInjector();
    RootBindingBuilder rootBindingBuilder = injector.createBindingBuilder();
    UninstalledBinding<String> stringBinging = rootBindingBuilder
      .bind(String.class)
      .toInstance("test");
    UninstalledBinding<String> stringNamedBinding = rootBindingBuilder
      .bind(String.class)
      .qualifiedWithName("test1")
      .toInstance("test2");
    UninstalledBinding<Integer> intBinding = rootBindingBuilder
      .bind(int.class)
      .toInstance(12345);
    UninstalledBinding<Integer> intNamedBinding = rootBindingBuilder
      .bind(int.class)
      .qualifiedWithName("testI1")
      .toInstance(54321);

    injector
      .installBinding(stringBinging)
      .installBinding(stringNamedBinding)
      .installBinding(intBinding)
      .installBinding(intNamedBinding);

    BindingKey<TestClass> key = BindingKey.of(TestClass.class);
    TestClass instance = injector.createInjectionRequest(key)
      .override(int.class, 995)
      .overrideProvider(BindingKey.of(String.class).withQualifier(new NamedImpl("test1")), () -> "Hello World!")
      .construct();
    Assertions.assertEquals("Hello World!", instance.test1);
    Assertions.assertEquals("test", instance.test0);
    Assertions.assertEquals(995, instance.testI0);
    Assertions.assertEquals(54321, instance.testI1);
  }

  @Test
  void testMemberInjectionOverride() {
    Injector injector = Injector.newInjector();
    RootBindingBuilder rootBindingBuilder = injector.createBindingBuilder();
    UninstalledBinding<String> stringBinging = rootBindingBuilder
      .bind(String.class)
      .toInstance("test");
    UninstalledBinding<String> stringNamedBinding = rootBindingBuilder
      .bind(String.class)
      .qualifiedWithName("test1")
      .toInstance("test2");
    UninstalledBinding<Integer> intBinding = rootBindingBuilder
      .bind(int.class)
      .toInstance(12345);
    UninstalledBinding<Integer> intNamedBinding = rootBindingBuilder
      .bind(int.class)
      .qualifiedWithName("testI1")
      .toInstance(54321);

    injector
      .installBinding(stringBinging)
      .installBinding(stringNamedBinding)
      .installBinding(intBinding)
      .installBinding(intNamedBinding);

    BindingKey<TestClass> key = BindingKey.of(TestClass.class);
    TestClass instance = injector.createInjectionRequest(key)
      .override(String.class, "Hello, World")
      .overrideProvider(BindingKey.of(int.class).withQualifier(new NamedImpl("testI1")), () -> 599)
      .construct();
    Assertions.assertEquals("test2", instance.test1);
    Assertions.assertEquals("Hello, World", instance.test0);
    Assertions.assertEquals(12345, instance.testI0);
    Assertions.assertEquals(599, instance.testI1);
  }

  // @formatter:off
  public static final class TestClass {
    final String test1;
    final int testI0;
    @Inject String test0;
    @Inject @Named("testI1") int testI1;
    @Inject public TestClass(@Named("test1") String test1, int testI0) { this.test1 = test1; this.testI0 = testI0; }
  }
  @SuppressWarnings("ClassExplicitlyAnnotation") public static final class NamedImpl implements Named {
    private final String name;
    public NamedImpl(String name) { this.name = name; }
    @Override public String value() { return this.name; }
    @Override public Class<? extends Annotation> annotationType() { return Named.class; }
    @Override public int hashCode() { return (127 * "value".hashCode()) ^ this.name.hashCode(); }
  }
  // @formatter:on
}
