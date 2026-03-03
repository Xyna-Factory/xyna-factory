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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.exceptions.XNWH_SharedResourceInstanceAlreadyExists;
import com.gip.xyna.xnwh.exceptions.XNWH_SharedResourceInstanceDoesNotExist;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CAPACITY_ALREADY_DEFINED;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.exceptions.XPRC_Scheduler_CapacityMissingException;
import com.gip.xyna.xprc.exceptions.XPRC_Scheduler_TooHighCapacityCardinalityException;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;
import com.gip.xyna.xprc.xsched.ExtendedCapacityUsageInformation;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.scheduling.CapacityReservation;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;



public class CMSharedResource implements CapacityManagementInterface {

  public static final SharedResourceDefinition<SharedResourceCapacity> XYNA_CAP_SR_DEF =
      new KryoSerializedSharedResourceDefinition<>(CapacityManagement.XYNA_CAPACITY_SR, SharedResourceCapacity.class, Map.class,
                                                   HashMap.class, Order.class);

  private static final Logger logger = CentralFactoryLogging.getLogger(CMSharedResource.class);
  private final SharedResourceManagement srm;

  private final Set<Long> skipUndoSet = new HashSet<>();


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
    Long now = System.currentTimeMillis();
    AllocateCapacitiesInformationContainer allocInfo;
    allocInfo = AllocateCapacitiesInformationContainer.createAllocateCapacitiesInformationContainer(orderInformation, schedulingData);
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    update = (x) -> {
      allocInfo.reset();
      if (capacityDisabled(x, allocInfo) || capacityInsufficient(x, allocInfo)) {
        return null;
      }

      allocRegularCapacities(x, allocInfo);
      return new SharedResourceInstance<>(x.getId(), now, x.getValue());
    };
    SharedResourceRequestResult<SharedResourceCapacity> result = srm.update(XYNA_CAP_SR_DEF, allocInfo.ids, update);
    if (!result.isSuccess()) {
      return handleFailedAllocation(result, allocInfo);
    }
    if (schedulingData.getMultiAllocationCapacities() != null) {
      return handleMultiAllocationCapacities(allocInfo, schedulingData);
    }
    schedulingData.setHasAcquiredCapacities(true);
    return CapacityAllocationResult.SUCCESS;
  }


  private CapacityAllocationResult handleMultiAllocationCapacities(AllocateCapacitiesInformationContainer allocInfo,
                                                                   SchedulingData schedulingData) {
    AllocationContainer allocationContainer = new AllocationContainer();
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    SharedResourceRequestResult<SharedResourceCapacity> result;
    if (schedulingData.getMultiAllocationCapacities().getCapacities().size() == 1) {
      //only one capacity to allocate
      update = (x) -> {
        allocInfo.reset();
        int cardinality = schedulingData.getMultiAllocationCapacities().getCapacities().get(0).getCardinality();
        int free = x.getValue().cardinality - x.getValue().inuse;
        int maxWantAllocations = schedulingData.getMultiAllocationCapacities().getMaxAllocation()
            - schedulingData.getMultiAllocationCapacities().getMinAllocation();
        int maxPosAllocations = free / cardinality;
        int maxAllocations = Math.min(maxWantAllocations, maxPosAllocations) * cardinality;
        boolean transferable = schedulingData.getMultiAllocationCapacities().isTransferable();
        x.getValue().inuse += maxAllocations;
        x.getValue().orders.get(allocInfo.orderId).capacityInstances += transferable ? 0 : maxAllocations;
        x.getValue().orders.get(allocInfo.orderId).transferableCapacityInstances += transferable ? maxAllocations : 0;
        logger.debug("allocated " + maxAllocations + " multi capacities for order " + allocInfo.orderId);
        int minAllocs = schedulingData.getMultiAllocationCapacities().getMinAllocation();
        allocationContainer.allocations = Math.max(minAllocs, maxAllocations + minAllocs);
        return x;
      };
      result = srm.update(XYNA_CAP_SR_DEF, allocInfo.ids, update);
    } else {
      result = executeMultiAllocation(allocInfo, schedulingData, allocationContainer);
    }
    if (!result.isSuccess()) {
      return handleFailedAllocation(result, allocInfo);
    }
    schedulingData.getMultiAllocationCapacities().setAllocations(allocationContainer.allocations);
    schedulingData.setHasAcquiredCapacities(true);
    return CapacityAllocationResult.SUCCESS;
  }


  private SharedResourceRequestResult<SharedResourceCapacity> executeMultiAllocation(AllocateCapacitiesInformationContainer allocInfo,
                                                                                     SchedulingData schedulingData,
                                                                                     AllocationContainer allocationContainer) {

    SharedResourceRequestResult<SharedResourceCapacity> lastSuccess = null;
    SharedResourceRequestResult<SharedResourceCapacity> lastResult = null;
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    Map<String, Integer> allocMap = new HashMap<>();
    for (Capacity cap : schedulingData.getMultiAllocationCapacities().getCapacities()) {
      allocMap.put(cap.getCapName(), cap.getCardinality());
    }
    update = (x) -> {
      allocInfo.reset();
      int cardinality = allocMap.get(x.getId());
      int free = x.getValue().cardinality - x.getValue().inuse;
      int maxPosAllocations = free / cardinality;
      if (maxPosAllocations <= 0) {
        return null;
      }
      int maxAllocations = cardinality;
      boolean transferable = schedulingData.getMultiAllocationCapacities().isTransferable();
      x.getValue().inuse += maxAllocations;
      x.getValue().orders.get(allocInfo.orderId).capacityInstances += transferable ? 0 : maxAllocations;
      x.getValue().orders.get(allocInfo.orderId).transferableCapacityInstances += transferable ? maxAllocations : 0;
      logger.debug("allocated " + maxAllocations + " multi capacities for order " + allocInfo.orderId);
      allocationContainer.allocations++;
      return x;
    };
    for (int i = schedulingData.getMultiAllocationCapacities().getMinAllocation(); i < schedulingData.getMultiAllocationCapacities()
        .getMaxAllocation(); i++) {
      SharedResourceRequestResult<SharedResourceCapacity> result = srm.update(XYNA_CAP_SR_DEF, allocInfo.ids, update);
      lastResult = result;
      if (!result.isSuccess()) {
        lastSuccess = result;
      }
    }
    return lastSuccess != null ? lastSuccess : lastResult;
  }


  private CapacityAllocationResult handleFailedAllocation(SharedResourceRequestResult<SharedResourceCapacity> result,
                                                          AllocateCapacitiesInformationContainer allocInfo) {
    skipUndoSet.add(allocInfo.orderId);
    if (result.getException() != null && result.getException() instanceof XNWH_SharedResourceInstanceDoesNotExist) {
      return createCapacityAllocationResultMissingCap(allocInfo);
    }
    if (allocInfo.capNameOfDisabled != null) {
      return new CapacityAllocationResult(allocInfo.capNameOfDisabled, allocInfo.capDemandDisabled, 0, true);
    }
    if (allocInfo.capNameInsufficent != null) {
      return createCapacityAllocationResultInsufficient(allocInfo);
    }
    throw new IllegalStateException("Allocation failed, but could not determine reason. Result: " + result + ", allocInfo: " + allocInfo);
  }


  private void allocRegularCapacities(SharedResourceInstance<SharedResourceCapacity> instance,
                                      AllocateCapacitiesInformationContainer allocInfo) {
    Order order = instance.getValue().orders.computeIfAbsent(allocInfo.orderId, (x) -> new Order());
    int regularCapacityInstances = allocInfo.regularCapacityMap.getOrDefault(instance.getId(), 0);
    int transferCapacityInstances = allocInfo.transferCapacityMap.getOrDefault(instance.getId(), 0);
    int transferReceivedCapacityInstances = order.receivedTransferableCapacityInstances;
    int newAllocations = regularCapacityInstances + transferCapacityInstances - transferReceivedCapacityInstances;
    logger
        .debug("Allocating regular capacities for " + allocInfo.orderId + " - " + instance.getId() + ". inuse: " + instance.getValue().inuse //
            + ", order.capacityInstances: " + order.capacityInstances //
            + ", order.transferableCapacityInstances: " + order.transferableCapacityInstances // 
            + ", order.receivedTransferableCapacityInstances: " + transferReceivedCapacityInstances //
            + ", NewAllocations: " + newAllocations + "(" + regularCapacityInstances + "+" + transferCapacityInstances + "-"
            + transferReceivedCapacityInstances + ")"); //
    order.orderType = allocInfo.orderType;
    order.capacityInstances += regularCapacityInstances;
    order.transferableCapacityInstances += transferCapacityInstances;
    order.receivedTransferableCapacityInstances =
        Math.max(0, transferReceivedCapacityInstances - regularCapacityInstances - transferCapacityInstances);
    instance.getValue().inuse += newAllocations;
    logger.debug("Allocated  regular capacities for " + allocInfo.orderId + " - " + instance.getId() //
        + ". inuse: " + instance.getValue().inuse //
        + ", order.capacityInstances: " + order.capacityInstances //
        + ", order.transferableCapacityInstances: " + order.transferableCapacityInstances //
        + ", order.receivedTransferableCapacityInstances: " + order.receivedTransferableCapacityInstances);//
  }


  private boolean capacityInsufficient(SharedResourceInstance<SharedResourceCapacity> x, AllocateCapacitiesInformationContainer allocInfo) {
    String capName = x.getId();
    int inUse = x.getValue().inuse;
    int cardinality = x.getValue().cardinality;
    int free = cardinality - inUse;
    Order order = x.getValue().orders.getOrDefault(allocInfo.orderId, new Order());
    int transfered = order.receivedTransferableCapacityInstances;
    int demand = allocInfo.totalCapacityMap.getOrDefault(capName, 0) - transfered;
    if (free < demand) {
      allocInfo.capNameInsufficent = capName;
      allocInfo.maxCardinalityInsufficient = cardinality;
      allocInfo.capFreeInsufficent = free;
      allocInfo.capDemandInsufficient = demand;
      logger.debug("Insufficient capacities for " + allocInfo.orderId + " - " + capName + ". f/d/t: " + free + "/" + demand + "/"
          + transfered + " map: " + allocInfo.totalCapacityMap.getOrDefault(capName, 0));
      return true;
    }
    return false;
  }


  private boolean capacityDisabled(SharedResourceInstance<SharedResourceCapacity> x, AllocateCapacitiesInformationContainer allocInfo) {
    String state = x.getValue().stateString;
    if (State.DISABLED.toString().equals(state)) {
      allocInfo.capNameOfDisabled = x.getId();
      allocInfo.capDemandDisabled = allocInfo.totalCapacityMap.get(x.getId());
      return true;
    }
    return false;
  }


  private CapacityAllocationResult createCapacityAllocationResultMissingCap(AllocateCapacitiesInformationContainer allocInfo) {
    String capName = allocInfo.capNameOfMissing;
    int demand = allocInfo.capDemandDisabled;
    switch (XynaProperty.SCHEDULER_UNDEFINED_CAPACITY_REACTION.get()) {
      case Schedule :
        return CapacityAllocationResult.SUCCESS;
      case Wait :
        return new CapacityAllocationResult(capName, demand, 0, true);
      case Fail :
      default :
        return new CapacityAllocationResult(capName, new XPRC_Scheduler_CapacityMissingException(capName));
    }
  }


  private CapacityAllocationResult createCapacityAllocationResultInsufficient(AllocateCapacitiesInformationContainer allocInfo) {
    String capName = allocInfo.capNameInsufficent;
    int numFree = allocInfo.capFreeInsufficent;
    int demand = allocInfo.capDemandInsufficient;

    if (allocInfo.maxCardinalityInsufficient >= demand) {
      return new CapacityAllocationResult(capName, demand, numFree, false);
    }

    switch (XynaProperty.SCHEDULER_UNSUFFICIENT_CAPACITY_REACTION.get()) {
      case Schedule :
        return new CapacityAllocationResult(capName, demand, numFree, false);
      case Wait :
        return new CapacityAllocationResult(capName, 0, numFree, true);
      case Fail :
      default :
        return new CapacityAllocationResult(capName, new XPRC_Scheduler_TooHighCapacityCardinalityException(capName, demand));
    }
  }


  @Override
  public void undoAllocation(OrderInformation orderInformation, SchedulingData schedulingData) {
    if (skipUndoSet.contains(orderInformation.getOrderId())) {
      if (logger.isDebugEnabled()) {
        logger.debug("Skipping undo of capacity allocation for order " + orderInformation.getOrderId()
            + " because allocation was not fully successful");
      }
      return;
    }
    SharedResourceRequestResult<SharedResourceCapacity> readResult = srm.readAll(XYNA_CAP_SR_DEF);
    if (!readResult.isSuccess() || readResult.getResources() == null) {
      logger.error("could not read capacities.", readResult.getException());
      return;
    }
    Long now = System.currentTimeMillis();
    Long orderId = orderInformation.getOrderId();
    List<String> ids = readResult.getResources().stream().filter(x -> x.getValue().orders.containsKey(orderId)).map(x -> x.getId())
        .collect(Collectors.toList());
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    update = (x) -> {
      Order order = x.getValue().orders.get(orderId);
      x.getValue().inuse -= order.capacityInstances + order.transferableCapacityInstances;
      x.getValue().orders.remove(orderId);
      return new SharedResourceInstance<>(x.getId(), now, x.getValue());
    };
    SharedResourceRequestResult<SharedResourceCapacity> updateResult = srm.update(XYNA_CAP_SR_DEF, ids, update);
    if (!updateResult.isSuccess()) {
      logger.error("could not undo capacity allocation.");
    }
  }


  @Override
  public boolean freeTransferableCapacities(XynaOrderServerExtension xo) {
    SharedResourceRequestResult<SharedResourceCapacity> readResult = srm.readAll(XYNA_CAP_SR_DEF);
    if (!readResult.isSuccess() || readResult.getResources() == null) {
      logger.error("could not read capacities.", readResult.getException());
      return false;
    }

    Long now = System.currentTimeMillis();
    List<String> ids = readResult.getResources().stream().filter(x -> x.getValue().orders.containsKey(xo.getId())).map(x -> x.getId())
        .collect(Collectors.toList());

    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    update = (x) -> {
      Order order = x.getValue().orders.get(xo.getId());
      x.getValue().inuse -= order.transferableCapacityInstances;
      order.transferableCapacityInstances = 0;
      if (order.capacityInstances == 0) {
        x.getValue().orders.remove(xo.getId());
      }
      return new SharedResourceInstance<>(x.getId(), now, x.getValue());
    };
    SharedResourceRequestResult<SharedResourceCapacity> updateResult = srm.update(XYNA_CAP_SR_DEF, ids, update);
    return updateResult.isSuccess();
  }


  @Override
  public boolean freeCapacities(XynaOrderServerExtension xo) {
    SharedResourceRequestResult<SharedResourceCapacity> readResult = srm.readAll(XYNA_CAP_SR_DEF);
    if (!readResult.isSuccess() || readResult.getResources() == null) {
      logger.error("could not read capacities.", readResult.getException());
      return false;
    }

    Long now = System.currentTimeMillis();
    List<String> ids = readResult.getResources().stream().filter(x -> x.getValue().orders.containsKey(xo.getId())).map(x -> x.getId())
        .collect(Collectors.toList());

    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    update = (x) -> {
      Order order = x.getValue().orders.get(xo.getId());
      x.getValue().inuse -= order.capacityInstances;
      order.capacityInstances = 0;
      if (order.transferableCapacityInstances == 0) {
        x.getValue().orders.remove(xo.getId());
      }
      return new SharedResourceInstance<>(x.getId(), now, x.getValue());
    };
    SharedResourceRequestResult<SharedResourceCapacity> updateResult = srm.update(XYNA_CAP_SR_DEF, ids, update);
    return updateResult.isSuccess();
  }


  @Override
  public boolean transferCapacities(XynaOrderServerExtension xo, TransferCapacities transferCapacities) {
    Map<String, Integer> capacitiesToTransferMap = new HashMap<>();
    for (Capacity cap : transferCapacities.getCapacities()) {
      capacitiesToTransferMap.put(cap.getCapName(), cap.getCardinality());
    }
    Long now = System.currentTimeMillis();
    Long fromOrderId = transferCapacities.getFromOrderId();
    List<String> ids = transferCapacities.getCapacities().stream().map(x -> x.getCapName()).collect(Collectors.toList());
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    update = (x) -> {
      Order oldOrder = x.getValue().orders.get(fromOrderId);
      if (oldOrder == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not transfer capacities from " + fromOrderId + " to " + xo.getId() + " because " + fromOrderId
              + " does not hold any capacities of type " + x.getId());
        }
        return null;
      }
      if (oldOrder.transferableCapacityInstances < capacitiesToTransferMap.get(x.getId())) {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not transfer capacities from " + fromOrderId + " to " + xo.getId() + " because " + fromOrderId
              + " does not hold enough capacities of type " + x.getId() + " (" + capacitiesToTransferMap.get(x.getId())
              + " are required, but only " + oldOrder.capacityInstances + " are held");
        }
        return null;
      }
      oldOrder.transferableCapacityInstances -= capacitiesToTransferMap.get(x.getId());
      Order newOrder = x.getValue().orders.computeIfAbsent(xo.getId(), (y) -> new Order());
      newOrder.receivedTransferableCapacityInstances = capacitiesToTransferMap.get(x.getId());
      newOrder.orderType = xo.getDestinationKey().getOrderType();

      return new SharedResourceInstance<>(x.getId(), now, x.getValue());
    };

    SharedResourceRequestResult<SharedResourceCapacity> updateResult = srm.update(XYNA_CAP_SR_DEF, ids, update);
    return updateResult.isSuccess();
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

    resources.removeIf(x -> !x.getValue().orders.containsKey(orderId));
    if (resources.isEmpty()) {
      return true;
    }
    List<String> ids = resources.stream().map(x -> x.getId()).collect(Collectors.toList());
    Long now = System.currentTimeMillis();
    Function<SharedResourceInstance<SharedResourceCapacity>, SharedResourceInstance<SharedResourceCapacity>> update;
    update = (x) -> {
      Order order = x.getValue().orders.get(orderId);
      if (order == null) {
        return null;
      }
      x.getValue().inuse -= order.capacityInstances + order.transferableCapacityInstances;
      x.getValue().orders.remove(orderId);

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
    value.orders = new HashMap<>();
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
      container.ex1 = null;
      container.ex2 = null;
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
    int slotIdx = 0;
    int maxSlotIdx = entry.getValue().cardinality - 1;
    int emptySlots = entry.getValue().cardinality - entry.getValue().inuse;
    for (Entry<Long, Order> order : entry.getValue().orders.entrySet()) {
      String orderType = order.getValue().orderType;
      Long orderId = order.getKey();
      for (int i = 0; i < order.getValue().capacityInstances; i++) {
        result.add(new CapacityUsageSlotInformation(name, idx, true, orderType, orderId, false, 0, slotIdx++, maxSlotIdx));
      }
      for (int i = 0; i < order.getValue().transferableCapacityInstances; i++) {
        result.add(new CapacityUsageSlotInformation(name, idx, true, orderType, orderId, true, 0, slotIdx++, maxSlotIdx));
      }
      for (int i = 0; i < order.getValue().receivedTransferableCapacityInstances; i++) {
        result.add(new CapacityUsageSlotInformation(name, idx, true, orderType, orderId, false, 0, slotIdx++, maxSlotIdx));
      }
    }
    for (int i = 0; i < emptySlots; i++) {
      result.add(new CapacityUsageSlotInformation(name, idx, false, null, ExtendedCapacityUsageInformation.ORDER_ID_FOR_UNOCCUPIED_CAPACITY,
                                                  false, 0, slotIdx++, maxSlotIdx));
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


  private static class AllocationContainer {

    int allocations;
  }

  private static class SharedResourceCapacity {

    public int cardinality;
    public int inuse;
    public String stateString;
    public Map<Long, Order> orders;
  }

  private static class Order {

    public String orderType;
    public int capacityInstances;
    public int transferableCapacityInstances;
    public int receivedTransferableCapacityInstances;
  }

  private static class ChangeCardinalityExceptionContainer {

    public XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState ex1;
    public XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain ex2;
  }

  private static class AllocateCapacitiesInformationContainer {

    //input
    public Map<String, Integer> totalCapacityMap;
    public Map<String, Integer> regularCapacityMap;
    public Map<String, Integer> transferCapacityMap;
    public Long orderId;
    public String orderType;
    public List<String> ids;

    //capacity missing
    public String capNameOfMissing;

    //capacity disabled
    public String capNameOfDisabled;
    public int capDemandDisabled;

    //insufficient free capacities
    public String capNameInsufficent;
    public int maxCardinalityInsufficient;
    public int capFreeInsufficent;
    public int capDemandInsufficient;


    public void reset() {
      capNameOfMissing = null;
      capNameOfDisabled = null;
      capDemandDisabled = 0;
      capNameInsufficent = null;
      maxCardinalityInsufficient = 0;
      capFreeInsufficent = 0;
      capDemandInsufficient = 0;
    }


    public static AllocateCapacitiesInformationContainer createAllocateCapacitiesInformationContainer(OrderInformation orderInformation,
                                                                                                      SchedulingData schedulingData) {
      Long orderId = orderInformation.getOrderId();
      String orderType = orderInformation.getOrderType();

      List<Capacity> transferableCapacities = new ArrayList<>();
      if (schedulingData.getTransferCapacities() != null) {
        transferableCapacities = schedulingData.getTransferCapacities().getCapacities();
      }

      List<Capacity> regularCapacities = schedulingData.getCapacities();

      boolean multiAlloCaps = schedulingData.getMultiAllocationCapacities() != null;
      if (multiAlloCaps) {
        MultiAllocationCapacities caps = schedulingData.getMultiAllocationCapacities();
        boolean transferable = schedulingData.getMultiAllocationCapacities().isTransferable();
        List<Capacity> listToAddTo = transferable ? transferableCapacities : regularCapacities;
        List<Capacity> capsToAdd = new ArrayList<>();
        for (Capacity cap : caps.getCapacities()) {
          Capacity multipliedCapacity = new Capacity(cap.getCapName(), cap.getCardinality() * caps.getMinAllocation());
          capsToAdd.add(multipliedCapacity);
        }
        listToAddTo.addAll(capsToAdd);
      }
      List<String> ids = transferableCapacities.stream().map(x -> x.getCapName()).collect(Collectors.toList());
      ids.addAll(regularCapacities.stream().map(x -> x.getCapName()).collect(Collectors.toList()));

      Map<String, Integer> totalCapacityMap = new HashMap<>();
      Map<String, Integer> regularCapacityMap = new HashMap<>();
      Map<String, Integer> transferableCapacityMap = new HashMap<>();
      for (Capacity capacity : transferableCapacities) {
        totalCapacityMap.putIfAbsent(capacity.getCapName(), 0);
        totalCapacityMap.compute(capacity.getCapName(), (x, y) -> y + capacity.getCardinality());
        transferableCapacityMap.put(capacity.getCapName(), capacity.getCardinality());

      }
      for (Capacity capacity : regularCapacities) {
        totalCapacityMap.putIfAbsent(capacity.getCapName(), 0);
        totalCapacityMap.compute(capacity.getCapName(), (x, y) -> y + capacity.getCardinality());
        regularCapacityMap.put(capacity.getCapName(), capacity.getCardinality());
      }


      AllocateCapacitiesInformationContainer allocInfo = new AllocateCapacitiesInformationContainer();
      allocInfo.totalCapacityMap = totalCapacityMap;
      allocInfo.regularCapacityMap = regularCapacityMap;
      allocInfo.transferCapacityMap = transferableCapacityMap;
      allocInfo.orderId = orderId;
      allocInfo.orderType = orderType;
      allocInfo.ids = ids;
      return allocInfo;
    }


    @Override
    public String toString() {
      return "AllocateCapacitiesInformationContainer{" + "totalCapacityMap=" + totalCapacityMap + ", regularCapacityMap="
          + regularCapacityMap + ", transferCapacityMap=" + transferCapacityMap + ", orderId=" + orderId + ", orderType='" + orderType
          + '\'' + ", ids=" + ids + '}';
    }
  }


}
