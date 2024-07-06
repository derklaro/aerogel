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

package dev.derklaro.aerogel.internal.binding.annotation;

import dev.derklaro.aerogel.binding.builder.BindingAnnotationBuilder;
import dev.derklaro.aerogel.binding.builder.QualifiableBindingBuilder;
import dev.derklaro.aerogel.binding.builder.ScopeableBindingBuilder;
import dev.derklaro.aerogel.internal.annotation.AnnotationDesc;
import io.leangen.geantyref.GenericTypeReflector;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BindingAnnotationBuilderImpl<A extends Annotation, T> implements BindingAnnotationBuilder<A, T> {

  private final Class<A> annotationType;
  private final QualifiableBindingBuilder<T> owner;
  private final BuilderAnnotationProxy<A> annotationProxy;

  private final AnnotationDesc targetAnnotationDescriptor;
  private final Map<String, Supplier<?>> propertyValueFactories;

  public BindingAnnotationBuilderImpl(@NotNull Class<A> annotationType, @NotNull QualifiableBindingBuilder<T> owner) {
    this.owner = owner;
    this.annotationType = annotationType;
    this.annotationProxy = BuilderAnnotationProxy.make(annotationType);

    this.propertyValueFactories = new HashMap<>();
    this.targetAnnotationDescriptor = AnnotationDesc.of(annotationType);
  }

  @Override
  public @NotNull <R> DefineReturn<A, T, R> property(@NotNull Function<A, R> accessor) {
    // call the annotation accessor to obtain the property that gets called
    A proxy = this.annotationProxy.proxyInstance();
    accessor.apply(proxy);

    // ensure a property was actually called
    String property = this.annotationProxy.getAndRemoveLastAccessedProperty();
    if (property == null) {
      throw new IllegalStateException("Accessor did not call any annotation method");
    }

    // disallow custom implementations of equals/hashCode/toString/annotationType
    // as these methods are required for internal stability
    if (property.equals("equals")
      || property.equals("hashCode")
      || property.equals("toString")
      || property.equals("annotationType")) {
      throw new IllegalArgumentException("Custom impl of equals/hashCode/toString/annotationType is not allowed");
    }

    // ensure that the method was not implemented before
    if (this.propertyValueFactories.containsKey(property)) {
      throw new IllegalStateException("Annotation method impl already set for " + property);
    }

    // get the annotation member to implement from the function call
    AnnotationDesc.Member annotationMember = this.targetAnnotationDescriptor.members().get(property);
    return new DefineReturnImpl<>(annotationMember, this);
  }

  @Override
  public @NotNull ScopeableBindingBuilder<T> require() {
    this.validateAllPropertiesAreSet();
    A proxy = BindingAnnotationProxy.makeProxy(this.annotationType, this.propertyValueFactories);
    return this.owner.qualifiedWith(proxy);
  }

  private void validateAllPropertiesAreSet() {
    Collection<AnnotationDesc.Member> allMembers = this.targetAnnotationDescriptor.members().values();
    for (AnnotationDesc.Member member : allMembers) {
      if (!this.propertyValueFactories.containsKey(member.name())) {
        Object defaultValue = member.defaultValue();
        if (defaultValue == null) {
          throw new IllegalStateException("No value for required property present: " + member.name());
        }

        // construct a default member provider for the default value
        Supplier<?> memberProvider = new AnnotationMemberValueSupplier<>(false, () -> defaultValue, member);
        this.propertyValueFactories.put(member.name(), memberProvider);
      }
    }
  }

  private static final class DefineReturnImpl<A extends Annotation, T, R> implements DefineReturn<A, T, R> {

    private final AnnotationDesc.Member currentMember;
    private final BindingAnnotationBuilderImpl<A, T> owner;

    private boolean useDefaultValueOnError = false;

    public DefineReturnImpl(
      @NotNull AnnotationDesc.Member currentMember,
      @NotNull BindingAnnotationBuilderImpl<A, T> owner
    ) {
      this.currentMember = currentMember;
      this.owner = owner;
    }

    @Override
    public @NotNull DefineReturn<A, T, R> orDefault() {
      this.useDefaultValueOnError = true;
      return this;
    }

    @Override
    public @NotNull <X extends R> BindingAnnotationBuilder<A, T> returns(@Nullable X returnValue) {
      return this.returnLazySupply(() -> returnValue);
    }

    @Override
    public @NotNull <X extends R> BindingAnnotationBuilder<A, T> returnLazyCall(@NotNull Callable<X> provider) {
      return this.returnLazySupply(() -> {
        try {
          return provider.call();
        } catch (Exception exception) {
          throw new IllegalStateException(
            "Unable to provide value for annotation member: " + this.currentMember.name());
        }
      });
    }

    @Override
    public @NotNull <X extends R> BindingAnnotationBuilder<A, T> returnLazySupply(@NotNull Supplier<X> provider) {
      AnnotationDesc.Member member = this.currentMember;
      Supplier<?> memberProvider = new AnnotationMemberValueSupplier<>(this.useDefaultValueOnError, provider, member);
      this.owner.propertyValueFactories.put(member.name(), memberProvider);
      return this.owner;
    }
  }

  private static final class AnnotationMemberValueSupplier<T> implements Supplier<T> {

    private final boolean defaultOnError;
    private final Supplier<T> valueSupplier;
    private final AnnotationDesc.Member annotationMember;

    private final Lock constructionLock = new ReentrantLock();
    private transient T constructedValue;

    private AnnotationMemberValueSupplier(
      boolean defaultOnError,
      @NotNull Supplier<T> valueSupplier,
      @NotNull AnnotationDesc.Member annotationMember
    ) {
      this.defaultOnError = defaultOnError;
      this.valueSupplier = valueSupplier;
      this.annotationMember = annotationMember;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(@NotNull Throwable throwable) throws T {
      throw (T) throwable;
    }

    @Override
    public @NotNull T get() {
      // check if the value was already constructed
      if (this.constructedValue != null) {
        return this.constructedValue;
      }

      Lock lock = this.constructionLock;
      lock.lock();
      try {
        // doubly-checked locking: check if the value was constructed by a different thread
        if (this.constructedValue != null) {
          return this.constructedValue;
        }

        // construct and set the constructed value
        T value = this.constructValue();
        this.constructedValue = value;
        return value;
      } finally {
        lock.unlock();
      }
    }

    @SuppressWarnings("unchecked")
    private @NotNull T constructValue() {
      // get the value from the wrapped supplier
      T value = this.getValueSilent();
      if (value == null && this.defaultOnError) {
        value = (T) this.annotationMember.defaultValue();
      }

      // check if the value is still not present, fail in that case
      if (value == null) {
        throw new IllegalStateException("Unable to get value for annotation property " + this.annotationMember.name());
      }

      // ensure that the return type is actually of the required type, boxing the required
      // type is needed as passing of primitive values is not possible and therefore all
      // values for the members are actually boxed and will be unboxed on method invocation
      Class<?> valueType = value.getClass();
      Class<?> requiredType = this.annotationMember.type();
      Class<?> boxedRequiredType = (Class<?>) GenericTypeReflector.box(requiredType);
      if (!boxedRequiredType.isAssignableFrom(valueType)) {
        throw new IllegalStateException("Invalid return type for annotation member " + this.annotationMember.name()
          + ". Expected " + boxedRequiredType.getName() + ", got " + valueType.getName());
      }

      return value;
    }

    private @Nullable T getValueSilent() {
      try {
        return this.valueSupplier.get();
      } catch (Throwable throwable) {
        if (!this.defaultOnError) {
          // return type of throwUnchecked is never
          throwUnchecked(throwable);
        }
      }

      return null;
    }
  }
}
