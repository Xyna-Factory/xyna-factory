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
package com.gip.xyna.utils.db.pool;

import java.sql.Connection;

import com.gip.xyna.utils.db.IConnectionFactory;


public class DefaultConnectionBuildStrategy implements ConnectionBuildStrategy {
  
  private IConnectionFactory connectionFactory;
  private String poolId;
  
  public DefaultConnectionBuildStrategy(IConnectionFactory connectionFactory) {
    this.connectionFactory = connectionFactory;
  }

  public void setName(String name) {
    this.poolId = "pool: " + name;
  }
  
  public Connection createNewConnection() {
    Connection con = connectionFactory.createNewConnection();
    if (con == null) {
      throw new RuntimeException("connectionfactory <" + connectionFactory + "> in connectionpool did not create connection but returned null.");
    }
    markConnectionNotInUse(con);
    return con;
  }
 
  public void markConnectionNotInUse(Connection con) {
    connectionFactory.markConnection(con, poolId);
  }

  public void markConnection(Connection con, String clientInfo) {
    connectionFactory.markConnection(con, clientInfo);
  }

}
