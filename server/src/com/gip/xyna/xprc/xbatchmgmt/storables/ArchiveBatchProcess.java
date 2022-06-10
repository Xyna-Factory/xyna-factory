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
package com.gip.xyna.xprc.xbatchmgmt.storables;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;


/**
 * WarehouseRetryExecutable, um ein BatchProcessRuntimeInformationStorable
 * zu löschen, nachdem das entsprechende BatchProcessArchiveStorable angelegt wurde.
 *
 */
public class ArchiveBatchProcess implements WarehouseRetryExecutableNoResult {
  
  private BatchProcessRuntimeInformationStorable runtimeInformation;
  private BatchProcessArchiveStorable batchProcessArchive;
  private BatchProcessCustomizationStorable customization;
  private BatchProcessRestartInformationStorable restartInformation;
  private OrderInstanceStatus orderStatus;
  
  private static StorableClassList storableClassList = new StorableClassList(BatchProcessRuntimeInformationStorable.class,
                                                                             BatchProcessArchiveStorable.class,
                                                                             BatchProcessCustomizationStorable.class);
  
  public ArchiveBatchProcess(BatchProcessArchiveStorable batchProcessArchive,
                             BatchProcessRuntimeInformationStorable runtimeInformation,
                             BatchProcessCustomizationStorable customization,
                             BatchProcessRestartInformationStorable restartInformation,
                             OrderInstanceStatus orderStatus) {
    this.batchProcessArchive = batchProcessArchive;
    this.runtimeInformation = runtimeInformation;
    this.customization = customization;
    this.restartInformation = restartInformation;
    this.orderStatus = orderStatus;
  }
  
  public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
    //Es soll jetzt nichts mehr an den RuntimeInformations geändert werden.
    runtimeInformation.getLock().lock();
    try {
      runtimeInformation.setUpdateAllowed(false);
    } finally {
      runtimeInformation.getLock().unlock();
    }
    
    //OrderStatus setzen
    batchProcessArchive.setOrderStatus(orderStatus);
    
    //BatchProcessArchive Eintrag updaten
    batchProcessArchive.updateWithRuntimeInformation(runtimeInformation, true);
    batchProcessArchive.updateWithCustomization(customization);
    batchProcessArchive.updateWithRestartInformation(restartInformation);
    con.persistObject(batchProcessArchive);
    
    //BatchProcessRuntimeInformation Eintrag löschen
    con.deleteOneRow(runtimeInformation);
    
    //BatchProcessCustomizationStorable Eintrag löschen
    con.deleteOneRow(customization);
    
    //BatchProcessRestartInformationStorable Eintrag soll derzeit nicht gelöscht werden
    //Daher sogar persistieren, um Änderungen zu behalten
    con.persistObject(restartInformation);
    
    con.commit();
  }
  
  public StorableClassList getStorableClassList() {
    return storableClassList;
  }
}
