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
package com.gip.xyna.update;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.update.specialstorables.serialdatapreserving.OrderInstanceBackupPreservingSerializedData;
import com.gip.xyna.update.specialstorables.serialdatapreserving.OrderInstanceDetailsPreservingSerializedData;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.utils.streams.TeeInputStream;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.AuditData;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance.DynamicOrderInstanceReader;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;



public class UpdateOrderInstanceBackupWithRootOrderID extends UpdateJustVersion {

  private static PreparedQueryCache queryCache = new PreparedQueryCache();
  private static final String sqlGetAllOrderBackupWithNonNullXynaOrder = "select * from "
      + OrderInstanceBackup.TABLE_NAME + " WHERE " + OrderInstanceBackup.COL_XYNAORDER + " IS NOT NULL ORDER BY "
      + OrderInstanceBackup.COL_ID + " ASC";
  private static final String sqlGetChildOrderBackupWithID = "select * from " + OrderInstanceBackup.TABLE_NAME
      + " WHERE " + OrderInstanceBackup.COL_ID + "=?";
  private static final String sqlGetAllOrderArchive = "select * from " + OrderInstance.TABLE_NAME;
  private static final String sqlGetAllOrderArchiveIDs = "select " + OrderInstance.COL_ID + ", "
      + OrderInstance.COL_PARENT_ID + " from " + OrderInstance.TABLE_NAME ;
  private static final String sqlUpdateOrderArchive = "update " + OrderInstance.TABLE_NAME + " set "
      + OrderInstance.COL_ROOT_ID + " = ? where " + OrderInstance.COL_ID + " = ?";

  public UpdateOrderInstanceBackupWithRootOrderID(Version oldVersion, Version newVersion, boolean needsRegenerate) {
    super(oldVersion, newVersion, needsRegenerate);
  }

  

  @Override
  protected void update() throws XynaException {
    super.update();

    XynaFactoryBase oldInstance = XynaFactory.getInstance();
    try {
      // factory ist noch nicht initialisiert, zur Deserialisierung des xprc.xfractwfe.base.XynaProcess aber nötig
      //FIXME siehe Bug 12743 xprc.xfractwfe.base.XynaProcess
      UpdateGeneratedClasses.mockFactory();

      ODS ods = ODSImpl.getInstance();
      ods.registerStorable(OrderInstanceDetailsPreservingSerializedData.class);
      ods.registerStorable(OrderInstanceBackupPreservingSerializedData.class);
      ODSConnection conDef = ods.openConnection(ODSConnectionType.DEFAULT);
      ODSConnection conHis = ods.openConnection(ODSConnectionType.HISTORY);
      SerializableClassloadedObject.setIgnoreExceptionsWhileDeserializing(true);
      try {
        updateOrderBackup(conDef);
        updateOrderArchive(conHis);

      } finally {
        conDef.closeConnection();
        conHis.closeConnection();
        ods.unregisterStorable(OrderInstanceDetailsPreservingSerializedData.class);
        ods.unregisterStorable(OrderInstanceBackupPreservingSerializedData.class);
        SerializableClassloadedObject.setIgnoreExceptionsWhileDeserializing(false);
      }
    } finally {
      XynaFactory.setInstance(oldInstance);
    }
  }

  private void updateOrderBackup(ODSConnection conDef) throws PersistenceLayerException {
    FactoryWarehouseCursor<OrderInstanceBackupPreservingSerializedData> cursorOIB =
        conDef.getCursor(sqlGetAllOrderBackupWithNonNullXynaOrder, new Parameter(),
                         OrderInstanceBackupPreservingSerializedData.reader, 100);
    try {
      for( List<OrderInstanceBackupPreservingSerializedData> nextOBs : cursorOIB.batched(100) ) {
        updateOrderBackup(conDef, nextOBs);
      }
      cursorOIB.checkForExceptions();
    } catch( PersistenceLayerException e ) {
      logger.warn( "Failed to read OrderInstanceBackup ", e);
      throw e;
    }
  }

