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
package com.gip.www.juno.WS.ConfigFile.Tools;

public class AuditDhcpPacketDatatype {

  private String host;
  private String ip;
  private String inTime;
  private String request;
  private String reply;

  
  public AuditDhcpPacketDatatype(String host, String ip, String inTime, String request, String reply)
  {
    this.setHost(host);
    this.setIp(ip);
    this.setInTime(inTime);
    this.setRequest(request);
    this.setReply(reply);
  }


  public void setHost(String host) {
    this.host = host;
  }


  public String getHost() {
    return host;
  }


  public void setIp(String ip) {
    this.ip = ip;
  }


  public String getIp() {
    return ip;
  }


  public void setInTime(String inTime) {
    this.inTime = inTime;
  }


  public String getInTime() {
    return inTime;
  }


  public void setRequest(String request) {
    this.request = request;
  }


  public String getRequest() {
    return request;
  }


  public void setReply(String reply) {
    this.reply = reply;
  }


  public String getReply() {
    return reply;
  }
  
  
}
