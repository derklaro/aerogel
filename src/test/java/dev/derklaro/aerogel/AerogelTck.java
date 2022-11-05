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

package dev.derklaro.aerogel;

import junit.framework.Test;
import junit.framework.TestCase;
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

/**
 * Tests aerogel against <a href="https://github.com/eclipse-ee4j/injection-tck">the injection tck (Technology
 * Compatibility Kit) of jakarta</a>
 */
public class AerogelTck extends TestCase {

  public static Test suite() {
    // set up the injector
    Injector injector = Injector.newInjector();
    // all bindings as described in https://github.com/eclipse-ee4j/injection-tck/blob/master/README.adoc#configuring-the-di-environment
    // every other binding can be done dynamically during the runtime
    injector
      .install(Bindings.constructing(Element.forType(Car.class), Element.forType(Convertible.class)))
      .install(Bindings.constructing(Element.forType(Engine.class), Element.forType(V8Engine.class)))
      .install(Bindings.constructing(
        Element.forType(Tire.class).requireName("spare"),
        Element.forType(SpareTire.class)))
      .install(Bindings.constructing(
        Element.forType(Seat.class).requireAnnotation(Drivers.class),
        Element.forType(DriversSeat.class)));
    // run the test
    return Tck.testsFor(injector.instance(Car.class), true, true);
  }
}
