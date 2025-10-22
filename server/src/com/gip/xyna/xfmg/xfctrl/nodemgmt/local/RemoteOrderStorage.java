/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.local;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteData;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.remotecall.RemoteOrderExecutionInterface.TransactionMode;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.DynamicRuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.OrderExecutionResponse;
import com.gip.xyna.xmcp.RemoteCallXynaOrderCreationParameter;
import com.gip.xyna.xmcp.SuccesfullOrderExecutionResponse;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_OrderCouldNotBeStartedException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;

public class RemoteOrderStorage extends ObjectWithRemovalSupport {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(RemoteOrderStorage.class);
  
  private interface RemoteDataCreation {

    public RemoteData createRemoteData();

  }

  private static final RemoteOrderResponseListener NO_RL = new RemoteOrderResponseListener(null, -1);
  private final String identifier;
  private final OrderArchive orderArchive;
  private final XynaProcessCtrlExecution execution;
  private final ConcurrentMap<Long,RemoteOrderResponseListener> startedOrders;
  private final LinkedBlockingQueue<RemoteDataCreation> remoteData; 
  private final Set<String> changedApplications;
  private final AtomicLong lastInteraction = new AtomicLong(System.currentTimeMillis());
  private final Set<String>  changeNotificationRequests;
  private final AtomicBoolean applicationDataIsQueued = new AtomicBoolean(false);

  public RemoteOrderStorage(String identifier) {
    this.identifier = identifier;
    this.startedOrders = new ConcurrentHashMap<Long,RemoteOrderResponseListener>();
    this.remoteData = new LinkedBlockingQueue<RemoteDataCreation>();
    
    this.orderArchive = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
    this.execution = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution();
    changedApplications = new HashSet<String>();
    changeNotificationRequests = new HashSet<String>();
    try {
      loadStoredResponses();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not initialize Remote Order Storage for Remote Factory " + identifier, e);
    }
  }


  public OrderExecutionResponse createOrderInternal(RemoteCallXynaOrderCreationParameter creationParameter, TransactionMode mode)
      throws XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException,
      PersistenceLayerException, XPRC_OrderCouldNotBeStartedException {
    lastInteraction.set(System.currentTimeMillis());
    // createOrder
    RuntimeContext rc = creationParameter.getDestinationKey().getRuntimeContext();
    if (rc instanceof DynamicRuntimeContext) {
      String appName = rc.getName();
      rc = ((DynamicRuntimeContext) rc).evaluate();
      if (rc == null) {
        if (logger.isTraceEnabled()) {
          logger.trace("Order could not be started because of missing application " + appName);
        }
        synchronized (changedApplications) {
          changeNotificationRequests.add(appName);
        }
        throw new XPRC_OrderCouldNotBeStartedException(appName,
                                                       new RuntimeException("Dynamic RuntimeContext could not be evaluated successfully."));
      }
      creationParameter.getDestinationKey().setRuntimeContext(rc);
    }
    if (rc instanceof Application) {
      try {
        RevisionOrderControl.checkRmiClosed(rc.getName(), ((Application) rc).getVersionName());
      } catch (RuntimeException e) {
        if (logger.isTraceEnabled()) {
          logger.trace("Order could not be started because of stopped application " + rc.getName() + "/"
              + ((Application) rc).getVersionName());
        }
        synchronized (changedApplications) {
          changeNotificationRequests.add(rc.getName());
        }
        throw new XPRC_OrderCouldNotBeStartedException(rc.getName(), e);
      }
    }
    creationParameter.convertInputPayload();

    XynaOrderServerExtension xose = new XynaOrderServerExtension(creationParameter);
    RemoteOrderResponseListener rorl = new RemoteOrderResponseListener(this, xose.getId());
    
    xose.setResponseListener(rorl);
    
    //TODO mode nicht implementiert

    // backup, can we backup in this state?
    orderArchive.backup(xose, BackupCause.ACKNOWLEDGED); //TODO normales Ack 
    
    addStartedOrder(rorl);
    
    OrderContextServerExtension context = new OrderContextServerExtension(xose);
    context.set(OrderContextServerExtension.CREATION_ROLE_KEY, creationParameter.getTransientCreationRole());
    execution.startOrder(xose, rorl, context);
    
    //TODO evtl. kommt SuccesfullOrderExecutionResponse nicht auf Aufruferseite an. In diesem 
    //Fall könnte der Auftrag gecancelt werden
    //Two-Phase-Commit?
    return new SuccesfullOrderExecutionResponse(xose.getId());
  }
  

