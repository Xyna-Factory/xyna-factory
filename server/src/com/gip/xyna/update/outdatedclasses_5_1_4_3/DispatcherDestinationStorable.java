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

package com.gip.xyna.update.outdatedclasses_5_1_4_3;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;



@Persistable(tableName = DispatcherDestinationStorable.TABLE_NAME, primaryKey = DispatcherDestinationStorable.COL_ID)
public class DispatcherDestinationStorable extends Storable<DispatcherDestinationStorable> {

 private static final long serialVersionUID = -4067556393760123896L;

 public static final String TABLE_NAME = "dispatcherdestinations";

 public static final String COL_ID = "id";
 public static final String COL_DESTINATION_KEY = "destinationkey";
 public static final String COL_DISPATCHER_NAME = "dispatchername";
 public static final String COL_DESTINATION_TYPE = "destinationtype";
 public static final String COL_DESTINATION_VALUE = "destinationvalue";
 public static final String COL_APPLICATIONNAME = "applicationname";
 public static final String COL_VERSIONNAME = "versionname";

 @Column(name = COL_ID, index = IndexType.PRIMARY)
 private long id;

 @Column(name = COL_DESTINATION_KEY)
 private String destinationKey;

 @Column(name = COL_DISPATCHER_NAME)
 private String dispatcherName;

 @Column(name = COL_DESTINATION_TYPE)
 private String destinationType;

 @Column(name = COL_DESTINATION_VALUE)
 private String destinationValue;

 @Column(name = COL_APPLICATIONNAME)
 private String applicationname;

 @Column(name = COL_VERSIONNAME)
 private String versionname;
 

 public DispatcherDestinationStorable() {
 }


 public DispatcherDestinationStorable(long id) {
   this.id = id;
 }


 public DispatcherDestinationStorable(String destinationKey, String dispatcherName, ExecutionType type,
                                      String destinationValue, String applicationName, String versionName) {
   this(XynaFactory.getInstance().getIDGenerator().getUniqueId(), destinationKey, dispatcherName, type,
        destinationValue, applicationName, versionName);
 }


 public DispatcherDestinationStorable(long id, String destinationKey, String dispatcherName, ExecutionType type,
                                      String destinationValue, String applicationName, String versionName) {
   this(id);
   this.destinationKey = destinationKey;
   this.dispatcherName = dispatcherName;
   this.destinationType = type.getTypeAsString();
   this.destinationValue = destinationValue;
   this.applicationname = applicationName;
   this.versionname = versionName;
 }


 @Override
 public ResultSetReader<? extends DispatcherDestinationStorable> getReader() {
   return reader;
 }


 @Override
 public Object getPrimaryKey() {
   return id;
 }


 @Override
 public <U extends DispatcherDestinationStorable> void setAllFieldsFromData(U data) {
   DispatcherDestinationStorable cast = data;
   this.id = cast.id;
   this.destinationKey = cast.destinationKey;
   this.dispatcherName = cast.dispatcherName;
   this.destinationType = cast.destinationType;
   this.destinationValue = cast.destinationValue;
   this.applicationname = cast.applicationname;
   this.versionname = cast.versionname;
    
 }


 private static void readFromResultSet(DispatcherDestinationStorable dds, ResultSet rs) throws SQLException {
   dds.id = rs.getLong(COL_ID);
   if (rs.wasNull()) {
     throw new SQLException("Primary key " + COL_ID + " may not be null");
   }
   dds.destinationKey = rs.getString(COL_DESTINATION_KEY);
   dds.dispatcherName = rs.getString(COL_DISPATCHER_NAME);
   dds.destinationType = rs.getString(COL_DESTINATION_TYPE);
   dds.destinationValue = rs.getString(COL_DESTINATION_VALUE);
   dds.applicationname = rs.getString(COL_APPLICATIONNAME);
   dds.versionname = rs.getString(COL_VERSIONNAME);
 }


 private static final DispatcherDestinationStorableReader reader = new DispatcherDestinationStorableReader();


 private static class DispatcherDestinationStorableReader implements ResultSetReader<DispatcherDestinationStorable> {

   public DispatcherDestinationStorable read(ResultSet rs) throws SQLException {
     DispatcherDestinationStorable result = new DispatcherDestinationStorable();
     readFromResultSet(result, rs);
     return result;
   }

 }


 public String getDispatcherName() {
   return this.dispatcherName;
 }


 public String getDestinationKey() {
   return this.destinationKey;
 }


