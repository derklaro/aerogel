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

package aerogel.internal;

import aerogel.Element;
import aerogel.internal.utility.ToStringHelper;
import java.lang.reflect.Type;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Don't use this class directly, use {@link Element#ofType(Type)} and {@link Element#named(String, Type)} instead.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class DefaultElement implements Element {

  private final String requiredName;
  private final Type componentType;

  /**
   * Constructs a new default element type instance.
   *
   * @param requiredName  the name required by the element or {@code null} if no name is required.
   * @param componentType the type of the element.
   */
  public DefaultElement(@Nullable String requiredName, @NotNull Type componentType) {
    this.requiredName = requiredName;
    this.componentType = componentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable String requiredName() {
    return this.requiredName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Type componentType() {
    return this.componentType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull String toString() {
    return ToStringHelper.from(this)
      .putField("requiredName", this.requiredName)
      .putField("componentType", this.componentType)
      .toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.requiredName, this.componentType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@NotNull Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DefaultElement)) {
      return false;
    }
    DefaultElement that = (DefaultElement) o;
    return Objects.equals(this.requiredName, that.requiredName)
      && Objects.equals(this.componentType, that.componentType);
  }
}
