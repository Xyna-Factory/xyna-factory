/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter;



public class McpLegacySession {

  private String sessionId;
  private boolean initialized;
  private String clientInfoName;
  private String clientInfoVersion;
  private String protocolVersion;


  public McpLegacySession(String sessionId, boolean initialized, String clientInfoName, String clientInfoVersion, String protocolVersion) {
    this.sessionId = sessionId;
    this.initialized = initialized;
    this.clientInfoName = clientInfoName;
    this.clientInfoVersion = clientInfoVersion;
    this.protocolVersion = protocolVersion;
  }


  public String getSessionId() {
    return sessionId;
  }


  public boolean isInitialized() {
    return initialized;
  }


  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }


  public String getClientInfoName() {
    return clientInfoName;
  }


  public void setClientInfoName(String clientInfoName) {
    this.clientInfoName = clientInfoName;
  }


  public String getClientInfoVersion() {
    return clientInfoVersion;
  }


  public void setClientInfoVersion(String clientInfoVersion) {
    this.clientInfoVersion = clientInfoVersion;
  }


  public String getProtocolVersion() {
    return protocolVersion;
  }
}
