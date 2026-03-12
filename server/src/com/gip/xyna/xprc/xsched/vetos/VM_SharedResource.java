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
import java.util.List;
import java.util.function.Function;
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
      new KryoSerializedSharedResourceDefinition<>(VetoManagement.XYNA_VETO_SR, SharedResourceVeto.class);
  private static final Logger logger = CentralFactoryLogging.getLogger(VM_SharedResource.class);
  private final SharedResourceManagement srm;


  public VM_SharedResource() {
    srm = XynaFactory.getInstance().getXynaNetworkWarehouse().getSharedResourceManagement();
  }


  @Override
  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> vetos, long urgency) {
    List<SharedResourceInstance<SharedResourceVeto>> vetosToCreate = new ArrayList<>();
    Long now = System.currentTimeMillis();
    for (String vetoName : vetos) {
      SharedResourceVeto value = new SharedResourceVeto();
      value.usingOrderId = orderInformation.getOrderId();
      value.usingOrderType = orderInformation.getOrderType();
      value.usingRootOrderId = orderInformation.getRootOrderId();
      SharedResourceInstance<SharedResourceVeto> instance = new SharedResourceInstance<>(vetoName, now, value);
      vetosToCreate.add(instance);
    }
    SharedResourceRequestResult<SharedResourceVeto> createResult = srm.create(XYNA_VETO_SR_DEF, vetosToCreate);
    if(logger.isDebugEnabled()) {
      logger.debug("create Veto Result for " + orderInformation.getOrderId() + ": success? " + createResult.isSuccess());
    }
    return createResult.isSuccess() ? VetoAllocationResult.SUCCESS : VetoAllocationResult.FAILED;
  }


  @Override
  public void undoAllocation(OrderInformation orderInformation, List<String> vetos) {
    freeVetosForced(orderInformation.getOrderId());
  }


  @Override
  public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos) {
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
    SharedResourceRequestResult<SharedResourceVeto> readVetosResult = srm.readAll(XYNA_VETO_SR_DEF);
    if (!readVetosResult.isSuccess()) {
      return false;
    }
    List<SharedResourceInstance<SharedResourceVeto>> vetos = readVetosResult.getResources();
    vetos = vetos == null ? Collections.emptyList() : vetos;
    vetos.removeIf(x -> x.getValue() != null && x.getValue().usingOrderId != orderId);

    List<String> vetoIds = vetos.stream().map(x -> x.getId()).collect(Collectors.toList());

    SharedResourceRequestResult<SharedResourceVeto> deleteVetosResult = srm.delete(XYNA_VETO_SR_DEF, vetoIds);
    return deleteVetosResult.isSuccess();
  }


  @Override
  public void allocateAdministrativeVeto(AdministrativeVeto administrativeVeto)
      throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException {

    SharedResourceVeto srVeto = new SharedResourceVeto();
    srVeto.documentation = administrativeVeto.getDocumentation();
    srVeto.usingOrderId = AdministrativeVeto.ADMIN_VETO_ORDERID;
    srVeto.usingOrderType = AdministrativeVeto.ADMIN_VETO_ORDERTYPE;
    srVeto.usingRootOrderId = AdministrativeVeto.ADMIN_VETO_ORDERID;
    Long now = System.currentTimeMillis();
    SharedResourceInstance<SharedResourceVeto> veto = new SharedResourceInstance<SharedResourceVeto>(administrativeVeto.getName(), now, srVeto);
    SharedResourceRequestResult<SharedResourceVeto> createResult = srm.create(XYNA_VETO_SR_DEF, List.of(veto));
    if (!createResult.isSuccess()) {
      SharedResourceRequestResult<SharedResourceVeto> readResult = srm.read(XYNA_VETO_SR_DEF, List.of(veto.getId()));
      Long usingOrderId = -2l;
      if (readResult.isSuccess() && readResult.getResources() != null && !readResult.getResources().isEmpty()) {
        usingOrderId = readResult.getResources().get(0).getValue().usingOrderId;
      }
      throw new XPRC_AdministrativeVetoAllocationDenied(administrativeVeto.getName(), usingOrderId);
    }
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
    SharedResourceRequestResult<SharedResourceVeto> readResult = srm.read(XYNA_VETO_SR_DEF, List.of(administrativeVeto.getName()));
    if (!readResult.isSuccess() || readResult.getResources() == null || readResult.getResources().isEmpty()) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    SharedResourceRequestResult<SharedResourceVeto> deleteResult = srm.delete(XYNA_VETO_SR_DEF, List.of(administrativeVeto.getName()));
    if (!deleteResult.isSuccess()) {
      throw new XPRC_AdministrativeVetoDeallocationDenied(administrativeVeto.getName());
    }
    SharedResourceVeto value = readResult.getResources().get(0).getValue();
    OrderInformation orderInfo = new OrderInformation(value.usingOrderId, value.usingRootOrderId, value.usingOrderType);
    Long created = administrativeVeto.getCreated();
    VetoInformation info = new VetoInformation(readResult.getResources().get(0).getId(), orderInfo, value.documentation, created, 0);
    return info;
  }


  @Override
  public Collection<VetoInformation> listVetos() {
    List<VetoInformation> result = new ArrayList<>();

    SharedResourceRequestResult<SharedResourceVeto> vetoData = srm.readAll(XYNA_VETO_SR_DEF);
    if (!vetoData.isSuccess() || vetoData.getResources() == null) {
      Collections.emptyList();
    }

    for (SharedResourceInstance<SharedResourceVeto> instance : vetoData.getResources()) {
      SharedResourceVeto value = instance.getValue();
      OrderInformation orderInfo = new OrderInformation(value.usingOrderId, value.usingRootOrderId, value.usingOrderType);
      VetoInformation info = new VetoInformation(instance.getId(), orderInfo, value.documentation, instance.getCreated(), 0);
      result.add(info);
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


  private static class SharedResourceVeto {

    public Long usingOrderId;
    public Long usingRootOrderId;
    public String usingOrderType;
    public String documentation;
  }

  private static class DocumentationContainer {

    private String documentation;
  }
}
