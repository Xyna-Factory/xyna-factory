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

package com.gip.xyna.update.outdatedclasses_5_1_4_3;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;


@Persistable(primaryKey= MonitoringDispatcherStorable.COL_ID, tableName = MonitoringDispatcherStorable.TABLENAME)
public class MonitoringDispatcherStorable extends Storable<MonitoringDispatcherStorable> {


  private static final long serialVersionUID = 7051229027026949381L;
  
  public static final String TABLENAME = "monitoringdispatcher";
  public static final String COL_ID = "id"; 
  public static final String COL_ORDERTYPE = "orderType";
  public static final String COL_COMPENSATE = "compensate";
  public static final String COL_MONITORINGLEVEL = "monitoringlevel";
  public static final String COL_APPLICATIONNAME = "applicationname";
  public static final String COL_VERSIONNAME = "versionname";

  @Column(name = COL_ID)
  private Long id;
  
  @Column(name = COL_ORDERTYPE)
  private String orderType;
  
  @Column(name = COL_COMPENSATE)
  private Boolean compensate;
  
  @Column(name = COL_MONITORINGLEVEL)
  private Integer monitoringlevel;
  
  @Column(name = COL_APPLICATIONNAME)
  private String applicationname;
  
  @Column(name = COL_VERSIONNAME)
  private String versionname;
  
  public MonitoringDispatcherStorable() {
  }
  
  public MonitoringDispatcherStorable(DestinationKey destinationKey, Integer monitoringlevel) {
    this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    this.orderType = destinationKey.getOrderType();
    this.compensate = destinationKey.isCompensate();
    this.monitoringlevel = monitoringlevel;
    this.applicationname = destinationKey.getApplicationName();
    this.versionname = destinationKey.getVersionName();
  }
  
  public MonitoringDispatcherStorable(DestinationKey destinationKey, Boolean compensate, Integer monitoringlevel, Long id) {
    if(XynaFactory.getInstance().finishedInitialization()) {
      throw new RuntimeException("Unallowed call of constructor.");
    }
    this.id = id;
    this.orderType = destinationKey.getOrderType();
    this.compensate = compensate;
    this.monitoringlevel = monitoringlevel;
    this.applicationname = destinationKey.getApplicationName();
    this.versionname = destinationKey.getVersionName();
  }
  
  public String getOrderType() {
    return orderType;
  }

  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }

  public Boolean getCompensate() {
    return compensate;
  }

  public void setCompensate(Boolean compensate) {
    this.compensate = compensate;
  }

  public Integer getMonitoringlevel() {
    return monitoringlevel;
  }

  public void setMonitoringlevel(Integer monitoringlevel) {
    this.monitoringlevel = monitoringlevel;
  }

  @Override
  public ResultSetReader<? extends MonitoringDispatcherStorable> getReader() {
    return new ResultSetReader<MonitoringDispatcherStorable>() {

      public MonitoringDispatcherStorable read(ResultSet rs)
          throws SQLException {
        MonitoringDispatcherStorable result = new MonitoringDispatcherStorable();
        result.id = rs.getLong(COL_ID);
        result.applicationname = rs.getString(COL_APPLICATIONNAME);
        result.versionname = rs.getString(COL_VERSIONNAME);
        result.compensate = rs.getBoolean(COL_COMPENSATE);
        result.monitoringlevel = rs.getInt(COL_MONITORINGLEVEL);
        result.orderType = rs.getString(COL_ORDERTYPE);
        return result;
      }
    };
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  
  public Long getId() {
    return id;
  }

  
  public String getApplicationname() {
    return applicationname;
  }

  
  public String getVersionname() {
    return versionname;
  }

  @Override
  public <U extends MonitoringDispatcherStorable> void setAllFieldsFromData(U data) {
    MonitoringDispatcherStorable cast = data;
    orderType = cast.orderType;
    compensate = cast.compensate;
    monitoringlevel = cast.monitoringlevel;
    id = cast.id;
    applicationname = cast.applicationname;
    versionname = cast.versionname;
  }
  
  public DestinationKey getDestinationKey() {
    return new DestinationKey(orderType, applicationname, versionname);
  }
  
}
