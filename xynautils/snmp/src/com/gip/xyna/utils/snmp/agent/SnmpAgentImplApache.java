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
package com.gip.xyna.utils.snmp.agent;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.transport.TransportMappings;

import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;
import com.gip.xyna.utils.snmp.manager.SnmpContextImplApache;

public class SnmpAgentImplApache implements SnmpAgent, CommandResponder {
  static Logger logger = Logger.getLogger(SnmpAgentImplApache.class.getName());

  private RequestHandler requestHandler;
  private SnmpAccessData snmpAccessData;
  private Snmp snmp;
  
  private SnmpAgentLogger snmpAgentLogger = new SnmpAgentLogger() { //Dummy Implementation, no null-check necessary
    public void setLastReceived(int lastReceived) {/*dummy*/}
    public void setLastSent(int lastSent) {/*dummy*/}
    public void setType(String string) {/*dummy*/}
    public void setFailed(SnmpRequestHandlerException e) {/*dummy*/}
    public void setSuccess() {/*dummy*/}
  };

  static {
    LogFactory.setLogFactory(new Log4jLogFactory());
  }

  public SnmpAgentImplApache( SnmpAccessData snmpAccessData ) {
    this.snmpAccessData = snmpAccessData;
    
    Address address = GenericAddress.parse("udp:"+snmpAccessData.getHost()+"/"+snmpAccessData.getPort());
    TransportMapping transport = TransportMappings.getInstance().createTransportMapping(address);
    
//    MessageDispatcher messageDispatcher = snmp.getMessageDispatcher();
//    messageDispatcher.addCommandResponder(snmp);
    snmp = new Snmp();
    MessageDispatcher messageDispatcher = snmp.getMessageDispatcher();
    messageDispatcher.addCommandResponder(snmp);
    
    if( snmpAccessData.isSNMPv1() ) {
      messageDispatcher.addMessageProcessingModel(new MPv1());
    } else if( snmpAccessData.isSNMPv2c() ) {
      messageDispatcher.addMessageProcessingModel(new MPv2c());
    } else if( snmpAccessData.isSNMPv3() ) {
      SnmpContextImplApache.initV3(snmp, snmpAccessData);

    } else {
      throw new IllegalArgumentException("SnmpAccessData with unknown SNMP version");
    }
    snmp.addTransportMapping(transport);
    
    messageDispatcher.addCommandResponder(this);
  }
  
  /**
   * Agent should listen
   * @throws IOException 
   * 
   */
  public void listen() throws IOException {
    snmp.listen();
  }

  /**
   * Is agent listening?
   * @return
   */
  public boolean isListening() {
    //diese Funktionalitaet sollte eigentlich schon Snmp4j-Snmp anbieten!
    for( Iterator<?> it = snmp.getMessageDispatcher().getTransportMappings().iterator(); it.hasNext(); ) {
      TransportMapping tm = (TransportMapping) it.next();
      if( !tm.isListening() ) {
        return false;
      }
    }
    return true;
  }
  
  public void processPdu(CommandResponderEvent event) {
    new PduEventHandlerApache( event, requestHandler, snmpAgentLogger ).run();
  }

  public void close() {
    try {
      snmp.close();
    } catch (IOException e) {
      logger.error("Error while closing SNMP-Agent", e);
    }
  }

  public void setRequestHandler(RequestHandler requestHandler) {
    this.requestHandler = requestHandler;
  }

  public void setSnmpAgentLogger(SnmpAgentLogger snmpAgentLogger) {
    this.snmpAgentLogger = snmpAgentLogger;
  }

}
