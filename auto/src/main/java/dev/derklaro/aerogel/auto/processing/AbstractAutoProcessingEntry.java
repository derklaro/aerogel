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

package dev.derklaro.aerogel.auto.processing;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * An abstract implementation of an annotation processing entry which takes over the handling of the name and supported
 * annotations of the entry.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.STABLE, since = "2.0")
public abstract class AbstractAutoProcessingEntry implements AutoProcessingEntry {

  private final String name;
  private final Collection<Class<? extends Annotation>> supportedAnnotations;

  /**
   * Constructs a new abstract processing entry instance.
   *
   * @param name                 the name of this entry.
   * @param supportedAnnotations the annotations supported by this entry.
   */
  @SafeVarargs
  public AbstractAutoProcessingEntry(
    @NotNull String name,
    @NotNull Class<? extends Annotation>... supportedAnnotations
  ) {
    this.name = name;
    this.supportedAnnotations = Arrays.asList(supportedAnnotations);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String name() {
    return this.name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<Class<? extends Annotation>> supportedAnnotations() {
    return this.supportedAnnotations;
  }
}
