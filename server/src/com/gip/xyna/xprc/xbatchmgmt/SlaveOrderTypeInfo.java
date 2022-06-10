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
package com.gip.xyna.xprc.xbatchmgmt;

import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessArchiveStorable;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;


public class SlaveOrderTypeInfo {
  
  private String slaveOrderType;
  private XynaOrderServerExtension masterOrder;
  private RuntimeContext runtimeContext;
  private Long revision;
  private boolean hasChangedPriority;
  private boolean hasChangedCapacities;
  private int priority;
  private List<Capacity> capacities;
  
  public SlaveOrderTypeInfo(String slaveOrderType) {
    this.slaveOrderType = slaveOrderType;
    hasChangedPriority = true;
    hasChangedCapacities = true;
  }
  
  public void setMasterOrder(XynaOrderServerExtension masterOrder) {
    this.masterOrder = masterOrder;
  }

  public void fillFromBatchProcessArchive(BatchProcessArchiveStorable batchProcessArchiveData) {
    this.revision = batchProcessArchiveData.getRevision();
    runtimeContext = batchProcessArchiveData.getRuntimeContext();
    hasChangedPriority = true;
    hasChangedCapacities = true;
  }
  
  
  /**
   * Bestimmt die Ordertyp-Priorität der Slaves
   * @return
   */
  public int getPriority() {
    if( hasChangedPriority ) {
      priority = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS()
        .getPriorityManagement().determinePriority(slaveOrderType, revision, masterOrder);
      hasChangedPriority = false;
    }
    return priority;
  }


  public List<Capacity> getCapacities() {
    if( hasChangedCapacities ) {
      DestinationKey key = new DestinationKey(slaveOrderType, runtimeContext);
      RuntimeContext rc;
      try {
        rc = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionEngineDispatcher().getRuntimeContextDefiningOrderType(key);
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        rc = runtimeContext;
      }
      capacities = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase().getCapacities(new DestinationKey(slaveOrderType, rc));
      hasChangedCapacities = false;
    }
    return capacities;
  }

  public boolean hasChangedPriority() {
    return hasChangedPriority;
  }

  public boolean hasChangedCapacities() {
    return hasChangedCapacities;
  }

  public void orderTypeChanged() {
    hasChangedPriority = true;
    hasChangedCapacities = true;
  }

}
