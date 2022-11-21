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

import static dev.derklaro.aerogel.auto.internal.utility.ClassLoadingUtil.loadClass;

import dev.derklaro.aerogel.AerogelException;
import dev.derklaro.aerogel.BindingConstructor;
import dev.derklaro.aerogel.Bindings;
import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.auto.AutoAnnotationEntry;
import dev.derklaro.aerogel.auto.Provides;
import dev.derklaro.aerogel.internal.utility.ElementHelper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;

/**
 * An auto annotation factory implementation which supports the {@link Provides} annotation.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0", consumers = "dev.derklaro.aerogel.auto.internal")
public final class ProvidesAutoAnnotationEntry implements AutoAnnotationEntry {

  private final String bindingName;
  private final Set<String> bindings;

  /**
   * Constructs an empty entry. Used for deserialization.
   */
  public ProvidesAutoAnnotationEntry() {
    this.bindingName = null;
    this.bindings = null;
  }

  /**
   * Constructs a new entry.
   *
   * @param element  the element which was annotated.
   * @param provides the annotation.
   */
  public ProvidesAutoAnnotationEntry(@NotNull TypeElement element, @NotNull List<? extends TypeMirror> provides) {
    this.bindingName = element.getQualifiedName().toString();
    // we assume that provides has always at least one provided class
    this.bindings = provides.stream().map(TypeMirror::toString).collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Set<BindingConstructor> makeBinding(@NotNull DataInputStream in) throws IOException {
    try {
      // the class to which all provided classes should get bound
      Element type = Element.forType(loadClass(in.readUTF()));

      // load the classes to which the binding provides
      Element[] providedElements = new Element[in.readInt()];
      for (int i = 0; i < providedElements.length; i++) {
        Class<?> providedClass = loadClass(in.readUTF());
        providedElements[i] = ElementHelper.buildElement(providedClass);
      }

      // make a constructor for each provided class
      BindingConstructor constructor = Bindings.constructing(type, providedElements);
      return Collections.singleton(constructor);
    } catch (ClassNotFoundException exception) {
      throw AerogelException.forMessagedException("Unable to provide bindings constructors", exception);
    }
  }
}
