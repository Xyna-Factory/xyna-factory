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
package com.gip.xyna.update;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.update.outdatedclasses_7_0_2_13.OrderInstanceBackup;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrder.RootRevisionHolder;
import com.gip.xyna.xprc.XynaOrderServerExtension;


public class UpdateAddRootRevisionForOrderBackups extends UpdateJustVersion {


  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateAddRootRevisionForOrderBackups.class);

  private static String selectAllLocalBackupIds = "select "+OrderInstanceBackup.COL_ID +"," + OrderInstanceBackup.COL_ROOT_ID +"," + OrderInstanceBackup.COL_REVISION +  " from " + OrderInstanceBackup.TABLE_NAME;
  private static String selectFamilyBackupIds = "select "+OrderInstanceBackup.COL_ID +" from " + OrderInstanceBackup.TABLE_NAME + " where " + OrderInstanceBackup.COL_ROOT_ID + "=?";
  private static String selectOrderBackup = "select * from " + OrderInstanceBackup.TABLE_NAME + " where " + OrderInstanceBackup.COL_ID + "=?";

  

  public UpdateAddRootRevisionForOrderBackups(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion, false);
    setExecutionTime(ExecutionTime.endOfFactoryStart);
  }

  

  protected void update() throws XynaException {
    try {
      ODS ods = ODSImpl.getInstance();
      ods.unregisterStorable(com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.class);
      ods.registerStorable(OrderInstanceBackup.class);
      ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
      SerializableClassloadedObject.setIgnoreExceptionsWhileDeserializing(true);
      try {
        Map<Long, Long> rootOrderIds = getAllRootOrderIdsAndRevisions(con);
        for (Long rootOrderId : rootOrderIds.keySet()) {
          RootRevisionHolder rrh = XynaOrderServerExtension.rootRevisionTL.get();
          rrh.set(rootOrderIds.get(rootOrderId));
          try {
            List<Long> familiyIds = getAllFamilyIds(con, rootOrderId);
            for (Long id : familiyIds) {
              PreparedQuery<OrderInstanceBackup> pq = con.prepareQuery(new Query<OrderInstanceBackup>(selectOrderBackup, OrderInstanceBackup.reader));
              try {
                OrderInstanceBackup oib = con.queryOneRow(pq, new Parameter(id));
                con.persistObject(oib);
              } catch (XynaException e) {
                if (identifiesAsAlreadyRan(e)) {
                  continue;
                } else {
                  throw e;
                }
              } catch (RuntimeException e) {
                if (identifiesAsAlreadyRan(e)) {
                  continue;
                } else {
                  throw e;
                }
              }
            }
            con.commit();
          } finally {
            if (rrh.remove()) {
              XynaOrderServerExtension.rootRevisionTL.remove();
            }
          }
        }
      } finally {
        con.closeConnection();
        ods.unregisterStorable(OrderInstanceBackup.class);
        ods.registerStorable(com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.class);
        SerializableClassloadedObject.setIgnoreExceptionsWhileDeserializing(false);
      }
    } catch (XynaException e) {
      throw new RuntimeException("Update failed", e);
    }
  }


   
  private boolean identifiesAsAlreadyRan(Throwable e) {
    Throwable current = e;
    while (current != null) {
      if (current.getMessage() != null && current.getMessage().equals(com.gip.xyna.update.outdatedclasses_7_0_2_13.AuditData.UPDATE_ALREADY_RAN_IDENTIFIER)) {
        return true;
      }
      if (current.getCause() == current) {
        return false;
      } else {
        current = current.getCause();
      }
    }
    return false;
  }



  private Map<Long, Long>/*rootOrderId -> revision*/ getAllRootOrderIdsAndRevisions(ODSConnection con) throws PersistenceLayerException {
    PreparedQuery<Triple<Long, Long, Long>> /*id, rootOrderId, revision*/ query = con.prepareQuery(new Query<Triple<Long, Long, Long>>(selectAllLocalBackupIds, new ResultSetReader<Triple<Long, Long, Long>>() {
          public Triple<Long, Long, Long> read(ResultSet rs) throws SQLException {
            return Triple.<Long, Long, Long>of(rs.getLong(OrderInstanceBackup.COL_ID), rs.getLong(OrderInstanceBackup.COL_ROOT_ID), rs.getLong(OrderInstanceBackup.COL_REVISION));
          }
        }));
    Map<Long, Long> map = new HashMap<Long, Long>();
    Collection<Triple<Long, Long, Long>> results =  con.query( query, new Parameter(), -1);
    for (Triple<Long, Long, Long> result : results) {
      if (result.getFirst().equals(result.getSecond())) {
        map.put(result.getFirst(), result.getThird());
      }
    }
    return map;
  }
  
  
  private List<Long> getAllFamilyIds(ODSConnection con, Long rootId) throws PersistenceLayerException {
    PreparedQuery<Long> query = con.prepareQuery(new Query<Long>(selectFamilyBackupIds, new ResultSetReader<Long>() {
      public Long read(ResultSet rs) throws SQLException {
        return rs.getLong(OrderInstanceBackup.COL_ID);
      }
    }));
    return con.query( query, new Parameter(rootId), -1);
  }


}
