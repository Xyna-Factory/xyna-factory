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
package com.gip.xyna.xprc.xprcods.orderarchive.audit;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;


public class EnhancedAudit implements Serializable {

  private static final long serialVersionUID = 1L;

  private Long version;
  private String audit;
  private String fqn;
  private RuntimeContext workflowContext;
  private List<AuditImport> imports;
  private long repositoryRevision;
  
  public EnhancedAudit(Long version, String audit, String fqn, List<AuditImport> imports, long repositoryRevision) {
    this.version = version;
    this.audit = audit;
    this.fqn = fqn;
    this.imports = imports;
    this.repositoryRevision = repositoryRevision;
  }
  
  public EnhancedAudit(Long version, String audit, String fqn, List<AuditImport> imports, long repositoryRevision, RuntimeContext context) {
    this(version, audit, fqn, imports, repositoryRevision);
    this.workflowContext = context;
  }
  
  
  public Long getVersion() {
    return version;
  }
  
  
  public String getAudit() {
    return audit;
  }
  
  
  public String getFqn() {
    return fqn;
  }
  
  
  public RuntimeContext getWorkflowContext() {
    return workflowContext;
  }
  
  
  public List<AuditImport> getImports() {
    if (imports == null) {
      return Collections.emptyList();
    } else {
      return Collections.unmodifiableList(imports);
    }
  }
  
  
  public long getRepositoryRevision() {
    return repositoryRevision;
  }
}
