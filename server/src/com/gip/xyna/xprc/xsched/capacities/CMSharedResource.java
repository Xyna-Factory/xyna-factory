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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.exceptions.XNWH_SharedResourceInstanceAlreadyExists;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.scheduling.CapacityReservation;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;



public class CMSharedResource implements CapacityManagementInterface {

  public static final SharedResourceDefinition<SharedResourceCapacity> XYNA_CAP_SR_DEF =
      new KryoSerializedSharedResourceDefinition<>(CapacityManagement.XYNA_CAPACITY_SR, SharedResourceCapacity.class, List.class,
                                                   Order.class);

  private static final Logger logger = CentralFactoryLogging.getLogger(CMSharedResource.class);
  private final SharedResourceManagement srm;


  public CMSharedResource() {
    srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
  }


  @Override
  public CapacityAllocationResult allocateCapacities(OrderInformation orderInformation, SchedulingData schedulingData) {
    if (schedulingData == null) {
      throw new IllegalArgumentException("SchedulingData may not be null when allocating capacity");
    }

    if (schedulingData.isHasAcquiredCapacities() && schedulingData.mustAcquireCapacitiesOnlyOnce()) {
      logger.warn(CMAbstract.TRIED_TO_AQUIRE_TWICE_EXCEPTION_MESSAGE + orderInformation);
    }

    if (!schedulingData.needsCapacities()) {
      schedulingData.setHasAcquiredCapacities(true);
      return CapacityAllocationResult.SUCCESS;
    }
    
    if( schedulingData.getTransferCapacities() != null ) {
      //TODO:
    }
    
    if( schedulingData.getMultiAllocationCapacities() != null ) {
      //TODO:
    }
    
    
    
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
    SharedResourceRequestResult<SharedResourceCapacity> readResult = srm.readAll(XYNA_CAP_SR_DEF);
    if (!readResult.isSuccess()) {
      return false;
    }
    List<SharedResourceInstance<SharedResourceCapacity>> resources = readResult.getResources();
    if (resources == null || resources.isEmpty()) {
      return true;
    }

    resources.removeIf(x -> x.getValue().orders.stream().anyMatch(y -> y.orderId == orderId));
    if (resources.isEmpty()) {
      return true;
    }
    List<String> ids = resources.stream().map(x -> x.getId()).collect(Collectors.toList());
    Long now = System.currentTimeMillis();
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    update = (x) -> {
      List<Order> orders = x.getValue().orders.stream().filter(y -> y.orderId == orderId).collect(Collectors.toList());
      if (orders.isEmpty()) {
        return null;
      }
      for (Order order : orders) {
        x.getValue().inuse -= order.capacityInstances;
        x.getValue().orders.remove(order);
      }
      return new SharedResourceInstance<CMSharedResource.SharedResourceCapacity>(x.getId(), now, x.getValue());
    };
    SharedResourceRequestResult<SharedResourceCapacity> updateResult = srm.update(XYNA_CAP_SR_DEF, ids, update);

    return updateResult.isSuccess();
  }


  @Override
  public boolean addCapacity(String name, int cardinality, State state) throws XPRC_CAPACITY_ALREADY_DEFINED {
    assertCapacityName(name);
    assertCapacityCardinality(cardinality);

    Long now = System.currentTimeMillis();
    SharedResourceCapacity value = new SharedResourceCapacity();
    value.cardinality = cardinality;
    value.inuse = 0;
    value.orders = new ArrayList<>();
    value.stateString = state.toString();
    SharedResourceInstance<SharedResourceCapacity> instance = new SharedResourceInstance<>(name, now, value);
    SharedResourceRequestResult<SharedResourceCapacity> createResult = srm.create(XYNA_CAP_SR_DEF, List.of(instance));
    if (!createResult.isSuccess() && createResult.getException() instanceof XNWH_SharedResourceInstanceAlreadyExists) {
      throw new XPRC_CAPACITY_ALREADY_DEFINED(name);
    }
    return createResult.isSuccess();
  }


