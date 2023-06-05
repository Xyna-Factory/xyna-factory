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
package com.gip.xyna.xprc.xprcods.orderarchive.audit;

import java.io.Serializable;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;


public class AuditImport implements Serializable {
  
  private static final long serialVersionUID = 1L;

  private String document;
  private RuntimeContext runtimeContext;
  private String fqn;
  
  public AuditImport(String document, RuntimeContext runtimeContext) {
    this(document, runtimeContext, null);
  }
  
  public AuditImport(String document, RuntimeContext runtimeContext, String fqn) {
    this.document = document;
    this.runtimeContext = runtimeContext;
    this.fqn = fqn;
  }
  
  public String getDocument() {
    return document;
  }
  
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }
  
  
  public String getFqn() {
    return fqn;
  }
}
