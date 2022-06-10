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

package com.gip.xyna.xprc.xprcods.abandonedorders.rules;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationEntry;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetails;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrderDetectionRule;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement.ResolveForAbandonedOrderNotSupported;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean;



public class OrderbackupWillNotResume extends AbandonedOrderDetectionRule<AbandonedOrderDetails> {

  private static PreparedQuery<OrderInstanceBackup> readOrderbackupsWithRootOrderId;


  public OrderbackupWillNotResume() throws PersistenceLayerException {
    super(false);

    if (readOrderbackupsWithRootOrderId == null) {
      ODSConnection con = ODSImpl.getInstance().openConnection();
      try {
        readOrderbackupsWithRootOrderId =
            con.prepareQuery(new Query<OrderInstanceBackup>("select * from " + OrderInstanceBackup.TABLE_NAME
                + " where " + OrderInstanceBackup.COL_ROOT_ID + "=? and " + OrderInstanceBackup.COL_BINDING + "=?",
                                                            new OrderInstanceBackup().getReader()), true);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Failed to prepare query. ", e);
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.warn("Failed to close connection.", e);
        }
      }
    }
  }


  @Override
  public List<AbandonedOrderDetails> detect(int maxrows) throws PersistenceLayerException {
    //          orderbackup-Eintrag existiert, Auftrag existiert nicht im Scheduler und nicht in Planning/Execution/Cleanup,
    //          es gibt kein potentiell resumendes Element (MI-Eintrag, cron mit gleicher rootOrderID, etc)
    //          oder keine suspensionentries
    //          ==> z.B. mit select id, rootId from orderbackup alle relevanten IDs ermitteln und dann die inmemory-Checks
    //              mit Hilfe von listschedulerinfo(verbose) und
    //              XynaFactory.getInstance().getProcessing().getWorkflowEngine().getPlanningProcessor().getOrdersOfRunningProcesses();
    //              etc. durchführen
    //              Wunsch: XynaFactory.getInstance().getProcessing().getWorkflowEngine().getSpecialPurposeHelper().containsResumerForOrder(con, id)

    int targetBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);

    List<AbandonedOrderDetails> result = new ArrayList<AbandonedOrderDetails>();

    int foundCount = 0;

    Set<Long> ordersPlanning =
        getIdsOfXynaOrderServerExtensions(XynaFactory.getInstance().getProcessing().getWorkflowEngine()
            .getPlanningProcessor().getOrdersOfRunningProcesses());
    Set<Long> ordersExecution =
        getIdsOfXynaOrderServerExtensions(XynaFactory.getInstance().getProcessing().getWorkflowEngine()
            .getExecutionProcessor().getOrdersOfRunningProcesses());
    Set<Long> ordersCleanup =
        getIdsOfXynaOrderServerExtensions(XynaFactory.getInstance().getProcessing().getWorkflowEngine()
            .getCleanupProcessor().getOrdersOfRunningProcesses());


    SchedulerInformationBean schedulerInformation =
        XynaFactory.getInstance().getXynaMultiChannelPortalPortal()
            .listSchedulerInformation(SchedulerInformationBean.Mode.Consistent);


    // Aus Performancegründen die Liste hier halten und nicht im SpecialPurposeHelper (evt. TODO)
    Set<Long> synchronizationEntries =
        getIdsOfSynchronizationEntries(((XynaFractalWorkflowEngine) XynaFactory.getInstance().getProcessing()
            .getWorkflowEngine()).getSynchronizationManagement().listCurrentSynchronizationEntries());

    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {

      PreparedQuery<OrderInstanceBackup> query =
          con.prepareQuery(new Query<OrderInstanceBackup>("select " + OrderInstanceBackup.COL_ID + ","
              + OrderInstanceBackup.COL_ROOT_ID + " from " + OrderInstanceBackup.TABLE_NAME + " where "
              + OrderInstanceBackup.COL_BINDING + "=?", new ResultSetReader<OrderInstanceBackup>() {

            public OrderInstanceBackup read(ResultSet rs) throws SQLException {
              OrderInstanceBackup oib = new OrderInstanceBackup(rs.getLong(OrderInstance.COL_ID), -1);
              oib.setRootId(rs.getLong(OrderInstance.COL_ROOT_ID));
              return oib;
            }
          }));

      List<OrderInstanceBackup> orders = con.query(query, new Parameter(targetBinding), Integer.MAX_VALUE);

      for (OrderInstanceBackup oib : orders) {
        if (checkOrder(con, oib.getId(), oib.getRootId(), ordersPlanning, ordersExecution, ordersCleanup,
                       synchronizationEntries, schedulerInformation)) {
          continue;
        }

        result.add(new AbandonedOrderDetails(oib.getId(), oib.getRootId()));
        foundCount++;
        if (foundCount > maxrows) {
          break;
        }
      }
    } finally {
      con.closeConnection();
    }

    return result;

  }


  private boolean checkOrder(ODSConnection con, Long orderId, Long rootOrderId, Set<Long> ordersPlanning,
                             Set<Long> ordersExecution, Set<Long> ordersCleanup, Set<Long> synchronizationEntries,
                             SchedulerInformationBean schedulerInformation) throws PersistenceLayerException {

    if (ordersPlanning.contains(orderId) || ordersExecution.contains(orderId) || ordersCleanup.contains(orderId)) {
      return true;
    }
    if (contains(schedulerInformation.getOrdersInScheduler(), orderId)) {
      return true;
    }

    // gibt es evt. einen resume-Eintrag?
    if (XynaFactory.getInstance().getProcessing().getWorkflowEngine().getSpecialPurposeHelper()
        .containsResumerForOrder(orderId, rootOrderId, con)) {
      return true;
    }

    // und ein SynchronzationEntry?
    if (synchronizationEntries.contains(orderId)) {
      return true;
    }

    return false;
  }


  private boolean contains(List<XynaOrderInfo> ordersInScheduler, Long orderId) {
    for (XynaOrderInfo xoi : ordersInScheduler) {
      if (xoi.getOrderId().equals(orderId)) {
        return true;
      }
    }
    return false;
  }


  private Set<Long> getIdsOfXynaOrderServerExtensions(Collection<XynaOrderServerExtension> orders) {
    Set<Long> result = new HashSet<Long>(orders.size());
    for (XynaOrderServerExtension order : orders) {
      result.add(order.getId());
    }
    return result;
  }


  private Set<Long> getIdsOfSynchronizationEntries(Collection<SynchronizationEntry> entries) {
    Set<Long> result = new HashSet<Long>(entries.size());
    for (SynchronizationEntry entry : entries) {
      result.add(entry.getOrderId());
    }
    return result;
  }


  @Override
  public String describeProblem(AbandonedOrderDetails information) {
    return "The orderbackup entry with id <" + information.getOrderID() + "> will possibly not be resumed.";
  }


  @Override
  public String getShortName() {
    return "Orderbackup entry will possibly not be resumed.";
  }


  @Override
  public String describeSolution() {
    return "Auto resolving not supported.";
  }


  @Override
  public void forceClean(AbandonedOrderDetails information) {
    int ownBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);
    
    Set<Long> ordersPlanning =
        getIdsOfXynaOrderServerExtensions(XynaFactory.getInstance().getProcessing().getWorkflowEngine()
            .getPlanningProcessor().getOrdersOfRunningProcesses());
    Set<Long> ordersExecution =
        getIdsOfXynaOrderServerExtensions(XynaFactory.getInstance().getProcessing().getWorkflowEngine()
            .getExecutionProcessor().getOrdersOfRunningProcesses());
    Set<Long> ordersCleanup =
        getIdsOfXynaOrderServerExtensions(XynaFactory.getInstance().getProcessing().getWorkflowEngine()
            .getCleanupProcessor().getOrdersOfRunningProcesses());


    SchedulerInformationBean schedulerInformation =
        XynaFactory.getInstance().getXynaMultiChannelPortalPortal()
            .listSchedulerInformation(SchedulerInformationBean.Mode.Consistent);

    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {

      // Aus Performancegründen die Liste hier halten und nicht im SpecialPurposeHelper (evt. TODO)
      Set<Long> synchronizationEntries =
          getIdsOfSynchronizationEntries(((XynaFractalWorkflowEngine) XynaFactory.getInstance().getProcessing()
              .getWorkflowEngine()).getSynchronizationManagement().listCurrentSynchronizationEntries());

      // check again
      if (checkOrder(con, information.getOrderID(), information.getRootOrderID(), ordersPlanning, ordersExecution,
                     ordersCleanup, synchronizationEntries, schedulerInformation)) {
        return;
      }

      // TODO : orderseries benachrichtigen
      con.deleteOneRow(new OrderInstanceBackup(information.getOrderID(), ownBinding));
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.error("Error while force cleaning abandoned order backup entry with order id <" + information.getOrderID() + ">", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }


  @Override
  public void forceCleanFamily(AbandonedOrderDetails information) {
    int ownBinding = new OrderInstanceBackup().getLocalBinding(ODSConnectionType.DEFAULT);
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);

    try {
      List<OrderInstanceBackup> oibs =
          con.query(readOrderbackupsWithRootOrderId, new Parameter(information.getRootOrderID(), ownBinding), Integer.MAX_VALUE);
      con.delete(oibs);
      con.commit();
    } catch (PersistenceLayerException e) {
      logger.error("Error while force cleaning abandoned order backup entries with root order id <" + information.getRootOrderID()
          + ">", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
    }
  }


  @Override
  public void resolve(AbandonedOrderDetails information) throws ResolveForAbandonedOrderNotSupported {
    throw new ResolveForAbandonedOrderNotSupported();
  }

}
