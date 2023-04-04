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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;

import dhcpAdapterDemon.DhcpData;
import dhcpAdapterDemon.db.dbFiller.DBFillerData;
import dhcpAdapterDemon.types.DhcpAction;
import snmpTrapDemon.poolUsage.Subnet;

public class CpeAuditDBFiller extends DhcpDataDBFillerImpl {
  final static Logger logger = Logger.getLogger(CpeAuditDBFiller.class);
   
  private final String INSERT_PART;
  private final String INSERT_NEW;
  private final String INSERT_RENEW;
  private final String DELETE;

  private final String INSERT_PACKETS;
  
  private ArrayList<Subnet> nonCpeSubnets;
  private ArrayList<String> subnets;

  /**
   * @param name
   * @param dbFillerData
   */
  public CpeAuditDBFiller(String name, DBFillerData dbFillerData, String dbName ) {
    super(name,dbFillerData,logger);
    String tableName = DemonProperties.getProperty("db."+dbName.toLowerCase()+".tablename" );
    String tableNamePackets =  DemonProperties.getProperty("db."+dbName.toLowerCase()+".packets.tablename");
    
    //INSERT_PART = "INSERT INTO "+tableName+" (host,ip,ipNum,startTime,endTime,type,remoteId,dppInstance) VALUES(?,?,?,?,?,?,?,?)";
    //INSERT_NEW   = INSERT_PART+" ON DUPLICATE KEY UPDATE ip = values(ip), ipNum = values(ipNum), startTime = values(startTime), endTime = values(endTime), type = values(type), remoteId = values(remoteId), dppInstance = values(dppInstance)";
    //INSERT_RENEW = INSERT_PART+" ON DUPLICATE KEY UPDATE ip = values(ip), ipNum = values(ipNum), endTime = values(endTime), type = values(type), remoteId = values(remoteId), dppInstance = values(dppInstance)";
    INSERT_PART = "INSERT INTO "+tableName+" VALUES(?,?,?,?,?,?,?,?,?,?)";
    //INSERT_NEW   = INSERT_PART+" ON DUPLICATE KEY UPDATE ip=values(ip),ipNum=values(ipNum),startTime=values(startTime),endTime=values(endTime),type=values(type),remoteId=values(remoteId),dppInstance=values(dppInstance)";
    INSERT_RENEW = INSERT_PART+" ON DUPLICATE KEY UPDATE ip=values(ip),ipNum=values(ipNum),startTime=values(startTime),endTime=values(endTime),type=values(type),dppInstance=values(dppInstance)";

    INSERT_NEW = "REPLACE INTO "+tableName+" VALUES(?,?,?,?,?,?,?,?,?,?)";
    //INSERT_RENEW = "INSERT INTO "+tableName+" (host,ip,ipNum,endTime,duration,type,dppInstance) VALUES(?,?,?,?,?,?,?)";

    
    DELETE = "DELETE FROM "+tableName+" WHERE host=?";

    dbFiller.cacheStatement( INSERT_NEW );
    dbFiller.cacheStatement( INSERT_RENEW );
    dbFiller.cacheStatement( DELETE );
  
  
    INSERT_PACKETS = "INSERT INTO "+tableNamePackets+" VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE ip=values(ip), inTime=values(inTime), discover=values(discover), offer=values(offer)";
    dbFiller.cacheStatement( INSERT_PACKETS );

  
  }

  public SQLData processData(DhcpData dhcpData) {
    //System.err.println( dhcpData ); 

//    logger.info("processData aufgerufen mit: "+dhcpData);
    if(dhcpData.getDiscover()!=null && dhcpData.getDiscover().length()>0)
    {
      logger.debug("Discover in Data not null: creating dhcpv4packets entry!");
      return processInsertPacket(dhcpData); // bei neuen Feldern auch in dhcpv4packets eintragen
    }

    
    if( dhcpData.isInsert() ) {
      String vci = dhcpData.getVci();
      if( vci != null && vci.length() > 20 ) {
        vci = vci.substring(0,20);
      }
      
      
      Parameter params = new Parameter(
                               dhcpData.getHostname(), 
                               dhcpData.getCiaddr().toString(),
                               dhcpData.getCiaddr().toLong(),
                               dhcpData.getStartTime(),
                               dhcpData.getEndTime(), 
                               "0", //duration
                               vci,
                               dhcpData.getRemoteId() == null ? null : dhcpData.getRemoteId(),
                               dhcpData.getDppInstance(),
                               dhcpData.getGiaddr()
                           );

      if( dhcpData.getAction() == DhcpAction.DHCPREQUEST_NEW ) {
        return new SQLData( INSERT_NEW, params, 3 );
      }
      if( dhcpData.getAction() == DhcpAction.DHCPREQUEST_RENEW ) {
        return new SQLData( INSERT_RENEW, params, 3 ); //siehe auch Bug http://bugs.mysql.com/bug.php?id=42087
      }
    }
    if( dhcpData.isDelete() ) {
      Parameter params = new Parameter( dhcpData.getHostname() );
      return new SQLData( DELETE, params, 1 );
    }
    return null; //sollte nicht auftreten
  }

  public void initialize(SQLUtils sqlUtils) {
//    String sql = "SELECT nonCpeSubnet from noncpesubnets";
//    subnets = sqlUtils.query(sql, null, ResultSetReaderFactory.getStringReader() );
//    sqlUtils.commit();//Transaktion beenden
//    fillNonCpeSubnets(); //füllt NonCpeSubnets, wenn NonCpeSubnets bereits gesetzt
    //nichts zu tun
  }
  
  /**
   * Eintragen der NonCpeSubnets in die übergebene Liste
   * @param nonCpeSubnets
   */
  public void fillNonCpeSubnets(@SuppressWarnings("hiding") ArrayList<Subnet> nonCpeSubnets) {
    this.nonCpeSubnets = nonCpeSubnets;
    fillNonCpeSubnets();//füllt NonCpeSubnets, wenn subnets bereits gesetzt
  }

  /**
   * Initialisierung der MtaSubnets
   */
  private void fillNonCpeSubnets() {
    if( subnets == null || nonCpeSubnets == null ) {
      return; //kann noch nicht initialisieren
    }
    for( String s : subnets ) {
      try {
        nonCpeSubnets.add( Subnet.parse(s) );
      } catch( Subnet.InvalidSubnetException e ) {
        logger.error( e );
      }
    }
    logger.info( "Read NonCpeSubnets: "+nonCpeSubnets );
  }
  
  private dhcpAdapterDemon.db.dbFiller.DataProcessor.SQLData processInsertPacket(DhcpData dhcpData) {
    Parameter params = new Parameter(dhcpData.getChaddr().toHex(), 
                                     dhcpData.getCiaddr().toString(),
                                     new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()),//inTime
                                     dhcpData.getDiscover(),
                                     dhcpData.getOffer()
    );
    return new SQLData( INSERT_PACKETS, params, 3 );    
  }


}
