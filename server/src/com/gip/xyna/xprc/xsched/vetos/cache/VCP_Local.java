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
package com.gip.xyna.xprc.xsched.vetos.cache;

import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;

public class VCP_Local extends VCP_Abstract {

  public VCP_Local(VetoCache vetoCache, VetoCachePersistence persistence) {
    super(vetoCache, persistence);
  }
  
  @Override
  protected boolean processCompare(VetoCacheEntry veto) {
    State next= null;
    if( veto.isAdministrative() ) {
      next = State.Scheduled;
    } else if( veto.getUrgency() != Long.MIN_VALUE ) {
      next = State.Local;
    } else {
      next = State.None;
    }
    if( veto.compareAndSetState(State.Compare, next)) {
      switch( next ) {
      case Scheduled:
        return processScheduled(veto);
      case Local:
        return true;
      case None:
        vetoCache.remove(veto, State.None);
        return false;
      default: //kann nicht vorkommen
        return false;
      }
    } else {
      return processAgain(veto, "not compare");
    }
  }
  
  @Override
  protected boolean processComparing(VetoCacheEntry veto) {
    //Nicht-Cluster-Betrieb: es darf kein Comparing geben -> daher entfernen
    return removeClusterState(veto, State.Comparing);
  }
  
  @Override
  protected boolean processRemote(VetoCacheEntry veto) {
    //Nicht-Cluster-Betrieb: es darf kein Remote geben -> daher entfernen
    return removeClusterState(veto, State.Remote);
  }
  
  @Override
  protected boolean processScheduled(VetoCacheEntry veto) {
    persist(veto, true);
    return false;
  }
  
  @Override
  protected boolean processFree(VetoCacheEntry veto) {
    persist(veto, false);
    return false;
  }
  
  @Override
  protected void processFreeAfterPersist(VetoCacheEntry veto) {
    if( veto.prepareCompare(State.Free, false) ) {
      veto.removeVetoInformation();
      processCompare(veto);
    } else {
      //unerwartet
      vetoCache.process(veto); //nochmal bearbeiten
    }
  }

  public VetoCacheEntry createNewVeto(String vetoName, long urgency) {
    return new VetoCacheEntry(vetoName, State.Usable, urgency);
  }

  @Override
  protected void appendImplInformation(StringBuilder sb) {
    sb.append("local");
  }

  private boolean removeClusterState(VetoCacheEntry veto, State expect) {
    if( veto.prepareCompare(expect, false) ) {
      return processCompare(veto);
    } else {
      return processAgain(veto, "not "+expect);
    }
  }

}
