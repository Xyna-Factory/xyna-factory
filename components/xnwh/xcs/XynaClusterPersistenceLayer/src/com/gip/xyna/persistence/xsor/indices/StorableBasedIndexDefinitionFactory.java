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
package com.gip.xyna.persistence.xsor.indices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.CompositeIndex;
import com.gip.xyna.xnwh.persistence.Storable;


public class StorableBasedIndexDefinitionFactory implements IndexDefinitionFactory {

  private static Pattern compositeIndexDisecter = Pattern.compile("(\\w+(?=(,|$))|[A-Z]+\\(.*\\))");
  
  
  public <S extends Storable> IndexDefinition<S, ? extends IndexKey, ? extends IndexSearchCriterion> createPrimaryKeyIndex(Class<S> clazz,
                                                                                                                           String primaryKeyColumnName) {
    String tableName = Storable.getPersistable(clazz).tableName();
    return new StorableBasedUniqueIndexDefinition<S>(tableName, new String[] {primaryKeyColumnName});
  }

  public <S extends Storable<S>> IndexDefinition<S, ? extends IndexKey, ? extends IndexSearchCriterion> createIndex(Class<S> clazz, Column column) {
            String tableName = Storable.getPersistable(clazz).tableName();
            switch (column.index()) {
              case MULTIPLE : // TODO or should we create ordered?
                return new StorableBasedHashIndexDefinition<S>(tableName, new String[] {column.name()});
              case PRIMARY :
              case UNIQUE :
                return new StorableBasedUniqueIndexDefinition<S>(tableName, new String[] {column.name()});
              default :
                return null;
            }
          }

  
  public <S extends Storable<S>> IndexDefinition<S, ? extends IndexKey, ? extends IndexSearchCriterion> createIndex(Class<S> clazz,
                                                                                                                 CompositeIndex composition) {
    String tableName = Storable.getPersistable(clazz).tableName();

    List<String> columnList = new ArrayList<String>();
    Matcher columnMatcher = compositeIndexDisecter.matcher(composition.value());
    while (columnMatcher.find()) {
      String column = columnMatcher.group().trim();
      columnList.add(column);
    }
    
    String[] columns = columnList.toArray(new String[columnList.size()]);
    
    switch (composition.type()) {
      case HASH :
        return new StorableBasedHashIndexDefinition<S>(tableName, columns);
      case UNIQUE :
        return new StorableBasedUniqueIndexDefinition<S>(tableName, columns);
      case ORDERED_LEX :
        return new StorableBasedOrderedIndexDefinition<S>(tableName, columns);
      default :
        return null;
    }
  }
  
   
}