  @Override
  public boolean changeCapacityName(String oldName, String newName) {
    assertCapacityName(oldName);
    assertCapacityName(newName);

    if (newName.equals(oldName)) {
      return true;
    }

    Long now = System.currentTimeMillis();
    SharedResourceCapacity newValue = new SharedResourceCapacity();
    SharedResourceInstance<SharedResourceCapacity> newInstance = new SharedResourceInstance<>(newName, now, newValue);
    //disable old capacity -> no changes allowed - but remember old value -> to recreate correct state
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    update = (x) -> {
      if (x.getValue().inuse != 0) {
        logger.info("Tried to rename a capacity that is in use - oldName: " + oldName);
        return null;
      }
      newInstance.getValue().cardinality = x.getValue().cardinality;
      newInstance.getValue().inuse = x.getValue().inuse;
      newInstance.getValue().orders = x.getValue().orders;
      newInstance.getValue().stateString = x.getValue().stateString;
      x.getValue().stateString = State.DISABLED.toString();
      return new SharedResourceInstance<SharedResourceCapacity>(x.getId(), x.getCreated(), x.getValue());
    };

    SharedResourceRequestResult<SharedResourceCapacity> updateResult = srm.update(XYNA_CAP_SR_DEF, List.of(oldName), update);
    if (!updateResult.isSuccess()) {
      return false;
    }

    SharedResourceRequestResult<SharedResourceCapacity> createResult = srm.create(XYNA_CAP_SR_DEF, List.of(newInstance));
    if (!createResult.isSuccess()) {
      if (!newValue.stateString.equals(State.DISABLED.toString())) {
        if (!changeState(oldName, State.valueOf(newValue.stateString))) {
          logger.error("Rename of Capacity " + oldName + " failed. It is now in state " + State.DISABLED.toString());
        }
        return false;
      }
    }

    SharedResourceRequestResult<SharedResourceCapacity> deleteResult = srm.delete(XYNA_CAP_SR_DEF, List.of(oldName));
    if (!deleteResult.isSuccess()) {
      logger.error("Rename of Capacity " + oldName + " succeeded, but old capacity could not be deleted. It is now in state "
          + State.DISABLED.toString());
    }

    return true;
  }


  @Override
  public boolean changeCardinality(String capName, int newOverallCardinality)
      throws XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    assertCapacityCardinality(newOverallCardinality);
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    ChangeCardinalityExceptionContainer container = new ChangeCardinalityExceptionContainer();
    update = (x) -> {
      if (x.getValue().inuse > newOverallCardinality) {
        String state = x.getValue().stateString;
        String active = State.ACTIVE.toString();
        String disabled = State.DISABLED.toString();
        container.ex1 = active.equals(state) ? new XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState() : null;
        container.ex2 = disabled.equals(state) ? new XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain() : null;
        logger.info("Can not change cardinality to the desired value, too many capacities in use");
        return null; //abort update
      }
      x.getValue().cardinality = newOverallCardinality;
      return new SharedResourceInstance<SharedResourceCapacity>(x.getId(), x.getCreated(), x.getValue());
    };

    SharedResourceRequestResult<SharedResourceCapacity> updateResult = srm.update(XYNA_CAP_SR_DEF, List.of(capName), update);

    if (container.ex1 != null) {
      throw container.ex1;
    }
    if (container.ex2 != null) {
      throw container.ex2;
    }

