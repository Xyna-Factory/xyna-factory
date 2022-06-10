/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.persistence.xsor.indices;


import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.CompositeIndex;
import com.gip.xyna.xnwh.persistence.Storable;


public interface IndexDefinitionFactory {

  public <S extends Storable<S>> IndexDefinition<S, ? extends IndexKey, ? extends IndexSearchCriterion> createIndex(Class<S> clazz, Column column);
  
  public <S extends Storable<S>> IndexDefinition<S, ? extends IndexKey, ? extends IndexSearchCriterion>  createIndex(Class<S> clazz, CompositeIndex composition);
  
}
