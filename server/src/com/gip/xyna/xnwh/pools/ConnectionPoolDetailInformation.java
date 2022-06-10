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
package com.gip.xyna.xnwh.pools;

import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.db.ConnectionPool.ThreadInformation;

public class ConnectionPoolDetailInformation extends ConnectionPoolInformation {
  
  private final List<ConnectionInformation> connectionInformation;
  
  protected ConnectionPoolDetailInformation(String name, String type,  int size, boolean canDynamicGrow, int used,  String state, String poolidentitiy, Map<String, Integer> sqlStats, ThreadInformation[] waitingThreadInformation, PoolDefinition poolDefinition, List<ConnectionInformation> connectionInformation) {
    super(name, type, size, canDynamicGrow, used, state, poolidentitiy, sqlStats, waitingThreadInformation,poolDefinition);
    this.connectionInformation = connectionInformation;
  }

  public List<ConnectionInformation> getConnectionInformation() {
    return connectionInformation;
  }
  
}
