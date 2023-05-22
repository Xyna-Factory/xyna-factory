/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xsor.indices.search;

public class SearchValue {
  
  protected final ComparisionAlgorithm algorithm;
  protected final Object value;
  
  public SearchValue(Object value) {
    this(value, ComparisionAlgorithm.getComparisonAlgorithmType(value));
  }
  
  public SearchValue(Object value, ComparisionAlgorithm algorithm) {
    this.value = value;
    this.algorithm = algorithm;
  }

  
  public ComparisionAlgorithm getComparisionAlgorithm() {
    return algorithm;
  }

  
  public Object getValue() {
    return value;
  }
  
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SearchValue) {
      return algorithm.areEqual(value, ((SearchValue)obj).value);
    } else {
      return false;
    }
  }
  
  
  @Override
  public int hashCode() {
    return algorithm.calculateHashCode(value);
  }
  
  
  
  
}
