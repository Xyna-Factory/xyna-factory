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
package com.gip.xyna.xprc.xsched.capacities;

import java.util.List;

import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_ClusterStateChangedException;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.scheduling.CapacityReservation;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;


/**
 *
 */
public interface CapacityManagementInterface {

  /**
   * Versucht, die Capacities f�r die �bergebene Order zu belegen.
   * Kann mehrfach gerufen werden, loggt dann aber eine Meldung auf WARN.
   * @param orderInformation
   * @param schedulingData
   * @return CapacityAllocationResult
   */
  public CapacityAllocationResult allocateCapacities(OrderInformation orderInformation, SchedulingData schedulingData);
  
  /**
   * Macht die Belegungen des letzten {@link #allocateCapacities(OrderInformation, SchedulingData)} r�ckg�ngig.
   * @param orderInformation
   * @param schedulingData
   */
  public void undoAllocation(OrderInformation orderInformation, SchedulingData schedulingData);
  
  /**
   * Gibt alle transferierbaren Capacities wieder frei
   * @param xo
   */
  public boolean freeTransferableCapacities(XynaOrderServerExtension xo); 

  /**
   * Versucht, die Capacities f�r die �bergebene XynaOrder freizugeben.
   * Kann mehrfach gerufen werden, loggt dann aber eine Meldung auf DEBUG.
   * @param xo
   * @return
   */
  public boolean freeCapacities(XynaOrderServerExtension xo);
  
  /**
   * Transferiert die Capacities in transferCapacities zu Order xo
   * @param xo
   * @param transferCapacities
   * @return
   */
  public boolean transferCapacities(XynaOrderServerExtension xo, TransferCapacities transferCapacities );
  

  
  /**
   * Gibt alle Capacities f�r die �bergebene XynaOrder frei.
   * Kann mehrfach gerufen werden.
   * @param orderId
   * @return
   */
  public boolean forceFreeCapacities(long orderId);

  
  public boolean addCapacity(String name, int cardinality, State state) throws XPRC_CAPACITY_ALREADY_DEFINED, PersistenceLayerException;

  /**
   * Change the name of a capacity
   * @param oldName name identifying the capacity before the transition
   * @param newName name identifying the capacity after the transition
   * @return true if the capacity existed and the name could be changed and false otherwise
   * @throws XPRC_ClusterStateChangedException 
   */
  public boolean changeCapacityName(String oldName, String newName) throws PersistenceLayerException, XPRC_ClusterStateChangedException;
 
  /**
   * Change the cardinality of a capacity
   * @param capName identifying the capacity
   * @param newOverallCardinality after the transition
   * @return true if the capacity existed and the cardinality could be changed and false otherwise
   * @throws XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain 
   * @throws XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState 
   * @throws XPRC_ClusterStateChangedException 
   */
  public boolean changeCardinality(String capName, int newOverallCardinality) throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain, XPRC_ClusterStateChangedException;

  /**
   * Changes the state of a capacity
   * @param capName identifying the capacity
   * @param newState the target state after the transition
   * @return true, if the capacity exists and the state could be changed and false otherwise
   */
  public boolean changeState(String capName, State newState) throws PersistenceLayerException;
  
  /**
   * Removes a capacity based on its name
   * @return true, if the capacity existed and could be removed and false otherwise
   * @throws XPRC_ClusterStateChangedException 
   */
  public boolean removeCapacity(String capName) throws PersistenceLayerException, XPRC_ClusterStateChangedException;

  public void removeAllCapacities() throws PersistenceLayerException;
     
  public CapacityInformation getCapacityInformation(String capName) throws XPRC_ClusterStateChangedException;
  
  public ExtendedCapacityUsageInformation getExtendedCapacityUsageInformation() throws XPRC_ClusterStateChangedException;
  
  public List<CapacityInformation> listCapacities() throws XPRC_ClusterStateChangedException;
  
  public void close();

  public CapacityReservation getCapacityReservation();
  
}
