/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.binding.BindingHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A basic collection for bindings of an injector that uses an array list as the backing collection and a rw lock to
 * allow concurrent access to it. This collection is designed to handle a lot of reads, but only a small amount of
 * writes, which should be case during runtime (but not during initial startup).
 * <p>
 * Implementation note: while it could be a consideration to build the injector binding tree in this implementation by
 * passing the parent binding collection into this and even sharing the same rw lock, but the throughput of single
 * operations, like a binding register will be faster if each collection has their own locking instead of locking the
 * complete binding collection tree.
 *
 * @author Pasqual K.
 * @since 2.2
 */
@API(status = API.Status.INTERNAL, since = "2.2", consumers = "dev.derklaro.aerogel.internal")
final class InjectorBindingCollection {

  private static final BindingHolder[] EMPTY_BINDING_HOLDERS = new BindingHolder[0];

  private final ReadWriteLock lock;
  private final List<BindingHolder> bindings;

  /**
   * Constructs a new binding collection with a new rw lock and a binding collection with an initial size of 16.
   */
  public InjectorBindingCollection() {
    this.lock = new ReentrantReadWriteLock();
    this.bindings = new ArrayList<>(16);
  }

  /**
   * Utility method that allows the recursive copy of all bindings in an injector tree, starting from the given parent
   * injector. This method optimizes the collect operation for the default injector implementations by preventing an
   * array copy during insertion into the given target collection.
   *
   * @param parent the parent injector from which the binding collection should start.
   * @param target the target collection to which the bindings should be added.
   */
  public static void copyBindingsFromParents(@Nullable Injector parent, @NotNull Collection<BindingHolder> target) {
    if (parent != null) {
      Injector injector = parent;
      do {
        if (injector instanceof DefaultInjector) {
          // fast path - this does not require us to copy the bindings
          DefaultInjector di = (DefaultInjector) injector;
          di.bindings.trustedBindingsCopyInto(target);
        } else if (injector instanceof DefaultSpecifiedInjector) {
          // fast path - this does not require us to copy the bindings
          DefaultSpecifiedInjector dsi = (DefaultSpecifiedInjector) injector;
          dsi.specificBindings.trustedBindingsCopyInto(target);
        } else {
          // slow path, possibly requires copy of the underlying collection
          target.addAll(injector.bindings());
        }
      } while ((injector = injector.parent()) != null);
    }
  }

  /**
   * Registers a new binding into this collection. There are no checks made if the binding is already registered. This
   * method causes a write lock operation.
   *
   * @param bindingHolder the binding to register.
   */
  public void registerBinding(@NotNull BindingHolder bindingHolder) {
    Lock writeLock = this.lock.writeLock();

    try {
      writeLock.lock();
      this.bindings.add(bindingHolder);
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Executes the given action on the backing binding collection in a write lock. Trusted method for default injector
   * implementations, do not use.
   *
   * @param action the action to execute.
   */
  public void withTrustedWriteAccess(@NotNull Consumer<List<BindingHolder>> action) {
    Lock writeLock = this.lock.writeLock();

    try {
      writeLock.lock();
      action.accept(this.bindings);
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Removes all elements that are matching the given filter from the backing collection. This method causes a write
   * lock operation.
   *
   * @param filter the filter for the bindings to remove.
   * @return true if at least one element was removed, false otherwise.
   */
  public boolean removeMatchingBindings(@NotNull Predicate<BindingHolder> filter) {
    Lock writeLock = this.lock.writeLock();

    try {
      writeLock.lock();
      return this.bindings.removeIf(filter);
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Finds a binding for the given element in this collection.This method causes a read lock operation.
   *
   * @param element the element to find the binding for.
   * @return the binding for the given element, null if no such binding is registered.
   */
  public @Nullable BindingHolder findBinding(@NotNull Element element) {
    Lock readLock = this.lock.readLock();

    try {
      readLock.lock();
      for (BindingHolder binding : this.bindings) {
        if (binding.elementMatcher().test(element)) {
          return binding;
        }
      }
      return null;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Get an unmodifiable copy of the bindings that are registered in this collection. This method causes a read lock
   * operation.
   *
   * @return an unmodifiable copy of the backing binding collection.
   */
  @Unmodifiable
  public @NotNull Collection<BindingHolder> allBindings() {
    Lock readLock = this.lock.readLock();

    try {
      readLock.lock();

      BindingHolder[] bindings = this.bindings.toArray(EMPTY_BINDING_HOLDERS);
      return Arrays.asList(bindings);
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Adds all registered bindings in the backing collection into the given target collection by calling the
   * {@link Collection#addAll(Collection)} method. This method causes a read lock operation. Trusted method for default
   * injector implementations, do not use.
   * <p>
   * Implementation note: this is a trusted method due to the fact that the backing collection is directly passed to the
   * given collection. This means that someone can create their own collection implementation and execute unsafe
   * operations on the backing collection.
   *
   * @param target the target collection to copy the registered bindings to.
   */
  public void trustedBindingsCopyInto(@NotNull Collection<BindingHolder> target) {
    Lock readLock = this.lock.readLock();

    try {
      readLock.lock();
      target.addAll(this.bindings);
    } finally {
      readLock.unlock();
    }
  }
}
