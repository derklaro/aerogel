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

import jakarta.inject.Inject;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CircularInjectionTest {

  @Test
  void testCyclicDependenciesAllClassesHaveInterfaces() {
    Injector injector = Injector.newInjector();
    T1Itf1 t1Itf1 = injector.instance(T1Itf1.class);
    T1Itf1Impl impl1 = Assertions.assertInstanceOf(T1Itf1Impl.class, t1Itf1);
    T1Itf2Impl impl2 = Assertions.assertInstanceOf(T1Itf2Impl.class, impl1.t1Itf2);
    Assertions.assertTrue(Proxy.isProxyClass(impl2.t1Itf3.getClass()));
    Assertions.assertDoesNotThrow(impl2.t1Itf3::hashCode); // ensures that the delegate has been set
    Assertions.assertEquals("works!", impl2.t1Itf3.toString());
  }

  @Test
  void testCyclicDependenciesOnlyASingleInterfaceInTree() {
    Injector injector = Injector.newInjector();
    T2Itf1 t1Itf1 = injector.instance(T2Itf1.class);
    T2Itf1 circular = t1Itf1.t2Class1().t2Class2.t2Itf1;
    Assertions.assertTrue(Proxy.isProxyClass(circular.getClass()));
    Assertions.assertDoesNotThrow(circular::hashCode); // ensures that the delegate has been set
    Assertions.assertEquals("works!", circular.toString());
  }

  @Test
  void testCyclicDependencyOnlyASingleInterfaceInTreeAndStartedFromConcreteClass() {
    Injector injector = Injector.newInjector();
    T2Class1 t2Class1 = injector.instance(T2Class1.class);
    T2Itf1 t2Itf1 = t2Class1.t2Class2.t2Itf1;
    Assertions.assertTrue(Proxy.isProxyClass(t2Itf1.getClass()));
    Assertions.assertDoesNotThrow(t2Itf1::hashCode); // ensures that the delegate has been set
    Assertions.assertEquals("works!", t2Itf1.toString());
  }

  // @formatter:off
  @ProvidedBy(T1Itf1Impl.class) public interface T1Itf1 {}
  @ProvidedBy(T1Itf2Impl.class) public interface T1Itf2 {}
  @ProvidedBy(T1Itf3Impl.class) public interface T1Itf3 {}
  public static final class T1Itf1Impl implements T1Itf1 {
    private final T1Itf2 t1Itf2;
    @Inject public T1Itf1Impl(T1Itf2 t1Itf2) { this.t1Itf2 = t1Itf2; }
  }
  public static final class T1Itf2Impl implements T1Itf2 {
    private final T1Itf3 t1Itf3;
    @Inject public T1Itf2Impl(T1Itf3 t1Itf3) { this.t1Itf3 = t1Itf3; }
  }
  public static final class T1Itf3Impl implements T1Itf3 {
    private final T1Itf1 t1Itf1;
    @Inject public T1Itf3Impl(T1Itf1 t1Itf1) { this.t1Itf1 = t1Itf1; }
    @Override public String toString() { return "works!"; }
  }

  @ProvidedBy(T2Itf1Impl.class) public interface T2Itf1 { T2Class1 t2Class1(); }
  public static final class T2Itf1Impl implements T2Itf1 {
    private final T2Class1 t2Class1;
    @Inject public T2Itf1Impl(T2Class1 t2Class1) { this.t2Class1 = t2Class1; }
    @Override public T2Class1 t2Class1() { return this.t2Class1; }
    @Override public String toString() { return "works!"; }
  }
  public static final class T2Class1 {
    private final T2Class2 t2Class2;
    @Inject public T2Class1(T2Class2 t2Class2) { this.t2Class2 = t2Class2; }
  }
  public static final class T2Class2 {
    private final T2Itf1 t2Itf1;
    @Inject public T2Class2(T2Itf1 t2Itf1) { this.t2Itf1 = t2Itf1; }
  }
  // @formatter:on
}
