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
package com.gip.xyna.xprc.xsched.capacities;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
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
public class CMUnsupported implements CapacityManagementInterface {

  public enum Cause {
    Unitialized, DisconnectedSlave, UnconsideredClusterStateChange;
  }
  
  private Cause cause;
  private CapacityCache cache;
  protected Logger logger;
  
  public CMUnsupported(CapacityCache cache, Cause cause) {
    this.logger = CentralFactoryLogging.getLogger(getClass());
    this.cache = cache;
    this.cause = cause;
  }

  public CapacityAllocationResult allocateCapacities(OrderInformation orderInformation, SchedulingData schedulingData) {
    CMAbstract.internalCheckAllocate( orderInformation, schedulingData, this, logger );
    
    if( ! schedulingData.needsCapacities() ) {
      //Aufträge, die keine Caps brauchen, sollen laufen dürfen, damit Server korrekt heruntergefahren werden kann
      schedulingData.setHasAcquiredCapacities(true);
      return CapacityAllocationResult.SUCCESS;
    } else {
      Capacity first = schedulingData.getCapacities().get(0);
      return new CapacityAllocationResult(first.getCapName(),first.getCardinality(),0,true);
    }
  }
  
  public boolean transferCapacities(XynaOrderServerExtension xo, TransferCapacities transferCapacities) {
    throw new UnsupportedOperationException("transferCapacities is unsupported. Cause "+cause );
  }
  
  public boolean addCapacity(String name, int cardinality, State state) throws XPRC_CAPACITY_ALREADY_DEFINED,
                  PersistenceLayerException {
    throw new UnsupportedOperationException("addCapacity is unsupported. Cause "+cause );
  }

  public boolean changeCapacityName(String oldName, String newName) throws PersistenceLayerException {
    throw new UnsupportedOperationException("changeCapacityName is unsupported. Cause "+cause );
  }

  public boolean changeCardinality(String capName, int newOverallCardinality) throws PersistenceLayerException {
    throw new UnsupportedOperationException("changeCardinality is unsupported. Cause "+cause );
  }

  public boolean changeState(String capName, State newState) throws PersistenceLayerException {
    throw new UnsupportedOperationException("changeState is unsupported. Cause "+cause );
  }

  public void removeAllCapacities() throws PersistenceLayerException {
    throw new UnsupportedOperationException("removeAllCapacities is unsupported. Cause "+cause );
  }

  public boolean removeCapacity(String capName) throws PersistenceLayerException {
    throw new UnsupportedOperationException("removeCapacity is unsupported. Cause "+cause );
  }
  
  public boolean forceFreeCapacities(long orderId) {
    //Vollständig implementiert, damit vergebene Caps auch im Fehlerfall korrekt zurückgegeben 
    //werden können und der Cache somit valide bleibt
    return CMAbstract.internalForceFreeCapacities(orderId, cache, logger);
  }

  
  public boolean forceFreeCapacitiesLocally(long orderId) {
    return forceFreeCapacities(orderId); // We have no ownBinding here, maybe we want to throw an exception instead?
  }

  public boolean freeTransferableCapacities(XynaOrderServerExtension xo) {
    //Vollständig implementiert, damit vergebene Caps auch im Fehlerfall korrekt zurückgegeben 
    //werden können und der Cache somit valide bleibt
    return CMAbstract.internalFreeCapacities(xo, cache, logger, true, false);
  }
  
  public boolean freeCapacities(XynaOrderServerExtension xo) {
    //Vollständig implementiert, damit vergebene Caps auch im Fehlerfall korrekt zurückgegeben 
    //werden können und der Cache somit valide bleibt
    return CMAbstract.internalFreeCapacities(xo, cache, logger, false, false);
  }

  public void undoAllocation(OrderInformation orderInformation, SchedulingData schedulingData) {
    //Vollständig implementiert, damit vergebene Caps auch im Fehlerfall korrekt zurückgegeben 
    //werden können und der Cache somit valide bleibt
    CMAbstract.internalFreeCapacities(orderInformation, schedulingData, cache, logger, false, true);
  }

  public CapacityInformation getCapacityInformation(String capName) {
    throw new UnsupportedOperationException("getCapacityInformation is unsupported. Cause "+cause );
  }

  public ExtendedCapacityUsageInformation getExtendedCapacityUsageInformation() {
    return new ExtendedCapacityUsageInformation();
  }

  public List<CapacityInformation> listCapacities() {
    return Collections.emptyList();
  }
  
  public void close() {
    //nichts zu tun
  }

  public CapacityReservation getCapacityReservation() {
    return null;
  }

}
