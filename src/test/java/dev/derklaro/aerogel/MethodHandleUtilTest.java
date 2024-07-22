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

import dev.derklaro.aerogel.internal.util.MethodHandleUtil;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MethodHandleUtilTest {

  @Test
  void testGenerificationOfStaticField() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle acc = lookup.findStaticSetter(MemberTestClass.class, "STATIC_FIELD", String.class);
    Assertions.assertEquals("world", MemberTestClass.STATIC_FIELD);

    MethodHandle generic = MethodHandleUtil.generifyFieldSetter(acc, true);
    MethodType mt = generic.type();
    Assertions.assertEquals(2, mt.parameterCount());
    Assertions.assertEquals(Object.class, mt.parameterType(0));
    Assertions.assertEquals(Object.class, mt.parameterType(1));

    Object newValue = "abc";
    generic.invoke(null, newValue);
    Assertions.assertEquals(newValue, MemberTestClass.STATIC_FIELD);
  }

  @Test
  void testGenerificationOfNonStaticField() throws Throwable {
    MemberTestClass inst = new MemberTestClass(0);
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle acc = lookup.findSetter(MemberTestClass.class, "nonStaticField", String.class);
    Assertions.assertEquals("world", inst.nonStaticField);

    MethodHandle generic = MethodHandleUtil.generifyFieldSetter(acc, false);
    MethodType mt = generic.type();
    Assertions.assertEquals(2, mt.parameterCount());
    Assertions.assertEquals(Object.class, mt.parameterType(0));
    Assertions.assertEquals(Object.class, mt.parameterType(1));

    Object newValue = "abc";
    generic.invoke(inst, newValue);
    Assertions.assertEquals(newValue, inst.nonStaticField);
  }

  @Test
  void testGenerifyStaticMethodWithReturn() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle acc = lookup.findStatic(
      MemberTestClass.class,
      "staticMethod",
      MethodType.methodType(String.class, int.class));
    Assertions.assertEquals("123", acc.invoke(123));

    MethodHandle generic = MethodHandleUtil.generifyMethodInvoker(acc, true, false);
    MethodType mt = generic.type();
    Assertions.assertEquals(2, mt.parameterCount());
    Assertions.assertEquals(Object.class, mt.parameterType(0));
    Assertions.assertEquals(Object[].class, mt.parameterType(1));

    Object abcParam = 9876;
    Object returnValue = generic.invoke(null, new Object[]{abcParam});
    Assertions.assertEquals("9876", returnValue);
  }

  @Test
  void testGenerifyStaticMethodWithoutReturn() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle acc = lookup.findStatic(
      MemberTestClass.class,
      "staticMethod",
      MethodType.methodType(String.class, int.class));
    Assertions.assertEquals("123", acc.invoke(123));

    MethodHandle generic = MethodHandleUtil.generifyMethodInvoker(acc, true, true);
    MethodType mt = generic.type();
    Assertions.assertEquals(2, mt.parameterCount());
    Assertions.assertEquals(Object.class, mt.parameterType(0));
    Assertions.assertEquals(Object[].class, mt.parameterType(1));

    Object abcParam = 9876;
    Object returnValue = generic.invoke(null, new Object[]{abcParam});
    Assertions.assertNull(returnValue); // return value is dropped
  }

  @Test
  void testGenerifyMethodWithReturn() throws Throwable {
    MemberTestClass inst = new MemberTestClass(0);
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle acc = lookup.findVirtual(
      MemberTestClass.class,
      "nonStaticMethod",
      MethodType.methodType(String.class, int.class));
    Assertions.assertEquals("123", acc.invoke(inst, 123));

    MethodHandle generic = MethodHandleUtil.generifyMethodInvoker(acc, false, false);
    MethodType mt = generic.type();
    Assertions.assertEquals(2, mt.parameterCount());
    Assertions.assertEquals(Object.class, mt.parameterType(0));
    Assertions.assertEquals(Object[].class, mt.parameterType(1));

    Object abcParam = 9876;
    Object returnValue = generic.invoke(inst, new Object[]{abcParam});
    Assertions.assertEquals("9876", returnValue);
  }

  @Test
  void testGenerifyMethodWithoutReturn() throws Throwable {
    MemberTestClass inst = new MemberTestClass(0);
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle acc = lookup.findVirtual(
      MemberTestClass.class,
      "nonStaticMethod",
      MethodType.methodType(String.class, int.class));
    Assertions.assertEquals("123", acc.invoke(inst, 123));

    MethodHandle generic = MethodHandleUtil.generifyMethodInvoker(acc, false, true);
    MethodType mt = generic.type();
    Assertions.assertEquals(2, mt.parameterCount());
    Assertions.assertEquals(Object.class, mt.parameterType(0));
    Assertions.assertEquals(Object[].class, mt.parameterType(1));

    Object abcParam = 9876;
    Object returnValue = generic.invoke(inst, new Object[]{abcParam});
    Assertions.assertNull(returnValue); // return value is dropped
  }

  @Test
  void testGenericConstructor() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    MethodHandle acc = lookup.findConstructor(MemberTestClass.class, MethodType.methodType(void.class, int.class));
    Assertions.assertNotNull(acc.invoke(123));

    MethodHandle generic = MethodHandleUtil.generifyConstructorInvoker(acc);
    MethodType mt = generic.type();
    Assertions.assertEquals(1, mt.parameterCount());
    Assertions.assertEquals(Object[].class, mt.parameterType(0));

    Object abcParam = 9876;
    Object returnValue = generic.invoke(new Object[]{abcParam});
    Assertions.assertNotNull(returnValue);
  }

  // @formatter:off
  public static final class MemberTestClass {
    public static String STATIC_FIELD = "world";
    public String nonStaticField = "world";
    public static String staticMethod(int abc) { return "" + abc; }
    public String nonStaticMethod(int abc) { return "" + abc; }
    public MemberTestClass(int abc) {}
  }
  // @formatter:on
}
