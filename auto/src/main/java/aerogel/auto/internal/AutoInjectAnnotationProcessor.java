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

package aerogel.auto.internal;

import aerogel.auto.Factory;
import aerogel.auto.Provides;
import aerogel.auto.internal.holder.FactoryProcessedAnnotation;
import aerogel.auto.internal.holder.ProcessedAnnotation;
import aerogel.auto.internal.holder.ProvidesProcessedAnnotation;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
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
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;
import org.jetbrains.annotations.NotNull;

final class AutoInjectAnnotationProcessor extends AbstractProcessor {

  // represents the open options used to emit the data of a successful processing round
  private static final OpenOption[] OO = new OpenOption[]{
    StandardOpenOption.APPEND,
    StandardOpenOption.CREATE,
    StandardOpenOption.WRITE};

  // all the entries this processor has found so far
  private final Set<ProcessedAnnotation> foundEntries = new HashSet<>();
  // the uri of the file we want
  private Path targetFile;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    // super to initialize the used 'processingEnv' field
    super.init(processingEnv);
    // open the target file object we want to write to
    try {
      // a little hacky to do it this way, but we can't access it in another good way (at least if found none)
      this.targetFile = Paths.get(processingEnv.getFiler().createResource(
        StandardLocation.CLASS_OUTPUT,
        "",
        "auto-factories.txt"
      ).toUri());
      // create a new file at the location if the file was not already created
      if (Files.notExists(this.targetFile)) {
        Files.createFile(this.targetFile);
      }
    } catch (IOException exception) {
      throw new IllegalStateException("Exception opening target class to write entries", exception);
    }
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    // allows access to newer language level features than 6 & does not emit a warning during the compile process
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // check if the processing round is over
    if (roundEnv.processingOver()) {
      // ensure that there is data we need to emit
      if (!this.foundEntries.isEmpty()) {
        // the round is over - dump the current result and clear the cache
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(this.targetFile, OO))) {
          // emit every entry to the data output
          for (ProcessedAnnotation entry : this.foundEntries) {
            entry.emit(out);
          }
        } catch (IOException exception) {
          throw new RuntimeException("Exception opening output file " + this.targetFile, exception);
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

  private void collectFactories(@NotNull ProcessingEnvironment env, @NotNull RoundEnvironment roundEnv) {
    // get all members annotated as @Factory
    for (Element element : roundEnv.getElementsAnnotatedWith(Factory.class)) {
      // ensure that the element is a method - print a warning if this is not the case
      if (element.getKind() != ElementKind.METHOD) {
        env.getMessager().printMessage(
          Kind.MANDATORY_WARNING,
          String.format(
            "Element of kind %s is annotated as %s but only methods are allowed to be annotated",
            element.getKind(),
            Factory.class.getCanonicalName()));
        continue;
      }
      // ensure that the method is static
      if (!element.getModifiers().contains(Modifier.STATIC)) {
        env.getMessager().printMessage(
          Kind.MANDATORY_WARNING,
          String.format("Factory method %s must be static", element.getSimpleName()));
        continue;
      }
      // ensure that the method has an actual return type
      ExecutableElement executableElement = (ExecutableElement) element;
      if (executableElement.getReturnType().getKind() == TypeKind.VOID) {
        env.getMessager().printMessage(
          Kind.MANDATORY_WARNING,
          String.format("Factory method %s returns void but an actual type is expected", element.getSimpleName()));
        continue;
      }
      // valid method - emit that
      this.foundEntries.add(new FactoryProcessedAnnotation(executableElement));
    }
  }

  private void collectProvides(@NotNull ProcessingEnvironment env, @NotNull RoundEnvironment roundEnv) {
    // get all members annotated as @Provides
    for (Element element : roundEnv.getElementsAnnotatedWith(Provides.class)) {
      // ensure that the element is a class - print a warning if this is not the case
      if (element.getKind() != ElementKind.CLASS) {
        env.getMessager().printMessage(
          Kind.MANDATORY_WARNING,
          String.format(
            "Element of kind %s is annotated as %s but only classes are allowed to be annotated",
            element.getKind(),
            Provides.class.getCanonicalName()));
        continue;
      }
      // ensure that the class is not abstract
      if (element.getModifiers().contains(Modifier.ABSTRACT)) {
        env.getMessager().printMessage(
          Kind.MANDATORY_WARNING,
          String.format("Binding class %s must not be abstract", element.getSimpleName()));
        continue;
      }
      // get the provides annotation from the class
      Provides provides = element.getAnnotation(Provides.class);
      // ensure that the annotation is actually providing something
      if (provides.value().length == 0) {
        env.getMessager().printMessage(
          Kind.MANDATORY_WARNING,
          String.format("Providing class %s provides nothing", element.getSimpleName()));
        continue;
      }
      // valid providing class - emit that
      this.foundEntries.add(new ProvidesProcessedAnnotation((TypeElement) element, provides));
    }
  }
}
