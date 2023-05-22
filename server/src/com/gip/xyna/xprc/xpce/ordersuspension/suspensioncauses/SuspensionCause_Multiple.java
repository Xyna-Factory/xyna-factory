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
package com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses;

import java.util.ArrayList;
import java.util.HashSet;

import com.gip.xyna.utils.collections.CounterMap;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode;


/**
 *
 */
public class SuspensionCause_Multiple extends SuspensionCause {
  private static final long serialVersionUID = 1L;

  private boolean needToFreeCapacities = true;
  private boolean needToFreeVetos = true;
  private final HashSet<String> nameSet = new HashSet<String>();
  private final ArrayList<String> laneIds = new ArrayList<String>();
  private transient CounterMap<String> counterMap = new CounterMap<String>();
  
  @Override
  public String getName() {
    String name = nameSet.toString();
    return name.substring(1,name.length()-1);
  }

  public void addCause(SuspensionCause suspensionCause) {
    if (suspensionCause == null) {
      return;
    }
    if( suspensionCause instanceof SuspensionCause_Multiple ) {
      SuspensionCause_Multiple scm = (SuspensionCause_Multiple)suspensionCause;
      nameSet.addAll( scm.nameSet );
      laneIds.addAll( scm.laneIds );
      if (scm.counterMap != null) {
        //FIXME da die map transient ist, ist es zu erwarten, dass sie hier null ist, nachdem deserialisiert wurde.
        //da die eintr�ge aus der map nur zu debugzwecken existieren, ist das erstmal nicht weiter schlimm
        counterMap.add( scm.counterMap );
      }
    } else {
      nameSet.add(suspensionCause.getName());
      laneIds.add(suspensionCause.getLaneId());
      counterMap.increment(suspensionCause.getName());
    }
    
    //nur dann Capacities/Vetos freigeben, wenn alle SuspensionCauses das erfordern.
    if( needToFreeCapacities ) { //bleibt nur true, wenn alle SuspensionCauses true sind
      needToFreeCapacities = suspensionCause.needToFreeCapacities();
    }
    if( needToFreeVetos ) { //bleibt nur true, wenn alle SuspensionCauses true sind
      needToFreeVetos = suspensionCause.needToFreeVetos();
    }
    if (orderBackupMode == null) {
      orderBackupMode = suspensionCause.orderBackupMode;
    } else {
      orderBackupMode = SuspensionBackupMode.combine(orderBackupMode, suspensionCause.orderBackupMode);
    }
  }
  
  public boolean needToFreeCapacities() {
    return needToFreeCapacities;
  }
  
  public boolean needToFreeVetos() {
    return needToFreeVetos;
  }

  public boolean containsCause(String name) {
    return nameSet.contains(name);
  }

  public int size() {
    return laneIds.size();
  }
  
  public String getNameCounted() {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for( String entry : counterMap.keySet() ) {
      sb.append(sep);
      sep =",";
      int count = counterMap.getCount(entry);
      if( count != 1 ) {
        sb.append(count).append("*");
      }
      sb.append(entry);
    }
    return sb.toString();
  }
  
}
