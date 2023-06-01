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
package com.gip.xyna.coherence.management;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.CacheControllerImpl;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.coherencemachine.ObjectChangeEvent;


/**
 * Group of cluster nodes
 *
 */
public class ClusterMembers extends CoherencePayload {

  /**
   * sollte immer einer zweierpotenz sein (aus performancegründen - naja nicht wirklich tragisch, aber kostet nix, dass
   * so zu machen). die zahl dynamisch anzupassen ist gefährlich, weil man kollisionen von prioritäten bekommen könnte.
   * (alter request wartet mit einer priorität die nach der änderung von {@link #STATIC_MOD_NUMBER} auch von einem
   * anderen knoten als priorität angegeben werden könnte).
   */
  public static long STATIC_MOD_NUMBER = 256;


  private static final long serialVersionUID = 1L;
  private ClusterMember[] members;


  public ClusterMembers(ClusterMember startMember) {
    members = new ClusterMember[] {startMember};
  }


  public void setMembers(ClusterMember[] members) {
    this.members = members;
  }


  public ClusterMember[] getMembers() {
    return members;
  }


  public long getModNumber() {
    return STATIC_MOD_NUMBER;
  }
  
  @Override
  protected void onChange(ObjectChangeEvent event, CacheController controller) {
    final CacheControllerImpl ccImpl = (CacheControllerImpl) controller;
    if (event == ObjectChangeEvent.PUSH) {
      ccImpl.onEventNewMembers(members);
    }
    
  }
}
