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
import jakarta.inject.Named;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
    @NotNull Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry
  ) {
    this(bindingKey, scopeRegistry, null, BindingOptionsImpl.EMPTY);
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
    // TODO: consider moving AnnotationDesc construction here and validating of annotation type to be qualifier
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
    // TODO: validate scope annotation to be a scope annotation
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
    ScopeApplier scope = this.resolveActualScopeApplier(instance.getClass());
    ProviderFactory<T> providerFactory = InstanceProviderFactory.ofInstance(instance);
    return new UninstalledBindingImpl<>(this.bindingKey, scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toFactoryMethod(@NotNull Method factoryMethod) {
    // TODO: a bit of validation...
    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = FactoryMethodProviderFactory.fromMethod(factoryMethod, lookup);

    ScopeApplier scope = this.resolveActualScopeApplier(factoryMethod.getReturnType());
    return new UninstalledBindingImpl<>(this.bindingKey, scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toProvider(@NotNull Provider<? extends T> provider) {
    ProviderFactory<T> providerFactory = DelegatingProviderFactory.toProvider(provider);
    return new UninstalledBindingImpl<>(this.bindingKey, this.scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toProvider(@NotNull Class<? extends Provider<? extends T>> providerType) {
    // TODO: should the scope apply to everything or just to the provider? how to handle scopes on the provider class?
    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = ConstructingDelegatingProviderFactory.fromProviderClass(providerType, lookup);
    return new UninstalledBindingImpl<>(this.bindingKey, this.scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toConstructor(@NotNull Constructor<? extends T> constructor) {
    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = ConstructorProviderFactory.fromConstructor(constructor, lookup);

    ScopeApplier scope = this.resolveActualScopeApplier(constructor.getDeclaringClass());
    return new UninstalledBindingImpl<>(this.bindingKey, scope, this.options, providerFactory);
  }

  @Override
  public @NotNull UninstalledBinding<T> toConstructingClass(@NotNull Class<? extends T> implementationType) {
    // TODO: a bit of validation...
    MethodHandles.Lookup lookup = this.resolveMemberLookup();
    ProviderFactory<T> providerFactory = ConstructorProviderFactory.fromClass(implementationType, lookup);

    ScopeApplier scope = this.resolveActualScopeApplier(implementationType);
    return new UninstalledBindingImpl<>(this.bindingKey, scope, this.options, providerFactory);
  }

  private @NotNull MethodHandles.Lookup resolveMemberLookup() {
    return this.options.memberLookup().orElse(LOOKUP);
  }

  private @Nullable ScopeApplier resolveActualScopeApplier(@NotNull Class<?> target) {
    // prefer overridden scope annotation
    if (this.scope != null) {
      return this.scope;
    }

    // scope annotations are not allowed on abstract types
    if (target.isInterface() || Modifier.isAbstract(target.getModifiers())) {
      return null;
    }

    // try to find a scope annotation and resolve the associated applier
    Class<? extends Annotation> boundScope = InjectAnnotationUtil.findScopeAnnotation(target.getAnnotations());
    if (boundScope != null) {
      return this.scopeRegistry.get(boundScope)
        .orElseThrow(() -> new IllegalStateException("Scope @" + boundScope.getName() + " has no registered applier"));
    }

    return null;
  }
}
