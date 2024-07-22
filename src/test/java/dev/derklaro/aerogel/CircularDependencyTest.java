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
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CircularDependencyTest {

  @Test
  void testCircularDependencyManagement() {
    Injector injector = Injector.newInjector();
    MainEntryPoint entryPoint = Assertions.assertDoesNotThrow(() -> injector.instance(MainEntryPoint.class));

    // singletons
    assertSameProxySafe(entryPoint.apiClassA, entryPoint.someProvider.apiClassA);
    assertSameProxySafe(entryPoint.apiClassA, entryPoint.someProvider.apiClassB.apiA());
    assertSameProxySafe(entryPoint.someProvider.otherProvider, entryPoint.someProvider.apiClassB.otherProvider());
    assertSameProxySafe(entryPoint.someProvider.otherProvider, entryPoint.apiClassA.someProvider().otherProvider);

    // not singleton
    assertNotSameProxySafe(entryPoint.someProvider, entryPoint.apiClassA.someProvider());
    assertNotSameProxySafe(entryPoint.someProvider.apiClassB, entryPoint.apiClassA.apiB());

    OtherProvider other = entryPoint.someProvider.otherProvider;
    assertNotSameProxySafe(other.apiB(), entryPoint.apiClassA.apiB());
    assertNotSameProxySafe(other.someProvider(), entryPoint.someProvider);

    // re-check singletons
    Assertions.assertSame(entryPoint, injector.instance(MainEntryPoint.class));
    Assertions.assertSame(entryPoint.apiClassA, injector.instance(ApiClassA.class));
    Assertions.assertSame(entryPoint.someProvider.otherProvider, injector.instance(OtherProvider.class));
  }

  private static void assertSameProxySafe(Object left, Object right) {
    Assertions.assertEquals(left.hashCode(), right.hashCode());
  }

  private static void assertNotSameProxySafe(Object left, Object right) {
    Assertions.assertNotEquals(left.hashCode(), right.hashCode());
  }

  @ProvidedBy(ApiClassAImpl.class)
  public interface ApiClassA {

    ApiClassB apiB();

    SomeProvider someProvider();
  }

  @ProvidedBy(ApiClassBImpl.class)
  public interface ApiClassB {

    ApiClassA apiA();

    OtherProvider otherProvider();
  }

  @ProvidedBy(SomeOtherProvider.class)
  public interface OtherProvider {

    ApiClassB apiB();

    SomeProvider someProvider();
  }

  @Singleton
  public static final class MainEntryPoint {

    private final ApiClassA apiClassA;
    private final SomeProvider someProvider;

    private SomeProvider memberProvidedProvider;

    @Inject
    public MainEntryPoint(ApiClassA apiClassA, SomeProvider someProvider) {
      this.apiClassA = apiClassA;
      this.someProvider = someProvider;
    }

    @Inject
    public void memberInjection(SomeProvider provider) {
      this.memberProvidedProvider = provider;
    }
  }

  public static final class SomeProvider {

    private final ApiClassA apiClassA;
    private final ApiClassB apiClassB;
    private final OtherProvider otherProvider;

    @Inject
    public SomeProvider(ApiClassA apiClassA, ApiClassB apiClassB, OtherProvider otherProvider) {
      this.apiClassA = apiClassA;
      this.apiClassB = apiClassB;
      this.otherProvider = otherProvider;
    }
  }

  @Singleton
  public static final class SomeOtherProvider implements OtherProvider {

    private final ApiClassB apiClassB;
    private final SomeProvider provider;

    @Inject
    public SomeOtherProvider(ApiClassB apiClassB, SomeProvider provider) {
      this.apiClassB = apiClassB;
      this.provider = provider;
    }

    @Override
    public ApiClassB apiB() {
      return this.apiClassB;
    }

    @Override
    public SomeProvider someProvider() {
      return this.provider;
    }
  }

  @Singleton
  public static final class ApiClassAImpl implements ApiClassA {

    private final ApiClassB apiClassB;
    private final SomeProvider someProvider;

    @Inject
    public ApiClassAImpl(ApiClassB apiClassB, SomeProvider someProvider) {
      this.apiClassB = apiClassB;
      this.someProvider = someProvider;
    }

    @Override
    public ApiClassB apiB() {
      return this.apiClassB;
    }

    @Override
    public SomeProvider someProvider() {
      return this.someProvider;
    }
  }

  public static final class ApiClassBImpl implements ApiClassB {

    private final ApiClassA apiClassA;
    private final OtherProvider otherProvider;

    @Inject
    public ApiClassBImpl(ApiClassA apiClassA, OtherProvider otherProvider) {
      this.apiClassA = apiClassA;
      this.otherProvider = otherProvider;
    }

    @Override
    public ApiClassA apiA() {
      return this.apiClassA;
    }

    @Override
    public OtherProvider otherProvider() {
      return this.otherProvider;
    }
  }
}
