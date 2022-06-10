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
package com.gip.xyna.xact.trigger;

import java.net.InetSocketAddress;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.session.ServerSession;

public class SSHConnectionParameter {

  private String user;
  private String localHost;
  private int localPort;
  private String remoteHost;
  private int remotePort;
  private String uniqueId;
  
  public String getUser() {
    return user;
  }

  public String getLocalHost() {
    return localHost;
  }

  public int getLocalPort() {
    return localPort;
  }

  public String getRemoteHost() {
    return remoteHost;
  }

  public int getRemotePort() {
    return remotePort;
  }
  
  public String getUniqueId() {
    return uniqueId;
  }

  public static class Builder {
    
  }

  public static SSHConnectionParameter build(ServerSession session, Environment environment) {
    SSHConnectionParameter scp = new SSHConnectionParameter();
    
    scp.user = session.getUsername();
    
    InetSocketAddress la = (java.net.InetSocketAddress)session.getIoSession().getLocalAddress();
    InetSocketAddress ra = (java.net.InetSocketAddress)session.getIoSession().getRemoteAddress();
    
    scp.localPort = la.getPort();
    scp.remotePort = ra.getPort();
    
    scp.localHost = la.getAddress().getHostAddress();
    scp.remoteHost = ra.getAddress().getHostAddress();
    
    scp.uniqueId = scp.user+"_"+ 
      scp.remoteHost+":"+scp.remotePort+"_"+
      scp.localHost+":"+scp.localPort+"_"+
      System.currentTimeMillis();
    
    return scp;
  }
  
}
