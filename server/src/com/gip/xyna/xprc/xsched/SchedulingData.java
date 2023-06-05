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
package com.gip.xyna.xprc.xsched;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.gip.xyna.xfmg.xods.priority.PriorityManagement;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xpce.planning.Veto;
import com.gip.xyna.xprc.xsched.capacities.MultiAllocationCapacities;
import com.gip.xyna.xprc.xsched.capacities.TransferCapacities;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Start;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintData;
import com.gip.xyna.xprc.xsched.xynaobjects.SchedulerInformation;


/**
 * SchedulingData enthält Daten, die für das Scheduling der XynaOrder relevant sind.
 * 
 * TODO Diese Daten werden derzeit noch meist in der XynaOrder gespeichert, dort sollen
 * sie ausgetragen werden.
 */
public class SchedulingData implements Serializable {

  private static final long serialVersionUID = 1L;

  private int priority = -1; //-1 bedeutet, dass PriorityManagement noch richtige Priority ermitteln muss
  private List<String> vetos;
  private List<Capacity> capacities;
  private MultiAllocationCapacities multiAllocationCapacities;
  private TransferCapacities transferCapacities;
  private TimeConstraintData timeConstraintData;
  
  private volatile boolean needsToCheckTimeConstraintOnNextScheduling = true;
  
  private volatile boolean needsToAcquireCapacitiesOnNextScheduling = true;
  private volatile boolean needsToAcquireVetosOnNextScheduling = true;
  
  //beim serverstart wird das flag resettet und auf false gestellt (von {@link XynaProcessing.startPersistedSuspendedOrders()})
  private volatile boolean hasAcquiredCapacities = false;

  private boolean mustAcquireCapacitiesOnlyOnce = true;

  public SchedulingData(long entranceTimestamp) {
    //this.entranceTimestamp = entranceTimestamp;
    capacities = Collections.emptyList();
    vetos = Collections.emptyList();
    timeConstraintData = new TimeConstraintData(entranceTimestamp);
  }
  
  
  /**
   * Kopiert alle Felder (flach)
   * @param schedulingData
   * @return
   */
  public static SchedulingData copyOf(SchedulingData schedulingData) {
    SchedulingData copy = new SchedulingData(schedulingData.timeConstraintData.getEntranceTimestamp());
    if( schedulingData.vetos.isEmpty() ) {
      copy.vetos = Collections.emptyList(); 
    } else {
      copy.vetos = new ArrayList<String>(schedulingData.vetos);
    }
    copy.setCapacities(copy.capacities);
    copy.timeConstraintData.setDefinition(copy.timeConstraintData.getDefinition());
    copy.priority = schedulingData.priority;
    return copy;
  }

  
  
  public void setEntranceTimestamp(long entranceTimestamp) {
    timeConstraintData.setEntranceTimestamp(entranceTimestamp);
  }
  
  public void setTimeConstraint(TimeConstraint timeConstraint) {
    this.timeConstraintData.setDefinition(timeConstraint);
  }
  
  public TimeConstraint getTimeConstraint() {
    return timeConstraintData.getDefinition();
  }
  
  public void setNeedsToCheckTimeConstraintOnNextScheduling(boolean needsToCheckTimeConstraintOnNextScheduling) {
    this.needsToCheckTimeConstraintOnNextScheduling = needsToCheckTimeConstraintOnNextScheduling;
  }
  
  public boolean needsToCheckTimeConstraintOnNextScheduling() {
    return needsToCheckTimeConstraintOnNextScheduling;
  }

  public void setSchedulerBean(SchedulerBean sb) {
    setVetos(sb.unversionedGetVetos());
    setCapacities(sb.unversionedGetCapacities());
    if( sb instanceof SchedulerInformation ) {
      SchedulerInformation si = (SchedulerInformation)sb;
      if( si.getPriority() != null && si.getPriority().getPriority() != null ) {
        setPriority(si.getPriority().getPriority());
      }
      if( si.getTimeConstraint() != null ) {
        this.timeConstraintData.setDefinition( si.getTimeConstraint().toDefinition() );
      }
    }
  }

  private void setCapacities(List<? extends Capacity> caps) {
    if( caps.isEmpty() ) {
      capacities = Collections.emptyList();
    } else {
      capacities = new ArrayList<Capacity>(caps);
    }
  }

  private void setVetos(List<? extends Veto> vs) {
    if( vs.isEmpty() ) {
      vetos = Collections.emptyList();
    } else {
      vetos = new ArrayList<String>(vs.size());
      for( Veto v : vs ) {
        vetos.add( v.getVetoName() );
      }
    }
  }

  public List<Capacity> getCapacities() {
    return capacities;
  }
  
