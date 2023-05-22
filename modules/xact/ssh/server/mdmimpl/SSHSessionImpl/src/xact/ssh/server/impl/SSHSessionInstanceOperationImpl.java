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
package xact.ssh.server.impl;


import xact.ssh.server.SSHHostKey;
import xact.ssh.server.SSHServerConnection;
import xact.ssh.server.SSHSession;
import xact.ssh.server.SSHSessionInstanceOperation;
import xact.ssh.server.SSHSessionStore;
import xact.ssh.server.SSHSessionStore.SSHConnection;
import xact.ssh.server.SSHSessionSuperProxy;


public class SSHSessionInstanceOperationImpl extends SSHSessionSuperProxy implements SSHSessionInstanceOperation {

  private static final long serialVersionUID = 1L;

  public SSHSessionInstanceOperationImpl(SSHSession instanceVar) {
    
    super(instanceVar);
  }

  @Override
  public SSHServerConnection getSSHConnection() {
    SSHServerConnection con = new SSHServerConnection(getInstanceVar().getUniqueId());
    return con;
  }

  @Override
  public SSHHostKey getSSHHostKey() {
    SSHConnection sshCon = SSHSessionStore.getSSHConnection( getInstanceVar().getUniqueId() );
    return new SSHHostKey(sshCon.getHostKey());
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
