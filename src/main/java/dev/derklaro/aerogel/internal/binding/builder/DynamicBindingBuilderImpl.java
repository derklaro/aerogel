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

import dev.derklaro.aerogel.binding.DynamicBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.DynamicBindingBuilder;
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.binding.builder.ScopeableBindingBuilder;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.binding.DynamicBindingImpl;
import io.leangen.geantyref.GenericTypeReflector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

final class DynamicBindingBuilderImpl implements DynamicBindingBuilder {

  private final RootBindingBuilder rootBindingBuilder;
  private Predicate<BindingKey<?>> bindingKeyMatcher;

  public DynamicBindingBuilderImpl(@NotNull RootBindingBuilder rootBindingBuilder) {
    this.rootBindingBuilder = rootBindingBuilder;
  }

  @Override
  public @NotNull DynamicBindingBuilder annotationPresent(@NotNull Class<? extends Annotation> annotationType) {
    this.appendFilter(bindingKey -> {
      Class<?> qualifierAnnotationType = bindingKey.qualifierAnnotationType().orElse(null);
      return qualifierAnnotationType != null && qualifierAnnotationType.equals(annotationType);
    });
    return this;
  }

  @Override
  public @NotNull DynamicBindingBuilder matchAnnotation(@NotNull Predicate<Annotation> filter) {
    this.appendFilter(bindingKey -> {
      Annotation qualifierAnnotationInstance = bindingKey.qualifierAnnotation().orElse(null);
      return qualifierAnnotationInstance != null && filter.test(qualifierAnnotationInstance);
    });
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull <A extends Annotation> DynamicBindingBuilder matchAnnotation(
    @NotNull Class<A> annotationType,
    @NotNull Predicate<A> filter
  ) {
    this.appendFilter(bindingKey -> {
      Annotation qualifierAnnotationInstance = bindingKey.qualifierAnnotation().orElse(null);
      if (qualifierAnnotationInstance != null) {
        Class<? extends Annotation> type = qualifierAnnotationInstance.annotationType();
        if (type.equals(annotationType)) {
          return filter.test((A) qualifierAnnotationInstance);
        }
      }
      return false;
    });
    return this;
  }

  @Override
  public @NotNull DynamicBindingBuilder exactRawType(@NotNull Class<?> type) {
    this.appendFilter(bindingKey -> {
      Class<?> rawMatchedType = GenericTypeReflector.erase(bindingKey.type());
      return rawMatchedType.equals(type);
    });
    return this;
  }

  @Override
  public @NotNull DynamicBindingBuilder superRawType(@NotNull Class<?> type) {
    this.appendFilter(bindingKey -> {
      Class<?> rawMatchedType = GenericTypeReflector.erase(bindingKey.type());
      return type.isAssignableFrom(rawMatchedType);
    });
    return this;
  }

  @Override
  public @NotNull DynamicBindingBuilder matchRawType(@NotNull Predicate<Class<?>> filter) {
    this.appendFilter(bindingKey -> {
      Class<?> rawMatchedType = GenericTypeReflector.erase(bindingKey.type());
      return filter.test(rawMatchedType);
    });
    return this;
  }

  @Override
  public @NotNull DynamicBindingBuilder matchType(@NotNull Predicate<Type> filter) {
    this.appendFilter(bindingKey -> filter.test(bindingKey.type()));
    return this;
  }

  @Override
  public @NotNull DynamicBinding toBinding(@NotNull DynamicBinding binding) {
    this.checkFilterPresent();
    return new DynamicBindingImpl(this.bindingKeyMatcher, bindingKey -> binding.tryMatch(bindingKey).orElse(null));
  }

  @Override
  public @NotNull DynamicBinding toKeyedBindingProvider(
    @NotNull Function<ScopeableBindingBuilder<?>, UninstalledBinding<?>> bindingProvider
  ) {
    this.checkFilterPresent();
    return new DynamicBindingImpl(this.bindingKeyMatcher, bindingKey -> {
      ScopeableBindingBuilder<?> bindingBuilder = this.rootBindingBuilder.bind(bindingKey);
      return bindingProvider.apply(bindingBuilder);
    });
  }

  @Override
  public @NotNull DynamicBinding toBindingProvider(
    @NotNull Function<RootBindingBuilder, UninstalledBinding<?>> bindingProvider
  ) {
    this.checkFilterPresent();
    return new DynamicBindingImpl(this.bindingKeyMatcher, bindingKey -> bindingProvider.apply(this.rootBindingBuilder));
  }

  private void appendFilter(@NotNull Predicate<BindingKey<?>> filter) {
    if (this.bindingKeyMatcher == null) {
      this.bindingKeyMatcher = filter;
    } else {
      this.bindingKeyMatcher = this.bindingKeyMatcher.and(filter);
    }
  }

  private void checkFilterPresent() {
    if (this.bindingKeyMatcher == null) {
      throw new IllegalStateException("No filter present");
    }
  }
}
