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

package dev.derklaro.aerogel.internal.context;

import dev.derklaro.aerogel.Element;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A stack of elements used for easy element access in the default injection context implementation.
 *
 * @author Pasqual K.
 * @since 1.0
 */
final class ElementStack {

  /**
   * The spacer between entries when dumping the walking stack.
   */
  private static final String PATH_CHAINER = " => ";

  /**
   * The current stack holder we are working on.
   */
  private final Deque<Element> stack = new ArrayDeque<>();

  /**
   * Removes all entries from the current stack.
   */
  public void dry() {
    this.stack.clear();
  }

  /**
   * Pushes the given {@code element} to the head of the stack.
   *
   * @param element the element to push into the stack.
   */
  public void push(@NotNull Element element) {
    this.stack.push(element);
  }

  /**
   * Checks if this stack contains the given {@code element}.
   *
   * @param element the element to check for.
   * @return true if this stack contains the given element, false otherwise.
   */
  public boolean has(@NotNull Element element) {
    return this.stack.contains(element);
  }

  /**
   * Removes the given element from the underlying stack.
   *
   * @param element the element to remove.
   * @since 2.0
   */
  public void take(@NotNull Element element) {
    this.stack.remove(element);
  }

  /**
   * Get if the underlying stack has no elements.
   *
   * @return true if the stack is empty, false otherwise.
   * @since 2.0
   */
  public boolean empty() {
    return this.stack.isEmpty();
  }

  /**
   * Searches through the whole stack for the first element which matches the given {@code filter}.
   *
   * @param filter the filter applied to each element in the stack.
   * @return the first element which successfully passed the {@code filter} or null if no element passes.
   */
  public @Nullable Element filter(@NotNull Predicate<Element> filter) {
    for (Element element : this.stack) {
      if (filter.test(element)) {
        return element;
      }
    }
    return null;
  }

  /**
   * Dumps the whole walking stack into a string beginning with the tail of the stack walking to the top of it.
   *
   * @param last the last element which should get append to the stack.
   * @return the dumped walking stack, may be an empty string if the stack is empty.
   */
  public @NotNull String dumpWalkingStack(@NotNull Element last) {
    // no element - no stack
    if (this.stack.isEmpty()) {
      return "";
    } else {
      // build the stack using a reversed iterator - the first added element should be the first of the chain
      StringBuilder stringBuilder = new StringBuilder();
      Iterator<Element> reversedIterator = this.stack.descendingIterator();
      // walk over the stack elements - start with the firstly added one
      while (reversedIterator.hasNext()) {
        stringBuilder.append(reversedIterator.next()).append(PATH_CHAINER);
      }
      // append the last element (or current one)
      stringBuilder.append(last);
      // convert to chain builder to a string
      return stringBuilder.toString();
    }
  }
}
