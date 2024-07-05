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

package dev.derklaro.aerogel.internal.binding.builder;

import dev.derklaro.aerogel.ScopeApplier;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.AdvancedBindingBuilder;
import dev.derklaro.aerogel.binding.builder.BindingAnnotationBuilder;
import dev.derklaro.aerogel.binding.builder.QualifiableBindingBuilder;
import dev.derklaro.aerogel.binding.builder.ScopeableBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.annotation.InjectAnnotationUtil;
import dev.derklaro.aerogel.internal.binding.BindingOptionsImpl;
import dev.derklaro.aerogel.internal.binding.UninstalledBindingImpl;
import dev.derklaro.aerogel.internal.binding.annotation.BindingAnnotationBuilderImpl;
import dev.derklaro.aerogel.internal.provider.ConstructingDelegatingProviderFactory;
import dev.derklaro.aerogel.internal.provider.ConstructorProviderFactory;
import dev.derklaro.aerogel.internal.provider.DelegatingProviderFactory;
import dev.derklaro.aerogel.internal.provider.FactoryMethodProviderFactory;
import dev.derklaro.aerogel.internal.provider.InstanceProviderFactory;
import dev.derklaro.aerogel.internal.provider.ProviderFactory;
import dev.derklaro.aerogel.internal.scope.SingletonScopeApplier;
import dev.derklaro.aerogel.internal.scope.UnscopedScopeApplier;
import dev.derklaro.aerogel.registry.Registry;
import io.leangen.geantyref.GenericTypeReflector;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ConcreteBindingBuilderImpl<T> implements QualifiableBindingBuilder<T> {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  // ==== must be set
  private final BindingKey<T> bindingKey;
  private final Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry;

  // ==== set lazily when required
  private final ScopeApplier scope;
  private final BindingOptionsImpl options;

  public ConcreteBindingBuilderImpl(
    @NotNull BindingKey<T> bindingKey,
    @NotNull BindingOptionsImpl bindingOptions,
    @NotNull Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry
  ) {
    this(bindingKey, scopeRegistry, null, bindingOptions);
  }

  public ConcreteBindingBuilderImpl(
    @NotNull BindingKey<T> bindingKey,
    @NotNull Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry,
    @Nullable ScopeApplier scope,
    @NotNull BindingOptionsImpl options
  ) {
    this.bindingKey = bindingKey;
    this.scopeRegistry = scopeRegistry;
    this.scope = scope;
    this.options = options;
  }

  @Override
  public @NotNull ScopeableBindingBuilder<T> qualifiedWithName(@NotNull String name) {
    return this.buildQualifier(Named.class).property(Named::value).returns(name).require();
  }

  @Override
  public @NotNull ScopeableBindingBuilder<T> qualifiedWith(@NotNull Annotation qualifierAnnotation) {
    BindingKey<T> newKey = this.bindingKey.withQualifier(qualifierAnnotation);
    return new ConcreteBindingBuilderImpl<>(newKey, this.scopeRegistry, this.scope, this.options);
  }

  @Override
  public @NotNull ScopeableBindingBuilder<T> qualifiedWith(
    @NotNull Class<? extends Annotation> qualifierAnnotationType
  ) {
    BindingKey<T> newKey = this.bindingKey.withQualifier(qualifierAnnotationType);
    return new ConcreteBindingBuilderImpl<>(newKey, this.scopeRegistry, this.scope, this.options);
  }

  @Override
  public @NotNull <A extends Annotation> BindingAnnotationBuilder<A, T> buildQualifier(
    @NotNull Class<A> qualifierAnnotationType
  ) {
    InjectAnnotationUtil.checkValidQualifierAnnotation(qualifierAnnotationType);
    return new BindingAnnotationBuilderImpl<>(qualifierAnnotationType, this);
  }

  @Override
  public @NotNull AdvancedBindingBuilder<T> unscoped() {
    return new ConcreteBindingBuilderImpl<>(
      this.bindingKey,
      this.scopeRegistry,
      UnscopedScopeApplier.INSTANCE,
      this.options);
  }

  @Override
  public @NotNull AdvancedBindingBuilder<T> scopedWithSingleton() {
    return new ConcreteBindingBuilderImpl<>(
      this.bindingKey,
      this.scopeRegistry,
      SingletonScopeApplier.INSTANCE,
      this.options);
  }

  @Override
  public @NotNull AdvancedBindingBuilder<T> scopedWith(@NotNull ScopeApplier scopeApplier) {
    return new ConcreteBindingBuilderImpl<>(this.bindingKey, this.scopeRegistry, scopeApplier, this.options);
  }

  @Override
  public @NotNull AdvancedBindingBuilder<T> scopedWith(@NotNull Class<? extends Annotation> scopeAnnotationType) {
    InjectAnnotationUtil.checkValidScopeAnnotation(scopeAnnotationType);
    ScopeApplier scopeApplier = this.scopeRegistry
      .get(scopeAnnotationType)
      .orElseThrow(() -> new IllegalArgumentException("scope annotation has no registered applier"));
    return this.scopedWith(scopeApplier);
  }

  @Override
  public @NotNull AdvancedBindingBuilder<T> memberLookup(@NotNull MethodHandles.Lookup lookup) {
    BindingOptionsImpl options = this.options.withMemberLookup(lookup);
    return new ConcreteBindingBuilderImpl<>(this.bindingKey, this.scopeRegistry, this.scope, options);
  }

  @Override
  public @NotNull UninstalledBinding<T> toInstance(@NotNull T instance) {
    ScopeApplier scope = this.resolveScopeApplier(instance.getClass().getAnnotations());
    ProviderFactory<T> providerFactory = InstanceProviderFactory.ofInstance(instance);
    return new UninstalledBindingImpl<>(this.bindingKey, scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toFactoryMethod(@NotNull Method factoryMethod) {
    Type targetType = this.bindingKey.type();
    Type returnType = GenericTypeReflector.box(factoryMethod.getGenericReturnType());
    if (!Modifier.isStatic(factoryMethod.getModifiers()) || !GenericTypeReflector.isSuperType(targetType, returnType)) {
      throw new IllegalArgumentException("Factory method must be static and return a subtype of " + targetType);
    }

    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = FactoryMethodProviderFactory.fromMethod(factoryMethod, lookup);

    ScopeApplier scope = this.resolveScopeApplier(factoryMethod);
    return new UninstalledBindingImpl<>(this.bindingKey, scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toProvider(@NotNull Provider<? extends T> provider) {
    ProviderFactory<T> providerFactory = DelegatingProviderFactory.toProvider(provider);
    return new UninstalledBindingImpl<>(this.bindingKey, this.scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toProvider(@NotNull Class<? extends Provider<? extends T>> providerType) {
    if (Modifier.isAbstract(providerType.getModifiers())) {
      throw new IllegalArgumentException("Cannot construct abstract provider type " + providerType.getName());
    }

    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = ConstructingDelegatingProviderFactory.fromProviderClass(providerType, lookup);
    return new UninstalledBindingImpl<>(this.bindingKey, this.scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toConstructor(@NotNull Constructor<? extends T> constructor) {
    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = ConstructorProviderFactory.fromConstructor(constructor, lookup);

    ScopeApplier scope = this.resolveScopeApplier(constructor.getDeclaringClass().getAnnotations());
    return new UninstalledBindingImpl<>(this.bindingKey, scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toConstructingClass(@NotNull Class<? extends T> implementationType) {
    if (Modifier.isAbstract(implementationType.getModifiers())) {
      throw new IllegalArgumentException("Cannot construct abstract type " + implementationType.getName());
    }

    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = ConstructorProviderFactory.fromClass(implementationType, lookup);

    ScopeApplier scope = this.resolveScopeApplier(implementationType.getAnnotations());
    return new UninstalledBindingImpl<>(this.bindingKey, scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toConstructingSelf() {
    //noinspection unchecked
    Class<? extends T> rawTargetClass = (Class<? extends T>) GenericTypeReflector.erase(this.bindingKey.type());
    return this.toConstructingClass(rawTargetClass);
  }

  private @NotNull MethodHandles.Lookup resolveMemberLookup() {
    return this.options.memberLookup().orElse(LOOKUP);
  }

  private @Nullable ScopeApplier resolveScopeApplier(@NotNull Method method) {
    // prefer overridden scope annotation
    if (this.scope != null) {
      return this.scope;
    }

    // first check directly on the method, then fall back to the return type
    // unless the returned type is abstract (includes interfaces)
    ScopeApplier methodScope = this.resolveScopeApplier(method.getAnnotations());
    if (methodScope == null) {
      Class<?> returnType = method.getReturnType();
      if (!Modifier.isAbstract(returnType.getModifiers())) {
        methodScope = this.resolveScopeApplier(returnType.getAnnotations());
      }
    }

    return methodScope;
  }

  private @Nullable ScopeApplier resolveScopeApplier(@NotNull Annotation[] targetAnnotations) {
    // prefer overridden scope annotation
    if (this.scope != null) {
      return this.scope;
    }

    // try to find a scope annotation and resolve the associated applier
    Class<? extends Annotation> boundScope = InjectAnnotationUtil.findScopeAnnotation(targetAnnotations);
    if (boundScope != null) {
      return this.scopeRegistry
        .get(boundScope)
        .orElseThrow(() -> new IllegalStateException("Scope @" + boundScope.getName() + " has no registered applier"));
    }

    return null;
  }
}
