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
import aerogel.Element;
import aerogel.auto.AutoAnnotationEntry;
import aerogel.auto.Provides;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.TypeElement;
import org.jetbrains.annotations.NotNull;

public final class ProvidesAutoAnnotationEntry implements AutoAnnotationEntry {

  private final String bindingName;
  private final Set<String> bindings;

  public ProvidesAutoAnnotationEntry() {
    this.bindingName = null;
    this.bindings = null;
  }

  public ProvidesAutoAnnotationEntry(@NotNull TypeElement element, @NotNull Provides provides) {
    this.bindingName = element.getQualifiedName().toString();
    // we assume that provides has always at least one provided class
    this.bindings = Arrays.stream(provides.value()).map(Class::getName).collect(Collectors.toSet());
  }

  @Override
  public void emit(@NotNull DataOutputStream out) throws IOException {
    out.writeUTF("provides"); // the processor which is responsible for the binding construction
    out.writeUTF(this.bindingName); // the class all provided classes should get bound to
    out.writeInt(this.bindings.size()); // the size of the bindings array
    // emit all bindings to the stream
    for (String binding : this.bindings) {
      out.writeUTF(binding);
    }
  }

  @Override
  public @NotNull Set<BindingConstructor> makeBinding(@NotNull DataInputStream in) throws IOException {
    try {
      Element type = Element.get(loadClass(in.readUTF())); // the class to which all provided classes should get bound
      // load the classes to which the binding provides
      Class<?>[] providedClasses = new Class<?>[in.readInt()];
      for (int i = 0; i < providedClasses.length; i++) {
        providedClasses[i] = loadClass(in.readUTF());
      }
      // make a constructor for each provided class
      return Arrays.stream(providedClasses)
        .map(providedClass -> Bindings.constructing(type, Element.get(providedClass)))
        .collect(Collectors.toSet());
    } catch (ClassNotFoundException exception) {
      throw AerogelException.forMessagedException("Unable to provide bindings constructors", exception);
    }
  }
}
