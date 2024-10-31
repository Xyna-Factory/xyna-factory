/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.Channel;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.Transport;
import net.schmizz.sshj.transport.TransportException;

import org.apache.log4j.Logger;
import com.gip.xyna.CentralFactoryLogging;

public class TransientConnectionData {

  private static final Logger logger = CentralFactoryLogging.getLogger(TransientConnectionData.class);

  private Transport transport;
  private Session session;
  private Channel channel;
  private InputStream inputStream;
  private OutputStream outputStream;

  public void setSession(Session session) {
    this.session = session;
  }

  public void setTransport(Transport transport) {
    this.transport = transport;
  }

  public void setChannelAndStreams(Channel channel) throws IOException {
    this.channel = channel;
    this.outputStream = channel.getOutputStream();
    this.inputStream = channel.getInputStream();
  }

  public Session getSession() {
    return session;
  }

  public Channel getChannel() {
    return channel;
  }

  public OutputStream getOutputStream() {
    return outputStream;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public void disconnect() throws TransportException, ConnectionException {
    try {
      if (channel != null) {
        channel.close();
        if (logger.isDebugEnabled())
          logger.debug("closed channel");
      }
    } finally {
      channel = null;
      try {
        if (session != null) {
          session.close();
          if (logger.isDebugEnabled())
            logger.debug("closed session");
        }
      } finally {
        session = null;
        try {
          if (transport != null) {
            transport.disconnect();
            if (logger.isDebugEnabled())
              logger.debug("closed transport");
          }
        } finally {
          try {
            if (outputStream != null) {
              outputStream.close();
              if (logger.isDebugEnabled())
                logger.debug("closed outputStream");
            }
            if (inputStream != null) {
              inputStream.close();
              if (logger.isDebugEnabled())
                logger.debug("closed inputStream");
            }
          } catch (IOException e) {
            if (logger.isDebugEnabled())
              logger.debug("error closing streams.", e);
          }
        }
      }
    }
  }

  public boolean isChannelNullOrClosed() {
    return channel == null || !channel.isOpen();
  }

}
