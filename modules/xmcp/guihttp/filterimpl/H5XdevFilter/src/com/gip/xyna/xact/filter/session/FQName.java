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
package com.gip.xyna.xact.filter.session;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

/**
 * vollqualifizierter Typname:
 * a) Revision/RuntimeContext
 * b) Pfad
 * c) Name 
 * 
 * sowie die File-Location
 */
public class FQName {

  public enum XmomVersion { SAVED, DEPLOYED }


  private String location;
  private Long revision;
  private Long definingRevision;
  private String fqName;
  private RuntimeContext runtimeContext = null;
  private RuntimeContext definingRuntimeContext = null;
  private XmomVersion xmomVersion = XmomVersion.SAVED;

  private static final RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
  
  public FQName(Long revision, String fqName) throws XFMG_NoSuchRevision {
    this.revision = revision;
    this.fqName = fqName;
    this.location = GenerationBase.getStorageLocation(fqName, revision, true, false);
    try {
      runtimeContext = getRevisionManagement().getRuntimeContext(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_NoSuchRevision(String.valueOf(revision));
    }
  }
  
  public FQName(RuntimeContext runtimeContext, String fqName) throws XFMG_NoSuchRevision {
    this.runtimeContext = runtimeContext;
    this.fqName = fqName;
    try {
      this.revision = getRevisionManagement().getRevision(runtimeContext);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new XFMG_NoSuchRevision(runtimeContext.getGUIRepresentation());
    }
    this.location = GenerationBase.getStorageLocation(fqName, revision, true, false);
  }

  public FQName(Long revision, RuntimeContext runtimeContext, String path, String name) {
    this(revision, runtimeContext, path, name, XmomVersion.SAVED);
  }

  public FQName(Long revision, RuntimeContext runtimeContext, String path, String name, XmomVersion xmomVersion) {
    this.revision = revision;
    this.runtimeContext = runtimeContext;
    int idx = name.indexOf('.');
    if( idx < 0 ) {
      this.fqName = path+"."+name;
    } else {
      this.fqName = path+"."+name.substring(0, idx);
    }
    this.location = GenerationBase.getStorageLocation(fqName, revision, true, true);
    this.xmomVersion = xmomVersion;
  }
  
  public FQName() {}

  @Override
  public String toString() {
    String fqNameStr = "FQName(" + revision + "," + runtimeContext + "," + fqName;

    if (xmomVersion == XmomVersion.DEPLOYED) {
      fqNameStr += "," + XmomVersion.DEPLOYED.name();
    }

    fqNameStr += ")";

    return fqNameStr;
  }
  
  private static RevisionManagement revisionManagement;
  private static RevisionManagement getRevisionManagement() {
    if (revisionManagement == null) {
      RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();  
      if (XynaFactory.getInstance().isStartingUp()) {
        return rm;
      }
      revisionManagement = rm;
    } 
    return revisionManagement;
  }
  
  
  public String getLocation() {
    return location;
  }
  
  public String getFqName() {
    return fqName;
  }
  
  public Long getRevision() {
    return revision;
  }

  public Long getDefiningRevision() {
    if (definingRevision == null) {
      definingRevision = rcdm.getRevisionDefiningXMOMObjectOrParent(fqName, revision);
    }

    return definingRevision;
  }

  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }

  public RuntimeContext getDefiningRuntimeContext() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    if (definingRuntimeContext == null) {
      definingRuntimeContext = getRevisionManagement().getRuntimeContext(getDefiningRevision());
    }

    return definingRuntimeContext;
  }

  public XmomVersion getXmomVersion() {
    return xmomVersion;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((location == null) ? 0 : location.hashCode());

    if (xmomVersion == XmomVersion.DEPLOYED) {
      result *= -1;
    }

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;

    FQName other = (FQName) obj;
    if (location == null) {
      if (other.location != null) {
        return false;
      }
    } else if (!location.equals(other.location)) {
      return false;
    }

    if (!xmomVersion.equals(other.xmomVersion)) {
      return false;
    }

    return true;
  }
  
  
  
  
}
