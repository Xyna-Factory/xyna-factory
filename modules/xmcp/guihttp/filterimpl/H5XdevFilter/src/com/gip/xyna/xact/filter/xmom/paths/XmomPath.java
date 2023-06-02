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
package com.gip.xyna.xact.filter.xmom.paths;

import java.util.Collection;
import java.util.TreeMap;

public class XmomPath {

  private String name;
  private String department;
  private boolean absolute;
  private String path;
  private TreeMap<String,XmomPath> children = new TreeMap<String,XmomPath>();
  private boolean containsFiles;

  public void setName(String name) {
    this.name = name;
  }
  
  public void merge(XmomPath xp) {
    for( XmomPath child : xp.getChildren() ) {
      XmomPath own = children.get(child.getName());
      if( own == null ) {
        addChild(child);
      } else {
        own.merge(child);
      }
    }
    if( xp.containsFiles() ) {
      containsFiles = true;
    }
  }
  
  public void addChild(XmomPath child) {
    children.put(child.getName(),child);
  }
  public String getName() {
    
    
    return name;
  }
  public void setDepartment(String department) {
    this.department = department;
  }
  public String getDepartment() {
    return department;
  }
  public void setAbsolute(boolean absolute) {
    this.absolute = absolute;
  }
  public boolean isAbsolute() {
    return absolute;
  }

  public void setPath(String path) {
    this.path = path;
  }
  public String getPath() {
    return path;
  }
  
  public Collection<XmomPath> getChildren() {
    return children.values();
  }

  public void setContainsFiles(boolean containsFiles) {
    this.containsFiles = containsFiles;
  }
  public boolean containsFiles() {
    return containsFiles;
  }

}