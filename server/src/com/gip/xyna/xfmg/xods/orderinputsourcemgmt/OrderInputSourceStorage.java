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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.selectorderinputsource.OrderInputSourceColumn;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceSpecificStorable;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceTypeStorable;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xpce.execution.XynaExecution;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;


public class OrderInputSourceStorage {

  private static Logger logger = CentralFactoryLogging.getLogger(OrderInputSourceStorage.class);

  private final ODSImpl ods;
  private final PreparedQueryCache queryCache;
  private final OrderInputSourceManagement oism;
  private final XynaExecution xynaExecution;

  private static final String QUERY_GET_ORDER_INPUT_SOURCE_SPECIFIC = "select * from " + OrderInputSourceSpecificStorable.TABLENAME
      + " where " + OrderInputSourceSpecificStorable.COL_SOURCEID + "=?";
  private static final String QUERY_GET_ORDER_INPUT_SOURCE_BY_NAME = "select * from " + OrderInputSourceStorable.TABLENAME + " where "
      + OrderInputSourceStorable.COL_NAME + " = ? and ((" + OrderInputSourceStorable.COL_APPLICATIONNAME + " = ? and "
      + OrderInputSourceStorable.COL_VERSIONNAME + " = ?) or " + OrderInputSourceStorable.COL_WORKSPACENAME + " = ?)";
  private static final String QUERY_COUNT_BY_TYPE = "select count(*) from " + OrderInputSourceStorable.TABLENAME + " where "
      + OrderInputSourceStorable.COL_TYPE + " = ?";

  private static ResultSetReader<Integer> countReader = new ResultSetReader<Integer>() {

    public Integer read(ResultSet rs) throws SQLException {
      int count = rs.getInt(1);
      return count;
    }
  };
  
  public OrderInputSourceStorage() throws PersistenceLayerException {
    ods = ODSImpl.getInstance();
    
    ods.registerStorable(OrderInputSourceTypeStorable.class);
    ods.registerStorable(OrderInputSourceStorable.class);
    ods.registerStorable(OrderInputSourceSpecificStorable.class);
    
    queryCache = new PreparedQueryCache();
    oism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
    xynaExecution = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution();
  }
  
