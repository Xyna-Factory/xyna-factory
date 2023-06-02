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

import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;


public class SingleColumnReader implements ResultSetReader<Object> {

  StorableColumnInformation singleColumn; 
  
  SingleColumnReader(XMOMStorableStructureInformation rootInfo) {
    for (StorableColumnInformation column : rootInfo.getColumnInfo(false)) {
      if (column.getType() == VarType.PK) {
        singleColumn = column;
      }
    }
  }
  
  SingleColumnReader(StorableColumnInformation columnInfo) {
    singleColumn = columnInfo;
  }
  
  public Object read(ResultSet rs) throws SQLException {
    Object pkValue;
    switch (singleColumn.getPrimitiveType()) {
      case BOOLEAN :
      case BOOLEAN_OBJ :
        pkValue = rs.getBoolean(singleColumn.getColumnName());
        break;
      case BYTE :
      case BYTE_OBJ :
        pkValue = rs.getByte(singleColumn.getColumnName());
        break;
      case DOUBLE :
      case DOUBLE_OBJ :
        pkValue = rs.getDouble(singleColumn.getColumnName());
        break;
      case INT :
      case INTEGER :
        pkValue = rs.getInt(singleColumn.getColumnName());
        break;
      case STRING :
        pkValue = rs.getString(singleColumn.getColumnName());
        break;
      case LONG :
      case LONG_OBJ :
        pkValue = rs.getLong(singleColumn.getColumnName());
        break;
      default :
        pkValue = rs.getObject(singleColumn.getColumnName());
        break;
    }
    if (rs.wasNull()) {
      return null;
    } else {
      return pkValue;
    }
  }

}
