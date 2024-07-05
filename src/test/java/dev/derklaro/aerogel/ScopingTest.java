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

import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import jakarta.inject.Inject;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class ScopingTest {

  @BeforeEach
  void resetStaticReferences() {
    ASingletonClass.COUNTER.set(0);
    NotASingletonClass.COUNTER.set(0);
    BaseSingletonTypeImpl.COUNTER.set(0);
  }

  @Test
  void testNonSingleton() {
    Injector injector = Injector.newInjector();
    NotASingletonClass instance1 = injector.instance(NotASingletonClass.class);
    NotASingletonClass instance2 = injector.instance(NotASingletonClass.class);
    Assertions.assertEquals(0, instance1.count);
    Assertions.assertEquals(1, instance2.count);
    Assertions.assertNotSame(instance1, instance2);
  }

  @Test
  void testAnnotatedSingleton() {
    Injector injector = Injector.newInjector();
    ASingletonClass instance1 = injector.instance(ASingletonClass.class);
    ASingletonClass instance2 = injector.instance(ASingletonClass.class);
    Assertions.assertEquals(0, instance1.count);
    Assertions.assertEquals(0, instance2.count);
    Assertions.assertSame(instance1, instance2);
  }

  @Test
  void testProvidedImplementationDetectsSingleton() {
    Injector injector = Injector.newInjector();
    BoundToSingletonInterface instance1 = injector.instance(BoundToSingletonInterface.class);
    BoundToSingletonInterface instance2 = injector.instance(BoundToSingletonInterface.class);

    ASingletonClass impl1 = Assertions.assertInstanceOf(ASingletonClass.class, instance1);
    ASingletonClass impl2 = Assertions.assertInstanceOf(ASingletonClass.class, instance2);

    Assertions.assertEquals(0, impl1.count);
    Assertions.assertEquals(0, impl2.count);
    Assertions.assertSame(impl1, impl2);
  }

  @Test
  void testExplicitlyBoundSingleton() {
    Injector injector = Injector.newInjector();

    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(NotASingletonClass.class)
      .scopedWithSingleton()
      .toConstructingSelf();
    injector.installBinding(binding);

    NotASingletonClass instance1 = injector.instance(NotASingletonClass.class);
    NotASingletonClass instance2 = injector.instance(NotASingletonClass.class);
    Assertions.assertEquals(0, instance1.count);
    Assertions.assertEquals(0, instance2.count);
    Assertions.assertSame(instance1, instance2);
  }

  @Test
  void testSingletonHandledPerInjector() {
    Injector injector1 = Injector.newInjector();
    Injector injector2 = Injector.newInjector();
    ASingletonClass instance1 = injector1.instance(ASingletonClass.class);
    ASingletonClass instance2 = injector2.instance(ASingletonClass.class);
    Assertions.assertNotSame(instance1, instance2);
  }

  @Test
  void testChildInjectorInheritsSingleton() {
    Injector injector = Injector.newInjector();
    Injector child = injector.createChildInjector();
    ASingletonClass instance1 = injector.instance(ASingletonClass.class);
    ASingletonClass instance2 = child.instance(ASingletonClass.class);
    Assertions.assertSame(instance1, instance2);
  }

  @Test
  void testSingletonScopeNotInherited() {
    Injector injector = Injector.newInjector();
    BaseSingletonType instance1 = injector.instance(BaseSingletonType.class);
    BaseSingletonType instance2 = injector.instance(BaseSingletonType.class);

    BaseSingletonTypeImpl impl1 = Assertions.assertInstanceOf(BaseSingletonTypeImpl.class, instance1);
    BaseSingletonTypeImpl impl2 = Assertions.assertInstanceOf(BaseSingletonTypeImpl.class, instance2);

    Assertions.assertEquals(0, impl1.count);
    Assertions.assertEquals(1, impl2.count);
    Assertions.assertNotSame(impl1, impl2);
  }

  @Test
  void testSingletonScopeCanBeOverridden() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(ASingletonClass.class)
      .unscoped()
      .toConstructingSelf();
    injector.installBinding(binding);

    ASingletonClass instance1 = injector.instance(ASingletonClass.class);
    ASingletonClass instance2 = injector.instance(ASingletonClass.class);
    Assertions.assertNotSame(instance1, instance2);
  }

  @Test
  void testBindingBuilderDetectsScopeAnnotation() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(BoundToSingletonInterface.class)
      .toConstructingClass(ASingletonClass.class);
    injector.installBinding(binding);

    BoundToSingletonInterface instance1 = injector.instance(BoundToSingletonInterface.class);
    BoundToSingletonInterface instance2 = injector.instance(BoundToSingletonInterface.class);
    Assertions.assertSame(instance1, instance2);
  }

  @Test
  void testBindingBuilderIgnoredScopeOnBaseType() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(BaseSingletonType.class)
      .toConstructingClass(BaseSingletonTypeImpl.class);
    injector.installBinding(binding);

    BaseSingletonType instance1 = injector.instance(BaseSingletonType.class);
    BaseSingletonType instance2 = injector.instance(BaseSingletonType.class);
    Assertions.assertNotSame(instance1, instance2);
  }

  @Test
  void testNullBoundAsSingleton() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(String.class)
      .scopedWithSingleton()
      .toProvider(() -> null);
    injector.installBinding(binding);

    String instance1 = injector.instance(String.class);
    String instance2 = injector.instance(String.class);
    Assertions.assertNull(instance1);
    Assertions.assertNull(instance2);
  }

  @Test
  void testCustomScopeIsAppliedEvenIfOtherScopeIsDefined() {
    Injector injector = Injector.newInjector();
    ASingletonClass customSingletonInstance = new ASingletonClass();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(ASingletonClass.class)
      .scopedWith(new CustomScopeApplier(customSingletonInstance))
      .toConstructingSelf();
    injector.installBinding(binding);

    ASingletonClass instance1 = injector.instance(ASingletonClass.class);
    ASingletonClass instance2 = injector.instance(ASingletonClass.class);
    Assertions.assertSame(customSingletonInstance, instance1);
    Assertions.assertSame(customSingletonInstance, instance2);
  }

  @Test
  void testCustomScopeAnnotationIsResolved() {
    Injector injector = Injector.newInjector();
    NotASingletonClass singletonInstance = new NotASingletonClass();
    injector.scopeRegistry().register(CustomScope.class, new CustomScopeApplier(singletonInstance));
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(NotASingletonClass.class)
      .scopedWith(CustomScope.class)
      .toConstructingSelf();
    injector.installBinding(binding);

    NotASingletonClass instance1 = injector.instance(NotASingletonClass.class);
    NotASingletonClass instance2 = injector.instance(NotASingletonClass.class);
    Assertions.assertSame(singletonInstance, instance1);
    Assertions.assertSame(singletonInstance, instance2);
  }

  @Test
  void testSingletonScopeIsPreservedAndNotReturnsCircularProxies() {
    Injector injector = Injector.newInjector();
    SomeServiceA serviceA = injector.instance(SomeServiceA.class);
    SomeServiceB serviceB = injector.instance(SomeServiceB.class);

    Assertions.assertFalse(Proxy.isProxyClass(serviceA.getClass()));
    Assertions.assertFalse(Proxy.isProxyClass(serviceB.getClass()));
    Assertions.assertTrue(Proxy.isProxyClass(serviceA.serviceB().getClass()));

    Assertions.assertEquals(serviceA.serviceB(), serviceB);
    Assertions.assertEquals(serviceB.hashCode(), serviceA.serviceB().hashCode());
    Assertions.assertSame(serviceA, serviceA.serviceB().serviceA());
  }

  @Test
  @Timeout(10)
  void testConcurrentSingletonConstructionDoesNotResultInMultipleInstances() throws Exception {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(ASingletonClass.class).toConstructingSelf();
    injector.installBinding(binding);

    ExecutorService service = Executors.newFixedThreadPool(2);
    Future<ASingletonClass> f1 = service.submit(() -> injector.createChildInjector().instance(ASingletonClass.class));
    Future<ASingletonClass> f2 = service.submit(() -> injector.createChildInjector().instance(ASingletonClass.class));
    ASingletonClass instance1 = f1.get(10, TimeUnit.SECONDS);
    ASingletonClass instance2 = f2.get(10, TimeUnit.SECONDS);
    Assertions.assertSame(instance1, instance2);
  }

  @Test
  @Timeout(10)
  void testConcurrentSingletonConstructDoesNotResultInMultipleProxies() throws Exception {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> bindingServiceA = injector.createBindingBuilder()
      .bind(SomeServiceA.class)
      .toConstructingClass(SomeServiceAImpl.class);
    UninstalledBinding<?> bindingServiceB = injector.createBindingBuilder()
      .bind(SomeServiceB.class)
      .toConstructingClass(SomeServiceBImpl.class);
    injector.installBinding(bindingServiceA).installBinding(bindingServiceB);

    ExecutorService service = Executors.newFixedThreadPool(2);
    Future<SomeServiceA> f1 = service.submit(() -> injector.createChildInjector().instance(SomeServiceA.class));
    Future<SomeServiceA> f2 = service.submit(() -> injector.createChildInjector().instance(SomeServiceA.class));
    SomeServiceA instance1 = f1.get(10, TimeUnit.SECONDS);
    SomeServiceA instance2 = f2.get(10, TimeUnit.SECONDS);

    Assertions.assertSame(instance1, instance2);
    Assertions.assertSame(instance1.serviceB(), instance2.serviceB());
    Assertions.assertSame(instance1, instance2.serviceB().serviceA());

    Assertions.assertTrue(Proxy.isProxyClass(instance1.serviceB().getClass()));
    Assertions.assertTrue(Proxy.isProxyClass(instance2.serviceB().getClass()));
    Assertions.assertSame(instance1.serviceB(), instance2.serviceB());

    SomeServiceB serviceB = injector.instance(SomeServiceB.class);
    Assertions.assertFalse(Proxy.isProxyClass(serviceB.getClass()));
    Assertions.assertEquals(serviceB.hashCode(), instance1.serviceB().hashCode());
    Assertions.assertEquals(serviceB.hashCode(), instance2.serviceB().hashCode());
  }

  // @formatter:off
  @Singleton @ProvidedBy(BaseSingletonTypeImpl.class)
  public interface BaseSingletonType {}

  @ProvidedBy(ASingletonClass.class)
  public interface BoundToSingletonInterface {}

  @ProvidedBy(SomeServiceAImpl.class)
  public interface SomeServiceA { SomeServiceB serviceB(); }

  @ProvidedBy(SomeServiceBImpl.class)
  public interface SomeServiceB { SomeServiceA serviceA(); }

  @Scope @Target(ElementType.TYPE) @Retention(RetentionPolicy.RUNTIME)
  public @interface CustomScope {}

  @Singleton
  public static final class SomeServiceAImpl implements SomeServiceA {
    private final SomeServiceB serviceB;
    @Inject public SomeServiceAImpl(SomeServiceB serviceB) { this.serviceB = serviceB; }
    @Override public SomeServiceB serviceB() { return this.serviceB; }
  }

  @Singleton
  public static final class SomeServiceBImpl implements SomeServiceB {
    private final SomeServiceA serviceA;
    @Inject public SomeServiceBImpl(SomeServiceA serviceA) { this.serviceA = serviceA; }
    @Override public SomeServiceA serviceA() { return this.serviceA; }
  }

  public static final class BaseSingletonTypeImpl implements BaseSingletonType {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private final int count;
    public BaseSingletonTypeImpl() { this.count = COUNTER.getAndIncrement(); }
  }

  public static final class NotASingletonClass {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private final int count;
    public NotASingletonClass() { this.count = COUNTER.getAndIncrement(); }
  }

  @Singleton
  public static final class ASingletonClass implements BoundToSingletonInterface {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private final int count;
    public ASingletonClass() { this.count = COUNTER.getAndIncrement(); }
  }

  public static final class CustomScopeApplier implements ScopeApplier {
    private final Object returnVal;
    public CustomScopeApplier(Object returnVal) { this.returnVal = returnVal; }
    @Override @NotNull
    public <T> ProviderWithContext<T> applyScope(@NotNull BindingKey<T> $, @NotNull ProviderWithContext<T> $$) {
      //noinspection unchecked
      return ($$$) -> (T) this.returnVal; // trust me
    }
  }
  // @formatter:on
}
