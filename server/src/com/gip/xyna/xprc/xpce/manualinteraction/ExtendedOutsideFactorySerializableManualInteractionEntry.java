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
package com.gip.xyna.xprc.xpce.manualinteraction;

import java.io.Serializable;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class ExtendedOutsideFactorySerializableManualInteractionEntry implements Serializable {

  private static final long serialVersionUID = 7683852259535365569L;
  
  private final long id;
  private final String reason;
  private final String type;
  private final String userGroup;
  private final String todo;
  private final String allowedResponses;
  private int priority = 0;
  private int monitoringCode = 0;
  private String ordertype = "";
  private String sessionId = "";
  private long parentId = 0;
  private final String application;
  private final String version;
  private final String workspace;

  public ExtendedOutsideFactorySerializableManualInteractionEntry(ManualInteractionEntry mie) {
    id = mie.getID();
    reason = mie.getReason();
    type = mie.getType();
    userGroup = mie.getUserGroup();
    todo = mie.getTodo();
    allowedResponses = mie.getAllowedResponses();
    if (mie.getXynaOrderId() != null) {
      priority = mie.getXynaOrderPriority();
      monitoringCode = mie.getXynaOrderMonitoringLevel();
      sessionId = mie.getXynaOrderSessionId();
      if (mie.getParentOrderId() != null) {
        parentId = mie.getParentOrderId();
        ordertype = mie.getParentOrderType();
      }
    }
    RuntimeContext rc;
    try {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(mie.getRevision());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    if (rc instanceof Workspace) {
      workspace = rc.getName();
      application = null;
      version = null;
    } else if (rc instanceof Application) {
      workspace = null;
      application = rc.getName();
      version = ((Application) rc).getVersionName();
    } else {
      throw new RuntimeException();
    }
  }
  
  
  public long getId() {
    return id;
  }
  
  public String getReason() {
    return reason;
  }
  
  public String getType() {
    return type;
  }
  
  public String getUserGroup() {
    return userGroup;
  }
  
  public String getTodo() {
    return todo;
  }
  
  public int getPriority() {
    return priority;
  }
  
  public int getMonitoringCode() {
    return monitoringCode;
  }
  
  public String getOrdertype() {
    return ordertype;
  }
  
  public String getSessionId() {
    return sessionId;
  }
  
  public long getParentId() {
    return parentId;
  }
  
  public String getAllowedResponses() {
    return allowedResponses;
  }

  public String getApplication() {
    return application;
  }

  public String getVersion() {
    return version;
  }

  public String getWorkspace() {
    return workspace;
  }
    
}
