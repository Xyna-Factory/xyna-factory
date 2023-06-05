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
package com.gip.xyna.xprc.xsched.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.OrderedQueue;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.capacities.CapacityAllocationResult;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache;


/**
 * Diese Implementation kann mit einem anderen Scheduler Informationen austauschen, damit 
 * die Capacities so gerecht verteilt werden koennen, so dass die Abarbeitung aller Auftraege 
 * insgesamt wenig Scheduling-Reihenfolgen-Fehler aufweist.
 * Dazu wird Klasse CapacityDemand und die CapacityReservation-Implementation im Zusammenspiel 
 * mit entsprechend angepasstem XynaScheduler und CapacityManagement verwendet.
 */
public class XynaSchedulerCustomisationCapacities {

  
  private static Logger logger = CentralFactoryLogging.getLogger(XynaSchedulerCustomisationCapacities.class);
  
  private OrderedQueue<CapacityDemandForNode> allForeignCapacityDemands; //sortierte List der fremden Capacity-Forderungen zur Abarbeitung
  private CapacityReservation capacityReservation; //Algorithmus zum Reservieren einer Capacity fuer einen fremden Knoten
  private List<CapacityDemandForNode> unsatisfiedOldCapacityDemands;
  private CapacityCache capacityCache;

  
  public XynaSchedulerCustomisationCapacities(
                                              CapacityCache capacityCache, 
                                              CapacityReservation capacityReservation) {
    this.capacityCache = capacityCache;
    setCapacityReservation(capacityReservation);
    allForeignCapacityDemands = new OrderedQueue<CapacityDemandForNode>();
    unsatisfiedOldCapacityDemands = new ArrayList<CapacityDemandForNode>();
  }
  
  public void setCapacityReservation(CapacityReservation capacityReservation) {
    if( capacityReservation == null ) {
      this.capacityReservation = new DefaultCapacityReservation();
    } else {
      this.capacityReservation = capacityReservation;
    }
  }
  
  private static class DefaultCapacityReservation implements CapacityReservation {

    public int reserveCap(int binding, Capacity capacity) {
      return 0;
    }
    
    public int transportReservedCaps() {
      return 0;
    }
    
    public boolean addDemand(CapacityAllocationResult car, long urgency) {
      if( car.getDemand() > 0 ) {
        return true; //auf Cap wartende UrgencyOrders werden sofort übersprungen
      } else {
        return false; //dies sollte eigentlich nur vorkommen, wenn Auftrag mehr 
        //Capacities fordert als vorhanden sind 
      }
    }

    public List<CapacityDemand> communicateOwnDemand() {
      return Collections.emptyList();
    }

    public void setForeignDemand(int binding, List<CapacityDemand> demand) {
      //ignorieren
    }

    public Map<Integer, List<CapacityDemand>> getForeignDemands() {
      return Collections.emptyMap();
    }

    public void listExtendedSchedulerInfo(StringBuilder sb, List<CapacityDemandForNode> unsatisfiedOldCapacityDemands) {
      //nichts auszugeben
    }

    public void refreshCapacity(String capName) {
      //ignorieren
    }

  }

  public void capacityReservation(SchedulerInformationBean information) {
    //falls lokal keine Aufträge vorliegen, sollen trotzdem Capacities für andere Knoten reserviert werden
    reserveCapsForOtherNodes(Long.MIN_VALUE);

    if (logger.isTraceEnabled()) {
      logger.trace("Foreign capacity demands after final reserveration: " + allForeignCapacityDemands);
    }

    //eigenen Capacity-Bedarf an die anderen Knoten melden
    capacityReservation.communicateOwnDemand();

    //nun können die gesammelten Capacity-Reservierungen übertragen werden
    int transportedCaps = capacityReservation.transportReservedCaps();
    information.setLastTransportedCaps(transportedCaps);

  }

