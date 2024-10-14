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
package com.gip.xyna.xact.trigger;

import java.nio.channels.SocketChannel;

import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import org.apache.log4j.Logger;

import xact.tcp.SocketChannelManagement;

public class ManagedSocketChannelTriggerConnection extends TriggerConnection {

  private static Logger logger = CentralFactoryLogging.getLogger(ManagedSocketChannelTriggerConnection.class);

  private final String socketMgmtName;
  private final byte[] message;
  
  public ManagedSocketChannelTriggerConnection(String socketMgmtName, byte[] message) {
    this.socketMgmtName = socketMgmtName;
    this.message = message;
  }
  
  
  public byte[] getMessage() {
    return message;
  }
  
  
  public String getSocketManagementName() {
    return socketMgmtName;
  }
  
  
  public SocketChannel getManagedSocketChannel() {
    return SocketChannelManagement.getInstance().getSocketChannel(socketMgmtName);
  }
  

}
