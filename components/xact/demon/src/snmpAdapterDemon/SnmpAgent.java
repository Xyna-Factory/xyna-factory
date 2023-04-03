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

import org.apache.log4j.Logger;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.transport.TransportMappings;

import snmpAdapterDemon.SnmpAdapterDemon.SimpleSnmp;

import com.gip.xyna.demon.persistency.PersistableCounter;
import com.gip.xyna.demon.worker.Master;
import com.gip.xyna.demon.worker.SlavePool;
import com.gip.xyna.demon.worker.SlavePool.CounterData;
import com.gip.xyna.utils.snmp.SnmpAccessData;

public class SnmpAgent implements Master<SimpleSnmp,CommandResponderEvent>, CommandResponder {
  static Logger logger = Logger.getLogger(SnmpAgent.class.getName());
  
  private SlavePool<SimpleSnmp,CommandResponderEvent> slavePool;
  private Snmp snmp;
  private volatile boolean listening;
  private SnmpAccessData snmpAccessData;

  public SnmpAgent(SnmpAccessData snmpAccessData) {
    this.snmpAccessData = snmpAccessData;
    Address address = GenericAddress.parse("udp:"+snmpAccessData.getHost()+"/"+snmpAccessData.getPort());
    TransportMapping transport = TransportMappings.getInstance().createTransportMapping(address);
    
    snmp = new Snmp();
    MessageDispatcher messageDispatcher = snmp.getMessageDispatcher();
    messageDispatcher.addCommandResponder(snmp);
    
    if( snmpAccessData.isSNMPv1() ) {
      messageDispatcher.addMessageProcessingModel(new MPv1());
    } else if( snmpAccessData.isSNMPv2c() ) {
      messageDispatcher.addMessageProcessingModel(new MPv2c());
    } else if( snmpAccessData.isSNMPv3() ) {
      messageDispatcher.addMessageProcessingModel(new MPv3());
      SecurityProtocols.getInstance().addDefaultProtocols();
    } else {
      throw new IllegalArgumentException("SnmpAccessData with unknown SNMP version");
    }
    snmp.addTransportMapping(transport);
 
    snmp.getMessageDispatcher().addCommandResponder(this);
  }

  public void setSlavePool(SlavePool<SimpleSnmp,CommandResponderEvent> slavePool) {
    this.slavePool = slavePool;
  }

  public void run() {
    logger.debug( "SnmpAgent.run" );
    listening = false;
    try {
      snmp.listen();
      listening = true;
    } catch (IOException e) {
      logger.error(e);
    }
    synchronized (this) {
      while( listening ) {
        try {
          wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void processPdu(CommandResponderEvent event ) {
    slavePool.execute( new CommandResponderEventWork(event) );
    //Falls der SlavePool überlastet ist, werden die CommandResponderEventWork 
    //nicht berabeitet. Daher wird keinen Antwort zurückgeschickt, so dass 
    //das Modem einen Timeout erhält und noch einmal senden muss.
  }

  public void terminate() {
    synchronized (this) {
      listening = false;
      notify();
    }
  }

  public void logStatus(Logger statusLogger) {
    if( listening ) {
      statusLogger.info("SnmpAgent is listening on port "+snmpAccessData.getPort() );
    } else {
      statusLogger.info("SnmpAgent is not listening");
    }
  }
  
  public PersistableCounter getSlaveCounter(CounterData counterData) {
    return slavePool.getPersistableCounter(counterData);
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
  
  public Status getStatus() {
    if( listening ) {
      return Status.LISTENING;
    } else {
      return Status.NOT_LISTENING;
    }
  }

}