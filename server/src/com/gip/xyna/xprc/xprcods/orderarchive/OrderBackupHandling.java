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
package com.gip.xyna.xprc.xprcods.orderarchive;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Future;


import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.concurrent.MergableWork;
import com.gip.xyna.utils.concurrent.MergingWorkProcessor;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;


public class OrderBackupHandling {

  // It might be better if AllOrdersList uses this as well, as currently additional backups (that won't actually be able to free memory) could be performed 
  
  private static MergingWorkProcessor<Long, ODSConnection, Void> orderBackupProcessor = MergingWorkProcessor.defaultMergingWorkProcessor("OrderBackupHandling");
  
  
  public static void backup(SchedulingOrder so) throws PersistenceLayerException {
    orderBackupProcessor.submit(new OrderBackupTask(so));
  }
  
  
  public static boolean isBackupInProgress(XynaOrderServerExtension xose) {
    Collection<Future<Void>> col = orderBackupProcessor.cancel(xose.getId());
    return col.size() > 0;
  }
  
  
  private static class OrderBackupTask implements MergableWork<Long, ODSConnection, Void> {
    
    private SchedulingOrder so;
    
    OrderBackupTask(SchedulingOrder so) {
      this.so = so;
    }

    public Long getKey() {
      return so.getRootOrderId();
    }

    public ODSConnection initSharedProcessingData(Iterator<MergableWork<Long, ODSConnection, Void>> workloadIterator) {
      return ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    }

    public Void process(ODSConnection sharedProcessingData) throws Exception {
      if (so.getXynaOrderOrNull() != null) {
        WarehouseRetryExecutor.buildCriticalExecutor().
        connection(sharedProcessingData).
        storables(new StorableClassList(OrderInstanceBackup.class, OrderInstanceDetails.class)).
        execute(new WarehouseRetryExecutableNoResult() {
          
          public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
            XynaOrderServerExtension xo = so.getXynaOrderOrNull();
            if( xo == null ) {
              //Auftrag ist schon gebackupt wegen OOM-Schutz
            } else {
              OrderArchive oa = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
              for (XynaOrderServerExtension xoOfFamily : xo.getRootOrder().getOrderAndChildrenRecursively()) {
                oa.backup(xoOfFamily, BackupCause.WAITING_FOR_CAPACITY, con);
              }
            }
          }
        });
      } // if not in Memory it was already backuped from AllOrdersList
      return null;
    }

    public void finalizeSharedProcessingData(ODSConnection sharedData, Iterator<MergableWork<Long, ODSConnection, Void>> workloadIterator) throws Exception {
      try {
        sharedData.commit();
        while (workloadIterator.hasNext()) {
          OrderBackupTask task = (OrderBackupTask) workloadIterator.next();
          XynaOrderServerExtension xo = task.so.getXynaOrderOrNull();
          if( xo == null ) {
            //XynaOrder ist bereits wegen OOM-Schutz gebackupt
          } else {
            for (XynaOrderServerExtension xoOfFamily : xo.getRootOrder().getOrderAndChildrenRecursively()) {
              xoOfFamily.setHasBeenBackuppedInScheduler(true);
            }
          }
        }
      } finally {
        try {
          sharedData.closeConnection();
        } finally {
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaScheduler().notifyScheduler();
        }
      }
    }

    public boolean merge(MergableWork<Long, ODSConnection, Void> other) {
      if (other instanceof OrderBackupTask &&
          so.getRootOrderId().equals(((OrderBackupTask)other).so.getRootOrderId()) /* this check should not be necessary with our choosen key */) {
        this.so = ((OrderBackupTask)other).so;
        return true;
      } else {
        throw new IllegalArgumentException("MergableWork other then OrderBackupTask (got " + other + ") should not appear in this processor!");
      }
    }

    public boolean isMergeable() {
      return true;
    }
    
  }
  
}


