/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package snmpAdapterDemon;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.DemonPersistency;
import com.gip.xyna.demon.persistency.PersistableCounter;


public class ConfigDataSender {
  final static Logger logger = Logger.getLogger(ConfigDataSender.class);

  public static final String PC_FAILED = null;
  
  private static ConfigDataSender INSTANCE;
  public static ConfigDataSender getInstance() {
    return INSTANCE;
  }
  
  public static void createInstance(String cfgHost, int cfgPort) {
    INSTANCE = new ConfigDataSender(cfgHost,cfgPort);
  }
  
  private String cfgHost;
  private int cfgPort;
  private Socket socket;
  private OutputStream outputStream;

  private PersistableCounter pcSucceeded;
  private PersistableCounter pcFailed;
  private PersistableCounter pcRebuilds;
  
  private ConfigDataSender(String cfgHost, int cfgPort) {
    pcSucceeded = new PersistableCounter("configDataSender.succeeded");
    pcFailed = new PersistableCounter("configDataSender.failed");
    pcRebuilds = new PersistableCounter("configDataSender.rebuilds");
    DemonPersistency dp = DemonPersistency.getInstance();
    dp.registerPersistable(pcSucceeded);
    dp.registerPersistable(pcFailed);
    dp.registerPersistable(pcRebuilds);
    
    this.cfgHost = cfgHost;
    this.cfgPort = cfgPort;
    try {
      openSocket();
    } catch( IOException e ) {
      logger.error( "Error while opening socket to "+cfgHost+":"+cfgPort, e );
    }
  }

  
  public synchronized void send(ConfigData cd) {
    if( outputStream == null ) {
      logger.warn( "No socket for sending ConfigData" );
      rebuildSocket();
    }
    if( outputStream == null ) {
      logger.error( "Error while sending to ConfigFileGenerator could not be solved" );
      logger.error( "Could not send "+cd );
      pcFailed.increment();
      return;
    }
    try {
      sendInternal(cd);
    } catch( IOException e ) {
      logger.error( "Error while sending to ConfigFileGenerator", e );
      rebuildSocket();
      try {
        sendInternal(cd);
      } catch( IOException e2 ) {
        logger.error( "Error while sending to ConfigFileGenerator could not be solved", e );
        logger.error( "Could not send "+cd );
        pcFailed.increment();
      }
    }
  }
  
  /**
   * @param cd
   * @throws IOException 
   */
  private void sendInternal(ConfigData cd) throws IOException {
    outputStream.write(cd.formatForCfgFileGenerator()); 
    outputStream.flush();
    if( logger.isTraceEnabled() ) {
      logger.trace("send to ConfigFileGenerator " + cd);
    } else if( logger.isDebugEnabled() ) {
      logger.debug("send to ConfigFileGenerator");
    }
    pcSucceeded.increment();
  }

  private void rebuildSocket() {
    logger.info( "Trying to rebuild socket");
    close();
    try {
     openSocket();
     pcRebuilds.increment();
    } catch (IOException e) {
      logger.error( "Error while opening socket to "+cfgHost+":"+cfgPort,e);
    }
  }

  public synchronized void close() {
    if( outputStream != null ) {
      try {
        outputStream.close();
      } catch (IOException e) {
        logger.error( "Error while closing outputStream",e);
      }  
    }
    if( socket != null ) {
      try {
        socket.close();
      } catch (IOException e) {
        logger.error( "Error while closing socket",e);
      } 
    }
  }

  private synchronized void openSocket() throws IOException {
    socket = new Socket();
    socket.setReuseAddress(true);
    socket.bind( new InetSocketAddress(0) );
    socket.connect( new InetSocketAddress(cfgHost, cfgPort) );
  
    outputStream = socket.getOutputStream();
  }

  /**
   * @return
   */
  public PersistableCounter getPcSucceeded() {
    return pcSucceeded;
  }

  /**
   * @return
   */
  public PersistableCounter getPcFailed() {
    return pcFailed;
  }

  /**
   * @return
   */
  public PersistableCounter getPcRebuilds() {
    return pcRebuilds;
  }
 
}
