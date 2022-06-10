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

package com.gip.xyna.xprc.xpce;

import java.io.Serializable;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;


/**
 * Only this class is allowed to change the value of "isRegisteredWithinXprcThreadMapping", which is important to make
 * the context object available for all threads during parallel execution
 */
public final class OrderContextServerExtension extends OrderContext {

  private static final long serialVersionUID = 1L;


  public static final String ACKNOWLEDGABLE_OBJECT_KEY = "xyna.acknowledgableObject";
  public static final String CREATION_ROLE_KEY = "xfmg.xopctrl.creationRole";


  public static interface AcknowledgableObject extends Serializable {

    public void acknowledgeSchedulerEntry(XynaOrderServerExtension xose)
        throws XPRC_OrderEntryCouldNotBeAcknowledgedException, XNWH_RetryTransactionException;
    
    public void handleErrorAtPlanning(XynaOrderServerExtension xose, Throwable throwable); 
  }


  /**
   * vererbt mapForSeriesFamily
   */
  private OrderContextServerExtension(OrderContext ctxse, XynaOrderServerExtension xo) {
    super(ctxse, xo);
  }


  public static OrderContextServerExtension createOrderContextFromExisting(OrderContext ocFromOtherOrder,
                                                                        XynaOrderServerExtension newOrder) {
    OrderContextServerExtension oc = new OrderContextServerExtension(ocFromOtherOrder, newOrder);
    return oc;
  }


  public OrderContextServerExtension(XynaOrderServerExtension savedOrder) {
    super(savedOrder);
  }


  /**
   * kann vom projekt nicht aufgerufen werden, weil dort nur {@link OrderContext} bekannt ist
   */
  public void set(String key, Serializable val) {
    super.set(key,val);
  }


  public void setSessionId(String value) {
    synchronized (this) {
      xo.setSessionId(value);
    }
  }


  /**
   * Existiert ein AbstractConnectionAwareAck-Object?
   * @return
   */
  public boolean hasAck() {
    return get(ACKNOWLEDGABLE_OBJECT_KEY) != null;
  }


  public final void acknowledgeSchedulerOrderEntry() throws XPRC_OrderEntryCouldNotBeAcknowledgedException,
      XNWH_RetryTransactionException {
    AbstractConnectionAwareAck ao = (AbstractConnectionAwareAck) get(ACKNOWLEDGABLE_OBJECT_KEY);
    set(ACKNOWLEDGABLE_OBJECT_KEY, null);

    if (ao != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Found acknowledgable object <" + ao + ">, executing");
      }

      ao.acknowledgeSchedulerEntry(xo);
    }
  }


  public final ODSConnection getAckConnection() {
    AbstractConnectionAwareAck ao = (AbstractConnectionAwareAck) get(ACKNOWLEDGABLE_OBJECT_KEY);

    if (ao != null) {
      return ao.getConnection();
    } else {
      return null;
    }
  }
  
  public final AbstractConnectionAwareAck getAcknowledgableObject() {
    return (AbstractConnectionAwareAck) get(ACKNOWLEDGABLE_OBJECT_KEY);
  }

  /**
   * Übergabe der Connection, mit der das Backup durchgeführt werden soll
   * @param con
   */
  public void setAckConnection(ODSConnection con) {
    AbstractConnectionAwareAck ao = (AbstractConnectionAwareAck) get(ACKNOWLEDGABLE_OBJECT_KEY);
    if (ao != null) {
      ao.setConnection(con);
    }
  }

  public AbstractConnectionAwareAck useDefaultAcknowledge() {
    AbstractConnectionAwareAck ack = new AbstractConnectionAwareAck(null) {
      private static final long serialVersionUID = 1L;

      public void acknowledgeSchedulerEntry(XynaOrderServerExtension xose)
                      throws XPRC_OrderEntryCouldNotBeAcknowledgedException, XNWH_RetryTransactionException {
        try {
          backup(xose);
          xose.setHasBeenBackuppedInScheduler(true);
        } catch (XNWH_RetryTransactionException rte) {
          logger.warn("Failed to create backup for XynaOrder <" + xose.getId() + ">", rte);
          throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(rte);
          //FIXME hier nur rte weiterwerfen? In com.gip.xyna.xprc.xpce.AbstractBackupAck ebenfalls anpassen!
        } catch (PersistenceLayerException ple) {
          logger.warn("Failed to create backup for XynaOrder <" + xose.getId() + ">", ple);
          throw new XPRC_OrderEntryCouldNotBeAcknowledgedException(ple);
        }
      }
      
      private void backup(final XynaOrderServerExtension xose) throws PersistenceLayerException {
        WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {
          public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
            XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
                .backup(xose, BackupCause.ACKNOWLEDGED, con);
          }
        };

        WarehouseRetryExecutor.buildCriticalExecutor().
          connection(getConnection()).
          storables(backupStorables()).
          execute(wre);
      }
      
    };
    
   set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY, ack);
   return ack;
  }


  public XynaOrderServerExtension getXynaOrder() {
    return xo;
  }
  
}
