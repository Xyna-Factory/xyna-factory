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

package com.gip.xyna.xprc.xsched.cronlikescheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoPersistenceLayerConfiguredForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResultOneException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderCount;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderColumn;


public final class CronLikeOrderHelpers {
  

  private static final Logger logger = CentralFactoryLogging.getLogger(CronLikeOrderHelpers.class);


  private CronLikeOrderHelpers() {
  }

  private static HashMap<EnumSet<CronLikeOrderColumn>, String> countKeyQueries = new HashMap<EnumSet<CronLikeOrderColumn>, String>();
  public static PreparedQueryCache queryCache = new PreparedQueryCache();


  public static final String sqlCountCronLikeOrders = "SELECT count(*) FROM " + CronLikeOrder.TABLE_NAME;
  
  public static final String sqlCountCronLikeOrdersForBinding = "SELECT count(*) FROM " + CronLikeOrder.TABLE_NAME + " WHERE "
                  + CronLikeOrder.COL_BINDING + " = ?";
  
  public static final String sqlGetCronLikeOrders = "SELECT * FROM " + CronLikeOrder.TABLE_NAME + " ORDER BY "
      + CronLikeOrder.COL_NEXT_EXEC_TIME + " ASC";

  public static final String sqlGetCronLikeOrdersForBinding = "SELECT * FROM " + CronLikeOrder.TABLE_NAME + " WHERE "
      + CronLikeOrder.COL_BINDING + " = ? ORDER BY " + CronLikeOrder.COL_NEXT_EXEC_TIME + " ASC";

  public static final String sqlGetCronLikeOrdersWithDifferentBinding = "SELECT * FROM " + CronLikeOrder.TABLE_NAME
      + " WHERE NOT " + CronLikeOrder.COL_BINDING + " = ? ORDER BY " + CronLikeOrder.COL_NEXT_EXEC_TIME + " ASC";
  
  private static final String sqlGetCronLikeOrdersWithOwnBindingAndRootOrderId = "SELECT * FROM " + CronLikeOrder.TABLE_NAME
                  + " WHERE " + CronLikeOrder.COL_BINDING + " = ? and " + CronLikeOrder.COL_ASSIGNED_ROOT_ORDER_ID + " =?";

  public static final String sqlGetNextCronLikeOrders = "SELECT * FROM " + CronLikeOrder.TABLE_NAME + " WHERE "
      + CronLikeOrder.COL_NEXT_EXEC_TIME + " IS NOT NULL ORDER BY " + CronLikeOrder.COL_NEXT_EXEC_TIME + " ASC";

  public static final String sqlGetNextCronLikeOrdersForBinding = "SELECT * FROM " + CronLikeOrder.TABLE_NAME
      + " WHERE " + CronLikeOrder.COL_BINDING + " = ?  AND "
      + CronLikeOrder.COL_NEXT_EXEC_TIME + " IS NOT NULL ORDER BY " + CronLikeOrder.COL_NEXT_EXEC_TIME + " ASC";
  
  public static final String sqlGetNextEnabledCronLikeOrders = "SELECT * FROM " + CronLikeOrder.TABLE_NAME + " WHERE "
      + CronLikeOrder.COL_ENABLED + " = ? AND " + CronLikeOrder.COL_NEXT_EXEC_TIME + " IS NOT NULL ORDER BY " 
      + CronLikeOrder.COL_NEXT_EXEC_TIME + " ASC";

  public static final String sqlGetNextEnabledCronLikeOrdersForBinding = "SELECT * FROM " + CronLikeOrder.TABLE_NAME
      + " WHERE " + CronLikeOrder.COL_BINDING + " = ?  AND " + CronLikeOrder.COL_ENABLED + " = ? AND "
      + CronLikeOrder.COL_NEXT_EXEC_TIME + " IS NOT NULL ORDER BY " + CronLikeOrder.COL_NEXT_EXEC_TIME + " ASC";
  
  private static void closeConnectionWithoutException(ODSConnection con) {
    try {
      con.closeConnection();
    } catch (PersistenceLayerException e) {
      logger.warn("Failed to close connection", e);
    }
  }


