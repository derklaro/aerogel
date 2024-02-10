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

package dev.derklaro.aerogel.binding.dynamic;

public enum StandardInjectableElementType implements InjectableElementType {
  CLASS(TypeFlag.TYPE_CLASS),
  FIELD(TypeFlag.TYPE_MEMBER),
  METHOD(TypeFlag.TYPE_MEMBER, TypeFlag.TYPE_EXECUTABLE),
  CONSTRUCTOR(TypeFlag.TYPE_MEMBER, TypeFlag.TYPE_EXECUTABLE),
  PARAMETER(TypeFlag.TYPE_PARAMETER);

  private final int flags;

  StandardInjectableElementType(int... flags) {
    this.flags = TypeFlag.encodeFlags(flags);
  }

  @Override
  public boolean isClass() {
    return TypeFlag.has(this.flags, TypeFlag.TYPE_CLASS);
  }

  @Override
  public boolean isMember() {
    return TypeFlag.has(this.flags, TypeFlag.TYPE_MEMBER);
  }

  @Override
  public boolean isExecutable() {
    return TypeFlag.has(this.flags, TypeFlag.TYPE_EXECUTABLE);
  }

  @Override
  public boolean isParameter() {
    return TypeFlag.has(this.flags, TypeFlag.TYPE_PARAMETER);
  }

  private static final class TypeFlag {

    private static final int TYPE_CLASS = 0x01;
    private static final int TYPE_MEMBER = 0x02;
    private static final int TYPE_EXECUTABLE = 0x04;
    private static final int TYPE_PARAMETER = 0x08;

    private static int encodeFlags(int... flags) {
      if (flags.length == 0) {
        return 0;
      } else if (flags.length == 1) {
        return flags[0];
      } else {
        int encodedFlags = 0;
        for (int flag : flags) {
          encodedFlags |= flag;
        }
        return encodedFlags;
      }
    }

    private static boolean has(int flags, int flag) {
      return (flags & flag) != 0;
    }
  }
}
