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
package xfmg.xfmon.protocolmsg.impl;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.update.Updater;
import com.gip.xyna.update.Version;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xpce.OrderContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import xfmg.xfmon.protocolmsg.HistorizationParameter;
import xfmg.xfmon.protocolmsg.ProtocolMessage;
import xfmg.xfmon.protocolmsg.ProtocolMessageStore;
import xfmg.xfmon.protocolmsg.ProtocolPayload;
import xfmg.xfmon.protocolmsg.RemoveParameter;
import xfmg.xfmon.protocolmsg.RetrieveParameter;
import xfmg.xfmon.protocolmsg.StoreParameter;
import xfmg.xfmon.protocolmsg.ProtocolMessageStoreServiceOperation;
import xfmg.xfmon.protocolmsg.data.ProtocolMessageStorable;


public class ProtocolMessageStoreServiceOperationImpl implements ExtendedDeploymentTask, ProtocolMessageStoreServiceOperation {

  private static final String TIME_DELETION_CURSOR_QUERY = "SELECT * FROM " + ProtocolMessageStorable.TABLENAME + " WHERE " + ProtocolMessageStorable.COL_TIME + " < ?";
  private static final long DELETION_CHECK_INTERVAL = 30000;
  static final int CURSOR_CACHE_SIZE = 100;
  static final Logger logger = CentralFactoryLogging.getLogger(ProtocolMessageStore.class);
  
  private static ODS ods;
  static PreparedQueryCache regularQueryCache;
  static PreparedQueryCache cursorQueryCache;
  private static XynaPropertyBuilds<XynaPropertyBuildMessageFilter> storeProtocolMessages;
  private static XynaPropertyDuration messageTimeout;
  private static long revision;
  private static AtomicLong lastDeletionCheck;
  private static IDGenerator idGenerator;

  public void onDeployment() throws XynaException {
    checkFactoryVersion();
    revision = ((ClassLoaderBase) ProtocolMessageStoreServiceOperationImpl.class.getClassLoader()).getRevision();
    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ProtocolMessageStorable.registerWithOdsIfNecessary(ods);
    regularQueryCache = new PreparedQueryCache();
    cursorQueryCache = new PreparedQueryCache();
    storeProtocolMessages = new XynaPropertyBuilds<XynaPropertyBuildMessageFilter>("xfmg.xfmon.protocolmsg.enablestorage",
                                                                                   new XynaPropertyMessageFilterBuilder(),
                                                                                   new XynaPropertyMessageFilterBuilder.GlobalMessageFilter(false));
    storeProtocolMessages.setDefaultDocumentation(DocumentationLanguage.EN, "Controls storage of messages. Either true or false for all or none respectivly or a comma seperated list of protcol types that should be allowed to be stored.")
                         .setDefaultDocumentation(DocumentationLanguage.DE, "Kontrolliert die Speicherung von Nachrichten. Entweder true oder fals um respektive alle oder keine Nachrichten zu speichern oder eine Komma separierte Liste von Protokollen für welche Nachrichten gespeichert werden dürfen.")
                         .registerDependency(UserType.Service, ProtocolMessageStore.class.getName());
    messageTimeout = new XynaPropertyDuration("xfmg.xfmon.protocolmsg.messagetimeout", Duration.valueOf("180", TimeUnit.SECONDS));
    messageTimeout.setDefaultDocumentation(DocumentationLanguage.EN, "The amount of time messages are kept alive.")
                  .setDefaultDocumentation(DocumentationLanguage.DE, "Die Zeitdauer welche bestimt wie lange Nachrichten aufbewahrt werden.")
                  .registerDependency(UserType.Service, ProtocolMessageStore.class.getName());
    lastDeletionCheck = new AtomicLong(System.currentTimeMillis());
    idGenerator = IDGenerator.getInstance();
    HistoryAdjusmentHandler.register();
  }

  
  private void checkFactoryVersion() throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException {
    Version neededVersion = new Version(7, 0, 5, 11);
    Version currentVersion = Updater.getInstance().getFactoryVersion();
    if (!currentVersion.isEqualOrGreaterThan(neededVersion)) {
      throw new UnsupportedOperationException("ProtocolMessageStore depends on RuntimeContextManagement, first introduced in version " + neededVersion.getString());
    }
  }