  public List<RemoteData> awaitData(long timeoutMillis) {
    if (logger.isTraceEnabled()) {
      logger.trace("Awaiting remote data for " + timeoutMillis + "ms for " + identifier + ". queuesize=" + remoteData.size());
    }
    lastInteraction.set(System.currentTimeMillis());
    RemoteDataCreation first = null;
    try {
      first = remoteData.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      //dann eben nicht mehr warten
    }
    if( first == null ) {
      return Collections.emptyList();
    }
    
    List<RemoteData> oers = new ArrayList<RemoteData>();
    addIfNull(oers, first.createRemoteData());
    
    RemoteDataCreation next = null;
    while( (next = remoteData.poll() ) != null ) {
      addIfNull(oers, next.createRemoteData());
    }
    //TODO evtl geht Verbindung nach den "next.finishOrder()" kaputt: Hier sind Aufträge dann als abgeholt markiert,
    //Ergebnisse gelangen aber nicht zum Aufrufer
    //Two-Phase-Commit?
    return oers;
  }
  
  private void addIfNull(List<RemoteData> oers, RemoteData createRemoteData) {
    if (createRemoteData != null) {
      oers.add(createRemoteData);
    }
  }


  public int getRunningCount() {
    return startedOrders.size();
  }

  public void finished(final Long orderId) {
    //Umtragen: Auftrag ist nicht mehr nur gestartet, sondern nun zur Abholung bereit
    remoteData.add( new RemoteDataCreation() {
      
      public RemoteData createRemoteData() {
        RemoteOrderResponseListener removed = startedOrders.remove(orderId);
        OrderExecutionResponse resp = removed.finishOrder();
        return new RemoteData.RemoteDataOrderResponse(resp);
      }
    });
  }

  public String getIdentifier() {
    return identifier;
  }

  public void addStartedOrder(RemoteOrderResponseListener rorl) {
    startedOrders.put(rorl.getOrderId(), rorl);
  }

  /**
   * Rückgabe der übergebenen orderIds, die hier unbekannt sind 
   * @param orderIds
   * @return
   */
  public List<Long> checkRunningOrders(List<Long> orderIds) {
    HashSet<Long> all = new HashSet<Long>(orderIds);
    all.removeAll( startedOrders.keySet() );
    if( all.isEmpty() ) {
      return Collections.emptyList();
    }
    return new ArrayList<Long>(all);
  }
 

  /**
   * Eine Application wurde gestartet .
   */
  public void applicationStarted(String applicationName) {
    if (System.currentTimeMillis() - lastInteraction.get() > 1000 * 60 * 10) {
      //aufrufer hat entweder keine verbindung, dann versucht er eh nochmal für alle applications nen start
      //oder es interessiert ihn gar nicht mehr, was hier für applicationchanges gesammelt sind
      synchronized (changedApplications) {
        changeNotificationRequests.clear();
        changedApplications.clear();
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Cleared application notification information of " + identifier);
      }
      return;
    }
    boolean notifyNecessary = false;
    synchronized (changedApplications) {
      if (changeNotificationRequests.remove(applicationName)) {
        changedApplications.add(applicationName);
        notifyNecessary = true;
        //nun ist sichergestellt, dass ein remotedata für diese application an den client verschickt wird
      }
    }
    if (notifyNecessary) {
      if (logger.isDebugEnabled()) {
        logger.debug("Notification of appstart " + applicationName + " to " + identifier + " necessary.");
      }
      //ein remotedata objekt in der queue genügt
      if (applicationDataIsQueued.compareAndSet(false, true)) {
        remoteData.add(new RemoteDataCreation() {

          public RemoteData createRemoteData() {
            applicationDataIsQueued.set(false);
            synchronized (changedApplications) {
              if (logger.isDebugEnabled()) {
                logger.debug("Sending list of started applications to " + identifier + " (" + changedApplications + ")");
              }
              RemoteData rd = new RemoteData.RemoteDataApplicationChangeNotification(new HashSet<String>(changedApplications));
              changedApplications.clear();
              return rd;
            }
          }

        });
        logger.debug("Msg was added to queue.");
      }
    }
  }


