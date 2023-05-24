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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.apache.sshd.common.session.helpers.TimeoutIndicator.TimeoutStatus;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.session.ServerSessionAware;
import org.apache.sshd.server.session.ServerSession;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.trigger.SSHTriggerConnection.RequestType;

import xact.ssh.server.HostKey;
import xact.ssh.server.SSHSessionStore;
import xact.ssh.server.SSHSessionStore.SSHConnection;
import xact.ssh.server.XynaSSHServer;

public class ShellCommand implements Command, ServerSessionAware, SSHConnection {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(ShellCommand.class);

  private XynaSSHServer sshd;
  private OutputStream errStream;
  private ExitCallback exitCallBack;
  private InputStream inStream;
  private OutputStream outStream;
  private Environment environment;
  private InputStreamReader inReader;
  private OutputStreamWriter outWriter;
  protected String currentPrompt; 
  private IOException lastException;
  private boolean invisible;
  private ServerSession session;
  private Queue<SSHTriggerConnection> requests;
  private boolean closed = false;
  private boolean clientExit = false;
  private SSHStartParameter startParameter;
  private SSHConnectionParameter connectionParameter;
  private SSHCustomizationParameter customization = SSHCustomizationParameter.buildDefault();
  
  public ShellCommand(XynaSSHServer sshd, Queue<SSHTriggerConnection> requests, SSHStartParameter startParameter) {
    this.sshd = sshd;
    this.requests = requests;
    this.startParameter = startParameter;
  }
 
  public void destroy(ChannelSession cs) {
    if( session.getTimeoutStatus().getStatus() == TimeoutStatus.NoTimeout) {
      if( clientExit ) {
        logger.info("Client requested disconnect on "+connectionParameter.getUniqueId() );
      } else {
        logger.info("Client forced disconnect on "+connectionParameter.getUniqueId() );
      }
    } else {
      logger.info("Server disconnect due to " +session.getTimeoutStatus()+" on "+connectionParameter.getUniqueId());
    }
  }

  public void setErrorStream(OutputStream errStream) {
    this.errStream = errStream;
  }

  public void setExitCallback(ExitCallback exitCallBack) {
    this.exitCallBack = exitCallBack;
  }

  public void setInputStream(InputStream inStream) {
    this.inStream = inStream;
  }

  public void setOutputStream(OutputStream outStream) {
    this.outStream = outStream;
  }

  public void setSession(ServerSession session) {
    this.session = session;
  }
  
  public SSHConnectionParameter getConnectionParameter() {
    return connectionParameter;
  }
  
  public void start(ChannelSession cs, Environment environment) throws IOException {
    this.environment = environment;
    
    //TODO Encoding aus environment lesen
    this.inReader = new InputStreamReader(inStream, "UTF-8");
    this.outWriter = new OutputStreamWriter(outStream, "UTF-8");
    
    this.connectionParameter = SSHConnectionParameter.build(session,environment);
    
    SSHSessionStore.store( connectionParameter.getUniqueId(), this );
    
    SSHTriggerConnection sshCon = new SSHTriggerConnection(this, RequestType.Init);
    requests.offer( sshCon );
  }

  private void exit() {
    close(outStream, "outStream");
    close(errStream, "errStream");
    close(inStream, "inStream");
    SSHSessionStore.remove( connectionParameter.getUniqueId() );
    
    clientExit = true;
    
    exitCallBack.onExit(0, "bye");
    
    closed = true;
    
    SSHTriggerConnection sshCon = new SSHTriggerConnection(this, RequestType.Close);
    requests.offer( sshCon );
  }
  
  private void close(Closeable c, String name) {
    try {
      c.close();
    } catch (IOException e) {
      if( !closed ) {
        logger.info( name + " closed? ", e );
      } else {
        logger.debug( name + " is probably closed");
      }
    }
  }

  public boolean isClosed() {
    return closed;
  }
 

