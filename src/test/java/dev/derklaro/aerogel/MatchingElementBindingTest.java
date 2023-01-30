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
import java.util.AbstractMap;
import java.util.IdentityHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class MatchingElementBindingTest {

  @Test
  void testMatchingElementProvide() {
    Injector injector = Injector.newInjector();

    // some default instances for validation
    Map<Type, Map.Entry<String, Object>> registeredInstances = new IdentityHashMap<>();
    registeredInstances.put(int.class, new AbstractMap.SimpleImmutableEntry<>("bing", 1234));
    registeredInstances.put(String.class, new AbstractMap.SimpleImmutableEntry<>("world", "Hello World!"));

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
      .toLazyProvider((element, $) -> () -> {
        // get the predicate for the RegisteredInstance annotation
        AnnotationPredicate registeredInstance = element.requiredAnnotations().stream()
          .filter(predicate -> predicate.annotationType().equals(RegisteredInstance.class))
          .findFirst()
          .orElseThrow(() -> new AssertionFailedError("Missing annotation predicate that must be present"));

        // extract & validate the name value
        Object nameAsObject = registeredInstance.annotationValues().get("value");
        Assertions.assertNotNull(nameAsObject);
        String name = Assertions.assertInstanceOf(String.class, nameAsObject);

        // get the registered instance for the type & validate the given name
        Map.Entry<String, Object> instanceEntry = registeredInstances.get(element.componentType());
        if (instanceEntry != null && instanceEntry.getKey().equals(name)) {
          // matches, return the instance
          return instanceEntry.getValue();
        } else {
          // doesn't match, return nothing
          return null;
        }
      });
    injector.install(bindingConstructor);

    // inject an example class
    TestingClass testingClass = injector.instance(TestingClass.class);
    Assertions.assertEquals(1234, testingClass.bingInt);
    Assertions.assertEquals("Hello World!", testingClass.worldString);
    Assertions.assertNull(testingClass.googleString);
  }

  @Qualifier
  @Target(ElementType.PARAMETER)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface RegisteredInstance {

    String value();
  }

  private static final class TestingClass {

    private final int bingInt;
    private final String worldString;
    private final String googleString;

    @Inject
    public TestingClass(
      @RegisteredInstance("bing") int bingInt,
      @RegisteredInstance("world") String worldString,
      @RegisteredInstance("google") String googleString
    ) {
      this.bingInt = bingInt;
      this.worldString = worldString;
      this.googleString = googleString;
    }
  }
}
