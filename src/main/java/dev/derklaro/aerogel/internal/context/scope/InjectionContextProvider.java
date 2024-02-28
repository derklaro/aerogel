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

package dev.derklaro.aerogel.internal.context.scope;

import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import jakarta.inject.Provider;
import java.util.Map;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A provider for injection context scopes.
 *
 * @author Pasqual K.
 * @since 3.0
 */
@API(status = API.Status.INTERNAL, since = "3.0")
public interface InjectionContextProvider {

  /**
   * Gets the singleton global injection context provider for all injectors.
   *
   * @return the injection context provider for all injectors.
   */
  static @NotNull InjectionContextProvider provider() {
    return InjectionContextProviderHolder.getContextProvider();
  }

  default @Nullable InjectionContext currentContext() {
    InjectionContextScope scope = this.currentScope();
    InjectionContext currentContext = scope == null ? null : scope.context();
    return currentContext == null || currentContext.obsolete() ? null : currentContext;
  }

  /**
   * Get the current injection context scope, if any is bound in the current context.
   *
   * @return the current injection context scope, if any is bound in the current context.
   */
  @Nullable
  InjectionContextScope currentScope();

  @NotNull
  InjectionContextScope enterContextScope(
    @NotNull InstalledBinding<?> binding,
    @NotNull Map<BindingKey<?>, Provider<?>> overrides);
}
