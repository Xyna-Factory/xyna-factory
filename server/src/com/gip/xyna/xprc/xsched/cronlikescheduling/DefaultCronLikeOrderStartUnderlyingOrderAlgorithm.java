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
package com.gip.xyna.xprc.xsched.cronlikescheduling;



import java.sql.Date;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.xfractwfe.OrderDeathException;
import com.gip.xyna.xprc.xpce.AbstractConnectionAwareAck;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;



public class DefaultCronLikeOrderStartUnderlyingOrderAlgorithm
    implements CronLikeOrderStartUnderlyingOrderAlgorithm  {

 public static final String CLO_ALREADY_EXECUTED_OR_CHANGED = 
     "It seems that the cron like order which started this order was already executed"
         + " or the execution time was changed. Aborting the execution of this order to prevent"
         + " repeated execution.";
  private static final long serialVersionUID = 1L;
  private static final StorableClassList BACKUP_STORABLES = new StorableClassList(OrderInstanceBackup.class,CronLikeOrder.class);

  private static final Logger logger = CentralFactoryLogging
      .getLogger(DefaultCronLikeOrderStartUnderlyingOrderAlgorithm.class);


  private final class CronLikeOrderAck extends AbstractConnectionAwareAck {

    private static final long serialVersionUID = 1L;

    private final CronLikeOrder order;


    public CronLikeOrderAck(CronLikeOrder order) {
      super(null);
      this.order = order;
    }

    @Override
    public StorableClassList backupStorables() {
      return BACKUP_STORABLES;
    }
    

    public void acknowledgeSchedulerEntry(final XynaOrderServerExtension xose)
        throws XPRC_OrderEntryCouldNotBeAcknowledgedException {

      WarehouseRetryExecutableNoException<Boolean> wre = 
        new WarehouseRetryExecutableNoException<Boolean>() {

        public Boolean executeAndCommit(ODSConnection con) throws PersistenceLayerException {
          try {
            if (!internally(xose, con)) {
              con.rollback();  //TODO schädigt evtl. äußere Transaktion
              return false;
            }
            return true;
          } catch (Error e) {
            updateCLOonError(con); //TODO schädigt evtl. äußere Transaktion
            throw e;
          } catch (RuntimeException e) {
            updateCLOonError(con);
            throw e;
          }
          
        }
      };

      try {
        boolean successfull =
            WarehouseRetryExecutor.buildCriticalExecutor().
              connection(getConnection()).
              storables(backupStorables()).
              execute(wre);

        if (!successfull) {
          Exception e = new Exception(CLO_ALREADY_EXECUTED_OR_CHANGED);
          throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(e);
        }
        
      } catch (XNWH_RetryTransactionException e) {
        if (new CronLikeOrder().getClusterState(ODSConnectionType.DEFAULT) == ClusterState.DISCONNECTED_SLAVE) {
          throw new OrderDeathException(e);
        } else {
          throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(e);
        }
      } catch (PersistenceLayerException ple) {
        throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(ple);
      }
    }
 
    private boolean internally(XynaOrderServerExtension cronTargetOrder, ODSConnection con)
        throws PersistenceLayerException {

      con.executeAfterRollback(new Runnable() {
        
        public void run() {
          logger.trace("executeAfterRollback");
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().unmarkAsNotToSchedule(order.getId());
          XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().recreateQueue();
        }
      });
      
      // CLO neu aus DB laden und dabei locken. Gibt die Sicherheit, dass die CLO nicht gleichzeitig ausgeführt wird
      // oder jetzt vor dem Ausführen geändert wird.
      final CronLikeOrder cronOrderDB = new CronLikeOrder(order.getId());
      try {
        con.queryOneRowForUpdate(cronOrderDB);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        // Auftrag muss schon ausgeführt wurden sein.
        logger.warn("CronLikeOrder with id <" + order.getId() + "> was not found. Abort starting the order.");
        return false;
      }
      if(!cronOrderDB.getNextExecution().equals(order.getNextExecution())) {
        // Ausführungszeit wurde geändert -> Abbruch der Ausführung
        // U.U. wurde der Auftrag schon ausgeführt.
        logger.warn("CronLikeOrder with id <" + order.getId() + "> was changed. Abort starting the order.");
        return false;
      }
      
      if (XynaProperty.XYNA_BACKUP_DURING_CRON_LIKE_SCHEDULING.get()) {
        try {
          XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
              .backup(cronTargetOrder, BackupCause.ACKNOWLEDGED, con);
          cronTargetOrder.setHasBeenBackuppedInScheduler(true);
        } catch (XNWH_RetryTransactionException e) {
          throw e;
        } catch (PersistenceLayerException e) {
          logger.warn("Failed to create backup for XynaOrder <" + cronTargetOrder.getId() + ">", e);
        }
      }
      
      updateCLOinDB(cronOrderDB, con);
      
      return true;
    }

    private void updateCLOonError(ODSConnection con) {
      try {
        logger.info("Trying update CronLikeOrder with id <" + order.getId() + "> after Error.");
        con.rollback();
        con.queryOneRowForUpdate(order);
        updateCLOinDB(order, con);
        con.commit();
      } catch (Throwable e) {
        logger.error("Error while trying update CronLikeOrder with id <" + order.getId() + "> after Error.", e);
      }
    }

    private void updateCLOinDB(final CronLikeOrder cronOrderDB, ODSConnection con) throws PersistenceLayerException {
      if (cronOrderDB.isSingleExecution()) {
        try {
          
          con.executeAfterCommit(new Runnable() {
            
            public void run() {
              logger.trace("executeAfterCommit");
              XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().unmarkAsNotToSchedule(cronOrderDB.getId());
            }
          });
          
          CronLikeOrderHelpers.delete(cronOrderDB.getId(), con);
        } catch (XPRC_CronRemovalException e) {
          logger.warn("Failed to remove cron like order <" + cronOrderDB.getId() + "> from table <"
              + CronLikeOrder.TABLE_NAME + ">", e);
        }
      } else {
        try {
          if (logger.isTraceEnabled()) {
            String traceMsg =
                "Recalculate next execution time for cron like order with id <"
                    + cronOrderDB.getId()
                    + ">: Old execution time "
                    + Constants.defaultUTCSimpleDateFormat().format(new Date(cronOrderDB.getNextExecution()))
                    + " ("
                    + cronOrderDB.getNextExecution()
                    + ").";
            logger.trace(traceMsg);
          }
          cronOrderDB.calculateNextFutureExecutionTime();
          if (logger.isTraceEnabled()) {
            String traceMsg =
                "Next execution time for cron like order with id <"
                    + cronOrderDB.getId()
                    + ">: "
                    + Constants.defaultUTCSimpleDateFormat().format(new Date(cronOrderDB.getNextExecution())) + " ("
                    + cronOrderDB.getNextExecution() + ")";
            logger.trace(traceMsg);
          }
          cronOrderDB.incExecCount();
          CronLikeOrderHelpers.store(cronOrderDB, con);
          con.executeAfterCommit(new Runnable() {
            
            public void run() {
              logger.trace("executeAfterCommit");
              XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().tryAddNewOrder(cronOrderDB);
            }
          });
        } catch (PersistenceLayerException e) {
          logger.error("Failed to store updated information for cron like order information <" + cronOrderDB.getId()
              + "> in table <" + CronLikeOrder.TABLE_NAME + ">. There may be duplicate execution.", e);
          con.rollback();
          throw e;
        }
      }
    }

    @Override
    public String toString() {
      return new StringBuilder().append(super.toString()).append(" - CronLikeOrderID=").append(order.getId())
          .toString();
    }

    @Override
    public void handleErrorAtPlanning(XynaOrderServerExtension xose, Throwable throwable) {      
      WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {
        
        public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
          updateCLOonError(con);
        }
      };
      
      try {
        WarehouseRetryExecutor.buildCriticalExecutor().
          connection(getConnection()).
          storable(CronLikeOrder.class).
          execute(wre);
      } catch (PersistenceLayerException e) {
        logger.error("Could not handle failed cron like order.", e);
      }
    }
  };

  public static DefaultCronLikeOrderStartUnderlyingOrderAlgorithm singleInstance =
      new DefaultCronLikeOrderStartUnderlyingOrderAlgorithm();


  private DefaultCronLikeOrderStartUnderlyingOrderAlgorithm() {
  }


  public void startUnderlyingOrder(CronLikeOrder cronLikeOrder, CronLikeOrderCreationParameter clocp,
                                   ResponseListener rl) {
    XynaOrderServerExtension xose = new XynaOrderServerExtension(clocp);
    OrderContextServerExtension ocse = new OrderContextServerExtension(xose);
    ocse.set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY,
             new CronLikeOrderAck(cronLikeOrder));
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrder(xose, rl, ocse);
  }

}
