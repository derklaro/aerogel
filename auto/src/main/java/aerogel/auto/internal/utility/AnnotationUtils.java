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

package aerogel.auto.internal.utility;

import java.util.List;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class for working compile time with annotations
 *
 * @author Pasqual K.
 * @since 1.0.1
 */
public final class AnnotationUtils {

  private AnnotationUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * A tiny hack which simplifies the value reading of an annotation during compile time. The processor is not allowed
   * to read values of an annotation directly (which is currently used to read the value of the {@link
   * aerogel.auto.Provides} annotation) which will cause an {@code MirroredTypesException}. In the exception the
   * compiler will then give us access to the mirrored types of the annotation value. There are some other ways around
   * there which are very long and complicated but don't need the exception throwing, but in my opinion this is the best
   * and cleanest way to get around the problem. <a href="https://stackoverflow.com/a/7688029" target="_blank">Thanks
   * Ralph!</a>.
   *
   * @param getter a runnable which gets called and the thrown exception gets catched.
   * @return the type mirrors of the annotation value.
   * @throws RuntimeException if the method is not used in the intended way.
   */
  public static @NotNull List<? extends TypeMirror> typesOfAnnotationValue(@NotNull Runnable getter) {
    try {
      getter.run();
      throw new RuntimeException("what?"); // cannot happen - just explode
    } catch (MirroredTypesException exception) {
      return exception.getTypeMirrors();
    }
  }
}
