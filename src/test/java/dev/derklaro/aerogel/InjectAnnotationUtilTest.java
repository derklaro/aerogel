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

import dev.derklaro.aerogel.internal.annotation.InjectAnnotationUtil;
import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeFactory;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InjectAnnotationUtilTest {

  @Test
  void testValidQualifierAnnotation() {
    Assertions.assertTrue(InjectAnnotationUtil.validQualifierAnnotation(ValidQualifier.class));
    Assertions.assertFalse(InjectAnnotationUtil.validQualifierAnnotation(QualifierMissingMarker.class));
    Assertions.assertFalse(InjectAnnotationUtil.validQualifierAnnotation(QualifierWrongRetention.class));
  }

  @Test
  void testCheckValidQualifierAnnotation() {
    Assertions.assertDoesNotThrow(() -> InjectAnnotationUtil.checkValidQualifierAnnotation(ValidQualifier.class));
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> InjectAnnotationUtil.checkValidQualifierAnnotation(QualifierMissingMarker.class));
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> InjectAnnotationUtil.checkValidQualifierAnnotation(QualifierWrongRetention.class));
  }

  @Test
  void testValidScopeAnnotation() {
    Assertions.assertTrue(InjectAnnotationUtil.validScopeAnnotation(ValidScope.class));
    Assertions.assertFalse(InjectAnnotationUtil.validScopeAnnotation(ScopeWithMembers.class));
    Assertions.assertFalse(InjectAnnotationUtil.validScopeAnnotation(ScopeMissingMarker.class));
    Assertions.assertFalse(InjectAnnotationUtil.validScopeAnnotation(ScopeWrongRetention.class));
  }

  @Test
  void testCheckValidScopeAnnotation() {
    Assertions.assertDoesNotThrow(() -> InjectAnnotationUtil.checkValidScopeAnnotation(ValidScope.class));
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> InjectAnnotationUtil.checkValidScopeAnnotation(ScopeWithMembers.class));
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> InjectAnnotationUtil.checkValidScopeAnnotation(ScopeMissingMarker.class));
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> InjectAnnotationUtil.checkValidScopeAnnotation(ScopeWrongRetention.class));
  }

  @Test
  void testExtractValidQualifierFromArrayOfValidAndInvalidQualifiers() throws AnnotationFormatException {
    Annotation[] annotations = new Annotation[]{
      TypeFactory.annotation(ValidQualifier.class, Collections.emptyMap()),
      TypeFactory.annotation(QualifierMissingMarker.class, Collections.emptyMap()),
      TypeFactory.annotation(QualifierWrongRetention.class, Collections.emptyMap()),
    };
    Annotation qualifierAnnotation = InjectAnnotationUtil.findQualifierAnnotation(annotations);
    Assertions.assertNotNull(qualifierAnnotation);
    Assertions.assertEquals(ValidQualifier.class, qualifierAnnotation.annotationType());
  }

  @Test
  void testThrowsNoExceptionIfQualifierArrayIsEmpty() {
    Annotation[] annotations = new Annotation[0];
    Annotation qualifierAnnotation = InjectAnnotationUtil.findQualifierAnnotation(annotations);
    Assertions.assertNull(qualifierAnnotation);
  }

  @Test
  void testThrowsNoExceptionIfQualifierArrayOnlyContainsInvalidQualifiers() throws AnnotationFormatException {
    Annotation[] annotations = new Annotation[]{
      TypeFactory.annotation(ValidScope.class, Collections.emptyMap()),
      TypeFactory.annotation(QualifierMissingMarker.class, Collections.emptyMap()),
      TypeFactory.annotation(QualifierWrongRetention.class, Collections.emptyMap()),
    };
    Annotation qualifierAnnotation = InjectAnnotationUtil.findQualifierAnnotation(annotations);
    Assertions.assertNull(qualifierAnnotation);
  }

  @Test
  void testThrowsExceptionIfArrayContainsTwoValidQualifierAnnotations() throws AnnotationFormatException {
    Annotation[] annotations = new Annotation[]{
      TypeFactory.annotation(ValidQualifier.class, Collections.emptyMap()),
      TypeFactory.annotation(ValidQualifier.class, Collections.emptyMap()),
      TypeFactory.annotation(QualifierMissingMarker.class, Collections.emptyMap()),
      TypeFactory.annotation(QualifierWrongRetention.class, Collections.emptyMap()),
    };
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> InjectAnnotationUtil.findQualifierAnnotation(annotations));
  }

  @Test
  void testExtractValidScopeAnnotationFromArrayOfValidAndInvalidScopes() throws AnnotationFormatException {
    Annotation[] annotations = new Annotation[]{
      TypeFactory.annotation(ValidScope.class, Collections.emptyMap()),
      TypeFactory.annotation(ScopeMissingMarker.class, Collections.emptyMap()),
      TypeFactory.annotation(ScopeWrongRetention.class, Collections.emptyMap()),
    };
    Class<?> scopeType = InjectAnnotationUtil.findScopeAnnotation(annotations);
    Assertions.assertEquals(ValidScope.class, scopeType);
  }

  @Test
  void testThrowsNoExceptionIfScopesArrayIsEmpty() {
    Annotation[] annotations = new Annotation[0];
    Class<?> scopeType = InjectAnnotationUtil.findScopeAnnotation(annotations);
    Assertions.assertNull(scopeType);
  }

  @Test
  void testThrowsNoExceptionIfScopesArrayOnlyContainsInvalidScopes() throws AnnotationFormatException {
    Annotation[] annotations = new Annotation[]{
      TypeFactory.annotation(ValidQualifier.class, Collections.emptyMap()),
      TypeFactory.annotation(ScopeMissingMarker.class, Collections.emptyMap()),
      TypeFactory.annotation(ScopeWrongRetention.class, Collections.emptyMap()),
    };
    Class<?> scopeType = InjectAnnotationUtil.findScopeAnnotation(annotations);
    Assertions.assertNull(scopeType);
  }

  @Test
  void testThrowsExceptionIfArrayContainsTwoValidScopeAnnotations() throws AnnotationFormatException {
    Annotation[] annotations = new Annotation[]{
      TypeFactory.annotation(ValidScope.class, Collections.emptyMap()),
      TypeFactory.annotation(ValidScope.class, Collections.emptyMap()),
      TypeFactory.annotation(ScopeMissingMarker.class, Collections.emptyMap()),
      TypeFactory.annotation(ScopeWrongRetention.class, Collections.emptyMap()),
    };
    Assertions.assertThrows(IllegalStateException.class, () -> InjectAnnotationUtil.findScopeAnnotation(annotations));
  }

  // @formatter:off
  @Retention(RetentionPolicy.RUNTIME) @Qualifier public @interface ValidQualifier {}
  @Retention(RetentionPolicy.RUNTIME) public @interface QualifierMissingMarker {}
  @Retention(RetentionPolicy.SOURCE) @Qualifier public @interface QualifierWrongRetention {}

  @Retention(RetentionPolicy.RUNTIME) @Scope public @interface ValidScope {}
  @Retention(RetentionPolicy.SOURCE) @Scope public @interface ScopeWrongRetention {}
  @Retention(RetentionPolicy.RUNTIME) public @interface ScopeMissingMarker {}
  @Retention(RetentionPolicy.RUNTIME) @Scope public @interface ScopeWithMembers { String value(); }
  // @formatter:on
}
