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
package com.gip.xyna.utils.soap.serializer;

public class SoapFault {

  private String faultCode;
  private String faultString;
  private String detail;
  private String faultActor;

  public SoapFault(String faultCode, String faultString, String detail, String faultActor) {
    this.faultCode = faultCode;
    this.faultString = faultString;
    this.detail = detail;
    this.faultActor = faultActor;
  }
  
  public String getFaultCode() {
    return faultCode;
  }
  
  public String getFaultString() {
    return faultString;
  }
  
  public String getDetail() {
    return detail;
  }
  
  public String getFaultActor() {
    return faultActor;
  }
}
