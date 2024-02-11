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

import org.apiguardian.api.API;

/**
 * Standard collection of supported element types.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public enum StandardInjectableElementType implements InjectableElementType {

  /**
   * The element type of some sort of class (interface, enum, record, ...).
   */
  CLASS(TypeFlag.TYPE_CLASS),
  /**
   * The element type for a field.
   */
  FIELD(TypeFlag.TYPE_MEMBER),
  /**
   * The element type for a method.
   */
  METHOD(TypeFlag.TYPE_MEMBER, TypeFlag.TYPE_EXECUTABLE),
  /**
   * The element type for a constructor.
   */
  CONSTRUCTOR(TypeFlag.TYPE_MEMBER, TypeFlag.TYPE_EXECUTABLE),
  /**
   * The element type for a method or constructor parameter.
   */
  PARAMETER(TypeFlag.TYPE_PARAMETER);

  private final int flags;

  /**
   * Constructs a new injectable element type. Valid flags are listed as constants in {@link TypeFlag}.
   *
   * @param flags the flags of the element type.
   */
  StandardInjectableElementType(int... flags) {
    this.flags = TypeFlag.encodeFlags(flags);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isClass() {
    return TypeFlag.has(this.flags, TypeFlag.TYPE_CLASS);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isMember() {
    return TypeFlag.has(this.flags, TypeFlag.TYPE_MEMBER);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isExecutable() {
    return TypeFlag.has(this.flags, TypeFlag.TYPE_EXECUTABLE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isParameter() {
    return TypeFlag.has(this.flags, TypeFlag.TYPE_PARAMETER);
  }

  /**
   * The valid type flags for standard element types.
   *
   * @author Pasqual Koschmieder
   * @since 3.0
   */
  @API(status = API.Status.INTERNAL, since = "3.0")
  private static final class TypeFlag {

    /**
     * Type flag for classes.
     */
    private static final int TYPE_CLASS = 0x01;
    /**
     * Type flag for class members.
     */
    private static final int TYPE_MEMBER = 0x02;
    /**
     * Type flag for executables.
     */
    private static final int TYPE_EXECUTABLE = 0x04;
    /**
     * Type flag for parameters.
     */
    private static final int TYPE_PARAMETER = 0x08;

    /**
     * Encodes the given flag array to a bitmask.
     *
     * @param flags the flags to encode.
     * @return a bitmask representing the given flags.
     */
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

    /**
     * Checks if a particular flag is set in the given flags bitmask.
     *
     * @param flags the flag bitmask.
     * @param flag  the flag to check for.
     * @return true if the given flag is set in the given flags bitmask, false otherwise.
     */
    private static boolean has(int flags, int flag) {
      return (flags & flag) != 0;
    }
  }
}
