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

package dev.derklaro.aerogel.binding.builder;

import dev.derklaro.aerogel.ScopeApplier;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.dynamic.DynamicBinding;
import dev.derklaro.aerogel.binding.dynamic.InjectableElement;
import dev.derklaro.aerogel.binding.dynamic.InjectableElementType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

public interface DynamicBindingBuilder {

  @NotNull
  DynamicBindingBuilder scopedWith(@NotNull ScopeApplier scopeApplier);

  @NotNull
  DynamicBindingBuilder scopedWith(@NotNull Class<? extends Annotation> scopeAnnotationType);

  @NotNull
  DynamicBindingBuilder matchElementType(@NotNull InjectableElementType elementType);

  @NotNull
  DynamicBindingBuilder annotationPresent(@NotNull Class<? extends Annotation> annotationType);

  @NotNull
  DynamicBindingBuilder matchAnnotation(@NotNull Predicate<? extends Annotation> filter);

  @NotNull
  <A extends Annotation> DynamicBindingBuilder matchAnnotation(
    @NotNull Class<A> annotationType,
    @NotNull Predicate<A> filter);

  @NotNull
  DynamicBindingBuilder exactRawType(@NotNull Class<?> type);

  @NotNull
  DynamicBindingBuilder superRawType(@NotNull Class<?> type);

  @NotNull
  DynamicBindingBuilder derivedRawType(@NotNull Class<?> type);

  @NotNull
  DynamicBindingBuilder matchRawType(@NotNull Predicate<Class<?>> filter);

  @NotNull
  DynamicBindingBuilder matchType(@NotNull Predicate<Type> filter);

  @NotNull
  DynamicBindingBuilder matchElement(@NotNull Predicate<InjectableElement> filter);

  @NotNull
  DynamicBinding toBinding(@NotNull DynamicBinding binding);

  @NotNull
  DynamicBinding toBindingProvider(@NotNull Supplier<UninstalledBinding<?>> bindingProvider);

  @NotNull
  DynamicBinding toBindingProvider(@NotNull Function<InjectableElement, UninstalledBinding<?>> bindingProvider);
}
