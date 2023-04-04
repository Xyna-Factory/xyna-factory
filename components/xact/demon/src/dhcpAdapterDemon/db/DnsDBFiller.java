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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.db.OutputParam;
import com.gip.xyna.utils.db.OutputParamFactory;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.ResultSetReaderFactory;
import com.gip.xyna.utils.db.ResultSetReaderFunction;
import com.gip.xyna.utils.db.SQLUtils;

import dhcpAdapterDemon.DhcpData;
import dhcpAdapterDemon.db.dbFiller.DBFillerData;
import dhcpAdapterDemon.types.DhcpAction;
import snmpTrapDemon.poolUsage.IPAddress;
import snmpTrapDemon.poolUsage.Subnet;

public class DnsDBFiller extends DhcpDataDBFillerImpl {
  final static Logger logger = Logger.getLogger(DnsDBFiller.class);
   
  private static final String UPSERT = "{CALL dns.upsertRecords(?,?,?,?,?,?,?)}";
  private static final String DELETE = "DELETE FROM records WHERE host=? OR host=?";
  private static Integer DEFAULT_TTL;
  private static Integer TTL_OFFSET;
  
  private HashMap<String,String> zones = new HashMap<String,String>();
  //private HashMap<String,String> mxPriorities = new HashMap<String,String>();
  private HashMap<String,Integer> ttls = new HashMap<String,Integer>();
  
  private ArrayList<Subnet> mtaSubnets;
  private ArrayList<String> subnets;
  

  /**
   * @param name
   * @param dbFillerData
   */
  public DnsDBFiller(String name, DBFillerData dbFillerData) {
    super(name,dbFillerData,logger);
    dbFiller.cacheStatement( UPSERT );
    dbFiller.cacheStatement( DELETE );
    DEFAULT_TTL = DemonProperties.getIntProperty("db.dns.default.ttl",86400);
    TTL_OFFSET = DemonProperties.getIntProperty("db.dns.ttl.offset",0);
  }

  public SQLData processData(DhcpData dhcpData) {
    //System.err.println( dhcpData );
    if( dhcpData.isInsert() ) {
      String zone = zones.get( dhcpData.getDppInstance() );
      //String mxPriority = mxPriorities.get( dhcpData.getDppInstance() );
      if( zone == null ) {
        logger.error( "Could not retrieve zone for dppInstance "+dhcpData.getDppInstance() );
        return null; 
      }
      Integer ttl = ttls.get( dhcpData.getDppInstance() );
      if( ttl == null ) {
        ttl = DEFAULT_TTL;
      }
      ttl=ttl+TTL_OFFSET;
      
      OutputParam<Integer> out = OutputParamFactory.createInteger();
      Parameter params = new Parameter(
          dhcpData.getHostname(), 
          zone, 
          ttl,
          null, //mxPriority darf nicht gesetzt werden
          dhcpData.getCiaddr().toString(),
          reverseIp( dhcpData.getCiaddr() ),
          out
          );
      if( dhcpData.getAction() == DhcpAction.DHCPREQUEST_NEW ) {
        return SQLData.newCall( UPSERT, params, out, 11 );
      }
      if( dhcpData.getAction() == DhcpAction.DHCPREQUEST_RENEW ) {
        return SQLData.newCall( UPSERT, params, out, 22 );
      }
    }
    if( dhcpData.isDelete() ) {
      Parameter params = new Parameter( dhcpData.getHostname(), reverseIp( dhcpData.getCiaddr() ) );
      return SQLData.newDML( DELETE, params, 2 );
    }
    return null; //sollte nicht auftreten
  }

  /**
   * Umdrehen der IP 10.2.5.11 -> 11.5.2.10 
   * @param ip
   * @return
   */
  private String reverseIp(IPAddress ip) {
    int[] iap = ip.toInts();
    int p;
    p = iap[0]; iap[0] = iap[3]; iap[3] = p; 
    p = iap[1]; iap[1] = iap[2]; iap[2] = p; 
    return IPAddress.parse(iap).toString();
  }

  public void initialize(SQLUtils sqlUtils) {
    //Lesen der Zonen aus der DB
    String sql 
      = "SELECT z.zone, z.dppInstance, z.mx_priority, s.ttl"
      + "  FROM zones z"
      + "  LEFT OUTER JOIN soansrecords s ON s.zone = z.zone";
    sqlUtils.query(sql, null, new ResultSetReaderFunction() {
      public boolean read(ResultSet rs) throws SQLException {
        String dpps = rs.getString("dppInstance");
        for( String dpp :  dpps.split(",") ) {
          dpp = dpp.trim();
          zones.put( dpp, rs.getString("zone") );
          //mxPriorities.put( dpp, rs.getString("mx_priority") );
          int ttl = rs.getInt("ttl");
          ttls.put( dpp, ttl > 0 ? Integer.valueOf(ttl) : null );
        }
        return true;
      }} );
    logger.info( "Read zones from DB: "+zones );
    sql = "SELECT mtasubnet from mtasubnets";
    subnets = sqlUtils.query(sql, null, ResultSetReaderFactory.getStringReader() );
    sqlUtils.commit();//Transaktion beenden 
    fillMtaSubnets(); //füllt MtaSubnets, wenn mtaSubnets bereits gesetzt
  }

  /**
   * Eintragen der MtaSubnets in die übergebene Liste
   * @param mtaSubnets
   */
  public void fillMtaSubnets(@SuppressWarnings("hiding") ArrayList<Subnet> mtaSubnets) {
    this.mtaSubnets = mtaSubnets;
    fillMtaSubnets();//füllt MtaSubnets, wenn subnets bereits gesetzt
  }

  /**
   * Initialisierung der MtaSubnets
   */
  private void fillMtaSubnets() {
    if( subnets == null || mtaSubnets == null ) {
      return; //kann noch nicht initialisieren
    }
    mtaSubnets.clear();
    for( String s : subnets ) {
      try {
        mtaSubnets.add( Subnet.parse(s) );
      } catch( Subnet.InvalidSubnetException e ) {
        logger.error( e );
      }
    }
    logger.info( "Read MtaSubnets: "+mtaSubnets );
  }
 
}
