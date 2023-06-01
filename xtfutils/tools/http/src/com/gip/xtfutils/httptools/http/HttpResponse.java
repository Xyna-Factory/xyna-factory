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


package com.gip.xtfutils.httptools.http;


public class HttpResponse {

  public static int RESPONSE_CODE_FOR_OK = 200;

  private int responseCode = -1;
  private String responseCodeMessage;

  /**
   * actual payload of http response
   */
  private String responsePayload;


  public int getResponseCode() {
    return responseCode;
  }


  public boolean isResponseCodeOK() {
    return (responseCode == RESPONSE_CODE_FOR_OK);
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }


  public String getResponseCodeMessage() {
    return responseCodeMessage;
  }


  public void setResponseCodeMessage(String responseCodeMessage) {
    this.responseCodeMessage = responseCodeMessage;
  }


  public String getResponsePayload() {
    return responsePayload;
  }


  public void setResponsePayload(String responsePayload) {
    this.responsePayload = responsePayload;
  }


  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("HttpResponse { \n");
    s.append("   responseCode: ").append(responseCode).append(",\n");
    s.append("   responseCodeMessage: ").append(responseCodeMessage).append(",\n");
    s.append("   responsePayload: ").append(responsePayload).append("\n}");
    return s.toString();
  }

}
