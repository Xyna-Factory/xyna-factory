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
package com.gip.xyna.utils.snmp.agent;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.transport.TransportMappings;

import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.exception.SnmpRequestHandlerException;

public class SnmpAgentPooledImplApache implements CommandResponder, SnmpAgentPooled {
  static Logger logger = Logger.getLogger(SnmpAgentPooledImplApache.class.getName());

  private SnmpAccessData snmpAccessData;
  private Snmp snmp;
  private ThreadPoolExecutor threadPoolExecutor;
  private RequestHandlerProvider requestHandlerProvider;
  private int counterRequests = 0;
  private int counterRejected = 0;
  private int counterFailed = 0;
  private int counterSuccessful = 0;
  
  private SnmpAgentLogger snmpAgentLogger = new SnmpAgentLogger() { //Dummy Implementation, no null-check necessary
    public void setLastReceived(int lastReceived) {/*dummy*/}
    public void setLastSent(int lastSent) {/*dummy*/}
    public void setType(String string) {/*dummy*/}
    public void setFailed(SnmpRequestHandlerException e) {
      ++counterFailed;
    }
    public void setSuccess() {
      ++counterSuccessful;
    }
  };

  public SnmpAgentPooledImplApache(SnmpAccessData snmpAccessData, ThreadPoolExecutor threadPoolExecutor) throws IOException {
    this.threadPoolExecutor = threadPoolExecutor;
   
    this.snmpAccessData = snmpAccessData;
    
    Address address = GenericAddress.parse("udp:"+snmpAccessData.getHost()+"/"+snmpAccessData.getPort());
    TransportMapping transport = TransportMappings.getInstance().createTransportMapping(address);
    snmp = new Snmp(transport);
    
    snmp.getMessageDispatcher().addCommandResponder(this);
    
    snmp.listen();
  }
  
  public void processPdu(CommandResponderEvent event) {
    ++counterRequests;
    RequestHandler requestHandler = requestHandlerProvider.newRequestHandler();
    try {
      threadPoolExecutor.execute( new PduEventHandlerApache( event, requestHandler, snmpAgentLogger ) );
    } catch( RejectedExecutionException e ) {
       System.out.println( "RejectedExecutionException" );
       ++counterRejected;
    }
  }

  

  public void close() {
    try {
      snmp.close();
    } catch (IOException e) {
      logger.error("Error whil closing SNMP-Agent", e);
    }
  }

  
  public void setRequestHandler(RequestHandler requestHandler) {
    throw new UnsupportedOperationException();
  }

  public void setSnmpAgentLogger(SnmpAgentLogger snmpAgentLogger) {
    this.snmpAgentLogger = snmpAgentLogger;
  }

  /**
   * @param requestHandlerProvider the requestHandlerProvider to set
   */
  public void setRequestHandlerProvider( RequestHandlerProvider requestHandlerProvider) {
    this.requestHandlerProvider = requestHandlerProvider;
  }

  public int getCounterRejected() {
    return counterRejected;
  }

  public int getCounterRequests() {
    return counterRequests;
  }

  public int getCounterFailed() {
    return counterFailed;
  }

  public int getCounterSuccessful() {
    return counterSuccessful;
  }

}
