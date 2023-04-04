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
package dhcpAdapterDemon;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;


public class SocketMaster  {

  static Logger logger = Logger.getLogger(SocketMaster.class.getName());

  /**
   * SocketReaderConsumer konsumiert die vom SocketReader gelesenen Daten.
   *
   */
  public interface SocketReaderConsumer {
    /**
     * Zu konsumierender String
     * (endet mit dem in {@link getDataSuffix} übergebenen Suffix oder ist
     * der letzte Rest vor dem EOF)
     * @param data
     */
    public void consume( String data );
    /**
     * Hierüber wird der Suffix der zu konsumierenden Daten angegeben. 
     * Der an {@link consume} weitergegebene String endet mit dem Suffix
     * Default bei null ist "\n"
     * @return suffix
     */
    public String getDataSuffix();
    /**
     * Übergibt die geschätzte Größe des zu lesenden Strings (StringBuilder-capacity kann besser gewählt werden)
     * Default bei <= 0 ist 1000
     * @return
     */
    public int estimatedLength();
  }

  private volatile boolean running = false;
  private String name;
  private int port;
  private ServerSocket server;
  private SocketReaderConsumer socketReaderConsumer;
  private HashMap<String,SocketReader> socketReaders;
  
  /**
   * @param name Name des SocketMasters
   * @param port Port, auf dem gelauscht wird
   * @param socketReaderConsumer konsumiert die vom SocketReader gelesenen Daten  
   */
  public SocketMaster(String name, int port, SocketReaderConsumer socketReaderConsumer) {
    this.name = name;
    this.port = port;
    this.socketReaderConsumer = socketReaderConsumer;
    logger.debug( "ServerSocket "+name+" will listen on port "+port );
    socketReaders = new HashMap<String,SocketReader>();
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
   * @see java.lang.Runnable#run()
   */
  public void run() {
    try {
      try {
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress((InetAddress)null, port));
        logger.debug( "ServerSocket "+name+" is now listening" );
        running = true;
      } catch( IOException e ) {
        logger.error( "Error while initializing ServerSocket",e);
        running = false;
      }
      //Initialisierung hat geklappt
      while ( running ) { 
        try { 
          Socket socket = server.accept();
          startNewSocketReader( socket );
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
      logger.info("ServerSocket "+name+" will now be terminated" );
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
  
  /**
   * Startet einen neuen SocketReader
   * @param socket
   */
  private void startNewSocketReader(Socket socket) {
    try {
      SocketReader sr = new SocketReader( socket, socketReaderConsumer, name, this );
      new Thread(sr,sr.getName() ).start();
    } catch( IOException e ) {
      logger.error( "Could not start SocketReader", e );
    }
  }

  
  /**
   * SocketReader liest aus dem Socket und gibt die gelesenen Daten an SocketReaderConsumer weiter.
   *
   */
  public static class SocketReader implements Runnable {
    private Socket socket;
    private String name;
    private SocketReaderConsumer socketReaderConsumer;
    private InputStream inputStream;
    private SocketMaster socketMaster;
 
    /**
     * @param socket
     * @param socketReaderConsumer 
     * @param namePrefix Namensprefix des Threads
     * @throws IOException falls InputStream nicht erhalten wird
     */
    public SocketReader(Socket socket, SocketReaderConsumer socketReaderConsumer, String namePrefix, SocketMaster socketMaster) throws IOException {
      this.socket = socket;
      this.socketReaderConsumer = socketReaderConsumer; 
      this.name = namePrefix+"-"+String.valueOf(socket.getPort());
      this.socketMaster = socketMaster;
      inputStream = socket.getInputStream();
    }
    
    /**
     * @return Name des Threads
     */
    public String getName() {
      return name;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
      socketMaster.register(this);
      if( socket.isClosed() ) {
        return;
      }
      logger.info( name+" has started" );
      try {
        InputStreamTokenizer isr = new InputStreamTokenizer(
            inputStream,
            socketReaderConsumer.getDataSuffix(),
            socketReaderConsumer.estimatedLength() );

        for( String data : isr ) {
          socketReaderConsumer.consume( data );
        }

      }
      catch( RuntimeException e ) {
        logger.error( "Error while reading from socket", e );
      }
      finally {
        try {
          inputStream.close();
          socket.close();
        } catch( Throwable t) {
          logger.error( "Error while closing socket", t );
        }
        
        socketMaster.deregister(this);
        logger.info( name+" has finished" );
        NDC.remove();
      }
    }

    public void terminate() {      
      try {
        socket.close();
        inputStream.close();
      } catch (IOException e) {
        logger.error( e );
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
    for( SocketReader sr : socketReaders.values() ) {
      sr.terminate();
    }
    
  }

  /**
   * SocketReader wird in die Menge aller laufenden SocketReader aufgenommen
   * @param socketReader
   */
  private synchronized void register(SocketReader socketReader) {
    socketReaders.put( socketReader.getName(), socketReader );
  }

  /**
   * SocketReader wird aus der Menge aller laufenden SocketReader entfernt
   * @param socketReader
   */
  private synchronized void deregister(SocketReader socketReader) {
    socketReaders.remove( socketReader.getName() );
  }

  /**
   * @param statusLogger
   */
  public void logStatus(Logger statusLogger) {
    StringBuilder sb = new StringBuilder();
    sb.append("ServerSocket ").append(name).append(" is").
    append(running?"":" not").append(" listening on port ").append(port).
    append(" with ").append(socketReaders.size()).append(" SocketReaders(").append(socketReaders.keySet()).append(")");
    statusLogger.info( sb.toString() );
  }
  
  /**
   * Ausgabe des Ports, auf dem der SocketMaster läuft
   * @return
   */
  public int getPort() {
    return port;
  }

  /**
   * Ausgabe des Listening-Status des SocketMaster 
   * @return
   */
  public Status getStatus() {
    if( running ) {
      return Status.LISTENING;
    } else {
      return Status.NOT_LISTENING;
    }
  }
  
}