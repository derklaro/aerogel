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
import dev.derklaro.aerogel.binding.ProviderWithContext;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.AdvancedBindingBuilder;
import dev.derklaro.aerogel.binding.builder.BindingAnnotationBuilder;
import dev.derklaro.aerogel.binding.builder.KeyableBindingBuilder;
import dev.derklaro.aerogel.binding.builder.ScopeableBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.annotation.InjectAnnotationUtil;
import dev.derklaro.aerogel.internal.binding.BindingOptionsImpl;
import dev.derklaro.aerogel.internal.binding.UninstalledBindingImpl;
import dev.derklaro.aerogel.internal.binding.annotation.BindingAnnotationBuilderImpl;
import dev.derklaro.aerogel.internal.provider.CascadingProviderFactory;
import dev.derklaro.aerogel.internal.provider.ConstructingDelegatingProviderFactory;
import dev.derklaro.aerogel.internal.provider.ConstructorProviderFactory;
import dev.derklaro.aerogel.internal.provider.DelegatingContextualProviderFactory;
import dev.derklaro.aerogel.internal.provider.DelegatingProviderFactory;
import dev.derklaro.aerogel.internal.provider.FactoryMethodProviderFactory;
import dev.derklaro.aerogel.internal.provider.InstanceProviderFactory;
import dev.derklaro.aerogel.internal.provider.ProviderFactory;
import dev.derklaro.aerogel.internal.scope.SingletonScopeApplier;
import dev.derklaro.aerogel.internal.scope.UnscopedScopeApplier;
import dev.derklaro.aerogel.registry.Registry;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ConcreteBindingBuilderImpl<T> implements KeyableBindingBuilder<T> {

  private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

  // ==== must be set
  private final List<BindingKey<? extends T>> bindingKeys;
  private final Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry;

  // ==== set lazily when required
  private final ScopeApplier scope;
  private final BindingOptionsImpl options;

  public ConcreteBindingBuilderImpl(
    @NotNull List<BindingKey<? extends T>> bindingKeys,
    @NotNull BindingOptionsImpl bindingOptions,
    @NotNull Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry
  ) {
    this(new ArrayList<>(bindingKeys), scopeRegistry, null, bindingOptions);
  }

  private ConcreteBindingBuilderImpl(
    @NotNull List<BindingKey<? extends T>> bindingKeys,
    @NotNull Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry,
    @Nullable ScopeApplier scope,
    @NotNull BindingOptionsImpl options
  ) {
    this.bindingKeys = bindingKeys;
    this.scopeRegistry = scopeRegistry;
    this.scope = scope;
    this.options = options;
  }

  @Override
  public @NotNull KeyableBindingBuilder<T> andBind(@NotNull Type type) {
    BindingKey<? extends T> key = BindingKey.of(type);
    return this.andBind(key);
  }

  @Override
  public @NotNull KeyableBindingBuilder<T> andBind(@NotNull Class<? extends T> type) {
    BindingKey<? extends T> key = BindingKey.of(type);
    return this.andBind(key);
  }

  @Override
  public @NotNull KeyableBindingBuilder<T> andBind(@NotNull TypeToken<? extends T> typeToken) {
    BindingKey<? extends T> key = BindingKey.of(typeToken);
    return this.andBind(key);
  }

  @Override
  public @NotNull KeyableBindingBuilder<T> andBind(@NotNull BindingKey<? extends T> bindingKey) {
    if (this.bindingKeys.contains(bindingKey)) {
      throw new IllegalArgumentException("Binding key " + bindingKey + " already bound in builder");
    }

    // we can keep the list mutable during the binding process, but need to make
    // the list immutable when the binding process finishes
    List<BindingKey<? extends T>> targetKeys = new ArrayList<>(this.bindingKeys);
    targetKeys.add(bindingKey);
    return new ConcreteBindingBuilderImpl<>(targetKeys, this.scopeRegistry, this.scope, this.options);
  }

  @Override
  public @NotNull ScopeableBindingBuilder<T> qualifiedWithName(@NotNull String name) {
    return this.buildQualifier(Named.class).property(Named::value).returns(name).require();
  }

  @Override
  public @NotNull ScopeableBindingBuilder<T> qualifiedWith(@NotNull Annotation qualifierAnnotation) {
    List<BindingKey<? extends T>> bindingKeys = this.bindingKeys.stream()
      .map(key -> key.withQualifier(qualifierAnnotation))
      .collect(Collectors.toList());
    return new ConcreteBindingBuilderImpl<>(bindingKeys, this.scopeRegistry, this.scope, this.options);
  }

  @Override
  public @NotNull ScopeableBindingBuilder<T> qualifiedWith(
    @NotNull Class<? extends Annotation> qualifierAnnotationType
  ) {
    List<BindingKey<? extends T>> bindingKeys = this.bindingKeys.stream()
      .map(key -> key.withQualifier(qualifierAnnotationType))
      .collect(Collectors.toList());
    return new ConcreteBindingBuilderImpl<>(bindingKeys, this.scopeRegistry, this.scope, this.options);
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
      this.bindingKeys,
      this.scopeRegistry,
      UnscopedScopeApplier.INSTANCE,
      this.options);
  }

  @Override
  public @NotNull AdvancedBindingBuilder<T> scopedWithSingleton() {
    return new ConcreteBindingBuilderImpl<>(
      this.bindingKeys,
      this.scopeRegistry,
      SingletonScopeApplier.INSTANCE,
      this.options);
  }

  @Override
  public @NotNull AdvancedBindingBuilder<T> scopedWith(@NotNull ScopeApplier scopeApplier) {
    return new ConcreteBindingBuilderImpl<>(this.bindingKeys, this.scopeRegistry, scopeApplier, this.options);
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
    return new ConcreteBindingBuilderImpl<>(this.bindingKeys, this.scopeRegistry, this.scope, options);
  }

  @Override
  public @NotNull UninstalledBinding<T> toInstance(@NotNull T instance) {
    this.addBindingTarget(instance.getClass()); // bind the implementation type as well
    ScopeApplier scope = this.resolveScopeApplier(instance.getClass().getAnnotations());
    ProviderFactory<T> providerFactory = InstanceProviderFactory.ofInstance(instance);
    return this.createFinalBinding(scope, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toFactoryMethod(@NotNull Method factoryMethod) {
    Type targetType = this.bindingKeys.get(0).type(); // first key is the main key given while starting the build
    Type returnType = GenericTypeReflector.box(factoryMethod.getGenericReturnType());
    if (!Modifier.isStatic(factoryMethod.getModifiers()) || !GenericTypeReflector.isSuperType(targetType, returnType)) {
      throw new IllegalArgumentException("Factory method must be static and return a subtype of " + targetType);
    }

    this.addBindingTarget(factoryMethod.getReturnType()); // bind the implementation type as well
    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = FactoryMethodProviderFactory.fromMethod(factoryMethod, lookup);

    ScopeApplier scope = this.resolveScopeApplier(factoryMethod);
    return this.createFinalBinding(scope, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toProvider(@NotNull Provider<? extends T> provider) {
    ProviderFactory<T> providerFactory = DelegatingProviderFactory.toProvider(provider);
    return this.createFinalBinding(this.scope, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toProvider(@NotNull Class<? extends Provider<? extends T>> providerType) {
    if (Modifier.isAbstract(providerType.getModifiers())) {
      throw new IllegalArgumentException("Cannot construct abstract provider type " + providerType.getName());
    }

    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = ConstructingDelegatingProviderFactory.fromProviderClass(providerType, lookup);
    return this.createFinalBinding(this.scope, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toProvider(@NotNull ProviderWithContext<? extends T> provider) {
    ProviderFactory<T> providerFactory = DelegatingContextualProviderFactory.toProvider(provider);
    return this.createFinalBinding(this.scope, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toConstructor(@NotNull Constructor<? extends T> constructor) {
    this.addBindingTarget(constructor.getDeclaringClass()); // bind the implementation type as well
    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = ConstructorProviderFactory.fromConstructor(constructor, lookup);

    ScopeApplier scope = this.resolveScopeApplier(constructor.getDeclaringClass().getAnnotations());
    return this.createFinalBinding(scope, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toConstructingClass(@NotNull Class<? extends T> implementationType) {
    if (Modifier.isAbstract(implementationType.getModifiers())) {
      throw new IllegalArgumentException("Cannot construct abstract type " + implementationType.getName());
    }

    this.addBindingTarget(implementationType); // bind the implementation type as well
    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = ConstructorProviderFactory.fromClass(implementationType, lookup);

    ScopeApplier scope = this.resolveScopeApplier(implementationType.getAnnotations());
    return this.createFinalBinding(scope, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toConstructingSelf() {
    BindingKey<? extends T> mainBindingKey = this.bindingKeys.get(0);
    //noinspection unchecked
    Class<? extends T> rawTargetClass = (Class<? extends T>) GenericTypeReflector.erase(mainBindingKey.type());
    return this.toConstructingClass(rawTargetClass);
  }

  @Override
  public @NotNull UninstalledBinding<T> cascadeTo(@NotNull UninstalledBinding<? extends T> other) {
    return this.cascadeTo(other.mainKey());
  }

  @Override
  public @NotNull UninstalledBinding<T> cascadeTo(@NotNull BindingKey<? extends T> other) {
    ProviderFactory<T> providerFactory = CascadingProviderFactory.cascadeTo(other);
    return this.createFinalBinding(this.scope, providerFactory);
  }

  private @NotNull UninstalledBinding<T> createFinalBinding(
    @Nullable ScopeApplier scope,
    @NotNull ProviderFactory<T> providerFactory
  ) {
    List<BindingKey<? extends T>> bindingKeys = List.copyOf(this.bindingKeys);
    return new UninstalledBindingImpl<>(bindingKeys, scope, this.options, providerFactory);
  }

  private void addBindingTarget(@NotNull Class<?> type) {
    //noinspection unchecked
    BindingKey<? extends T> targetKey = (BindingKey<? extends T>) this.bindingKeys.get(0).withType(type);
    if (!this.bindingKeys.contains(targetKey)) {
      this.bindingKeys.add(targetKey);
    }
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
