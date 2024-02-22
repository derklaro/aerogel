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
import java.lang.annotation.Annotation;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A binding step that allows for scoping of the binding. Scopes allow the return value of a binding to be modified for
 * the current scope. An example could be a user instance that is bound to the current HTTP request. This could be
 * compared to ThreadLocal or ScopedValue which are built-in the JDK. By default, a singleton scope is supported, which
 * indicates that an instance creation should only happen once for the binding during the JVM lifetime. This is the same
 * behaviour as a lazy singleton.
 * <p>
 * If no scopes are provided to the binding builder, the scopes that are declared on the target type are used instead.
 * If the builder wants to override this behaviour to specifically not add any scopes, the {@link #unscoped()} method
 * can be used to indicate this.
 * <p>
 * If a scope is applied using the annotation type ({@link #scopedWith(Class)}), the corresponding scope applier is
 * resolved from the scope registry of the injector that owns the builder. Only a single scope can be applied to a
 * binding.
 *
 * @param <T> the type of values handled by this binding that is being built.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface ScopeableBindingBuilder<T> extends AdvancedBindingBuilder<T> {

  /**
   * Instructs the builder that explicitly no scoping should be applied to the binding. Calling this method differs from
   * adding no scopes, as the scopes that are declared on the target type are not applied as well.
   *
   * @return the next builder step which allows to target the binding builder.
   */
  @NotNull
  AdvancedBindingBuilder<T> unscoped();

  /**
   * Instructs this builder that the singleton scope should be applied after constructing the binding.
   *
   * @return the next builder step which allows to target the binding builder.
   * @see jakarta.inject.Singleton
   */
  @NotNull
  AdvancedBindingBuilder<T> scopedWithSingleton();

  /**
   * Instructs the builder to apply the given scope after constructing the binding.
   *
   * @param scopeApplier the scope to apply after constructing the binding.
   * @return the next builder step which allows to target the binding builder.
   */
  @NotNull
  AdvancedBindingBuilder<T> scopedWith(@NotNull ScopeApplier scopeApplier);

  /**
   * Resolves the scope applier that is associated with the given scope annotation from the scope registry of the
   * injector that owns this binding. The given annotation type must be a scope annotation.
   *
   * @param scopeAnnotationType the annotation type of the scope to apply.
   * @return the next builder step which allows to target the binding builder.
   * @throws IllegalArgumentException if the given annotation is not a scope annotation or no applier is registered.
   */
  @NotNull
  AdvancedBindingBuilder<T> scopedWith(@NotNull Class<? extends Annotation> scopeAnnotationType);
}
