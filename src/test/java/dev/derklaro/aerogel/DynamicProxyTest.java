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

import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import jakarta.inject.Inject;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DynamicProxyTest {

  @Test
  void testMultipleInterfacesProxiedCorrectly() {
    Injector injector = Injector.newInjector();
    RootBindingBuilder bindingBuilder = injector.createBindingBuilder();
    UninstalledBinding<?> aItfBinding = bindingBuilder
      .bind(AItf1.class).andBind(AItf2.class) // main key should be AItf1 as we inject AItf2 in BImpl
      .toConstructingClass(AItf1Impl.class);
    injector.installBinding(aItfBinding);

    BImpl b = Assertions.assertDoesNotThrow(() -> injector.instance(BImpl.class));
    Assertions.assertNotNull(b.aItf2);
    Assertions.assertTrue(Proxy.isProxyClass(b.aItf2.getClass()));
    Assertions.assertInstanceOf(AItf1.class, b.aItf2);
  }

  @Test
  void testProxyNotAttemptedIfNonProxyableTypeIsRequested() {
    Injector injector = Injector.newInjector();
    RootBindingBuilder bindingBuilder = injector.createBindingBuilder();
    UninstalledBinding<?> a1ItfBinding = bindingBuilder.bind(AItf1.class).toConstructingClass(CImpl.class);
    UninstalledBinding<?> a2ItfBinding = bindingBuilder.bind(AItf2.class).toConstructingClass(DImpl.class);
    injector.installBinding(a1ItfBinding).installBinding(a2ItfBinding);

    Exception thrown = Assertions.assertThrows(RuntimeException.class, () -> injector.instance(CImpl.class));
    Assertions.assertTrue(thrown.getMessage().startsWith("Detected cyclic dependency while constructing"));
  }

  // @formatter:off
  public interface AItf1 {}
  public interface AItf2 {}
  public static class AItf1Impl implements AItf1, AItf2 {
    @Inject public AItf1Impl(BImpl b) {}
  }
  public static class BImpl {
    private final AItf2 aItf2;
    @Inject public BImpl(AItf2 aItf2) { this.aItf2 = aItf2; }
  }
  public static class CImpl implements AItf1 {
    @Inject public CImpl(DImpl d) {}
  }
  public static class DImpl implements AItf2 {
    @Inject public DImpl(CImpl c) {}
  }
  // @formatter:on
}