 public String getDestinationType() {
   return this.destinationType;
 }


 public ExecutionType getDestinationTypeAsEnum() {
   return ExecutionType.getByTypeString(this.destinationType);
 }


 public String getDestinationValue() {
   return this.destinationValue;
 }


 public long getId() {
   return this.id;
 }


 private static Map<ODSConnectionType, PreparedCommand> deleteDestinationkeyByApplicationNameCommandByConnectionType = new HashMap<ODSConnectionType, PreparedCommand>();
 private static Map<ODSConnectionType, PreparedQuery<DispatcherDestinationStorable>> getAllDestinationsWithDestinationKeyForThisDispatcherByConnectionType =
     new HashMap<ODSConnectionType, PreparedQuery<DispatcherDestinationStorable>>();
 private static Map<ODSConnectionType, PreparedQuery<DispatcherDestinationStorable>> getAllDestinationsWithDestinationKeyByConnectionType =
     new HashMap<ODSConnectionType, PreparedQuery<DispatcherDestinationStorable>>();

 
 

 public static PreparedCommand getCommandDeleteByDestinationKeyAndApplicationName(ODSConnection connection)
                 throws PersistenceLayerException {
   ODSConnectionType connectionType = connection.getConnectionType();
   PreparedCommand result = deleteDestinationkeyByApplicationNameCommandByConnectionType.get(connectionType);
   if (result != null) {
     return result;
   }

   synchronized (DispatcherDestinationStorable.class) {
     result = deleteDestinationkeyByApplicationNameCommandByConnectionType.get(connectionType);
     if (result != null) {
       return result;
     }
     result = connection.prepareCommand(new Command("delete from " + DispatcherDestinationStorable.TABLE_NAME 
                                                    + " where " + DispatcherDestinationStorable.COL_DESTINATION_KEY 
                                                    + " = ? and " + COL_APPLICATIONNAME + " = ? and " + COL_VERSIONNAME + " = ?"),
             true);
     deleteDestinationkeyByApplicationNameCommandByConnectionType.put(connectionType, result);
     return result;

   }
 }
 
 
 private static final String SELECT_ALL_BY_DISP_NAME_AND_DK = "select * from " + TABLE_NAME + " where "
     + COL_DESTINATION_KEY + " = ? and " + COL_DISPATCHER_NAME + " = ?";


 public static PreparedQuery<DispatcherDestinationStorable> getAllDestinationsWithDestinationKeyForThisDispatcher(ODSConnection connection)
                 throws PersistenceLayerException {
   
   ODSConnectionType connectionType = connection.getConnectionType();
   PreparedQuery<DispatcherDestinationStorable> result = getAllDestinationsWithDestinationKeyForThisDispatcherByConnectionType
                   .get(connectionType);
   if (result != null) {
     return result;
   }
   synchronized (DispatcherDestinationStorable.class) {
     result = getAllDestinationsWithDestinationKeyForThisDispatcherByConnectionType.get(connectionType);
     if (result != null) {
       return result;
     }
     result = connection
                     .prepareQuery(new Query<DispatcherDestinationStorable>(SELECT_ALL_BY_DISP_NAME_AND_DK, reader),
                                   true);
     getAllDestinationsWithDestinationKeyForThisDispatcherByConnectionType.put(connectionType, result);
     return result;
   }
 }
 
 
 private static final String SELECT_ALL_BY_DK = "select * from " + TABLE_NAME + " where " + COL_DESTINATION_KEY + " = ?";

 public static PreparedQuery<DispatcherDestinationStorable> getAllDestinationsWithDestinationKey(ODSConnection connection)
                 throws PersistenceLayerException {

   ODSConnectionType connectionType = connection.getConnectionType();
   PreparedQuery<DispatcherDestinationStorable> result = getAllDestinationsWithDestinationKeyByConnectionType.get(connectionType);
   if (result != null) {
     return result;
   }
   synchronized (DispatcherDestinationStorable.class) {
     result = getAllDestinationsWithDestinationKeyByConnectionType.get(connectionType);
     if (result != null) {
       return result;
     }
     result = connection.prepareQuery(new Query<DispatcherDestinationStorable>(SELECT_ALL_BY_DK, reader), true);
     getAllDestinationsWithDestinationKeyByConnectionType.put(connectionType, result);
     return result;
   }
 }


 
 public String getApplicationname() {
   return applicationname;
 }

 
 public String getVersionname() {
   return versionname;
 }


}
