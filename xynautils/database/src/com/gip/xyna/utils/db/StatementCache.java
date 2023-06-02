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
package com.gip.xyna.utils.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

public class StatementCache {

  private HashMap<String,PreparedStatement> statementCache = new HashMap<String,PreparedStatement>();

  
  /**
   * Cachet das PreparedStatement zur späteren Wiederverwendung
   * Achtung: nicht für Callable-Statements verwendbar
   * @param connection 
   * @param sql
   * @throws SQLException 
   */
  public void cache(Connection connection, String sql) throws SQLException {
    if( ! statementCache.containsKey( sql) ) {
      statementCache.put( sql, connection.prepareStatement(sql) );
    }
    
  }


  public PreparedStatement getPreparedStatement( String sql ) {
    return statementCache.get( sql );
  }


  public boolean contains(PreparedStatement stmt) {
    return statementCache.containsValue( stmt );
  }


  public void close() {
    for( PreparedStatement ps : statementCache.values() ) {
      try { ps.close(); } catch( SQLException e ) {};
    }
    statementCache.clear();
  }

}
