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
package com.gip.xyna.xprc.xsched.vetos.cache.cluster;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheEntry;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoRequest.VetoRequestEntry.Action;

public class VetoRequest implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private long id;
  private int binding;
  private LinkedHashSet<VetoRequestEntry> entries;

  public VetoRequest(long id, int binding) {
    this.id = id;
    this.binding = binding;
    this.entries = new LinkedHashSet<>();
  }
  
  @Override
  public String toString() {
    return "VetoRequest("+id+","+binding+","+entries+")";
  }
  
  public long getId() {
    return id;
  }
  
  public int getBinding() {
    return binding;
  }
  
  public boolean isEmpty() {
    return entries.isEmpty();
  }
  
  public Set<VetoRequestEntry> getEntries() {
    return entries;
  }

  public VetoRequestEntry compare(VetoCacheEntry veto) {
    VetoRequestEntry vr = new VetoRequestEntry(veto, Action.Compare);
    vr.urgency = veto.getUrgency(); //TODO erst beim Serialisieren auswerten? dann ist der Wert aktueller..
    if( veto.isAdministrative() ) {
      vr.vetoInformation = veto.getVetoInformation();
    }
    return add(veto, vr);
  }
  
  public VetoRequestEntry free(VetoCacheEntry veto) {
    VetoRequestEntry vr = new VetoRequestEntry(veto, Action.Free);
    return add(veto, vr);
  }
  
  public VetoRequestEntry freeAdmin(VetoCacheEntry veto) {
    VetoRequestEntry vr = new VetoRequestEntry(veto, Action.Free);
    vr.urgency = Long.MAX_VALUE; //siehe VCP_Remote.processFree
    return add(veto, vr);
  }
  
  public VetoRequestEntry scheduled(VetoCacheEntry veto) {
    VetoRequestEntry vr = new VetoRequestEntry(veto, Action.Scheduled);
    vr.vetoInformation = veto.getVetoInformation();
    return add(veto, vr);
  }

  
  private VetoRequestEntry add(VetoCacheEntry veto, VetoRequestEntry vr) {
    if( entries.add(vr) ) {
      veto.getHistory().remoteActionStart(vr.getAction(), id, binding);
    }
    return vr;
  }


  public static class VetoRequestEntry implements Serializable {
    
    private static final long serialVersionUID = 1L;
   
    public enum Action {
      Compare, Scheduled, Free;
    }
   
    private final String name;
    private final Action action;
    private long urgency;
    private VetoInformation vetoInformation;
    
    public VetoRequestEntry(VetoCacheEntry veto, Action action) {
      this.name = veto.getName();
      this.action = action;
    }
    
    @Override
    public String toString() {
      return action+"("+name+","+urgency+")";
    }

    public Action getAction() {
      return action;
    }

    public String getName() {
      return name;
    }

    public long getUrgency() {
      return urgency;
    }

    public VetoInformation getVetoInformation() {
      return vetoInformation;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((action == null) ? 0 : action.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      VetoRequestEntry other = (VetoRequestEntry) obj;
      if (action != other.action)
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      return true;
    }

  }

}