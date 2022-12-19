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

import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.util.Qualifiers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SpecifiedInjectorTest {

  @Test
  void testNonSpecifiedParentLookup() {
    Injector parent = Injector.newInjector();
    SpecifiedInjector specified1 = parent.newSpecifiedInjector();
    SpecifiedInjector specified2 = specified1.newSpecifiedInjector();

    // assert the parent status
    Assertions.assertNull(parent.parent());
    Assertions.assertSame(parent, specified1.parent());
    Assertions.assertSame(specified1, specified2.parent());

    // get the non-specified parent from the chain
    Assertions.assertSame(parent, specified1.firstNonSpecifiedParent());
    Assertions.assertSame(parent, specified2.firstNonSpecifiedParent());
  }

  @Test
  void requestingInjectorFromSpecifiedReturnsSpecifiedInjector() {
    Injector injector = Injector.newInjector();
    SpecifiedInjector specified = injector.newSpecifiedInjector();

    // validate the injector return value from the parent
    Injector parentInstance = injector.instance(Injector.class);
    Assertions.assertSame(injector, parentInstance);

    // validate the injector return from the specified injector
    Injector specifiedInstance = specified.instance(Injector.class);
    Assertions.assertSame(specified, specifiedInstance);
  }

  @Test
  void specifiedInjectorOnlyKnowsRegisteredBindings() {
    Injector injector = Injector.newInjector();
    SpecifiedInjector specified = injector.newSpecifiedInjector();

    // register the same binding only to the parent
    Element element = Element.forType(int.class).requireAnnotation(Qualifiers.named("testing"));
    injector.install(BindingBuilder.create().bind(element).toInstance(123));

    // both should return the same
    Assertions.assertEquals(Integer.valueOf(123), injector.instance(element));
    Assertions.assertEquals(Integer.valueOf(123), specified.instance(element));

    // install to the specific injector as well
    specified.installSpecified(BindingBuilder.create().bind(element).toInstance(1234));
    Assertions.assertEquals(Integer.valueOf(123), injector.instance(element));
    Assertions.assertEquals(Integer.valueOf(1234), specified.instance(element));
  }

  @Test
  void instanceCreationCanUseParentAndSpecificRegisteredBindings() {
    Injector injector = Injector.newInjector();
    SpecifiedInjector specified = injector.newSpecifiedInjector();

    // only install the binding for def to the parent injector (the call order to "instance" matters here)
    injector.install(BindingBuilder.create()
      .bind(Element.forType(String.class).requireAnnotation(Qualifiers.named("def")))
      .toInstance("Testing"));
    Assertions.assertThrows(AerogelException.class, () -> specified.instance(TestingClass.class));
    Assertions.assertThrows(AerogelException.class, () -> injector.instance(TestingClass.class));

    // install the binding for the int to the specific injector
    specified.installSpecified(BindingBuilder.create()
      .bind(Element.forType(int.class).requireAnnotation(Qualifiers.named("abc")))
      .toInstance(1234));
    TestingClass instance = Assertions.assertDoesNotThrow(() -> specified.instance(TestingClass.class));

    // validate the class instance
    Assertions.assertEquals(1234, instance.abc);
    Assertions.assertEquals("Testing", instance.def);

    // the parent should be able to get the instance once the specific injector was called on the instance once
    TestingClass instance2 = Assertions.assertDoesNotThrow(() -> injector.instance(TestingClass.class));
    Assertions.assertEquals(1234, instance2.abc);
    Assertions.assertEquals("Testing", instance2.def);
  }

  @Test
  void testConstructedBindingsAreRemovedFromTheParentInjector() {
    Injector parent = Injector.newInjector();
    SpecifiedInjector specified = parent.newSpecifiedInjector();

    // get an instance of the testing class
    Assertions.assertDoesNotThrow(() -> specified.instance(EmptyTestingClass.class));
    Assertions.assertNotNull(parent.fastBinding(Element.forType(EmptyTestingClass.class)));

    // remove the bindings created by the specified injector
    Assertions.assertTrue(specified.removeConstructedBindings());
    Assertions.assertNull(parent.fastBinding(Element.forType(EmptyTestingClass.class)));
  }

  private static final class TestingClass {

    private final int abc;
    private final String def;

    @Inject
    public TestingClass(@Name("abc") int abc, @Name("def") String def) {
      this.abc = abc;
      this.def = def;
    }
  }

  private static final class EmptyTestingClass {

  }
}
