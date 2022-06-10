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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;


public class WebSphereMQConnectData extends QueueConnectData {

  private static final long serialVersionUID = 1L;

  private String queueManager;
  private String hostname;
  private int port;
  //private String queueName;
  private String channel;


  public WebSphereMQConnectData() {
  }



  public String getQueueManager() {
    return queueManager;
  }



  public void setQueueManager(String queueManager) {
    this.queueManager = queueManager;
  }



  public String getHostname() {
    return hostname;
  }



  public void setHostname(String hostname) {
    this.hostname = hostname;
  }



  public int getPort() {
    return port;
  }



  public void setPort(int port) {
    this.port = port;
  }


  public void setPort(String port) {
    int val = Integer.parseInt(port);
    this.port = val;
  }


  public String getChannel() {
    return channel;
  }



  public void setChannel(String channel) {
    this.channel = channel;
  }


  public String toString() {
    StringBuilder s = new StringBuilder(" WebSphereMQConnectData { ");
    s.append("queueManager: ").append(this.getQueueManager());
    s.append(", hostname: ").append(this.getHostname());
    s.append(", port: ").append(this.getPort());
    s.append(", channel: ").append(this.getChannel());
    s.append(" } ");
    return s.toString();
  }

}
