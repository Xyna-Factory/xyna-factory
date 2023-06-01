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
package com.gip.xyna.xnwh.persistence.xmom;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.generation.StorableCodeBuilder;

public class StorableTypDecisionReader implements ResultSetReader {
  
  private Map<String, ResultSetReader<?>> readers = new HashMap<>();
  private String typenameColumn;
  
  public StorableTypDecisionReader(String typenameColumn, Collection<StorableStructureInformation> structures) {
    this.typenameColumn = typenameColumn;
    for (StorableStructureInformation structure : structures) {
      readers.put(structure.fqClassNameOfDatatype, structure.getResultSetReaderForDatatype());
    }
  }

  
  public Object read(ResultSet rs) throws SQLException {
    String typeName = rs.getString(typenameColumn);
    if (typeName == null) {
      // TODO we might be a null element in a list :-/
      Logger.getLogger(StorableTypDecisionReader.class).warn("No typename in column...return null");
      return null;
    }
    ResultSetReader typedReader = readers.get(typeName);
    if (typedReader == null) {
      throw new SQLException("Type " + typeName + " is unresolvable!");
    } else {
      return typedReader.read(rs);
    }
  }

}
