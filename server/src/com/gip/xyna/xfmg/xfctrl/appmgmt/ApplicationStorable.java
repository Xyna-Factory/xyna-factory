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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = ApplicationStorable.COL_ID, tableName = ApplicationStorable.TABLE_NAME)
public class ApplicationStorable extends Storable<ApplicationStorable> {
  
  private static final long serialVersionUID = 4268531150180082132L;

  public static final String TABLE_NAME = "application";
  public static final String COL_ID = "id";
  public static final String COL_NAME = "name";
  public static final String COL_VERSION = "version";
  public static final String COL_STATE = "state";
  public final static String COL_COMMENT = "comment";
  public final static String COL_PARENT_REVISION = "parentRevision";
  public final static String COL_CREATION_DATE = "creationDate";
  public final static String COL_REMOTE_STUB = "remoteStub";
  

  @Column(name = COL_ID)
  private Long id;

  @Column(name = COL_NAME)
  private String name;

  @Column(name = COL_VERSION)
  private String version;

  @Column(name = COL_STATE)
  private String state;
  
  @Column(name = COL_COMMENT)
  private String comment;

  @Column(name = COL_PARENT_REVISION)
  private Long parentRevision;

  @Column(name = COL_CREATION_DATE)
  private long creationDate;
  
  @Column(name = COL_REMOTE_STUB)
  private boolean remoteStub;

  
  public ApplicationStorable() {
  }
  
  public ApplicationStorable(String applicationName, String version, String comment, Long parentRevision) {
    this(applicationName, version, ApplicationState.WORKINGCOPY, comment); //Zustand WORKINGCOPY im Storable wegen Abw�rtskompatibilit�t behalten (sollte aber nicht mehr ausgewertet werden)
    this.parentRevision = parentRevision;
  }
  
  public ApplicationStorable(String applicationName, String version, ApplicationState state, String comment) {
    this.state = state.toString();
    this.name = applicationName;
    this.version = version;
    id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    this.comment = comment;
    creationDate = System.currentTimeMillis();
  }
  
  private static ResultSetReader<ApplicationStorable> reader =  new ResultSetReader<ApplicationStorable>() {

    public ApplicationStorable read(ResultSet rs) throws SQLException {
      ApplicationStorable app = new ApplicationStorable();
      app.setId(rs.getLong(COL_ID));
      app.setName(rs.getString(COL_NAME));
      app.setVersion(rs.getString(COL_VERSION));
      app.setState(rs.getString(COL_STATE));
      app.setComment(rs.getString(COL_COMMENT));
      app.setParentRevision(rs.getLong(COL_PARENT_REVISION));
      app.creationDate = rs.getLong(COL_CREATION_DATE);
      app.remoteStub = rs.getBoolean(COL_REMOTE_STUB);
      return app;
    }    
  };
  
  public static ResultSetReader<? extends ApplicationStorable> getStaticReader() {
    return reader;
  }
  
  @Override
  public ResultSetReader<? extends ApplicationStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends ApplicationStorable> void setAllFieldsFromData(U data) {
    ApplicationStorable cast = data;
    id = cast.id;
    name = cast.name;
    version = cast.version;
    state = cast.state;
    comment = cast.comment;
    parentRevision = cast.parentRevision;
    creationDate = cast.creationDate;
    remoteStub = cast.remoteStub;
  }
  
  public Long getId() {
    return id;
  }
  
  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }
 
  public void setName(String name) {
    this.name = name;
  }
  
  public ApplicationState getStateAsEnum() {
    return ApplicationState.valueOf(state);
  }
  
  public String getState() {
    return state;
  }
 
  
  public void setState(ApplicationState state) {
    this.state = state.toString();
  }
  
  public void setState(String state) {
    this.state = state;
  }
  
  public String getVersion() {
    return version;
  }
  
  public void setVersion(String version) {
    this.version = version;
  }
  
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("Application {  ");
    s.append("Id : ").append(this.getId()).append(", ");
    s.append("Name : ").append(this.getName()).append(", ");
    s.append("Version: ").append(this.getVersion().toString()).append(", ");
    s.append("ApplicationState : ").append(this.getState().toString());
    s.append(" } \n");
    return s.toString();
  }

  public String getComment() {
    return comment;
  }

  
  public void setComment(String comment) {
    this.comment = comment;
  }

  
  public Long getParentRevision() {
    return parentRevision;
  }

  
  public void setParentRevision(Long parentRevision) {
    this.parentRevision = parentRevision;
  }
  
  
  public boolean isApplicationDefinition() {
    return parentRevision != null && parentRevision != 0;
  }

  
  public long getCreationDate() {
    return creationDate;
  }
  
  public void setCreationDate(long l) {
    creationDate = l;
  }
  
  
  public boolean getRemoteStub() {
    return remoteStub;
  }
  
  public void setRemoteStub(boolean remoteStub) {
    this.remoteStub = remoteStub;
  }
  
}
