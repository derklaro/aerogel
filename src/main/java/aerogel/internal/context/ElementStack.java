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

package aerogel.internal.context;

import aerogel.Element;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ElementStack {

  private static final String PATH_CHAINER = " => ";

  private final Deque<Element> stack = new ArrayDeque<>();

  public void dry() {
    this.stack.clear();
  }

  public void push(@NotNull Element element) {
    this.stack.push(element);
  }

  public boolean has(@NotNull Element element) {
    return this.stack.contains(element);
  }

  public @Nullable Element filter(@NotNull Predicate<Element> filter) {
    for (Element element : stack) {
      if (filter.test(element)) {
        return element;
      }
    }
    return null;
  }

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
