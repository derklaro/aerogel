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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.internal.member.DefaultMemberInjectionSettingsBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Holds all settings which are relevant when injecting members into a class.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public interface MemberInjectionSettings {

  /**
   * Creates a new builder for {@link MemberInjectionSettings}. All settings are enabled by default except for {@link
   * #injectOnlyUninitializedFields()} which is disabled by default.
   *
   * @return a new builder for {@link MemberInjectionSettings}.
   */
  @Contract(pure = true)
  static @NotNull Builder builder() {
    return new DefaultMemberInjectionSettingsBuilder();
  }

  /**
   * Get if private methods should get injected. This defaults to {@code true}.
   *
   * @return if private methods should get injected.
   */
  boolean injectPrivateMethods();

  /**
   * Get if static methods should get injected. This defaults to {@code true}. Static methods are only injected once per
   * {@link MemberInjector}.
   *
   * @return if static methods should get injected.
   */
  boolean injectStaticMethods();

  /**
   * Get if instance (non-static) methods should get injected. This defaults to {@code true}.
   *
   * @return if instance (non-static) methods should get injected.
   */
  boolean injectInstanceMethods();

  /**
   * Get if inherited methods should get injected. This defaults to {@code true}.
   *
   * @return if inherited methods should get injected.
   */
  boolean injectInheritedMethods();

  /**
   * Get if private fields should get injected. This defaults to {@code true}.
   *
   * @return if private fields should get injected.
   */
  boolean injectPrivateFields();

  /**
   * Get if static fields should get injected. This defaults to {@code true}.
   *
   * @return if static fields should get injected.
   */
  boolean injectStaticFields();

  /**
   * Get if instance (non-static) fields should get injected. This defaults to {@code true}.
   *
   * @return if instance (non-static) fields should get injected.
   */
  boolean injectInstanceFields();

  /**
   * Get if inherited fields should get injected. This defaults to {@code true}.
   *
   * @return if inherited fields should get injected.
   */
  boolean injectInheritedFields();

  /**
   * Get if only uninitialized fields should get injected. A field is considered uninitialized if it's current value is
   * {@code null} or if the primitive type has its default uninitialized value as described in the oracle documentation
   * <a href="https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html">here</a>. This defaults to
   * {@code false}.
   *
   * @return if only uninitialized fields should get injected.
   */
  boolean injectOnlyUninitializedFields();

  /**
   * Represents a builder for {@link MemberInjectionSettings}.
   *
   * @author Pasqual K.
   * @since 1.0
   */
  interface Builder {

    /**
     * Sets if private methods should get injected. This defaults to {@code true}.
     *
     * @param injectPrivateMethods if private methods should get injected.
     * @return the same builder instance as used for calling the method, for chaining.
     */
    @NotNull Builder injectPrivateMethods(boolean injectPrivateMethods);

    /**
     * Sets if static methods should get injected. Static methods are only injected once per {@link MemberInjector}.
     * This defaults to {@code true}.
     *
     * @param injectStaticMethods if static methods should get injected.
     * @return the same builder instance as used for calling the method, for chaining.
     */
    @NotNull Builder injectStaticMethods(boolean injectStaticMethods);

    /**
     * Sets if instance (non-static) methods should get injected. This defaults to {@code true}.
     *
     * @param injectInstanceMethods if instance methods should get injected.
     * @return the same builder instance as used for calling the method, for chaining.
     */
    @NotNull Builder injectInstanceMethods(boolean injectInstanceMethods);

    /**
     * Sets if inherited methods should get injected. This applies to static as well as instance methods. This defaults
     * to {@code true}.
     *
     * @param injectInheritedMethods if inherited methods should get injected.
     * @return the same builder instance as used for calling the method, for chaining.
     */
    @NotNull Builder injectInheritedMethods(boolean injectInheritedMethods);

    /**
     * Sets if private fields should get injected. This defaults to {@code true}.
     *
     * @param injectPrivateFields if private fields should get injected.
     * @return the same builder instance as used for calling the method, for chaining.
     */
    @NotNull Builder injectPrivateFields(boolean injectPrivateFields);

    /**
     * Sets if static fields should get injected. This defaults to {@code true}.
     *
     * @param injectStaticFields if static fields should get injected.
     * @return the same builder instance as used for calling the method, for chaining.
     */
    @NotNull Builder injectStaticFields(boolean injectStaticFields);

    /**
     * Sets if instance (non-static) fields should get injected. This defaults to {@code true}.
     *
     * @param injectInstanceFields if instance fields should get injected.
     * @return the same builder instance as used for calling the method, for chaining.
     */
    @NotNull Builder injectInstanceFields(boolean injectInstanceFields);

    /**
     * If inherited fields should get injected. This applies to static as well as instance fields. This defaults to
     * {@code true}.
     *
     * @param injectInheritedFields if inherited fields should get injected.
     * @return the same builder instance as used for calling the method, for chaining.
     */
    @NotNull Builder injectInheritedFields(boolean injectInheritedFields);

    /**
     * Sets if a field should only be initialized if the field was not yet initialized. A field is considered
     * uninitialized if it's current value is {@code null} or if the primitive type has its default uninitialized value
     * as described in the oracle documentation <a href="https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html">here</a>.
     * This defaults to {@code false}.
     *
     * @param injectOnlyUninitializedFields if only uninitailized fields should get injected.
     * @return the same builder instance as used for calling the method, for chaining.
     */
    @NotNull Builder injectOnlyUninitializedFields(boolean injectOnlyUninitializedFields);

    /**
     * Builds a new {@link MemberInjectionSettings} based on the given settings. A builder can get re-used after a call
     * to this method.
     *
     * @return a new {@link MemberInjectionSettings} based on the given settings.
     */
    @Contract(pure = true)
    @NotNull MemberInjectionSettings build();
  }
}
