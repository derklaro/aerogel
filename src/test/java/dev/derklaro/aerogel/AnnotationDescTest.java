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

import dev.derklaro.aerogel.internal.annotation.AnnotationDesc;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AnnotationDescTest {

  @Test
  void testScopeAnnotationWithoutMembersAndWrongRetention() {
    AnnotationDesc desc = AnnotationDesc.of(SourceScope.class);
    Assertions.assertFalse(desc.scope());
    Assertions.assertFalse(desc.qualifier());
    Assertions.assertFalse(desc.hasProperties());
    Assertions.assertTrue(desc.members().isEmpty());
    Assertions.assertEquals(SourceScope.class, desc.annotationType());
    Assertions.assertEquals(RetentionPolicy.SOURCE, desc.retentionPolicy());
  }

  @Test
  void testScopeAnnotationWithoutMembersAndValidRetention() {
    AnnotationDesc desc = AnnotationDesc.of(ValidScope.class);
    Assertions.assertTrue(desc.scope());
    Assertions.assertFalse(desc.qualifier());
    Assertions.assertFalse(desc.hasProperties());
    Assertions.assertTrue(desc.members().isEmpty());
    Assertions.assertEquals(ValidScope.class, desc.annotationType());
    Assertions.assertEquals(RetentionPolicy.RUNTIME, desc.retentionPolicy());
  }

  @Test
  void testQualifierAnnotationWithoutMembersAndValidRetention() {
    AnnotationDesc desc = AnnotationDesc.of(NoMemberQualifier.class);
    Assertions.assertFalse(desc.scope());
    Assertions.assertTrue(desc.qualifier());
    Assertions.assertFalse(desc.hasProperties());
    Assertions.assertTrue(desc.members().isEmpty());
    Assertions.assertEquals(NoMemberQualifier.class, desc.annotationType());
    Assertions.assertEquals(RetentionPolicy.RUNTIME, desc.retentionPolicy());
  }

  @Test
  void testQualifierAnnotationWithSingleMemberAndValidRetention() {
    AnnotationDesc desc = AnnotationDesc.of(MemberQualifier.class);
    Assertions.assertFalse(desc.scope());
    Assertions.assertTrue(desc.qualifier());
    Assertions.assertTrue(desc.hasProperties());
    Assertions.assertEquals(1, desc.members().size());
    Assertions.assertNotNull(desc.members().get("value"));
    Assertions.assertEquals(MemberQualifier.class, desc.annotationType());
    Assertions.assertEquals(RetentionPolicy.RUNTIME, desc.retentionPolicy());
  }

  @Test
  void testMultiMemberAnnotation() {
    AnnotationDesc desc = AnnotationDesc.of(MultiMember.class);
    Assertions.assertFalse(desc.scope());
    Assertions.assertFalse(desc.qualifier());
    Assertions.assertTrue(desc.hasProperties());
    Assertions.assertEquals(3, desc.members().size());
    Assertions.assertEquals(RetentionPolicy.CLASS, desc.retentionPolicy());

    AnnotationDesc.Member valueMember = desc.members().get("value");
    Assertions.assertNotNull(valueMember);
    Assertions.assertEquals("value", valueMember.name());
    Assertions.assertEquals(String.class, valueMember.type());
    Assertions.assertNull(valueMember.defaultValue());

    AnnotationDesc.Member value2Member = desc.members().get("value2");
    Assertions.assertNotNull(value2Member);
    Assertions.assertEquals("value2", value2Member.name());
    Assertions.assertEquals(int.class, value2Member.type());
    Assertions.assertNotNull(value2Member.defaultValue());
    Assertions.assertEquals(25, value2Member.defaultValue());

    AnnotationDesc.Member value3Member = desc.members().get("value3");
    Assertions.assertNotNull(value3Member);
    Assertions.assertEquals("value3", value3Member.name());
    Assertions.assertEquals(Class.class, value3Member.type());
    Assertions.assertNotNull(value3Member.defaultValue());
    Assertions.assertEquals(String.class, value3Member.defaultValue());
  }

  // @formatter:off
  @Scope @Retention(RetentionPolicy.SOURCE) public @interface SourceScope {}
  @Scope @Retention(RetentionPolicy.RUNTIME) public @interface ValidScope {}
  @Qualifier @Retention(RetentionPolicy.RUNTIME) public @interface NoMemberQualifier {}
  @Qualifier @Retention(RetentionPolicy.RUNTIME) public @interface MemberQualifier {
    String value();
  }
  @Retention(RetentionPolicy.CLASS) public @interface MultiMember {
    String value();
    int value2() default 25;
    Class<?> value3() default String.class;
  }
  // @formatter:on
}
