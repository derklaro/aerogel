/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
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

package aerogel.internal;

import aerogel.BindingConstructor;
import aerogel.BindingHolder;
import aerogel.Element;
import aerogel.Injector;
import aerogel.MemberInjector;
import aerogel.internal.binding.ConstructingBindingHolder;
import aerogel.internal.member.DefaultMemberInjector;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DefaultInjector implements Injector {

  private final Injector parent;
  private final Map<Element, BindingHolder> bindings;
  private final Map<Class<?>, MemberInjector> cachedMemberInjectors;

  public DefaultInjector(@Nullable Injector parent) {
    this.parent = parent;
    this.bindings = new ConcurrentHashMap<>();
    this.cachedMemberInjectors = new ConcurrentHashMap<>();
  }

  @Override
  public @Nullable Injector parent() {
    return this.parent;
  }

  @Override
  public @NotNull Injector newChildInjector() {
    return new DefaultInjector(this);
  }

  @Override
  public <T> T instance(@NotNull Class<T> type) {
    return this.instance(Element.get(type));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T instance(@NotNull Element element) {
    return (T) this.binding(element).get();
  }

  @Override
  public @NotNull Injector install(@NotNull BindingConstructor constructor) {
    Objects.requireNonNull(constructor, "constructor");
    // construct the binding
    BindingHolder holder = Objects.requireNonNull(constructor.construct(this), "holder");
    // registers the binding
    this.bindings.putIfAbsent(holder.type(), holder);
    // for chaining
    return this;
  }

  @Override
  public @NotNull Injector install(@NotNull Iterable<BindingConstructor> constructors) {
    // install all constructors
    for (BindingConstructor constructor : constructors) {
      this.install(constructor);
    }
    // for chaining
    return this;
  }

  @Override
  public @NotNull MemberInjector memberInjector(@NotNull Class<?> memberClazz) {
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

  @Override
  public @Nullable MemberInjector fastMemberInjector(@NotNull Class<?> memberHolderClass) {
    return this.cachedMemberInjectors.get(memberHolderClass);
  }

  @Override
  public @NotNull BindingHolder binding(@NotNull Type target) {
    return this.binding(Element.get(target));
  }

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
    // create a constructing holder for the class - we can not support other binding types
    holder = ConstructingBindingHolder.create(this, element);
    // cache the holder
    this.bindings.put(element, holder);
    // return the constructed binding
    return holder;
  }

  @Override
  public @Nullable BindingHolder bindingOrNull(@NotNull Element element) {
    // check if we have a cached bindingHolder
    BindingHolder bindingHolder = this.bindings.get(element);
    if (bindingHolder == null && this.parent != null) {
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

  @Override
  public @Nullable BindingHolder fastBinding(@NotNull Element element) {
    return this.bindings.get(element);
  }
}
