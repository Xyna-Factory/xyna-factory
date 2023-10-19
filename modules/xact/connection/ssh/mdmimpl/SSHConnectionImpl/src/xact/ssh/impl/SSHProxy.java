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
// TODO: SSHProxy
/*
package xact.ssh.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.jcraft.jsch.ChannelDirectTCPIP;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SocketFactory;


public class SSHProxy implements Proxy {
  
  private static Logger logger = CentralFactoryLogging.getLogger(SSHProxy.class);
  
  private Session session;
  private InputStream inStream;
  private OutputStream outStream;
  private ChannelDirectTCPIP channel;
  
  public SSHProxy(Session session) {
    this.session = session;
  }

  public void close() {
    logger.info("SSHProxy.close called");
    try {
      if (channel != null) {
        channel.disconnect();
      }
    } finally {
      if (session != null) {
        session.disconnect();
      }
    }
  }

  public void connect(SocketFactory ignore, String host,
                      int port, int timeout) throws Exception {
    channel = (ChannelDirectTCPIP)session.openChannel("direct-tcpip");
    channel.setHost(host);
    channel.setPort(port);
    // important: first create the streams, then connect.
    inStream = channel.getInputStream();
    outStream = channel.getOutputStream();
    channel.connect();
  }

  public InputStream getInputStream() {
    return inStream;
  }

  public OutputStream getOutputStream() {
    return outStream;
  }

  public Socket getSocket() {
    return null; //gibt es nicht
  }

}
*/