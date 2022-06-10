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
package com.gip.xyna.xfmg.xfctrl.versionmgmt;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;

@Persistable(primaryKey = XMOMVersionStorable.COL_ID, tableName = XMOMVersionStorable.TABLE_NAME)
public class XMOMVersionStorable extends ClusteredStorable<XMOMVersionStorable> {

  private static final long serialVersionUID = 967503194994024766L;

  public static final String TABLE_NAME = "xmomversion";
  public static final String COL_VERSIONNAME = "versionName";
  public static final String COL_REVISION = "revision";
  public static final String COL_APPLICATION = "application";
  public static final String COL_WORKSPACE = "workspace";
  public static final String COL_ID = "id";
  
  @Column(name = COL_ID)
  private String id;
  
  @Column(name = COL_VERSIONNAME)
  private String versionName;
  
  @Column(name = COL_APPLICATION)
  private String application;

  @Column(name = COL_WORKSPACE)
  private String workspace;
  
  @Column(name = COL_REVISION)
  private Long revision;
  
  public XMOMVersionStorable(String application, String versionName, Long revision, int binding) {
    super(binding);
    this.application = application;
    this.versionName = versionName;
    this.revision = revision;
    this.id = revision + "#" + binding;
  }

  public XMOMVersionStorable(RuntimeContext runtimeContext, Long revision, int binding) {
    super(binding);
    
    if (runtimeContext instanceof Application) {
      this.application = runtimeContext.getName();
      this.versionName = ((Application)runtimeContext).getVersionName();
    }
    if (runtimeContext instanceof Workspace) {
      this.workspace = runtimeContext.getName();
    }

    this.revision = revision;
    this.id = revision + "#" + binding;
  }
  
  public XMOMVersionStorable(int binding) {
    super(binding);
  }
  
  public XMOMVersionStorable() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }
  
  private static ResultSetReader<XMOMVersionStorable> reader =  new ResultSetReader<XMOMVersionStorable>() {

    public XMOMVersionStorable read(ResultSet rs) throws SQLException {
      XMOMVersionStorable xmomversion = new XMOMVersionStorable();
      ClusteredStorable.fillByResultSet(xmomversion, rs);
      xmomversion.setVersionName(rs.getString(COL_VERSIONNAME));
      xmomversion.setRevision(rs.getLong(COL_REVISION));
      xmomversion.setApplication(rs.getString(COL_APPLICATION));
      xmomversion.setWorkspace(rs.getString(COL_WORKSPACE));
      xmomversion.id = rs.getString(COL_ID);
      return xmomversion;
    }    
  };
  
  @Override
  public ResultSetReader<? extends XMOMVersionStorable> getReader() {
    return reader;
  }

  
  public static ResultSetReader<? extends XMOMVersionStorable> getStaticReader() {
    return reader;
  }
  
  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends XMOMVersionStorable> void setAllFieldsFromData(U data) {
    super.setBinding(data.getBinding());
    XMOMVersionStorable cast = data;
    versionName = cast.versionName;
    revision = cast.revision;
    application = cast.application;
    workspace = cast.workspace;
    id = cast.id;
  }

  
  public String getVersionName() {
    return versionName;
  }

  
  public void setVersionName(String versionName) {
    this.versionName = versionName;
  }

  
  public Long getRevision() {
    return revision;
  }

  
  public void setRevision(Long revision) {
    this.revision = revision;
  }
  
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("XMOMVersion {  ");
    s.append("Revision: ").append(this.getRevision()).append(", ");
    if (this.getApplication() != null && this.getApplication().length() > 0) {
      s.append("Application: ").append(this.getApplication()).append(", ");
      s.append("VersionName : ").append(this.getVersionName()).append(", ");
    }
    if (this.getWorkspace() != null) {
      s.append("Workspace: ").append(this.getWorkspace()).append(", ");
    }
    s.append("Binding : ").append(this.getBinding());    
    s.append(" } \n");
    return s.toString();
  }

  public String getId() {
    return id;
  }
  
  public String getApplication() {
    return application;
  }

  
  public void setApplication(String application) {
    this.application = application;
  }

  
  public String getWorkspace() {
    return workspace;
  }

  
  public void setWorkspace(String workspace) {
    this.workspace = workspace;
  }

}
