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

import dev.derklaro.aerogel.binding.key.BindingKey;
import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BindingKeyTest {

  @Test
  void testPrimitiveTypesAreBoxedOnKeyCreation() {
    BindingKey<?> byteKey = BindingKey.of(byte.class);
    Assertions.assertEquals(Byte.class, byteKey.type());

    BindingKey<?> shortKey = BindingKey.of(short.class);
    Assertions.assertEquals(Short.class, shortKey.type());

    BindingKey<?> intKey = BindingKey.of(int.class);
    Assertions.assertEquals(Integer.class, intKey.type());

    BindingKey<?> longKey = BindingKey.of(long.class);
    Assertions.assertEquals(Long.class, longKey.type());

    BindingKey<?> floatKey = BindingKey.of(float.class);
    Assertions.assertEquals(Float.class, floatKey.type());

    BindingKey<?> doubleKey = BindingKey.of(double.class);
    Assertions.assertEquals(Double.class, doubleKey.type());

    BindingKey<?> booleanKey = BindingKey.of(boolean.class);
    Assertions.assertEquals(Boolean.class, booleanKey.type());

    BindingKey<?> charKey = BindingKey.of(char.class);
    Assertions.assertEquals(Character.class, charKey.type());
  }

  @Test
  void testBindingKeyForSameRawTypeHasSameHashAndIsEqual() {
    BindingKey<?> key1 = BindingKey.of(String.class);
    BindingKey<?> key2 = BindingKey.of(String.class);
    Assertions.assertEquals(key1, key2);
    Assertions.assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void testBindingKeyForSameGenericTypeHasSameHashAndIsEqual() {
    Type parameterizedType1 = TypeFactory.parameterizedClass(Collection.class, String.class);
    Type parameterizedType2 = TypeFactory.parameterizedClass(Collection.class, String.class);
    Assertions.assertNotSame(parameterizedType1, parameterizedType2);

    BindingKey<?> key1 = BindingKey.of(parameterizedType1);
    BindingKey<?> key2 = BindingKey.of(parameterizedType2);
    Assertions.assertEquals(key1, key2);
    Assertions.assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void testBindingKeyForSameTypeTokenHasSameHashAndIsEqual() {
    TypeToken<?> typeToken1 = new TypeToken<Collection<String>>() {
    };
    TypeToken<?> typeToken2 = new TypeToken<Collection<String>>() {
    };
    Assertions.assertNotSame(typeToken1, typeToken2);

    BindingKey<?> key1 = BindingKey.of(typeToken1);
    BindingKey<?> key2 = BindingKey.of(typeToken2);
    Assertions.assertEquals(key1, key2);
    Assertions.assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void testBindingKeyForDifferentRawTypeHasDifferentHashAndIsNotEqual() {
    BindingKey<?> key1 = BindingKey.of(String.class);
    BindingKey<?> key2 = BindingKey.of(Integer.class);
    Assertions.assertNotEquals(key1, key2);
    Assertions.assertNotEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void testBindingKeyWithAnnotationHasDifferentHashCodeThanWithoutAnnotation() {
    BindingKey<?> key1 = BindingKey.of(String.class);
    BindingKey<?> key2 = BindingKey.of(String.class).withQualifier(SomeQualifier.class);
    Assertions.assertNotEquals(key1, key2);
    Assertions.assertNotEquals(key1.hashCode(), key2.hashCode());
    Assertions.assertFalse(key1.qualifierAnnotation().isPresent());
    Assertions.assertFalse(key2.qualifierAnnotation().isPresent());
    Assertions.assertFalse(key1.qualifierAnnotationType().isPresent());
    Assertions.assertTrue(key2.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(SomeQualifier.class, key2.qualifierAnnotationType().get());
  }

  @Test
  void testBindingKeyUsesAnnotationTypeForMatchingIfAnnotationHasNoMembers() throws AnnotationFormatException {
    Annotation annotationInstance = TypeFactory.annotation(MarkerQualifier.class, Collections.emptyMap());
    BindingKey<?> key = BindingKey.of(String.class).withQualifier(annotationInstance);
    Assertions.assertFalse(key.qualifierAnnotation().isPresent());
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MarkerQualifier.class, key.qualifierAnnotationType().get());
  }

  @Test
  void testBindingKeyUsesAnnotationInstanceForMatchingIfAnnotationHasMembers() throws AnnotationFormatException {
    Annotation annotationInstance = TypeFactory.annotation(SomeQualifier.class, Collections.emptyMap());
    BindingKey<?> key = BindingKey.of(String.class).withQualifier(annotationInstance);
    Assertions.assertTrue(key.qualifierAnnotation().isPresent());
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertSame(annotationInstance, key.qualifierAnnotation().get());
    Assertions.assertEquals(SomeQualifier.class, key.qualifierAnnotationType().get());
  }

  @Test
  void testBindingKeyWithSameTypeButDifferentQualifiersDoNotMatch() {
    BindingKey<?> key1 = BindingKey.of(String.class).withQualifier(MarkerQualifier.class);
    BindingKey<?> key2 = BindingKey.of(String.class).withQualifier(SomeQualifier.class);
    Assertions.assertTrue(key1.qualifierAnnotationType().isPresent());
    Assertions.assertTrue(key2.qualifierAnnotationType().isPresent());
    Assertions.assertNotEquals(key1, key2);
    Assertions.assertNotEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void testBindingKeyWithDifferentTypeButSameQualifierDoNotMatch() {
    BindingKey<?> key1 = BindingKey.of(String.class).withQualifier(MarkerQualifier.class);
    BindingKey<?> key2 = BindingKey.of(Integer.class).withQualifier(MarkerQualifier.class);
    Assertions.assertTrue(key1.qualifierAnnotationType().isPresent());
    Assertions.assertTrue(key2.qualifierAnnotationType().isPresent());
    Assertions.assertNotEquals(key1, key2);
    Assertions.assertNotEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void testBindingKeyWithSameTypeAndSomeQualifierWithoutMemberValuesMatch() {
    BindingKey<?> key1 = BindingKey.of(String.class).withQualifier(MarkerQualifier.class);
    BindingKey<?> key2 = BindingKey.of(String.class).withQualifier(MarkerQualifier.class);
    Assertions.assertTrue(key1.qualifierAnnotationType().isPresent());
    Assertions.assertTrue(key2.qualifierAnnotationType().isPresent());
    Assertions.assertSame(MarkerQualifier.class, key1.qualifierAnnotationType().get());
    Assertions.assertSame(MarkerQualifier.class, key2.qualifierAnnotationType().get());
    Assertions.assertEquals(key1, key2);
    Assertions.assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void testBindingKeyWithSameTypeAndSameQualifierAndSameMemberValuesMatch() throws AnnotationFormatException {
    Annotation anno1 = TypeFactory.annotation(SomeQualifier.class, Collections.singletonMap("value", "world"));
    Annotation anno2 = TypeFactory.annotation(SomeQualifier.class, Collections.singletonMap("value", "world"));
    BindingKey<?> key1 = BindingKey.of(String.class).withQualifier(anno1);
    BindingKey<?> key2 = BindingKey.of(String.class).withQualifier(anno2);
    Assertions.assertTrue(key1.qualifierAnnotation().isPresent());
    Assertions.assertTrue(key2.qualifierAnnotation().isPresent());
    Assertions.assertSame(anno1, key1.qualifierAnnotation().get());
    Assertions.assertSame(anno2, key2.qualifierAnnotation().get());
    Assertions.assertEquals(key1, key2);
    Assertions.assertEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void testBindingKeyWithSameTypeAndSameQualifierButDifferentMemberValuesDoNotMatch() throws AnnotationFormatException {
    Annotation anno1 = TypeFactory.annotation(SomeQualifier.class, Collections.emptyMap());
    Annotation anno2 = TypeFactory.annotation(SomeQualifier.class, Collections.singletonMap("value", "world"));
    BindingKey<?> key1 = BindingKey.of(String.class).withQualifier(anno1);
    BindingKey<?> key2 = BindingKey.of(String.class).withQualifier(anno2);
    Assertions.assertTrue(key1.qualifierAnnotation().isPresent());
    Assertions.assertTrue(key2.qualifierAnnotation().isPresent());
    Assertions.assertSame(anno1, key1.qualifierAnnotation().get());
    Assertions.assertSame(anno2, key2.qualifierAnnotation().get());
    Assertions.assertNotEquals(key1, key2);
    Assertions.assertNotEquals(key1.hashCode(), key2.hashCode());
  }

  @Test
  void testWithoutQualifierProperlyRemovesQualifierFromKey() {
    BindingKey<?> key1 = BindingKey.of(String.class).withQualifier(MarkerQualifier.class);
    BindingKey<?> key2 = BindingKey.of(String.class).withQualifier(MarkerQualifier.class);
    Assertions.assertTrue(key1.qualifierAnnotationType().isPresent());
    Assertions.assertTrue(key2.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(key1, key2);
    Assertions.assertEquals(key1.hashCode(), key2.hashCode());

    BindingKey<?> key1WithoutQualifier = key1.withoutQualifier();
    Assertions.assertFalse(key1WithoutQualifier.qualifierAnnotationType().isPresent());
    Assertions.assertNotEquals(key1WithoutQualifier, key2);
    Assertions.assertNotEquals(key1WithoutQualifier.hashCode(), key2.hashCode());
  }

  @Test
  void testSelectQualifierProperlyPicksValidQualifierFromArray() throws AnnotationFormatException {
    BindingKey<?> bindingKey = BindingKey.of(String.class);
    Assertions.assertFalse(bindingKey.qualifierAnnotationType().isPresent());

    Annotation[] annotations = new Annotation[]{
      TypeFactory.annotation(MarkerQualifier.class, Collections.emptyMap()),
      TypeFactory.annotation(FunctionalInterface.class, Collections.emptyMap()),
    };
    BindingKey<?> keyWithQualifier = bindingKey.selectQualifier(annotations);
    Assertions.assertTrue(keyWithQualifier.qualifierAnnotationType().isPresent());
    Assertions.assertFalse(keyWithQualifier.qualifierAnnotation().isPresent());
    Assertions.assertEquals(MarkerQualifier.class, keyWithQualifier.qualifierAnnotationType().get());
  }

  @Test
  void testSelectQualifierDoesNothingIfGivenEmptyArray() {
    BindingKey<?> bindingKey = BindingKey.of(String.class);
    BindingKey<?> bindingKeyWithQualifier = bindingKey.selectQualifier(new Annotation[0]);
    Assertions.assertSame(bindingKey, bindingKeyWithQualifier);
  }

  @Test
  void testSelectQualifierDoesNothingIfGivenArrayWithoutQualifier() throws AnnotationFormatException {
    Annotation[] annotations = new Annotation[]{
      TypeFactory.annotation(FunctionalInterface.class, Collections.emptyMap()),
    };
    BindingKey<?> bindingKey = BindingKey.of(String.class);
    BindingKey<?> bindingKeyWithQualifier = bindingKey.selectQualifier(annotations);
    Assertions.assertSame(bindingKey, bindingKeyWithQualifier);
  }

  @Test
  void testSelectQualifierThrowsExceptionIfMultipleQualifiersAreGiven() throws AnnotationFormatException {
    Annotation[] annotations = new Annotation[]{
      TypeFactory.annotation(SomeQualifier.class, Collections.emptyMap()),
      TypeFactory.annotation(MarkerQualifier.class, Collections.emptyMap()),
    };
    BindingKey<?> bindingKey = BindingKey.of(String.class);
    Assertions.assertThrows(IllegalStateException.class, () -> bindingKey.selectQualifier(annotations));
  }

  @Test
  void testRawTypeSwitchedCorrectlyInWithType() {
    BindingKey<?> bindingKey = BindingKey.of(String.class);
    Assertions.assertEquals(String.class, bindingKey.type());
    BindingKey<?> withChangedType = bindingKey.withType(Integer.class);
    Assertions.assertEquals(Integer.class, withChangedType.type());
  }

  @Test
  void testGenericTypeSwitchedCorrectlyInWithType() {
    Type originalType = TypeFactory.parameterizedClass(Collection.class, String.class);
    BindingKey<?> bindingKey = BindingKey.of(originalType);
    Assertions.assertEquals(originalType, bindingKey.type());

    Type switchedType = TypeFactory.parameterizedClass(Map.class, String.class, Integer.class);
    BindingKey<?> withChangedType = bindingKey.withType(switchedType);
    Assertions.assertEquals(switchedType, withChangedType.type());
  }

  @Test
  void testTypeTokenSwitchedCorrectlyInWithType() {
    TypeToken<?> originalTypeToken = new TypeToken<Collection<String>>() {
    };
    BindingKey<?> bindingKey = BindingKey.of(originalTypeToken);
    Assertions.assertEquals(originalTypeToken.getType(), bindingKey.type());

    TypeToken<?> switchedTypeToken = new TypeToken<Map<String, Integer>>() {
    };
    BindingKey<?> withChangedType = bindingKey.withType(switchedTypeToken);
    Assertions.assertEquals(switchedTypeToken.getType(), withChangedType.type());
  }

  @Test
  void testSwitchRawTypePreservesQualifierMatcher() {
    BindingKey<?> bindingKey = BindingKey.of(String.class).withQualifier(MarkerQualifier.class);
    Assertions.assertEquals(String.class, bindingKey.type());
    Assertions.assertTrue(bindingKey.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MarkerQualifier.class, bindingKey.qualifierAnnotationType().get());

    BindingKey<?> withChangedType = bindingKey.withType(Integer.class);
    Assertions.assertEquals(Integer.class, withChangedType.type());
    Assertions.assertTrue(withChangedType.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MarkerQualifier.class, withChangedType.qualifierAnnotationType().get());

    Assertions.assertNotEquals(bindingKey, withChangedType);
    Assertions.assertNotEquals(bindingKey.hashCode(), withChangedType.hashCode());
  }

  @Test
  void testSwitchTypeTokenPreservesQualifierMatcher() {
    TypeToken<?> originalTypeToken = new TypeToken<Collection<String>>() {
    };
    BindingKey<?> bindingKey = BindingKey.of(originalTypeToken).withQualifier(MarkerQualifier.class);
    Assertions.assertEquals(originalTypeToken.getType(), bindingKey.type());
    Assertions.assertTrue(bindingKey.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MarkerQualifier.class, bindingKey.qualifierAnnotationType().get());

    TypeToken<?> switchedTypeToken = new TypeToken<Map<String, Integer>>() {
    };
    BindingKey<?> withChangedType = bindingKey.withType(switchedTypeToken);
    Assertions.assertEquals(switchedTypeToken.getType(), withChangedType.type());
    Assertions.assertTrue(withChangedType.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MarkerQualifier.class, withChangedType.qualifierAnnotationType().get());

    Assertions.assertNotEquals(bindingKey, withChangedType);
    Assertions.assertNotEquals(bindingKey.hashCode(), withChangedType.hashCode());
  }

  @Test
  void testSwitchToRawType() {
    Type genericType = TypeFactory.parameterizedClass(Map.class, String.class, Integer.class);
    BindingKey<?> bindingKey = BindingKey.of(genericType);
    BindingKey<?> withRawType = bindingKey.withRawType();
    Assertions.assertEquals(genericType, bindingKey.type());
    Assertions.assertEquals(Map.class, withRawType.type());
    Assertions.assertNotEquals(bindingKey, withRawType);
    Assertions.assertNotEquals(bindingKey.hashCode(), withRawType.hashCode());
  }

  @Test
  void testSwitchToRawTypeOnRawTypeSucceeds() {
    BindingKey<?> bindingKey = BindingKey.of(String.class);
    BindingKey<?> withRawType = bindingKey.withRawType();
    Assertions.assertEquals(String.class, bindingKey.type());
    Assertions.assertEquals(String.class, withRawType.type());
    Assertions.assertEquals(bindingKey, withRawType);
    Assertions.assertEquals(bindingKey.hashCode(), withRawType.hashCode());
  }

  @Test
  void testSwitchToRawTypePreservesAnnotationMatcher() {
    Type genericType = TypeFactory.parameterizedClass(Map.class, String.class, Integer.class);
    BindingKey<?> bindingKey = BindingKey.of(genericType).withQualifier(MarkerQualifier.class);
    Assertions.assertEquals(genericType, bindingKey.type());
    Assertions.assertTrue(bindingKey.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MarkerQualifier.class, bindingKey.qualifierAnnotationType().get());

    BindingKey<?> withRawType = bindingKey.withRawType();
    Assertions.assertEquals(Map.class, withRawType.type());
    Assertions.assertTrue(bindingKey.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MarkerQualifier.class, bindingKey.qualifierAnnotationType().get());

    Assertions.assertNotEquals(bindingKey, withRawType);
    Assertions.assertNotEquals(bindingKey.hashCode(), withRawType.hashCode());
  }

  // @formatter:off
  @Retention(RetentionPolicy.RUNTIME) @Qualifier public @interface MarkerQualifier {}
  @Retention(RetentionPolicy.RUNTIME) @Qualifier public @interface SomeQualifier { String value() default "test"; }
  // @formatter:on
}
