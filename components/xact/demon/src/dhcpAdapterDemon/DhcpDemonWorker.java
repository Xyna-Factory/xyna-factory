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

import java.util.EnumMap;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.demon.DemonSnmpConfigurator;
import com.gip.xyna.demon.DemonWorker;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.failover.FailoverDBConnectionData;
import com.gip.xyna.utils.db.failover.FailoverSources;

import common.FailoverOidSingleHandler;
import common.FailoverSourceOID;
import dhcpAdapterDemon.db.AuditDBFiller;
import dhcpAdapterDemon.db.CpeAuditDBFiller;
import dhcpAdapterDemon.db.DhcpDataDBFiller;
import dhcpAdapterDemon.db.DnsDBFiller;
import dhcpAdapterDemon.db.LeaseLogDBFiller;
import dhcpAdapterDemon.db.dbFiller.DBFillerData;
import dhcpAdapterDemon.snmp.DhcpCounter;
import dhcpAdapterDemon.snmp.DhcpDatabases;
import dhcpAdapterDemon.snmp.DhcpGeneral;
import dhcpAdapterDemon.types.Database;

public class DhcpDemonWorker implements DemonWorker {

  static Logger logger = Logger.getLogger(DhcpDemonWorker.class.getName());
  private static final String DA_PORT = "dhcpAdapter.port";
  private static final String NAME = ".name";
  
  private EnumMap<Database,DBFillerData> dbFillerData = new EnumMap<Database,DBFillerData>(Database.class);
  private EnumMap<Database,DhcpDataDBFiller> dbFiller = new EnumMap<Database,DhcpDataDBFiller>(Database.class);
  private DhcpCounter dhcpCounter;
  private DhcpDatabases dhcpDatabases;
  private DhcpGeneral dhcpGeneral;
  private SocketMaster socketMaster;
  private String demonName;

  private boolean dodns=true;
  private boolean dolegalintercept=true;
  
  public DhcpDemonWorker(String demonPrefix) {
    initialize(demonPrefix);
  }
  
  public void initialize(String demonPrefix) {
    demonName = DemonProperties.getProperty(demonPrefix+NAME);

    dodns = DemonProperties.getBooleanProperty("db.doDNS", true);
    dolegalintercept = DemonProperties.getBooleanProperty("db.doLegalIntercept", true);
    
    DhcpData.setReservedLeasesLeaseTime( DemonProperties.getIntProperty("reserved.leases.lease.time",3600) );
    
    //DB-Initialisierung
    FailoverSources.addFailover("oid", new FailoverSourceOID() );
    
    logger.debug( "initialize: DB");
    for( Database db : Database.values() ) {
      if(db==Database.DNS && dodns==false)continue;
      if(db==Database.LEASELOG && dolegalintercept==false)continue;
      dbFillerData.put( db, readDBFillerData(db) );
      dbFiller.put( db, createDBFiller(db) );
    }

    DnsDBFiller dnsfiller = null;
    if(dodns) dnsfiller = (DnsDBFiller)dbFiller.get(Database.DNS);
    
    LeaseLogDBFiller leaselogfiller = null;
    if(dolegalintercept) leaselogfiller = (LeaseLogDBFiller)dbFiller.get(Database.LEASELOG);
    
    //Bearbeitung der eingehenden Daten
    DhcpDbStack dhcpDbStack = new DhcpDbStack( 
        (AuditDBFiller)dbFiller.get(Database.AUDIT), 
        dnsfiller, 
        leaselogfiller,
        (CpeAuditDBFiller)dbFiller.get(Database.CPEAUDIT1),
        (CpeAuditDBFiller)dbFiller.get(Database.CPEAUDIT2) );

    //Einlesen der Daten aus dem Socket
    logger.debug( "initialize: SocketMaster");
    socketMaster = new SocketMaster("dhcpDataReader", DemonProperties.getIntProperty( DA_PORT ), dhcpDbStack );

    //SNMP-Ausgaben einrichten
    logger.debug( "initialize: SNMP");
    dhcpCounter = new DhcpCounter();
    dhcpDatabases = new DhcpDatabases();
    dhcpGeneral = new DhcpGeneral();

    dhcpGeneral.initialize(socketMaster, dhcpDbStack);

    for( Database db : Database.values() ) {
      if(db==Database.DNS && dodns==false)continue;
      if(db==Database.LEASELOG && dolegalintercept==false)continue;
      dhcpCounter.setDbFillerStatistics( db, dbFiller.get(db).getDBFillerStatistics() );
      dhcpDatabases.setDbFillerStatistics( db, dbFiller.get(db).getDBFillerStatistics() );
    }
  }

