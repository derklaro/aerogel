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

package aerogel;

import aerogel.internal.member.DefaultMemberInjectionSettingsBuilder;
import org.jetbrains.annotations.NotNull;

public interface MemberInjectionSettings {

  static @NotNull Builder builder() {
    return new DefaultMemberInjectionSettingsBuilder();
  }

  boolean injectPrivateMethods();

  boolean injectStaticMethods();

  boolean injectInstanceMethods();

  boolean injectInheritedMethods();

  boolean injectPrivateFields();

  boolean injectStaticFields();

  boolean injectInstanceFields();

  boolean injectInheritedFields();

  boolean injectOnlyUninitializedFields();

  interface Builder {

    @NotNull Builder injectPrivateMethods(boolean injectPrivateMethods);

    @NotNull Builder injectStaticMethods(boolean injectStaticMethods);

    @NotNull Builder injectInstanceMethods(boolean injectInstanceMethods);

    @NotNull Builder injectInheritedMethods(boolean injectInheritedMethods);

    @NotNull Builder injectPrivateFields(boolean injectPrivateFields);

    @NotNull Builder injectStaticFields(boolean injectStaticFields);

    @NotNull Builder injectInstanceFields(boolean injectInstanceFields);

    @NotNull Builder injectInheritedFields(boolean injectInheritedFields);

    @NotNull Builder injectOnlyUninitializedFields(boolean injectOnlyUninitializedFields);

    @NotNull MemberInjectionSettings build();
  }
}
