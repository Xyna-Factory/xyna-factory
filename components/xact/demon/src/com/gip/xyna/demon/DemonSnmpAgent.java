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
package com.gip.xyna.demon;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.snmp.JVMHandler;
import com.gip.xyna.demon.snmp.StatusHandler;
import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.agent.RequestHandler;
import com.gip.xyna.utils.snmp.agent.SnmpAgentImplApache;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleDispatcher;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.varbind.VarBindList;
import com.gip.xyna.utils.snmp.OID;

/**
 * DemonSnmpAgent ist ein SnmpAgent zur Überwachung des Demons.
 * Er besitzt folgende Überwachungsmöglichkeiten:
 * <ul>
 *   <li>Ausgabe des aktuellen Status "running" oder "stopped"</li>
 *   <li>Verschicken eines Signals an den Demon, Start(1), Stop(15), Log(0), Kill(9)</li>
 *   <li>Ausgabe folgender Parameter zur Überwachung, entweder einzeln oder als Walk:
 *     <ul>
 *       <li>Anzahl aktiver Threads</li>
 *       <li>Anzahl der eingegangenen Requests</li>
 *       <li>Anzahl erfolgreich bearbeiteter Requests</li>
 *       <li>Anzahl nicht erfolgreich bearbeiteter Requests</li>
 *       <li>Anzahl erfolgreich bearbeiteter Requests</li>
 *       <li>Anzahl nicht bearbeiteter (abgewiesener) Requests</li>
 *       <li>aktueller Speicherverbrauch</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 */
public class DemonSnmpAgent implements RequestHandler, Runnable {
  static Logger logger = Logger.getLogger(DemonSnmpAgent.class.getName());
      
  private OidSingleDispatcher oidSingleDispatcher;
  private StatusHandler statusHandler;
  private JVMHandler jvmHandler;
  private SnmpAccessData snmpAccessData;
  private SnmpAgentImplApache snmpAgent;
  private volatile boolean listening = false;
  
  /**
   * Konstruktor
   * @param demon
   * @param index
   * @param snmpAccessData 
   * @param demonStatus
   */
  public DemonSnmpAgent(Demon demon, String index, SnmpAccessData snmpAccessData ) {
    statusHandler = new StatusHandler(demon);
    jvmHandler = new JVMHandler( index );
    this.snmpAccessData = snmpAccessData;
    reset();
  }
  
  /**
   * @throws IOException 
   * 
   */
  public void start() throws IOException {
    if( listening ) {
      logger.warn( "DemonSnmpAgent is already running");
      return;
    }
    listening = true;
    snmpAgent = new SnmpAgentImplApache(snmpAccessData);
    logger.debug( "SnmpAgent started");
    snmpAgent.setRequestHandler( this );
    new Thread(this,"DemonSnmpAgent").start();
  }
 
  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    while( listening ) {
      try {
        snmpAgent.listen();
        while( snmpAgent.isListening() ) {
          Thread.sleep(60000);
        }
      } catch (InterruptedException e) {
        logger.error( "Ignored interrupt while listening", e );
      } catch (IOException e) {
        logger.error( "Error while listening", e );
      } catch( Throwable t ) {
        logger.error( "Unexpected error while listening, trying to keep running", t );
      }
    }
  }
  
  public void terminate( ) {
    listening = false;
    snmpAgent.close();
  }
  
  public void registerOidSingleHandler(OidSingleHandler oidSingleHandler) {
    oidSingleDispatcher.add( oidSingleHandler );
  }
  
  public void deregisterOidSingleHandler(OidSingleHandler oidSingleHandler) {
    oidSingleDispatcher.remove( oidSingleHandler );
  }
 
  /**
   * Reset des DemonSnmpAgent, so dass nur die Demon-eigene MIB bearbeitet wird 
   */
  public void reset() {
    oidSingleDispatcher = new OidSingleDispatcher();
    oidSingleDispatcher.add( statusHandler.getOidSingleHandler() );
    oidSingleDispatcher.add( jvmHandler.getOidSingleHandler() );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.RequestHandler#snmpGet(com.gip.xyna.utils.snmp.varbind.VarBindList)
   */
  public VarBindList snmpGet(VarBindList vbl) throws SnmpRequestHandlerException {
    if( logger.isTraceEnabled() ) {
      logger.trace( "snmpGet "+ vbl );
    }
    try {
      VarBindList response = new VarBindList();
      for( int i=0; i<vbl.size(); ++i ) {
        response.add( oidSingleDispatcher.get( vbl.get(i), i ) );
      }
      return response;
    } catch( SnmpRequestHandlerException e ) {
      throw e;
    } catch( RuntimeException e ) {
      logger.error( "Exception in snmpGet",e);
      throw new SnmpRequestHandlerException( RequestHandler.GENERAL_ERROR, 0 );
    }
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.RequestHandler#snmpGetNext(com.gip.xyna.utils.snmp.varbind.VarBindList)
   */
  public VarBindList snmpGetNext(VarBindList vbl) throws SnmpRequestHandlerException {
    if( logger.isTraceEnabled() ) {
      logger.trace( "snmpGetNext "+ vbl );
    }
    try {
      VarBindList response = new VarBindList();
      for( int i=0; i<vbl.size(); ++i ) {
        response.add( oidSingleDispatcher.getNext( vbl.get(i), i ) );
      }
      return response;
    } catch( SnmpRequestHandlerException e ) {
      throw e;
    } catch( RuntimeException e ) {
      logger.error( "Exception in snmpGetNext",e);
      throw new SnmpRequestHandlerException( RequestHandler.GENERAL_ERROR, 0 );
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.RequestHandler#snmpSet(com.gip.xyna.utils.snmp.varbind.VarBindList)
   */
  public void snmpSet(VarBindList vbl) throws SnmpRequestHandlerException {
    if( logger.isTraceEnabled() ) {
      logger.trace( "snmpSet "+ vbl );
    }
    try {
      for( int i=0; i<vbl.size(); ++i ) {
        oidSingleDispatcher.set( vbl.get(i), i );
      }
    } catch( SnmpRequestHandlerException e ) {
      throw e;
    } catch( RuntimeException e ) {
      logger.error( "Exception in snmpSet",e);
      throw new SnmpRequestHandlerException( RequestHandler.GENERAL_ERROR, 0 );
    }
  }
 
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.RequestHandler#snmpInform(com.gip.xyna.utils.snmp.varbind.VarBindList)
   */
  public void snmpInform(VarBindList vbl) throws SnmpRequestHandlerException {
    if( logger.isTraceEnabled() ) {
      logger.trace( "snmpInform "+ vbl );
    }
    throw new SnmpRequestHandlerException( RequestHandler.NO_SUCH_NAME, 0 );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.RequestHandler#snmpTrap(com.gip.xyna.utils.snmp.varbind.VarBindList)
   */
  public void snmpTrap(VarBindList vbl) throws SnmpRequestHandlerException {
    logger.warn("No handling of traps "+vbl );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.RequestHandler#close()
   */
  public void close() {
    //nothing to do
  }
  
  public void snmpInform(OID arg0, VarBindList arg1) {
    //ignore inform
    logger.debug("ingnore inform oid="+arg0.getOid());    
  }

  public void snmpTrap(OID arg0, VarBindList arg1) {
    //ignore trap
    logger.debug("ingnore trap oid="+arg0.getOid());
  }


}
