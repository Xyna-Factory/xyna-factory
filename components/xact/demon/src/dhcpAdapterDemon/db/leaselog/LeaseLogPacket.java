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

package dhcpAdapterDemon.db.leaselog;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.db.SQLUtils;

import dhcpAdapterDemon.DhcpData;
import dhcpAdapterDemon.db.DBFillerStatistics;
import dhcpAdapterDemon.db.dbFiller.PacketDBFiller;
import dhcpAdapterDemon.db.leaselog.FilePersistentList.Status;
import dhcpAdapterDemon.types.State;

/**
 * LeaseLogPacket ist die Implementation des Datenpakets, das gemeinsam durch den 
 * PacketDBFiller eingetragen wird.
 *
 */
public class LeaseLogPacket implements PacketDBFiller.Packet {
  final static Logger logger = Logger.getLogger(LeaseLogPacket.class);
  
  FilePersistentList<LeaseLogData> data;
  
  private LeaseLogData lastData;
  private DBFillerStatistics dbFillerStatistics;
  private long creationTime;
    
  public static final String SQL_IP_LEASES_ACT = 
    //"(lact_ip_addr,lact_start_tm,lact_act_cd) " +
    //"VALUES(?,TO_DATE(?,'YYYY/MM/DD HH24:MI:SS'),?)";
    "(lact_ip_addr,lact_start_tm,lact_act_cd) " +
    "VALUES(HEXTORAW(?),TO_DATE(?,'YYYY/MM/DD HH24:MI:SS'),?)";

  public static final String SQL_IP_LEASES = 
    "(lease_ip_addr,lease_start_tm,lease_mac_addr,lease_end_tm,lease_is_reserved,lease_host_type,lease_cm_mac_addr) " +
    "VALUES(HEXTORAW(?),TO_DATE(?,'YYYY/MM/DD HH24:MI:SS'),HEXTORAW(?),TO_DATE(?,'YYYY/MM/DD HH24:MI:SS'),?,?,HEXTORAW(?))";

  private static String TABLE_LEASES_ACT = "IpLeasesAct";
  private static String TABLE_LEASES = "IpLeases";
    
  public static void setTableNameLeases(String leases) {
    TABLE_LEASES = leases;
  }
  public static void setTableNameLeasesAct(String leasesAct) {
    TABLE_LEASES_ACT = leasesAct;
  }
 
  /**
   * Konstruktor für ein neu anzulegendes Paket
   * @param dbFillerStatistics
   * @param filename 
   * @throws IOException 
   */
  public LeaseLogPacket(DBFillerStatistics dbFillerStatistics, String filename) throws IOException {
    creationTime = System.currentTimeMillis();
    this.dbFillerStatistics = dbFillerStatistics;
    data = new FilePersistentList<LeaseLogData>( new File(filename+creationTime+".dat" ), LeaseLogData.getFactory(), Status.NEW );
  }
  
  /**
   * Konstruktor für ein bereits bestehendes Paket, das aus einer Datei gelesen werden muss
   * @param dbFillerStatistics
   * @param creationTime
   * @param filename
   * @throws IOException
   */
  public LeaseLogPacket(DBFillerStatistics dbFillerStatistics, long creationTime, String filename ) throws IOException {
    this.creationTime = creationTime;
    this.dbFillerStatistics = dbFillerStatistics;
    data = new FilePersistentList<LeaseLogData>( new File(filename+creationTime+".dat" ), LeaseLogData.getFactory(), Status.READONLY );
  }

  /**
   * @param dhcpData
   */
  public void add(DhcpData dhcpData) {
    LeaseLogData lld = new LeaseLogData(dhcpData);
    data.add( lld );
     if( logger.isTraceEnabled() ) {
      logger.trace( "added \""+lld+"\" to "+data );
    } else {
      if( logger.isDebugEnabled() ) {
        logger.debug( "done" );
      }
    }
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.neu.PacketDBFiller.Packet#process(com.gip.xyna.utils.db.SQLUtils)
   */
  public void process(SQLUtils sqlUtils) throws SQLException {
    PreparedStatement ps1 = null;
    PreparedStatement ps2 = null;
    String statement="";
    String statementData="";
    try {
      String sqlPs1Statement = "INSERT INTO "+TABLE_LEASES_ACT+SQL_IP_LEASES_ACT;
      ps1 = sqlUtils.prepareStatement(sqlPs1Statement);
      String sqlPs2Statement = "INSERT INTO "+TABLE_LEASES+SQL_IP_LEASES;
      ps2 = sqlUtils.prepareStatement(sqlPs2Statement);
      

      for( LeaseLogData d : data ) {
        logger.trace("process "+d);
        lastData = d;

        ps1.setString(1, d.get(LeaseLogData.Data.IP_ADDR) );
        ps1.setString(2, d.get(LeaseLogData.Data.START_TM) );
        ps1.setString(3, d.get(LeaseLogData.Data.ACT_CD) );
        statement=sqlPs1Statement;
        statementData=  d.toString();
        sqlUtils.excuteUpdate(ps1);

        if( d.isInsert() ) {
          ps2.setString(1, d.get(LeaseLogData.Data.IP_ADDR) );
          ps2.setString(2, d.get(LeaseLogData.Data.START_TM) );
          ps2.setString(3, d.get(LeaseLogData.Data.MAC_ADDR) );
          ps2.setString(4, d.get(LeaseLogData.Data.END_TM) );
          ps2.setString(5, d.get(LeaseLogData.Data.IS_RESERVED) );
          ps2.setString(6, d.get(LeaseLogData.Data.HOST_TYPE) );
          ps2.setString(7, d.get(LeaseLogData.Data.CM_MAC_ADDR) );
          statement=sqlPs2Statement;
          statementData=  d.toString();
          sqlUtils.excuteUpdate(ps2);
        }
      }
    } catch (SQLException e){
      logger.error("Error processing Statement "+statement+":"+statementData,e);
      throw e;
    }finally {
      sqlUtils.finallyClose(null, ps1);
      sqlUtils.finallyClose(null, ps2);
    }
  }
  
  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.neu.PacketDBFiller.Packet#commit()
   */
  public void commit() {
    for( LeaseLogData d : data ) {
      dbFillerStatistics.state( d.getDhcpAction(), State.SUCCEEDED );
    }
    
    try {
      data.close();
      data.delete();
    } catch (IOException e) {
      logger.error("Error while deleting "+this.toString(),e);
    }

    logger.debug("commit");
  }

  /* (non-Javadoc)
   * @see dhcpAdapterDemon.db.neu.PacketDBFiller.Packet#rollback()
   */
  public void rollback() {
    logger.warn("rollback, last inserted data=\""+ lastData +"\"");
  }

  /**
   * @return
   */
  public int size() {
    return data.size();
  }

  /**
   * Schließt das LeaseLogPacket, so dass keine weiteren Daten mehr eingetragen werden können
   */
  public void close() {
    try {
      data.close();
    } catch (IOException e) {
      logger.error("Error while closing "+this.toString(),e);
    }
  }

  /**
   * @return the creationTime
   */
  public long getCreationTime() {
    return creationTime;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append( "LeaseLogpacket(").append(data).append(")");
    return sb.toString();
  }
  
}