  private static void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }

  
  public void persistOrderInputSourceType(String name, String fqClassName) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      OrderInputSourceTypeStorable oigts = new OrderInputSourceTypeStorable(name,fqClassName);
      con.persistObject(oigts);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }

  public Collection<OrderInputSourceTypeStorable> getAllOrderInputSourceTypes() throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      return con.loadCollection(OrderInputSourceTypeStorable.class);
    } finally {
      finallyClose(con);
    }
  }

  public void deleteOrderInputSourceType(String name) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      OrderInputSourceTypeStorable oigts = new OrderInputSourceTypeStorable(name);
      con.deleteOneRow(oigts);
      con.commit();
    } finally {
      finallyClose(con);
    }
  }


  public static final XynaPropertyBoolean selectRefCountProperty =
      new XynaPropertyBoolean("xfmg.xods.orderinputsource.selection.refcnt", true)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Search requests for order input sources will not include reference count evaluation if this property is set to false.");
  public static final XynaPropertyBoolean selectStateProperty = new XynaPropertyBoolean("xfmg.xods.orderinputsource.selection.state", true)
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Search requests for order input sources will not include state evaluation if this property is set to false.");


  public SearchResult<OrderInputSourceStorable> search(SearchRequestBean searchRequest)
                  throws PersistenceLayerException, XNWH_SelectParserException, XNWH_InvalidSelectStatementException {
    SearchResult<OrderInputSourceStorable> result = new SearchResult<OrderInputSourceStorable>();
    Selection selection = SelectionParser.generateSelectObjectFromSearchRequestBean(searchRequest);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      PreparedQuery<OrderInputSourceStorable> query = queryCache
                      .getQueryFromCache(selection.getSelectString(), con,
                                         selection.getReader(OrderInputSourceStorable.class));
      List<OrderInputSourceStorable> orderInputSources = con.query(query, selection.getParameter(),
                                                                         searchRequest.getMaxRows());
      result.setResult(orderInputSources);
      if (orderInputSources.size() >= searchRequest.getMaxRows()) {
        PreparedQuery<Integer> queryCount = queryCache.getQueryFromCache(selection.getSelectCountString(), con,
                                                                         countReader);
        result.setCount(con.queryOneRow(queryCount, selection.getParameter()));
      } else {
        result.setCount(orderInputSources.size());
      }
      boolean selectRefCnt = selection.containsColumn(OrderInputSourceColumn.REFERENCE_COUNT) && selectRefCountProperty.get();
      boolean selectState = selection.containsColumn(OrderInputSourceColumn.STATE) && selectStateProperty.get();
      if (selection.containsColumn(OrderInputSourceColumn.PARAMETER)) {
        for (OrderInputSourceStorable oigs : orderInputSources) {
          oigs.setParameters(getOrderInputSpecifics(con, oigs.getId()));
          setTransientCols(oigs, selectRefCnt, selectState);
        }
      } else {
        for (OrderInputSourceStorable oigs : orderInputSources) {
          setTransientCols(oigs, selectRefCnt, selectState);
        }
      }
    } finally {
      finallyClose(con);
    }
    return result;
  }


  private void setTransientCols(OrderInputSourceStorable oigs, boolean selectRefCount, boolean selectState) {
    if (selectState) {
      oigs.setState(getStatus(oigs.getName(), oigs.getRevision()));
    } else {
      oigs.setState("-");
    }
    if (selectRefCount) {
      try {
        oigs.setReferencedInputSourceCount(oism.getReferenceCount(xynaExecution.getExecutionDestination(oigs.getDestinationKey()),
                                                                  oigs.getRevision()));
      } catch (XPRC_DESTINATION_NOT_FOUND e) {
        oigs.setReferencedInputSourceCount(-1); //unknown = -1
      }
    }
  }


  private String getStatus(String name, long revision) {
    DeploymentItemState deploymentItemState =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement()
            .get(OrderInputSourceManagement.convertNameToUniqueDeploymentItemStateName(name), revision);
    if (deploymentItemState == null) {
      //keine abh�ngigkeiten zu xmom objekten - gibts eigtl gar nicht?!
      return "OK";
    }
    Set<DeploymentItemInterface> inconsistencies =
        deploymentItemState.getInconsistencies(DeploymentLocation.DEPLOYED, DeploymentLocation.DEPLOYED, false);
    if (inconsistencies.size() > 0) {
      return "INVALID";
    } else {
      return "OK";
    }
  }

  public List<OrderInputSourceSpecificStorable> getOrderInputSpecifics(ODSConnection con, long orderInputSourceId) throws PersistenceLayerException {
    PreparedQuery<OrderInputSourceSpecificStorable> query = 
        queryCache.getQueryFromCache(QUERY_GET_ORDER_INPUT_SOURCE_SPECIFIC, con, OrderInputSourceSpecificStorable.reader);
    return con.query(query, new Parameter(orderInputSourceId), -1);
  }


  public void findAndFillId(OrderInputSourceStorable inputSource) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      PreparedQuery<OrderInputSourceStorable> pq =
          queryCache.getQueryFromCache(QUERY_GET_ORDER_INPUT_SOURCE_BY_NAME, con, OrderInputSourceStorable.reader);
      //nulls werden nicht unterst�tzt, deshalb hier etwas tricksen und in der query mit "or" arbeiten
      List<OrderInputSourceStorable> list =
          con.query(pq,
                    new Parameter(inputSource.getName(), notNull(inputSource.getApplicationName()), notNull(inputSource.getVersionName()),
                                  notNull(inputSource.getWorkspaceName())), -1);
      if (list.size() == 0) {
        throw new RuntimeException("InputSource " + inputSource.getName() + " not found.");
      }
      if (list.size() > 1) {
        throw new RuntimeException("InputSource " + inputSource.getName() + " not unique.");
      }
      inputSource.setId(list.get(0).getId());
    } finally {
      con.closeConnection();
    }
  }


  private String notNull(String s) {
    return s == null ? "" : s;
  }

  public void deleteOrderInputSource(long inputSourceId) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteOneRow(new OrderInputSourceStorable(inputSourceId));
      con.delete(getOrderInputSpecifics(con, inputSourceId));
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  
  /**
   * Alle OrderInputSources einer Revision l�schen
   */
  public List<OrderInputSourceStorable> deleteOrderInputSourcesForRevision(long revision) throws PersistenceLayerException {
    List<OrderInputSourceStorable> toDelete = getOrderInputSourcesForRevision(revision);
    
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.delete(toDelete);
      con.commit();
    } finally {
      con.closeConnection();
    }
    
    return toDelete;
  }

  /**
   * Alle OrderInputSources einer Revision suchen.
   */
  public List<OrderInputSourceStorable> getOrderInputSourcesForRevision(long revision) throws PersistenceLayerException {
    SearchRequestBean srb = new SearchRequestBean(ArchiveIdentifier.orderInputSource, -1);
    Map<String, String> filter = new HashMap<String, String>();
    setFilterForRevision(filter, revision);
    srb.setFilterEntries(filter);
    
    try {
      return search(srb).getResult();
    } catch (XNWH_SelectParserException e) {
      throw new RuntimeException(e);
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException(e);
    }
  }

  public void setFilterForRevision(Map<String, String> filter, long revision) {
    RuntimeContext rc;
    try {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    String applicationName = rc instanceof Application ? ((Application) rc).getName() : null;
    String versionName = rc instanceof Application ? ((Application) rc).getVersionName() : null;
    String workspaceName = rc instanceof Workspace ? ((Workspace) rc).getName() : null;
    filter.put(OrderInputSourceStorable.COL_APPLICATIONNAME, SelectionParser.escape(applicationName));
    filter.put(OrderInputSourceStorable.COL_VERSIONNAME, SelectionParser.escape(versionName));
    filter.put(OrderInputSourceStorable.COL_WORKSPACENAME, SelectionParser.escape(workspaceName));
  }


  public OrderInputSourceStorable getInputSourceByName(String name, Long revision, boolean withParameters) throws PersistenceLayerException {
    RuntimeContext rc;
    try {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    return getInputSourceByName(name, rc instanceof Application ? ((Application) rc).getName() : null,
                                rc instanceof Application ? ((Application) rc).getVersionName() : null,
                                rc instanceof Workspace ? ((Workspace) rc).getName() : null,
                                withParameters);
  }


  public long getInputSourceIdByName(String name, Long revision) throws PersistenceLayerException {
    OrderInputSourceStorable oisStorable = getInputSourceByName(name, revision, false);
    if (oisStorable == null) {
      throw new IllegalArgumentException("No Order Input Source with name \"" + name + "\" available in revision "
          + revision);
    }
    return oisStorable.getId();
  }


  public OrderInputSourceStorable getInputSourceByName(String name, String applicationName, String versionName, String workspaceName, boolean withParameters)
      throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      PreparedQuery<OrderInputSourceStorable> pq =
          queryCache.getQueryFromCache(QUERY_GET_ORDER_INPUT_SOURCE_BY_NAME, con, OrderInputSourceStorable.reader);
      //nulls werden nicht unterst�tzt, deshalb hier etwas tricksen und in der query mit "or" arbeiten
      List<OrderInputSourceStorable> list =
          con.query(pq, new Parameter(name, notNull(applicationName), notNull(versionName), notNull(workspaceName)), 1);
      if (list.size() > 0) {
        OrderInputSourceStorable ois = list.get(0);
        if (withParameters) {
          ois.setParameters(getOrderInputSpecifics(con, ois.getId()));
        }
        return ois;
      }
    } finally {
      con.closeConnection();
    }
    return null;
  }

  /**
   * enth�lt nicht die specifics
   */
  public OrderInputSourceStorable getOrderInputSourceById(long orderInputSourceId) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      OrderInputSourceStorable ois = new OrderInputSourceStorable(orderInputSourceId);
      con.queryOneRow(ois);
      return ois;
    } finally {
      finallyClose(con);
    }
  }
  
  public List<String> getOrderInputSourceNames(Set<Long> orderInputSourceIds) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    List<String> inputSources = new ArrayList<String>();

    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      for (Long id : orderInputSourceIds) {
        OrderInputSourceStorable ois = new OrderInputSourceStorable(id);
        con.queryOneRow(ois);
        inputSources.add(ois.getName());
      }
    } finally {
      finallyClose(con);
    }
    
    return inputSources;
  }


  public int countInputSourcesOfType(String typeName) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      PreparedQuery<? extends OrderCount> pq = queryCache.getQueryFromCache(QUERY_COUNT_BY_TYPE, con, OrderCount.getCountReader());
      return con.queryOneRow(pq, new Parameter(typeName)).getCount();
    } finally {
      finallyClose(con);
    }
  }
}