  @Override
  protected boolean shouldBeDeleted() {
    return startedOrders.size() == 0 && System.currentTimeMillis() - lastInteraction.get() > 1000 * 60 * 30;
  }


  public Object resolveRemoteOrderResponseListener(RemoteOrderResponseListener remoteOrderResponseListener) {
    return startedOrders.putIfAbsent(remoteOrderResponseListener.getOrderId(), remoteOrderResponseListener);
  }
  
  private PreparedQuery<Long> loadIds;
  

  public void loadStoredResponses() throws PersistenceLayerException {
    //lade responses aus db und stelle sie zur verfügung, so dass sie abgeholt werden können
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
    try {
      if (loadIds == null) {
        loadIds = con.prepareQuery(new Query<Long>("select " + StoredResponse.COL_ID + " from " + StoredResponse.TABLE_NAME + " where "
            + StoredResponse.COL_FACTORYID + " = ?", new ResultSetReader<Long>() {

              @Override
              public Long read(ResultSet rs) throws SQLException {
                return rs.getLong(StoredResponse.COL_ID);
              }

            }, StoredResponse.TABLE_NAME));
      }
      List<Long> l = con.query(loadIds, new Parameter(identifier), -1);
      for (final Long id : l) {
        //TODO performance: für jede gespeicherte antwort wird im createRemoteData ein db-zugriff durchgeführt. 
        //     stattdessen alle aufträge aufeinmal in memory laden.
        //     
        remoteData.add(new RemoteDataCreation() {

          @Override
          public RemoteData createRemoteData() {
            StoredResponse resp = loadAndDeleteStoredResponse(id);
            if (resp == null) {
              return null;
            }
            String xml = resp.getResponse();
            if (xml == null) {
              return null;
            }
            OrderExecutionResponse oer = resp.deserializeResponse();
            return new RemoteData.RemoteDataOrderResponse(oer);
          }


        });
        startedOrders.put(id, NO_RL);
      }
    } finally {
      con.closeConnection();
    }
  }


  private StoredResponse loadAndDeleteStoredResponse(Long orderId) {
    try {
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
      try {
        StoredResponse resp = new StoredResponse(orderId, identifier, null);
        try {
          con.queryOneRow(resp);
          con.deleteOneRow(resp);
          con.commit();
          return resp;
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          return null;
        }
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Could not delete stored remote call response for order " + orderId, e);
      return null;
    }
  }


  //response muss später abgeholt werden
  public void storeResponse(Long orderId, OrderExecutionResponse response, ODSConnection historyCon) throws PersistenceLayerException {
    String xml = StoredResponse.serializeResponse(response);
    StoredResponse resp = new StoredResponse(orderId, identifier, xml);
    boolean conWasNull = false;
    if (historyCon == null) {
      historyCon = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
      conWasNull = true;
    }
    try {
      if (historyCon.persistObject(resp)) {
        //dann soll es gelöscht werden, s.u.
        historyCon.deleteOneRow(resp);
      }
      if (conWasNull) {
        historyCon.commit();
      }
    } finally {
      if (conWasNull) {
        historyCon.closeConnection();
      }
    }
  }


  //response wurde abgeholt
  //achtung, es kann sein, dass das aufgerufen wird, bevor das store committed wurde.
  public void removeResponse(Long orderId) {
    try {
      ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.HISTORY);
      try {
        StoredResponse resp = new StoredResponse(orderId, identifier, null);
        try {
          con.queryOneRow(resp);
          con.deleteOneRow(resp);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          //delete vor store -> dann insert probieren, damit das store erkennt, dass das delete durchzuführen ist.
          if (con.persistObject(resp)) {
            //nun ist das persist doch bereits durchgeführt worden
            con.deleteOneRow(resp);
          }
        }
        con.commit();
      } finally {
        con.closeConnection();
      }
    } catch (PersistenceLayerException e) {
      logger.warn("Could not delete stored remote call response for order " + orderId, e);
    }
  }

}
