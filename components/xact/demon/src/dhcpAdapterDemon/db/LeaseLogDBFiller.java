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
package dhcpAdapterDemon.db;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.db.SQLUtils;

import dhcpAdapterDemon.DhcpData;
import dhcpAdapterDemon.db.dbFiller.DBFiller;
import dhcpAdapterDemon.db.dbFiller.DBFillerData;
import dhcpAdapterDemon.db.dbFiller.DataProcessor;
import dhcpAdapterDemon.db.dbFiller.PacketDBFiller;
import dhcpAdapterDemon.db.leaselog.LeaseLogDBFillerStatistics;
import dhcpAdapterDemon.db.leaselog.LeaseLogPacket;
import dhcpAdapterDemon.db.leaselog.LeaseLogReader;
import dhcpAdapterDemon.types.DhcpAction;
import dhcpAdapterDemon.types.State;

public class LeaseLogDBFiller implements DhcpDataDBFiller, DataProcessor<LeaseLogPacket> {
  final static Logger logger = Logger.getLogger(LeaseLogDBFiller.class);
 
  protected DBFiller<LeaseLogPacket> dbFiller;
  private DBFillerStatistics dbFillerStatistics;
  private LeaseLogPacket llp;
  private int bulkSize;
  private String fileName;
  private int rejectedCounter;
  private LeaseLogReader leaseLogReader;
  
  /**
   * @param name
   * @param dbFillerData
   */
  public LeaseLogDBFiller(String name, DBFillerData dbFillerData) {
    LeaseLogPacket.setTableNameLeases(    DemonProperties.getProperty("db.leaselog.tablename.leases",    "Sys.IpLeases") ); //TODO default entfernen
    LeaseLogPacket.setTableNameLeasesAct( DemonProperties.getProperty("db.leaselog.tablename.leasesAct", "Sys.IpLeasesAct") );
    fileName = dbFillerData.getFilename();
    dbFiller = new PacketDBFiller<LeaseLogPacket>(dbFillerData,getClass().getSimpleName(), true);
    bulkSize = dbFillerData.getBulksize();
    dbFiller.setLogger(logger);
    dbFiller.setDataProcessor(this);
    dbFillerStatistics = new LeaseLogDBFillerStatistics(name,dbFiller,bulkSize);
    leaseLogReader = new LeaseLogReader(dbFiller,dbFillerStatistics,dbFillerData.getFilename());
  }

  /**
   * @return
   */
  private LeaseLogPacket createNewLeaseLogPacket() {
    try {
      LeaseLogPacket ret = new LeaseLogPacket(dbFillerStatistics,fileName);
      leaseLogReader.setNewestPacket( ret.getCreationTime() );
      return ret;
    } catch (IOException e) {
      logger.error("Error while initializing LeaseLogPacket",e);
      return null;
    }
  }
  
  /**
   * @return
   */
  private LeaseLogPacket tryRebuildLeaseLogPacket() {
    logger.warn( "rejectedCounter="+rejectedCounter);
    LeaseLogPacket ret = null;
    if( rejectedCounter %  bulkSize == 0 ) { //nur alle bulksize Requests neues LeaseLogPacket bauen
      ret = createNewLeaseLogPacket();
      if( ret != null ) {
        rejectedCounter = 0; 
      }
    }
    return ret;
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#add(dhcpAdapterDemon.DhcpData)
   */
  public void add(DhcpData dhcpData) {
    //NDC.push( dhcpData.getChaddr() );
    try {
      dbFillerStatistics.state(dhcpData.getAction(),State.REQUESTED);
      if( llp == null ) {
        //Versuch, das LeaseLogPacket neu anzulegen
        llp = tryRebuildLeaseLogPacket();
      }
      if( llp == null ) {
        ++rejectedCounter;
        logger.warn("Rejected: "+dhcpData );
        dbFillerStatistics.state(dhcpData.getAction(),State.REJECTED);
        return;
      } else {
        llp.add( dhcpData );
        if( llp.size() >= bulkSize ) {
          llp.close();
          dbFiller.add(llp);
          llp = createNewLeaseLogPacket();
        }
      }
    } finally {
      //NDC.pop();
    }
  }
  
  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#getDBFillerStatistics()
   */
  public DBFillerStatistics getDBFillerStatistics() {
    return dbFillerStatistics;
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#logStatus(org.apache.log4j.Logger)
   */
  public void logStatus(Logger statusLogger) {
    if( dbFiller != null ) {
      dbFiller.logStatus(statusLogger);
    }
    String scn = getClass().getSimpleName() +" ";
    for( DhcpAction a : DhcpAction.values() ) {
      statusLogger.info( scn + dbFillerStatistics.getCountersAsString(a) );
    }
    if( leaseLogReader != null ) {
      leaseLogReader.logStatus(statusLogger);
    }
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#start()
   */
  public void start() {
    Thread t = new Thread(dbFiller);
    t.setName( dbFiller.getName() );
    t.start();
    Thread t2 = new Thread(leaseLogReader);
    t2.setName( leaseLogReader.getName() );
    t2.start();
    llp = createNewLeaseLogPacket();
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.DhcpDataDBFiller#terminate()
   */
  public void terminate() {
    if( llp != null ) {
      llp.close();
      dbFiller.add(llp);
      llp = null;
    }
    if( dbFiller != null ) {
      dbFiller.terminate();
    }
    if( leaseLogReader != null ) {
      leaseLogReader.terminate();
    }
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DataProcessor#getNDC(java.lang.Object)
   */
  public String getNDC(LeaseLogPacket data) {
    return null;
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DataProcessor#initialize(com.gip.xyna.utils.db.SQLUtils)
   */
  public void initialize(SQLUtils sqlUtils) {
    sqlUtils.cacheStatement( LeaseLogPacket.SQL_IP_LEASES );
    sqlUtils.cacheStatement( LeaseLogPacket.SQL_IP_LEASES_ACT );
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DataProcessor#processData(java.lang.Object)
   */
  public dhcpAdapterDemon.db.dbFiller.DataProcessor.SQLData processData(LeaseLogPacket data) {
    //wird von PacketDBFiller nicht gerufen
    return null;
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.dbFiller.DataProcessor#state(java.lang.Object, dhcpAdapterDemon.types.State)
   */
  public void state(LeaseLogPacket data, State state) {
    switch( state ) {
    case REQUESTED: 
      break;//ignorieren, wird anders gezählt
    case SUCCEEDED:
      leaseLogReader.setInsertPossible(); //Info an LeaseLogReader weiterreichen
      break;
    case REJECTED:
      //sollten irgendwann wieder aus dem DateiSystem gelesen werden und dadurch wiederholt werden
      leaseLogReader.incrementLostPacketCounter();
      break;
    case FAILED:
      //sollten irgendwann wieder aus dem DateiSystem gelesen werden und dadurch wiederholt werden
      leaseLogReader.incrementLostPacketCounter();
      break;
    default:
      //ignorieren
    }
  }

  
  
}
