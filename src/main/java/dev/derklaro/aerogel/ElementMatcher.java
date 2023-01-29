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

package dev.derklaro.aerogel;

import java.util.function.Predicate;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A matcher than checks if an element matches a condition.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@FunctionalInterface
@API(status = API.Status.STABLE, since = "2.0")
public interface ElementMatcher extends Predicate<Element> {

  /**
   * Checks if the single, given element equals the element to check against.
   *
   * @param requiredElement the element that must match.
   * @return a matcher that checks that the given value is equal to the required element.
   */
  static @NotNull ElementMatcher matchesOne(@NotNull Element requiredElement) {
    return element -> element.equals(requiredElement);
  }

  /**
   * Checks if all given elements equal the element to check against.
   *
   * @param requiredElements the elements that must match.
   * @return a matcher that checks that all given elements are equal to the required element.
   */
  static @NotNull ElementMatcher matchesAll(@NotNull Element[] requiredElements) {
    return element -> {
      for (Element requiredElement : requiredElements) {
        if (!element.equals(requiredElement)) {
          return false;
        }
      }
      return true;
    };
  }

  /**
   * Checks if one of the given elements is equal to the element to check against.
   *
   * @param requiredElements the elements that must match.
   * @return a matcher that checks that one of the given elements is equal to the required element.
   */
  static @NotNull ElementMatcher matchesSome(@NotNull Element[] requiredElements) {
    return element -> {
      for (Element requiredElement : requiredElements) {
        if (element.equals(requiredElement)) {
          return true;
        }
      }
      return false;
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  boolean test(Element element);

  /**
   * {@inheritDoc}
   */
  @Override
  default @NotNull ElementMatcher negate() {
    return element -> !this.test(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default @NotNull ElementMatcher and(@NotNull Predicate<? super Element> other) {
    return element -> this.test(element) && other.test(element);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  default @NotNull ElementMatcher or(@NotNull Predicate<? super Element> other) {
    return element -> this.test(element) || other.test(element);
  }
}
