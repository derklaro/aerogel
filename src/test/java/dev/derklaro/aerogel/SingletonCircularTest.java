/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021-2025 Pasqual K. and contributors
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
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingletonCircularTest {

  @Test
  void testSingletonKeptOnDuplicateCircularDependency() {
    Injector injector = Injector.newInjector();
    RootBindingBuilder bindingBuilder = injector.createBindingBuilder();
    UninstalledBinding<?> aItfBinding = bindingBuilder.bind(AItf1.class).toConstructingClass(AItf1Impl.class);
    injector.installBinding(aItfBinding);

    Assertions.assertDoesNotThrow(() -> injector.instance(AItf1.class));
    Assertions.assertEquals(1, A_IMPL_INIT_CALLS.get());
    Assertions.assertEquals(2, NON_A_IMPL_INIT_CALLS.get());
  }

  // @formatter:off
  private static final AtomicInteger A_IMPL_INIT_CALLS = new AtomicInteger(0);
  private static final AtomicInteger NON_A_IMPL_INIT_CALLS = new AtomicInteger(0);
  public interface AItf1 {}
  @Singleton public static class AItf1Impl implements AItf1 {
    @Inject public AItf1Impl(BImpl b, CImpl c) { A_IMPL_INIT_CALLS.incrementAndGet(); }
  }
  public static class BImpl {
    @Inject public BImpl(AItf1 aItf1) { NON_A_IMPL_INIT_CALLS.incrementAndGet(); }
  }
  public static class CImpl {
    @Inject public CImpl(AItf1 aItf1) { NON_A_IMPL_INIT_CALLS.incrementAndGet(); }
  }
  // @formatter:on
}
