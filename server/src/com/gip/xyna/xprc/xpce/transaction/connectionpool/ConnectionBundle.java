/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xpce.transaction.connectionpool;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConnectionBundle implements Iterable<Connection> {

  private final Map<String, Connection> poolConnections;
  private final List<String> orderedPoolnames;
  
  ConnectionBundle() {
    poolConnections = new HashMap<>();
    orderedPoolnames = new ArrayList<>();
  }
  
  public void add(String conPoolName, Connection connection) {
    poolConnections.put(conPoolName, connection);
    orderedPoolnames.add(conPoolName);
  }

  public Iterator<Connection> iterator() {
    return poolConnections.values().iterator();
  }
  
  public Connection getConnection() {
    if (orderedPoolnames.size() > 0) {
      return poolConnections.get(orderedPoolnames.get(0));
    } else {
      return null;
    }
  }
  
  public Connection getConnection(int index) {
    if (orderedPoolnames.size() > index) {
      return poolConnections.get(orderedPoolnames.get(index));
    } else {
      return null;
    }
  }

  public Connection getConnection(String poolname) {
    return poolConnections.get(poolname);
  }
  
}