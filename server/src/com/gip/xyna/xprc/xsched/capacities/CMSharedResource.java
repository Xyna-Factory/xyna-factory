/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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



import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_ClusterStateChangedException;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.capacities.CapacityCache.CapacityEntry;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.scheduling.CapacityReservation;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;



public class CMSharedResource implements CapacityManagementInterface {

  public static final SharedResourceDefinition<SharedResourceCapacity> XYNA_CAP_SR_DEF =
      new KryoSerializedSharedResourceDefinition<>(CapacityManagement.XYNA_CAPACITY_SR, SharedResourceCapacity.class);

  private static final Logger logger = CentralFactoryLogging.getLogger(CMSharedResource.class);
  private final SharedResourceManagement srm;


  public CMSharedResource() {
    srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
  }


  @Override
  public CapacityAllocationResult allocateCapacities(OrderInformation orderInformation, SchedulingData schedulingData) {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public void undoAllocation(OrderInformation orderInformation, SchedulingData schedulingData) {
    // TODO Auto-generated method stub

  }


  @Override
  public boolean freeTransferableCapacities(XynaOrderServerExtension xo) {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean freeCapacities(XynaOrderServerExtension xo) {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean transferCapacities(XynaOrderServerExtension xo, TransferCapacities transferCapacities) {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean forceFreeCapacities(long orderId) {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean addCapacity(String name, int cardinality, State state) throws XPRC_CAPACITY_ALREADY_DEFINED, PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean changeCapacityName(String oldName, String newName) throws PersistenceLayerException, XPRC_ClusterStateChangedException {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean changeCardinality(String capName, int newOverallCardinality)
      throws PersistenceLayerException, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
      XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain, XPRC_ClusterStateChangedException {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean changeState(String capName, State newState) throws PersistenceLayerException {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public boolean removeCapacity(String capName) throws PersistenceLayerException, XPRC_ClusterStateChangedException {
    // TODO Auto-generated method stub
    return false;
  }


  @Override
  public void removeAllCapacities() throws PersistenceLayerException {
    // TODO Auto-generated method stub

  }


  @Override
  public CapacityInformation getCapacityInformation(String capName) throws XPRC_ClusterStateChangedException {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public ExtendedCapacityUsageInformation getExtendedCapacityUsageInformation() {
    SharedResourceRequestResult<SharedResourceCapacity> readResult = srm.readAll(XYNA_CAP_SR_DEF);
    if (!readResult.isSuccess() || readResult.getResources() == null) {
      logger.error("could not read capacities.", readResult.getException());
    }
    int privateCapacityIndex = 0;
    ExtendedCapacityUsageInformation ecui = new ExtendedCapacityUsageInformation();
    for (SharedResourceInstance<SharedResourceCapacity> entry : readResult.getResources()) {
      List<CapacityUsageSlotInformation> cusiList = convert(entry, privateCapacityIndex);
      for(CapacityUsageSlotInformation cusi : cusiList) {
        ecui.addSlotInformation(privateCapacityIndex, cusi);
      }
      ++privateCapacityIndex;
    }
    return ecui;
  }


  private List<CapacityUsageSlotInformation> convert(SharedResourceInstance<SharedResourceCapacity> entry, int idx) {
    String name = entry.getId();
    boolean occupied = entry.getValue().inuse > 0;
    int slotIdx = 0;
    int maxSlotIdx = entry.getValue().cardinality-1;
    new CapacityUsageSlotInformation(name, idx, occupied, ordertype, orderid, false, 0, slotIdx, maxSlotIdx);
    
    return null;
  }


  @Override
  public List<CapacityInformation> listCapacities() {
    SharedResourceRequestResult<SharedResourceCapacity> readResult = srm.readAll(XYNA_CAP_SR_DEF);
    if (!readResult.isSuccess() || readResult.getResources() == null) {
      logger.error("could not read capacities.", readResult.getException());
    }
    List<CapacityInformation> result = new ArrayList<>();
    for (SharedResourceInstance<SharedResourceCapacity> entry : readResult.getResources()) {
      String name = entry.getId();
      int inuse = entry.getValue().inuse;
      int cardinality = entry.getValue().cardinality;
      State state = State.valueOf(entry.getValue().stateString);
      result.add(new CapacityInformation(name, cardinality, inuse, state));
    }
    return result;
  }


  @Override
  public void close() {
    //ntbd
  }


  @Override
  public CapacityReservation getCapacityReservation() {
    return null;
  }


  private static class SharedResourceCapacity {

    public int cardinality;
    public int inuse;
    public String stateString;
    public Map<Integer, Order> orders;
  }
  
  private static class Order {
    
  }

}
