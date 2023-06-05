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

import java.util.List;
import java.util.Map;

import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.capacities.CapacityAllocationResult;


/**
 *
 */
public interface CapacityReservation {
  
  /**
   * Reservieren einer Capacity für ein fremdes Binding. Die Capacity steht dann lokal nicht mehr zu Verfügung,
   * ist aber noch nicht an den anderen Knoten übertragen.
   * @param binding
   * @param capacity
   * @return true, wenn Capacity reserviert werden konnte
   */
  int reserveCap(int binding, Capacity capacity);
  
  /**
   * Übertragen aller reservierten Capacities an die anderen Knoten
   * @return Anzahl aller übertragenen Capacities
   */
  public int transportReservedCaps();
  
  /**
   * @param car
   * @return true, falls maximaler Demand erreicht wurde
   */
  public boolean addDemand(CapacityAllocationResult car, long urgency);

  /**
   * Kommuniziert den eigenen Capacity-Bedarf, der zuvor mit addDemand erzeugt wurde.
   * Nach dam Senden wird der eigene Capacity-Bedarf wieder geleert.
   * @return gesendeter CapacityDemand
   */
  List<CapacityDemand> communicateOwnDemand();

  /**
   * Eintragen der Demands der anderen Knoten
   * @param binding
   * @param demand
   */
  void setForeignDemand(int binding, List<CapacityDemand> demand);

  /**
   * Ausgabe der Demands der anderen Knoten
   * @return
   */
  Map<Integer, List<CapacityDemand>> getForeignDemands();

  
  /**
   * Ausgabe von ausführlichen Informationen
   * @param sb
   * @param unsatisfiedOldCapacityDemands 
   */
  void listExtendedSchedulerInfo(StringBuilder sb, List<CapacityDemandForNode> unsatisfiedOldCapacityDemands);

  /**
   * Capacity hat sich im Cache geändert
   * @param capName
   */
  void refreshCapacity(String capName);

}
