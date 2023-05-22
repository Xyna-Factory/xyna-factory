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
package com.gip.xyna.xprc.xprcods.orderarchive;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.SearchMode;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;



public class LocalRemote implements RemoteInterface {

  private static final XynaPropertyBoolean propertyUseAdditionalLastUpdateQuery = new XynaPropertyBoolean("xprc.xprcods.orderarchive.count.uselastupdatecondition", true);
  static {
    propertyUseAdditionalLastUpdateQuery.setDefaultDocumentation(DocumentationLanguage.EN, "If a count query to orderarchive has no filter, an additional filter on lastudpate column will be used.");
    propertyUseAdditionalLastUpdateQuery.registerDependency(OrderArchive.DEFAULT_NAME);
  }
  
  private ODS ods;
  private static final Logger logger = CentralFactoryLogging.getLogger(OrderArchive.class);
  private OrderArchive orderarchive;

  LocalRemote(ODS ods) {
    this.ods = ods;
    orderarchive = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive();
  }
  
  

  public Pair<SortedMap<OrderInstance, Collection<OrderInstance>>, List<OrderInstance>> searchConnectionType(OrderInstanceSelect select,
                                                                                                             int maxRows,
                                                                                                             long startTime,
                                                                                                             SearchMode searchMode,
                                                                                                             ODSConnectionType connectionType,
                                                                                                             List<OrderInstance> selectedPreCommittedOrdersFromDEFAULT)
      throws PersistenceLayerException {

    List<OrderInstance> affectedByPreCommit = new ArrayList<OrderInstance>();
    Parameter paras;
    try {
      select.selectRootId();
      select.selectLastUpdate(); // for sorting
      if (connectionType == ODSConnectionType.DEFAULT) { //�ndert das select und braucht nur einmal ge�ndert zu werden
        if (propertyUseAdditionalLastUpdateQuery.get()) {
          boolean foundLastUpdateCondition = false;
          // this is a hint for oracle/mysql to use an index to avoid a full table scan
          for (WhereClausesConnection<?> o : select.getWhereClauses()) {
            // TODO this can be optimized: this wont find lastUpdate conditions within braces
            if (o.getConnectedObject().getColumn() != null && o.getConnectedObject().getColumn().equals(OrderInstance.COL_LAST_UPDATE)) {
              foundLastUpdateCondition = true;
            }
          }
          if (!foundLastUpdateCondition) {
            if (select.getWhereClauses().size() > 0) {
              select =
                  select.getWhereClauses().get(select.getWhereClauses().size() - 1).and().whereLastUpdate().isBiggerThan(0)
                      .finalizeSelect(OrderInstanceSelect.class);
            } else {
              select.whereLastUpdate().isBiggerThan(0);
            }
          }
        }

        //daf�r sorgen, dass man nicht in HISTORY ganz neue auftr�ge findet (wenn die dann auch noch partiell committed sind, macht das probleme)
        if (select.getWhereClauses().size() > 0) {
          select =
              select.getWhereClauses().get(select.getWhereClauses().size() - 1).and().whereStartTime().isSmallerThan(startTime)
                  .finalizeSelect(OrderInstanceSelect.class);
        } else {
          select.whereStartTime().isSmallerThan(startTime);
        }
        
        select.addOrderBy(OrderInstanceColumn.C_LAST_UPDATE, OrderInstanceSelect.OrderByDesignators.DESC);
      }
      
      paras = select.getParameter();
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException("problem with select statement", e);
    }

    List<OrderInstance> orders = new ArrayList<OrderInstance>();
    ODSConnection con = ods.openConnection(connectionType);
    try {
      PreparedQuery<OrderInstance> query = orderarchive.auditAccess.prepareQuery(con, select);
      orders.addAll(con.query(query, paras, maxRows));
    } finally {
      con.closeConnection();
    }

    if (connectionType == ODSConnectionType.HISTORY && searchMode != SearchMode.FLAT) {
      orders.addAll(selectedPreCommittedOrdersFromDEFAULT); //achtung, hier sind nun evtl orderinstances doppelt drin
    }

    //nach rootids sortieren
    Map<Long, Map<Long, OrderInstance>> families = sortByRootId(orders);

    SortedMap<OrderInstance, Collection<OrderInstance>> resultMap =
        new TreeMap<OrderInstance, Collection<OrderInstance>>(lastUpdateComperator);
    //vollst�ndige familien nachselektieren, falls notwendig
    con = ods.openConnection(connectionType);
    try {
      for (Map<Long, OrderInstance> family : families.values()) {
        long rootId = family.values().iterator().next().getRootId();
        final boolean checkForPreCommits = connectionType == ODSConnectionType.DEFAULT && searchMode != SearchMode.FLAT;
        AtomicBoolean preCommitted;
        if (checkForPreCommits) {
          //zu einem beliebigen zeitpunkt beim sammeln der familienmitglieder kann die teilweise archivierung stattfinden
          preCommitted = new AtomicBoolean(false);
          orderarchive.registerPreCommitNotification(preCommitted, rootId);
        } else {
          preCommitted = null;
        }
        try {
          List<OrderInstance> wholeFamily;
          if (searchMode != SearchMode.FLAT) {
            //performance: hier sucht man bei searchMode=CHILDREN zuviel. unklar, ob es besser ist, wenn man da erst die ids vorselektiert
            wholeFamily = getOrderInstancesByRootId(rootId, con);
          } else {
            wholeFamily = null;
          }
          for (OrderInstance o : family.values()) {
            //jedes mal die gleiche family, weil gleicher root - spart speicher und wird sp�ter eh geflattet.
            
            resultMap.put(o, gatherFamilyForOrder(rootId, o, wholeFamily, searchMode));
          }
        } finally {
          if (checkForPreCommits) {
            if (preCommitted.get()) {
              //es wurde zwischenzeitlich ein teil der zugeh�rigen auftragsfamilie archiviert -> nochmal in HISTORY suchen
              if (logger.isDebugEnabled()) {
                logger.debug("found partial committed order family (root=" + rootId + ").");
              }
              affectedByPreCommit.addAll(family.values());
            }
            orderarchive.removePreCommitNotification(preCommitted, rootId);
          }
        }
      }
    } finally {
      con.closeConnection();
    }

    return Pair.of(resultMap, affectedByPreCommit);
  }


