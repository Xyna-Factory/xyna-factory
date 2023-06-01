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
package com.gip.xyna.xfmg.xfctrl.xmomdatabase.search;

import java.io.Serializable;
import java.util.Objects;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;


public class XMOMDatabaseSearchResultEntry implements Serializable {
  
  private static final long serialVersionUID = 8735906276721778374L;
  
  private String label;
  private String fqName;
  private String simplepath;
  private String simplename;
  private XMOMDatabaseType type;
  private int weigth;
  private RuntimeContext runtimeContext;
  
  
  public XMOMDatabaseSearchResultEntry(String fqName, XMOMDatabaseType type, int weigth) {
    this.fqName = fqName;
    this.type = type;
    this.weigth = weigth;
  }

  
  public <E extends XMOMDatabaseEntry> XMOMDatabaseSearchResultEntry(E entry, int weigth) {
    this(entry.getFqname(), entry.getXMOMDatabaseType(), weigth);
    this.simplename = entry.getName();
    this.simplepath = entry.getPath();
  }

  
  public XMOMDatabaseSearchResultEntry(String fqName, String simplename, String simplepath, XMOMDatabaseType type, int weigth) {
    this(fqName, type, weigth);
    this.simplename = simplename;
    this.simplepath = simplepath;
  }
  
  public String getLabel() {
    return label;
  }
  public void setLabel(String label) {
    this.label = label;
  }
  
  public String getFqName() {
    return fqName;
  }


  
  public void setFqName(String fqName) {
    this.fqName = fqName;
  }


  
  public XMOMDatabaseType getType() {
    return type;
  }


  
  public void setType(XMOMDatabaseType type) {
    this.type = type;
  }


  
  public int getWeigth() {
    return weigth;
  }


  
  public void setWeigth(int weigth) {
    this.weigth = weigth;
  }
  
  
  public String getSimplename() {
    return simplename;
  }

  
  public void setSimplename(String simplename) {
    this.simplename = simplename;
  }
  
  
  public String getSimplepath() {
    return simplepath;
  }


  
  public void setSimplePath(String simplepath) {
    this.simplepath = simplepath;
  }


  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }

  
  public void setRuntimeContext(RuntimeContext rtCtx) {
    this.runtimeContext = rtCtx;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof XMOMDatabaseSearchResultEntry)) {
      return false;
    }
    return Objects.equals(this.fqName, ((XMOMDatabaseSearchResultEntry)obj).fqName) &&
           Objects.equals(this.runtimeContext, ((XMOMDatabaseSearchResultEntry)obj).runtimeContext); 
  }


  @Override
  public int hashCode() {
    return Objects.hash(fqName, runtimeContext);
  }


 

}
