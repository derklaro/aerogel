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

package aerogel;

import java.lang.annotation.Annotation;
import org.jetbrains.annotations.NotNull;

/**
 * Allows to compare an annotation from element against this holder. This may be done in different ways. By using...
 *
 * <ul>
 *   <li>... by comparing the type of the annotation.</li>
 *   <li>... an annotation instance to compare the instances against each other.</li>
 *   <li>... any way you can implement ;)</li>
 * </ul>
 *
 * <p>Every {@link Element} holds annotation comparer which can be accessed through {@link Element#annotationComparer()}
 * and added via {@link Element#requireAnnotations(Annotation...)} and {@link Element#requireAnnotations(Class[])}.</p>
 *
 * @author Pasqual K.
 * @since 1.0
 */
@FunctionalInterface
public interface AnnotationComparer {

  /**
   * Checks if comparer holds the same annotation instance as provided to this method.
   *
   * @param annotation the annotation to check.
   * @return if the annotation in this provider and the annotation provided do equal.
   */
  boolean doesEqual(@NotNull Annotation annotation);
}
