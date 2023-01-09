/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2023 Pasqual K. and contributors
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

package dev.derklaro.aerogel.member;

import java.util.EnumSet;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A list of all member types which can be injected.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public enum InjectionSetting {

  /**
   * Represents all static methods without specifying if they are static or not. Note that the flag for static or
   * instance methods must be present along this type as well.
   */
  PRIVATE_METHODS,
  /**
   * Represents all static methods without specifying if they are private or inherited. The flag for private methods
   * must be given as well to inject the private static methods.
   */
  STATIC_METHODS,
  /**
   * Represents method which are available to instances. Note that this does not include private and inherited methods
   * by default, the associated flags must be set as well.
   */
  INSTANCE_METHODS,
  /**
   * Represents all inherited methods, excluding private methods. The flag for private methods must be given in order to
   * also inject private inherited methods.
   */
  INHERITED_METHODS,

  /**
   * Represents all private fields. This does not mean that all private fields are injected, the flags for static and
   * instance fields need to be present for that purpose as well.
   */
  PRIVATE_FIELDS,
  /**
   * Represents all static fields. If the flag for inherited fields is not given, only the static fields in the current
   * class are injected.
   */
  STATIC_FIELDS,
  /**
   * Represents all field which are only available to instances. If the flag for inherited fields is not given, only the
   * fields in the current class are injected.
   */
  INSTANCE_FIELDS,
  /**
   * Represents all fields that are inherited. This does not mean that all inherited fields are injected, the flags for
   * static and instance fields need to be present for that purpose as well.
   */
  INHERITED_FIELDS,
  /**
   * Only injects the uninitialized fields. A field is marked as uninitialized when
   * <ol>
   *   <li>The field value is null, that applies to all non-primitive types.
   *   <li>The field value is the default primitive value, that applied to all primitive types.
   * </ol>
   */
  ONLY_UNINITIALIZED_FIELDS,

  /**
   * Defines if post construct listener should get executed.
   */
  RUN_POST_CONSTRUCT_LISTENERS;

  /**
   * A flag type representing all static and inherited members.
   */
  public static final long FLAGS_STATIC_MEMBERS = toFlag(
    InjectionSetting.PRIVATE_FIELDS,
    InjectionSetting.PRIVATE_METHODS,
    InjectionSetting.STATIC_FIELDS,
    InjectionSetting.STATIC_METHODS,
    InjectionSetting.INHERITED_FIELDS,
    InjectionSetting.INHERITED_METHODS);
  /**
   * A flag that represents all members: static, instance & inherited.
   */
  public static final long FLAGS_ALL_MEMBERS = toFlag(
    InjectionSetting.PRIVATE_METHODS,
    InjectionSetting.STATIC_METHODS,
    InjectionSetting.INSTANCE_METHODS,
    InjectionSetting.INHERITED_METHODS,
    InjectionSetting.PRIVATE_FIELDS,
    InjectionSetting.STATIC_FIELDS,
    InjectionSetting.INSTANCE_FIELDS,
    InjectionSetting.INHERITED_FIELDS,
    InjectionSetting.RUN_POST_CONSTRUCT_LISTENERS);
  /**
   * All member types which are known.
   */
  private static final InjectionSetting[] VALUES = values();

  private final long raw;

  /**
   * Constructs a new member type, setting the raw value of the type.
   */
  InjectionSetting() {
    this.raw = 1L << this.ordinal();
  }

  /**
   * Converts the given array of member types to a flag which can be checked if a member type is present. The given
   * array has no order requirement for that.
   *
   * @param injectionSettings the member types to convert to a flag.
   * @return the flag value constructed from the raw value of all given member types.
   */
  public static long toFlag(@NotNull InjectionSetting... injectionSettings) {
    long raw = 0L;
    for (InjectionSetting injectionSetting : injectionSettings) {
      raw |= injectionSetting.raw;
    }
    return raw;
  }

  /**
   * Reverses the member type flag creation by converting the given flags back to an enum set of member types.
   *
   * @param flags the flags to parse the member types from.
   * @return the member types that are present in the given flags.
   */
  public static @NotNull EnumSet<InjectionSetting> fromFlags(long flags) {
    // if no flags are given we can just return an empty set
    EnumSet<InjectionSetting> injectionSettings = EnumSet.noneOf(InjectionSetting.class);
    if (flags == 0) {
      return injectionSettings;
    }

    // check for each flag if it is present
    for (InjectionSetting value : VALUES) {
      if (value.enabled(flags)) {
        injectionSettings.add(value);
      }
    }
    return injectionSettings;
  }

  /**
   * Checks if this member type is enabled in the given flag bitmask.
   *
   * @param flags the flags to check in.
   * @return true if the flag is enabled, false otherwise.
   */
  public boolean enabled(long flags) {
    return (flags & this.raw) == this.raw;
  }

  /**
   * Checks if this member type is disabled in the given flag bitmask.
   *
   * @param flags the flags to check in.
   * @return true if the flag is disabled, false otherwise.
   */
  public boolean disabled(long flags) {
    return (flags & this.raw) == 0;
  }
}
