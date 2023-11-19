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

package dev.derklaro.scopedvalue;

import dev.derklaro.aerogel.ContextualProvider;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Inject;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.Name;
import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.context.InjectionContext;
import dev.derklaro.aerogel.context.InjectionContextScope;
import dev.derklaro.aerogel.internal.context.util.ContextInstanceResolveHelper;
import dev.derklaro.aerogel.util.Qualifiers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ScopedValueContextKeptInScopeTest {

  @Test
  void contextKeptDuringScopedOperations() {
    Element element = Element.forType(String.class).requireAnnotation(Qualifiers.named("test"));

    Injector injector = Injector.newInjector();
    injector.install(BindingBuilder.create().bind(element).toInstance("Hello World!"));

    // with override
    {
      Element someClassElement = Element.forType(SomeClass.class);
      ContextualProvider<Object> provider = injector.binding(someClassElement).provider(someClassElement);
      InjectionContextScope scope = InjectionContext.builder(SomeClass.class, provider)
        .override(element, "World!")
        .enterScope();

      Object constructed = ContextInstanceResolveHelper.resolveInstanceScoped(scope);
      Assertions.assertNotNull(constructed);
      SomeClass someClass = Assertions.assertInstanceOf(SomeClass.class, constructed);

      // the overridden value
      Assertions.assertEquals("World!", someClass.world);
      Assertions.assertNotNull(someClass.otherClass);
      Assertions.assertEquals("World!", someClass.otherClass.world);

      // the overridden value should not be kept over member injection
      Assertions.assertEquals("World!", someClass.helloWorld);
      Assertions.assertEquals("World!", someClass.otherClass.helloWorld);
    }

    // without override
    {
      Element someClassElement = Element.forType(SomeClass.class);
      Object constructed = injector.instance(someClassElement);
      Assertions.assertNotNull(constructed);
      SomeClass someClass = Assertions.assertInstanceOf(SomeClass.class, constructed);

      // the overridden value
      Assertions.assertEquals("Hello World!", someClass.world);
      Assertions.assertNotNull(someClass.otherClass);
      Assertions.assertEquals("Hello World!", someClass.otherClass.world);

      // the overridden value should not be kept over member injection
      Assertions.assertEquals("Hello World!", someClass.helloWorld);
      Assertions.assertEquals("Hello World!", someClass.otherClass.helloWorld);
    }
  }

  public static final class SomeClass {

    private final String world;
    private final SomeOtherClass otherClass;

    private String helloWorld;

    @Inject
    public SomeClass(@Name("test") String world, Injector injector) {
      this.world = world;
      this.otherClass = injector.instance(SomeOtherClass.class);
    }

    @Inject
    public void injectHelloWorld(@Name("test") String helloWorld) {
      this.helloWorld = helloWorld;
    }
  }

  public static final class SomeOtherClass {

    private final String world;

    @Inject
    @Name("test")
    private String helloWorld;

    @Inject
    public SomeOtherClass(@Name("test") String world) {
      this.world = world;
    }
  }
}
