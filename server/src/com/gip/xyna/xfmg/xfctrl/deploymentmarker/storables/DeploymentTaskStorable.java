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
package com.gip.xyna.xfmg.xfctrl.deploymentmarker.storables;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = DeploymentTaskStorable.COL_ID, tableName = DeploymentTaskStorable.TABLENAME)
public class DeploymentTaskStorable extends Storable<DeploymentTaskStorable>{

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "deploymenttask";

  public static final String COL_ID = "id";
  public static final String COL_DEPLOYMENT_ITEM_NAME = "deploymentItemName";
  public static final String COL_DEPLOYMENT_ITEM_TYPE = "deploymentItemType";
  public static final String COL_REVISION = "revision";
  public static final String COL_INDEX = "index";
  public static final String COL_DESCRIPTION = "description";
  public static final String COL_DONE = "done";
  public static final String COL_PRIORITY = "priority";
  
  @Column(name = COL_ID)
  private long id;
  
  @Column(name = COL_DEPLOYMENT_ITEM_NAME)
  private String deploymentItemName;
  
  @Column(name = COL_DEPLOYMENT_ITEM_TYPE)
  private String deploymentItemType;
  
  @Column(name = COL_REVISION)
  private Long revision;
  
  @Column(name = COL_INDEX)
  private int index;
  
  @Column(name = COL_DESCRIPTION)
  private String description;
  
  @Column(name = COL_DONE)
  private boolean done;
  
  @Column(name = COL_PRIORITY)
  private int priority;

  
  public DeploymentTaskStorable() {
  }

  public DeploymentTaskStorable(long id) {
    this.id = id;
  }
  

  public DeploymentTaskStorable(String deploymentItemName, XMOMType deploymentItemType, long revision,
                      int index, String description, boolean done, int priority) {
    this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    this.deploymentItemName = deploymentItemName;
    this.deploymentItemType = deploymentItemType.toString();
    this.revision = revision;
    this.index = index;
    this.description = description;
    this.done = done;
    this.priority = priority;
  }


  private static final ResultSetReader<DeploymentTaskStorable> reader = new ResultSetReader<DeploymentTaskStorable>() {

    public DeploymentTaskStorable read(ResultSet rs) throws SQLException {
      DeploymentTaskStorable task = new DeploymentTaskStorable();
      
      task.id = rs.getLong(COL_ID);
      task.deploymentItemName = rs.getString(COL_DEPLOYMENT_ITEM_NAME);
      task.deploymentItemType = rs.getString(COL_DEPLOYMENT_ITEM_TYPE);
      task.revision = rs.getLong(COL_REVISION);
      task.index = rs.getInt(COL_INDEX);
      task.description = rs.getString(COL_DESCRIPTION);
      task.done = rs.getBoolean(COL_DONE);
      task.priority = rs.getInt(COL_PRIORITY);
      
      return task;
    }

  };

  @Override
  public ResultSetReader<? extends DeploymentTaskStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends DeploymentTaskStorable> void setAllFieldsFromData(U data) {
    DeploymentTaskStorable cast = data;
    this.id = cast.id;
    this.deploymentItemName = cast.deploymentItemName;
    this.deploymentItemType = cast.deploymentItemType;
    this.revision = cast.revision;
    this.index = cast.index;
    this.description = cast.description;
    this.done = cast.done;
    this.priority = cast.priority;
  }

  
  public long getId() {
    return id;
  }

  
  public void setId(long id) {
    this.id = id;
  }

  
  public String getDeploymentItemName() {
    return deploymentItemName;
  }

  
  public void setDeploymentItemName(String deploymentItemName) {
    this.deploymentItemName = deploymentItemName;
  }

  
  public String getDeploymentItemType() {
    return deploymentItemType;
  }

  
  public void setDeploymentItemType(String deploymentItemType) {
    this.deploymentItemType = deploymentItemType;
  }

  
  public Long getRevision() {
    return revision;
  }

  
  public void setRevision(Long revision) {
    this.revision = revision;
  }

  
  public int getIndex() {
    return index;
  }

  
  public void setIndex(int index) {
    this.index = index;
  }

  
  public String getDescription() {
    return description;
  }

  
  public void setDescription(String description) {
    this.description = description;
  }

  
  public boolean isDone() {
    return done;
  }

  
  public void setDone(boolean done) {
    this.done = done;
  }

  
  public int getPriority() {
    return priority;
  }

  
  public void setPriority(int priority) {
    this.priority = priority;
  }

}
