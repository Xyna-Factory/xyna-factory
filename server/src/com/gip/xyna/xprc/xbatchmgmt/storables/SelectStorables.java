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
package com.gip.xyna.xprc.xbatchmgmt.storables;

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResultOneException;

/**
 * WarehouseRetryExecutable, um alle Storables zu einem BatchProcess auszulesen.
 *
 */
public class SelectStorables implements WarehouseRetryExecutableNoResultOneException<XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY>{
  private BatchProcessRuntimeInformationStorable runtimeInformation;
  private BatchProcessArchiveStorable archiveData;
  private BatchProcessCustomizationStorable customizationData;
  private BatchProcessRestartInformationStorable restartInformation;
  
  private static StorableClassList storableClassList = new StorableClassList(BatchProcessRuntimeInformationStorable.class,
                                                                             BatchProcessArchiveStorable.class,
                                                                             BatchProcessCustomizationStorable.class,
                                                                             BatchProcessRestartInformationStorable.class);

  public SelectStorables(BatchProcessRuntimeInformationStorable runtimeInformation,
                         BatchProcessArchiveStorable archiveData,
                         BatchProcessCustomizationStorable customizationData,
                         BatchProcessRestartInformationStorable restartInformation) {
    this.runtimeInformation = runtimeInformation;
    this.archiveData = archiveData;
    this.customizationData = customizationData;
    this.restartInformation = restartInformation;
  }

  public void executeAndCommit(ODSConnection con) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    con.queryOneRow(archiveData);
    con.queryOneRow(runtimeInformation);
    con.queryOneRow(customizationData);
    con.queryOneRow(restartInformation);
  }
  
  public StorableClassList getStorableClassList() {
    return storableClassList;
  }
}
