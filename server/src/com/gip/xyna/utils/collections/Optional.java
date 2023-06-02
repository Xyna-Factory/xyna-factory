/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.utils.collections;

import java.io.Serializable;
import java.util.NoSuchElementException;


// Stripped Optional that can be replaced with java.util once >= jdk 1.8 
// the Mapper- and Factory-methods are missing in this implementation and it only serves to more clearly communicate a possible null-value as in- or out-put 
/**
 * Optional ist nur dann Serializable, wenn auch T Serializable ist 
 */
public class Optional<T> implements Serializable {
  
  private static final long serialVersionUID = 1L;

  private final static Optional<?> EMPTY = new Optional();

  private final T value; //achtung: T ist nicht immer serializable, dann ist auch optional nicht serializable. so wird es derzeit verwendet, auch wenn das unschön ist
  private final boolean present;

  public Optional(T value) {
      this.value = value;
      this.present = true;
  }

  private Optional() {
      this.value = null;
      this.present = false;
  }

  public static <T> Optional<T> empty() {
      return (Optional<T>) EMPTY;
  }

  // not part if the jdk impl (at least last I checked)
  public static <T> Optional<T> of(T value) {
    if (value == null) {
      return Optional.empty();
    } else {
      return new Optional<T>(value);  
    }
  }
  
  public T get() {
      if (!present)
          throw new NoSuchElementException();
      return value;
  }

  public boolean isPresent() {
      return present;
  }

  public T orElse(T other) {
      return present ? value : other;
  }


  @Override
  public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Optional optional = (Optional) o;

      if (present != optional.present) return false;
      if (value != null ? !value.equals(optional.value) : optional.value != null) return false;

      return true;
  }

  @Override
  public int hashCode() {
      int result = value != null ? value.hashCode() : 0;
      result = 31 * result + (present ? 1 : 0);
      return result;
  }
}