  @SuppressWarnings("unchecked")
  protected static int countCronLikeOrders(ODSConnection con) throws PersistenceLayerException {
    OrderCount count;
    try {
      PreparedQuery<OrderCount> pq =
        (PreparedQuery<OrderCount>) queryCache.getQueryFromCache(sqlCountCronLikeOrders, con,
                                                                    OrderCount.getCountReader());
      count = con.queryOneRow(pq, new Parameter());
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      queryCache.clear();
      
      PreparedQuery<OrderCount> pq =
                      (PreparedQuery<OrderCount>) queryCache.getQueryFromCache(sqlCountCronLikeOrders, con,
                                                                                  OrderCount.getCountReader());
      count = con.queryOneRow(pq, new Parameter());
    }
    return count.getCount();
  }

  
  @SuppressWarnings("unchecked")
  protected static int countCronLikeOrdersForBinding(int binding, ODSConnection con) throws PersistenceLayerException {
    OrderCount count;
    try {
      PreparedQuery<OrderCount> pq =
        (PreparedQuery<OrderCount>) queryCache.getQueryFromCache(sqlCountCronLikeOrdersForBinding, con,
                                                                    OrderCount.getCountReader());
      count = con.queryOneRow(pq, new Parameter(binding));
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      queryCache.clear();
      PreparedQuery<OrderCount> pq =
                      (PreparedQuery<OrderCount>) queryCache.getQueryFromCache(sqlCountCronLikeOrdersForBinding, con,
                                                                                  OrderCount.getCountReader());
      count = con.queryOneRow(pq, new Parameter(binding));
    }

    return count.getCount();
  }

  
  /**
   * Delete cron like order with given id from default and history connection.
   * @return true if the order existed within the default connection
   */
  protected static boolean delete(final Long id, ODSConnection con) throws XPRC_CronRemovalException,
      XNWH_RetryTransactionException {

    final boolean createdConnection;

    if (con == null) {
      con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      createdConnection = true;
    } else {
      createdConnection = false;
      if (con.getConnectionType() != ODSConnectionType.DEFAULT) {
        throw new RuntimeException("Unexpected connection type <" + con.getConnectionType() + ">");
      }
    }

    WarehouseRetryExecutableNoException<Boolean> wre = new WarehouseRetryExecutableNoException<Boolean>() {

      public Boolean executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        CronLikeOrder defaultOrder = new CronLikeOrder(id);
        boolean existed = con.containsObject(defaultOrder);
        // Das kann z.B. false sein, wenn es eine Racecondition gibt zwischen dem Löschen und dem Ausführen des Jobs.
        // Z.B. bei Synchronization, falls der timeout los läuft und den job löschen will und in der Zwischenzeit ist
        // er aber gecancelt worden.
        if (existed) {
          con.deleteOneRow(defaultOrder);
        }
        if (createdConnection) {
          con.commit();
        }
        return existed;
      }
    };

    final boolean existedInDefault;
    try {
      if (!createdConnection) {
        existedInDefault = wre.executeAndCommit(con);
      } else {
        existedInDefault =
            WarehouseRetryExecutor
                .executeWithRetriesNoException(wre, ODSConnectionType.DEFAULT,
                                               Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                               Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                               new StorableClassList(CronLikeOrder.class));
      }
    } catch (XNWH_RetryTransactionException e) {
      if (!createdConnection) {
        throw e;
      } else {
        throw new XPRC_CronRemovalException(id, e);
      }
    } catch (PersistenceLayerException ple) {
      throw new XPRC_CronRemovalException(id, ple);
    }

    // delete from history if necessary
    boolean historyAndMemoryAreTheSame = false;
    try {
      historyAndMemoryAreTheSame =
          ODSImpl.getInstance().isSamePhysicalTable(CronLikeOrder.TABLE_NAME, ODSConnectionType.HISTORY,
                                                    ODSConnectionType.DEFAULT);
    } catch (XNWH_NoPersistenceLayerConfiguredForTableException e) {
      throw new RuntimeException("Failed to check table identity", e);
    }

    if (!historyAndMemoryAreTheSame) {
      // always use a new connection for history access
      final CronLikeOrder hisOrder = new CronLikeOrder(id);

      WarehouseRetryExecutableNoResultOneException<XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY> wre2 =
          new WarehouseRetryExecutableNoResultOneException<XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY>() {

            public void executeAndCommit(ODSConnection conHis) throws PersistenceLayerException,
                XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
              conHis.queryOneRow(hisOrder);
              conHis.delete(Arrays.asList(hisOrder));
              conHis.commit();
            }
          };