  public void send(String msg) throws IOException {
    try {
      String mapped = customization.getNewLine().mapNewLines(msg);
      outWriter.write(mapped);
      outWriter.flush();
    } catch( IOException e ) {
      lastException = e;
    }
  }
  
  public void sendLine(String msg) throws IOException {
    try {
      String mapped = customization.getNewLine().mapNewLines(msg);
      outWriter.write(mapped);
      outWriter.write(customization.getNewLine().getNewLine());
      outWriter.flush();
    } catch( IOException e ) {
      lastException = e;
    }
  }
  
  public void sendPrompt() throws IOException {
    try {
      outWriter.write(currentPrompt);
      outWriter.flush();
    } catch( IOException e ) {
      lastException = e;
    }
  }
  
  public String readLine() throws IOException {
    StringBuilder sb = new StringBuilder();
    boolean appending = true;
    while( appending ) {
      int b = inReader.read();
      appending = read(sb, b);
    }
    return sb.toString();
  }
  
  public String readLine(long timeout) throws IOException {
    StringBuilder sb = new StringBuilder();
    boolean appending = true;
    while( appending && System.currentTimeMillis() < timeout ) {
      if( ! inReader.ready() ) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          //dann halt sofort zurück
          return sb.toString();
        }
      }
      while( appending && inReader.ready() ) {
        appending = read(sb, inReader.read() );
      }
    }
    return sb.toString();
  }
  
  @Override
  public String readAll(long timeout) throws IOException {
    StringBuilder sb = new StringBuilder();
    while( System.currentTimeMillis() < timeout ) {
      if( ! inReader.ready() ) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          //dann halt sofort zurück
          return sb.toString();
        }
      }
      while( inReader.ready() ) {
        boolean appending = read(sb, inReader.read() );
        if( ! appending ) {
          sb.append("\n");
        }
      }
    }
    return sb.toString();
  }
  
  
  public String readMultiLineNonBlocking() throws IOException {
    StringBuilder sb = new StringBuilder();
    while( inReader.ready() ) {
      boolean appending = read(sb, inReader.read());
      if( ! appending ) {
        sb.append("\n");
      }
    }
    return sb.toString();
  }
  
  private boolean read(StringBuilder sb, int b) throws IOException {
    boolean appending = true;
    switch( b ) {
    case -1:
      closed = true;
      appending = false;
      break;
    case 10: //\n, Line Feed
      logger.info("10");
      appending = false;
      break;
    case 13: //\r, Carriage Return
      logger.info("13");
      if( inReader.ready() ) {
        logger.info("in is ready " );
        //TODO was, wenn kein Line Feed folgt?
      } else {
        appending = false;
      }
      break;
    case 127: //Delete
      outWriter.write("\u0008 \u0008");//Backspace, mit Leerzeichen überschreiben und nochmal Backspace
      outWriter.flush();
      if( sb.length() > 0 ) {
        sb.deleteCharAt(sb.length()-1);
      }
      break;
    default:
      if( ! invisible ) {
        outWriter.write(b);
        outWriter.flush();
      }
      sb.append((char)b);
    }
    if( ! appending ) {
      if( !closed ) {
        outWriter.write(customization.getNewLine().getNewLine()); 
        outWriter.flush();
      }
    }
    return appending;
  }

  protected String getUser() {
    return environment.getEnv().get(Environment.ENV_USER);
  }
  
  protected void setInvisible(boolean invisible) {
    this.invisible = invisible;
  }
  
  
  protected XynaSSHServer getSshServer() {
    return sshd;
  }

  public void close() {
    exit();
  }

  public void nextRequest() {
    SSHTriggerConnection sshCon = new SSHTriggerConnection(this, RequestType.Exec);
    requests.offer( sshCon );
  }

  public void customize(SSHCustomizationParameter customization) {
    this.customization = customization;
  }

  public SSHCustomizationParameter getCustomization() {
    return customization;
  }

  public SSHStartParameter getStartParameter() {
    return startParameter;
  }

  @Override
  public String getHostKey() {
    HostKey hk = sshd.getHostKey();
    return hk.getSshRsaKey();
  }

}
