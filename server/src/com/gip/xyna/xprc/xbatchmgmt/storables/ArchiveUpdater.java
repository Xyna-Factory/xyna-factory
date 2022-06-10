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

import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;


/**
 * ArchiveUpdater führt ein Update auf einem BatchProcessArchiveStorable aus.
 *
 */
public class ArchiveUpdater implements WarehouseRetryExecutableNoResult {
  
  private static Logger logger = CentralFactoryLogging.getLogger(ArchiveUpdater.class);
  private UpdateTask updateTask;
  private BatchProcessArchiveStorable archive;
  private int canceled;
  private String label;
  private String component;
  
  public ArchiveUpdater(BatchProcessArchiveStorable archive) {
    this.archive = archive;
  }
  
  public void pauseBatchProcess() {
    this.updateTask = UpdateTask.Pause;
  }
  public void continueBatchProcess() {
    this.updateTask = UpdateTask.Continue;
  }

  public void canceled(int canceled) {
    this.updateTask = UpdateTask.Canceled;
    this.canceled = canceled;
  }

  public void labelAndComponent(String label, String component) {
    this.updateTask = UpdateTask.LabelAndComponent;
    this.label = label;
    this.component = component;
  }


    
  public void update() {
    Lock lock = getLock();
    lock.lock();
    try {
      updateTask.update(archive,this);
      WarehouseRetryExecutor.buildCriticalExecutor().
      storables(getStorableClassList()).
      execute(this);
    } catch (PersistenceLayerException e) {
      logger.warn("Could not update Archive for batchProcess "+ archive.getOrderId() );
      //FIXME Exception wird hier unterdrückt
    } finally {
      lock.unlock();
    }
  }

  private Lock getLock() {
    return archive.getLock();
  }

  private StorableClassList getStorableClassList() {
    return new StorableClassList(BatchProcessArchiveStorable.class);
  }

  public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
    con.persistObject(archive);
    con.commit();
  }
  
  
  private enum UpdateTask {
    
    Pause() {
      public void update(BatchProcessArchiveStorable archive,ArchiveUpdater archiveUpdater) {
        archive.setOrderStatus(OrderInstanceStatus.WAITING_FOR_BATCH_PROCESS);
      }
    },
    Continue() {
      public void update(BatchProcessArchiveStorable archive,ArchiveUpdater archiveUpdater) {
        archive.setOrderStatus(OrderInstanceStatus.SCHEDULING);
      }
    }, 
    Canceled() {
      public void update(BatchProcessArchiveStorable archive,ArchiveUpdater archiveUpdater) {
        archive.setCanceled(archiveUpdater.canceled);
      }
    },
    LabelAndComponent() {
      public void update(BatchProcessArchiveStorable archive,ArchiveUpdater archiveUpdater) {
        archive.setLabel(archiveUpdater.label);
        archive.setComponent(archiveUpdater.component);
      }
    };
    
    
   
    public abstract void update(BatchProcessArchiveStorable archive,
                                ArchiveUpdater archiveUpdater);
 
  }





}
