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
package com.gip.xyna.xact.trigger;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = "id", tableName = TriggerConfigurationStorable.TABLE_NAME)
public class TriggerConfigurationStorable extends Storable<TriggerConfigurationStorable> {

  private static final long serialVersionUID = 6286369646193743075L;

  public static final String TABLE_NAME = "triggerconfiguration";
  
  @Column(name = "id", size = 50)
  private String id;
  
  @Column(name = "triggerInstanceName", size = 50)
  private String triggerInstanceName;

  @Column(name = "maxReceives")
  private long maxReceives;

  @Column(name = "autoReject")
  private boolean autoReject;
  
  @Column(name = "revision")
  private Long revision;
  


  public TriggerConfigurationStorable(String triggerInstanceName, Long revision) {
    this.triggerInstanceName = triggerInstanceName;
    this.revision = revision;
    this.id = generateId(triggerInstanceName, revision);
  }

  public TriggerConfigurationStorable() {

  }

  public TriggerConfigurationStorable(String triggerInstanceName, long maxNumberEvents, boolean autoReject, Long revision) {
    this.triggerInstanceName = triggerInstanceName;
    this.maxReceives = maxNumberEvents;
    this.autoReject = autoReject;
    this.revision = revision;
    this.id = generateId(triggerInstanceName, revision);
  }

  
  private String generateId(String key, Long revision) {
    return key + "#" + revision;
  }  

  public boolean getAutoReject() {
    return autoReject;
  }


  public long getMaxReceives() {
    return maxReceives;
  }


  public String getTriggerInstanceName() {
    return triggerInstanceName;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }

  
  public String getId() {
    return id;
  }


  private static ResultSetReader<TriggerConfigurationStorable> reader = new ResultSetReader<TriggerConfigurationStorable>() {

    public TriggerConfigurationStorable read(ResultSet rs) throws SQLException {
      String triggerInstanceName = rs.getString("triggerInstanceName");
      long maxReceives = rs.getLong("maxReceives");
      boolean autoReject = rs.getBoolean("autoReject");
      long revision = rs.getLong("revision");
      if(revision == 0 && rs.wasNull()) {
        revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
      }
      TriggerConfigurationStorable ret = new TriggerConfigurationStorable(triggerInstanceName, maxReceives,
                                                                          autoReject, revision);
      return ret;
    }

  };


  @Override
  public ResultSetReader<? extends TriggerConfigurationStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends TriggerConfigurationStorable> void setAllFieldsFromData(U data) {
    TriggerConfigurationStorable cast = data;
    triggerInstanceName = cast.triggerInstanceName;
    maxReceives = cast.maxReceives;
    autoReject = cast.autoReject;
    revision = cast.revision;
    id = cast.id;
  }

  
  public Long getRevision() {
    return revision;
  }

}
