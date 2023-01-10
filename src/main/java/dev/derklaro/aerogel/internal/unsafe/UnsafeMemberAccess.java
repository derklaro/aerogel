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

package dev.derklaro.aerogel.internal.unsafe;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * An unsafe way to set members accessible, even if they are not accessible from the caller class.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.internal")
public final class UnsafeMemberAccess {

  // marker object to retrieve the override boolean field
  private static final Object SOME_OBJECT = new Object();

  private static final long OVERRIDE_BOOLEAN_OFFSET;

  static {
    long overrideBooleanOff = -1;

    // we can only do that if we are able to get the unsafe instance
    if (UnsafeAccess.isAvailable()) {
      try {
        // get the SOME_OBJECT field in this class, leave it inaccessible
        Field inaccessibleField = UnsafeMemberAccess.class.getDeclaredField("SOME_OBJECT");

        // get the SOME_OBJECT field in this class, make it accessible
        Field accessibleField = UnsafeMemberAccess.class.getDeclaredField("SOME_OBJECT");
        accessibleField.setAccessible(true);

        // find the offset of the accessible override boolean
        for (long off = 8; off < 128; off++) {
          byte accessible = UnsafeAccess.U.getByte(accessibleField, off);
          byte inaccessible = UnsafeAccess.U.getByte(inaccessibleField, off);

          // check if we got both bytes and if they differ
          if (accessible == 1 && inaccessible == 0) {
            // try to make the inaccessible field accessible with the offset
            UnsafeAccess.U.putByte(inaccessibleField, off, (byte) 1);
            //noinspection deprecation
            if (inaccessibleField.isAccessible()) {
              // that's the correct field
              overrideBooleanOff = off;
              break;
            } else {
              // wrong field, revert the change
              UnsafeAccess.U.putByte(inaccessibleField, off, (byte) 0);
            }
          }
        }
      } catch (Exception ignored) {
      }
    }

    // assign the offset
    OVERRIDE_BOOLEAN_OFFSET = overrideBooleanOff;
  }

  private UnsafeMemberAccess() {
    throw new UnsupportedOperationException();
  }

  /**
   * Makes a field accessible, ignoring potential thrown exceptions when the offset is not present and the fallback
   * method usage needs to be used.
   *
   * @param accessibleObject the object to make accessible.
   * @param <T>              the type of accessible object to make accessible.
   * @return the same object as given, for chaining.
   * @throws NullPointerException if the given accessible object is null.
   */
  public static @NotNull <T extends AccessibleObject> T makeAccessible(@NotNull T accessibleObject) {
    makeAccessible(accessibleObject, false);
    return accessibleObject;
  }

  /**
   * Makes a field accessible, rethrowing potential exceptions when the offset is not present and the fallback method
   * usage needs to be used.
   *
   * <p>This method throws an <strong>java.lang.reflect.InaccessibleObjectException</strong> if the fallback
   * setAccessible call fails, starting with Java 9.
   *
   * @param accessibleObject the object to make accessible.
   * @param <T>              the type of accessible object to make accessible.
   * @return the same object as given, for chaining.
   * @throws NullPointerException if the given accessible object is null.
   */
  public static @NotNull <T extends AccessibleObject> T forceMakeAccessible(@NotNull T accessibleObject) {
    makeAccessible(accessibleObject, true);
    return accessibleObject;
  }

  /**
   * Makes a field accessible.
   *
   * <p>This method throws an <strong>java.lang.reflect.InaccessibleObjectException</strong> if the fallback
   * setAccessible call fails and ensure is set to true, starting with Java 9.
   *
   * @param accessibleObject the object to make accessible.
   * @param ensure           if any exceptions should get re-thrown.
   * @throws NullPointerException if the given accessible object is null.
   */
  private static void makeAccessible(@NotNull AccessibleObject accessibleObject, boolean ensure) {
    // check if the object is already accessible, no need to do anything
    // we can ignore deprecation here as the method was deprecated because the name might be misleading,
    // but in our cases it does exactly what we want
    //noinspection deprecation
    if (accessibleObject.isAccessible()) {
      return;
    }

    if (OVERRIDE_BOOLEAN_OFFSET != -1) {
      // we got the offset, force our way in
      UnsafeAccess.U.putByte(accessibleObject, OVERRIDE_BOOLEAN_OFFSET, (byte) 1);
    } else {
      try {
        // try to make it accessible the old way
        accessibleObject.setAccessible(true);
      } catch (Exception exception) {
        // unable to make the field accessible, rethrow the exception if we need to ensure that after the method call
        // the field really is accessible
        if (ensure) {
          throw exception;
        }
      }
    }
  }
}
