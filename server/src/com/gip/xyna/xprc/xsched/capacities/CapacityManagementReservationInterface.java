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

import java.util.List;

import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache.CapacityEntryInformation;
import com.gip.xyna.xprc.xsched.scheduling.CapacityDemand;


/**
 * Interface, über das die CapacityReservation auf das CapacityManagement zugreift.
 * 
 */
public interface CapacityManagementReservationInterface {

  /**
   * Rückgabe des eigenen Bindings, damit bekannt ist, für welches Binding Capacities angefordert werden sollen
   * @return
   */
  public int getOwnBinding();

  /**
   * Reservieren einer Capacity für ein fremdes Binding. Die Capacity steht dann lokal nicht mehr zu Verfügung,
   * ist aber noch nicht an den anderen Knoten übertragen.
   * @param binding
   * @param capacity
   * @return
   */
  public int reserveCapForForeignBinding(int binding, Capacity capacity);

  /**
   * Übertragen aller reservierten Capacities an die anderen Knoten
   * @return Anzahl aller übertragenen Capacities
   */
  public int transportReservedCaps();

  /**
   * Ausgabe aller Information zur Capacity aus dem CapacityCache
   * @param capName
   * @return
   */
  CapacityEntryInformation getCapacityEntryInformation(String capName);

  
  public boolean communicateDemand(int binding, List<CapacityDemand> demand);

}
