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
package com.gip.xyna.utils.misc.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;

import java.net.SocketTimeoutException;

import org.apache.commons.net.telnet.TelnetClient;

public class Telnet {
  
  private TelnetClient telnet = new TelnetClient();
  private InputStream is;
  private PrintStream ps;
  private String prompt;
  private IOLogger logger;
  
  private final static int ONEHOUR=3600*1000;
  
  /**
   * Verbindung zum TelnetServer
   * @param server
   * @param port
   * @throws SocketException
   * @throws IOException
   */
  public void connect( String server, int port ) throws SocketException, IOException {
    telnet.setReaderThread(false);
    telnet.connect( server, port );
    is = telnet.getInputStream();
    ps = new PrintStream( telnet.getOutputStream() );
  }
  
  /**
   * Login f�r normales Telnet auf Port 23
   * @param username
   * @param password
   */
  public void login( String username, String password ) throws SocketTimeoutException,
                                              SocketException, IOException {
    readUntil( new StringCondition("login: "), ONEHOUR);
    writeln( username );
    readUntil(  new StringCondition("Password: "), ONEHOUR);
    writeln( password );
    readUntil( new StringCondition(prompt), ONEHOUR);
  }
   
  /**
   * Solange aus dem Stream lesen, bis Pattern erreicht wurde
   * @param pattern
   * @return kompletter gelesener String
   */
  public String readUntil( Condition condition, int timeout )throws SocketTimeoutException,
                                                SocketException, IOException {
    
    //long systime=System.currentTimeMillis();
    Integer oldTimeout=null;
    try {
      oldTimeout=telnet.getSoTimeout();
      telnet.setSoTimeout(timeout);
      
      StringBuffer sb = new StringBuffer();
      long startTimeMillis=System.currentTimeMillis();
      char ch; 
      do {
        ch = ( char )is.read();
        sb.append( ch );
        if (condition.check(sb.toString())){
            break;
        }
      } while( ch != 65535 ); //Ende des Input-Streams
          String ret = sb.toString();
          if( ch == 65535 ) {
            if (! condition.check(sb.toString())){
                throw new IOException("input stream closed: timeout="+timeout+" ms, waited "+(System.currentTimeMillis()-startTimeMillis) +" ms for "+condition.getWaitForDescription()+"got:"+ret);
                
          }
        if( logger != null ) {
          logger.debug( "End of Input-Stream");
        }
        if( ret.length() > 1 ) {
          ret = ret.substring(0, ret.length()-1 );
        }
      }
      if( logger != null ) {
        logger.logTelnetOutput( ret );
      }
      return ret;
    } catch (IOException ex){
        try{telnet.setSoTimeout(10);is.read();}catch(Exception e){/*Clean Exception*/}
        throw ex;
    }
     finally{
        try{
            if (oldTimeout!=null) 
                telnet.setSoTimeout(oldTimeout);
        }catch(Throwable t) {
            /*nop*/
        }
    }
    //return null;
  }
  
  /**
   * String an Server schicken
   * @param string
   */
  public void writeln( String string ) {
    ps.println( string );
    ps.flush();
  }


 /**
  * String an Server schicken
 * @param string
 */
  public void write( String string ) {
    ps.println( string );
    ps.flush();
  }
  
  /**
   * Ausf�hren eines Kommandos und R�ckgabe der Antwort 
   * @param cmd
   * @return Antwot des Kommandos
   */
  public String exec(String cmd) throws SocketTimeoutException,
                                          SocketException, IOException {
    writeln( cmd );
    if( logger != null ) {
      logger.logTelnetCmd( cmd );
    }
    return readUntil( new StringCondition(prompt), ONEHOUR);
  }

  /**
   * R�ckgabe des aktuellen Prompts
   * @return
   */
  public String getFullPrompt() throws SocketTimeoutException,
                                         SocketException, IOException {
    writeln( "" );
    return readUntil( new StringCondition(prompt), ONEHOUR);
  }

  /**
   * Trennen der Verbindung
   * @throws IOException
   */
  public void disconnect() throws IOException {
    telnet.disconnect();
    
  }

  /**
   * Einrichtung des Loggers
   * @param iol
   */
  public void setIOLogger(IOLogger iol) {
    this.logger = iol;
  }
  
  /**
   * Setzen des erwarteten Prompts, damit exec erkennen kann, 
   * wann die Antwort fertig ausgegben worden ist
   * @param prompt
   */
  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

}
