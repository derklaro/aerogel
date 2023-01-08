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

import static dev.derklaro.aerogel.auto.internal.utility.TypeUtil.typesOfAnnotationValue;

import dev.derklaro.aerogel.auto.Provides;
import dev.derklaro.aerogel.auto.internal.utility.TypeUtil;
import dev.derklaro.aerogel.auto.processing.AbstractAutoProcessingEntry;
import dev.derklaro.aerogel.auto.processing.AnnotationEntryWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A processing entry which emits data of provides annotations.
 *
 * @author Pasqual K.
 * @since 2.0
 */
public final class ProvidesAutoProcessingEntry extends AbstractAutoProcessingEntry {

  /**
   * Constructs a new factory auto processing entry instance.
   */
  public ProvidesAutoProcessingEntry() {
    super("provides", Provides.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Collection<AnnotationEntryWriter> parseElements(
    @NotNull Collection<? extends Element> annotatedElements,
    @NotNull ProcessingEnvironment processingEnvironment
  ) {
    Collection<AnnotationEntryWriter> writers = new LinkedList<>();
    for (Element element : annotatedElements) {
      // ensure that the element is a class - print a warning if this is not the case
      if (element.getKind() != ElementKind.CLASS) {
        processingEnvironment.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format(
            "Element of kind %s is annotated as %s but only classes are allowed to be annotated",
            element.getKind(),
            Provides.class.getCanonicalName()));
        continue;
      }

      // ensure that the class is not abstract
      if (element.getModifiers().contains(Modifier.ABSTRACT)) {
        processingEnvironment.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Binding class %s must not be abstract", element.getSimpleName()));
        continue;
      }

      // get the type mirrors the annotation is providing
      //noinspection ResultOfMethodCallIgnored
      List<? extends TypeMirror> value = typesOfAnnotationValue(() -> element.getAnnotation(Provides.class).value());
      // ensure that the annotation is actually providing something
      if (value.isEmpty()) {
        processingEnvironment.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Providing class %s provides nothing", element.getSimpleName()));
        continue;
      }

      // we can cast to a type element now
      TypeElement typeElement = (TypeElement) element;

      // get the information about the binding class and bound types
      String bindingName = TypeUtil.getBinaryName(processingEnvironment.getElementUtils(), typeElement).toString();
      Set<String> bindings = value.stream()
        .map(typeMirror -> TypeUtil.asRuntimeType(
          typeMirror,
          processingEnvironment.getTypeUtils(),
          processingEnvironment.getElementUtils()))
        .collect(Collectors.toSet());

      // register the writer for the annotation
      writers.add(new ProvidesAnnotationEntryWriter(bindingName, bindings));
    }
    return writers;
  }

  /**
   * A writer which emits annotation data of provides annotations.
   *
   * @author Pasqual K.
   * @since 2.0
   */
  @API(status = API.Status.INTERNAL, since = "2.0")
  private static final class ProvidesAnnotationEntryWriter implements AnnotationEntryWriter {

    private final String bindingName;
    private final Set<String> bindings;

    /**
     * Constructs a new provides annotation writer instance.
     *
     * @param bindingName the name of the class to bind to.
     * @param bindings    the bindings which should get bound to the given binding class.
     */
    public ProvidesAnnotationEntryWriter(@NotNull String bindingName, @NotNull Set<String> bindings) {
      this.bindingName = bindingName;
      this.bindings = bindings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void emitEntry(@NotNull DataOutputStream target) throws IOException {
      target.writeShort(1); // the data version of the content
      target.writeUTF(this.bindingName); // the class all provided classes should get bound to
      target.writeInt(this.bindings.size()); // the size of the bindings array
      // emit all bindings to the stream
      for (String binding : this.bindings) {
        target.writeUTF(binding);
      }
    }
  }
}
