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
package com.gip.xyna.xsor.indices;

import com.gip.xyna.xsor.indices.search.ComparisionAlgorithm;


public class XSORPayloadPrimaryKeyIndexKey extends UniqueIndexKey {
  
  Object xsorPayloadPrimaryKey;
  ComparisionAlgorithm comparisonAlgorithm;
  
  XSORPayloadPrimaryKeyIndexKey(Object xsorPayloadPrimaryKey) {
    this.xsorPayloadPrimaryKey = xsorPayloadPrimaryKey;
    this.comparisonAlgorithm = ComparisionAlgorithm.getComparisonAlgorithmType(xsorPayloadPrimaryKey);
  }

  @Override
  public boolean keyEquals(UniqueIndexKey otherKey) {
    return comparisonAlgorithm.areEqual(xsorPayloadPrimaryKey, ((XSORPayloadPrimaryKeyIndexKey)otherKey).xsorPayloadPrimaryKey);
  }

  @Override
  public int keyHash() {
    return comparisonAlgorithm.calculateHashCode(xsorPayloadPrimaryKey);
  }
  
  
  public Object getXSORPayloadPrimaryKey() {
    return xsorPayloadPrimaryKey;
  }
  
}