    return updateResult.isSuccess();
  }


  @Override
  public boolean changeState(String capName, State newState) {
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    update = (x) -> {
      x.getValue().stateString = newState.toString();
      return new SharedResourceInstance<SharedResourceCapacity>(x.getId(), x.getCreated(), x.getValue());
    };

    SharedResourceRequestResult<SharedResourceCapacity> updateResult = srm.update(XYNA_CAP_SR_DEF, List.of(capName), update);
    return updateResult.isSuccess();
  }


  @Override
  public boolean removeCapacity(String capName) {
    SharedResourceRequestResult<SharedResourceCapacity> deleteResult = srm.delete(XYNA_CAP_SR_DEF, List.of(capName));
    return deleteResult.isSuccess();
  }


  @Override
  public void removeAllCapacities() {
    SharedResourceRequestResult<SharedResourceCapacity> readResult = srm.readAll(XYNA_CAP_SR_DEF);
    if (!readResult.isSuccess() || readResult.getResources() == null) {
      logger.error("could not read capacities.", readResult.getException());
      return;
    }
    List<String> ids = readResult.getResources().stream().map(x -> x.getId()).collect(Collectors.toList());
    SharedResourceRequestResult<SharedResourceCapacity> deleteResult = srm.delete(XYNA_CAP_SR_DEF, ids);
    if (!deleteResult.isSuccess()) {
      logger.error("could not delete capacities.", deleteResult.getException());
    }
  }


  @Override
  public CapacityInformation getCapacityInformation(String capName) {
    SharedResourceRequestResult<SharedResourceCapacity> readResult = srm.read(XYNA_CAP_SR_DEF, List.of(capName));
    if (!readResult.isSuccess()) {
      throw new RuntimeException("Could not read capacity", readResult.getException());
    }
    if (readResult.getResources() == null || readResult.getResources().isEmpty()) {
      throw new IllegalArgumentException("Unknown capacity: <" + capName + ">");
    }

    SharedResourceCapacity instance = readResult.getResources().get(0).getValue();
    int cardinality = instance.cardinality;
    int inuse = instance.inuse;
    State state = State.valueOf(instance.stateString);
    return new CapacityInformation(capName, cardinality, inuse, state);
  }


  @Override
  public ExtendedCapacityUsageInformation getExtendedCapacityUsageInformation() {
    SharedResourceRequestResult<SharedResourceCapacity> readResult = srm.readAll(XYNA_CAP_SR_DEF);
    if (!readResult.isSuccess() || readResult.getResources() == null) {
      logger.error("could not read capacities.", readResult.getException());
      return new ExtendedCapacityUsageInformation();
    }
    int privateCapacityIndex = 0;
    ExtendedCapacityUsageInformation ecui = new ExtendedCapacityUsageInformation();
    for (SharedResourceInstance<SharedResourceCapacity> entry : readResult.getResources()) {
      List<CapacityUsageSlotInformation> cusiList = convert(entry, privateCapacityIndex);
      for (CapacityUsageSlotInformation cusi : cusiList) {
        ecui.addSlotInformation(privateCapacityIndex, cusi);
      }
      ++privateCapacityIndex;
    }
    return ecui;
  }


  private List<CapacityUsageSlotInformation> convert(SharedResourceInstance<SharedResourceCapacity> entry, int idx) {
    List<CapacityUsageSlotInformation> result = new ArrayList<>();
    String name = entry.getId();
    boolean occupied = entry.getValue().inuse > 0;
    int slotIdx = 0;
    int maxSlotIdx = entry.getValue().cardinality - 1;
    for (Order order : entry.getValue().orders) {
      String orderType = order.orderType;
      Long orderId = order.orderId;
      for (int i = 0; i < order.capacityInstances; i++) {
        result.add(new CapacityUsageSlotInformation(name, idx, occupied, orderType, orderId, false, 0, slotIdx++, maxSlotIdx));
      }
    }

    return result;
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


  private void assertCapacityName(String name) {
    if (name == null) {
      throw new IllegalArgumentException("New capacity name may not be null");
    }

    if (name.length() == 0) {
      throw new IllegalArgumentException("Empty string is not allowed as capacity name");
    }
  }


  private void assertCapacityCardinality(int cardinality) {
    if (cardinality < 0) {
      throw new IllegalArgumentException("Cardinality may not be negative");
    }
  }


  private static class SharedResourceCapacity {

    public int cardinality;
    public int inuse;
    public String stateString;
    public List<Order> orders;
  }

  private static class Order {

    public long orderId;
    public String orderType;
    public int capacityInstances;
  }

  private static class ChangeCardinalityExceptionContainer {

    public XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState ex1;
    public XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain ex2;
  }

}
