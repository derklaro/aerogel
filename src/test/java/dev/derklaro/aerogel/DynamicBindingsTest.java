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

import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.DynamicBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DynamicBindingsTest {

  @Test
  void testSimpleDynamicBindingOnlyMatchingType() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<String> binding = injector.createBindingBuilder().bind(String.class).toInstance("World!");
    DynamicBinding dynamicBinding = injector.createBindingBuilder().bindDynamically()
      .exactRawType(String.class)
      .toBindingProvider((key, rbb) -> binding);
    injector.installBinding(dynamicBinding);
    InstalledBinding<String> resolvedBinding = injector.binding(BindingKey.of(String.class));
    Assertions.assertSame(binding, resolvedBinding.asUninstalled());
  }

  @Test
  void testThrowsExceptionIfNoFiltersWereSet() {
    Injector injector = Injector.newInjector();
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> injector.createBindingBuilder().bindDynamically().toKeyedBindingProvider((key, bb) -> null));
  }

  @Test
  void testExplicitBindingOverridesDynamicBinding() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> explicitBinding = injector.createBindingBuilder().bind(String.class).toInstance("World!");
    injector.installBinding(explicitBinding);

    BindingKey<String> bindingKey = BindingKey.of(String.class);
    DynamicBinding dynamicBinding = injector.createBindingBuilder().bindDynamically()
      .exactRawType(String.class)
      .toKeyedBindingProvider((key, bb) -> bb.toInstance("Hello"));
    injector.installBinding(dynamicBinding);
    InstalledBinding<?> resolvedBinding = injector.binding(bindingKey);
    Assertions.assertTrue(dynamicBinding.supports(bindingKey));
    Assertions.assertSame(explicitBinding, resolvedBinding.asUninstalled());
  }

  @Test
  void testDynamicBindingMatchingAnnotationType() {
    Injector injector = Injector.newInjector();
    DynamicBinding dynamicBinding = injector.createBindingBuilder().bindDynamically()
      .exactRawType(String.class)
      .annotationPresent(NoMemberQualifier.class)
      .toKeyedBindingProvider((key, bb) -> bb.toInstance("Hello"));
    BindingKey<?> stringOnlyKey = BindingKey.of(String.class);
    BindingKey<?> keyWithQualifier = stringOnlyKey.withQualifier(NoMemberQualifier.class);
    Assertions.assertFalse(dynamicBinding.supports(stringOnlyKey));
    Assertions.assertTrue(dynamicBinding.supports(keyWithQualifier));
  }

  @Test
  void testDynamicBindingMatchingOnAnnotationWithMembers() throws AnnotationFormatException {
    Annotation anno1 = TypeFactory.annotation(MemberQualifier.class, Collections.singletonMap("value", "test1"));
    Annotation anno2 = TypeFactory.annotation(MemberQualifier.class, Collections.singletonMap("value", "test2"));
    Annotation anno3 = TypeFactory.annotation(MemberQualifier.class, Collections.singletonMap("value", "test3"));

    Injector injector = Injector.builder().jitBindingFilter(key -> false).build();
    UninstalledBinding<?> test1Binding = injector.createBindingBuilder().bind(String.class).toInstance("Hello");
    UninstalledBinding<?> test2Binding = injector.createBindingBuilder().bind(String.class).toInstance("World");

    DynamicBinding dynamicBinding = injector.createBindingBuilder().bindDynamically()
      .exactRawType(String.class)
      .matchAnnotation(MemberQualifier.class, memberQualifier -> {
        String value = memberQualifier.value();
        return value.equals("test1") || value.equals("test2");
      })
      .toKeyedBindingProvider((key, builder) -> {
        Assertions.assertTrue(key.qualifierAnnotation().isPresent());
        MemberQualifier mq = Assertions.assertInstanceOf(MemberQualifier.class, key.qualifierAnnotation().get());
        Assertions.assertTrue(mq.value().equals("test1") || mq.value().equals("test2"));
        return mq.value().equals("test1") ? test1Binding : test2Binding;
      });
    injector.installBinding(dynamicBinding);

    BindingKey<?> bindingKey = BindingKey.of(String.class);
    InstalledBinding<?> anno1Binding = injector.binding(bindingKey.withQualifier(anno1));
    Assertions.assertEquals(test1Binding, anno1Binding.asUninstalled());

    InstalledBinding<?> anno2Binding = injector.binding(bindingKey.withQualifier(anno2));
    Assertions.assertEquals(test2Binding, anno2Binding.asUninstalled());

    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(bindingKey));
    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(bindingKey.withQualifier(anno3)));
  }

  @Test
  void testAnnotationPredicateOnlyCalledIfAnnotationInstanceIsPresent() throws AnnotationFormatException {
    Annotation anno1 = TypeFactory.annotation(MemberQualifier.class, Collections.singletonMap("value", "test1"));
    Annotation anno2 = TypeFactory.annotation(MemberQualifier.class, Collections.singletonMap("value", "test2"));

    AtomicInteger matchCallCount = new AtomicInteger();
    Injector injector = Injector.builder().jitBindingFilter(key -> false).build();
    DynamicBinding binding = injector.createBindingBuilder().bindDynamically()
      .exactRawType(String.class)
      .matchAnnotation(annotation -> {
        matchCallCount.getAndIncrement();
        if (annotation instanceof MemberQualifier) {
          MemberQualifier mq = (MemberQualifier) annotation;
          return mq.value().equals("test1");
        } else {
          return false;
        }
      })
      .toKeyedBindingProvider((key, bb) -> bb.toInstance("Hello World!"));
    injector.installBinding(binding);

    BindingKey<?> bindingKey = BindingKey.of(String.class);
    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(bindingKey));
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> injector.binding(bindingKey.withQualifier(NoMemberQualifier.class)));
    Assertions.assertEquals(0, matchCallCount.get());

    Assertions.assertDoesNotThrow(() -> injector.binding(bindingKey.withQualifier(anno1)));
    Assertions.assertEquals(2, matchCallCount.get()); // one for .supports & one during construction
    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(bindingKey.withQualifier(anno2)));
    Assertions.assertEquals(3, matchCallCount.get());
  }

  @Test
  void testDynamicBindingOnlyAcceptsQualifierAnnotationsForMatching() {
    Injector injector = Injector.newInjector();
    DynamicBindingBuilder builder = injector.createBindingBuilder().bindDynamically();
    Assertions.assertThrows(IllegalStateException.class, () -> builder.annotationPresent(ValidScope.class));
    Assertions.assertThrows(IllegalStateException.class, () -> builder.annotationPresent(SourceScope.class));
    Assertions.assertThrows(IllegalStateException.class, () -> builder.annotationPresent(InvalidQualifier.class));
    Assertions.assertThrows(IllegalStateException.class, () -> builder.annotationPresent(FunctionalInterface.class));
    Assertions.assertThrows(IllegalStateException.class, () -> builder.matchAnnotation(ValidScope.class, $ -> false));
    Assertions.assertThrows(IllegalStateException.class, () -> builder.matchAnnotation(SourceScope.class, $ -> false));
    Assertions.assertDoesNotThrow(() -> builder.matchAnnotation(MemberQualifier.class, $ -> false));
    Assertions.assertDoesNotThrow(() -> builder.matchAnnotation(NoMemberQualifier.class, $ -> false));
  }

  @Test
  void testMatchingOnPrimitiveType() {
    Injector injector = Injector.builder().jitBindingFilter(key -> false).build();
    DynamicBinding binding = injector.createBindingBuilder().bindDynamically()
      .exactRawType(int.class)
      .toKeyedBindingProvider((key, builder) -> builder.toInstance(12345));
    injector.installBinding(binding);

    BindingKey<?> intBindingKey = BindingKey.of(int.class);
    BindingKey<?> longBindingKey = BindingKey.of(long.class);
    Assertions.assertEquals(Integer.class, intBindingKey.type());
    Assertions.assertDoesNotThrow(() -> injector.binding(intBindingKey));
    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(longBindingKey));
  }

  @Test
  void testMatchingOnSuperType() {
    Injector injector = Injector.builder().jitBindingFilter(key -> false).build();
    DynamicBinding binding = injector.createBindingBuilder().bindDynamically()
      .superRawType(Set.class)
      .toKeyedBindingProvider((key, builder) -> builder.toInstance(new HashSet<>()));
    injector.installBinding(binding);

    BindingKey<?> exactTypeKey = BindingKey.of(Set.class);
    BindingKey<?> subTypeKey = BindingKey.of(HashSet.class);
    BindingKey<?> mismatchTypeKey = BindingKey.of(ArrayList.class);
    Assertions.assertDoesNotThrow(() -> injector.binding(exactTypeKey));
    Assertions.assertDoesNotThrow(() -> injector.binding(subTypeKey));
    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(mismatchTypeKey));
  }

  @Test
  void testCustomRawTypeMatcherPredicate() {
    Injector injector = Injector.builder().jitBindingFilter(key -> false).build();
    DynamicBinding binding = injector.createBindingBuilder().bindDynamically()
      .matchRawType(clazz -> clazz == Set.class || clazz == HashSet.class)
      .toKeyedBindingProvider((key, builder) -> builder.toInstance(new HashSet<>()));
    injector.installBinding(binding);

    BindingKey<?> setKey = BindingKey.of(Set.class);
    BindingKey<?> hashSetKey = BindingKey.of(HashSet.class);
    BindingKey<?> treeSetKey = BindingKey.of(TreeSet.class);
    BindingKey<?> arrayListKey = BindingKey.of(ArrayList.class);
    Assertions.assertDoesNotThrow(() -> injector.binding(setKey));
    Assertions.assertDoesNotThrow(() -> injector.binding(hashSetKey));
    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(treeSetKey));
    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(arrayListKey));
  }

  @Test
  void testMatchingGenericType() {
    Injector injector = Injector.builder().jitBindingFilter(key -> false).build();
    DynamicBinding binding = injector.createBindingBuilder().bindDynamically()
      .matchType(type -> {
        if (type instanceof ParameterizedType) {
          ParameterizedType parameterizedType = (ParameterizedType) type;
          Type[] typeArguments = parameterizedType.getActualTypeArguments();
          if (typeArguments.length == 1) {
            return parameterizedType.getRawType() == Set.class && typeArguments[0] == String.class;
          }
        }
        return false;
      })
      .toKeyedBindingProvider((key, builder) -> builder.toInstance(new HashSet<>()));
    injector.installBinding(binding);

    Type setStringType = TypeFactory.parameterizedClass(Set.class, String.class);
    Type setIntegerType = TypeFactory.parameterizedClass(Set.class, Integer.class);
    Type mapStringStringType = TypeFactory.parameterizedClass(Map.class, String.class, String.class);
    Assertions.assertDoesNotThrow(() -> injector.binding(BindingKey.of(setStringType)));
    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(BindingKey.of(setIntegerType)));
    Assertions.assertThrows(IllegalStateException.class, () -> injector.binding(BindingKey.of(mapStringStringType)));
  }

  // @formatter:off
  @Scope @Retention(RetentionPolicy.SOURCE) public @interface SourceScope {}
  @Scope @Retention(RetentionPolicy.RUNTIME) public @interface ValidScope {}
  @Qualifier @Retention(RetentionPolicy.SOURCE) public @interface InvalidQualifier {}
  @Qualifier @Retention(RetentionPolicy.RUNTIME) public @interface NoMemberQualifier {}
  @Qualifier @Retention(RetentionPolicy.RUNTIME) public @interface MemberQualifier {
    String value();
  }
  // @formatter:on
}
