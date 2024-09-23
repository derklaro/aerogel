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
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InjectorTest {

  @Test
  void testParentInjectorIsKnownToChildInjector() {
    {
      Injector parent = Injector.newInjector();
      Assertions.assertFalse(parent.parentInjector().isPresent());
    }

    {
      Injector parent = Injector.newInjector();
      Injector child = parent.createChildInjector();
      Assertions.assertFalse(parent.parentInjector().isPresent());
      Assertions.assertTrue(child.parentInjector().isPresent());
      Assertions.assertSame(parent, child.parentInjector().get());
    }

    {
      Injector parent = Injector.newInjector();
      Injector child1 = parent.createChildInjector();
      Injector child2 = child1.createChildInjector();
      Injector child3 = parent.createChildInjector();
      Assertions.assertFalse(parent.parentInjector().isPresent());
      Assertions.assertTrue(child1.parentInjector().isPresent());
      Assertions.assertTrue(child2.parentInjector().isPresent());
      Assertions.assertTrue(child3.parentInjector().isPresent());
      Assertions.assertSame(parent, child1.parentInjector().get());
      Assertions.assertSame(child1, child2.parentInjector().get());
      Assertions.assertSame(parent, child3.parentInjector().get());
    }
  }

  @Test
  void testBindingBuilderIsAlwaysPresentAndNew() {
    Injector injector = Injector.newInjector();
    RootBindingBuilder rbb1 = injector.createBindingBuilder();
    RootBindingBuilder rbb2 = injector.createBindingBuilder();
    Assertions.assertNotNull(rbb1);
    Assertions.assertNotNull(rbb2);
    Assertions.assertNotSame(rbb1, rbb2);

    Injector child = injector.createChildInjector();
    RootBindingBuilder rbb3 = child.createBindingBuilder();
    RootBindingBuilder rbb4 = child.createBindingBuilder();
    Assertions.assertNotNull(rbb3);
    Assertions.assertNotNull(rbb4);
    Assertions.assertNotSame(rbb3, rbb4);
    Assertions.assertNotSame(rbb1, rbb3);
    Assertions.assertNotSame(rbb2, rbb4);
  }

  @Test
  void testMemberInjectionGet() {
    Injector injector = Injector.newInjector();
    MemberInjector<TestItfImpl> memberInjector = injector.memberInjector(TestItfImpl.class);
    Assertions.assertNotNull(memberInjector);
    Assertions.assertSame(injector, memberInjector.injector());
    Assertions.assertEquals(TestItfImpl.class, memberInjector.target());

    MemberInjector<TestItfImpl> secondMemberInjector = injector.memberInjector(TestItfImpl.class);
    Assertions.assertSame(memberInjector, secondMemberInjector);

    MemberInjector<TestItfImpl> miWithLookup = injector.memberInjector(TestItfImpl.class, MethodHandles.lookup());
    Assertions.assertSame(memberInjector, miWithLookup); // lookup isn't allowed to make a difference
  }

  @Test
  void testImplementationInstanceCanBeGetDirectly() {
    Injector injector = Injector.newInjector();
    TestItfImpl impl1 = injector.instance(TestItfImpl.class);
    TestItfImpl impl2 = injector.instance((Type) TestItfImpl.class); // explicit cast to use the type method
    TestItfImpl impl3 = injector.instance(TypeToken.get(TestItfImpl.class));
    TestItfImpl impl4 = injector.instance(BindingKey.of(TestItfImpl.class));
    Assertions.assertNotSame(impl1, impl2);
    Assertions.assertNotSame(impl3, impl4);
    Assertions.assertNotSame(impl1, impl3);
    Assertions.assertNotSame(impl2, impl4);
  }

  @Test
  void testJitBindingCreatedForInterfaceBasedOnProvidedBy() {
    Injector injector = Injector.newInjector();
    TestItf instance = injector.instance(TestItf.class);
    Assertions.assertNotNull(instance);
    Assertions.assertInstanceOf(TestItfImpl.class, instance);
  }

  @Test
  void testNoJitBindingCreatedWhenDisabled() {
    Injector injector = Injector.builder().jitBindingFilter(key -> false).build();
    Throwable thrown = Assertions.assertThrows(IllegalStateException.class, () -> injector.instance(TestItf.class));
    Assertions.assertNotNull(thrown.getMessage());
    Assertions.assertTrue(thrown.getMessage().startsWith("Creating of jit binding for key"));
    Assertions.assertTrue(thrown.getMessage().endsWith("is explicitly disabled"));
  }

  @Test
  void testJitDisabledForSpecificType() {
    Injector injector = Injector.builder().jitBindingFilter(key -> key.type() != TestItf.class).build();
    Assertions.assertThrows(IllegalStateException.class, () -> injector.instance(TestItf.class));
    Assertions.assertDoesNotThrow(() -> injector.instance(TestItfImpl.class));
  }

  @Test
  void testJitBindingRuleAppliesToChildInjectorsAsWell() {
    Injector injector = Injector.builder().jitBindingFilter(key -> false).build();
    Injector child = injector.createChildInjector();
    Assertions.assertThrows(IllegalStateException.class, () -> injector.instance(TestItf.class));
    Assertions.assertThrows(IllegalStateException.class, () -> child.instance(TestItf.class));
  }

  @Test
  void testBindingIsSharedBetweenParentAndChild() {
    Injector injector = Injector.newInjector();
    Injector childInjector = injector.createChildInjector();
    SingletonTest fromRoot = injector.instance(SingletonTest.class);
    SingletonTest fromChild = childInjector.instance(SingletonTest.class);
    Assertions.assertSame(fromRoot, fromChild);
  }

  @Test
  void testDynamicBindingFromParentIsResolvedInChild() {
    TestItf instance = new TestItfImpl();
    Injector injector = Injector.newInjector();
    DynamicBinding binding = injector.createBindingBuilder()
      .bindDynamically()
      .exactRawType(TestItf.class)
      .toKeyedBindingProvider((key, builder) -> builder.toInstance(instance));
    injector.installBinding(binding);

    Injector child = injector.createChildInjector();
    TestItf resolvedFromChild = child.instance(TestItf.class);
    Assertions.assertSame(instance, resolvedFromChild);

    TestItf resolvedFromRoot = injector.instance(TestItf.class);
    Assertions.assertSame(instance, resolvedFromRoot);
  }

  @Test
  void testDynamicCreationOfProviderBinding() {
    TypeToken<Provider<TestItf>> testItfProviderToken = new TypeToken<Provider<TestItf>>() {
    };
    TypeToken<Provider<TestItfImpl>> testItfImplProviderToken = new TypeToken<Provider<TestItfImpl>>() {
    };

    TestItfImpl testItfInstance = new TestItfImpl();
    Provider<TestItf> manualProvider = TestItfImpl::new;

    Injector injector = Injector.newInjector();
    UninstalledBinding<Provider<TestItf>> providerBinding = injector.createBindingBuilder()
      .bind(testItfProviderToken)
      .toInstance(manualProvider);
    UninstalledBinding<TestItfImpl> testItfImplBinding = injector.createBindingBuilder()
      .bind(TestItfImpl.class)
      .toInstance(testItfInstance);
    injector
      .installBinding(providerBinding)
      .installBinding(testItfImplBinding);

    Provider<TestItf> testItfProvider = injector.instance(testItfProviderToken);
    Assertions.assertSame(manualProvider, testItfProvider);

    Provider<TestItfImpl> testItfImplProvider = injector.instance(testItfImplProviderToken);
    Assertions.assertNotNull(testItfImplProvider);
    Assertions.assertSame(testItfInstance, testItfImplProvider.get());
  }

  @Test
  void testMemberInjectorViaJitBinding() {
    TypeToken<MemberInjector<TestItfImpl>> testItfMemberInjectorTypeToken = new TypeToken<MemberInjector<TestItfImpl>>() {
    };

    Injector injector = Injector.newInjector();
    MemberInjector<TestItfImpl> memberInjector = injector.instance(testItfMemberInjectorTypeToken);
    Assertions.assertEquals(TestItfImpl.class, memberInjector.target());
    Assertions.assertSame(injector, memberInjector.injector());
  }

  @Test
  void testJitNotCreatedIfQualifierAnnotationIsPresent() {
    Injector injector = Injector.newInjector();
    BindingKey<TestItfImpl> testItfBindingKey = BindingKey.of(TestItfImpl.class).withQualifier(Named.class);
    Throwable thrown = Assertions.assertThrows(IllegalStateException.class, () -> injector.instance(testItfBindingKey));
    Assertions.assertNotNull(thrown.getMessage());
    Assertions.assertTrue(thrown.getMessage().startsWith("Unable to create JIT binding for key with qualifier"));
  }

  @Test
  void testJitBindingCanBeCreatedForUnqualifiedInjector() throws AnnotationFormatException {
    Injector injector = Injector.newInjector();
    Injector currentInjector = injector.instance(Injector.class);
    Assertions.assertSame(injector, currentInjector);

    Named named = TypeFactory.annotation(Named.class, Collections.singletonMap("value", "test"));
    BindingKey<Injector> injectorKey = BindingKey.of(Injector.class).withQualifier(named);
    Throwable thrown = Assertions.assertThrows(IllegalStateException.class, () -> injector.instance(injectorKey));
    Assertions.assertNotNull(thrown.getMessage());
    Assertions.assertTrue(thrown.getMessage().startsWith("Unable to create JIT binding for key with qualifier"));
  }

  @Test
  void testProvidedByValidatedToNotBeSameClass() {
    Injector injector = Injector.newInjector();
    Throwable thrown = Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> injector.instance(ProvidedBySameClass.class));
    Assertions.assertNotNull(thrown.getMessage());
    Assertions.assertTrue(thrown.getMessage().startsWith("@ProvidedBy: implementation"));
    Assertions.assertTrue(thrown.getMessage().endsWith("is the same as target"));
  }

  @Test
  void testProvidedByValidatesThatImplementationIsActuallySubclass() {
    Injector injector = Injector.newInjector();
    Throwable thrown = Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> injector.instance(ProvidedByNonSubclass.class));
    Assertions.assertNotNull(thrown.getMessage());
    Assertions.assertTrue(thrown.getMessage().startsWith("@ProvidedBy: implementation"));
    Assertions.assertTrue(thrown.getMessage().contains("is actually not an implementation of"));
  }

  @Test
  void testExplictBindingInParentInjectorIsPreferredOverJitBinding() {
    Injector injector = Injector.newInjector();
    Injector childInjector = injector.createChildInjector();

    BindingKey<String> key = BindingKey.of(String.class);
    UninstalledBinding<String> binding = injector.createBindingBuilder().bind(key).toInstance("World!");
    injector.installBinding(binding);

    InstalledBinding<String> viaExisting = injector.existingBinding(key).orElse(null);
    Assertions.assertNotNull(viaExisting);

    InstalledBinding<String> viaDirect = injector.binding(key);
    Assertions.assertSame(viaExisting, viaDirect);

    InstalledBinding<String> viaExistingChild = childInjector.existingBinding(key).orElse(null);
    Assertions.assertNotNull(viaExistingChild);
    Assertions.assertSame(viaExisting, viaExistingChild);

    InstalledBinding<String> viaDirectChild = childInjector.binding(key);
    Assertions.assertSame(viaDirect, viaDirectChild);
  }

  @Test
  void testChildInjectorBindingIsPreferredOverParentBinding() {
    Injector injector = Injector.newInjector();
    Injector childInjector = injector.createChildInjector();

    BindingKey<String> key = BindingKey.of(String.class);
    UninstalledBinding<String> parentBinding = injector.createBindingBuilder().bind(key).toInstance("World!");
    injector.installBinding(parentBinding);

    UninstalledBinding<String> childBinding = childInjector.createBindingBuilder().bind(key).toInstance("Hello!");
    childInjector.installBinding(childBinding);

    Assertions.assertEquals("World!", injector.instance(key));
    Assertions.assertEquals("Hello!", childInjector.instance(key));
  }

  @Test
  void testReconstructOfJitBindingInChildInjectorPrefersChildBindings() throws AnnotationFormatException {
    Injector injector = Injector.newInjector();
    Injector childInjector = injector.createChildInjector();

    Named named = TypeFactory.annotation(Named.class, Collections.singletonMap("value", "test"));
    BindingKey<String> key = BindingKey.of(String.class).withQualifier(named);
    UninstalledBinding<String> rootBinding = injector.createBindingBuilder().bind(key).toInstance("World!");
    injector.installBinding(rootBinding);

    UninstalledBinding<String> childBinding = childInjector.createBindingBuilder().bind(key).toInstance("Hello!");
    childInjector.installBinding(childBinding);

    InjectableClass instanceRoot = injector.instance(InjectableClass.class);
    InjectableClass instanceTargeted = childInjector.instance(InjectableClass.class);
    Assertions.assertEquals("World!", instanceRoot.test);
    Assertions.assertEquals("Hello!", instanceTargeted.test);
  }

  @Test
  void testReconstructOfJitBindingInChildInjectorPrefersChildBindingsWithProvider() throws AnnotationFormatException {
    Injector injector = Injector.newInjector();
    Injector childInjector = injector.createChildInjector();

    Named named = TypeFactory.annotation(Named.class, Collections.singletonMap("value", "test"));
    BindingKey<String> key = BindingKey.of(String.class).withQualifier(named);
    UninstalledBinding<String> rootBinding = injector.createBindingBuilder().bind(key).toInstance("World!");
    injector.installBinding(rootBinding);

    UninstalledBinding<String> childBinding = childInjector.createBindingBuilder().bind(key).toInstance("Hello!");
    childInjector.installBinding(childBinding);

    Provider<InjectableClass> rootProvider = injector.provider(BindingKey.of(InjectableClass.class));
    Provider<InjectableClass> childProvider = childInjector.provider(BindingKey.of(InjectableClass.class));
    Assertions.assertEquals("World!", rootProvider.get().test);
    Assertions.assertEquals("Hello!", childProvider.get().test);
  }

  @Test
  void testReuseOfTargetedInjectorBuilderIsPermitted() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<String> binding = injector.createBindingBuilder().bind(String.class).toInstance("world!");

    TargetedInjectorBuilder builder = injector.createTargetedInjectorBuilder();
    Assertions.assertDoesNotThrow(() -> builder.installBinding(binding));
    Assertions.assertDoesNotThrow(builder::build);

    Throwable thrownInstall = Assertions.assertThrows(
      IllegalStateException.class,
      () -> builder.installBinding(binding));
    Assertions.assertNotNull(thrownInstall.getMessage());
    Assertions.assertEquals("specific injector builder already targeted", thrownInstall.getMessage());

    Throwable thrownBuild = Assertions.assertThrows(IllegalStateException.class, builder::build);
    Assertions.assertNotNull(thrownBuild.getMessage());
    Assertions.assertEquals("specific injector builder already targeted", thrownBuild.getMessage());
  }

  @Test
  void testTargetedInjectorBindingOverridesParentInjectorBinding() throws AnnotationFormatException {
    Injector injector = Injector.newInjector();

    Named named = TypeFactory.annotation(Named.class, Collections.singletonMap("value", "test"));
    BindingKey<String> key = BindingKey.of(String.class).withQualifier(named);
    UninstalledBinding<String> rootBinding = injector.createBindingBuilder().bind(key).toInstance("World!");
    injector.installBinding(rootBinding);

    UninstalledBinding<String> targetedBinding = injector.createBindingBuilder().bind(key).toInstance("Hello!");
    Injector targeted = injector.createTargetedInjectorBuilder().installBinding(targetedBinding).build();

    InjectableClass instanceRoot = injector.instance(InjectableClass.class);
    InjectableClass instanceTargeted = targeted.instance(InjectableClass.class);
    Assertions.assertEquals("World!", instanceRoot.test);
    Assertions.assertEquals("Hello!", instanceTargeted.test);
  }

  @Test
  void testInjectorJitBindingAlwaysReturnsCurrentInjector() {
    Injector injector = Injector.newInjector();
    Injector childInjector = injector.createChildInjector();

    Injector fromParent = injector.instance(Injector.class);
    Assertions.assertSame(injector, fromParent);

    Injector fromChild = childInjector.instance(Injector.class);
    Assertions.assertSame(childInjector, fromChild);
  }

  @Test
  void testProviderJitBindingUsesBindingsFromCurrentInjector() throws AnnotationFormatException {
    TypeToken<Provider<InjectableClass>> classProviderToken = new TypeToken<Provider<InjectableClass>>() {
    };

    Injector injector = Injector.newInjector();
    Injector childInjector = injector.createChildInjector();

    Named named = TypeFactory.annotation(Named.class, Collections.singletonMap("value", "test"));
    BindingKey<String> key = BindingKey.of(String.class).withQualifier(named);
    UninstalledBinding<String> parentBinding = injector.createBindingBuilder().bind(key).toInstance("World!");
    injector.installBinding(parentBinding);

    UninstalledBinding<String> childBinding = childInjector.createBindingBuilder().bind(key).toInstance("Hello!");
    childInjector.installBinding(childBinding);

    Provider<InjectableClass> fromParent = injector.instance(classProviderToken);
    InjectableClass parentInstance = fromParent.get();
    Assertions.assertEquals("World!", parentInstance.test);

    Provider<InjectableClass> fromChild = childInjector.instance(classProviderToken);
    InjectableClass childInstance = fromChild.get();
    Assertions.assertEquals("Hello!", childInstance.test);
  }

  @Test
  void testMemberInjectorJitUsesCurrentInjector() {
    TypeToken<MemberInjector<InjectableClass>> memberInjectorTypeToken = new TypeToken<MemberInjector<InjectableClass>>() {
    };

    Injector injector = Injector.newInjector();
    Injector childInjector = injector.createChildInjector();

    MemberInjector<InjectableClass> fromParent = injector.instance(memberInjectorTypeToken);
    Assertions.assertSame(injector, fromParent.injector());

    MemberInjector<InjectableClass> fromChild = childInjector.instance(memberInjectorTypeToken);
    Assertions.assertSame(childInjector, fromChild.injector());
  }

  @Test
  void testInjectorProviderAlwaysReturnsFreshInstanceForNonSingleton() {
    Injector injector = Injector.newInjector();
    Provider<TestItfImpl> provider = injector.provider(BindingKey.of(TestItfImpl.class));
    TestItfImpl instance1 = provider.get();
    TestItfImpl instance2 = provider.get();
    Assertions.assertNotSame(instance1, instance2);
  }

  @Test
  void testTargetedInjectorUnregistersRegisteredBindingsOnClose() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<String> stringBinding = injector.createBindingBuilder()
      .bind(String.class)
      .toInstance("Hello");
    UninstalledBinding<Integer> intBinding = injector.createBindingBuilder()
      .bind(int.class)
      .toInstance(123);

    Injector targetedInjector = injector.createTargetedInjectorBuilder()
      .installBinding(stringBinding)
      .build();
    targetedInjector.installBinding(intBinding);

    // ensure bindings are present and in correct injector
    InstalledBinding<String> sb = targetedInjector.binding(BindingKey.of(String.class));
    Assertions.assertSame(targetedInjector, sb.installedInjector());
    InstalledBinding<Integer> ib = targetedInjector.binding(BindingKey.of(Integer.class));
    Assertions.assertSame(injector, ib.installedInjector());

    // call close, the int binding should be removed from parent injector now
    targetedInjector.close();
    InstalledBinding<String> sbr = targetedInjector.binding(BindingKey.of(String.class));
    Assertions.assertSame(targetedInjector, sbr.installedInjector());
    Assertions.assertTrue(targetedInjector.existingBinding(BindingKey.of(Integer.class)).isEmpty());
  }

  // @formatter:off
  @ProvidedBy(TestItfImpl.class) public interface TestItf {}
  @ProvidedBy(TestItfImpl.class) public interface ProvidedByNonSubclass {}
  public static class TestItfImpl implements TestItf {}
  @Singleton public static class SingletonTest {}
  @ProvidedBy(ProvidedBySameClass.class) public static final class ProvidedBySameClass {}
  public static final class InjectableClass {
    private final String test;
    @Inject public InjectableClass(@Named("test") String test) { this.test = test; }
  }
  // @formatter:on
}
