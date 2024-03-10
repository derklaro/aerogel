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

package dev.derklaro.aerogel.auto.internal.processing;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.auto.processing.AnnotationEntryWriter;
import dev.derklaro.aerogel.auto.processing.AutoProcessingEntry;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import org.apiguardian.api.API;

/**
 * An annotation processor which will collect and emit all auto annotation entries into a file. New annotation entries
 * can be registered by using the service loader api.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class AutoInjectAnnotationProcessor extends AbstractProcessor {

  // output file config
  private static final String OPTION_OUTPUT_FILE_NAME = "aerogelAutoFileName";
  private static final JavaFileManager.Location OUTPUT_FILE_LOCATION = StandardLocation.CLASS_OUTPUT;

  // the supported annotations of this processor
  private final Set<AutoProcessingEntry> processingEntries = new HashSet<>();
  // all the entries this processor has found so far
  private final Map<String, Set<AnnotationEntryWriter>> foundEntries = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    // we can load the supported entries here, this method is called before any other method in this class
    ServiceLoader<AutoProcessingEntry> processingLoader = ServiceLoader.load(
      AutoProcessingEntry.class,
      this.getClass().getClassLoader());
    processingLoader.forEach(this.processingEntries::add);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SourceVersion getSupportedSourceVersion() {
    // allows access to newer language level features than 6 & does not emit a warning during the compile process
    return SourceVersion.latestSupported();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getSupportedOptions() {
    return new HashSet<>(Collections.singleton(OPTION_OUTPUT_FILE_NAME));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return this.processingEntries.stream()
      .flatMap(entry -> entry.supportedAnnotations().stream())
      .map(Class::getCanonicalName)
      .collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // check if the processing round is over
    if (roundEnv.processingOver()) {
      // ensure that there is data we need to emit
      if (!this.foundEntries.isEmpty()) {
        // the round is over - dump the current result and clear the cache
        try {
          // get the output file name & create the final file
          String fileName = this.processingEnv.getOptions().getOrDefault(OPTION_OUTPUT_FILE_NAME, "auto-config.aero");
          FileObject file = this.processingEnv.getFiler().createResource(OUTPUT_FILE_LOCATION, "", fileName);

          // write the factory data
          try (DataOutputStream out = new DataOutputStream(file.openOutputStream())) {
            for (Map.Entry<String, Set<AnnotationEntryWriter>> entry : this.foundEntries.entrySet()) {
              for (AnnotationEntryWriter writer : entry.getValue()) {
                // write the name of each entry, then let the writer emit its data to the stream
                out.writeUTF(entry.getKey());
                writer.emitEntry(out);
              }
            }
          }
        } catch (IOException exception) {
          throw AerogelException.forMessagedException("Exception writing data to output file", exception);
        }
      }

      // never claim annotation so that other processors can visit them as well
      return false;
    }

    // call each registered entry
    for (AutoProcessingEntry processingEntry : this.processingEntries) {
      // collect all elements which are annotated with the annotations supported by the entry
      Collection<? extends Element> elements = processingEntry.supportedAnnotations().stream()
        .flatMap(annotation -> roundEnv.getElementsAnnotatedWith(annotation).stream())
        .collect(Collectors.toSet());

      // let the entry construct all writers & register them if there are any
      Collection<AnnotationEntryWriter> writers = processingEntry.parseElements(elements, this.processingEnv);
      if (!writers.isEmpty()) {
        this.foundEntries.computeIfAbsent(processingEntry.name(), $ -> new LinkedHashSet<>()).addAll(writers);
      }
    }

    // never claim annotation so that other processors can visit them as well
    return false;
  }
}
