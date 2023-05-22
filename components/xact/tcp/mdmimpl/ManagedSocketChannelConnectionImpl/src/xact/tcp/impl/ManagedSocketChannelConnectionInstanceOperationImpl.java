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
package xact.tcp.impl;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ClassNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import xact.connection.Command;
import xact.connection.CommandResponseTuple;
import xact.connection.ConnectionAlreadyClosed;
import xact.connection.DeviceType;
import xact.connection.SendParameter;
import xact.templates.DocumentType;
import xprc.synchronization.CorrelationId;
import xact.tcp.ManagedSocketChannelConnectionParameter;
import xact.tcp.ManagedSocketChannelConnectionSuperProxy;
import xact.tcp.ManagedSocketChannelConnectionInstanceOperation;
import xact.tcp.ManagedSocketChannelConnection;
import xact.tcp.ManagedSocketChannelSendParameter;
import xact.tcp.SocketChannelManagement;

/*
 * Written and tested with CONTEX-Usecase in mind:
 * Single user that is in most cases sync waiting for a response
 */
public class ManagedSocketChannelConnectionInstanceOperationImpl extends ManagedSocketChannelConnectionSuperProxy implements ManagedSocketChannelConnectionInstanceOperation {

  private static final long serialVersionUID = 1L;

  public ManagedSocketChannelConnectionInstanceOperationImpl(ManagedSocketChannelConnection instanceVar) {
    super(instanceVar);
  }

  /*public void addResponses(CommandResponseTuple response) {
    // Implemented as code snippet!
  }

  public SendException buildException(List<? extends Command> commands, DetectedError detectedError) {
    // Implemented as code snippet!
    return null;
  }

  public void clearResponses() {
    // Implemented as code snippet!
  }*/

  public void connect() {
    //TODO implementation
    //TODO update dependency XML
    //TODO ntbd? 
  }

  public void disconnect() {
    //TODO implementation
    //TODO update dependency XML
    //TODO ntbd?
  }

  public CommandResponseTuple send(Command command, DocumentType documentType, DeviceType deviceType, SendParameter sendParameter) {
    // Implemented as workflow!
    return null;
  }

  public void sendAsync(DocumentType documentType, DeviceType deviceType, Command command, ManagedSocketChannelSendParameter sendParameter) throws ConnectionAlreadyClosed {
    ManagedSocketChannelConnectionParameter conParams = (ManagedSocketChannelConnectionParameter) this.instanceVar.getConnectionParameter();
    String socketChannelName = conParams.getHostname() + ":" + conParams.getPort();
    writeToChannel(socketChannelName, conParams.getCharsetName(), command);
  }

  public CorrelationId sendSync(Command command, DeviceType deviceType, DocumentType documentType, ManagedSocketChannelSendParameter sendParameter) throws ConnectionAlreadyClosed {
    ManagedSocketChannelConnectionParameter conParams = (ManagedSocketChannelConnectionParameter) this.instanceVar.getConnectionParameter();
    String socketChannelName = conParams.getHostname() + ":" + conParams.getPort();
    writeToChannel(socketChannelName, conParams.getCharsetName(), command);
    if (sendParameter.getCorrelationId() == null || sendParameter.getCorrelationId().length() <= 0) {
      return new CorrelationId(socketChannelName);
    } else {
      return new CorrelationId(sendParameter.getCorrelationId());
    }
  }
  
  
  private void writeToChannel(String socketChannelName, String charsetName, Command command) throws ConnectionAlreadyClosed {
    SocketChannel channel = SocketChannelManagement.getInstance().getSocketChannel(socketChannelName);
    if (channel.isOpen() && channel.isConnected()) {
      try {
        channel.write(ByteBuffer.wrap(command.getContent().getBytes(charsetName)));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new ConnectionAlreadyClosed(command);
    }
  }

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }

}
