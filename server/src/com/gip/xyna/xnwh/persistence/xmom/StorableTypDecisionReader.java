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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;

public class StorableTypDecisionReader implements ResultSetReader<Object> {

  private static final Logger logger = CentralFactoryLogging.getLogger(StorableTypDecisionReader.class);
  
  private Map<String, ResultSetReader<? extends Object>> readers = new HashMap<>();
  private String typenameColumn;
  private String tableName;
  
  public StorableTypDecisionReader(String typenameColumn, Collection<StorableStructureInformation> structures, String tableName) {
    this.typenameColumn = typenameColumn;
    this.tableName = tableName;
    for (StorableStructureInformation structure : structures) {
      readers.put(structure.fqClassNameOfDatatype, structure.getResultSetReaderForDatatype());
    }
  }

  
  public Object read(ResultSet rs) throws SQLException {
    String typeName = rs.getString(typenameColumn);
    if (typeName == null) {
      // TODO we might be a null element in a list :-/
      if(logger.isWarnEnabled()) {
        logger.warn(String.format("No typename in column %s of table %s...return null", typenameColumn, tableName));
      }
      return null;
    }
    ResultSetReader<? extends Object> typedReader = readers.get(typeName);
    if (typedReader == null) {
      String expected = String.join(", ", readers.keySet());
      throw new SQLException("Type " + typeName + " is unresolvable in table" + tableName + "! Expected one of: { " + expected + " }.");
    } else {
      return typedReader.read(rs);
    }
  }

}