      try {
        WarehouseRetryExecutor
            .executeWithRetriesOneException(wre2, ODSConnectionType.HISTORY,
                                            Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                            Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                            new StorableClassList(CronLikeOrder.class));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // Es muss nicht immer ein History-Eintrag vorhanden sein!
      } catch (PersistenceLayerException e) {
        throw new XPRC_CronRemovalException(id, e);
      }
    }

    // return the default order because this is the most up2date one
    return existedInDefault;

  }


  /**
   * Find cron like order with given primary key in default connection
   * @return the found order
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY if no order with given id was found
   */
  public static CronLikeOrder find(long pk, ODSConnection externalConnection) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    CronLikeOrder retVal = new CronLikeOrder(pk);
    ODSConnection con = externalConnection;
    boolean usingForeignConnection = externalConnection != null;
    
    if (!usingForeignConnection) {
      con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    }
    
    try {
      con.queryOneRow(retVal);
    } finally {
      if (!usingForeignConnection) {
        closeConnectionWithoutException(con);
      }
    }
    
    return retVal;
  }
  
  /**
   * Find all cron like orders from default connection with a maximum number of rows.
   * @param maxRows the number of rows to read
   */
  @SuppressWarnings("unchecked")
  public static List<CronLikeOrder> findAll(long maxRows) throws PersistenceLayerException {
    List<CronLikeOrder> retVal = new ArrayList<CronLikeOrder>();
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      PreparedQuery<CronLikeOrder> pq =
        (PreparedQuery<CronLikeOrder>) queryCache.getQueryFromCache(sqlGetCronLikeOrders, con,
                                                                    new CronLikeOrder().getReader());
      retVal = con.query(pq, new Parameter(), (int) maxRows);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      queryCache.clear();
      PreparedQuery<CronLikeOrder> pq =
        (PreparedQuery<CronLikeOrder>) queryCache.getQueryFromCache(sqlGetCronLikeOrders, con,
                                                                    new CronLikeOrder().getReader());
      retVal = con.query(pq, new Parameter(), (int) maxRows);
    } finally {
      closeConnectionWithoutException(con);
    }

    return retVal;
  }
  
  
  /**
   * Find all cron like orders from default connection that are not set to the given binding.
   * @param binding of the requested cron like orders
   */
  @SuppressWarnings("unchecked")
  public static List<CronLikeOrder> findAllWithRootOrderId(int binding, Long rootOrderId, ODSConnection con) throws PersistenceLayerException {
    List<CronLikeOrder> retVal = new ArrayList<CronLikeOrder>();
    boolean createdNewConnection = false;
    if(con == null) {
      con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      createdNewConnection = true;
    }
    try {
      PreparedQuery<CronLikeOrder> pq =
        (PreparedQuery<CronLikeOrder>) queryCache.getQueryFromCache(sqlGetCronLikeOrdersWithOwnBindingAndRootOrderId, con,
                                                                    new CronLikeOrder().getReader());
      Parameter sqlParamBinding = new Parameter(binding, rootOrderId);
      retVal = con.query(pq, sqlParamBinding, -1);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      queryCache.clear();
      PreparedQuery<CronLikeOrder> pq =
        (PreparedQuery<CronLikeOrder>) queryCache.getQueryFromCache(sqlGetCronLikeOrdersWithOwnBindingAndRootOrderId, con,
                                                                    new CronLikeOrder().getReader());
      Parameter sqlParamBinding = new Parameter(binding, rootOrderId);
      retVal = con.query(pq, sqlParamBinding, -1);
    } finally {
      if(createdNewConnection) {
        closeConnectionWithoutException(con);
      }
    }
    return retVal;
  }
  
  protected static void selectForUpdate(CronLikeOrder order, ODSConnection con) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    if (con == null) {
      throw new IllegalArgumentException("connection may not be null");
    }
    con.queryOneRowForUpdate(order);
  }


  /**
   * Store all cron like orders in default connection.
   * @param order collection of cron like orders to be stored
   */
  protected static void store(final Collection<CronLikeOrder> order, ODSConnection connection)
      throws PersistenceLayerException {
    if ((order == null) || (order.size() < 1)) {
      return;
    }

    final boolean createdConnection;

    if (connection == null) {
      connection = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
      createdConnection = true;
    } else {
      createdConnection = false;

      if (connection.getConnectionType() != ODSConnectionType.DEFAULT) {
        throw new RuntimeException("Unexpected connection type <" + connection.getConnectionType() + ">");
      }
    }

    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        con.persistCollection(order);

        if (createdConnection) {
          con.commit();
        }
      }
    };

    if (!createdConnection) {
      wre.executeAndCommit(connection);
    } else {
      WarehouseRetryExecutor.executeWithRetriesNoException(wre, ODSConnectionType.DEFAULT,
                                                           Constants.DEFAULT_CONNECTION_TO_CLUSTER_BROKEN_RETRIES,
                                                           Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__CRITICAL,
                                                           new StorableClassList(CronLikeOrder.class));
    }

  }


  /**
   * Store a single cron like order in default connection.
   * @param order the cron like order to store
   */
  protected static void store(CronLikeOrder order, ODSConnection con) throws PersistenceLayerException {
    store(Arrays.asList(order), con);
  }

  public static int countKeys(EnumSet<CronLikeOrderColumn> uniqueKeys, CronLikeOrder values, ODSConnection con) throws PersistenceLayerException {
    String query = null;
    synchronized ( countKeyQueries ) {
      query = countKeyQueries.get(uniqueKeys);
    }
    if( query == null ) {
      StringBuilder sb = new StringBuilder();
      sb.append( "select count(*) from ").append(CronLikeOrder.TABLE_NAME);
      String sep = " where ";
      for( CronLikeOrderColumn  cloc : uniqueKeys ) {
        sb.append(sep).append(cloc.getColumnName()).append("=?");
        sep = " and ";
      }
      query = sb.toString();
      synchronized ( countKeyQueries ) {
        countKeyQueries.put(uniqueKeys,query);
      }
    }
      
    Parameter params = new Parameter();
    for( CronLikeOrderColumn cloc : uniqueKeys ) {
      params.add(values.get(cloc));
    }
    OrderCount count = con.queryOneRow(prepareQuery(query,con,OrderCount.getCountReader()), params);
    return count.getCount();
  }

  private static <R> PreparedQuery<R> prepareQuery(String query, ODSConnection con,
                                                   ResultSetReader<R> reader) throws PersistenceLayerException {
    try {
      return queryCache.getQueryFromCache(query, con, reader );
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      queryCache.clear();
      return queryCache.getQueryFromCache(query, con, reader );
    }
  }
  

  
}
