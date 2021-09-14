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

package aerogel;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies qualifier annotations. These annotations are used for annotation matching in an {@link Injector}. See
 * {@link Element#requireAnnotations(Class[])} and {@link Element#requireAnnotations(Annotation...)}.
 * <p>
 * A qualifier annotation...
 *
 * <ul>
 *   <li>... must be annotated with {@literal @}Qualifier.</li>
 *   <li>... must be {@literal @}Retention(RetentionPolicy.RUNTIME).</li>
 *   <li>... can have attributes (will be matched in the runtime differently).</li>
 * </ul>
 *
 * <p>A custom qualifier annotation can look like:
 *
 * <pre>
 *   &#064;aerogel.Qualifier
 *   &#064;java.lang.annotation.Documented
 *   &#064;java.lang.annotation.Retention(RUNTIME)
 *   public @interface Employee {
 *     Type type() default NORMAL;
 *
 *     public enum Type {
 *       SENIOR,
 *       NORMAL,
 *       GONE
 *     }
 *   }
 * </pre>
 *
 * <p>It can then be used as follows for injection when a binding was created for the {@code NORMAL} and {@code SENIOR}
 * employee type:
 *
 * <pre>
 *   public class Company {
 *     &#064;Inject
 *     public Company(&#064;Employee Employee johnWick, &#064;Employee(type = Type.SENIOR) Employee spiderMan) {
 *
 *     }
 *   }
 * </pre>
 *
 * @author Pasqual K.
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Qualifier {

}