  /**
   * Versucht, Capacities fuer andere Knoten zu reservieren, falls deren Dringlichkeit hoeher ist als die Dringlichkeit
   * urgency der naechsten XynaOrder
   */
  public void reserveCapsForOtherNodes(long urgency) {
    if (logger.isTraceEnabled()) {
      logger.trace("Reserving capacities for other nodes (" + urgency + ")");
      if( !allForeignCapacityDemands.isEmpty() ) {
        long other = allForeignCapacityDemands.peek().getMaxUrgency();
        logger.trace("compare urgency: own="+urgency+", other="+other +" -> diff="+(other-urgency) );
      }
    }

    while (!allForeignCapacityDemands.isEmpty() && allForeignCapacityDemands.peek().getMaxUrgency() > urgency) {
      //anderer Knoten meldet hoeheren Bedarf an
      CapacityDemandForNode demand = allForeignCapacityDemands.peek();
      
      int reserved = 0;
      if( capacityReservation != null ) {
        int cardinality = 1;
        if( urgency == Long.MIN_VALUE ) {
          cardinality = demand.getCount(); //kompletten restlichen Bedarf decken
        }
        reserved = capacityReservation.reserveCap(demand.getNode(),new Capacity(demand.getCapName(),cardinality));
      }
      
      if( reserved > 0 ) {
        demand.decrement(reserved); //aendert Sortierkriterium, weil urgency neu berechnet wird
        if( demand.isFullfilled() ) {
          //Bedarf ist gestillt, daher demand aus otherCapacityDemands entfernen
          allForeignCapacityDemands.poll();
        } else {
          if( urgency == Long.MIN_VALUE ) {
            //diese Capacity ist derzeit nicht mehr frei, daher bringt
            //ein weiterer Versuch, diese Cap zu reservieren nichts mehr
            //-> demand aus otherCapacityDemands entfernen, aber trotzdem aufheben
            allForeignCapacityDemands.poll();
            unsatisfiedOldCapacityDemands.add(demand);
          } else {
            //demand anpassen und wieder in die sortierte Liste packen
            allForeignCapacityDemands.refresh();
          }
        }
      } else {
        //diese Capacity ist derzeit nicht mehr frei, daher bringt
        //ein weiterer Versuch, diese Cap zu reservieren nichts mehr
        //-> demand aus otherCapacityDemands entfernen, aber trotzdem aufheben
        allForeignCapacityDemands.poll();
        unsatisfiedOldCapacityDemands.add(demand);
      }
    }
    
    if (logger.isTraceEnabled()) {
      logger.trace("Foreign capacity demand after reservation: " + allForeignCapacityDemands);
    }
  }

  public void setCurrentSchedulingRun(long totalSchedulerRuns) {
    capacityCache.setCurrentSchedulingRun(totalSchedulerRuns);
  }

  /**
   * Sammeln der Capacity-Forderungen der anderen Knoten
   */
  public void gatherDemands() {
    for (CapacityDemandForNode cdfn : unsatisfiedOldCapacityDemands) {
      allForeignCapacityDemands.offer(cdfn);
    }
    unsatisfiedOldCapacityDemands.clear();
    Map<Integer, List<CapacityDemand>> demandListMap = capacityReservation.getForeignDemands();
    
    for (Map.Entry<Integer, List<CapacityDemand>> entry : demandListMap.entrySet()) {
      int node = entry.getKey();

      for (CapacityDemand cd : entry.getValue()) {
        if (cd.isFullfilled()) {
          allForeignCapacityDemands.remove(new CapacityDemandForNode(node, cd.getCapName()));
          if (logger.isDebugEnabled()) {
            logger.debug("no demand from node " + node + " for cap " + cd.getCapName() + " any longer");
          }
        } else {
          try {
            CapacityDemandForNode newCdfn = new CapacityDemandForNode(node, cd);
            allForeignCapacityDemands.remove(newCdfn); //equals enthält nicht alle Attribute!
            allForeignCapacityDemands.add(newCdfn);
          } catch (IllegalArgumentException e) {
            logger.warn(cd + " invalid: " + e.getMessage() + "; will be ignored");
          }
        }
      }
    }
    if (logger.isDebugEnabled()) {
      if( allForeignCapacityDemands.isEmpty() ) {
        if (logger.isTraceEnabled()) {
          logger.trace("otherCapacityDemands " + allForeignCapacityDemands);
        }
      } else {
        logger.debug("otherCapacityDemands " + allForeignCapacityDemands);
      }
    }
  }

  public void listExtendedSchedulerInfo(StringBuilder sb) {
    capacityReservation.listExtendedSchedulerInfo(sb, unsatisfiedOldCapacityDemands );
  }

  public void setUnsatisfiedForeignDemand(SchedulerInformationBean information) {
    information.setUnsatisfiedForeignDemand( unsatisfiedOldCapacityDemands.size() );
  }

  /**
   * @param car
   * @param urgency
   * @return true, falls maximaler Demand erreicht wurde
   */
  public boolean addDemand(CapacityAllocationResult car, long urgency) {
    return capacityReservation.addDemand(car, urgency);
  }

  
}
