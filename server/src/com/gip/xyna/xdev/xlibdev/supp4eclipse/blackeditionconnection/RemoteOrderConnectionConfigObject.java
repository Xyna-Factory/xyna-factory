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

package com.gip.xyna.xdev.xlibdev.supp4eclipse.blackeditionconnection;


public class RemoteOrderConnectionConfigObject {

  public String username;
  public String password;
  public String remoteHost;
  public int remotePort;
  public String sessionId;

  public final boolean useEncryption;
  public final String keyStorePath;
  public final String keyStoreType;
  public final String keyStorePassword;


  public RemoteOrderConnectionConfigObject(String username, String password, String remoteHost, int remotePort,
                                           String sessionId, boolean useEncryption, String keyStorePath,
                                           String keyStoreType, String keyStorePassword) {
    this.username = username;
    this.password = password;
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
    this.sessionId = sessionId;

    this.useEncryption = useEncryption;
    this.keyStorePath = keyStorePath;
    this.keyStoreType = keyStoreType;
    this.keyStorePassword = keyStorePassword;

  }


  public RemoteOrderConnectionConfigObject(String username, String password, String remoteHost, int remotePort,
                                           boolean useEncryption, String keyStorePath, String keyStoreType,
                                           String keyStorePassword) {
    this.username = username;
    this.password = password;
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;

    this.useEncryption = useEncryption;
    this.keyStorePath = keyStorePath;
    this.keyStoreType = keyStoreType;
    this.keyStorePassword = keyStorePassword;

  }


  public RemoteOrderConnectionConfigObject(String username, String password, String remoteHost, int remotePort,
                                           String sessionId) {
    this(username, password, remoteHost, remotePort, sessionId, false, null, null, null);
  }


  public RemoteOrderConnectionConfigObject(String username, String password, String remoteHost, int remotePort) {
    this(username, password, remoteHost, remotePort, null);
  }

}
