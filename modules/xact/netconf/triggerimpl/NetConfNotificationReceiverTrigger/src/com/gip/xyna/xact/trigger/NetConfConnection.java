/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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


import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.security.Security;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.trigger.NetConfNotificationReceiverCredentials.AuthMethodName;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.method.AuthMethod;
import xact.ssh.HostKeyAliasMapping;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import xact.ssh.SSHJReflection;


public class NetConfConnection {

  public static enum HostKeyAuthMode {
    direct, none
  }
  
  private static Logger logger = CentralFactoryLogging.getLogger(NetConfNotificationReceiverTriggerConnection.class);

  private Socket socket;
  private String socket_host;
  private int socket_port;

  private SSHClient ssh;
  private Session session;
  private Session.Subsystem channel;

  private final String username;
  private final String password;
  private final String connectionID;
  private final HostKeyAuthMode hostKeyAuthenticationMode;

  private InputStream inputStream;
  private OutputStream outputStream;


  public NetConfConnection(String ConnectionID, String username, String password, HostKeyAuthMode hostKeyAuthenticationMode,
                           ConnectionList connlist) {
    this.connectionID = ConnectionID;
    this.username = username;
    this.password = password;
    this.hostKeyAuthenticationMode = hostKeyAuthenticationMode;
    try {
      Socket newSocket = connlist.getSocket(this.connectionID);
      this.socket = newSocket;
      this.socket_host = socket.getInetAddress().toString().replace("/", "");
      this.socket_port = socket.getPort();

      if (!this.socket.isConnected()) {
        throw new RuntimeException("NetConfConnection: Socket is not connected");
      }

      if (logger.isDebugEnabled()) {
        logger.debug("NetConfNotificationReceiver: Host: " + this.socket_host + ", Port: " + this.socket_port);
      }
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: Initialization of NetConfConnection failed", t);
    }
  }


  public void openNetConfConnection(BasicCredentials cred) throws Throwable {
    Security.addProvider(new BouncyCastleProvider());
    NetConfNotificationReceiverCredentials credentialsSSHJ = new NetConfNotificationReceiverCredentials(cred);
    SSHClient client = null;

    AuthMethodName method = AuthMethodName.DEFAULT;
    String hostkeyAlias = socket_host;

    if (this.password.isEmpty()) {
      HostKeyAliasMapping.injectHostname(socket_host, hostkeyAlias, false);
      credentialsSSHJ.injectHostKeyHash(socket_host, hostkeyAlias);
      if (logger.isDebugEnabled()) {
        logger.debug("NetConfNotificationReceiver: injectHostname " + socket_host + " " + HostKeyAliasMapping.convertHostname(socket_host));
      }
      client = credentialsSSHJ.initSSHClient();
      method = AuthMethodName.PUBLICKEY;
    } else {
      client = new SSHClient();
      method = AuthMethodName.PASSWORD;
    }
    
    if (hostKeyAuthenticationMode == HostKeyAuthMode.none) {
      client.addHostKeyVerifier(new PromiscuousVerifier());
    }

    SSHJReflection.recall(client, socket);
    Collection<AuthMethod> aMethod = credentialsSSHJ.convertAuthMethod(method, socket_host, socket_port);
    client.auth(username, aMethod);

    Session session = client.startSession();
    Session.Subsystem channel = session.startSubsystem("netconf");

    this.inputStream = channel.getInputStream();
    this.outputStream = channel.getOutputStream();

    this.ssh = client;
    this.session = session;
    this.channel = channel;
  };


  public int read() {
    int intread = -1;
    try {
      intread = this.inputStream.read();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: " + "NetConfRead failed", t);
    }
    return intread;
  }


  public void send(String message) {
    try {
      PrintStream printstream = new PrintStream(this.outputStream);
      printstream.write(message.getBytes("UTF-8"));
      printstream.flush();

    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: " + "NetConfSend failed", t);
    }
  }


  public void closeNetconfConnection() {
    try {
      this.channel.close();
      this.session.close();
      this.ssh.close();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: " + "Close NetConfConnection failed", t);
    }
  }


  public void closeSocket() {
    try {
      this.socket.close();
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: " + "Close Socket failed", t);
    }
  }


  public String getIP() {
    String IP = "";
    try {
      IP = this.socket.getInetAddress().toString().replace("/", "");
    } catch (Throwable t) {
      logger.warn("NetConfNotificationReceiver: " + "Conversion of IP failed", t);
    }
    return IP;
  }

}
