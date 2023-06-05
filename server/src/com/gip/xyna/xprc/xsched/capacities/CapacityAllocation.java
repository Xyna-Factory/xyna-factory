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
package com.gip.xyna.xprc.xsched.capacities;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.exceptions.XPRC_Scheduler_CapacityMissingException;
import com.gip.xyna.xprc.exceptions.XPRC_Scheduler_TooHighCapacityCardinalityException;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityManagement.CapacityProblemReaction;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache.CapacityEntry;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;


/**
 *
 */
public class CapacityAllocation {
  private static Logger logger = CentralFactoryLogging.getLogger(CapacityAllocation.class);
  private String capName;
  private CapacityEntry capacityEntry;
  private int demand;
  private boolean skipCheckAndAllocation = false;

  public CapacityAllocation(Capacity cap) {
    this.capName = cap.getCapName();
    this.demand = cap.getCardinality();
  }
 
  /**
   * @param cap
   * @param previouslyAllocated
   */
  public CapacityAllocation(Capacity cap, Integer previouslyAllocated) {
    this.capName = cap.getCapName();
    if( previouslyAllocated != null ) {
      this.demand = Math.max(0, cap.getCardinality()-previouslyAllocated); //sollte nicht negativ werden, 
      //überzählige Caps werden nicht freigegeben
    } else {
      this.demand = cap.getCardinality();
    }
  }

  public CapacityAllocationResult initCache(CapacityCache cache) {
    capacityEntry = cache.get( capName );
    
    if( capacityEntry == null ) {
      logger.warn("XynaOrder required unknown capacity '" + capName + "'");
      CapacityProblemReaction cpr = XynaProperty.SCHEDULER_UNDEFINED_CAPACITY_REACTION.get();
      switch( cpr ) {
        case Wait:
          return new CapacityAllocationResult(capName,demand,0,true);
        case Schedule:
          skipCheckAndAllocation = true;
          break;
        case Fail:
          return new CapacityAllocationResult(capName, new XPRC_Scheduler_CapacityMissingException(capName) );
      }
    }
    return null;
  }

  

  public CapacityAllocationResult checkAllocationPossible() {
    if( skipCheckAndAllocation ) {
      return null; //Check überpringen, wird als erfolgreich gewertet
    }
    if( capacityEntry.isDisabled() ) {
      if (logger.isDebugEnabled()) {
        logger.debug("Allocation of capacity in state '" + State.DISABLED.toString() + "' blocked.");
      }
      return new CapacityAllocationResult(capName,demand,0,true);
    }
    
    if( ! capacityEntry.checkAllocationPossible(demand) ) {
      int numFree = capacityEntry.getNumberOfFreeCapsForScheduling();
      if( demand > capacityEntry.getTotalCardinality() ) {
        logger.warn("XynaOrder requires more capacities "+demand+ " of type '"+capName+"' than possible "+capacityEntry.getTotalCardinality() );
        CapacityProblemReaction cpr = XynaProperty.SCHEDULER_UNSUFFICIENT_CAPACITY_REACTION.get();
        switch( cpr ) {
          case Wait:
            return new CapacityAllocationResult(capName, 0, numFree, true); //ohne Demand -> CapacityDemand wird nicht voll und 
            //verhindert nicht die weitere Iteration
          case Schedule:
            if( numFree == capacityEntry.getTotalCardinality() ) {
              demand = numFree; //Demand wird verringert!
            } else {
              capacityEntry.reserveAllRemainingCaps();
              return new CapacityAllocationResult(capName, demand-numFree, 0, false);
            }
            break;
          case Fail:
            return new CapacityAllocationResult(capName, new XPRC_Scheduler_TooHighCapacityCardinalityException(capName, demand) ); 
        }
      } else {
        capacityEntry.reserveAllRemainingCaps();
        return new CapacityAllocationResult(capName, demand-numFree, 0, false);
      }
    }
 
    return null;
  }

  public void allocate(OrderInformation orderInformation, boolean transferable) {
    if( skipCheckAndAllocation ) {
      return; //Allocation überpringen
    }
    capacityEntry.allocate( demand, orderInformation, transferable);
  }
  

}
