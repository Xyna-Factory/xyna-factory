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
package com.gip.juno.ws.tools.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.gip.juno.ws.enums.VirtualCol;
import com.gip.juno.ws.handler.TableHandler;
import com.gip.juno.ws.tools.ColInfo;
import com.gip.xyna.utils.db.ResultSetReader;


public class TableHandlerBasedRowMapReader implements ResultSetReader<TreeMap<String, String>> {

  private final TableHandler handler;
  
  public TableHandlerBasedRowMapReader(TableHandler handler) {
    this.handler = handler;
  }
  
  @Override
  public TreeMap<String, String> read(ResultSet rs) throws SQLException {
    TreeMap<String, String> map = new TreeMap<String, String>();
    for (Entry<String, ColInfo> column : handler.getDBTableInfo().getColumns().entrySet()) {
      if (column.getValue().virtual == VirtualCol.False) {
        String value = rs.getString(column.getKey());
        if (!rs.wasNull()) {
          map.put(column.getKey(), value);
        }
      }
    }
    return map;
  }

}
