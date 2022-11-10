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

package dev.derklaro.aerogel.internal.member;

import dev.derklaro.aerogel.MemberInjectionSettings;
import org.jetbrains.annotations.NotNull;

/**
 * A default implementation of a {@link MemberInjectionSettings.Builder}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class DefaultMemberInjectionSettingsBuilder implements MemberInjectionSettings.Builder {

  private boolean injectPrivateMethods = true;
  private boolean injectStaticMethods = true;
  private boolean injectInstanceMethods = true;
  private boolean injectInheritedMethods = true;
  private boolean injectPrivateFields = true;
  private boolean injectStaticFields = true;
  private boolean injectInstanceFields = true;
  private boolean injectInheritedFields = true;
  private boolean injectOnlyUninitializedFields = false;
  private boolean executePostConstructListeners = true;

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder injectPrivateMethods(boolean injectPrivateMethods) {
    this.injectPrivateMethods = injectPrivateMethods;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder injectStaticMethods(boolean injectStaticMethods) {
    this.injectStaticMethods = injectStaticMethods;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder injectInstanceMethods(boolean injectInstanceMethods) {
    this.injectInstanceMethods = injectInstanceMethods;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder injectInheritedMethods(boolean injectInheritedMethods) {
    this.injectInheritedMethods = injectInheritedMethods;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder injectPrivateFields(boolean injectPrivateFields) {
    this.injectPrivateFields = injectPrivateFields;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder injectStaticFields(boolean injectStaticFields) {
    this.injectStaticFields = injectStaticFields;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder injectInstanceFields(boolean injectInstanceFields) {
    this.injectInstanceFields = injectInstanceFields;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder injectInheritedFields(boolean injectInheritedFields) {
    this.injectInheritedFields = injectInheritedFields;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder injectOnlyUninitializedFields(boolean injectOnlyUninitializedFields) {
    this.injectOnlyUninitializedFields = injectOnlyUninitializedFields;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings.Builder executePostConstructListeners(boolean executePostConstructListeners) {
    this.executePostConstructListeners = executePostConstructListeners;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull MemberInjectionSettings build() {
    return new DefaultMemberInjectionSettings(
      this.injectPrivateMethods,
      this.injectStaticMethods,
      this.injectInstanceMethods,
      this.injectInheritedMethods,
      this.injectPrivateFields,
      this.injectStaticFields,
      this.injectInstanceFields,
      this.injectInheritedFields,
      this.injectOnlyUninitializedFields,
      this.executePostConstructListeners);
  }
}
