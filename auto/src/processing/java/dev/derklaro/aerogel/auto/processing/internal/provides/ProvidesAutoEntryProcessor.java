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

package dev.derklaro.aerogel.auto.processing.internal.provides;

import dev.derklaro.aerogel.auto.annotation.Provides;
import dev.derklaro.aerogel.auto.processing.AutoEntryProcessor;
import dev.derklaro.aerogel.auto.processing.internal.util.AutoTypeEncoder;
import dev.derklaro.aerogel.auto.processing.internal.util.AutoTypeEncodingUtil;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.jetbrains.annotations.NotNull;

final class ProvidesAutoEntryProcessor implements AutoEntryProcessor {

  private final AutoTypeEncoder typeEncoder;

  public ProvidesAutoEntryProcessor(@NotNull AutoTypeEncoder typeEncoder) {
    this.typeEncoder = typeEncoder;
  }

  @Override
  public @NotNull Class<? extends Annotation> handledAnnotation() {
    return Provides.class;
  }

  @Override
  public void emitEntries(
    @NotNull DataOutput output,
    @NotNull Collection<? extends Element> annotatedElements
  ) throws IOException {
    for (Element annotatedElement : annotatedElements) {
      if (!(annotatedElement instanceof TypeElement)) {
        throw new IllegalStateException(
          "@Provides is applied to " + annotatedElement.getKind() + " but can only be applied to types");
      }

      // extract the provided types and validate their presence
      Provides annotation = annotatedElement.getAnnotation(Provides.class);
      List<? extends TypeMirror> providedTypes = AutoTypeEncodingUtil.getTypesFromAnnotationProperty(annotation::value);
      if (providedTypes.isEmpty()) {
        throw new IllegalStateException("Provided types in @Provides annotation is empty on " + annotatedElement);
      }

      // write the provides codec id and binary name of the annotated type (implementation)
      String annotatedTypeBinaryName = this.typeEncoder.getBinaryName(annotatedElement.asType());
      output.writeUTF(Provides.CODEC_ID);
      output.writeUTF(annotatedTypeBinaryName);

      // write the amount of types that are implemented by the annotated type and the binary names of them
      output.writeInt(providedTypes.size());
      for (TypeMirror providedType : providedTypes) {
        String providedTypeBinaryName = this.typeEncoder.getBinaryName(providedType);
        output.writeUTF(providedTypeBinaryName);
      }
    }
  }
}
