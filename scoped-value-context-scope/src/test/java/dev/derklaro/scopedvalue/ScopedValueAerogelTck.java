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

package dev.derklaro.scopedvalue;

import dev.derklaro.aerogel.Injector;
import dev.derklaro.aerogel.binding.UninstalledBinding;
import dev.derklaro.aerogel.binding.builder.RootBindingBuilder;
import dev.derklaro.aerogel.internal.context.scope.InjectionContextProvider;
import dev.derklaro.aerogel.scopedvalue.ScopedValueInjectionContextProvider;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.atinject.tck.auto.Convertible;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.Engine;
import org.atinject.tck.auto.Seat;
import org.atinject.tck.auto.Tire;
import org.atinject.tck.auto.V8Engine;
import org.atinject.tck.auto.accessories.SpareTire;
import org.junit.Assert;

/**
 * Tests aerogel against <a href="https://github.com/eclipse-ee4j/injection-tck">the injection tck (Technology
 * Compatibility Kit) of jakarta</a> using scoped values.
 */
public class ScopedValueAerogelTck extends TestCase {

  public static Test suite() {
    // required bindings are described in the following document, everything else can be done dynamically:
    // https://github.com/eclipse-ee4j/injection-tck/blob/master/README.adoc#configuring-the-di-environment
    Injector injector = Injector.newInjector();

    RootBindingBuilder bindingBuilder = injector.createBindingBuilder();
    UninstalledBinding<Car> carBinding = bindingBuilder
      .bind(Car.class)
      .toConstructingClass(Convertible.class);
    UninstalledBinding<Engine> engineBinding = bindingBuilder
      .bind(Engine.class)
      .toConstructingClass(V8Engine.class);
    UninstalledBinding<Tire> spareTireBinding = bindingBuilder
      .bind(Tire.class)
      .qualifiedWithName("spare")
      .toConstructingClass(SpareTire.class);
    UninstalledBinding<Seat> driversSeatBinding = bindingBuilder
      .bind(Seat.class)
      .qualifiedWith(Drivers.class)
      .toConstructingClass(DriversSeat.class);

    injector
      .installBinding(carBinding)
      .installBinding(engineBinding)
      .installBinding(spareTireBinding)
      .installBinding(driversSeatBinding);
    Car constructedCar = injector.instance(Car.class);

    TestSuite testSuite = new TestSuite("ScopedValue Jakarta TCK");
    testSuite.addTestSuite(ScopedValueDiscoveryTest.class);

    Test tckSuite = Tck.testsFor(constructedCar, true, true);
    testSuite.addTest(tckSuite);
    return testSuite;
  }

  public static final class ScopedValueDiscoveryTest extends TestCase {

    public void testScopedValueContextUsed() {
      InjectionContextProvider contextProvider = InjectionContextProvider.provider();
      Assert.assertSame(ScopedValueInjectionContextProvider.class, contextProvider.getClass());
    }
  }
}
