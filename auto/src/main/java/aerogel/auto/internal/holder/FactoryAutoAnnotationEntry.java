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

package aerogel.auto.internal.holder;

import static aerogel.auto.internal.utility.ClassLoadingUtils.loadClass;

import aerogel.AerogelException;
import aerogel.BindingConstructor;
import aerogel.Bindings;
import aerogel.auto.AutoAnnotationEntry;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.jetbrains.annotations.NotNull;

/**
 * An auto annotation factory implementation which supports the {@link aerogel.auto.Factory} annotation.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class FactoryAutoAnnotationEntry implements AutoAnnotationEntry {

  private final String methodName;
  private final String enclosingClass;
  private final Set<String> methodArguments;

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
  public FactoryAutoAnnotationEntry(@NotNull ExecutableElement element) {
    this.methodName = element.getSimpleName().toString();
    // we assume that the executable element is a method element in which case the enclosing element is the declaring class
    this.enclosingClass = ((TypeElement) element.getEnclosingElement()).getQualifiedName().toString();
    // ensure that we extract the arguments correctly
    if (element.getParameters().isEmpty()) {
      this.methodArguments = Collections.emptySet(); // save an empty set allocation here
    } else {
      // collects each type parameter with the fully qualified type name for later identification of the method
      this.methodArguments = element.getParameters().stream()
        .map(typeMirror -> typeMirror.asType().toString())
        .collect(Collectors.toSet());
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
    for (String argument : this.methodArguments) {
      out.writeUTF(argument);
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
    String[] typeArguments = new String[in.readInt()];
    // assign each argument
    for (int i = 0; i < typeArguments.length; i++) {
      typeArguments[i] = in.readUTF();
    }
    // try to load every type & find the method
    try {
      // load the declaring class
      Class<?> declaringClass = loadClass(enclosingClass);
      // load the type arguments as classes to a new array
      Class<?>[] types = new Class<?>[typeArguments.length];
      for (int i = 0; i < typeArguments.length; i++) {
        types[i] = loadClass(typeArguments[i]);
      }
      // get the factory method from the declaring class
      Method factoryMethod = declaringClass.getDeclaredMethod(methodName, types);
      // create a constructing holder from that
      return Collections.singleton(Bindings.factory(factoryMethod));
    } catch (Exception exception) {
      throw AerogelException.forMessagedException(String.format(
        "Unable to construct factory binding on %s.%s(%s)",
        enclosingClass,
        methodName,
        String.join(", ", typeArguments)
      ), exception);
    }
  }
}
