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
package com.gip.xyna.xsor.indices.management;

import com.gip.xyna.xsor.indices.CompositeIndex;
import com.gip.xyna.xsor.indices.HashIndex;
import com.gip.xyna.xsor.indices.PessimisticOrderedIndex;
import com.gip.xyna.xsor.indices.UniqueIndex;
import com.gip.xyna.xsor.indices.XSORPayloadPrimaryKeyIndex;
import com.gip.xyna.xsor.indices.XSORPayloadPrimaryKeyIndexImpl;
import com.gip.xyna.xsor.indices.definitions.HashIndexDefinition;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.definitions.OrderedIndexDefinition;
import com.gip.xyna.xsor.indices.definitions.UniqueIndexDefinition;




public class BasicIndexFactory implements IndexFactory {

  private final int concurrentHashMapInitialCapacity;
  private final float concurrentHashMapLoadFactor;
  private final int concurrentHashMapConcurrencyLevel;
  private final ConcurrencySetting concurrencySetting;
  private final int optimisticRetries;
  
  public BasicIndexFactory() {
    // values taken from tests against early index implementations
    this(1000000, 0.75f, 32, ConcurrencySetting.PESSIMISTIC);
  }
  
  public BasicIndexFactory(int concurrentHashMapInitialCapacity, float concurrentHashMapLoadFactor, int concurrentHashMapConcurrencyLevel) {
    this(concurrentHashMapInitialCapacity, concurrentHashMapLoadFactor, concurrentHashMapConcurrencyLevel, ConcurrencySetting.PESSIMISTIC);
  }
  
  public BasicIndexFactory(int concurrentHashMapInitialCapacity, float concurrentHashMapLoadFactor, int concurrentHashMapConcurrencyLevel, ConcurrencySetting concurrencySetting) {
    this(concurrentHashMapInitialCapacity, concurrentHashMapLoadFactor, concurrentHashMapConcurrencyLevel, concurrencySetting, -1);
  }
  
  public BasicIndexFactory(int concurrentHashMapInitialCapacity, float concurrentHashMapLoadFactor, int concurrentHashMapConcurrencyLevel, ConcurrencySetting concurrencySetting, int optimisticRetries) {
    this.concurrentHashMapInitialCapacity = concurrentHashMapInitialCapacity;
    this.concurrentHashMapLoadFactor = concurrentHashMapLoadFactor;
    this.concurrentHashMapConcurrencyLevel = concurrentHashMapConcurrencyLevel;
    this.concurrencySetting = concurrencySetting;
    this.optimisticRetries = optimisticRetries;
  }
  
  public CompositeIndex createIndex(IndexDefinition definition) {
    if (definition instanceof UniqueIndexDefinition) {
      return new UniqueIndex((UniqueIndexDefinition)definition,
                             concurrencySetting.getAtomicitySupportForSetting(),
                             concurrentHashMapInitialCapacity,
                             concurrentHashMapLoadFactor,
                             concurrentHashMapConcurrencyLevel);
    } else if (definition instanceof HashIndexDefinition) {
      return new HashIndex((HashIndexDefinition)definition,
                           concurrencySetting.getAtomicitySupportForSetting(),
                           concurrentHashMapInitialCapacity,
                           concurrentHashMapLoadFactor,
                           concurrentHashMapConcurrencyLevel,
                           optimisticRetries);
    } else if (definition instanceof OrderedIndexDefinition) {
      return new PessimisticOrderedIndex((OrderedIndexDefinition)definition);
    }
    throw new UnsupportedOperationException("Unknown IndexDefinition type"); // TODO other error
  }
  
  
  public XSORPayloadPrimaryKeyIndex createXSORPayloadPrimaryKeyIndex() {
    return new XSORPayloadPrimaryKeyIndexImpl(concurrentHashMapInitialCapacity, concurrentHashMapLoadFactor, concurrentHashMapConcurrencyLevel);
  }

}