  public void onUndeployment() throws XynaException {
    HistoryAdjusmentHandler.unregister();
    ODSConnection con = openConnection();
    try {
      con.deleteAll(ProtocolMessageStorable.class);
      con.commit();
    } finally {
      con.closeConnection();
    }
    ods.unregisterStorable(ProtocolMessageStorable.class);
  }

  public Long getOnUnDeploymentTimeout() {
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return BehaviorAfterOnUnDeploymentTimeout.IGNORE;
  }


  public List<? extends ProtocolMessage> retrieve(ProtocolMessage filterCriterion, RetrieveParameter retrieveParameter) throws XynaException {
    Pair<String, Parameter> queryParameter = generateSqlSelect(filterCriterion, !retrieveParameter.getFromHistory());
    ODSConnection con = openConnection(retrieveParameter.getFromHistory());
    try {
      PreparedQuery<ProtocolMessageStorable> query = regularQueryCache.getQueryFromCache(queryParameter.getFirst(), con, ProtocolMessageStorable.reader);
      List<? extends ProtocolMessageStorable> result = con.query(query, queryParameter.getSecond(), retrieveParameter.getMaxResults());
      if (!retrieveParameter.getFromHistory()) {
        evaluateDeletionCriteria(con);
      }
      return convertResult(result);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to close connection gracefully", e);
      }
    }
  }
  
  
  private Collection<ProtocolMessageFilter> getFilter() {
    return Collections.<ProtocolMessageFilter>singleton(storeProtocolMessages.get());
  }

  
  private void evaluateDeletionCriteria(ODSConnection con) throws PersistenceLayerException {
    long current = System.currentTimeMillis();
    long previous = lastDeletionCheck.get();
    if (current > previous + DELETION_CHECK_INTERVAL &&
        lastDeletionCheck.compareAndSet(previous, current)) {
      long timeout = current - messageTimeout.getMillis();
      FactoryWarehouseCursor<ProtocolMessageStorable> msgCursor = 
                      con.getCursor(TIME_DELETION_CURSOR_QUERY, new Parameter(timeout), ProtocolMessageStorable.reader, 50, cursorQueryCache);
      
      List<ProtocolMessageStorable> nextCache = msgCursor.getRemainingCacheOrNextIfEmpty();
      while (nextCache != null && nextCache.size() > 0) {
        con.delete(nextCache);
        nextCache = msgCursor.getRemainingCacheOrNextIfEmpty();
      }
      con.commit();  
    }
  }
  

  private Pair<String, Parameter> generateSqlSelect(ProtocolMessage filterCriterion, boolean appendRevisionFilter) {
    StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ");
    sqlBuilder.append(ProtocolMessageStorable.TABLENAME).append(" ");
    Parameter params = new Parameter();
    boolean firstCriterion = true;
    for (String column : ProtocolMessageStorable.FILTERABLE_COLUMNS) {
      Object value = null;
      try {
        value = filterCriterion.get(column);
      } catch (InvalidObjectPathException e) {
        throw new RuntimeException(e);
      }
      if (value != null) {
        if (firstCriterion) {
          sqlBuilder.append("WHERE ");
          firstCriterion = false;
        } else {
          sqlBuilder.append("AND ");
        }
        sqlBuilder.append(column).append(" = ? ");
        params.add(value);
      }
    }
    if (appendRevisionFilter) {
      if (firstCriterion) {
        sqlBuilder.append("WHERE ");
      } else {
        sqlBuilder.append("AND ");
      }
      sqlBuilder.append(ProtocolMessageStorable.COL_REVISION).append(" = ? ");
      params.add(revision);
    }
    sqlBuilder.append("ORDER BY ").append(ProtocolMessageStorable.COL_TIME).append(" ASC"); // TODO ASC or DESC
    return Pair.of(sqlBuilder.toString(), params);
  }


  private List<? extends ProtocolMessage> convertResult(List<? extends ProtocolMessageStorable> result) {
    List<ProtocolMessage> conversion = new ArrayList<ProtocolMessage>();
    for (ProtocolMessageStorable storable : result) {
      conversion.add(convertStorable(storable));
    }
    return conversion;
  }


  private ProtocolMessage convertStorable(ProtocolMessageStorable storable) {
    ProtocolMessage.Builder builder = new ProtocolMessage.Builder(); 
    builder.rootOrderId(storable.getRootOrderId())
           .originId(storable.getOriginId())
           .connectionId(storable.getConnectionId())
           .partnerAddress(storable.getPartnerAddress())
           .localAddress(storable.getLocalAddress())
           .protocolName(storable.getProtocolName())
           .payload((ProtocolPayload) storable.getPayload())
           .time(storable.getTime())
           .communicationDirection(storable.getCommunicationDirection())
           .protocolAdapterName(storable.getProtocolAdapterName())
           .messageType(storable.getMessageType());
    return builder.instance();
  }


  public void store(ProtocolMessage msg, StoreParameter storeParameter) throws XynaException {
    if (acceptMessage(msg)) {
      ODSConnection con = openConnection();
      try {
        evaluateDeletionCriteria(con);
        con.persistObject(new ProtocolMessageStorable(msg, revision));
        con.commit();
      } finally {
        try {
          con.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Failed to close connection gracefully", e);
        }
      }
    }
  }

  private boolean acceptMessage(ProtocolMessage msg) {
    for (ProtocolMessageFilter filter : getFilter()) {
      if (!filter.accept(msg)) {
        return false;
      }
    }
    return true;
  }

  public ProtocolMessage createFilterCriterionOnThisOrderHierarchy() throws XynaException {
    OrderContext ctx = XynaProcessing.getOrderContext();
    ProtocolMessage criterion = new ProtocolMessage();
    criterion.setRootOrderId(ctx.getRootOrderContext().getOrderId());
    return criterion;
  }
  
  private ODSConnection openConnection(boolean forHistory) {
    return forHistory ? openHistorizationConnection() : openConnection();
  }
  
  private ODSConnection openConnection() {
    return ods.openConnection(ODSConnectionType.DEFAULT);
  }
  
  private ODSConnection openHistorizationConnection() {
    return ods.openConnection(ODSConnectionType.HISTORY);
  }

  public void remove(ProtocolMessage filterCriterion, RemoveParameter removeParams) throws XynaException {
    Pair<String, Parameter> queryParameter = generateSqlSelect(filterCriterion, !removeParams.getFromHistory());
    ODSConnection con = openConnection(removeParams.getFromHistory());
    try {
      FactoryWarehouseCursor<ProtocolMessageStorable> cursor = con.getCursor(queryParameter.getFirst(), queryParameter.getSecond(), ProtocolMessageStorable.reader, CURSOR_CACHE_SIZE, cursorQueryCache);
      List<ProtocolMessageStorable> msgs = cursor.getRemainingCacheOrNextIfEmpty();
      while (msgs != null && msgs.size() > 0) {
        con.delete(msgs);
        msgs = cursor.getRemainingCacheOrNextIfEmpty();
      }
      con.commit();
      if (!removeParams.getFromHistory()) {
        evaluateDeletionCriteria(con);
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to close connection gracefully", e);
      }
    }
  }


  public void historize(ProtocolMessage filterCriterion, HistorizationParameter historizationParameter) throws XynaException {
    Pair<String, Parameter> queryParameter = generateSqlSelect(filterCriterion, true);
    ODSConnection con = openConnection();
    try {
      ODSConnection historizationCon = openHistorizationConnection();
      try {
        FactoryWarehouseCursor<ProtocolMessageStorable> cursor = con.getCursor(queryParameter.getFirst(), queryParameter.getSecond(), ProtocolMessageStorable.reader, CURSOR_CACHE_SIZE, cursorQueryCache);
        List<ProtocolMessageStorable> msgs = cursor.getRemainingCacheOrNextIfEmpty();
        while (msgs != null && msgs.size() > 0) {
          List<ProtocolMessageStorable> newMsgs = new ArrayList<ProtocolMessageStorable>(); 
          for (ProtocolMessageStorable msg : msgs) {
            ProtocolMessageStorable newMsg = new ProtocolMessageStorable(msg);
            newMsg.setMessageId(idGenerator.getUniqueId("protocolmsg"));
            newMsgs.add(newMsg);
          }
          historizationCon.persistCollection(newMsgs);
          con.delete(msgs);
          msgs = cursor.getRemainingCacheOrNextIfEmpty();
        }
        historizationCon.commit();
        con.commit();
      } finally {
        try {
          historizationCon.closeConnection();
        } catch (PersistenceLayerException e) {
          logger.debug("Failed to close historizationConnection gracefully", e);
        }
      }
      evaluateDeletionCriteria(con);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.debug("Failed to close connection gracefully", e);
      }
    }
  }

}
