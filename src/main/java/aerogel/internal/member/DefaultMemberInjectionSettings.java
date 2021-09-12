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

package aerogel.internal.member;

import aerogel.MemberInjectionSettings;

final class DefaultMemberInjectionSettings implements MemberInjectionSettings {

  private final boolean injectPrivateMethods;
  private final boolean injectStaticMethods;
  private final boolean injectInstanceMethods;
  private final boolean injectInheritedMethods;
  private final boolean injectPrivateFields;
  private final boolean injectStaticFields;
  private final boolean injectInstanceFields;
  private final boolean injectInheritedFields;
  private final boolean injectOnlyUninitializedFields;

  public DefaultMemberInjectionSettings(
    boolean injectPrivateMethods,
    boolean injectStaticMethods,
    boolean injectInstanceMethods,
    boolean injectInheritedMethods,
    boolean injectPrivateFields,
    boolean injectStaticFields,
    boolean injectInstanceFields,
    boolean injectInheritedFields,
    boolean injectOnlyUninitializedFields
  ) {
    this.injectPrivateMethods = injectPrivateMethods;
    this.injectStaticMethods = injectStaticMethods;
    this.injectInstanceMethods = injectInstanceMethods;
    this.injectInheritedMethods = injectInheritedMethods;
    this.injectPrivateFields = injectPrivateFields;
    this.injectStaticFields = injectStaticFields;
    this.injectInstanceFields = injectInstanceFields;
    this.injectInheritedFields = injectInheritedFields;
    this.injectOnlyUninitializedFields = injectOnlyUninitializedFields;
  }

  @Override
  public boolean injectPrivateMethods() {
    return this.injectPrivateMethods;
  }

  @Override
  public boolean injectStaticMethods() {
    return this.injectStaticMethods;
  }

  @Override
  public boolean injectInstanceMethods() {
    return this.injectInstanceMethods;
  }

  @Override
  public boolean injectInheritedMethods() {
    return this.injectInheritedMethods;
  }

  @Override
  public boolean injectPrivateFields() {
    return this.injectPrivateFields;
  }

  @Override
  public boolean injectStaticFields() {
    return this.injectStaticFields;
  }

  @Override
  public boolean injectInstanceFields() {
    return this.injectInstanceFields;
  }

  @Override
  public boolean injectInheritedFields() {
    return this.injectInheritedFields;
  }

  @Override
  public boolean injectOnlyUninitializedFields() {
    return this.injectOnlyUninitializedFields;
  }
}
