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
package com.gip.xyna.xact;

import xact.http.enums.statuscode.Accepted;
import xact.http.enums.statuscode.BadRequest;
import xact.http.enums.statuscode.Created;
import xact.http.enums.statuscode.Forbidden;
import xact.http.enums.statuscode.GatewayTimeout;
import xact.http.enums.statuscode.HTTPStatusCode;
import xact.http.enums.statuscode.InternalServerError;
import xact.http.enums.statuscode.LengthRequired;
import xact.http.enums.statuscode.MethodNotAllowed;
import xact.http.enums.statuscode.NoContent;
import xact.http.enums.statuscode.NotAcceptable;
import xact.http.enums.statuscode.NotFound;
import xact.http.enums.statuscode.NotImplemented;
import xact.http.enums.statuscode.OK;
import xact.http.enums.statuscode.TemporaryRedirect;
import xact.http.enums.statuscode.Unauthorized;


public enum StatusCode {

  OK(200, "OK", OK.class),
  Created(201, "Created", Created.class),
  Accepted(202, "Accepted", Accepted.class),
  NoContent(204, "No Content", NoContent.class),
  TemporaryRedirect(307, "Temporary Redirect", TemporaryRedirect.class),
  BadRequest(400, "Bad Request", BadRequest.class),
  Unauthorized(401, "Unauthorized", Unauthorized.class),
  Forbidden(403, "Forbidden", Forbidden.class),
  NotFound(404, "Not Found", NotFound.class),
  MethodNotAllowed(405, "Method Not Allowed", MethodNotAllowed.class),
  NotAcceptable(406, "Not Acceptable", NotAcceptable.class),
  LengthRequired(411, "Length Required", LengthRequired.class),
  InternalServerError(500, "Internal Server Error", InternalServerError.class),
  NotImplemented(501, "Not Implemented", NotImplemented.class),
  GatewayTimeout(504, "Gateway Timeout", GatewayTimeout.class)
  ;//FIXME weitere
  
  
  private int code;
  private String name;
  private String reason;
  private Class<? extends HTTPStatusCode> statusClass;

  private StatusCode(int code, String name, Class<? extends HTTPStatusCode> statusClass) {
    this.code = code;
    this.name = name;
    this.reason = code +" " + name;
    this.statusClass =statusClass;
  }
  
  public int getCode() {
    return code;
  }
  
  public String getName() {
    return name;
  }
  
  public String getReason() {
    return reason;
  }
  
  public static HTTPStatusCode newInstanceFor(int code, String reason) {
    for( StatusCode sc : values() ) {
      if( sc.code == code ) {
        return sc.newInstance(reason);
      }
    }
    return new HTTPStatusCode(code,reason);
  }
  
  private HTTPStatusCode newInstance(String reason) {
    try {
      HTTPStatusCode sc = statusClass.newInstance();
      sc.unversionedSetCode(code);
      sc.unversionedSetReason(reason);
      return sc;
    } catch (Exception e) { //InstantiationException, IllegalAccessException
      throw new RuntimeException(e); //sollte nicht auftreten
    }
  }

  public static StatusCode parse(HTTPStatusCode hsc) {
    if( hsc == null ) {
      return OK;
    }
    Class<? extends HTTPStatusCode> statusClass = hsc.getClass();
    for( StatusCode sc : values() ) {
      if( sc.statusClass.isAssignableFrom(statusClass) ) {
        return sc;
      }
    }
    return null;
  }

}
