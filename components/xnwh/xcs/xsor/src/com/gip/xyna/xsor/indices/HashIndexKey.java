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
package com.gip.xyna.xsor.indices;


public abstract class HashIndexKey extends IndexKey {

  private Integer hash;
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof HashIndexKey) {
      return hashCode() == obj.hashCode();
    } else {
      return false;
    }
  }
  
  @Override //TODO redux ?
  public int hashCode() {
    if (hash == null) {
      hash = new Integer(keyHash());
    }
    return hash.intValue();
  }
  
  public abstract int keyHash();
  
}
