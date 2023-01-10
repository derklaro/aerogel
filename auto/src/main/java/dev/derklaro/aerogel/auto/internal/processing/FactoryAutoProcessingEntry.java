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

import dev.derklaro.aerogel.auto.Factory;
import dev.derklaro.aerogel.auto.internal.util.TypeUtil;
import dev.derklaro.aerogel.auto.processing.AbstractAutoProcessingEntry;
import dev.derklaro.aerogel.auto.processing.AnnotationEntryWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * A processing entry which emits data of factory methods.
 *
 * @author Pasqual K.
 * @since 2.0
 */
@API(status = API.Status.INTERNAL, since = "2.0")
public final class FactoryAutoProcessingEntry extends AbstractAutoProcessingEntry {

  /**
   * Constructs a new factory auto processing entry instance.
   */
  public FactoryAutoProcessingEntry() {
    super("factory", Factory.class);
  }

  /**
   * Parses the argument types which can be loaded in the runtime of the given executable element.
   *
   * @param env     the current processing environment.
   * @param element the element to get the argument types of.
   * @return an ordered list of type names of the arguments of the given executable element.
   */
  private static @NotNull List<String> parseMethodArguments(
    @NotNull ProcessingEnvironment env,
    @NotNull ExecutableElement element
  ) {
    if (element.getParameters().isEmpty()) {
      return Collections.emptyList(); // save an empty set allocation here
    } else {
      // collects each type parameter with the fully qualified type name for later identification of the method
      return element.getParameters().stream()
        .map(variableElement -> {
          // erase the generic data from the element type
          TypeMirror erasedType = env.getTypeUtils().erasure(variableElement.asType());

          // if the type of the parameter is an array we need to fetch the info specifically for that
          if (erasedType.getKind() == TypeKind.ARRAY) {
            Map.Entry<TypeMirror, Integer> arrayTypeInfo = TypeUtil.innermostComponentType((ArrayType) erasedType);

            // based on the array type info we can get the element of the component type
            String runtimeComponentType = TypeUtil.asRuntimeType(
              arrayTypeInfo.getKey(),
              env.getTypeUtils(),
              env.getElementUtils());
            String array = String.join("", Collections.nCopies(arrayTypeInfo.getValue(), TypeUtil.ARRAY_INDICATOR));

            // return the runtime type based on the component type and array depth
            return String.format("%s%s", runtimeComponentType, array);
          } else {
            // we can just return the type information as a runtime type info
            return TypeUtil.asRuntimeType(erasedType, env.getTypeUtils(), env.getElementUtils());
          }
        })
        .collect(Collectors.toCollection(LinkedList::new));
    }
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
      // ensure that the element is a method - print a warning if this is not the case
      if (element.getKind() != ElementKind.METHOD) {
        processingEnvironment.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format(
            "Element of kind %s is annotated as %s but only methods are allowed to be annotated",
            element.getKind(),
            Factory.class.getCanonicalName()));
        continue;
      }

      // ensure that the method is static
      if (!element.getModifiers().contains(Modifier.STATIC)) {
        processingEnvironment.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Factory method %s must be static", element.getSimpleName()));
        continue;
      }

      // ensure that the method has an actual return type
      ExecutableElement executableElement = (ExecutableElement) element;
      if (executableElement.getReturnType().getKind() == TypeKind.VOID) {
        processingEnvironment.getMessager().printMessage(
          Diagnostic.Kind.MANDATORY_WARNING,
          String.format("Factory method %s returns void but an actual type is expected", element.getSimpleName()));
        continue;
      }

      // get the basic information about the factory method
      String methodName = element.getSimpleName().toString();
      List<String> methodArguments = parseMethodArguments(processingEnvironment, executableElement);
      String enclosingClass = TypeUtil.getBinaryName(
        processingEnvironment.getElementUtils(),
        element.getEnclosingElement()
      ).toString();

      // register a writer for the factory method
      writers.add(new FactoryAnnotationEntryWriter(methodName, enclosingClass, methodArguments));
    }
    return writers;
  }

  /**
   * A writer which emits annotation data of factory methods.
   *
   * @author Pasqual K.
   * @since 2.0
   */
  @API(status = API.Status.INTERNAL, since = "2.0")
  private static final class FactoryAnnotationEntryWriter implements AnnotationEntryWriter {

    private final String methodName;
    private final String enclosingClass;
    private final List<String> methodArguments;

    /**
     * Constructs a new factory annotation writer instance.
     *
     * @param methodName      the name of the factory method.
     * @param enclosingClass  the class which declares the factory method.
     * @param methodArguments the argument types that the factory method takes.
     */
    public FactoryAnnotationEntryWriter(
      @NotNull String methodName,
      @NotNull String enclosingClass,
      @NotNull List<String> methodArguments
    ) {
      this.methodName = methodName;
      this.enclosingClass = enclosingClass;
      this.methodArguments = methodArguments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void emitEntry(@NotNull DataOutputStream target) throws IOException {
      target.writeShort(1); // the data version of the content
      target.writeUTF(this.methodName); // the method - the top most thing we need to know
      target.writeUTF(this.enclosingClass); // the class in which the method is located
      target.writeInt(this.methodArguments.size()); // the size of the following argument array
      // write every type of each variable for later method identification
      for (String type : this.methodArguments) {
        target.writeUTF(type);
      }
    }
  }
}
