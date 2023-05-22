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

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable.BatchProcessState;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;


/**
 * RuntimeInformationUpdater f�hrt ein Update auf einem BatchProcessRuntimeInformationStorable aus.
 *
 * Hierbei werden folgende Tasks unterst�tzt:
 * <ul>
 * <li> InputData: lastInputGeneratorId wird zu openDataPlanning hinzugef�gt und persistiert</li>
 * <li> SlaveAcknowledged: Running wird um eins erh�ht und Inputs aktualisiert</li>
 * <li> SlaveFinished: Finished wird um eins erh�ht, Running um eins reduziert </li>
 * <li> SlaveFailed: Failed wird um eins erh�ht, Running um eins reduziert </li>
 * <li> Pause: der Status wird auf PAUSED gesetzt </li>
 * <li> Continue: der Status wird auf RUNNING gesetzt </li>
 * <li> Cancel: der Status wird auf CANCELED gesetzt </li>
 * </ul>
 */
public class RuntimeInformationUpdater implements WarehouseRetryExecutableNoResult {
  
  private BatchProcessRuntimeInformationStorable runtimeInformation;
  private UpdateTask updateTask;
  private ODSConnection con;
  private String lastInputId;
  private String currentInputId;
  private String pauseCause;
  private OrderInstanceStatus currentSlaveState;
  
  
  public enum UpdateTask {
    InputData () {
      @Override
      public void update(BatchProcessRuntimeInformationStorable runtimeInformation, RuntimeInformationUpdater runtimeInformationUpdater) {
        runtimeInformation.setLastInputGeneratorID(runtimeInformationUpdater.lastInputId);
        runtimeInformation.addOpenDataPlanning( runtimeInformationUpdater.currentInputId );
      }
    },
    
    SlaveAcknowledged () {
      @Override
      public void update(BatchProcessRuntimeInformationStorable runtimeInformation, RuntimeInformationUpdater runtimeInformationUpdater) {
        runtimeInformation.setRunning(runtimeInformation.getRunning() + 1);
        if (runtimeInformationUpdater.currentInputId != null && runtimeInformation.getOpenDataPlanning() != null) {
          runtimeInformation.getOpenDataPlanning().remove(runtimeInformationUpdater.currentInputId);
        }
      }
    },
    
    SlaveFinished () {
      @Override
      public void update(BatchProcessRuntimeInformationStorable runtimeInformation, RuntimeInformationUpdater runtimeInformationUpdater) {
        runtimeInformation.setFinished(runtimeInformation.getFinished() + 1);
        runtimeInformation.decrRunningIfBackuped(runtimeInformationUpdater.currentSlaveState);
      }
    },
    
    SlaveFailed () {
      @Override
      public void update(BatchProcessRuntimeInformationStorable runtimeInformation, RuntimeInformationUpdater runtimeInformationUpdater) {
        runtimeInformation.setFailed(runtimeInformation.getFailed() + 1);
        runtimeInformation.decrRunningIfBackuped(runtimeInformationUpdater.currentSlaveState);
      }
    },
    
    Pause () {
      @Override
      public void update(BatchProcessRuntimeInformationStorable runtimeInformation, RuntimeInformationUpdater runtimeInformationUpdater) {
        runtimeInformation.setState(BatchProcessState.PAUSED);
        runtimeInformation.setPauseCause(runtimeInformationUpdater.pauseCause);
      }
    },
    
    Continue() {
      @Override
      public void update(BatchProcessRuntimeInformationStorable runtimeInformation, RuntimeInformationUpdater runtimeInformationUpdater) {
        runtimeInformation.setState(BatchProcessState.RUNNING);
        runtimeInformation.setPauseCause(null);
      }
    },
    
    Cancel () {
      @Override
      public void update(BatchProcessRuntimeInformationStorable runtimeInformation, RuntimeInformationUpdater runtimeInformationUpdater) {
        runtimeInformation.setState(BatchProcessState.CANCELED);
      }
    },
    
    Timeout () {
      @Override
      public void update(BatchProcessRuntimeInformationStorable runtimeInformation, RuntimeInformationUpdater runtimeInformationUpdater) {
        runtimeInformation.setState(BatchProcessState.TIMEOUT);
      }
    };
    
    
    public abstract void update(BatchProcessRuntimeInformationStorable runtimeInformation, RuntimeInformationUpdater runtimeInformationUpdater);
  }

  
  public RuntimeInformationUpdater (BatchProcessRuntimeInformationStorable runtimeInformation) {
    this.runtimeInformation = runtimeInformation;
  }

  public void setCurrentInput (String lastInputId, String currentInputId) {
    updateTask = UpdateTask.InputData;
    this.lastInputId = lastInputId;
    this.currentInputId = currentInputId;
  }
  
  public void acknowledgeSlave (ODSConnection con, String currentInputId) {
    updateTask = UpdateTask.SlaveAcknowledged;
    this.con = con;
    this.currentInputId = currentInputId;
  }
  
  public void terminateSlave (OrderInstanceStatus slaveState, boolean success) {
    updateTask = success ? UpdateTask.SlaveFinished : UpdateTask.SlaveFailed;
    this.currentSlaveState = slaveState;
  }

  public void pauseBatchProcess (String pauseCause) {
    updateTask = UpdateTask.Pause;
    this.pauseCause = pauseCause;
  }

  public void continueBatchProcess () {
    updateTask = UpdateTask.Continue;
  }

  public void cancelBatchProcess () {
    updateTask = UpdateTask.Cancel;
  }
  
  public void timeoutBatchProcess() {
    updateTask = UpdateTask.Timeout;
  }


  public void update() throws PersistenceLayerException {
    if (! runtimeInformation.isUpdateAllowed()) {
      return; //darf nicht geschrieben werden
    }
    //hier kein Lock holen, damit Reihenfolge Connection/Lock immer gleich ist
    WarehouseRetryExecutor.buildCriticalExecutor().
      connection(con).
      storables(this.getStorableClassList()).
      execute(this);
  }
  
  public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
    runtimeInformation.getLock().lock();
    try {
      if (runtimeInformation.isUpdateAllowed()) {
        updateTask.update(runtimeInformation,this);
        con.persistObject(runtimeInformation);
      }
    } finally {
      runtimeInformation.getLock().unlock();
    }
  }
  
  private StorableClassList getStorableClassList() {
    return new StorableClassList(BatchProcessRuntimeInformationStorable.class);
  }
 }
