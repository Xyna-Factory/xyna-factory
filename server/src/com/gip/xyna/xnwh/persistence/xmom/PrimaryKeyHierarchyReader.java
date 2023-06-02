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
package com.gip.xyna.xnwh.persistence.xmom;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator.AliasDictionary;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator.QualifiedStorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;


public class PrimaryKeyHierarchyReader implements ResultSetReader<Void> {

  private List<QualifiedStorableColumnInformation> resultSetColumns = new ArrayList<QualifiedStorableColumnInformation>();
  private AliasDictionary dictionary;
  private Map<String, Set<Object>> resultMap = new HashMap<String, Set<Object>>();
  private Map<String, Set<String>> typeMap = new HashMap<>();
  
  PrimaryKeyHierarchyReader(List<QualifiedStorableColumnInformation> selectedColumns, AliasDictionary dictionary) {
    for (QualifiedStorableColumnInformation entry : selectedColumns) {
      if (entry.getColumn().getType() == VarType.PK ||
          entry.getColumn().getType() == VarType.TYPENAME) {
        resultSetColumns.add(entry);
      }
    }
    this.dictionary = dictionary;
  }
  
  
  public Void read(ResultSet rs) throws SQLException {
    for (QualifiedStorableColumnInformation entry : resultSetColumns) {
      if (entry.getColumn().getType() == VarType.PK) {
        Object pkValue;
        switch (entry.getColumn().getPrimitiveType()) {
          case BOOLEAN :
          case BOOLEAN_OBJ :
            pkValue = rs.getBoolean(dictionary.getOrCreateColumnAlias(entry));
            break;
          case BYTE :
          case BYTE_OBJ :
            pkValue = rs.getByte(dictionary.getOrCreateColumnAlias(entry));
            break;
          case DOUBLE :
          case DOUBLE_OBJ :
            pkValue = rs.getDouble(dictionary.getOrCreateColumnAlias(entry));
            break;
          case INT :
          case INTEGER :
            pkValue = rs.getInt(dictionary.getOrCreateColumnAlias(entry));
            break;
          case STRING :
            pkValue = rs.getString(dictionary.getOrCreateColumnAlias(entry));
            break;
          case LONG :
          case LONG_OBJ :
            pkValue = rs.getLong(dictionary.getOrCreateColumnAlias(entry));
            break;
          default :
            pkValue = rs.getObject(dictionary.getOrCreateColumnAlias(entry));
            break;
        }
        if (!rs.wasNull()) {
          StorableStructureInformation parentTable = entry.getColumn().getParentStorableInfo();
          Set<Object> pksForTable = resultMap.get(parentTable.getTableName());
          if (pksForTable == null) {
            pksForTable = new HashSet<Object>();
          }
          pksForTable.add(pkValue);
          resultMap.put(parentTable.getTableName(), pksForTable);
        }
      } else {
        String typeName = rs.getString(dictionary.getOrCreateColumnAlias(entry));
        if (!rs.wasNull()) {
          StorableStructureInformation parentTable = entry.getColumn().getParentStorableInfo();
          Set<String> typesForTable = typeMap.get(parentTable.getTableName());
          if (typesForTable == null) {
            typesForTable = new HashSet<>();
          }
          typesForTable.add(typeName);
          typeMap.put(parentTable.getTableName(), typesForTable);
        }
      }
    }
    return null;
  }
  
  
  public Pair<Map<String, Set<Object>>, Map<String, Set<String>>> getResult() {
    return Pair.of(resultMap, typeMap);
  }

}
