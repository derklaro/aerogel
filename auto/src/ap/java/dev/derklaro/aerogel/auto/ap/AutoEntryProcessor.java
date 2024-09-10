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

package dev.derklaro.aerogel.auto.ap;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.lang.model.SourceVersion;
import org.jetbrains.annotations.NotNull;

abstract class AutoEntryProcessor extends AbstractProcessor {

  protected static final String FILE_NAME_OPTION = "aerogelAutoFileName";

  private final Set<String> supportedAnnotations;
  private final Set<String> supportedOptions;

  public AutoEntryProcessor(@NotNull Class<? extends Annotation> processedAnnotation) {
    this.supportedAnnotations = Collections.singleton(processedAnnotation.getCanonicalName());
    this.supportedOptions = Collections.singleton(FILE_NAME_OPTION);
  }

  @Override
  public @NotNull SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public @NotNull Set<String> getSupportedAnnotationTypes() {
    return this.supportedAnnotations;
  }

  @Override
  public @NotNull Set<String> getSupportedOptions() {
    return this.supportedOptions;
  }

  protected @NotNull Optional<String> optionValue(@NotNull String optionKey) {
    String optionValue = this.processingEnv.getOptions().get(optionKey);
    return Optional.ofNullable(optionValue);
  }
}
