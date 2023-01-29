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

import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.binding.BindingConstructor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.IdentityHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MatchingElementBindingTest {

  @Test
  void testMatchingElementProvide() {
    Injector injector = Injector.newInjector();

    // some default instances for validation
    Map<Type, Object> registeredInstances = new IdentityHashMap<>();
    registeredInstances.put(int.class, 1234);
    registeredInstances.put(String.class, "Hello World!");

    // register a lazy provider binding
    BindingConstructor bindingConstructor = BindingBuilder.create()
      .bindMatching(element -> {
        // ensure that the element has special requirements
        if (element.hasSpecialRequirements()) {
          return element.requiredAnnotations()
            .stream()
            .anyMatch(predicate -> predicate.annotationType().equals(RegisteredInstance.class));
        } else {
          return false;
        }
      })
      .toLazyProvider((element, $) -> () -> registeredInstances.get(element.componentType()));
    injector.install(bindingConstructor);

    // build elements that are requesting the needed annotation
    Element intElement = Element.forType(int.class).requireAnnotation(RegisteredInstance.class);
    Element stringElement = Element.forType(String.class).requireAnnotation(RegisteredInstance.class);

    // check if the instances are correct
    Assertions.assertEquals(1234, injector.<Integer>instance(intElement));
    Assertions.assertEquals("Hello World!", injector.instance(stringElement));
    Assertions.assertNull(injector.instance(Element.forType(long.class).requireAnnotation(RegisteredInstance.class)));
  }

  @Target(ElementType.PARAMETER)
  @Retention(RetentionPolicy.RUNTIME)
  private @interface RegisteredInstance {

  }
}
