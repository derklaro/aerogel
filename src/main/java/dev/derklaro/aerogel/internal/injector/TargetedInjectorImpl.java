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
import dev.derklaro.aerogel.internal.util.MapUtil;
import dev.derklaro.aerogel.registry.Registry;
import io.leangen.geantyref.TypeToken;
import jakarta.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class TargetedInjectorImpl implements Injector {

  final Injector parent;
  final InjectorOptions injectorOptions;
  final Registry.WithKeyMapping<BindingKey<?>, InstalledBinding<?>> bindingRegistry;

  private final Injector nonTargetedInjector;
  private final JitBindingFactory jitBindingFactory;
  private final ContextualBindingResolver contextualBindingResolver;

  private final Map<Class<?>, MemberInjector<?>> memberInjectorCache = MapUtil.newConcurrentMap();

  TargetedInjectorImpl(
    @NotNull Injector parent,
    @NotNull Injector nonTargetedInjector,
    @NotNull InjectorOptions injectorOptions,
    @NotNull Registry.WithKeyMapping<BindingKey<?>, InstalledBinding<?>> bindingRegistry
  ) {
    this.parent = parent;
    this.injectorOptions = injectorOptions;
    this.bindingRegistry = bindingRegistry.freeze();

    this.nonTargetedInjector = nonTargetedInjector;
    this.jitBindingFactory = new JitBindingFactory(this);
    this.contextualBindingResolver = new ContextualBindingResolver(this);
  }

  @Override
  public @NotNull Optional<Injector> parentInjector() {
    return Optional.of(this.parent);
  }

  @Override
  public @NotNull Injector createChildInjector() {
    return new InjectorImpl(this);
  }

  @Override
  public @NotNull TargetedInjectorBuilder createTargetedInjectorBuilder() {
    return new TargetedInjectorBuilderImpl(this, this.nonTargetedInjector, this.injectorOptions);
  }

  @Override
  public @NotNull RootBindingBuilder createBindingBuilder() {
    BindingOptionsImpl standardBindingOptions = new BindingOptionsImpl(this.injectorOptions.memberLookup());
    return new RootBindingBuilderImpl(standardBindingOptions, this.parent.scopeRegistry());
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
    BindingKey<T> key = BindingKey.of(type);
    return this.instance(key);
  }

  @Override
  public @Nullable <T> T instance(@NotNull Type type) {
    BindingKey<T> key = BindingKey.of(type);
    return this.instance(key);
  }

  @Override
  public @Nullable <T> T instance(@NotNull TypeToken<T> typeToken) {
    BindingKey<T> key = BindingKey.of(typeToken);
    return this.instance(key);
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
    // try to get an overridden binding from the local binding registry
    InstalledBinding<?> binding = this.bindingRegistry.getDirect(key).orElse(null);
    if (binding != null) {
      return (InstalledBinding<T>) binding;
    }

    // try to get an existing binding from the parent registry
    InstalledBinding<T> existingParentBinding = this.parent.existingBinding(key).orElse(null);
    if (existingParentBinding != null) {
      return existingParentBinding;
    }

    // check if creating of jit binding for key was explicitly disabled
    if (!this.injectorOptions.shouldConstructJitBinding(key)) {
      throw new IllegalStateException("Creating of jit binding for key " + key + " is explicitly disabled");
    }

    // construct a new jit binding for the type and register it to the non-targeted injector in the hierarchy
    InstalledBinding<?> jitBinding = this.jitBindingFactory.createJitBinding(key);
    this.nonTargetedInjector.bindingRegistry().register(key, jitBinding);
    return (InstalledBinding<T>) jitBinding;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull <T> Optional<InstalledBinding<T>> existingBinding(@NotNull BindingKey<T> key) {
    // try to get an overridden binding from the local binding registry
    InstalledBinding<?> directBinding = this.bindingRegistry.getDirect(key).orElse(null);
    if (directBinding != null) {
      return Optional.of((InstalledBinding<T>) directBinding);
    }

    // try to get an existing binding from the parent injector
    return this.parent.existingBinding(key);
  }

  @Override
  public @NotNull Injector installBinding(@NotNull DynamicBinding binding) {
    this.parent.installBinding(binding);
    return this;
  }

  @Override
  public @NotNull <T> Injector installBinding(@NotNull UninstalledBinding<T> binding) {
    this.parent.installBinding(binding);
    return this;
  }

  @Override
  public @NotNull Registry.WithKeyMapping<BindingKey<?>, InstalledBinding<?>> bindingRegistry() {
    return this.bindingRegistry;
  }

  @Override
  public @NotNull Registry.WithoutKeyMapping<BindingKey<?>, DynamicBinding> dynamicBindingRegistry() {
    return this.parent.dynamicBindingRegistry();
  }

  @Override
  public @NotNull Registry.WithKeyMapping<Class<? extends Annotation>, ScopeApplier> scopeRegistry() {
    return this.parent.scopeRegistry();
  }
}
