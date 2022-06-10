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
package com.gip.xyna.xprc.xsched.vetos.cache.cluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheEntry;

public class ReplicateVetoRequest implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private int binding;
  private List<ReplicateVetoRequestEntry> entries;
  
  public enum Replication {
    Parked, Remote, Info, Scheduled;
  }
  
  public ReplicateVetoRequest(int binding) {
    this.binding = binding;
    this.entries = new ArrayList<>();
  }

  public void replicate(VetoCacheEntry veto) {
    Replication replication = null;
    switch( veto.getState() ) {
    case Free:      replication = Replication.Parked; break;
    case Local:     replication = Replication.Remote; break;
    case Compare:   replication = Replication.Remote; break;
    case None:      replication = Replication.Info; break;
    case Remote:    replication = Replication.Info; break;
    case Scheduled: replication = Replication.Scheduled; break;
    case Scheduling:replication = Replication.Remote; break;
    case Usable:    replication = Replication.Remote; break;
    case Used:      replication = Replication.Scheduled; break;
    default:        replication = Replication.Info; break;
    }
    if( replication == Replication.Scheduled ) {
      if( binding != veto.getBinding() ) {
        replication = Replication.Info;
      }
    }
    ReplicateVetoRequestEntry rvr = new ReplicateVetoRequestEntry(veto.getName(), replication);
    rvr.vetoInformation = veto.getVetoInformation();
    entries.add(rvr);
  }
  

  public int getBinding() {
    return binding;
  }

  public List<ReplicateVetoRequestEntry> getEntries() {
    return entries;
  }
  
  public static class ReplicateVetoRequestEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public VetoInformation vetoInformation;
    private String vetoName;
    private Replication replication;

    public ReplicateVetoRequestEntry(String vetoName, Replication replication) {
      this.vetoName = vetoName;
      this.replication = replication;
    }
    
    public String getVetoName() {
      return vetoName;
    }
    
    public VetoInformation getVetoInformation() {
      return vetoInformation;
    }
    
    public Replication getReplication() {
      return replication;
    }
  }
  
}
