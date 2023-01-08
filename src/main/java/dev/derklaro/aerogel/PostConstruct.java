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

package dev.derklaro.aerogel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apiguardian.api.API;

/**
 * The post construct annotation can be placed on an instance method (non-static) which needs to be executed after the
 * successful creation and member injection of a class. This annotation can be applied to multiple methods in a class.
 * Each instance creation will trigger a new call to all annotated methods.
 *
 * <p>The call oder of post construct listeners can be influenced by using an {@link Order} annotation on the method.
 *
 * <p>A post construct method needs to meet the following criteria:
 * <ol>
 *   <li>The method takes no arguments.
 *   <li>The method can return something, but the return value is ignored.
 *   <li>The method must be public, protected, package private or private.
 *   <li>The method must not be static, native or abstract.
 *   <li>This annotation can not be combined with {@link Inject}.
 * </ol>
 *
 * <p>Example usage:
 * <pre>
 *   public class Company {
 *     &#064;Inject
 *     private String name;
 *
 *     &#064;PostConstruct
 *     public void postConstruct() {
 *       System.out.printf("Company with name %s constructed.", this.name);
 *     }
 *   }
 * </pre>
 *
 * @author Pasqual K.
 * @since 2.0
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@API(status = API.Status.STABLE, since = "2.0")
public @interface PostConstruct {

}
