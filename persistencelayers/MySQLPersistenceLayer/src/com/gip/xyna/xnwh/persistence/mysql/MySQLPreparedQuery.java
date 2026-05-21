/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.persistence.mysql;

import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;


public class MySQLPreparedQuery<T> implements PreparedQuery<T> {

  private Query<T> query;
  
  public MySQLPreparedQuery(Query<T> query) {
    this.query = query;
    String existingTableName = query.getTable();
    if(XynaProperty.QUERY_ESCAPE.get()) {
      this.query.modifyTargetTable(existingTableName, "`");
    } else {
      this.query.modifyTargetTable(existingTableName.toLowerCase());
    }
  }
  
  public ResultSetReader<? extends T> getReader() {
    return query.getReader();
  }

  public String getTable() {
    return query.getTable();
  }
  
  public Query<T> getQuery() {
    return query;
  }

}
