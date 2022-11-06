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

package dev.derklaro.aerogel.auto.internal.holder;

import static dev.derklaro.aerogel.auto.internal.utility.ClassLoadingUtils.loadClass;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.BindingConstructor;
import dev.derklaro.aerogel.Bindings;
import dev.derklaro.aerogel.auto.AutoAnnotationEntry;
import dev.derklaro.aerogel.auto.Factory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.jetbrains.annotations.NotNull;

/**
 * An auto annotation factory implementation which supports the {@link Factory} annotation.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class FactoryAutoAnnotationEntry implements AutoAnnotationEntry {

  // the flags which are indicating the type of entry
  private static final int FLAG_ARRAY = 0x01;

  private final String methodName;
  private final String enclosingClass;
  private final List<Map.Entry<String, Integer>> methodArguments;

  /**
   * Constructs a new empty factory used for deserialization.
   */
  public FactoryAutoAnnotationEntry() {
    this.methodName = null;
    this.enclosingClass = null;
    this.methodArguments = null;
  }

  /**
   * Constructs a new element.
   *
   * @param element the element which was annotated.
   */
  public FactoryAutoAnnotationEntry(@NotNull ExecutableElement element, @NotNull Types types) {
    this.methodName = element.getSimpleName().toString();
    // we assume that the executable element is a method element in which case the enclosing element is the declaring class
    this.enclosingClass = ((TypeElement) element.getEnclosingElement()).getQualifiedName().toString();
    // ensure that we extract the arguments correctly
    if (element.getParameters().isEmpty()) {
      this.methodArguments = Collections.emptyList(); // save an empty set allocation here
    } else {
      // collects each type parameter with the fully qualified type name for later identification of the method
      this.methodArguments = element.getParameters().stream()
        .map(variableElement -> {
          int flags = 0;

          // extract the component type of the element
          TypeMirror variableType = variableElement.asType();
          if (variableType instanceof ArrayType) {
            flags |= FLAG_ARRAY;
            variableType = ((ArrayType) variableType).getComponentType();
          }

          // erase the unneeded type information & box the mirror in a type
          TypeMirror erasedType = types.erasure(variableType);
          Element typeElement = types.asElement(erasedType);

          // if the type element is present we can use that element as the parameter type
          if (typeElement != null) {
            return new SimpleImmutableEntry<>(typeElement.toString(), flags);
          }

          try {
            // the element might still be primitive, try that
            PrimitiveType primitive = types.getPrimitiveType(erasedType.getKind());
            return new SimpleImmutableEntry<>(primitive.toString(), flags);
          } catch (IllegalArgumentException ex) {
            // not primitive
            throw AerogelException.forMessageWithoutStack(String.format(
              "TypeMirror %s has no corresponding element and is not primitive " + (variableElement.asType()
                .getClass()),
              erasedType));
          }
        })
        .collect(Collectors.toCollection(LinkedList::new));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void emit(@NotNull DataOutputStream out) throws IOException {
    out.writeUTF("factory"); // the processor which is responsible for the binding construction
    out.writeUTF(this.methodName); // the method - the top most thing we need to know
    out.writeUTF(this.enclosingClass); // the class in which the method is located
    out.writeInt(this.methodArguments.size()); // the size of the following argument array
    // write every type of each variable for later method identification
    for (Map.Entry<String, Integer> argument : this.methodArguments) {
      out.writeUTF(argument.getKey());
      out.writeInt(argument.getValue());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Set<BindingConstructor> makeBinding(@NotNull DataInputStream in) throws IOException {
    String methodName = in.readUTF(); // the name of the factory method
    String enclosingClass = in.readUTF(); // the class which declares the method
    // read the method argument types as an array from the stream
    //noinspection unchecked
    Map.Entry<String, Integer>[] typeArguments = new Map.Entry[in.readInt()];
    // assign each argument
    for (int i = 0; i < typeArguments.length; i++) {
      typeArguments[i] = new SimpleImmutableEntry<>(in.readUTF(), in.readInt());
    }

    // try to load every type & find the method
    try {
      // load the declaring class
      Class<?> declaringClass = loadClass(enclosingClass);
      // load the type arguments as classes to a new array
      Class<?>[] types = new Class<?>[typeArguments.length];
      for (int i = 0; i < typeArguments.length; i++) {
        Map.Entry<String, Integer> entry = typeArguments[i];

        // load the component type of the entry
        Class<?> componentType = loadClass(entry.getKey());
        if ((entry.getValue() & FLAG_ARRAY) != 0) {
          // the type is an array
          Object array = Array.newInstance(componentType, 0);
          componentType = array.getClass();
        }

        // register the argument
        types[i] = componentType;
      }

      // get the factory method from the declaring class & create a constructing holder from that
      Method factoryMethod = declaringClass.getDeclaredMethod(methodName, types);
      return Collections.singleton(Bindings.factory(factoryMethod));
    } catch (Exception exception) {
      throw AerogelException.forMessagedException(String.format(
        "Unable to construct factory binding on %s.%s(%s)",
        enclosingClass,
        methodName,
        Arrays.stream(typeArguments)
          .map(entry -> String.format(
            "%s%s",
            entry.getKey(),
            entry.getValue() != 0 ? String.format(" (0x%02X)", 0xFF & entry.getValue()) : ""))
          .collect(Collectors.joining(", "))
      ), exception);
    }
  }
}
