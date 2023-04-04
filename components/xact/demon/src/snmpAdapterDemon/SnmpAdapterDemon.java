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
import org.apache.log4j.PropertyConfigurator;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import com.gip.xyna.demon.Demon;
import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.demon.DemonWorkConfigurator;
import com.gip.xyna.demon.DemonWorker;
import com.gip.xyna.demon.DemonWorkerFactory;
import com.gip.xyna.demon.snmp.DemonSnmpExtension;
import com.gip.xyna.demon.worker.SlaveInitializer;
import com.gip.xyna.utils.snmp.SnmpAccessData;

import snmpAdapterDemon.snmp.SnmpGeneral;


/**
 * Der SNMPDemon ist ein SNMP-Agent, der von den MTAs aufgerufen wird.
 * Er wird mit zwei INFORM beauauftragt, beantwortet den ersten und ignoriert 
 * den zweiten, da dieser keine benötigten Daten enthält
 * Aus den Nachrichten werden bestimmte Informationen über ein Socket an den 
 * Config-File-Generator geliefert. 
 * Der SNMPDemon kann über SNMP gestartet und gestoppt werden, zusätzlich 
 * können aktuelle Kennwerte abgefragt werden.
 *
 */
public class SnmpAdapterDemon implements DemonWorkConfigurator {

  static Logger logger = Logger.getLogger(SnmpAdapterDemon.class.getName());

  private static final String PROPERTY_FILENAME = "snmpAdapterDemon.properties";
  private static final String LOGFILE_DEFAULT = PROPERTY_FILENAME;
   
  private static final String CFG_HOST = "cfg.host";
  private static final String CFG_PORT = "cfg.port";

  private static final String DEMON_NAME = "demon";
  private static final String WORKER_NAME = "snmpAdapter";
    
  private SnmpGeneral snmpGeneral;
  private DemonWorker demonWorker;
  
  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) {
    
    String pathToProperties = (args.length > 0 ? args[0] : "./config" )+"/";
    logger.debug("directory of properties= '" + pathToProperties + "'");
    
    PropertyConfigurator.configure(pathToProperties+LOGFILE_DEFAULT);
    DemonProperties.readProperties(pathToProperties+PROPERTY_FILENAME);
    
    logger.debug( "Initializing Demon");
    Demon demon = Demon.createDemon( DEMON_NAME );  
    demon.startDemon();  
    logger.info( "Demon started");

    try {
      
      SnmpAdapterDemon sad = new SnmpAdapterDemon();
      sad.startDemonWorker( demon );
      
    } catch( Exception e ) {
      logger.error("Exception while initializing SnmpAdapterDemon",e);
      logger.error("Demon will be stopped now");
      demon.stopDemon();
    }
    
  }
    
 

  public SnmpAdapterDemon() {
    SnmpAccessData sadWorker = DemonProperties.getSnmpAccessData( SnmpAccessData.VERSION_2c, WORKER_NAME );
    ConfigDataSender.createInstance(
        DemonProperties.getProperty( CFG_HOST ),
        DemonProperties.getIntProperty( CFG_PORT )
        );
    logger.debug( "ConfigDataSender has opened socket");
    logger.debug( "Properties successfully read, configuring SnmpWorker");
      
    logger.debug( "Initializing demonWorker");
    SnmpContextSlaveInitializer scsi = new SnmpContextSlaveInitializer(sadWorker);
    SnmpAgent snmpAgent = new SnmpAgent(sadWorker); 
     
    snmpGeneral = new SnmpGeneral();
    DemonSnmpExtension dse = new DemonSnmpExtension();
    //dse.add( new LogNdcHandler() );
    dse.add( snmpGeneral.getOidSingleHandler() );
    demonWorker = DemonWorkerFactory.createDemonWorker( DEMON_NAME, snmpAgent, scsi, dse, this );
    
    snmpGeneral.initialize( snmpAgent, ConfigDataSender.getInstance() );
    
  }
  
  private void startDemonWorker(Demon demon) {
    logger.debug( "Starting demonWorker");
    demon.setDemonWorker( demonWorker );
    demon.startDemonWorker();
  }
  
  public void initialize() {
    //nichts zu tun
  }

  public void start() {
   //nichts zu tun
  }

  public void terminate() {
    //nichts zu tun
  }  
  
  public static class SnmpContextSlaveInitializer implements SlaveInitializer<SimpleSnmp> {
    private SnmpAccessData snmpAccessData;

    public SnmpContextSlaveInitializer(SnmpAccessData snmpAccessData) {
      this.snmpAccessData = snmpAccessData;
    }

    public SimpleSnmp create(int number) {
      try {
        logger.debug( "create new Snmp "+number);
        return new SimpleSnmp( snmpAccessData );
      } catch (IOException e) {
        logger.error( "Failed to create an SnmpAgent", e );
        return null;
      }
    }

    public void destroy(SimpleSnmp snmp, int number) {
      snmp.close();
    }

    public String getThreadNamePrefix() {
      return "snmpReceiver";
    }

    public void initialize() {/*nichts zu tun*/}

    public void terminate() {/*nichts zu tun*/}

    public void logStatus(Logger statusLogger) {/*nichts zu tun*/}

    
  }


  

  
  
  public static class SimpleSnmp {
    Snmp snmp;
    public SimpleSnmp(SnmpAccessData snmpAccessData) throws IOException {
      TransportMapping transport = new DefaultUdpTransportMapping();
      snmp = new Snmp(transport);
      CommunityTarget ct = new CommunityTarget();
      ct.setCommunity(new OctetString(snmpAccessData.getCommunity()));
      ct.setVersion(SnmpConstants.version2c);  
    }

    public void close() {
      try {
        snmp.close();
      } catch (IOException e) {
        logger.error(e);
      }
    }
    
    public void sendResponse(PDU pdu, Target target) throws IOException {
      snmp.send(pdu,target);
    }
    
    public Snmp getSnmp() {
      return snmp;
    }
    
  }
  
  
}
