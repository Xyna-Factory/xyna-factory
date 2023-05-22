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
package com.gip.xyna.xfmg.xfctrl.dependencies;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.DataModel;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext.RuntimeContextType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = RuntimeContextDependencyStorable.COL_ID, tableName = RuntimeContextDependencyStorable.TABLENAME)
public class RuntimeContextDependencyStorable extends Storable<RuntimeContextDependencyStorable>{

  private static final long serialVersionUID = 1L;

  public static final String TABLENAME = "runtimecontextdependency";

  public static final String COL_ID = "id";
  public static final String COL_TYPE = "type";
  public static final String COL_NAME = "name";
  public static final String COL_ADDITION = "addition";
  public static final String COL_DEP_TYPE = "depType";
  public static final String COL_DEP_NAME = "depName";
  public static final String COL_DEP_ADDITION = "depAddition";
  
  public static final ResultSetReader<RuntimeContextDependencyStorable> reader = new RuntimeContextDependencyStorableReader();

  @Column(name = COL_ID)
  private long id;
  
  @Column(name = COL_TYPE)
  private String type;
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_ADDITION)
  private String addition;

  @Column(name = COL_DEP_TYPE)
  private String depType;
  
  @Column(name = COL_DEP_NAME)
  private String depName;
  
  @Column(name = COL_DEP_ADDITION)
  private String depAddition;
  
  

  
  public RuntimeContextDependencyStorable() {
  }

  public RuntimeContextDependencyStorable(RuntimeDependencyContext owner, RuntimeDependencyContext dependency) {
    this.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    this.type = owner.getRuntimeDependencyContextType().name();
    this.name = owner.getName();
    if (owner instanceof Application) {
      this.addition = ((Application) owner).getVersionName();
    }
    if (owner instanceof ApplicationDefinition) {
      this.addition = ((ApplicationDefinition) owner).getParentWorkspace().getName();
    }
    this.depType = dependency.getRuntimeDependencyContextType().name();
    this.depName = dependency.getName();
    if (dependency instanceof Application) {
      this.depAddition = ((Application) dependency).getVersionName();
    }
    if (dependency instanceof ApplicationDefinition) {
      this.depAddition = ((ApplicationDefinition) dependency).getParentWorkspace().getName();
    }
  }
  
  @Override
  public ResultSetReader<? extends RuntimeContextDependencyStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }

  @Override
  public <U extends RuntimeContextDependencyStorable> void setAllFieldsFromData(U data) {
    RuntimeContextDependencyStorable cast = data;
    this.id = cast.id;
    this.type = cast.type;
    this.name = cast.name;
    this.addition = cast.addition;
    this.depType = cast.depType;
    this.depName = cast.depName;
    this.depAddition = cast.depAddition;
  }

  private static void fillByResultset(RuntimeContextDependencyStorable rcd, ResultSet rs) throws SQLException {
    rcd.id = rs.getLong(COL_ID);
    rcd.type = rs.getString(COL_TYPE);
    rcd.name = rs.getString(COL_NAME);
    rcd.addition = rs.getString(COL_ADDITION);
    rcd.depType = rs.getString(COL_DEP_TYPE);
    rcd.depName = rs.getString(COL_DEP_NAME);
    rcd.depAddition = rs.getString(COL_DEP_ADDITION);
  }

  private static class RuntimeContextDependencyStorableReader implements ResultSetReader<RuntimeContextDependencyStorable> {

    public RuntimeContextDependencyStorable read(ResultSet rs) throws SQLException {
      RuntimeContextDependencyStorable result = new RuntimeContextDependencyStorable();
      fillByResultset(result, rs);
      return result;
    }

  }

  
  public long getId() {
    return id;
  }

  
  public void setId(long id) {
    this.id = id;
  }

  
  public String getType() {
    return type;
  }

  public RuntimeDependencyContextType getTypeAsEnum() {
    return RuntimeDependencyContextType.valueOf(type);
  }

  
  public void setType(String type) {
    this.type = type;
  }

  
  public String getName() {
    return name;
  }

  
  public void setName(String name) {
    this.name = name;
  }

  
  public String getAddition() {
    return addition;
  }

  
  public void setAddition(String addition) {
    this.addition = addition;
  }

  
  public String getDepType() {
    return depType;
  }

  public RuntimeDependencyContextType getDepTypeAsEnum() {
    return RuntimeDependencyContextType.valueOf(depType);
  }
  
  public void setDepType(String depType) {
    this.depType = depType;
  }

  
  public String getDepName() {
    return depName;
  }

  
  public void setDepName(String depName) {
    this.depName = depName;
  }

  
  public String getDepAddition() {
    return depAddition;
  }

  
  public void setDepAddition(String depAddition) {
    this.depAddition = depAddition;
  }

  public RuntimeDependencyContext getOwner() {
    switch(getTypeAsEnum()) {
      case Application: 
        return new Application(name, addition);
      case ApplicationDefinition: 
        return new ApplicationDefinition(name, new Workspace(addition));
      case Workspace: 
        return new Workspace(name);
      default:
        return null;
    }
  }

  public RuntimeDependencyContext getDependency() {
    switch(getDepTypeAsEnum()) {
      case Application: 
        return new Application(depName, depAddition);
      case ApplicationDefinition: 
        return new ApplicationDefinition(depName, new Workspace(depAddition));
      case Workspace: 
        return new Workspace(depName);
      default:
        return null;
    }
  }
}
