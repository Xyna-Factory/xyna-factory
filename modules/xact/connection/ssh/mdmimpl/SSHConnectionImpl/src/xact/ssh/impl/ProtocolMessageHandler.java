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
package xact.ssh.impl;



import java.io.Serializable;



/**
 * Verzweigung Store/Ignore sehr fr�h, um unn�tige Arbeit zu sparen und 
 * um v�llig ohne ProtocolMessagesStore auskommen zu k�nnen.
 *
 */
public abstract class ProtocolMessageHandler implements Serializable {

  private static final long serialVersionUID = 1L;

  private static final ProtocolMessageHandlerStore STORE = new ProtocolMessageHandlerStore();


  public static ProtocolMessageHandler newInstance() {
    return STORE;
  }


  public abstract void handleProtocol(SSHConnectionInstanceOperationImpl sshConnectionInstanceOperationImpl, String content, String type,
                                      boolean commandSent, long recieveTime);


}
