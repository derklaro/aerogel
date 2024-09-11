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

package dev.derklaro.aerogel.auto.processing.internal.util;

import java.util.Collections;
import java.util.List;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import org.jetbrains.annotations.NotNull;

public final class AutoTypeEncodingUtil {

  private AutoTypeEncodingUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull List<? extends TypeMirror> getTypesFromAnnotationProperty(@NotNull Runnable extractor) {
    try {
      extractor.run();
      throw new IllegalStateException("Extractor did not access type(s) from annotation property");
    } catch (MirroredTypeException exception) {
      // single type was mirrored (Class)
      return Collections.singletonList(exception.getTypeMirror());
    } catch (MirroredTypesException exception) {
      // multiple types were mirrored (Class[])
      return exception.getTypeMirrors();
    }
  }
}
