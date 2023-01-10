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

package dev.derklaro.aerogel.util;

import dev.derklaro.aerogel.Name;
import dev.derklaro.aerogel.internal.annotation.AnnotationFactory;
import java.util.Collections;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A collection of default qualifiers.
 *
 * @since 2.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public final class Qualifiers {

  private Qualifiers() {
    throw new UnsupportedOperationException();
  }

  /**
   * Constructs a new name annotation, for example to require a parameter binding to have the given name.
   *
   * @param name the name which is required.
   * @return a name annotation which has the value set to the given name.
   * @throws NullPointerException if the given name is null.
   */
  public static @NotNull Name named(@NotNull String name) {
    return AnnotationFactory.generateAnnotation(Name.class, Collections.singletonMap("value", name));
  }
}
