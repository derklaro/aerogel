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

package dev.derklaro.aerogel.auto.processing.internal;

import dev.derklaro.aerogel.auto.processing.AutoEntryProcessor;
import dev.derklaro.aerogel.auto.processing.AutoEntryProcessorFactory;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import org.jetbrains.annotations.NotNull;

public final class AutoEntryAnnotationProcessor extends AbstractProcessor {

  // options that are used by this processor
  private static final String OPTION_NAME_OUTPUT_FILE = "aerogelAutoFileName";
  private static final String OPTION_NAME_EMIT_OUTPUT_FILE_IF_EMPTY = "aerogelEmitAutoFileIfEmpty";
  private static final Set<String> SUPPORTED_OPTIONS = new HashSet<String>(2, 1.0f) {
    {
      this.add(OPTION_NAME_OUTPUT_FILE);
      this.add(OPTION_NAME_EMIT_OUTPUT_FILE_IF_EMPTY);
    }
  };

  // supportedAnnotationTypes - the annotation types for which a processor was loaded
  // loadedProcessors - the auto entry processors that were loaded
  private final Set<String> supportedAnnotationTypes = new HashSet<>();
  private final List<AutoEntryProcessor> loadedProcessors = new ArrayList<>();

  // dataHolderStream - holds the data that was actually written
  // dataWriterStream - the target stream for data written during the encoding process
  private final ByteArrayOutputStream dataHolderStream = new ByteArrayOutputStream();
  private final DataOutputStream dataWriterStream = new DataOutputStream(this.dataHolderStream);

  // option values that are retrieved from the processing environment
  // non-null if this processor was initialized (and the option has a default value in case it's not provided)
  private String outputFileName;
  private boolean emitOutputFileIfEmpty;

  @Override
  public synchronized void init(@NotNull ProcessingEnvironment processingEnv) {
    super.init(processingEnv);

    // assign option values
    Map<String, String> options = processingEnv.getOptions();
    this.outputFileName = options.getOrDefault(OPTION_NAME_OUTPUT_FILE, "auto-config.aero");
    this.emitOutputFileIfEmpty = Boolean.parseBoolean(options.get(OPTION_NAME_EMIT_OUTPUT_FILE_IF_EMPTY));

    // load factories for auto entry processors and register those which are constructable
    // ignore all services that cannot be loaded (for example due to a failed precondition)
    ClassLoader cl = AutoEntryAnnotationProcessor.class.getClassLoader();
    ServiceLoader<AutoEntryProcessorFactory> loader = ServiceLoader.load(AutoEntryProcessorFactory.class, cl);
    Iterator<AutoEntryProcessorFactory> services = loader.iterator();
    while (services.hasNext()) {
      try {
        AutoEntryProcessorFactory factory = services.next();
        AutoEntryProcessor processor = factory.constructProcessor(processingEnv);
        String handledAnnotationTypeName = processor.handledAnnotation().getCanonicalName();
        if (this.supportedAnnotationTypes.add(handledAnnotationTypeName)) {
          this.loadedProcessors.add(processor);
          processingEnv.getMessager().printMessage(
            Diagnostic.Kind.NOTE,
            String.format(
              "Loaded aerogel auto processor from factory %s which supports @%s",
              factory.getClass().getName(), handledAnnotationTypeName));
        } else {
          processingEnv.getMessager().printMessage(
            Diagnostic.Kind.MANDATORY_WARNING,
            String.format(
              "Detected duplicate aerogel auto processor for annotation @%s (one coming from factory %s)",
              handledAnnotationTypeName, factory.getClass().getName()));
        }
      } catch (ServiceConfigurationError ignored) {
      }
    }
  }

  @Override
  public boolean process(@NotNull Set<? extends TypeElement> annotations, @NotNull RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      try {
        // processing is done, emit the final output file if needed
        byte[] serializedData = this.dataHolderStream.toByteArray();
        if (serializedData.length > 0 || this.emitOutputFileIfEmpty) {
          Filer filer = this.processingEnv.getFiler();
          JavaFileManager.Location outputLocation = StandardLocation.CLASS_OUTPUT;
          FileObject outputFile = filer.createResource(outputLocation, "", this.outputFileName);
          try (OutputStream outputFileStream = outputFile.openOutputStream()) {
            outputFileStream.write(serializedData);
            outputFileStream.flush();
          }
        }
      } catch (IOException exception) {
        this.processingEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          String.format(
            "Unable to write generated aerogel auto entries into %s: %s",
            this.outputFileName, exception.getMessage()));
      } finally {
        try {
          this.dataWriterStream.close();
          this.dataHolderStream.close();
        } catch (IOException ignored) {
          // silent close of output holders, just ignore errors thrown here
        }
      }
    } else {
      // call all discovered processor with the elements annotated with their handled annotation
      for (AutoEntryProcessor processor : this.loadedProcessors) {
        try {
          Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(processor.handledAnnotation());
          if (!annotatedElements.isEmpty()) {
            processor.emitEntries(this.dataWriterStream, annotatedElements);
          }
        } catch (Exception exception) {
          this.processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            String.format(
              "Caught exception in aerogel auto processor %s: %s",
              processor.getClass().getName(), exception.getMessage()));
        }
      }
    }

    return false;
  }

  @Override
  public @NotNull Set<String> getSupportedAnnotationTypes() {
    // accept all annotation types to be able to emit an empty file if
    // no elements with one of the supported annotations were present
    return Collections.singleton("*");
  }

  @Override
  public @NotNull Set<String> getSupportedOptions() {
    return SUPPORTED_OPTIONS;
  }

  @Override
  public @NotNull SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}
