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
package com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem;


public enum DeploymentItemColumn {
  
  FQNAME("name"),
  LABEL("label"),
  TYPE("type"),
  APPLICATION("application"),
  VERSION("version"),
  WORKSPACE("workspace"),
  DOCUMENTATION("documentation"),
  STATE("state"),
  LASTSTATECHANGE("statedetail.laststatechange"),
  LASTSTATECHANGEBY("statedetail.laststatechangeby"),
  LASTMODIFIED("statedetail.lastmodified"),
  LASTMODIFIEDBY("statedetail.lastmodifiedby"),
  RESOLUTION("statedetail.resolution"),
  ROLLBACKCAUSE("statedetail.rollbackcause"),
  ROLLBACKEXCEPTION("statedetail.rollbackexception"),
  BUILDEXCEPTION("statedetail.buildexception"),
  ROLLBACKOCCURRED("rollbackoccurred"),
  BUILDEXCEPTIONOCCURRED("buildexceptionoccurred"),
  MARKER("marker"),
  TAG("tag"),
  TASK("task"),
  TASKCOUNT("taskcount"),
  DEPENDENCY("dependency"),
  CREATION_HINT("statedetail.resolution.creationhint");
  
  
  private String columnName;
  
  private DeploymentItemColumn(String columnName) {
    this.columnName = columnName;
  }
  
  public String getColumnName() {
    return columnName;
  }
  
  public static DeploymentItemColumn getColumnByName(String columnName) {
    for (DeploymentItemColumn type : values()) {
      if (type.columnName.equals(columnName)) {
        return type;
      }
    }
    throw new IllegalArgumentException(columnName);
  }
  
}
