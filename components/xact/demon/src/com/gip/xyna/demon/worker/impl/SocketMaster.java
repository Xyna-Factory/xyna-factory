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
package com.gip.xyna.demon.worker.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.worker.Master;
import com.gip.xyna.demon.worker.SlavePool;
import com.gip.xyna.demon.worker.SlaveWork;


/**
 * Vollständige Implementierung eines Masters, der auf dem angegebenen Socket lauscht und
 * die erhaltenen Pakete über den {@link SlaveWorkBuilder.buildWork} in Arbeitspakete für den {@link SlavePool}
 * verwandelt und diese dann dem SlavePool übergibt.
 *
 * @param <Tool>
 */
public class SocketMaster<Tool> implements Master<Tool,Socket>  {

  static Logger logger = Logger.getLogger(SocketMaster.class.getName());

  private volatile boolean running = false;
  private SlavePool<Tool,Socket> slavePool;
  private String name;
  private int port;
  private SlaveWorkBuilder<Tool> slaveWorkBuilder;
  private ServerSocket server;
  private AtomicInteger requestedCounter;
  
  /**
   * Interface, um den SocketMaster zu befähigen, Arbeitspakete SlaveWork<Tool,Socket> aus den Socket-Daten zu bauen.
   *
   * @param <Tool>
   */
  public static interface SlaveWorkBuilder<Tool> {
    public SlaveWork<Tool,Socket> buildWork( Socket socket );
  }

  /**
   * 
   * @param name Name des SocketMasters
   * @param port Port, auf dem gelauscht wird
   * @param slaveWorkBuilder Bau der Arbeitspakete
   * @throws IOException
   */
  public SocketMaster(String name, int port, SlaveWorkBuilder<Tool> slaveWorkBuilder ) throws IOException {
    this.name = name;
    this.port = port;
    this.slaveWorkBuilder = slaveWorkBuilder;
    requestedCounter = new AtomicInteger();
    logger.debug( "ServerSocket "+name+" will listen on port "+port );
    //noch nicht an den Port binden
  }

  public static enum Status {
    LISTENING("listening"), 
    NOT_LISTENING("not listening");
    
    private String name;
    private Status(String name) {
      this.name = name;
    }
    
    public int toInt() {
      return ordinal()+1;
    }
    @Override
    public String toString() {
      return name;
    }
  }
  
  /* (non-Javadoc)
   * @see demon.worker.Master#setSlavePool(demon.worker.SlavePool)
   */
  public void setSlavePool(SlavePool<Tool,Socket> slavePool) {
    this.slavePool=slavePool;
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    try {
      try {
        server = new ServerSocket();
        server.bind(new InetSocketAddress((InetAddress)null, port));
        logger.debug( "ServerSocket "+name+" is now listening" );
        running = true;
      } catch( IOException e ) {
        logger.error( "Error while intialzing ServerSocket",e);
        running = false;
      }
      //Initialisierung hat geklappt
      while ( running ) { 
        try { 
          Socket socket = server.accept();
          requestedCounter.getAndIncrement();
          slavePool.execute( slaveWorkBuilder.buildWork(socket) );
        } catch ( IOException e ) { 
          if( e.getMessage().equals("Socket closed") && running == false ) {
            //kein Fehler, Socket soll geschlossen werden
          } else {
            logger.error( "Error while listening",e);
          }
        }
      }
    }
    finally {
      running = false;
      try {
        if( server != null ) {
          server.close();
        }
      } catch (IOException e) {
        logger.error( "Error while closing ServerSocket", e );
      }
    }
  }
  
  /* (non-Javadoc)
   * @see demon.worker.Master#terminate()
   */
  public void terminate() {
    running = false;
    if( server != null ) {
      try {
        server.close();
      } catch (IOException e) {
        logger.error( "Error while closing ServerSocket", e );
      }
    }
    
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.worker.Master#logStatus(org.apache.log4j.Logger)
   */
  public void logStatus( Logger statusLogger ) {
    statusLogger.info( getStatusString() );
  }

  /**
   * @return
   */
  private String getStatusString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ServerSocket ").append(name).append(" is").
    append(running?"":" not").append(" listening on port ").append(port);
    return sb.toString();    
  }
  
  public int getPort() {
    return port;
  }

  public Status getStatus() {
    if( running ) {
      return Status.LISTENING;
    } else {
      return Status.NOT_LISTENING;
    }
  }

  public int getRequested() {
    return requestedCounter.get();
  }
  
}