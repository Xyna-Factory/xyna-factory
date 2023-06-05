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

import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

/**
 * Antwort-Objekt der XMOMGui-Auftrufe
 *
 */
public class XMOMGuiReply {
  
  public enum Status {
    success(HTTPTriggerConnection.HTTP_OK),
    failed(HTTPTriggerConnection.HTTP_INTERNALERROR),
    forbidden(HTTPTriggerConnection.HTTP_FORBIDDEN),
    notfound(HTTPTriggerConnection.HTTP_NOTFOUND),
    badRequest(HTTPTriggerConnection.HTTP_BADREQUEST),
    unauthorized(HTTPTriggerConnection.HTTP_UNAUTHORIZED),
    conflict(HTTPTriggerConnection.HTTP_CONFLICT),
    policyNotFulfilled("420 Policy Not Fulfilled")
    ;
    
    private String httpStatus;

    private Status(String httpStatus) {
      this.httpStatus = httpStatus;
    }
    
    public String getHttpStatus() {
      return httpStatus;
    }
  }
  
  private GeneralXynaObject xynaObject;

  private String json;
  private Status status;
  
  public XMOMGuiReply() {
    status = Status.success;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Use xynaObject instead
   * @param json
   */
  @Deprecated
  public void setJson(String json) {
    this.json = json;
  }

  public String getJson() {
    if(xynaObject != null) {
      return Utils.xoToJson(xynaObject);
    }
    return json;
  }
  
  public GeneralXynaObject getXynaObject() {
    return xynaObject;
  }

  
  public void setXynaObject(GeneralXynaObject xynaObject) {
    this.xynaObject = xynaObject;
  }

  public String getHttpStatus() {
    return status.getHttpStatus();
  }

  public static XMOMGuiReply fail(Status status, Exception exception) {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.status = status;
    reply.xynaObject = Utils.error(exception);
    return reply;
  }

  public static XMOMGuiReply fail(Status status, String message, Exception exception) {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.status = status;
    reply.xynaObject = Utils.error(message, exception);
    return reply;
  }
  
  public static XMOMGuiReply fail(Status status, String message) {
    XMOMGuiReply reply = new XMOMGuiReply();
    reply.status = status;
    reply.xynaObject = Utils.error(message);
    return reply;
  }
  
}
