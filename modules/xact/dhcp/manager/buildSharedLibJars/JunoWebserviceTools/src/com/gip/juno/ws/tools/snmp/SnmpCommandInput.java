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


package com.gip.juno.ws.tools.snmp;


public class SnmpCommandInput {

  protected String ip = null;
  protected String port = "161";
  protected int snmpTimeout = 10000;
  protected int socketTimeout = 10000;
  protected boolean logThreads = false;
  protected String communityRead = null;
  protected String communityReadWrite = null;

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public int getSnmpTimeout() {
    return snmpTimeout;
  }

  public void setSnmpTimeout(int timeout) {
    this.snmpTimeout = timeout;
  }

  public boolean getLogThreads() {
    return logThreads;
  }

  public void setLogThreads(boolean logThreads) {
    this.logThreads = logThreads;
  }

  public int getSocketTimeout() {
    return socketTimeout;
  }

  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  public String getCommunityRead() {
    return communityRead;
  }

  public void setCommunityRead(String communityRead) {
    this.communityRead = communityRead;
  }

  public String getCommunityReadWrite() {
    return communityReadWrite;
  }

  public void setCommunityReadWrite(String communityReadWrite) {
    this.communityReadWrite = communityReadWrite;
  }

}
