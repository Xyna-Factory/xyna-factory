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


public class AuditV6Datatype {
  private String host;
  private String ip;
  private String inTime;
  private String solicit;
  private String advertise;

  
  public AuditV6Datatype(String host, String ip, String inTime, String solicit, String advertise)
  {
    this.setHost(host);
    this.setIp(ip);
    this.setInTime(inTime);
    this.setSolicit(solicit);
    this.setAdvertise(advertise);
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


  public void setSolicit(String solicit) {
    this.solicit = solicit;
  }


  public String getSolicit() {
    return solicit;
  }


  public void setAdvertise(String advertise) {
    this.advertise = advertise;
  }


  public String getAdvertise() {
    return advertise;
  }
  
  
  
  
}
