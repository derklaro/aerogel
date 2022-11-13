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

package dev.derklaro.aerogel.auto.internal.processor;

import static dev.derklaro.aerogel.auto.internal.utility.TypeUtil.typesOfAnnotationValue;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.auto.AutoAnnotationEntry;
import dev.derklaro.aerogel.auto.Factory;
import dev.derklaro.aerogel.auto.Provides;
import dev.derklaro.aerogel.auto.internal.holder.FactoryAutoAnnotationEntry;
import dev.derklaro.aerogel.auto.internal.holder.ProvidesAutoAnnotationEntry;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import org.jetbrains.annotations.NotNull;

/**
 * An annotation processor which will collect and emit all {@link Provides} and {@link Factory} elements to a file in
 * the class output directory called {@code auto-factories.txt}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class AutoInjectAnnotationProcessor extends AbstractProcessor {

  // the supported annotations of this processor
  private static final Set<String> SUPPORTED_ANNOTATIONS = Stream.of(Factory.class, Provides.class)
    .map(Class::getCanonicalName)
    .collect(Collectors.toSet());

  // output file config
  private static final String OPTION_OUTPUT_FILE_NAME = "aerogelAutoFileName";
  private static final JavaFileManager.Location OUTPUT_FILE_LOCATION = StandardLocation.CLASS_OUTPUT;

  // all the entries this processor has found so far
  private final Set<AutoAnnotationEntry> foundEntries = new HashSet<>();

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
    return SUPPORTED_ANNOTATIONS;
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
            for (AutoAnnotationEntry entry : this.foundEntries) {
              entry.emit(out);
            }
            // not necessary normal, just to be sure
            this.foundEntries.clear();
          }
        } catch (IOException exception) {
          throw AerogelException.forMessagedException("Exception writing data to output file", exception);
        }
      }
      // never claim annotation so that other processors can visit them as well
      return false;
    }

    // walk over each factory annotation
    this.collectFactories(this.processingEnv, roundEnv);
    // walk over each provides annotation
    this.collectProvides(this.processingEnv, roundEnv);

    // never claim annotation so that other processors can visit them as well
    return false;
  }

  /**
   * Collects all elements which are annotated as {@link Factory}.
   *
   * @param env      the current processing environment.
   * @param roundEnv the current round processing environment.
   */
  private void collectFactories(@NotNull ProcessingEnvironment env, @NotNull RoundEnvironment roundEnv) {
    // get all members annotated as @Factory
    for (Element element : roundEnv.getElementsAnnotatedWith(Factory.class)) {
      // ensure that the element is a method - print a warning if this is not the case
      if (element.getKind() != ElementKind.METHOD) {
        env.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format(
            "Element of kind %s is annotated as %s but only methods are allowed to be annotated",
            element.getKind(),
            Factory.class.getCanonicalName()));
        continue;
      }
      // ensure that the method is static
      if (!element.getModifiers().contains(Modifier.STATIC)) {
        env.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Factory method %s must be static", element.getSimpleName()));
        continue;
      }
      // ensure that the method has an actual return type
      ExecutableElement executableElement = (ExecutableElement) element;
      if (executableElement.getReturnType().getKind() == TypeKind.VOID) {
        env.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Factory method %s returns void but an actual type is expected", element.getSimpleName()));
        continue;
      }
      // valid method - emit that
      this.foundEntries.add(new FactoryAutoAnnotationEntry(executableElement, this.processingEnv.getTypeUtils()));
    }
  }

  /**
   * Collects all elements which are annotated as {@link Provides}.
   *
   * @param env      the current processing environment.
   * @param roundEnv the current round processing environment.
   */
  private void collectProvides(@NotNull ProcessingEnvironment env, @NotNull RoundEnvironment roundEnv) {
    // get all members annotated as @Provides
    for (Element element : roundEnv.getElementsAnnotatedWith(Provides.class)) {
      // ensure that the element is a class - print a warning if this is not the case
      if (element.getKind() != ElementKind.CLASS) {
        env.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format(
            "Element of kind %s is annotated as %s but only classes are allowed to be annotated",
            element.getKind(),
            Provides.class.getCanonicalName()));
        continue;
      }
      // ensure that the class is not abstract
      if (element.getModifiers().contains(Modifier.ABSTRACT)) {
        env.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Binding class %s must not be abstract", element.getSimpleName()));
        continue;
      }
      // get the type mirrors the annotation is providing
      //noinspection ResultOfMethodCallIgnored
      List<? extends TypeMirror> value = typesOfAnnotationValue(() -> element.getAnnotation(Provides.class).value());
      // ensure that the annotation is actually providing something
      if (value.isEmpty()) {
        env.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Providing class %s provides nothing", element.getSimpleName()));
        continue;
      }
      // valid providing class - emit that
      this.foundEntries.add(new ProvidesAutoAnnotationEntry((TypeElement) element, value));
    }
  }
}