  public List<String> getVetos() {
    return vetos;
  }
  
  public boolean mustAcquireCapacitiesOnlyOnce() {
    return mustAcquireCapacitiesOnlyOnce ;
  }

  public void setMustAcquireCapacitiesOnlyOnce(boolean mustAcquireCapacitiesOnlyOnce) {
    this.mustAcquireCapacitiesOnlyOnce = mustAcquireCapacitiesOnlyOnce;
  }
  
  public TimeConstraintData getTimeConstraintData() {
    return timeConstraintData;
  }
  

  public static TimeConstraint legacyTimeConstraintFor(long entranceTimestamp,
                                                       long earliestStartTimestamp,
                                                       Long schedulingTimeout) {
    TimeConstraint_Start tc = null;
    if( entranceTimestamp == earliestStartTimestamp ) {
      tc = TimeConstraint.immediately();
    } else {
      if( earliestStartTimestamp == 0 ) {
        tc = TimeConstraint.immediately();
      } else {
        tc = TimeConstraint.at(earliestStartTimestamp);
      }
    }
    if( schedulingTimeout != null ) {
      tc = tc.withAbsoluteSchedulingTimeout(schedulingTimeout);
    }
    return tc;
  }

  
  public void setMultiAllocationCapacities(MultiAllocationCapacities multiAllocationCapacities) {
    this.multiAllocationCapacities = multiAllocationCapacities;
  }

  /**
   * @return the multiAllocationCapacities
   */
  public MultiAllocationCapacities getMultiAllocationCapacities() {
    return multiAllocationCapacities;
  }

  public void setTransferCapacities(TransferCapacities transferCapacities) {
    this.transferCapacities = transferCapacities;
  }
  
  public TransferCapacities getTransferCapacities() {
    return transferCapacities;
  }

  /**
   * @return
   */
  public boolean needsCapacities() {
    if( ! capacities.isEmpty() ) {
      return true;
    }
    if( multiAllocationCapacities != null ) {
      return ! multiAllocationCapacities.getCapacities().isEmpty();
    }
    return false;
  }
  
  public int getPriority() {
    return priority;
  }
  
  public void setPriority(int priority) {
    this.priority = PriorityManagement.restrictPriorityToThreadPriorityBounds(priority);
  }

  public boolean removeCapacity(String capName) {
    if( capacities.isEmpty() ) {
      return false;
    }
    Iterator<Capacity> iter = capacities.iterator();
    while( iter.hasNext() ) {
      Capacity cap = iter.next();
      if( capName.equals(cap.getCapName()) ) {
        iter.remove();
        return true;
      }
    }
    return false;
  }

  public boolean addOrChangeCapacity(String capName, int cardinality) {
    if( capacities.isEmpty() ) {
      capacities = new ArrayList<Capacity>(1);
      capacities.add( new Capacity(capName,cardinality));
      return true;
    }
    for( Capacity cap : capacities ) {
      if( capName.equals(cap.getCapName()) ) {
        if( cap.getCardinality() == cardinality ) {
          return false; //nichts zu ändern
        } else {
          cap.setCardinality(cardinality);
          return true;
        }
      }
    }
    //nicht gefunden, daher neu eintragen
    capacities.add( new Capacity(capName,cardinality));
    return true;
  }


  /**
   * sollen capacities beim nächsten scheduling entnommen werden 
   */
  public boolean isNeedsToAcquireCapacitiesOnNextScheduling() {
    return needsToAcquireCapacitiesOnNextScheduling;
  }


  /**
   * falls false, zählt das nur für das nächste scheduling. dann wird das flag wieder auf true gesetzt.
   * falls true, bleibt es true.
   * bewirkt, ob beim nächsten scheduling die capacities entnommen werden. 
   */
  public void setNeedsToAcquireCapacitiesOnNextScheduling(boolean needsToAcquireCapacitiesOnNextScheduling) {
    this.needsToAcquireCapacitiesOnNextScheduling = needsToAcquireCapacitiesOnNextScheduling;
  }


  public boolean isNeedsToAcquireVetosOnNextScheduling() {
    return needsToAcquireVetosOnNextScheduling;
  }


  public void setNeedsToAcquireVetosOnNextScheduling(boolean needsToAcquireVetosOnNextScheduling) {
    this.needsToAcquireVetosOnNextScheduling = needsToAcquireVetosOnNextScheduling;
  }


  public boolean isHasAcquiredCapacities() {
    return hasAcquiredCapacities;
  }


  public void setHasAcquiredCapacities(boolean hasAcquiredCapacities) {
    this.hasAcquiredCapacities = hasAcquiredCapacities;
  }
  
}
