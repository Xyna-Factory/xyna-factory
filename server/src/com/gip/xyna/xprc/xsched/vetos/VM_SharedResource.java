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
package com.gip.xyna.xprc.xsched.vetos;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.sharedresources.KryoSerializedSharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceDefinition;
import com.gip.xyna.xnwh.sharedresources.SharedResourceInstance;
import com.gip.xyna.xnwh.sharedresources.SharedResourceManagement;
import com.gip.xyna.xnwh.sharedresources.SharedResourceRequestResult;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.VetoManagement;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.vetos.VM_Cache.VetoFilter;


public class VM_SharedResource implements VetoManagementInterface {

  public static final SharedResourceDefinition<SharedResourceVeto> XYNA_VETO_SR_DEF =
      new KryoSerializedSharedResourceDefinition<>(VetoManagement.XYNA_VETO_SR, SharedResourceVeto.class, ArrayList.class);
  private static final Logger logger = CentralFactoryLogging.getLogger(VM_SharedResource.class);
  private final SharedResourceManagement srm;


  public VM_SharedResource() {
    srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
  }


  @Override
  @Deprecated
  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> vetos, long urgency) {
    return allocateVetos(orderInformation, vetos, Collections.emptyList(), urgency);
  }

  @Override
  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> exclusiveVetos, List<String> sharedVetos, long urgency) {
    try {
      return allocateVetosImpl(orderInformation, exclusiveVetos, sharedVetos);
    } catch (Exception e) {
      logger.error("Error allocating shared resource vetos. " + e.getMessage(), e);
      return VetoAllocationResult.FAILED;
    }
  }

  
  private VetoAllocationResult allocateVetosImpl(OrderInformation orderInformation, List<String> exclusiveVetos,
                                                 List<String> sharedVetos) {
    if ((orderInformation == null) || (orderInformation.getOrderId() == null)) {
      logger.error("Error allocating shared resource vetos: received empty order information.");
      return VetoAllocationResult.FAILED;
    }
    VetoUpdater updater = new VetoUpdater(UpdaterMode.EXPECT_ORDER_START_ALLOWED, exclusiveVetos, sharedVetos, orderInformation);
    if (updater.getVetoIds().isEmpty()) {
      return VetoAllocationResult.SUCCESS;
    }
    SharedResourceRequestResult<SharedResourceVeto> readResult = srm.read(XYNA_VETO_SR_DEF, updater.getVetoIds());
    if (!readResult.isSuccess()) {
      logger.error("Error reading shared resource vetos.", readResult.getException());
      return VetoAllocationResult.FAILED;
    }
    
    // For vetos not yet existing: Insert uninitialized objects so that they too can be handled with update command below
    List<SharedResourceInstance<SharedResourceVeto>> uninitializedList = buildEmptyVetoList(exclusiveVetos, sharedVetos,
                                                                                            readResult);
    if (!uninitializedList.isEmpty()) {
      SharedResourceRequestResult<SharedResourceVeto> createResult = srm.create(XYNA_VETO_SR_DEF, uninitializedList);
      if (!createResult.isSuccess()) {
        logger.error("Error inserting uninitialized shared resource vetos.", createResult.getException());
        return VetoAllocationResult.FAILED;
      }
    }
    
    // First try: Try to update full scope until encountering an existing veto that disallows order start, in that case stop and rollback
    SharedResourceRequestResult<SharedResourceVeto> updateResult = srm.update(XYNA_VETO_SR_DEF, updater.getVetoIds(), updater);
    if (!updateResult.isSuccess()) {
      if (updateResult.getException() != null) {
        logger.error("Error updating shared resource vetos.", updateResult.getException());
        return VetoAllocationResult.FAILED;
      }
      if (!updater.isOrderStartDisallowed()) {
        logger.error("Unexpected result trying to update shared resource vetos.");
        return VetoAllocationResult.FAILED;
      }
    }
    if(logger.isDebugEnabled()) {
      logger.debug("Update Veto Result for " + orderInformation.getOrderId() + ": Return allocation success? " +
                   !updater.isOrderStartDisallowed());
    }
    if (!updater.isOrderStartDisallowed()) {
      return VetoAllocationResult.SUCCESS;
    }
    // Second try, if order start is disallowed: Update only pendingExclusive vetos where possible
    updater = new VetoUpdater(UpdaterMode.ORDER_START_DISALLOWED, exclusiveVetos, sharedVetos, orderInformation);
    updateResult = srm.update(XYNA_VETO_SR_DEF, updater.getVetoIds(), updater);
    if (!updateResult.isSuccess()) {
      logger.error("Error updating shared resource vetos.", updateResult.getException());
    }
    return VetoAllocationResult.FAILED;
  }
  
  /*
  private void logResult(String msg, SharedResourceRequestResult<SharedResourceVeto> result) {
    if (!logger.isDebugEnabled()) { return; }
    try {
      logger.debug(msg);
      if (result == null) {
        logger.debug("result is null");
        return;
      }
      if (result.getResources() == null) {
        logger.debug("resources are null");
        return;
      }
      if (result.getResources().isEmpty()) {
        logger.debug("resources are empty");
        return;
      }
      for (SharedResourceInstance<SharedResourceVeto> item : result.getResources()) {
        logger.debug(item.getId() + " ->" + (
            item == null ? "null" :
            (item.getValue() == null ? "null" : item.getValue().asString())
                     ));
      }
    } catch (Exception e) {
      logger.debug(e.getMessage(), e);
    }
  }
  */
  
  public List<SharedResourceInstance<SharedResourceVeto>> buildEmptyVetoList(
                                                            List<String> exclusiveVetos, List<String> sharedVetos,
                                                            SharedResourceRequestResult<SharedResourceVeto> existingList) {
    long now = System.currentTimeMillis();
    VetoMap existing = new VetoMap(existingList.getResources());
    List<SharedResourceInstance<SharedResourceVeto>> ret = new ArrayList<>();
    if (exclusiveVetos != null) {
      for (String id : exclusiveVetos) {
        if (!existing.contains(id)) {
          SharedResourceInstance<SharedResourceVeto> sri = new SharedResourceInstance<>(id, now, new SharedResourceVeto());
          ret.add(sri);
        }
      }
    }
    if (sharedVetos != null) {
      for (String id : sharedVetos) {
        if (!existing.contains(id)) {
          SharedResourceInstance<SharedResourceVeto> sri = new SharedResourceInstance<>(id, now, new SharedResourceVeto());
          ret.add(sri);
        }
      }
    }
    return ret;
  }
  
  
  @Override
  @Deprecated
  public void undoAllocation(OrderInformation orderInformation, List<String> vetos) {
    undoAllocation(orderInformation, vetos, Collections.emptyList());
  }

  @Override
  public void undoAllocation(OrderInformation orderInformation, List<String> exclusiveVetos, List<String> sharedVetos) {
    freeVetosForced(orderInformation.getOrderId());
  }

  @Override
  @Deprecated
  public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos) {
    finalizeAllocation(orderInformation, vetos, Collections.emptyList());
  }

  @Override
  public void finalizeAllocation(OrderInformation orderInformation, List<String> exclusiveVetos, List<String> sharedVetos) {
    //ntbd
  }


  @Override
  public boolean freeVetos(OrderInformation orderInformation) {
    return freeVetosOfOrder(orderInformation.getOrderId());
  }


  @Override
  public boolean freeVetosForced(long orderId) {
    return freeVetosOfOrder(orderId);
  }


  private boolean freeVetosOfOrder(long orderId) {
    try {
      return freeVetosOfOrderImpl(orderId);
    } catch (Exception e) {
      logger.error("Error trying to free shared resource vetos. " + e.getMessage(), e);
      return false;
    }
  }
  
  
  private boolean freeVetosOfOrderImpl(long orderId) {
    SharedResourceRequestResult<SharedResourceVeto> readVetosResult = srm.readAll(XYNA_VETO_SR_DEF);
    if (!readVetosResult.isSuccess()) {
      return false;
    }
    List<SharedResourceInstance<SharedResourceVeto>> allVetos = readVetosResult.getResources();
    if (allVetos == null) { return true; }
    if (allVetos.isEmpty()) { return true; }

    List<String> exclusiveIds = allVetos.stream().filter(x -> (x.getValue() != null) && Objects.equals(x.getValue().usingOrderId, orderId))
                                        .map(x -> x.getId()).collect(Collectors.toList());
    if (!exclusiveIds.isEmpty()) {
      SharedResourceRequestResult<SharedResourceVeto> deleteVetosResult = srm.delete(XYNA_VETO_SR_DEF, exclusiveIds);
      if (!deleteVetosResult.isSuccess()) {
        logger.error("Error freeing shared resource vetos.", deleteVetosResult.getException());
        return false;
      }
    }
    VetoRemover remover = new VetoRemover(orderId);
    List<String> updateIds = allVetos.stream().filter(remover).map(x -> x.getId()).collect(Collectors.toList());
    SharedResourceRequestResult<SharedResourceVeto> updateResult = srm.update(XYNA_VETO_SR_DEF, updateIds, remover);
    if (!updateResult.isSuccess()) {
      logger.error("Error freeing shared resource vetos.", updateResult.getException());
      return false;
    }
    return true;
  }

  
  @Override
  public void allocateAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    try {
      allocateAdministrativeVetoImpl(administrativeVeto);
    } catch (XPRC_AdministrativeVetoAllocationDenied | PersistenceLayerException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error allocating administrative shared resource veto. " + e.getMessage(), e);
    }
  }
  
  
  private void allocateAdministrativeVetoImpl(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {
    SharedResourceRequestResult<SharedResourceVeto> readResult = srm.read(XYNA_VETO_SR_DEF, List.of(administrativeVeto.getName()));
    Long blockingOrderId = null;
    if (!readResult.isSuccess()) {
      logger.error("AllocateAdministrativeVeto: Error reading shared resource vetos.", readResult.getException());
      throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), null, readResult.getException());
    }
    if ((readResult.getResources() != null) && (!readResult.getResources().isEmpty())) {
      if ((readResult.getResources().get(0) != null) && (readResult.getResources().get(0).getValue() != null)) {
        SharedResourceVeto veto = readResult.getResources().get(0).getValue();
        blockingOrderId = veto.usingOrderId != null ? veto.usingOrderId : veto.pendingExclusiveOrderId;
      }
      throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), blockingOrderId);
    }
    // For veto not yet existing: Insert uninitialized object so that it can be handled with update command below
    long now = System.currentTimeMillis();
    SharedResourceInstance<SharedResourceVeto> sri = new SharedResourceInstance<>(administrativeVeto.getName(), now,
                                                                                  new SharedResourceVeto());
    SharedResourceRequestResult<SharedResourceVeto> createResult = srm.create(XYNA_VETO_SR_DEF, List.of(sri));
    if (!createResult.isSuccess()) {
      logger.error("AllocateAdministrativeVeto: Error inserting uninitialized shared resource vetos.", createResult.getException());
      throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), blockingOrderId, createResult.getException());
    }
    
    // First try: Try to update full scope until encountering an existing veto that disallows order start, in that case stop and rollback
    VetoUpdater updater = new VetoUpdater(UpdaterMode.EXPECT_ORDER_START_ALLOWED, List.of(administrativeVeto.getName()),
                                          null, AdministrativeVeto.ADMIN_VETO_ORDER_INFORMATION);
    SharedResourceRequestResult<SharedResourceVeto> updateResult = srm.update(XYNA_VETO_SR_DEF, updater.getVetoIds(), updater);
    if (!updateResult.isSuccess()) {
      if (updateResult.getException() != null) {
        logger.error("AllocateAdministrativeVeto: Error updating shared resource vetos.", updateResult.getException());
        throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), blockingOrderId, updateResult.getException());
      }
      if (!updater.isOrderStartDisallowed()) {
        logger.error("AllocateAdministrativeVeto: Unexpected result trying to update shared resource vetos.");
        throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), blockingOrderId);
      }
    }
    if (!updater.isOrderStartDisallowed()) {
      return;
    }
    // Second try, if order start is disallowed: Update only pendingExclusive veto if possible
    updater = new VetoUpdater(UpdaterMode.ORDER_START_DISALLOWED, List.of(administrativeVeto.getName()), null,
                              AdministrativeVeto.ADMIN_VETO_ORDER_INFORMATION);
    updateResult = srm.update(XYNA_VETO_SR_DEF, updater.getVetoIds(), updater);
    if (!updateResult.isSuccess()) {
      logger.error("AllocateAdministrativeVeto: Error updating shared resource vetos.", updateResult.getException());
      throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), blockingOrderId, updateResult.getException());
    }
    throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), blockingOrderId);
  }
  
  
  @Override
  public String setDocumentationOfAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    DocumentationContainer oldDoc = new DocumentationContainer();

    Function<SharedResourceInstance<SharedResourceVeto>, SharedResourceInstance<SharedResourceVeto>> update = (x) -> {
      oldDoc.documentation = x.getValue().documentation;
      x.getValue().documentation = administrativeVeto.getDocumentation();
      return new SharedResourceInstance<SharedResourceVeto>(x.getId(), x.getCreated(), x.getValue());
    };

    SharedResourceRequestResult<SharedResourceVeto> updateResult;
    updateResult = srm.update(XYNA_VETO_SR_DEF, List.of(administrativeVeto.getName()), update);
    if (!updateResult.isSuccess()) {
      throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(administrativeVeto.getName(), VetoInformationStorable.TABLE_NAME);
    }

    return oldDoc.documentation;
  }


  @Override
  public VetoInformation freeAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    try {
      return freeAdministrativeVetoImpl(administrativeVeto);
    } catch (XPRC_AdministrativeVetoDeallocationDenied | PersistenceLayerException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Error trying to free administrative shared resource vetos. " + e.getMessage(), e);
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName(), e);
    }
  }
  
  
  private VetoInformation freeAdministrativeVetoImpl(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException {
    SharedResourceRequestResult<SharedResourceVeto> readResult = srm.read(XYNA_VETO_SR_DEF, List.of(administrativeVeto.getName()));
    if (!readResult.isSuccess() || readResult.getResources() == null || readResult.getResources().isEmpty()) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    if (readResult.getResources().size() > 1) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    if (readResult.getResources().get(0) == null) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    SharedResourceVeto existing = readResult.getResources().get(0).getValue();
    
    if (Objects.equals(AdministrativeVeto.ADMIN_VETO_ORDERID, existing.usingOrderId)) {
      SharedResourceRequestResult<SharedResourceVeto> deleteVetosResult = srm.delete(XYNA_VETO_SR_DEF, List.of(administrativeVeto.getName()));
      if (!deleteVetosResult.isSuccess()) {
        logger.error("Error trying to delete administrative shared resource vetos.", deleteVetosResult.getException());
        throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
      }
    } else {
      VetoRemover remover = new VetoRemover(AdministrativeVeto.ADMIN_VETO_ORDERID);
      //List<String> updateIds = allVetos.stream().filter(remover).map(x -> x.getId()).collect(Collectors.toList());
      SharedResourceRequestResult<SharedResourceVeto> updateResult = srm.update(XYNA_VETO_SR_DEF,
                                                                                List.of(administrativeVeto.getName()), remover);
      if (!updateResult.isSuccess()) {
        logger.error("Error updating administrative shared resource vetos.", updateResult.getException());
        throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
      }
    }
    Long created = administrativeVeto.getCreated();
    OrderInformation orderInfo = new OrderInformation(existing.usingOrderId, existing.usingRootOrderId, existing.usingOrderType);
    VetoInformation info = new VetoInformation(readResult.getResources().get(0).getId(), orderInfo, existing.sharedOrderIds,
                                               existing.pendingExclusiveOrderId, existing.documentation, created, 0);
    return info;
  }


  @Override
  public Collection<VetoInformation> listVetos() {
    List<VetoInformation> result = new ArrayList<>();
    try {
      SharedResourceRequestResult<SharedResourceVeto> vetoData = srm.readAll(XYNA_VETO_SR_DEF);
      if (!vetoData.isSuccess() || vetoData.getResources() == null) {
        Collections.emptyList();
      }
  
      for (SharedResourceInstance<SharedResourceVeto> instance : vetoData.getResources()) {
        SharedResourceVeto value = instance.getValue();
        OrderInformation orderInfo = new OrderInformation(value.usingOrderId, value.usingRootOrderId, value.usingOrderType);
        VetoInformation info = new VetoInformation(instance.getId(), orderInfo, value.sharedOrderIds, value.pendingExclusiveOrderId,
                                                   value.documentation, instance.getCreated(), 0);
        result.add(info);
      }
    } catch (Exception e) {
      logger.error("Error in list shared resource vetos. " + e.getMessage(), e);
    }
    return result;
  }


  @Override
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException {
    List<VetoInformation> filtered = CollectionUtils.filter(listVetos(), new VetoFilter(select));
    int countAll = filtered.size();
    if (maxRows >= 0) {
      filtered = filtered.subList(0, Math.min(countAll, maxRows));
    }
    List<VetoInformationStorable> viss = CollectionUtils.transform(filtered, VetoInformationStorable.fromVetoInformation);
    return new VetoSearchResult(viss, countAll);
  }


  @Override
  public VetoManagementAlgorithmType getAlgorithmType() {
    return VetoManagementAlgorithmType.SharedResourceManagement;
  }


  @Override
  public String showInformation() {
    return getAlgorithmType() + ": " + getAlgorithmType().getDocumentation().get(DocumentationLanguage.EN);
  }

  
  public static class SharedResourceVeto {
    public boolean initialized;
    public Long usingOrderId;
    public Long usingRootOrderId;
    public String usingOrderType;
    public String documentation;
    public ArrayList<Long> sharedOrderIds;
    public Long pendingExclusiveOrderId;
    
    
    public SharedResourceVeto() {
      initialized = false;
    }
    
    public SharedResourceVeto(Long usingOrderId, Long usingRootOrderId, String usingOrderType, String documentation,
                              List<Long> sharedOrderIds, Long pendingExclusiveOrderId) {
      this.initialized = true;
      this.usingOrderId = usingOrderId;
      this.usingRootOrderId = usingRootOrderId;
      this.usingOrderType = usingOrderType;
      this.documentation = documentation;
      if (sharedOrderIds != null) {
        this.sharedOrderIds = new ArrayList<>(sharedOrderIds);
      }
      this.pendingExclusiveOrderId = pendingExclusiveOrderId;
    }
    
    public SharedResourceVeto(boolean initialized, Long usingOrderId, Long usingRootOrderId, String usingOrderType,
                              String documentation, List<Long> sharedOrderIds, Long pendingExclusiveOrderId) {
      this.initialized = initialized;
      this.usingOrderId = usingOrderId;
      this.usingRootOrderId = usingRootOrderId;
      this.usingOrderType = usingOrderType;
      this.documentation = documentation;
      if (sharedOrderIds != null) {
        this.sharedOrderIds = new ArrayList<>(sharedOrderIds);
      }
      this.pendingExclusiveOrderId = pendingExclusiveOrderId;
    }
    
    public SharedResourceVeto cloneInitialized() {
      return new SharedResourceVeto(true, usingOrderId, usingRootOrderId, usingOrderType, documentation,
                                    sharedOrderIds, pendingExclusiveOrderId);
    }

    public boolean isEmpty() {
      if (usingOrderId != null) { return false; }
      if (pendingExclusiveOrderId != null) { return false; }
      if ((sharedOrderIds != null) && (!sharedOrderIds.isEmpty())) {
        return false;
      }
      return true;
    }
    
    public String asString() {
      StringBuilder s = new StringBuilder();
      s.append("{");
      s.append("initialized: ").append(initialized);
      s.append(", usingOrderId: ").append(usingOrderId);
      s.append(", pendingExclusiveOrderId: ").append(pendingExclusiveOrderId);
      s.append(", sharedOrderIds: ");
      if (sharedOrderIds == null) { s.append("null"); }
      else {
        s.append("[");
        boolean isfirst = true;
        for (Long val : sharedOrderIds) {
          if (isfirst) { isfirst = false; }
          else { s.append(", "); }
          s.append(val);
        }
        s.append("]");
      }
      s.append(" }");
      return s.toString();
    }
  }
  
  
  public static class SRVetoHelper {
    
    public SharedResourceVeto buildEmpty() {
      return new SharedResourceVeto();
    }

    /*
    public SharedResourceVeto build(Long usingOrderId, Long usingRootOrderId, String usingOrderType, String documentation,
                              List<Long> sharedOrderIds, Long pendingExclusiveOrderId) {
      SharedResourceVeto ret = new SharedResourceVeto();
      ret.initialized = true;
      ret.usingOrderId = usingOrderId;
      ret.usingRootOrderId = usingRootOrderId;
      ret.usingOrderType = usingOrderType;
      ret.documentation = documentation;
      ret.sharedOrderIds = new ArrayList<>(sharedOrderIds);
      ret.pendingExclusiveOrderId = pendingExclusiveOrderId;
      return ret;
    }
    */
    
    /*
    private SharedResourceVeto build(boolean initialized, Long usingOrderId, Long usingRootOrderId, String usingOrderType,
                               String documentation, List<Long> sharedOrderIds, Long pendingExclusiveOrderId) {
      SharedResourceVeto ret = new SharedResourceVeto();
      ret.initialized = initialized;
      ret.usingOrderId = usingOrderId;
      ret.usingRootOrderId = usingRootOrderId;
      ret.usingOrderType = usingOrderType;
      ret.documentation = documentation;
      if (sharedOrderIds != null) {
        ret.sharedOrderIds = new ArrayList<>(sharedOrderIds);
      }
      ret.pendingExclusiveOrderId = pendingExclusiveOrderId;
      return ret;
    }
    */
    /*
    public SharedResourceVeto doClone() {
      return new SharedResourceVeto(initialized, usingOrderId, usingRootOrderId, usingOrderType, documentation,
                                    sharedOrderIds, pendingExclusiveOrderId);
    }
  */
    
    /*
    public SharedResourceVeto cloneInitialized(SharedResourceVeto veto) {
      return build(true, veto.usingOrderId, veto.usingRootOrderId, veto.usingOrderType, veto.documentation,
                   veto.sharedOrderIds, veto.pendingExclusiveOrderId);
    }

    public boolean isEmpty(SharedResourceVeto veto) {
      if (veto.usingOrderId != null) { return false; }
      if (veto.pendingExclusiveOrderId != null) { return false; }
      if ((veto.sharedOrderIds != null) && (!veto.sharedOrderIds.isEmpty())) {
        return false;
      }
      return true;
    }
    */
  }
  

  private static class DocumentationContainer {

    private String documentation;
  }
  
  
  public static class VetoMap {
    private Map<String, SharedResourceInstance<SharedResourceVeto>> _map = new HashMap<>();
    
    protected VetoMap() {}
    
    public VetoMap(List<SharedResourceInstance<SharedResourceVeto>> list) {
      if (list == null) { return; }
      for (SharedResourceInstance<SharedResourceVeto> item : list) {
        _map.put(item.getId(), item);
      }
    }
    
    public boolean contains(String id) {
      return _map.containsKey(id);
    }
    
    public SharedResourceInstance<SharedResourceVeto> getVeto(String id) {
      return _map.get(id);
    }
  }
  
  
  public static enum UpdaterMode {
    EXPECT_ORDER_START_ALLOWED, ORDER_START_DISALLOWED;
  }
  
  
  public static class VetoUpdateData {
    private final SharedResourceVeto veto;
    private boolean orderStartDisallowed = false;
    private boolean independentOfOrderStart = false;
    
    public VetoUpdateData(SharedResourceVeto veto) {
      this.veto = veto;
    }
    public boolean isIndependentOfOrderStart() {
      return independentOfOrderStart;
    }
    public VetoUpdateData setIndependentOfOrderStart(boolean independentOfOrderStart) {
      this.independentOfOrderStart = independentOfOrderStart;
      return this;
    }
    public SharedResourceVeto getVeto() {
      return veto;
    }
    public boolean isOrderStartDisallowed() {
      return orderStartDisallowed;
    }
    public VetoUpdateData setOrderStartDisallowed(boolean orderStartDisallowed) {
      this.orderStartDisallowed = orderStartDisallowed;
      return this;
    }
  }
  
  
  public static class VetoAllocator {

    public VetoUpdateData allocateExclusiveVetoWithoutOrderStart(OrderInformation orderInfo,
                                                                 SharedResourceVeto veto) {
      if ((veto.sharedOrderIds != null) && (!veto.sharedOrderIds.isEmpty())) {
        if (veto.pendingExclusiveOrderId == null) {
          veto.pendingExclusiveOrderId = orderInfo.getOrderId();
          return new VetoUpdateData(veto);
        }
      }
      return new VetoUpdateData(veto);
    }
    
    public VetoUpdateData allocateExclusiveVeto(OrderInformation orderInfo, SharedResourceVeto veto) {
      if (!veto.initialized) {
        return new VetoUpdateData(this.createExclusiveSRVeto(orderInfo));
      }
      if (veto.usingOrderId != null) {
        if (!orderInfo.getOrderId().equals(veto.usingOrderId)) {
          return new VetoUpdateData(veto).setOrderStartDisallowed(true);
        }
        return new VetoUpdateData(veto);
      }
      if ((veto.sharedOrderIds != null) && (!veto.sharedOrderIds.isEmpty())) {
        if (veto.pendingExclusiveOrderId == null) {
          veto.pendingExclusiveOrderId = orderInfo.getOrderId();
          return new VetoUpdateData(veto).setOrderStartDisallowed(true).setIndependentOfOrderStart(true);
        }
        return new VetoUpdateData(veto).setOrderStartDisallowed(true);
      }
      if (veto.pendingExclusiveOrderId != null) {
        if (!veto.pendingExclusiveOrderId.equals(orderInfo.getOrderId())) {
          return new VetoUpdateData(veto).setOrderStartDisallowed(true);
        }
      }
      return new VetoUpdateData(this.createExclusiveSRVeto(orderInfo));
    }
  
    public VetoUpdateData allocateSharedVeto(OrderInformation orderInfo, SharedResourceVeto veto) {
      if (!veto.initialized) {
        return new VetoUpdateData(this.createSharedSRVeto(orderInfo));
      }
      if (veto.usingOrderId != null) {
        return new VetoUpdateData(veto).setOrderStartDisallowed(true);
      }
      if (veto.pendingExclusiveOrderId != null) {
        return new VetoUpdateData(veto).setOrderStartDisallowed(true);
      }
      if (veto.sharedOrderIds == null) {
        veto.sharedOrderIds = new ArrayList<>();
        veto.sharedOrderIds.add(orderInfo.getOrderId());
        return new VetoUpdateData(veto);
      }
      if (veto.sharedOrderIds.contains(orderInfo.getOrderId())) { 
        return new VetoUpdateData(veto);
      }
      veto.sharedOrderIds.add(orderInfo.getOrderId());
      return new VetoUpdateData(veto);
    }
    
    private SharedResourceVeto createExclusiveSRVeto(OrderInformation orderInformation) {
      SharedResourceVeto ret = new SharedResourceVeto();
      ret.usingOrderId = orderInformation.getOrderId();
      ret.usingOrderType = orderInformation.getOrderType();
      ret.usingRootOrderId = orderInformation.getRootOrderId();
      return ret;
    }
    
    private SharedResourceVeto createSharedSRVeto(OrderInformation orderInformation) {
      SharedResourceVeto ret = new SharedResourceVeto();
      ret.sharedOrderIds = new ArrayList<>();
      ret.sharedOrderIds.add(orderInformation.getOrderId());
      return ret;
    }
  }
  
  
  public static class VetoRemover implements Function<SharedResourceInstance<SharedResourceVeto>,
                                                      SharedResourceInstance<SharedResourceVeto>>,
                                             Predicate<SharedResourceInstance<SharedResourceVeto>> {
    private final long _orderId;
    private long _now = System.currentTimeMillis();
    
    public VetoRemover(long orderId) {
      this._orderId = orderId;
    }
    
    public boolean test(SharedResourceInstance<SharedResourceVeto> sri) {
      if (sri == null) { return false; }
      if (sri.getValue() == null) { return false; }
      SharedResourceVeto veto = sri.getValue();
      
      if ((veto.sharedOrderIds != null) &&
          (veto.sharedOrderIds.contains(_orderId))) {
        return true;
      }
      if (Objects.equals(_orderId, veto.pendingExclusiveOrderId)) {
        return true;
      }
      return false;
    }
    
    @Override
    public SharedResourceInstance<SharedResourceVeto> apply(SharedResourceInstance<SharedResourceVeto> sri) {
      if (sri == null) { return null; }
      if (sri.getValue() == null) { return null; }
      SharedResourceVeto veto = sri.getValue().cloneInitialized();
      boolean matches = false;
      if ((veto.sharedOrderIds != null) &&
          (veto.sharedOrderIds.contains(_orderId))) {
        veto.sharedOrderIds.removeIf(x -> x == _orderId);
        matches = true;
      }
      if (Objects.equals(_orderId, veto.pendingExclusiveOrderId)) {
        veto.pendingExclusiveOrderId = null;
        matches = true;
      }
      if (matches) {
        if (veto.isEmpty()) {
          veto = new SharedResourceVeto();
        }
        return new SharedResourceInstance<>(sri.getId(), _now, veto);
      }
      return sri;
    }
  }
  
  
  public static class VetoUpdater implements Function<SharedResourceInstance<SharedResourceVeto>,
                                                      SharedResourceInstance<SharedResourceVeto>> {
    private long _now = System.currentTimeMillis();
    private OrderInformation _orderInfo;
    private Set<String> _exclusiveVetos = new HashSet<>();
    private Set<String> _sharedVetos = new HashSet<>();
    private List<String> _vetoIds = new ArrayList<>();
    private UpdaterMode _mode;
    private boolean _orderStartDisallowed = false;
    private VetoAllocator _allocator = new VetoAllocator();
    
    public VetoUpdater(UpdaterMode mode, List<String> exclusiveVetos, List<String> sharedVetos, OrderInformation orderInformation) {
      this._mode = mode;
      this._orderInfo = orderInformation;
      if (exclusiveVetos != null) {
        this._vetoIds.addAll(exclusiveVetos);
        this._exclusiveVetos.addAll(exclusiveVetos);
      }
      if (sharedVetos != null) {
        this._vetoIds.addAll(sharedVetos);
        this._sharedVetos.addAll(sharedVetos);
      }
    }
    
    @Override
    public SharedResourceInstance<SharedResourceVeto> apply(SharedResourceInstance<SharedResourceVeto> input) {
      if (input == null) { return null; }
      if (input.getValue() == null) { return null; }
      SharedResourceVeto veto = input.getValue().cloneInitialized();
      String id = input.getId();
      VetoUpdateData vud;
      if (_mode != UpdaterMode.EXPECT_ORDER_START_ALLOWED) {
        vud = _allocator.allocateExclusiveVetoWithoutOrderStart(_orderInfo, veto);
        return buildSri(id, vud.getVeto());
      }
      if (_exclusiveVetos.contains(id)) {
        vud = _allocator.allocateExclusiveVeto(_orderInfo, veto);
      } else if (_sharedVetos.contains(id)) {
        vud = _allocator.allocateSharedVeto(_orderInfo, veto);
      } else {
        return null;
      }
      if (vud == null) { return null; }
      if (vud.isOrderStartDisallowed()) {
        _orderStartDisallowed = true;
        return null;
      }
      return buildSri(id, vud.getVeto());
    }
    
    private SharedResourceInstance<SharedResourceVeto> buildSri(String id, SharedResourceVeto data) {
      return new SharedResourceInstance<>(id, _now, data.isEmpty() ? new SharedResourceVeto() : data);
    }
    
    public List<String> getVetoIds() {
      return _vetoIds;
    }

    public boolean isOrderStartDisallowed() {
      return _orderStartDisallowed;
    }
  }

}
