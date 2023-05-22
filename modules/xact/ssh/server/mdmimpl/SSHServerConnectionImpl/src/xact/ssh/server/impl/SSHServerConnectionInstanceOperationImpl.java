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


import java.io.IOException;

import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;
import com.gip.xyna.xprc.xsched.xynaobjects.RelativeDate;

import xact.connection.Command;
import xact.connection.Response;
import xact.ssh.server.SSHServerConnection;
import xact.ssh.server.SSHServerConnectionInstanceOperation;
import xact.ssh.server.SSHServerConnectionSuperProxy;
import xact.ssh.server.SSHSessionStore;
import xact.ssh.server.SSHSessionStore.SSHConnection;
import xact.templates.Document;


public class SSHServerConnectionInstanceOperationImpl extends SSHServerConnectionSuperProxy implements SSHServerConnectionInstanceOperation {

  private static final long serialVersionUID = 1L;
  private transient SSHConnection sshCon;

  public SSHServerConnectionInstanceOperationImpl(SSHServerConnection instanceVar) {
    super(instanceVar);
  }

  private void connect() {
    if( sshCon == null ) {
      sshCon = SSHSessionStore.getSSHConnection( getInstanceVar().getSessionId() );
    }
  }

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
    connect();
  }

  @Override
  public void send(Command command) {
    connect();
    try {
      sshCon.send(command.getContent());
    } catch ( IOException e ) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendDocument(Document document) {
    connect();
    try {
      sshCon.send(document.getText());
    } catch ( IOException e ) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void sendLine(Command command) {
    connect();
    try {
      sshCon.sendLine(command.getContent());
    } catch ( IOException e ) {
      throw new RuntimeException(e);
    }
  }



  @Override
  public Response readAllUntilTimeout(RelativeDate timeout) {
    connect();
    try {
      AbsRelTime art = timeout.toAbsRelTime();
      String response = sshCon.readAll(art.getAbsoluteTime());
      
      return new Response(response);
    } catch ( IOException e ) {
      throw new RuntimeException(e);
    }
  }



  @Override
  public Response readLine(RelativeDate timeout) {
    connect();
    try {
      AbsRelTime art = timeout.toAbsRelTime();
      String response = sshCon.readLine(art.getAbsoluteTime());
      return new Response(response);
    } catch ( IOException e ) {
      throw new RuntimeException(e);
    }
  }



}