  private Map<Long, Map<Long, OrderInstance>> sortByRootId(List<OrderInstance> orders) {
    Map<Long, Map<Long, OrderInstance>> families = new HashMap<Long, Map<Long, OrderInstance>>();
    for (OrderInstance o : orders) {
      Map<Long, OrderInstance> family = families.get(o.getRootId());
      if (family == null) {
        family = new HashMap<Long, OrderInstance>();
        families.put(o.getRootId(), family);
      }
      OrderInstance old = family.get(o.getId());
      if (old == null) {
        family.put(o.getId(), o);
      } //else alte orderinstance belassen, das ist dann die gerade selektierte
    }
    return families;
  }


  //FIXME gleiche selektion von spalten verwenden, wie beim �bergebenen select an "searchConnectionType"
  private Collection<OrderInstance> gatherFamilyForOrder(long rootId, OrderInstance parent, List<OrderInstance> wholeFamily,
                                                         SearchMode searchMode) throws PersistenceLayerException {
    switch (searchMode) {
      case FLAT :
        //do nothing
        return Collections.emptyList();
      case HIERARCHY :
        return wholeFamily;
      case CHILDREN :
        // nun alle Auftr�ge enternen, die nicht Kinder vom gesuchten Auftrag sind
        return orderarchive.findChildOrders(parent, wholeFamily);
      default :
        throw new RuntimeException("Unsupported searchMode: " + searchMode);
    }
  }


  
  
  private List<OrderInstance> getOrderInstancesByRootId(long rootId, ODSConnection con) throws PersistenceLayerException {
    switch (con.getConnectionType()) {
      case DEFAULT :
        return con.query(OrderArchive.getOrderInstancesByRootIdFromDefault, new Parameter(rootId), -1);
      case HISTORY :
        return con.query(OrderArchive.getOrderInstancesByRootIdFromHistory, new Parameter(rootId), -1);
      default :
        return new ArrayList<OrderInstance>();
    }

  }


  public int sendCountQueryForConnectionType(OrderInstanceSelect select, ODSConnectionType connectionType)
      throws PersistenceLayerException {
    String selectCountString;
    Parameter paras;
    try {
      selectCountString = select.getSelectCountString();
      paras = select.getParameter();
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException("problem with select statement", e);
    }

    ODSConnection con = ods.openConnection(connectionType);
    try {
      PreparedQuery<? extends OrderCount> queryCount =
          OrderArchive.cache.getQueryFromCache(selectCountString, con, OrderCount.getCountReader());
      OrderCount count = con.queryOneRow(queryCount, paras);
      return count.getCount();
    } finally {
      con.closeConnection();
    }
  }


  /**
   * gibt auftrag mit auditdetails in xml form zur�ck. falls auftrag noch laufend ist, werden auditdetails in xml form
   * erst erzeugt (aber nicht gespeichert)
   */
  public OrderInstanceDetails getCompleteOrder(long id) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

    //erst in memory nach auftrag suchen
    //dann in archiv
    ODSConnection conDefault = ods.openConnection(ODSConnectionType.DEFAULT);
    try {
      OrderInstanceDetails oid = new OrderInstanceDetails(id);
      try {
        conDefault.queryOneRow(oid);
        oid = oid.clone();
        Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
        try {
          revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
                          .getRevision(oid.getApplicationName(), oid.getVersionName(), oid.getWorkspaceName());
        } catch(XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          if (oid.getApplicationName() != null) {
            logger.warn("Can't get revision for application " + oid.getApplicationName(), e);
          } else {
            logger.warn("Can't get revision for workspace " + oid.getWorkspaceName(), e);
          }
        }
        
        try {
          oid.convertAuditDataToXML(revision, false);
        } catch (XPRC_CREATE_MONITOR_STEP_XML_ERROR e) {
          logger.warn("could not create xml auditdata", e);
        }
        return oid;
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        ODSConnection conHistory = ods.openConnection(ODSConnectionType.HISTORY);
        try {
          oid = orderarchive.auditAccess.restore(conHistory, id, false);
          return oid.clone();
        } finally {
          conHistory.closeConnection();
        }
      }
    } finally {
      conDefault.closeConnection();
    }
  }


  public static Comparator<OrderInstance> lastUpdateComperator = new LastUpdateComparator();


  private static class LastUpdateComparator implements Comparator<OrderInstance>, Serializable {

    private static final long serialVersionUID = -3237897263112860618L;


    public int compare(OrderInstance o1, OrderInstance o2) {
      if (o1.getLastUpdate() == o2.getLastUpdate()) {
        // two orders can always be sorted according to their id which always has to be unique
        if (o1.getId() == o2.getId()) {
          return 0;
        } else if (o1.getId() > o2.getId()) {
          return -1;
        } else {
          return 1;
        }
      } else if (o1.getLastUpdate() > o2.getLastUpdate()) {
        return -1;
      } else {
        return 1;
      }
    }

  }

}
