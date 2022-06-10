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

import java.util.Map;

import com.gip.xyna.utils.db.ConnectionPool.ThreadInformation;

public class ConnectionPoolInformation {
  
  private final String name;
  private final int size;
  private final boolean isDynamic;
  private final int used;
  private final String state;
  private final String poolidentity;
  private final Map<String, Integer> sqlStats;
  private final ThreadInformation[] waitingThreadInformation;
  private final String pooltype;
  private final PoolDefinition poolDefinition;
  
  
  protected ConnectionPoolInformation(String name, String pooltype, int size, boolean isDynamic, int used, String state, String poolidentitiy, 
                                      Map<String, Integer> sqlStats, ThreadInformation[] waitingThreadInformation, PoolDefinition poolDefinition) {
    this.name = name;
    this.size = size;
    this.isDynamic = isDynamic;
    this.used = used;
    this.state = state;
    this.poolidentity = poolidentitiy;
    this.sqlStats = sqlStats;
    this.waitingThreadInformation = waitingThreadInformation;
    this.pooltype = pooltype;
    this.poolDefinition = poolDefinition;
  }
  
  public String getName() {
    return name;
  }

  public int getSize() {
    return size;
  }
  
  public int getUsed() {
    return used;
  }
  
  public String getState() {
    return state;
  }

  public String getPoolidentity() {
    return poolidentity;
  }
  
  public String getPooltype() {
    return pooltype;
  }
  
  public Map<String, Integer> getSqlStats() {
    return sqlStats;
  }

  public ThreadInformation[] getWaitingThreadInformation() {
    return waitingThreadInformation;
  }
  
  public PoolDefinition getPoolDefinition() {
    return poolDefinition;
  }

  public boolean isDynamic() {
    return isDynamic;
  }
  
}
