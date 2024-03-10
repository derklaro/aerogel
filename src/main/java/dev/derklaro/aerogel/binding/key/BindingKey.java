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

package dev.derklaro.aerogel.binding.key;

import dev.derklaro.aerogel.internal.annotation.InjectAnnotationUtil;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import org.apiguardian.api.API;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A key that uniquely describes a binding by using the target type and qualifier annotation. Primitive types are
 * replaced by their box types when a key is created. Therefore, creating a key for an {@code int} matches
 * {@code Integer} as well (and vice-versa).
 * <p>
 * Creating a binding key with the type {@code PaymentService} and the qualifier annotation {@code PayPal} created by
 * {@code BindingKey.of(PaymentService.class).withQualifier(PayPal.class)} would for example match:
 * <pre>
 * {@code
 * @Paypal
 * private PaymentService paymentService;
 * }
 * </pre>
 *
 * @param <T> the type that is matched by this key.
 * @author Pasqual Koschmieder
 * @since 3.0
 */
@API(status = API.Status.STABLE, since = "3.0")
public final class BindingKey<T> {

  private final int hash;

  private final Type type;
  private final AnnotationMatcher annotationMatcher;

  /**
   * Constructs a new binding key instance for the given type and optional annotation matcher. This constructor should
   * only be used by internal code, external code should use the factory methods instead.
   *
   * @param type    the type that this key targets.
   * @param matcher a matcher for the optional qualifier annotation.
   */
  private BindingKey(@NotNull Type type, @Nullable AnnotationMatcher matcher) {
    this.type = GenericTypeReflector.box(type);
    this.annotationMatcher = matcher;
    this.hash = Objects.hash(this.type, this.annotationMatcher);
  }

  /**
   * Checks if the given annotation type is a valid qualifier annotation, throws an illegal argument exception if not.
   *
   * @param annotationType the annotation type that should be validated.
   * @throws IllegalArgumentException if the given annotation type is not a valid qualifier annotation.
   */
  private static void checkValidQualifierAnnotation(@NotNull Class<? extends Annotation> annotationType) {
    boolean validQualifier = InjectAnnotationUtil.validQualifierAnnotation(annotationType);
    if (!validQualifier) {
      throw new IllegalArgumentException("Annotation " + annotationType + " is not a qualifier annotation");
    }
  }

  /**
   * Constructs a new binding key for the given generic type without matching any qualifier annotation. If possible, a
   * raw type or type token should be used instead, as the returned key is directly bound to the type in these cases.
   *
   * @param type the type that the key should match.
   * @param <T>  the type that is being matched.
   * @return a new binding key matching the given type without any annotation.
   * @see #of(Class)
   */
  @Contract(value = "_ -> new", pure = true)
  public static @NotNull <T> BindingKey<T> of(@NotNull Type type) {
    return new BindingKey<>(type, null);
  }

  /**
   * Constructs a new binding key for the given raw type without matching any qualifier annotation. For nested types
   * (such as parameterized types), use the factory methods that take a type token or generic type instead.
   *
   * @param type the raw type to match.
   * @param <T>  the type that is being matched.
   * @return a new binding key matching the given raw type without any annotations.
   * @see #of(TypeToken)
   */
  @Contract(value = "_ -> new", pure = true)
  public static @NotNull <T> BindingKey<T> of(@NotNull Class<? extends T> type) {
    return new BindingKey<>(type, null);
  }

  /**
   * Constructs a new binding key for the type that is wrapped in the given type token without matching any qualifier
   * annotations. This method is preferred in cases where nested types being bound, due to the type safety.
   *
   * @param typeToken the type token wrapping the type that should be matched.
   * @param <T>       the type that is being matched.
   * @return a new binding key matching the type wrapped in the given type token without any annotations.
   */
  @Contract(value = "_ -> new", pure = true)
  public static @NotNull <T> BindingKey<T> of(@NotNull TypeToken<? extends T> typeToken) {
    return of(typeToken.getType());
  }

  /**
   * Returns a new binding key that matches the same type that is being matched by this key, but without the optional
   * qualifier annotation.
   *
   * @return a new binding key matching the same type as this key but no qualifier annotation.
   */
  @CheckReturnValue
  public @NotNull BindingKey<T> withoutQualifier() {
    return new BindingKey<>(this.type, null);
  }

  /**
   * Constructs a new binding key that uses the same type as this key but also requires the given qualifier annotation
   * to match.
   *
   * @param qualifierAnnotation the qualifier annotation to require.
   * @return a new binding key matching the type of this key and the given qualifier annotation.
   * @throws IllegalArgumentException if the given annotation is not a valid qualifier annotation.
   * @see jakarta.inject.Qualifier
   */
  @CheckReturnValue
  public @NotNull BindingKey<T> withQualifier(@NotNull Annotation qualifierAnnotation) {
    Class<? extends Annotation> type = qualifierAnnotation.annotationType();
    checkValidQualifierAnnotation(type);

    AnnotationMatcher matcher = AnnotationMatcher.matchingStrategyFor(qualifierAnnotation);
    return new BindingKey<>(this.type, matcher);
  }

