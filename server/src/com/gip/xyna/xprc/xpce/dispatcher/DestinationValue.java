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
package com.gip.xyna.xprc.xpce.dispatcher;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;



public abstract class DestinationValue implements Serializable {

  private static final long serialVersionUID = 1L;
  private final String fqName;
  private int poolId;
  @Deprecated // could be used as cache
  private final Long revision;
  private transient int hash;

  public DestinationValue(String fqName) {
    this.fqName = fqName;
    this.revision = -1L;
  }


  public String getFQName() {
    return fqName;
  }


  public void setPoolId(int poolId) {
    this.poolId = poolId;
  }


  public int getPoolId() {
    return poolId;
  }


  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (obj.getClass() != getClass())) {
      return false;
    }
    if (obj.hashCode() != hashCode()) {
      return false;
    }
    DestinationValue test = (DestinationValue) obj;
    if (fqName == null) {
      return test.fqName == null;
    }
    return fqName.equals(test.getFQName());
  }


  public int hashCode() {
    if (hash == 0) {
      hash = fqName.hashCode() ^ revision.hashCode();
    }
    return hash;
  }


  public abstract ExecutionType getDestinationType();


  public abstract boolean isPoolable();

  
  public Long resolveRevision(DestinationKey dk) {
    try {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = revMgmt.getRevision(dk.getRuntimeContext());
      return resolveRevision(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return -1L;
    }
  }
  
  
  public Long resolveRevision(Long revision) {
    RuntimeContextDependencyManagement rtCtxDepMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    return rtCtxDepMgmt.getRevisionDefiningXMOMObjectOrParent(getOriginalFqName(getFQName(), revision), revision);
  }
  
  
  public Set<Long> resolveAllRevisions(DestinationKey dk) {
    try {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      Long revision = revMgmt.getRevision(dk.getRuntimeContext());
      return resolveAllRevisions(revision);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return Collections.emptySet();
    }
  }
  
  
  public Set<Long> resolveAllRevisions(Long revision) {
    RuntimeContextDependencyManagement rtCtxDepMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    Set<Long> allRevisions = rtCtxDepMgmt.getAllRevisionsDefiningXMOMObject(getOriginalFqName(getFQName(), revision), revision);
    if (allRevisions.size() <= 0) {
      allRevisions.add(revision);
    }
    return allRevisions;
  }


  public String getOriginalFqName(String fqClassName, Long revision) {
    WorkflowDatabase wdb = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();
    String originalFqName = wdb.getXmlName(fqClassName, revision);

    if (originalFqName == null){
      originalFqName = fqClassName; //Wf ist nicht deployed -> fqClassName beibehalten
    }
    
    return originalFqName;
  }
  

}
