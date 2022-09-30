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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MemberInjectorTest {

  @AfterEach
  void resetStaticFields() {
    FieldHolder.dataClass = null;
    FieldHolder.dataClass2 = null;

    MethodHolder.test = null;
    MethodHolder.test1 = null;
  }

  @Test
  void testOnlyNonPrivateStaticFieldInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().injectPrivateFields(false).build();

    injector.memberInjector(FieldHolder.class).inject(settings);
    Assertions.assertNull(FieldHolder.dataClass);
    Assertions.assertNotNull(FieldHolder.dataClass2);
  }

  @Test
  void testAllStaticFieldInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().build();

    injector.memberInjector(FieldHolder.class).inject(settings);
    Assertions.assertNotNull(FieldHolder.dataClass);
    Assertions.assertNotNull(FieldHolder.dataClass2);
  }

  @Test
  void testOnlyUninitializedStaticFieldInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().injectOnlyUninitializedFields(true).build();

    DataClass dataClass = new DataClass();
    FieldHolder.dataClass = dataClass;

    injector.memberInjector(FieldHolder.class).inject(settings);
    Assertions.assertNotNull(FieldHolder.dataClass);
    Assertions.assertNotNull(FieldHolder.dataClass2);

    Assertions.assertSame(dataClass, FieldHolder.dataClass);
  }

  @Test
  void testSpecificStaticFieldInjection() {
    Injector injector = Injector.newInjector();
    injector.memberInjector(FieldHolder.class).injectField("dataClass2");

    Assertions.assertNull(FieldHolder.dataClass);
    Assertions.assertNotNull(FieldHolder.dataClass2);
  }

  @Test
  void testOnlyNonPrivateInstanceFieldInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().injectPrivateFields(false).build();
    // test instance based field injection
    FieldHolder instance = new FieldHolder();

    injector.memberInjector(FieldHolder.class).inject(instance, settings);
    Assertions.assertNull(instance.dataClassI);
    Assertions.assertNotNull(instance.dataClassI2);
  }

  @Test
  void testAllInstanceFieldInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().build();
    // test instance based field injection
    FieldHolder instance = new FieldHolder();

    injector.memberInjector(FieldHolder.class).inject(instance, settings);
    Assertions.assertNotNull(instance.dataClassI);
    Assertions.assertNotNull(instance.dataClassI2);
  }

  @Test
  void testOnlyUninitializedInstanceFieldInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().injectOnlyUninitializedFields(true).build();
    // test instance based field injection
    FieldHolder instance = new FieldHolder();

    DataClass dataClass = new DataClass();
    instance.dataClassI = dataClass;

    injector.memberInjector(FieldHolder.class).inject(instance, settings);
    Assertions.assertNotNull(instance.dataClassI);
    Assertions.assertNotNull(instance.dataClassI2);

    Assertions.assertSame(dataClass, instance.dataClassI);
  }

  @Test
  void testSpecificInstanceFieldInjection() {
    Injector injector = Injector.newInjector();
    // test instance based field injection
    FieldHolder instance = new FieldHolder();
    injector.memberInjector(FieldHolder.class).injectField(instance, "dataClassI2");

    Assertions.assertNull(instance.dataClassI);
    Assertions.assertNotNull(instance.dataClassI2);
  }

  @Test
  void testOnlyNonPrivateStaticMethodInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().injectPrivateMethods(false).build();

    injector.memberInjector(MethodHolder.class).inject(settings);
    Assertions.assertNull(MethodHolder.test1);
    Assertions.assertNotNull(MethodHolder.test);
  }

  @Test
  void testAllStaticMethodInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().build();

    injector.memberInjector(MethodHolder.class).inject(settings);
    Assertions.assertNotNull(MethodHolder.test);
    Assertions.assertNotNull(MethodHolder.test1);
  }

  @Test
  void testSpecificStaticMethodInjection() {
    Injector injector = Injector.newInjector();
    injector.memberInjector(MethodHolder.class).injectMethod("test1");

    Assertions.assertNull(MethodHolder.test);
    Assertions.assertNotNull(MethodHolder.test1);
  }

  @Test
  void testOnlyNonPrivateInstanceMethodInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().injectPrivateMethods(false).build();
    // test instance based method injection
    MethodHolder instance = new MethodHolder();

    injector.memberInjector(MethodHolder.class).inject(instance, settings);
    Assertions.assertNull(instance.testI1);
    Assertions.assertNotNull(instance.testI);
  }

  @Test
  void testAllInstanceMethodInjection() {
    Injector injector = Injector.newInjector();
    MemberInjectionSettings settings = MemberInjectionSettings.builder().build();
    // test instance based method injection
    MethodHolder instance = new MethodHolder();

    injector.memberInjector(MethodHolder.class).inject(instance, settings);
    Assertions.assertNotNull(instance.testI);
    Assertions.assertNotNull(instance.testI1);
  }

  @Test
  void testSpecificInstanceMethodInjection() {
    Injector injector = Injector.newInjector();
    // test instance based method injection
    MethodHolder instance = new MethodHolder();
    injector.memberInjector(MethodHolder.class).injectMethod(instance, "testI");

    Assertions.assertNull(instance.testI1);
    Assertions.assertNotNull(instance.testI);
  }

  private static final class FieldHolder {

    @Inject
    public static DataClass dataClass2;

    @Inject
    private static DataClass dataClass;

    @Inject
    public DataClass dataClassI2;

    @Inject
    private DataClass dataClassI;
  }

  @SuppressWarnings("unused")
  private static final class MethodHolder {

    private static String test;
    private static String test1;

    private String testI;
    private String testI1;

    @Inject
    public static void test() {
      test = "test";
    }

    @Inject
    private static void test1() {
      test1 = "test1";
    }

    @Inject
    public void testI() {
      this.testI = "testI";
    }

    @Inject
    private void testI1() {
      this.testI1 = "testI1";
    }
  }

  private static final class DataClass {

  }
}
