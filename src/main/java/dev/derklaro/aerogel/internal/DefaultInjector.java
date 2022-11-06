/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 Pasqual K. and contributors
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

package dev.derklaro.aerogel.internal;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.BindingConstructor;
import dev.derklaro.aerogel.BindingHolder;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.MemberInjector;
import dev.derklaro.aerogel.internal.binding.ConstructingBindingHolder;
import dev.derklaro.aerogel.internal.binding.ImmediateBindingHolder;
import dev.derklaro.aerogel.internal.member.DefaultMemberInjector;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of an injector. See {@link Injector#newInjector()}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class DefaultInjector implements Injector {

  /**
   * Represents the injector element without any annotations or a name which makes it special
   */
  private static final Element INJECTOR_ELEMENT = Element.forType(Injector.class);

  private final Injector parent;
  private final Map<Element, BindingHolder> bindings;
  private final Map<Class<?>, MemberInjector> cachedMemberInjectors;
  // represents the binding for this injector
  private final ImmediateBindingHolder injectorBinding;

  /**
   * Constructs a new injector using the given {@code parent} injector as its parent.
   *
   * @param parent the parent injector of the new injector, might be null if the injector has no parent.
   */
  public DefaultInjector(@Nullable Injector parent) {
    this.parent = parent;
    this.bindings = new ConcurrentHashMap<>();
    this.cachedMemberInjectors = new ConcurrentHashMap<>();
    this.injectorBinding = new ImmediateBindingHolder(INJECTOR_ELEMENT, this, this, INJECTOR_ELEMENT);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Injector parent() {
    return this.parent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector newChildInjector() {
    return new DefaultInjector(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T instance(@NotNull Class<T> type) {
    return this.instance(Element.forType(type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> T instance(@NotNull Type type) {
    return this.instance(Element.forType(type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T instance(@NotNull Element element) {
    return (T) this.binding(element).get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull BindingConstructor constructor) {
    Objects.requireNonNull(constructor, "constructor");
    // construct the binding
    BindingHolder holder = Objects.requireNonNull(constructor.construct(this), "holder");
    // registers the binding
    for (Element type : holder.types()) {
      this.bindings.putIfAbsent(type, holder);
    }
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Injector install(@NotNull Iterable<BindingConstructor> constructors) {
    // install all constructors
    for (BindingConstructor constructor : constructors) {
      this.install(constructor);
    }
    // for chaining
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjector memberInjector(@NotNull Class<?> memberClazz) {
    Objects.requireNonNull(memberClazz, "memberClazz");
    // try to find the member injector in one of the parent injectors
    Injector injector = this;
    do {
      // check if the injector has a cached member injector for the class - in this case use that one
      MemberInjector memberInjector = injector.fastMemberInjector(memberClazz);
      if (memberInjector != null) {
        return memberInjector;
      }
    } while ((injector = injector.parent()) != null);
    // construct a new member injector as neither this nor the parent injectors have a cached member injector instance
    MemberInjector memberInjector = new DefaultMemberInjector(this, memberClazz);
    this.cachedMemberInjectors.putIfAbsent(memberClazz, memberInjector); // putIfAbsent for concurrency reasons
    // return the newly created injector
    return memberInjector;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable MemberInjector fastMemberInjector(@NotNull Class<?> memberHolderClass) {
    Objects.requireNonNull(memberHolderClass, "memberHolderClass");
    // read the cached instance from the class.
    return this.cachedMemberInjectors.get(memberHolderClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingHolder binding(@NotNull Type target) {
    return this.binding(Element.forType(target));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull BindingHolder binding(@NotNull Element element) {
    Objects.requireNonNull(element, "element");
    // get the binding if a parent has already one
    BindingHolder holder = this.bindingOrNull(element);
    if (holder != null) {
      // cache locally to prevent further deep lookups
      this.bindings.putIfAbsent(element, holder);
      // return the looked-up holder
      return holder;
    }
    // check if the element has special parameters - in this case we will strictly not mock the element
    if (element.requiredName() != null || !element.requiredAnnotations().isEmpty()) {
      throw AerogelException.forMessageWithoutStack(
        "Element " + element + " has special properties, unable to make a runtime binding for it");
    }
    // check if the element is of the type Injector - return us for it
    if (INJECTOR_ELEMENT.equals(element)) {
      return this.injectorBinding;
    }
    // create a constructing holder for the class - we can not support other binding types
    holder = ConstructingBindingHolder.create(this, element);
    // cache the holder
    this.bindings.put(element, holder);
    // return the constructed binding
    return holder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder bindingOrNull(@NotNull Element element) {
    Objects.requireNonNull(element, "element");
    // check if we have a cached bindingHolder
    BindingHolder bindingHolder = this.bindings.get(element);
    // check if we need a parent injector lookup - skip the parent lookup if the element is the current injector element
    // in this case we always want to inject this injector, not the parent
    if (bindingHolder == null && this.parent != null && !INJECTOR_ELEMENT.equals(element)) {
      // check if one of the parents has a cached bindingHolder
      Injector injector = this.parent;
      do {
        // get a cached bindingHolder from the parent injector
        bindingHolder = injector.fastBinding(element);
        if (bindingHolder != null) {
          // found one!
          return bindingHolder;
        }
        // check the parent injector of the injector
        injector = injector.parent();
      } while (injector != null);
    }
    // no bindingHolder cached
    return bindingHolder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable BindingHolder fastBinding(@NotNull Element element) {
    Objects.requireNonNull(element, "element");
    // read from the store
    return this.bindings.get(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnmodifiableView @NotNull Collection<BindingHolder> bindings() {
    return Collections.unmodifiableCollection(this.bindings.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @UnmodifiableView @NotNull Collection<BindingHolder> allBindings() {
    // the resulting collection
    Collection<BindingHolder> bindings = new HashSet<>(this.bindings.values());
    // check if this injector has a parent
    if (this.parent != null) {
      // walk down the parent chain - add all of their bindings as well
      Injector target = this.parent;
      do {
        // add all bindings to the result
        bindings.addAll(target.bindings());
      } while ((target = target.parent()) != null);
    }
    // the return value should be unmodifiable
    return Collections.unmodifiableCollection(bindings);
  }
}
