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

import java.io.IOException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.trigger.SSHStartParameter.ErrorHandling;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;

public class SSHTriggerConnection extends TriggerConnection {
  
  private static final long serialVersionUID = 1L;
  
  private static final Logger logger = CentralFactoryLogging.getLogger(SSHTriggerConnection.class);

  
  public enum RequestType {
    Init, Exec, Close;
  }

  private RequestType requestType;
  private transient ShellCommand shellCommand;

  private boolean closeConnection = true;
  
  public SSHTriggerConnection(ShellCommand shellCommand, RequestType requestType) {
    this.shellCommand = shellCommand;
    this.requestType = requestType;
  }
  
  public RequestType getRequestType() {
    return requestType;
  }

  public SSHConnectionParameter getConnectionParameter() {
    return shellCommand.getConnectionParameter();
  }

  public String getOrderType() {
    return shellCommand.getStartParameter().getOrderTypes().get(requestType);
  }

  public void sendLineQuietly(String msg) {
    try {
      shellCommand.sendLine(msg);
    } catch( IOException e ) {
      logger.warn( "Could not sendLine ", e);
    }
  }
  
  public void sendQuietly(String msg) {
    try {
      shellCommand.send(msg);
    } catch( IOException e ) {
      logger.warn( "Could not sendLine ", e);
    }
  }
  
  
  public void sendLine(String msg) throws IOException {
    shellCommand.sendLine(msg);
  }
  
  public void send(String msg) throws IOException {
    shellCommand.send(msg);
  }
  
  public synchronized void close() {
    try {
      if( closeConnection ) {
        if( requestType == RequestType.Close ) {
          //bei RequestType Close ist ShellCommand bereits geschlossen
        } else {
          shellCommand.close();
        }
      }
    } finally {
      super.close();
    }
  }
  
  public String readLine() throws IOException {
    return shellCommand.readLine();
  }

  public void nextRequest() {
    if( !shellCommand.isClosed() ) {
      closeConnection = false;
      shellCommand.nextRequest();
    }
  }

  public void customize(SSHCustomizationParameter customization) {
    shellCommand.customize(customization);
  }
  
  public SSHCustomizationParameter getCustomization() {
    return shellCommand.getCustomization();
  }

  public boolean isConnectionClosed() {
    return shellCommand.isClosed();
  }

  public ErrorHandling getErrorHandling() {
    return shellCommand.getStartParameter().getErrorHandling();
  }
 
}
