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

package com.gip.xyna.update.outdatedclasses_5_1_4_3;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;



@Persistable(tableName = CapacityMappingStorable.TABLE_NAME, primaryKey = CapacityMappingStorable.COL_ID)
public class CapacityMappingStorable extends Storable<CapacityMappingStorable> {

 private static final long serialVersionUID = -2913888790855728921L;

 public static final String TABLE_NAME = "capacitymappings";
 public static final String COL_ID = "id";
 public static final String COL_ORDER_TYPE = "ordertype";
 public static final String COL_REQUIRED_CAPACITIES = "requiredcapacities";
 public static final String COL_APPLICATIONNAME = "applicationname";
 public static final String COL_VERSIONNAME = "versionname";

 private static final ResultSetReader<CapacityMappingStorable> reader = new CapacityMappingStorableReader();

 @Column(name = COL_ID, index = IndexType.PRIMARY)
 private Long id;
 
 @Column(name = COL_ORDER_TYPE)
 private String orderType;

 @Column(name = COL_REQUIRED_CAPACITIES, type = ColumnType.BLOBBED_JAVAOBJECT)
 private ArrayList<Capacity> requiredCapacities;
 
 @Column(name = COL_APPLICATIONNAME)
 private String applicationname;
 
 @Column(name = COL_VERSIONNAME)
 private String versionname;


 public CapacityMappingStorable() {
 }


 public CapacityMappingStorable(DestinationKey destinationKey) {
   this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
   this.orderType = destinationKey.getOrderType();
   this.applicationname = destinationKey.getApplicationName();
   this.versionname = destinationKey.getVersionName();
   this.requiredCapacities = new ArrayList<Capacity>();
 }
 
 public CapacityMappingStorable(DestinationKey destinationKey, List<Capacity> requiredCapacities, Long id) {
   if(XynaFactory.getInstance().finishedInitialization()) {
     throw new RuntimeException("Unallowed call of constructor.");
   }
   this.id = id;
   this.orderType = destinationKey.getOrderType();
   this.applicationname = destinationKey.getApplicationName();
   this.versionname = destinationKey.getVersionName();
   this.requiredCapacities = new ArrayList<Capacity>(requiredCapacities);
 }


 @Override
 public ResultSetReader<? extends CapacityMappingStorable> getReader() {
   return reader;
 }

 @Override
 public Object getPrimaryKey() {
   return id;
 }


 public Long getId() {
   return id;
 }


 @Override
 public <U extends CapacityMappingStorable> void setAllFieldsFromData(U data) {
   CapacityMappingStorable cast = data;
   this.orderType = cast.orderType;
   this.requiredCapacities = new ArrayList<Capacity>(cast.requiredCapacities);
   this.id = cast.id;
   this.applicationname = cast.applicationname;
   this.versionname = cast.versionname;
 }


 private static void fillByResultset(CapacityMappingStorable cms, ResultSet rs) throws SQLException {
   cms.orderType = rs.getString(COL_ORDER_TYPE);
   cms.requiredCapacities = (ArrayList<Capacity>) cms.readBlobbedJavaObjectFromResultSet(rs, COL_REQUIRED_CAPACITIES);
   cms.applicationname = rs.getString(COL_APPLICATIONNAME);
   cms.versionname = rs.getString(COL_VERSIONNAME);
   cms.id = rs.getLong(COL_ID);
 }


 private static class CapacityMappingStorableReader implements ResultSetReader<CapacityMappingStorable> {

   public CapacityMappingStorable read(ResultSet rs) throws SQLException {
     CapacityMappingStorable newCms = new CapacityMappingStorable();
     fillByResultset(newCms, rs);
     return newCms;
   }

 }


 public String getOrderType() {
   return this.orderType;
 }


 public ArrayList<Capacity> getRequiredCapacities() {
   return this.requiredCapacities;
 }

 public DestinationKey getDestinationKey() {
   return new DestinationKey(orderType, applicationname, versionname);
 }
 
 public String getApplicationname() {
   return applicationname;
 }
 
 public String getVersionname() {
   return versionname;
 }
 
}

