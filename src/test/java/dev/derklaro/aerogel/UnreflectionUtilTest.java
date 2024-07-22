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

import dev.derklaro.aerogel.internal.util.UnreflectionUtil;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

@EnabledForJreRange(min = JRE.JAVA_16, disabledReason = "Cannot test for illegal access on those")
public class UnreflectionUtilTest {

  @Test
  void testFieldUnreflectionWithLookupThatHasAccess() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup().in(MemberTestClass.class);
    Field nonStaticField = MemberTestClass.class.getDeclaredField("nonStaticField");
    Assertions.assertFalse(nonStaticField.isAccessible());
    Assertions.assertDoesNotThrow(() -> lookup.unreflectSetter(nonStaticField));
    Assertions.assertDoesNotThrow(() -> UnreflectionUtil.unreflectFieldSetter(nonStaticField, lookup));
  }

  @Test
  void testFieldUnreflectionWithLookupThatDoesNotHaveAccess() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    Field nonStaticField = MemberTestClass.class.getDeclaredField("nonStaticField");
    Assertions.assertFalse(nonStaticField.isAccessible());
    Assertions.assertThrows(IllegalAccessException.class, () -> lookup.unreflectSetter(nonStaticField));
    Assertions.assertDoesNotThrow(() -> UnreflectionUtil.unreflectFieldSetter(nonStaticField, lookup));
    Assertions.assertFalse(nonStaticField.isAccessible());
  }

  @Test
  void testMethodUnreflectionWithLookupThatHasAccess() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup().in(MemberTestClass.class);
    Method nonStaticMethod = MemberTestClass.class.getDeclaredMethod("nonStaticMethod", int.class);
    Assertions.assertFalse(nonStaticMethod.isAccessible());
    Assertions.assertDoesNotThrow(() -> lookup.unreflect(nonStaticMethod));
    Assertions.assertDoesNotThrow(() -> UnreflectionUtil.unreflectMethod(nonStaticMethod, lookup));
  }

  @Test
  void testMethodUnreflectionWithLookupThatDoesNotHaveAccess() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    Method nonStaticMethod = MemberTestClass.class.getDeclaredMethod("nonStaticMethod", int.class);
    Assertions.assertFalse(nonStaticMethod.isAccessible());
    Assertions.assertThrows(IllegalAccessException.class, () -> lookup.unreflect(nonStaticMethod));
    Assertions.assertDoesNotThrow(() -> UnreflectionUtil.unreflectMethod(nonStaticMethod, lookup));
    Assertions.assertFalse(nonStaticMethod.isAccessible());
  }

  @Test
  void testConstructorUnreflectionWithLookupThatHasAccess() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup().in(MemberTestClass.class);
    Constructor<?> constructor = MemberTestClass.class.getDeclaredConstructor(int.class);
    Assertions.assertFalse(constructor.isAccessible());
    Assertions.assertDoesNotThrow(() -> lookup.unreflectConstructor(constructor));
    Assertions.assertDoesNotThrow(() -> UnreflectionUtil.unreflectConstructor(constructor, lookup));
  }

  @Test
  void testConstructorUnreflectionWithLookupThatDoesNotHaveAccess() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.publicLookup();
    Constructor<?> constructor = MemberTestClass.class.getDeclaredConstructor(int.class);
    Assertions.assertFalse(constructor.isAccessible());
    Assertions.assertThrows(IllegalAccessException.class, () -> lookup.unreflectConstructor(constructor));
    Assertions.assertDoesNotThrow(() -> UnreflectionUtil.unreflectConstructor(constructor, lookup));
    Assertions.assertFalse(constructor.isAccessible());
  }

  // @formatter:off
  public static final class MemberTestClass {
    private String nonStaticField = "world";
    private String nonStaticMethod(int abc) { return "" + abc; }
    private MemberTestClass(int abc) {}
  }
  // @formatter:on
}
