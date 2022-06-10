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

import com.gip.xyna.utils.db.ConnectionPool.ThreadInformation;

public class ConnectionInformation {
  
  private final boolean used;
  private final long lastAquired;
  private final long lastValidated;
  private final long lastCommit;
  private final long lastRollback;
  private final long countUses;
  private final boolean lastCheckSuccessfull; 
  private final String lastSQL;
  private final ThreadInformation currentThreadInformation;
  private final StackTraceElement[] openingStackTrace;
  
  protected ConnectionInformation(boolean used, long lastAquired, long lastValidated, long lastCommit, long lastRollback, long countUses, boolean lastCheckSuccessfull, String lastSQL, ThreadInformation currentThreadInformation,  StackTraceElement[] openingStackTrace) {
    this.used = used;
    this.lastAquired = lastAquired;
    this.lastValidated = lastValidated;
    this.lastCommit = lastCommit;
    this.lastRollback = lastRollback;
    this.countUses = countUses;
    this.lastSQL =lastSQL;
    this.currentThreadInformation = currentThreadInformation;
    this.openingStackTrace = openingStackTrace;
    this.lastCheckSuccessfull = lastCheckSuccessfull;
  }
  
  public boolean isUsed() {
    return used;
  }
  
  public long getLastAquired() {
    return lastAquired;
  }
  
  public long getLastValidated() {
    return lastValidated;
  }
  
  public long getLastCommit() {
    return lastCommit;
  }
  
  public long getLastRollback() {
    return lastRollback;
  }
  
  public long getCountUses() {
    return countUses;
  }
  
  public boolean wasLastCheckSuccessfull() {
    return lastCheckSuccessfull;
  }
  
  public String getLastSQL() {
    return lastSQL;
  }
  
  public ThreadInformation getCurrentThreadInformation() {
    return currentThreadInformation;
  }

  public StackTraceElement[] getOpeningStackTrace() {
    return openingStackTrace;
  }
  
}
