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

import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DynamicBindingProviderInjectTest {

  @Test
  void testDynamicBindingResolvedForConcreteTypeRatherThanProvider() {
    Injector injector = Injector.newInjector();
    DynamicBinding binding = injector.createBindingBuilder()
      .bindDynamically()
      .annotationPresent(Service.class)
      .toKeyedBindingProvider((key, scopedBuilder) -> {
        Service annotation = (Service) key.qualifierAnnotation().orElseThrow();
        return scopedBuilder.toInstance(annotation.value());
      });
    injector.installBinding(binding);

    TestServiceClass instance = Assertions.assertDoesNotThrow(() -> injector.instance(TestServiceClass.class));
    Assertions.assertNotNull(instance.stringProvider);
    Assertions.assertNotNull(instance.stringCtrProvider);

    Assertions.assertEquals("Hello World!", instance.stringProvider.get());
    Assertions.assertEquals("HI!", instance.stringCtrProvider.get());
  }

  @Test
  void testConcreteBindingCanOverrideDynamicBinding() {
    Injector injector = Injector.newInjector();

    // binding to handle everything annotated with @Service
    DynamicBinding binding = injector.createBindingBuilder()
      .bindDynamically()
      .annotationPresent(Service.class)
      .toKeyedBindingProvider((key, scopedBuilder) -> {
        Service annotation = (Service) key.qualifierAnnotation().orElseThrow();
        return scopedBuilder.toInstance(annotation.value());
      });
    injector.installBinding(binding);

    // binding to specifically handle @Service("HI!")
    UninstalledBinding<String> concreteBinding = injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(Service.class).property(Service::value).returns("HI!").require()
      .toProvider(() -> "Hello HI! :)");
    injector.installBinding(concreteBinding);

    TestServiceClass instance = Assertions.assertDoesNotThrow(() -> injector.instance(TestServiceClass.class));
    Assertions.assertNotNull(instance.stringProvider);
    Assertions.assertNotNull(instance.stringCtrProvider);

    Assertions.assertEquals("Hello World!", instance.stringProvider.get());
    Assertions.assertEquals("Hello HI! :)", instance.stringCtrProvider.get());
  }

  @Test
  void testConcreteProviderBindingCanOverrideDynamicBinding() {
    Injector injector = Injector.newInjector();

    // binding to handle everything annotated with @Service
    DynamicBinding binding = injector.createBindingBuilder()
      .bindDynamically()
      .annotationPresent(Service.class)
      .toKeyedBindingProvider((key, scopedBuilder) -> {
        Service annotation = (Service) key.qualifierAnnotation().orElseThrow();
        return scopedBuilder.toInstance(annotation.value());
      });
    injector.installBinding(binding);

    // binding to specifically handle @Service("HI!")
    Type providerStringType = TypeFactory.parameterizedClass(Provider.class, String.class);
    UninstalledBinding<Provider<String>> concreteBinding = injector.createBindingBuilder()
      .<Provider<String>>bind(providerStringType)
      .buildQualifier(Service.class).property(Service::value).returns("HI!").require()
      .toInstance(() -> "Hello HI! :)");
    injector.installBinding(concreteBinding);

    TestServiceClass instance = Assertions.assertDoesNotThrow(() -> injector.instance(TestServiceClass.class));
    Assertions.assertNotNull(instance.stringProvider);
    Assertions.assertNotNull(instance.stringCtrProvider);

    Assertions.assertEquals("Hello World!", instance.stringProvider.get());
    Assertions.assertEquals("Hello HI! :)", instance.stringCtrProvider.get());
  }

  // @formatter:off
  @Qualifier @Retention(RetentionPolicy.RUNTIME) @interface Service { String value(); }
  public static class TestServiceClass {
    private final Provider<String> stringCtrProvider;
    @Inject @Service("Hello World!") public Provider<String> stringProvider;
    @Inject public TestServiceClass(@Service("HI!") Provider<String> provider) { this.stringCtrProvider = provider; }
  }
  // @formatter:on
}
