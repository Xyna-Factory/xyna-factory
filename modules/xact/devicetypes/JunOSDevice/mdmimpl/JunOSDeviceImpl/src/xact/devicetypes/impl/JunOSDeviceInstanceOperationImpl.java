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
package xact.devicetypes.impl;


import org.apache.log4j.Logger;

import xact.connection.Command;
import xact.connection.CommandResponseTuple;
import xact.connection.DetectedError;
import xact.connection.ManagedConnection;
import xact.devicetypes.JunOSDevice;
import xact.devicetypes.JunOSDeviceInstanceOperation;
import xact.devicetypes.JunOSDeviceSuperProxy;
import xact.ssh.ErrorConditions;
import xact.ssh.SSHTools;
import xact.templates.DocumentType;

import com.gip.xyna.XynaFactory;

public class JunOSDeviceInstanceOperationImpl extends JunOSDeviceSuperProxy implements JunOSDeviceInstanceOperation {

  private static final long serialVersionUID = 1L;
  private static String errorConditionsProperty = "xact.devicetypes.JunOSDevice.errorConditions";
  private static ErrorConditions errorConditions = new ErrorConditions("");
  private static String errorIndicator = "syntax error,";
  
  private static Logger logger = Logger.getLogger(JunOSDeviceInstanceOperationImpl.class);
  
  public JunOSDeviceInstanceOperationImpl(JunOSDevice instanceVar) {
    super(instanceVar);
  }

  static {
      String path = XynaFactory.getInstance().getXynaMultiChannelPortal().getProperty(errorConditionsProperty);
      errorConditions = new ErrorConditions(path);
  }


  public Boolean checkInteraction(CommandResponseTuple response, DocumentType documentType) {
    //TODO implementation
    //TODO update dependency XML
    return false;
  }


  public void cleanupAfterError(CommandResponseTuple response, DocumentType documentType, ManagedConnection managedConnection) {
    //TODO implementation
    //TODO update dependency XML
  }


  public void detectCriticalError(CommandResponseTuple response, DocumentType documentType) throws DetectedError {
      if(logger.isDebugEnabled()) {
          logger.debug("### detectCriticalError: Command: " + response.getCommand().getContent() + ", Response: '" + response.getResponse().getContent() + "'");
      }
    boolean result = SSHTools.checkForError(response.getResponse().getContent(), errorConditions, errorIndicator);
    if (result) {
        throw new DetectedError(response.getCommand(), response.getResponse());
    }
  }

  public Command enrichCommand(Command command) {
      String content = command.getContent();

      if (content.contains("[[")) {
          for (int i=0; i < 33; i++) {
              content = content.replaceAll("\\[\\[" + i + "\\]\\]", String.valueOf((char) i));
          }
      }
      Command retVal = new Command(content);

      return retVal;
  } 
  
  
  public Boolean isResponseComplete(String response, DocumentType documentType, ManagedConnection mannagedConnection, Command command) {
      if(logger.isDebugEnabled()) {
          logger.debug("### isResponseComplete: Command: '" + command.getContent() + "', Response: '" + response +"'");
      }
      
      return super.isResponseComplete(response, documentType, mannagedConnection, command);
  }



  public CommandResponseTuple removeDeviceSpecifics(CommandResponseTuple response) {   
    return response;
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
