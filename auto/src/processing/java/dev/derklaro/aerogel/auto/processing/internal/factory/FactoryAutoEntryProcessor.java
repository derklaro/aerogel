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

package dev.derklaro.aerogel.auto.processing.internal.factory;

import dev.derklaro.aerogel.auto.annotation.Factory;
import dev.derklaro.aerogel.auto.processing.AutoEntryProcessor;
import dev.derklaro.aerogel.auto.processing.internal.util.AutoTypeEncoder;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import org.jetbrains.annotations.NotNull;

final class FactoryAutoEntryProcessor implements AutoEntryProcessor {

  private final AutoTypeEncoder typeEncoder;

  public FactoryAutoEntryProcessor(@NotNull AutoTypeEncoder typeEncoder) {
    this.typeEncoder = typeEncoder;
  }

  @Override
  public @NotNull Class<? extends Annotation> handledAnnotation() {
    return Factory.class;
  }

  @Override
  public void emitEntries(
    @NotNull DataOutput output,
    @NotNull Collection<? extends Element> annotatedElements
  ) throws IOException {
    for (Element annotatedElement : annotatedElements) {
      if (!(annotatedElement instanceof ExecutableElement) || annotatedElement.getKind() != ElementKind.METHOD) {
        throw new IllegalStateException(
          "@Factory is applied to " + annotatedElement.getKind() + " but can only be applied to methods");
      }

      // check if the return type of the method is valid
      ExecutableElement annotatedMethod = (ExecutableElement) annotatedElement;
      if (annotatedMethod.getReturnType().getKind() == TypeKind.VOID) {
        throw new IllegalStateException("@Factory method is returning void: " + annotatedElement);
      }

      // check if the method is static
      if (!annotatedMethod.getModifiers().contains(Modifier.STATIC)) {
        throw new IllegalStateException("@Factory method is not static: " + annotatedElement);
      }

      // write the factory codec id, method name and defining class
      String methodName = annotatedMethod.getSimpleName().toString();
      String definingClassBinaryName = this.typeEncoder.getBinaryName(annotatedMethod.getEnclosingElement().asType());
      output.writeUTF(Factory.CODEC_ID);
      output.writeUTF(methodName);
      output.writeUTF(definingClassBinaryName);

      // write the binary names of the parameter types
      List<? extends VariableElement> parameters = annotatedMethod.getParameters();
      output.writeInt(parameters.size());
      for (VariableElement parameter : parameters) {
        String parameterBinaryName = this.typeEncoder.getBinaryName(parameter.asType());
        output.writeUTF(parameterBinaryName);
      }
    }
  }
}
