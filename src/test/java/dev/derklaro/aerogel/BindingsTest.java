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
import dev.derklaro.aerogel.binding.builder.QualifiableBindingBuilder;
import dev.derklaro.aerogel.binding.builder.ScopeableBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.scope.SingletonScopeApplier;
import dev.derklaro.aerogel.internal.scope.UnscopedScopeApplier;
import io.leangen.geantyref.AnnotationFormatException;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BindingsTest {

  @Test
  void testSimpleBindingCreation() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(String.class).toInstance("Hello World!");
    Assertions.assertEquals(String.class, binding.mainKey().type());
    Assertions.assertFalse(binding.mainKey().qualifierAnnotationType().isPresent());
    Assertions.assertFalse(binding.scope().isPresent());
  }

  @Test
  void testChangingScopeApplierOnBinding() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(int.class).toInstance(1);
    Assertions.assertFalse(binding.scope().isPresent());
    Assertions.assertEquals(Integer.class, binding.mainKey().type());

    UninstalledBinding<?> withSingletonScope = binding.withScope(SingletonScopeApplier.INSTANCE);
    Assertions.assertEquals(Integer.class, withSingletonScope.mainKey().type());
    Assertions.assertTrue(withSingletonScope.scope().isPresent());
    Assertions.assertSame(SingletonScopeApplier.INSTANCE, withSingletonScope.scope().get());

    UninstalledBinding<?> withUnscopedScope = withSingletonScope.withScope(UnscopedScopeApplier.INSTANCE);
    Assertions.assertEquals(Integer.class, withUnscopedScope.mainKey().type());
    Assertions.assertTrue(withUnscopedScope.scope().isPresent());
    Assertions.assertSame(UnscopedScopeApplier.INSTANCE, withUnscopedScope.scope().get());
  }

  @Test
  void testBindingKeyIsNotChangedWhenChangingScope() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(int.class).toInstance(1);
    Assertions.assertFalse(binding.scope().isPresent());
    Assertions.assertEquals(Integer.class, binding.mainKey().type());

    UninstalledBinding<?> withSingletonScope = binding.withScope(SingletonScopeApplier.INSTANCE);
    Assertions.assertTrue(withSingletonScope.scope().isPresent());
    Assertions.assertSame(SingletonScopeApplier.INSTANCE, withSingletonScope.scope().get());
    Assertions.assertSame(binding.mainKey(), withSingletonScope.mainKey());
  }

  @Test
  void testLookupPassedToBinding() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(String.class).toInstance("Hello World!");
    Assertions.assertEquals(String.class, binding.mainKey().type());
    Assertions.assertTrue(binding.options().memberLookup().isPresent());
    Assertions.assertEquals("InjectorOptions", binding.options().memberLookup().get().lookupClass().getSimpleName());

    MethodHandles.Lookup lookup = MethodHandles.lookup();
    UninstalledBinding<?> bindingWithLookup = injector.createBindingBuilder()
      .bind(String.class)
      .memberLookup(lookup)
      .toInstance("Hello :)");
    Assertions.assertEquals(String.class, bindingWithLookup.mainKey().type());
    Assertions.assertTrue(bindingWithLookup.options().memberLookup().isPresent());
    Assertions.assertSame(lookup, bindingWithLookup.options().memberLookup().get());

    UninstalledBinding<?> withLookupAndScope = bindingWithLookup.withScope(SingletonScopeApplier.INSTANCE);
    Assertions.assertEquals(String.class, withLookupAndScope.mainKey().type());
    Assertions.assertTrue(withLookupAndScope.scope().isPresent());
    Assertions.assertSame(SingletonScopeApplier.INSTANCE, withLookupAndScope.scope().get());
    Assertions.assertTrue(withLookupAndScope.options().memberLookup().isPresent());
    Assertions.assertSame(lookup, withLookupAndScope.options().memberLookup().get());
  }

  @Test
  void testBindingAnnotationProperlyAppliedToBindingKey() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(int.class)
      .qualifiedWith(NoMemberQualifier.class)
      .toInstance(1);
    BindingKey<?> key = binding.mainKey();
    Assertions.assertEquals(Integer.class, key.type());
    Assertions.assertFalse(key.qualifierAnnotation().isPresent());
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(NoMemberQualifier.class, key.qualifierAnnotationType().get());
  }

  @Test
  void testInvalidBindingAnnotationIsRejectedByBuilder() {
    Injector injector = Injector.newInjector();
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> injector.createBindingBuilder().bind(String.class).qualifiedWith(ValidScope.class));
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> injector.createBindingBuilder().bind(String.class).qualifiedWith(ClassQualifier.class));
  }

  @Test
  void testQualifiedWithName() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(String.class)
      .qualifiedWithName("world")
      .toInstance("World!");
    BindingKey<?> key = binding.mainKey();
    Assertions.assertEquals(String.class, key.type());
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(Named.class, key.qualifierAnnotationType().get());
    Assertions.assertTrue(key.qualifierAnnotation().isPresent());
    Named annotation = Assertions.assertInstanceOf(Named.class, key.qualifierAnnotation().get());
    Assertions.assertEquals("world", annotation.value());
  }

  @Test
  void testQualifiedWithAnnotationInstanceWithoutMembers() throws AnnotationFormatException {
    Annotation qualifierAnnotation = TypeFactory.annotation(NoMemberQualifier.class, Collections.emptyMap());
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(String.class)
      .qualifiedWith(qualifierAnnotation)
      .toInstance("World!");
    BindingKey<?> key = binding.mainKey();
    Assertions.assertEquals(String.class, key.type());
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(NoMemberQualifier.class, key.qualifierAnnotationType().get());
    Assertions.assertFalse(key.qualifierAnnotation().isPresent());
  }

  @Test
  void testQualifiedWithAnnotationInstanceWithMembers() throws AnnotationFormatException {
    Annotation qualifierAnnotation = TypeFactory.annotation(MemberQualifier.class, new HashMap<String, Object>() {{
      this.put("value", "world");
      this.put("target", String.class);
      this.put("retention", 12345);
    }});
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(String.class)
      .qualifiedWith(qualifierAnnotation)
      .toInstance("World!");
    BindingKey<?> key = binding.mainKey();
    Assertions.assertEquals(String.class, key.type());
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MemberQualifier.class, key.qualifierAnnotationType().get());
    Assertions.assertTrue(key.qualifierAnnotation().isPresent());
    MemberQualifier annotation = Assertions.assertInstanceOf(MemberQualifier.class, key.qualifierAnnotation().get());
    Assertions.assertEquals("world", annotation.value());
    Assertions.assertEquals(String.class, annotation.target());
    Assertions.assertEquals(12345, annotation.retention());
  }

  @Test
  void testQualifiedWithAnnotationInstanceThatIsNotAQualifier() throws AnnotationFormatException {
    Annotation notAQualifierAnnotation = TypeFactory.annotation(ValidScope.class, Collections.emptyMap());
    Injector injector = Injector.newInjector();
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> injector.createBindingBuilder().bind(String.class).qualifiedWith(notAQualifierAnnotation));
  }

  @Test
  void testQualifierBuildingWorksWithNoMemberQualifier() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(NoMemberQualifier.class).require()
      .toInstance("Hello World");
    BindingKey<?> key = binding.mainKey();
    Assertions.assertEquals(String.class, key.type());
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(NoMemberQualifier.class, key.qualifierAnnotationType().get());
    Assertions.assertFalse(key.qualifierAnnotation().isPresent());
  }

  @Test
  void testQualifierBuildingWorksWithMultipleMemberQualifier() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::value).returns("testing")
      .property(MemberQualifier::target).returns(String.class)
      .property(MemberQualifier::retention).returns(12345)
      .require()
      .toInstance("Hello World");
    BindingKey<?> key = binding.mainKey();
    Assertions.assertEquals(String.class, key.type());
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MemberQualifier.class, key.qualifierAnnotationType().get());
    Assertions.assertTrue(key.qualifierAnnotation().isPresent());
    MemberQualifier annotation = Assertions.assertInstanceOf(MemberQualifier.class, key.qualifierAnnotation().get());
    Assertions.assertEquals("testing", annotation.value());
    Assertions.assertEquals(String.class, annotation.target());
    Assertions.assertEquals(12345, annotation.retention());
  }

  @Test
  void testInvalidTypesAreCaughtByQualifierBuilder() {
    Injector injector = Injector.newInjector();
    Exception thrown = Assertions.assertThrows(IllegalStateException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::value).returns("testing")
      .property(mq -> (Type) mq.target()).returns(TypeFactory.parameterizedClass(List.class, String.class))
      .property(MemberQualifier::retention).returns(12345)
      .require());
    Assertions.assertTrue(thrown.getMessage().contains("Invalid return type for annotation member target"));
  }

  @Test
  void testInvalidTypesAreCaughtWhenUsingSupplier() {
    Injector injector = Injector.newInjector();
    Exception thrown = Assertions.assertThrows(IllegalStateException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::value).returns("testing")
      .property(mq -> (Type) mq.target()).returnLazySupply(() -> TypeFactory.parameterizedClass(Set.class, Byte.class))
      .property(MemberQualifier::retention).returns(12345)
      .require());
    Assertions.assertTrue(thrown.getMessage().contains("Invalid return type for annotation member target"));
  }

  @Test
  void testReturningSomethingFromPropertyMethodWithoutCallingMethodThrowsException() {
    Injector injector = Injector.newInjector();
    Exception thrown = Assertions.assertThrows(IllegalStateException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::value).returns("testing")
      .property(mq -> String.class).returns(String.class)
      .property(MemberQualifier::retention).returns(12345)
      .require());
    Assertions.assertEquals("Accessor did not call any annotation method", thrown.getMessage());
  }

  @Test
  @SuppressWarnings("ConstantValue")
  void testCallingStandardAnnotationTypeMethodsIsForbidden() {
    Injector injector = Injector.newInjector();
    Exception thrownTS = Assertions.assertThrows(IllegalArgumentException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::toString).returns("world")
      .require());
    Exception thrownEQ = Assertions.assertThrows(IllegalArgumentException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(mq -> mq.equals(null)).returns(true)
      .require());
    Exception thrownHC = Assertions.assertThrows(IllegalArgumentException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::hashCode).returns(12345)
      .require());
    Exception thrownAT = Assertions.assertThrows(IllegalArgumentException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::annotationType).returns(MemberQualifier.class)
      .require());
    String expectedErrorMessage = "Custom impl of equals/hashCode/toString/annotationType is not allowed";
    Assertions.assertEquals(expectedErrorMessage, thrownTS.getMessage());
    Assertions.assertEquals(expectedErrorMessage, thrownEQ.getMessage());
    Assertions.assertEquals(expectedErrorMessage, thrownHC.getMessage());
    Assertions.assertEquals(expectedErrorMessage, thrownAT.getMessage());
  }

  @Test
  void testDuplicatePropertyDefinitionThrowsException() {
    Injector injector = Injector.newInjector();
    Exception thrown = Assertions.assertThrows(IllegalStateException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::retention).returns(12345)
      .property(MemberQualifier::retention).returns(56789)
      .require());
    Assertions.assertEquals("Annotation method impl already set for retention", thrown.getMessage());
  }

  @Test
  void testNullValueIsRejectedAsValidAnnotationMemberValue() {
    Injector injector = Injector.newInjector();
    Exception thrown = Assertions.assertThrows(IllegalStateException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::value).returnLazySupply(() -> null)
      .property(MemberQualifier::target).returns(String.class)
      .property(MemberQualifier::retention).returns(12345)
      .require());
    Assertions.assertEquals("Unable to get value for annotation property value", thrown.getMessage());
  }

  @Test
  void testNullValueCausesDefaultValueToBeReturnedIfEnabled() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::value).orDefault().returnLazySupply(() -> null)
      .property(MemberQualifier::target).returns(String.class)
      .property(MemberQualifier::retention).returns(12345)
      .require()
      .toInstance("World :)");
    BindingKey<?> key = binding.mainKey();
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MemberQualifier.class, key.qualifierAnnotationType().get());
    Assertions.assertTrue(key.qualifierAnnotation().isPresent());
    MemberQualifier annotation = Assertions.assertInstanceOf(MemberQualifier.class, key.qualifierAnnotation().get());
    Assertions.assertEquals("test12", annotation.value());
    Assertions.assertEquals(String.class, annotation.target());
    Assertions.assertEquals(12345, annotation.retention());
  }

  @Test
  void testQualifierBuilderDefaultValueIsNotReturnedOnComputationError() {
    Injector injector = Injector.newInjector();
    Exception thrown = Assertions.assertThrows(IllegalStateException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::value).returnLazyCall(() -> {
        throw new IllegalStateException("hello world :)");
      })
      .property(MemberQualifier::target).returns(String.class)
      .property(MemberQualifier::retention).returns(12345)
      .require());
    Assertions.assertEquals("Unable to provide value for annotation member: value", thrown.getMessage());
  }

  @Test
  void testQualifierBuilderReturnsDefaultValueOnComputationErrorWhenEnabled() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(String.class)
      .buildQualifier(MemberQualifier.class)
      .property(MemberQualifier::value).orDefault().returnLazyCall(() -> {
        throw new IllegalStateException("hello world :)");
      })
      .property(MemberQualifier::target).returns(String.class)
      .property(MemberQualifier::retention).returns(12345)
      .require()
      .toInstance("World :)");
    BindingKey<?> key = binding.mainKey();
    Assertions.assertTrue(key.qualifierAnnotationType().isPresent());
    Assertions.assertEquals(MemberQualifier.class, key.qualifierAnnotationType().get());
    Assertions.assertTrue(key.qualifierAnnotation().isPresent());
    MemberQualifier annotation = Assertions.assertInstanceOf(MemberQualifier.class, key.qualifierAnnotation().get());
    Assertions.assertEquals("test12", annotation.value());
    Assertions.assertEquals(String.class, annotation.target());
    Assertions.assertEquals(12345, annotation.retention());
  }

  @Test
  void testNoScopeIsDetectedIfNoScopeIsDefined() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(UnscopedClass.class).toConstructingSelf();
    Assertions.assertFalse(binding.scope().isPresent());
  }

  @Test
  void testSingletonScopeIsDetectedFromBindingClass() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(SingletonClass.class).toConstructingSelf();
    Assertions.assertTrue(binding.scope().isPresent());
    Assertions.assertSame(SingletonScopeApplier.INSTANCE, binding.scope().get());
  }

  @Test
  void testQualifierAnnotationsAreIgnoredIfScopeIsExplicitlyRemoved() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(SingletonClass.class)
      .unscoped()
      .toConstructingSelf();
    Assertions.assertTrue(binding.scope().isPresent());
    Assertions.assertSame(UnscopedScopeApplier.INSTANCE, binding.scope().get());
  }

  @Test
  void testExplicitlyAddingSingletonToUnscopedClass() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(UnscopedClass.class)
      .scopedWithSingleton()
      .toConstructingSelf();
    Assertions.assertTrue(binding.scope().isPresent());
    Assertions.assertSame(SingletonScopeApplier.INSTANCE, binding.scope().get());
  }

  @Test
  void testCustomScopeOverridesExplicitlyDefinedScope() {
    Injector injector = Injector.newInjector();
    ScopeApplier customScopeApplier = new ValidScopeScopeApplier();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(UnscopedClass.class)
      .scopedWith(customScopeApplier)
      .toConstructingSelf();
    Assertions.assertTrue(binding.scope().isPresent());
    Assertions.assertSame(customScopeApplier, binding.scope().get());
  }

  @Test
  void testCustomScopeAnnotationCanBeResolvedFromInjectorScopeRegistry() {
    Injector injector = Injector.newInjector();
    ScopeApplier customScopeApplier = new ValidScopeScopeApplier();
    injector.scopeRegistry().register(ValidScope.class, customScopeApplier);
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(UnscopedClass.class)
      .scopedWith(ValidScope.class)
      .toConstructingSelf();
    Assertions.assertTrue(binding.scope().isPresent());
    Assertions.assertSame(customScopeApplier, binding.scope().get());
  }

  @Test
  void testThrowsExceptionIfScopedWithReceivesInvalidScopeAnnotation() {
    Injector injector = Injector.newInjector();
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> injector.createBindingBuilder().bind(UnscopedClass.class).scopedWith(SourceScope.class));
  }

  @Test
  void testThrowsExceptionIfScopeAnnotationCannotBeResolvedFromInjector() {
    Injector injector = Injector.newInjector();
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> injector.createBindingBuilder().bind(UnscopedClass.class).scopedWith(ValidScope.class));
  }

  @Test
  void testSingletonScopeIsRegisteredInInjectorByDefault() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(UnscopedClass.class)
      .scopedWith(Singleton.class)
      .toConstructingSelf();
    Assertions.assertTrue(binding.scope().isPresent());
    Assertions.assertSame(SingletonScopeApplier.INSTANCE, binding.scope().get());
  }

  @Test
  void testFactoryMethodCanBeUsedAsTarget() throws NoSuchMethodException {
    Injector injector = Injector.newInjector();
    Method factoryMethod = BindingsTest.class.getMethod("factoryForString");
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(String.class).toFactoryMethod(factoryMethod);
    Assertions.assertEquals(String.class, binding.mainKey().type());
  }

  @Test
  void testFactoryMethodForPrimitiveTypeIsAccepted() throws NoSuchMethodException {
    Injector injector = Injector.newInjector();
    Method factoryMethod = BindingsTest.class.getMethod("factoryForInt");
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(int.class).toFactoryMethod(factoryMethod);
    Assertions.assertEquals(Integer.class, binding.mainKey().type());
  }

  @Test
  void testNonStaticFactoryMethodIsRejected() throws NoSuchMethodException {
    Injector injector = Injector.newInjector();
    Method factoryMethod = BindingsTest.class.getMethod("nonStaticFactory");
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> injector.createBindingBuilder().bind(String.class).toFactoryMethod(factoryMethod));
  }

  @Test
  void testFactoryMethodReturningWrongTypeIsRejected() throws NoSuchMethodException {
    Injector injector = Injector.newInjector();
    Method factoryMethod = BindingsTest.class.getMethod("factoryForInt");
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> injector.createBindingBuilder().bind(String.class).toFactoryMethod(factoryMethod));
  }

  @Test
  void testProviderForBinding() {
    Injector injector = Injector.newInjector();
    Provider<String> stringProvider = new ProviderForString();
    UninstalledBinding<?> binding = injector.createBindingBuilder().bind(String.class).toProvider(stringProvider);
    Assertions.assertEquals(String.class, binding.mainKey().type());
  }

  @Test
  void testProviderClassForBinding() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(String.class)
      .toProvider(ProviderForString.class);
    Assertions.assertEquals(String.class, binding.mainKey().type());
  }

  @Test
  void testAbstractProviderClassForBindingIsRejected() {
    Injector injector = Injector.newInjector();
    Assertions.assertThrows(IllegalArgumentException.class, () -> injector.createBindingBuilder()
      .bind(String.class)
      .toProvider(AbstractProviderForString.class));
  }

  @Test
  void testBindToConstructor() throws NoSuchMethodException {
    Injector injector = Injector.newInjector();
    Constructor<? extends UnscopedClass> constructor = UnscopedClass.class.getConstructor();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(UnscopedClass.class)
      .toConstructor(constructor);
    Assertions.assertEquals(UnscopedClass.class, binding.mainKey().type());
  }

  @Test
  void testBindToClass() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<?> binding = injector.createBindingBuilder()
      .bind(Provider.class)
      .toConstructingClass(ProviderForString.class);
    Assertions.assertEquals(Provider.class, binding.mainKey().type());
  }

  @Test
  void testBindToAbstractClassIsRejected() {
    Injector injector = Injector.newInjector();
    Assertions.assertThrows(IllegalArgumentException.class, () -> injector.createBindingBuilder()
      .bind(Provider.class)
      .toConstructingClass(AbstractProviderForString.class));
  }

  @Test
  void testBindToGenericType() {
    Injector injector = Injector.newInjector();
    Type listStringType = TypeFactory.parameterizedClass(List.class, String.class);
    QualifiableBindingBuilder<List<String>> bindingBuilder = injector.createBindingBuilder().bind(listStringType);
    UninstalledBinding<List<String>> binding = bindingBuilder.toInstance(new ArrayList<>());
    Assertions.assertEquals(listStringType, binding.mainKey().type());
  }

  @Test
  void testBindToFactoryIsRejectedOnGenericTypeMismatch() throws NoSuchMethodException {
    Injector injector = Injector.newInjector();
    Type listStringType = TypeFactory.parameterizedClass(List.class, String.class);
    Method factoryMethod = BindingsTest.class.getMethod("factorForSetString");
    QualifiableBindingBuilder<List<String>> bindingBuilder = injector.createBindingBuilder().bind(listStringType);
    Assertions.assertThrows(IllegalArgumentException.class, () -> bindingBuilder.toFactoryMethod(factoryMethod));
  }

  @Test
  void testBindTypeTokenToGenericFactoryMethodSucceeds() throws NoSuchMethodException {
    Injector injector = Injector.newInjector();
    TypeToken<Set<String>> typeToken = new TypeToken<Set<String>>() {
    };
    Method factoryMethod = BindingsTest.class.getMethod("factorForSetString");
    QualifiableBindingBuilder<Set<String>> bindingBuilder = injector.createBindingBuilder().bind(typeToken);
    UninstalledBinding<Set<String>> binding = bindingBuilder.toFactoryMethod(factoryMethod);
    Assertions.assertEquals(typeToken.getType(), binding.mainKey().type());
  }

  @Test
  void testBindBindingKeyToGenericFactoryMethodSucceeds() throws NoSuchMethodException {
    Injector injector = Injector.newInjector();
    Type setStringType = TypeFactory.parameterizedClass(Set.class, String.class);
    BindingKey<Set<String>> setStringBindingKey = BindingKey.of(setStringType);
    Method factoryMethod = BindingsTest.class.getMethod("factorForSetString");
    ScopeableBindingBuilder<Set<String>> bindingBuilder = injector.createBindingBuilder().bind(setStringBindingKey);
    UninstalledBinding<Set<String>> binding = bindingBuilder.toFactoryMethod(factoryMethod);
    Assertions.assertEquals(setStringType, binding.mainKey().type());
  }

  @Test
  void testCascadedBindingKeepsCharacteristicsOfCascadedTarget() {
    Injector injector = Injector.newInjector();
    UninstalledBinding<Object> singletonObjectBinding = injector.createBindingBuilder()
      .bind(Object.class)
      .scopedWithSingleton()
      .toProvider(Object::new);
    UninstalledBinding<Object> cascadedBinding = injector.createBindingBuilder()
      .bind(Object.class)
      .qualifiedWithName("cascaded")
      .cascadeTo(singletonObjectBinding);
    injector.installBinding(singletonObjectBinding).installBinding(cascadedBinding);

    Object instanceA = injector.instance(Object.class);
    Object instanceB = injector.instance(Object.class);
    Assertions.assertSame(instanceA, instanceB);

    Object instanceC = injector.instance(cascadedBinding.mainKey());
    Object instanceD = injector.instance(cascadedBinding.mainKey());
    Assertions.assertSame(instanceC, instanceD);
    Assertions.assertSame(instanceA, instanceC);
  }

  @Test
  @SuppressWarnings("rawtypes")
  void testBindingMultipleKeysToOneBinding() {
    Injector injector = Injector.newInjector();
    Type listWildcardType = TypeFactory.parameterizedClass(List.class, TypeFactory.unboundWildcard());
    BindingKey<AbstractList> abstractListWithQualifier = BindingKey
      .of(AbstractList.class)
      .withQualifier(NoMemberQualifier.class);
    UninstalledBinding<Collection> binding = injector.createBindingBuilder()
      .bind(Collection.class)
      .andBind(ArrayList.class)
      .andBind(listWildcardType)
      .andBind(abstractListWithQualifier)
      .andBind(TypeToken.get(AbstractCollection.class))
      .scopedWithSingleton()
      .toProvider(() -> new ArrayList());
    injector.installBinding(binding);

    Collection colA = injector.instance(Collection.class);
    Collection colB = injector.instance(ArrayList.class);
    Collection colC = injector.instance(listWildcardType);
    Collection colD = injector.instance(abstractListWithQualifier);
    Collection colE = injector.instance(TypeToken.get(AbstractCollection.class));
    Assertions.assertSame(colA, colB);
    Assertions.assertSame(colA, colC);
    Assertions.assertSame(colA, colD);
    Assertions.assertSame(colA, colE);

    BindingKey<AbstractList> abstractListWithoutQualifier = abstractListWithQualifier.withoutQualifier();
    Assertions.assertThrows(IllegalArgumentException.class, () -> injector.binding(abstractListWithoutQualifier));
  }

  // @formatter:off
  @Scope @Retention(RetentionPolicy.SOURCE) public @interface SourceScope {}
  @Scope @Retention(RetentionPolicy.RUNTIME) public @interface ValidScope {}
  @Qualifier @Retention(RetentionPolicy.RUNTIME) public @interface NoMemberQualifier {}
  @Qualifier @Retention(RetentionPolicy.RUNTIME) public @interface MemberQualifier {
    String value() default "test12";
    Class<?> target();
    int retention();
  }
  @Retention(RetentionPolicy.CLASS) public @interface ClassQualifier {}
  public static final class UnscopedClass {}
  @Singleton public static final class SingletonClass {}
  public static final class ValidScopeScopeApplier implements ScopeApplier {
    @Override @NotNull
    public<T> ProviderWithContext<T> applyScope(
      @NotNull List<BindingKey<? extends T>> keys,
      @NotNull ProviderWithContext<T> original
    ) {
      return SingletonScopeApplier.INSTANCE.applyScope(keys, original);
    }
  }
  public static final class ProviderForString implements Provider<String> {
    @Override public String get() {
      return "hello world";
    }
  }
  public static abstract class AbstractProviderForString implements Provider<String> {}
  public String nonStaticFactory() { return "World"; }
  public static String factoryForString() { return "Hello World"; }
  public static int factoryForInt() { return 1; }
  public static Set<String> factorForSetString() { return new HashSet<>(); }
  // @formatter:on
}
