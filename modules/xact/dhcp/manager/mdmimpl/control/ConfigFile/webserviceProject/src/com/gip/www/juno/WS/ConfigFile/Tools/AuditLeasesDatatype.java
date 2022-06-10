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

public class AuditLeasesDatatype {

  private String host;
  private String ip;
  //private long ipNum;
  private String starttime;
  private String endtime;
  private long duration;
  private String type;
  private String remoteId;
  private String dppInstance;

  
  public AuditLeasesDatatype(String host, String ip, String starttime, String endtime, long duration, String type, String remoteId, String dppInstance)
  {
    this.setHost(host);
    this.setIp(ip);
    this.setStarttime(starttime);
    this.setEndtime(endtime);
    this.setDuration(duration);
    this.setType(type);
    this.setRemoteId(remoteId);
    this.setDppInstance(dppInstance);
  }


  public String getHost() {
    return host;
  }


  public void setHost(String host) {
    this.host = host;
  }


  public String getIp() {
    return ip;
  }


  public void setIp(String ip) {
    this.ip = ip;
  }


  public String getStarttime() {
    return starttime;
  }


  public void setStarttime(String starttime) {
    this.starttime = starttime;
  }


  public String getEndtime() {
    return endtime;
  }


  public void setEndtime(String endtime) {
    this.endtime = endtime;
  }


  public long getDuration() {
    return duration;
  }


  public void setDuration(long duration) {
    this.duration = duration;
  }


  public String getType() {
    return type;
  }


  public void setType(String type) {
    this.type = type;
  }


  public String getRemoteId() {
    return remoteId;
  }


  public void setRemoteId(String remoteId) {
    this.remoteId = remoteId;
  }


  public String getDppInstance() {
    return dppInstance;
  }


  public void setDppInstance(String dppInstance) {
    this.dppInstance = dppInstance;
  }


  
  
}