  /**
   * Constructs a new binding key that uses the same type as this key but also requires the given qualifier annotation
   * type to match. Note that this method should not be used if the given qualifier annotation type has properties, as
   * keys created from real members will always require the properties as well.
   *
   * @param qualifierAnnotationType the qualifier annotation type to require.
   * @return a new binding key matching the type of this key and the given qualifier annotation type.
   * @throws IllegalArgumentException if the given annotation type is not a valid qualifier annotation.
   * @see jakarta.inject.Qualifier
   * @see #withQualifier(Annotation)
   */
  @CheckReturnValue
  public @NotNull BindingKey<T> withQualifier(@NotNull Class<? extends Annotation> qualifierAnnotationType) {
    checkValidQualifierAnnotation(qualifierAnnotationType);
    AnnotationMatcher matcher = AnnotationMatcher.forMatchingType(qualifierAnnotationType);
    return new BindingKey<>(this.type, matcher);
  }

  /**
   * Tried to find a qualifier annotation in the given annotation array and applies it as a requirement to this binding
   * key. If no qualifier annotations are present in the given annotation array, this binding key is returned without
   * any modifications.
   *
   * @param annotations the array of annotations in which a qualifier annotation is to be searched for.
   * @return a new binding key with the qualifier annotation from the given array applied, else this key.
   * @throws IllegalArgumentException if multiple qualifier annotations are detected in the given annotation array.
   */
  @CheckReturnValue
  public @NotNull BindingKey<T> selectQualifier(@NotNull Annotation[] annotations) {
    Annotation qualifier = null;
    for (Annotation annotation : annotations) {
      boolean validQualifier = InjectAnnotationUtil.validQualifierAnnotation(annotation.annotationType());
      if (validQualifier) {
        if (qualifier != null) {
          throw new IllegalArgumentException("Detected duplicate qualifier annotation: "
            + annotation.annotationType()
            + " and "
            + qualifier.annotationType());
        }

        qualifier = annotation;
      }
    }

    if (qualifier == null) {
      return this;
    } else {
      AnnotationMatcher matcher = AnnotationMatcher.matchingStrategyFor(qualifier);
      return new BindingKey<>(this.type, matcher);
    }
  }

  /**
   * Constructs a new binding key that matches the given type and the same qualifier annotation that this key is
   * matching.
   *
   * @param type the type that the new binding key should match.
   * @param <R>  the new type being matched.
   * @return a new binding key requiring the same annotation as this key but the given type.
   */
  @CheckReturnValue
  public @NotNull <R> BindingKey<R> withType(@NotNull Type type) {
    return new BindingKey<>(type, this.annotationMatcher);
  }

  /**
   * Constructs a new binding key that matches the given type and the same qualifier annotation that this key is
   * matching.
   *
   * @param type the type that the new binding key should match.
   * @param <R>  the new type being matched.
   * @return a new binding key requiring the same annotation as this key but the given type.
   */
  @CheckReturnValue
  public @NotNull <R> BindingKey<R> withType(@NotNull Class<? extends R> type) {
    return new BindingKey<>(type, this.annotationMatcher);
  }

  /**
   * Constructs a new binding key that matches the type wrapped in the given type token and the same qualifier
   * annotation that this key is matching.
   *
   * @param typeToken the type token that wraps the type that the new binding key should match.
   * @param <R>       the new type being matched.
   * @return a new binding key requiring the same annotation as this key but the type wrapped in the given type token.
   */
  @CheckReturnValue
  public @NotNull <R> BindingKey<R> withType(@NotNull TypeToken<? extends R> typeToken) {
    return this.withType(typeToken.getType());
  }

  /**
   * Constructs a new binding key that matches the raw type of the type that is currently wrapped in this key and the
   * same qualifier annotation that this key is matching.
   *
   * @param <R> the raw variant of the type being matched by this key.
   * @return a new binding key that matches the raw type of the type that is wrapped in this binding key.
   */
  @CheckReturnValue
  public @NotNull <R> BindingKey<R> withRawType() {
    @SuppressWarnings("unchecked")
    Class<R> rawType = (Class<R>) GenericTypeReflector.erase(this.type);
    return this.withType(rawType);
  }

  /**
   * Get the type that is being matched by this binding key.
   *
   * @return the type that is being matched by this binding key.
   */
  @NotNull
  public Type type() {
    return this.type;
  }

  /**
   * Get the type of the optional qualifier annotation that is being matched by this key.
   *
   * @return the type of the optional qualifier annotation that is being matched by this key.
   */
  @NotNull
  public Optional<Class<? extends Annotation>> qualifierAnnotation() {
    if (this.annotationMatcher == null) {
      return Optional.empty();
    } else {
      return Optional.of(this.annotationMatcher.annotationType());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return this.hash;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj instanceof BindingKey<?>) {
      BindingKey<?> other = (BindingKey<?>) obj;
      return other.type.equals(this.type) && Objects.equals(other.annotationMatcher, this.annotationMatcher);
    }

    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String toString() {
    return "BindingKey[type=" + this.type + ", annotations=" + this.annotationMatcher + "]";
  }
}
