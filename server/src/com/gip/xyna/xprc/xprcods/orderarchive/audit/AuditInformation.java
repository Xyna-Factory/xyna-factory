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
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;


public class AuditInformation implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private final EnhancedAudit audit;
  private final RuntimeContext runtimeContext;
  private final ExecutionType type;
  private String servicepath;
  private String servicename;
  private String serviceoperation;
  
  public AuditInformation(EnhancedAudit audit, RuntimeContext runtimeContext, ExecutionType type) {
    this.audit = audit;
    this.runtimeContext = runtimeContext;
    this.type = type;
  }
  
  public void setServiceDestinationInfo(String servicepath, String servicename, String serviceoperation) {
    this.servicename = servicename;
    this.servicepath = servicepath;
    this.serviceoperation = serviceoperation;
  }
  
  public ExecutionType getType() {
    return type;
  }
  
  public String getServiceName() {
    return servicename;
  }
  
  public String getServicePath() {
    return servicepath;
  }
  
  public String getServiceOperation() {
    return serviceoperation;
  }

  public EnhancedAudit getAudit() {
    return audit;
  }
  
  public RuntimeContext getRuntimeContext() {
    return runtimeContext;
  }
  
  
}
