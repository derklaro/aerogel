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

package dev.derklaro.aerogel.auto.processing;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import javax.lang.model.element.Element;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A processor for a specific auto annotation. When, during an annotation processing round, an element with the
 * supported annotation of this processor is found, this processor is invoked. It can, based on the given elements, emit
 * information into the provided data output which can be used for deserialization in runtime.
 *
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public interface AutoEntryProcessor {

  /**
   * Get the annotation type that is handled by this auto entry processor.
   *
   * @return the annotation type that is handled by this auto entry processor.
   */
  @NotNull
  Class<? extends Annotation> handledAnnotation();

  /**
   * Called if elements annotated with the handled annotations are found in a processing round. Based on the elements
   * the processor can write none, one or multiple entries into the given data output which will be available in runtime
   * for deserialization.
   *
   * @param output            the output to write serialized data about the annotated elements into.
   * @param annotatedElements the elements that are annotated with the handled annotation.
   * @throws IOException if an I/O error occurs while serializing the information into the data output.
   */
  void emitEntries(
    @NotNull DataOutput output,
    @NotNull Collection<? extends Element> annotatedElements
  ) throws IOException;
}
