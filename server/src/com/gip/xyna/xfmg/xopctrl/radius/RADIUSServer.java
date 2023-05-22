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
package com.gip.xyna.xfmg.xopctrl.radius;

import java.io.Serializable;

import com.gip.xyna.XMOM.base.IP;


public class RADIUSServer implements Serializable {

  private static final long serialVersionUID = -2739407417543253221L;

  private IP ip;
  
  private RADIUSServerPort port;
  
  private PresharedKey presharedKey;

  
  public RADIUSServer() {
    super();
  }
  
  
  public RADIUSServer(IP ip, RADIUSServerPort port, PresharedKey presharedKey) {
    this();
    this.ip = ip;
    this.port = port;
    this.presharedKey = presharedKey;
  }
  
  
  public IP getIp() {
    return ip;
  }

  
  public void setIp(IP ip) {
    this.ip = ip;
  }

  
  public RADIUSServerPort getPort() {
    return port;
  }

  
  public void setPort(RADIUSServerPort port) {
    this.port = port;
  }

  
  public PresharedKey getPresharedKey() {
    return presharedKey;
  }

  
  public void setPresharedKey(PresharedKey presharedKey) {
    this.presharedKey = presharedKey;
  }
  
}
