/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2022 Pasqual K. and contributors
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

import java.io.DataOutputStream;
import java.io.IOException;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A writer for an entry in an autoconfiguration file. A writer instance is constructed by a processing entry and will
 * be called to write the known data of the entry to the final data stream.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@FunctionalInterface
@API(status = API.Status.STABLE, since = "2.0")
public interface AnnotationEntryWriter {

  /**
   * Emits the data of this binding to the given data stream.
   *
   * @param target the stream to write the known data to.
   * @throws IOException if an i/o exception occurs.
   */
  void emitEntry(@NotNull DataOutputStream target) throws IOException;
}