  /**
   * @param db
   * @return
   */
  private DBFillerData readDBFillerData(Database db) {
    String propname = null;
    boolean isCpeAudit = ( db == Database.CPEAUDIT1 || db == Database.CPEAUDIT2 );
    if( isCpeAudit ) {
      propname = "cpeaudit";
    } else {
      propname = db.name().toLowerCase();
    }
    
    DBConnectionData dcd = DemonProperties.getDBProperty(propname);
    FailoverDBConnectionData fcd = DemonProperties.getFailoverDBProperty(propname,dcd);
    if( isCpeAudit ) {
      dcd = fcd.getConnectionData(db==Database.CPEAUDIT2); //für CPEAUDIT1 normale, für ...
                                                           //... CPEAUDIT2 Failover- ConnectionData
      fcd = FailoverDBConnectionData.newFailoverDBConnectionData().
      dbConnectionData(dcd).failoverUrl(dcd.getUrl()).failoverSource("none").build();
    }
    
    DBFillerData.Type type = DBFillerData.Type.BULK_COMMIT;
    if( fcd.getNormalConnectionData().isAutoCommit() ) {
      type = DBFillerData.Type.SINGLE_COMMIT;
    }
    if( Database.LEASELOG.name().equals(db) ) {
      type = DBFillerData.Type.PACKET;
    }
    String filename = DemonProperties.getProperty(  "db."+propname+".filename" );
    if( isCpeAudit ) {
      filename = filename + (db==Database.CPEAUDIT1 ? "1" : "2" );
    }
    
    return 
      DBFillerData.
      newDBFiller(type).
      connectionData(fcd).
      capacity( readAndCheckIntProperty( "db."+propname+".capacity", 20 ) ).
      bulksize( readAndCheckIntProperty( "db."+propname+".bulksize", 20 ) ).
      filename( filename ).
      waitReconnect(readAndCheckIntProperty( "db."+propname+".waitreconnect", 10 ) ).
      build();
  }

  /**
   * @param key
   * @param defVal
   * @return
   */
  private int readAndCheckIntProperty(String key, int defVal) {
    int val = DemonProperties.getIntProperty( key, defVal );
    if( val <= 0 ) {
      val = 20;
      logger.warn(key+" <=0, adjusting to "+defVal );
    }
    return val;
  }

  /**
   * @param audit
   * @return
   */
  private DhcpDataDBFiller createDBFiller(Database db) {
    String name = db.toString();
    DBFillerData dfd = dbFillerData.get(db);
    switch( db ) {
    case AUDIT:     return new AuditDBFiller(    name, dfd );
    case DNS:       return new DnsDBFiller(      name, dfd );
    case LEASELOG:  return new LeaseLogDBFiller( name, dfd );
    case CPEAUDIT1: return new CpeAuditDBFiller( name, dfd, "cpeaudit" );
    case CPEAUDIT2: return new CpeAuditDBFiller( name, dfd, "cpeaudit" );
    }
    throw new IllegalArgumentException();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.DemonWorker#configureDemonSnmp(com.gip.xyna.demon.DemonSnmpConfigurator)
   */
  public void configureDemonSnmp(DemonSnmpConfigurator demonSnmpConfigurator) {
    
    demonSnmpConfigurator.addOidSingleHandler( dhcpCounter.getOidSingleHandler() );
    demonSnmpConfigurator.addOidSingleHandler( dhcpGeneral.getOidSingleHandler() );
    demonSnmpConfigurator.addOidSingleHandler( dhcpDatabases.getOidSingleHandler() );
    demonSnmpConfigurator.addOidSingleHandler( FailoverOidSingleHandler.getInstance());
    //demonSnmpConfigurator.addOidSingleHandler( new LogNdcHandler() );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.DemonWorker#getName()
   */
  public String getName() {
    return demonName;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.DemonWorker#logStatus(org.apache.log4j.Logger)
   */
  public void logStatus(Logger statusLogger) {
    socketMaster.logStatus(statusLogger);
    for( Database db : Database.values() ) {
      if( dbFiller.get(db) != null ) {
        dbFiller.get(db).logStatus(statusLogger);
      }
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.DemonWorker#run()
   */
  public void run() {
    for( Database db : Database.values() ) {
      if(db==Database.DNS && dodns==false)continue;
      if(db==Database.LEASELOG && dolegalintercept==false)continue;
      dbFiller.get(db).start();
    }
    socketMaster.run(); //sollte nun blockieren
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.demon.DemonWorker#terminate()
   */
  public void terminate() {
    if( socketMaster != null ) {
      socketMaster.terminate();
    }
    
    for( Database db : Database.values() ) {

      if( dbFiller.get(db) != null ) {
        if( dbFiller.get(db) != null ) {
          dbFiller.get(db).terminate();
        }
      }
    }  
  }

}