  private void updateOrderArchive(ODSConnection conHis) throws PersistenceLayerException {
    // build a map from the order archive
    Map<Long, Long> rootMap = new HashMap<Long, Long>();
    Set<OrderInstanceColumn> selectiveReadColumns = new TreeSet<OrderInstanceColumn>();
    selectiveReadColumns.add(OrderInstanceColumn.C_ID);
    selectiveReadColumns.add(OrderInstanceColumn.C_PARENT_ID);
    FactoryWarehouseCursor<OrderInstance> cursorOI =
        conHis.getCursor(sqlGetAllOrderArchiveIDs, new Parameter(),
                         new DynamicOrderInstanceReader(selectiveReadColumns), 100);

    try {
      for( List<OrderInstance> nextOIs : cursorOI.batched(100) ) {
        for (OrderInstance oi : nextOIs) {
          Long id = oi.getId();
          Long parentID = oi.getParentId();

          if (parentID == null) {
            parentID = id;
          }

          rootMap.put(id, parentID);
        }
      }
      cursorOI.checkForExceptions();
    } catch( PersistenceLayerException e ) {
      logger.warn( "Failed to read OrderInstance ", e);
      throw e;
    }

    examineRootMap(rootMap);

    // update order archive
    try {
      Command cmd = new Command(sqlUpdateOrderArchive);
      PreparedCommand pcmd = conHis.prepareCommand(cmd);

      for ( Map.Entry<Long,Long> entry: rootMap.entrySet() ) {
        Long id = entry.getKey();
        Long rootID = entry.getValue();
        conHis.executeDML(pcmd, new Parameter(rootID, id));
      }
      
      conHis.commit();
    } catch (PersistenceLayerException ple) {
      //falls Update in diesem PersistenceLayer nicht existiert: 
      //Fallback auf umständliches komplettes Auslesen und Persistieren
      FactoryWarehouseCursor<OrderInstanceDetailsPreservingSerializedData> cursorOID =
          conHis.getCursor(sqlGetAllOrderArchive, new Parameter(),
            new OrderInstanceDetailsPreservingSerializedData().getReader(),
            100);

      while (true) {
        List<OrderInstanceDetailsPreservingSerializedData> nextOIDs = cursorOID.getRemainingCacheOrNextIfEmpty();

        if (nextOIDs == null || nextOIDs.size() == 0) {
          break;
        }

        updateOrderArchive(conHis, nextOIDs, rootMap);
      }
    }

  }


  private void updateOrderBackup(ODSConnection con, List<OrderInstanceBackupPreservingSerializedData> orderBackups)
      throws PersistenceLayerException {
    List<OrderInstanceBackupPreservingSerializedData> modifiedEntries = new ArrayList<OrderInstanceBackupPreservingSerializedData>();

    PreparedQuery<OrderInstanceBackupPreservingSerializedData> childOrderBackupPreparedQuery = 
    (PreparedQuery<OrderInstanceBackupPreservingSerializedData>) queryCache
    .getQueryFromCache(sqlGetChildOrderBackupWithID, con,
                       OrderInstanceBackupPreservingSerializedData.reader);
    
    
    for (OrderInstanceBackupPreservingSerializedData orderBackup : orderBackups) {
      XynaOrderServerExtension xose = orderBackup.getXynaorder();
      
      if (xose == null) {
        continue;
      }
      long rootID = xose.getRootOrder().getId();

      for (XynaOrderServerExtension child : xose.getOrderAndChildrenRecursively() ) {
        
        OrderInstanceBackupPreservingSerializedData childBackup = con.queryOneRow(childOrderBackupPreparedQuery, new Parameter(child.getId()));
        if (childBackup != null) { //muss nicht notwendigerweise existieren
          //childBackup.getDetails().setRootId(rootID); wird nicht mehr in DB serialisiert 
          childBackup.setRootId(rootID);
          modifiedEntries.add(childBackup);
        }
      }
    }

    con.persistCollection(modifiedEntries);
    con.commit();
  }


  private void examineRootMap(Map<Long, Long> rootMap) {
    for (Long key : rootMap.keySet()) {
      Long myID = key;
      Long parentID = rootMap.get(myID);

      if (parentID == null || parentID == -1) {
        parentID = myID;
      }

      while (!parentID.equals(myID)) {
        myID = parentID;
        parentID = rootMap.get(myID);

        if (parentID == null || parentID == -1) {
          parentID = myID;
        }
      }

      rootMap.put(key, myID);
    }
  }


  private void updateOrderArchive(ODSConnection con, List<OrderInstanceDetailsPreservingSerializedData> orderArchives, Map<Long, Long> rootMap)
      throws PersistenceLayerException {
    List<OrderInstanceDetailsPreservingSerializedData> modifiedEntries = new ArrayList<OrderInstanceDetailsPreservingSerializedData>();

    for (OrderInstanceDetailsPreservingSerializedData orderArchive : orderArchives) {
      orderArchive.setRootId(rootMap.get(orderArchive.getId()));
      modifiedEntries.add(orderArchive);
    }

    con.persistCollection(modifiedEntries);
    con.commit();
  }
}
