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

package com.gip.xyna.xprc.xprcods.orderarchive;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;



public final class OrderArchiveQueryHelper {


  /**
   * ohne suspendierte aufträge mit monitoringlevel &gt;= 15, ggfs inkl suspendierter aufträge 
   * mit kleinerem monitoringlevel (dort kann man es nicht unterscheiden)<p>
   * da eine query aufs orderarchive, werden typischerweise nur aufträge mit monitoringlevel &gt;= 10 angezeigt
   */
  protected static String allActiveInstancesSQL = "select * from " + OrderInstanceDetails.TABLE_NAME
      + getWhereClauseStringBuilderForActiveInstancesFilter().toString() + " and ("
      + OrderInstance.COL_SUSPENSION_STATUS + " != '" + OrderInstanceSuspensionStatus.SUSPENDED.getName() + "' or "
      + OrderInstance.COL_SUSPENSION_STATUS + " is null) order by " + OrderInstanceDetails.COL_ID
      + " asc";
  
  protected static String getGetAllOwnSuspendedLogWarningIfNotSerializableWithBeginIndex_XynaOrderOnly = "select "
      + OrderInstanceBackup.COL_ID + ", " + OrderInstanceBackup.COL_ROOT_ID + ", " + OrderInstanceBackup.COL_XYNAORDER
      + ", " + OrderInstanceBackup.COL_BACKUP_CAUSE + ", " + OrderInstanceBackup.COL_BINDING + ", " + OrderInstanceBackup.COL_REVISION + ", "
      + OrderInstanceBackup.COL_BOOTCNTID + " from " + OrderInstanceBackup.TABLE_NAME + " where "
      + OrderInstanceBackup.COL_BACKUP_CAUSE + " = '" + BackupCause.SUSPENSION + "'" + " and not ("
      + OrderInstanceBackup.COL_XYNAORDER + " is null) and " + OrderInstanceBackup.COL_BINDING + " = ?";

  private static final ResultSetReader<? extends OrderInstance> orderInstanceReaderWithoutExceptionsAndExecType =
      new OrderInstance().getReaderWithoutExceptions();


  private OrderArchiveQueryHelper() {
  }


  static PreparedQuery<OrderInstanceBackup> getGetAllBackupsInRangeQuery(ODSConnection connection)
      throws PersistenceLayerException {
    Query<OrderInstanceBackup> qB =
        new Query<OrderInstanceBackup>("select * from " + OrderInstanceBackup.TABLE_NAME + " where "
            + OrderInstanceBackup.COL_ID + "<? and " + OrderInstanceBackup.COL_ID + ">? and "
            + OrderInstanceBackup.COL_BINDING + "=?", new OrderInstanceBackup().getReader());
    return connection.prepareQuery(qB, true);
  }


  private static StringBuilder getWhereClauseStringBuilderForActiveInstancesFilter() {
    StringBuilder whereClauseStringBuilder = new StringBuilder();
    whereClauseStringBuilder.append(" where (");
    appendActiveStatus( whereClauseStringBuilder );
    whereClauseStringBuilder.append(")");
    return whereClauseStringBuilder;
  }

  private static void appendActiveStatus(StringBuilder whereClauseStringBuilder) {
    String sep = "";
    for( OrderInstanceStatus e : OrderInstanceStatus.values() ) {
      if( e.isActive() ) {
        whereClauseStringBuilder.append(sep).
        append(OrderInstanceColumn.C_STATUS.getColumnName()).
        append(" = '").append(e.getName()).append("'");
        sep = " or ";
      }
    }
  }


  public static PreparedCommand getDeleteOldArchived(OrderArchive oa, ODSConnection con) throws PersistenceLayerException {
    OrderInstance backingClass = oa.auditAccess.getQueryBackingClass(con);
    return con.prepareCommand(new Command("delete from " + backingClass.getTableName() + " where "
        + OrderInstanceColumn.C_LAST_UPDATE.getColumnName() + " < ?"));
  }


