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

package dev.derklaro.aerogel;

import dev.derklaro.aerogel.binding.InstalledBinding;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.key.BindingKey;
import dev.derklaro.aerogel.internal.context.InjectionContext;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextScope;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThreadLocalContextKeptInScopeTest {

  @SuppressWarnings("unchecked")
  private static @Nullable <T> T resolveInstanceScoped(@NotNull InjectionContextScope scope) {
    // TODO: move this to some utility? might be useful for external users as well
    InjectionContext context = scope.context();
    return scope.executeScoped(() -> {
      try {
        return (T) context.resolveInstance();
      } finally {
        if (context.root()) {
          context.finishConstruction();
        }
      }
    });
  }

  @Test
  void contextKeptDuringScopedOperations() {
    // add a global binding for "@Named("test") String"
    Injector injector = Injector.newInjector();
    UninstalledBinding<String> worldStringBinding = injector.createBindingBuilder()
      .bind(String.class)
      .qualifiedWithName("test")
      .toInstance("Hello World!");
    injector.installBinding(worldStringBinding);

    // with override
    {
      InstalledBinding<SomeClass> someClassBinding = injector.binding(BindingKey.of(SomeClass.class));
      InjectionContextScope scope = InjectionContextProvider.provider().enterContextScope(
        injector,
        someClassBinding,
        Collections.singletonMap(worldStringBinding.mainKey(), () -> "World!"));

      SomeClass someClass = resolveInstanceScoped(scope);
      Assertions.assertNotNull(someClass);

      // the overridden value
      Assertions.assertEquals("World!", someClass.world);
      Assertions.assertNotNull(someClass.otherClass);
      Assertions.assertEquals("World!", someClass.otherClass.world);

      Assertions.assertEquals("World!", someClass.helloWorld);
      Assertions.assertEquals("World!", someClass.otherClass.helloWorld);
    }

    // without override
    {
      Object constructed = injector.instance(SomeClass.class);
      Assertions.assertNotNull(constructed);
      SomeClass someClass = Assertions.assertInstanceOf(SomeClass.class, constructed);

      Assertions.assertEquals("Hello World!", someClass.world);
      Assertions.assertNotNull(someClass.otherClass);
      Assertions.assertEquals("Hello World!", someClass.otherClass.world);

      Assertions.assertEquals("Hello World!", someClass.helloWorld);
      Assertions.assertEquals("Hello World!", someClass.otherClass.helloWorld);
    }
  }

  public static final class SomeClass {

    private final String world;
    private final SomeOtherClass otherClass;

    private String helloWorld;

    @Inject
    public SomeClass(@Named("test") String world, Injector injector) {
      this.world = world;
      this.otherClass = injector.instance(SomeOtherClass.class);
    }

    @Inject
    public void injectHelloWorld(@Named("test") String helloWorld) {
      this.helloWorld = helloWorld;
    }
  }

  public static final class SomeOtherClass {

    private final String world;

    @Inject
    @Named("test")
    private String helloWorld;

    @Inject
    public SomeOtherClass(@Named("test") String world) {
      this.world = world;
    }
  }
}
