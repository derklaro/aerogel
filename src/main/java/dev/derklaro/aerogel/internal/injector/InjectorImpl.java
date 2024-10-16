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

package dev.derklaro.aerogel.internal.injector;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.MemberInjector;
import dev.derklaro.aerogel.ScopeApplier;
import dev.derklaro.aerogel.TargetedInjectorBuilder;
import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.binding.BindingOptionsImpl;
import dev.derklaro.aerogel.internal.binding.builder.RootBindingBuilderImpl;
import dev.derklaro.aerogel.internal.context.ContextualBindingResolver;
import dev.derklaro.aerogel.internal.member.DefaultMemberInjector;
import dev.derklaro.aerogel.internal.scope.SingletonScopeApplier;
import dev.derklaro.aerogel.internal.util.MapUtil;
import dev.derklaro.aerogel.registry.Registry;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InjectorImpl implements Injector {

  private final Injector parent;
  private final InjectorOptions injectorOptions;
  private final JitBindingFactory jitBindingFactory;
  private final ContextualBindingResolver contextualBindingResolver;

  private final Map<Class<?>, MemberInjector<?>> memberInjectorCache = MapUtil.newConcurrentMap();

  private final Registry.WithKeyMapping<BindingKey<?>, InstalledBinding<?>> bindingRegistry;
  private final Registry.WithoutKeyMapping<BindingKey<?>, DynamicBinding> dynamicBindingRegistry;
  private final Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry;

  public InjectorImpl() {
    this(InjectorOptions.DEFAULT);
  }

  private InjectorImpl(@NotNull InjectorImpl parent) {
    this(
      parent,
      parent.injectorOptions,
      parent.bindingRegistry.createChildRegistry(),
      parent.scopeRegistry.createChildRegistry(),
      parent.dynamicBindingRegistry.createChildRegistry());
  }

  InjectorImpl(@NotNull InjectorOptions options) {
    this(
      null,
      options,
      Registry.createRegistryWithKeys(),
      Registry.createRegistryWithKeys(),
      Registry.createRegistryWithoutKeys((key, binding) -> binding.supports(key)));
  }

  /* trusted */
  InjectorImpl(@NotNull TargetedInjectorImpl targetedInjector) {
    this(
      targetedInjector,
      targetedInjector.injectorOptions,
      targetedInjector.bindingRegistry.createChildRegistry(),
      targetedInjector.parent.scopeRegistry().createChildRegistry(),
      targetedInjector.parent.dynamicBindingRegistry().createChildRegistry());
  }

  private InjectorImpl(
    @Nullable Injector parent,
    @NotNull InjectorOptions injectorOptions,
    @NotNull Registry.WithKeyMapping<BindingKey<?>, InstalledBinding<?>> bindingRegistry,
    @NotNull Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry,
    @NotNull Registry.WithoutKeyMapping<BindingKey<?>, DynamicBinding> dynamicBindingRegistry
  ) {
    this.parent = parent;
    this.injectorOptions = injectorOptions;
    this.jitBindingFactory = new JitBindingFactory(this);
    this.contextualBindingResolver = new ContextualBindingResolver(this);

    this.scopeRegistry = scopeRegistry;
    this.bindingRegistry = bindingRegistry;
    this.dynamicBindingRegistry = dynamicBindingRegistry;

    // register the singleton scope into all injectors to reduce access time in the injection tree
    this.scopeRegistry.register(Singleton.class, SingletonScopeApplier.INSTANCE);
  }

  @Override
  public @NotNull Optional<Injector> parentInjector() {
    return Optional.ofNullable(this.parent);
  }

  @Override
  public @NotNull Injector createChildInjector() {
    return new InjectorImpl(this);
  }

  @Override
  public @NotNull TargetedInjectorBuilder createTargetedInjectorBuilder() {
    return new TargetedInjectorBuilderImpl(this, this, this.injectorOptions);
  }

  @Override
  public @NotNull RootBindingBuilder createBindingBuilder() {
    BindingOptionsImpl standardBindingOptions = new BindingOptionsImpl(this.injectorOptions.memberLookup());
    return new RootBindingBuilderImpl(standardBindingOptions, this.scopeRegistry);
  }

  @Override
  public @NotNull <T> MemberInjector<T> memberInjector(@NotNull Class<T> memberHolderClass) {
    return this.memberInjector(memberHolderClass, null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull <T> MemberInjector<T> memberInjector(
    @NotNull Class<T> memberHolderClass,
    @Nullable MethodHandles.Lookup givenLookup
  ) {
    // try to load from cache first
    MemberInjector<?> cachedMemberInjector = this.memberInjectorCache.get(memberHolderClass);
    if (cachedMemberInjector != null) {
      return (MemberInjector<T>) cachedMemberInjector;
    }

    // create a new member injector
    MethodHandles.Lookup lookup = givenLookup != null ? givenLookup : this.injectorOptions.memberLookup();
    MemberInjector<T> newMemberInjector = new DefaultMemberInjector<>(memberHolderClass, this, lookup);
    MemberInjector<?> knownInjector = this.memberInjectorCache.putIfAbsent(memberHolderClass, newMemberInjector);
    return (MemberInjector<T>) (knownInjector != null ? knownInjector : newMemberInjector);
  }

  @Override
  public @Nullable <T> T instance(@NotNull Class<T> type) {
    BindingKey<T> bindingKey = BindingKey.of(type);
    return this.instance(bindingKey);
  }

  @Override
  public @Nullable <T> T instance(@NotNull Type type) {
    BindingKey<T> bindingKey = BindingKey.of(type);
    return this.instance(bindingKey);
  }

  @Override
  public @Nullable <T> T instance(@NotNull TypeToken<T> typeToken) {
    BindingKey<T> bindingKey = BindingKey.of(typeToken);
    return this.instance(bindingKey);
  }

  @Override
  public @Nullable <T> T instance(@NotNull BindingKey<T> key) {
    InstalledBinding<T> binding = this.binding(key);
    return this.contextualBindingResolver.resolveInstance(binding);
  }

  @Override
  public @NotNull <T> Provider<T> provider(@NotNull BindingKey<T> key) {
    InstalledBinding<T> binding = this.binding(key);
    return this.contextualBindingResolver.constructProvider(binding);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull <T> InstalledBinding<T> binding(@NotNull BindingKey<T> key) {
    return this.existingBinding(key).orElseGet(() -> {
      if (this.injectorOptions.shouldConstructJitBinding(key)) {
        InstalledBinding<?> jitBinding = this.jitBindingFactory.createJitBinding(key);
        this.bindingRegistry.register(key, jitBinding);
        return (InstalledBinding<T>) jitBinding;
      } else {
        throw new IllegalStateException("Creating of jit binding for key " + key + " is explicitly disabled");
      }
    });
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull <T> Optional<InstalledBinding<T>> existingBinding(@NotNull BindingKey<T> key) {
    // try to load an existing binding that is mapped to the given key
    InstalledBinding<?> directBinding = this.bindingRegistry.get(key).orElse(null);
    if (directBinding != null) {
      return Optional.of((InstalledBinding<T>) directBinding);
    }

    // try to load a binding from the dynamic binding registry
    return this.dynamicBindingRegistry.get(key)
      .flatMap(dynamicBinding -> dynamicBinding.tryMatch(key))
      .map(uninstalledBinding -> {
        InstalledBinding<T> binding = uninstalledBinding.prepareForInstallation(this);
        this.bindingRegistry.register(key, binding);
        return binding;
      });
  }

  @Override
  public @NotNull Injector installBinding(@NotNull DynamicBinding binding) {
    this.dynamicBindingRegistry.register(binding);
    return this;
  }

  @Override
  public @NotNull <T> Injector installBinding(@NotNull UninstalledBinding<T> binding) {
    InstalledBinding<?> installedBinding = binding.prepareForInstallation(this);
    for (BindingKey<? extends T> key : binding.keys()) {
      this.bindingRegistry.register(key, installedBinding);
    }

    return this;
  }

  @Override
  public @NotNull Registry.WithKeyMapping<BindingKey<?>, InstalledBinding<?>> bindingRegistry() {
    return this.bindingRegistry;
  }

  @Override
  public @NotNull Registry.WithoutKeyMapping<BindingKey<?>, DynamicBinding> dynamicBindingRegistry() {
    return this.dynamicBindingRegistry;
  }

  @Override
  public @NotNull Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry() {
    return this.scopeRegistry;
  }

  @Override
  public void close() {
  }
}