  private static void appendAllOrderInstanceColumns(StringBuilder selectRelevantByParentIdSQL) {
    Column[] cols = Storable.getColumns(OrderInstance.class);
    for (Column column : cols) {
      selectRelevantByParentIdSQL.append(column.name());
      if (column != cols[cols.length - 1]) {
        selectRelevantByParentIdSQL.append(", ");
      }
    }
  }


  public static PreparedQuery<OrderInstance> getAllChildOrdersByParentId(OrderArchive oa, ODSConnection con)
      throws PersistenceLayerException {
    
    StringBuilder selectRelevantByParentIdSQL = new StringBuilder();
    selectRelevantByParentIdSQL.append("select ");

    appendAllOrderInstanceColumns(selectRelevantByParentIdSQL);

    OrderInstance backingClass = oa.auditAccess.getQueryBackingClass(con);
    selectRelevantByParentIdSQL.append(" from ").append(backingClass.getTableName()).append(" where ");
    selectRelevantByParentIdSQL.append(OrderInstance.COL_PARENT_ID).append("=?");

    return con.prepareQuery(new Query<OrderInstance>(selectRelevantByParentIdSQL.toString(),
                                                     orderInstanceReaderWithoutExceptionsAndExecType), true);

  }


  public static PreparedQuery<OrderInstance> getOrderInstanceByIdWithoutExceptions(OrderArchive oa, ODSConnection connection)
      throws PersistenceLayerException {
    StringBuilder selectRelevantColumns = new StringBuilder("select ");
    appendAllOrderInstanceColumns(selectRelevantColumns);
    
    OrderInstance backingClass = oa.auditAccess.getQueryBackingClass(connection);
    selectRelevantColumns.append(" from ").append(backingClass.getTableName()).append(" where ");
    
    selectRelevantColumns.append(OrderInstance.COL_ID).append("=?");
    return connection.prepareQuery(new Query<OrderInstance>(selectRelevantColumns.toString(),
                                                            orderInstanceReaderWithoutExceptionsAndExecType), true);
  }
  

  public static PreparedQuery<OrderInstance> getGetOrderInstanceByRootIdQuery(OrderArchive oa, ODSConnection connection)
                  throws PersistenceLayerException {
    StringBuilder selectRelevantColumns = new StringBuilder("select ");
    appendAllOrderInstanceColumns(selectRelevantColumns);
    
    OrderInstance backingClass = oa.auditAccess.getQueryBackingClass(connection);
    selectRelevantColumns.append(" from ").append(backingClass.getTableName()).append(" where ");
    
    selectRelevantColumns.append(OrderInstance.COL_ROOT_ID).append("=?");
    return connection.prepareQuery(new Query<OrderInstance>(selectRelevantColumns.toString(),
                                                            orderInstanceReaderWithoutExceptionsAndExecType), true);
  }


  public static PreparedCommand getUpdateOrderInstanceBackupCause(ODSConnection defaultCon)
      throws PersistenceLayerException {
    return defaultCon.prepareCommand(new Command("update " + OrderInstanceBackup.TABLE_NAME + " set "
        + OrderInstanceBackup.COL_BACKUP_CAUSE + "=? where " + OrderInstanceBackup.COL_ID + "=?"), true);
  }


  public static PreparedQuery<Long> getGetRootOrderIdsQuery(ODSConnection connection) throws PersistenceLayerException {
    String sql = "select "+OrderInstanceBackup.COL_ROOT_ID+" from " + OrderInstanceBackup.TABLE_NAME;
    ResultSetReader<Long> rsr = new ResultSetReader<Long>() {
      public Long read(ResultSet rs) throws SQLException {
        return rs.getLong(1);
      }
    };
    return connection.prepareQuery(new Query<Long>(sql, rsr), true);
  }
 
}
