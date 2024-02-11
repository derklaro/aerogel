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
 * Defines a type of injectable element. Some default methods are provided for distinction, but virtually every other
 * implementation is possible, even if it matches none of the characteristics described by the methods below.
 *
 * @author Pasqual Koschmieder
 * @see StandardInjectableElementType
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface InjectableElementType {

  /**
   * Get if the element type is some sort of class (interface, record, enum, ...).
   *
   * @return if the element type is some sort of class.
   */
  boolean isClass();

  /**
   * Get if this element is some sort of class member (field, method or constructor).
   *
   * @return if this element is some sort of class member.
   */
  boolean isMember();

  /**
   * Get if this element is executable (method or constructor).
   *
   * @return if this element is executable.
   */
  boolean isExecutable();

  /**
   * Get if this element is some sort of executable parameter (method or constructor parameter).
   *
   * @return if this element is some sort of executable parameter.
   */
  boolean isParameter();
}
