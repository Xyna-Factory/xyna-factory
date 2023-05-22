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
package xact.ssh.impl;

import org.apache.log4j.Logger;

import xact.ssh.SSHConnectionParameter;
import xfmg.xfmon.protocolmsg.ProtocolMessage;
import xfmg.xfmon.protocolmsg.ProtocolMessageStore;
import xfmg.xfmon.protocolmsg.StoreParameter;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xpce.OrderContext;


public class ProtocolMessageHandlerStore extends ProtocolMessageHandler {
  
  private static final long serialVersionUID = 1L;

  private static Logger logger = CentralFactoryLogging.getLogger(ProtocolMessageHandlerStore.class);

  @Override
  public void handleProtocol(SSHConnectionInstanceOperationImpl sshConnectionInstanceOperationImpl, 
                             String content, String communicationDirection, boolean commandSent, long recieveTime) {
    ProtocolMessage protocolMessage = sshConnectionInstanceOperationImpl.createPartialProtocolMessage(content);
    if (protocolMessage == null) {
      return;
    }
    protocolMessage.setCommunicationDirection(communicationDirection);
    protocolMessage.setMessageType(commandSent ? "Communication" : "Login" );
    protocolMessage.setTime(recieveTime);
    
    fillPartialMessage(sshConnectionInstanceOperationImpl, protocolMessage);
    tryToSetDataFromOrderContext(protocolMessage);
    
    storeProtocolMessage(protocolMessage);
   
  }
  
  private void fillPartialMessage(SSHConnectionInstanceOperationImpl sshConnectionInstanceOperationImpl,
                                  ProtocolMessage protocolMessage) {
    SSHConnectionParameter connectionParams = sshConnectionInstanceOperationImpl.getSSHConnectionParameter();
    protocolMessage.setConnectionId(String.valueOf(sshConnectionInstanceOperationImpl.transientDataId));
    // TODO use session.setSocketFactory to control the local interface
    //partialMessage.setLocalAddress("");
    protocolMessage.setPartnerAddress(connectionParams.getHost() + ":" + (connectionParams.getPort() == null ? "22" : String.valueOf(connectionParams.getPort())));
  }  
  
  private void storeProtocolMessage(ProtocolMessage protocolMessage) {
    StoreParameter storeParams = new StoreParameter();
    try {
      ProtocolMessageStore.store(protocolMessage, storeParams);
    } catch (XynaException e) {
      logger.debug("Failed to store protocol message",e);
    }
  }
  
  
  private void tryToSetDataFromOrderContext(ProtocolMessage protocolMessage) {
    try {
      OrderContext ctx = XynaProcessing.getOrderContext();
      protocolMessage.setOriginId(String.valueOf(ctx.getOrderId()));
      protocolMessage.setRootOrderId(ctx.getRootOrderContext().getOrderId());
    } catch (IllegalStateException e) {
      logger.debug("Could not retrieve order ids for protocol message",e);
    }
  }


  
}
