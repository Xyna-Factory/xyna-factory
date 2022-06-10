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

@Persistable(primaryKey = DeploymentTagStorable.COL_ID, tableName = DeploymentTagStorable.TABLENAME)
public class DeploymentTagStorable extends Storable<DeploymentTagStorable>{

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "deploymenttag";

  public static final String COL_ID = "id";
  public static final String COL_DEPLOYMENT_ITEM_NAME = "deploymentItemName";
  public static final String COL_DEPLOYMENT_ITEM_TYPE = "deploymentItemType";
  public static final String COL_REVISION = "revision";
  public static final String COL_INDEX = "index";
  public static final String COL_LABEL = "label";
  
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
  
  @Column(name = COL_LABEL)
  private String label;
  
  
  public DeploymentTagStorable() {
  }

  public DeploymentTagStorable(long id) {
    this.id = id;
  }
  

  public DeploymentTagStorable(String deploymentItemName, XMOMType deploymentItemType, long revision,
                    int index, String label) {
    this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    this.deploymentItemName = deploymentItemName;
    this.deploymentItemType = deploymentItemType.toString();
    this.revision = revision;
    this.index = index;
    this.label = label;
  }


  private static final ResultSetReader<DeploymentTagStorable> reader = new ResultSetReader<DeploymentTagStorable>() {

    public DeploymentTagStorable read(ResultSet rs) throws SQLException {
      DeploymentTagStorable tag = new DeploymentTagStorable();
      
      tag.id = rs.getLong(COL_ID);
      tag.deploymentItemName = rs.getString(COL_DEPLOYMENT_ITEM_NAME);
      tag.deploymentItemType = rs.getString(COL_DEPLOYMENT_ITEM_TYPE);
      tag.revision = rs.getLong(COL_REVISION);
      tag.index = rs.getInt(COL_INDEX);
      tag.label = rs.getString(COL_LABEL);
      
      return tag;
    }

  };

  @Override
  public ResultSetReader<? extends DeploymentTagStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends DeploymentTagStorable> void setAllFieldsFromData(U data) {
    DeploymentTagStorable cast = data;
    this.id = cast.id;
    this.deploymentItemName = cast.deploymentItemName;
    this.deploymentItemType = cast.deploymentItemType;
    this.revision = cast.revision;
    this.index = cast.index;
    this.label = cast.label;
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

  
  public String getLabel() {
    return label;
  }

  
  public void setLabel(String label) {
    this.label = label;
  }

}
