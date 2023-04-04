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

import java.text.SimpleDateFormat;
import java.util.Date;

import com.gip.xyna.demon.DemonProperties;

import snmpTrapDemon.leases.MacAddress;
import snmpTrapDemon.poolUsage.IPAddress;
import dhcpAdapterDemon.DhcpData;
import dhcpAdapterDemon.db.leaselog.FilePersistentList.StringPersistableFactory;
import dhcpAdapterDemon.types.DhcpAction;


/**
 * Zusammenstellung aller Daten, die in die LeaseLog-DB eingetragen werden müssen.
 * 
 * Mehrere LeaseLogData werden zusammen in LeasLogPacket eingetragen und sind auch als
 * Zeilen in den Dateien der persistenten Speicherung im Filesystem zu finden. 
 *
 */
public class LeaseLogData {
  
  public static enum Data {
    IP_ADDR, START_TM, ACT_CD, MAC_ADDR, END_TM, IS_RESERVED, HOST_TYPE, CM_MAC_ADDR;
  }
  private static final int NUM_DATA = Data.values().length;
  private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  private static LeaseLogDataFactory leaseLogDataFactory = new LeaseLogDataFactory();
  
  private static String DEFAULT_REMOTE_ID=DemonProperties.getProperty("remoteid.to.be.set","00:00:00:00:00:00");

  public static class LeaseLogDataFactory implements StringPersistableFactory<LeaseLogData> {
    
    /* (non-Javadoc)
     * @see dhcpAdapterDemon.db.leaselog.FilePersistentList.StringPersistableFactory#asString(java.lang.Object)
     */
    public String asString(LeaseLogData entry) {
      StringBuilder sb = new StringBuilder(100);
      sb.append(entry.data[0]);
      for( int d=1; d<entry.data.length; ++d ) {
        sb.append('\t');
        if( entry.data[d] != null ) {
          sb.append(entry.data[d]);
        }
      }
      return sb.toString();
    }
    /* (non-Javadoc)
     * @see dhcpAdapterDemon.db.leaselog.FilePersistentList.StringPersistableFactory#fromString(java.lang.String)
     */
    public LeaseLogData fromString(String string) {
      String[] parts = string.split("\t",NUM_DATA);
      if( parts.length != NUM_DATA ) {
        throw new IllegalArgumentException("Could not parse string, invalid number of fields");
      }
      LeaseLogData lld = new LeaseLogData();
      lld.data = parts;
      String actCD = lld.data[Data.ACT_CD.ordinal()];
      lld.insert = actCD.equals("1") || actCD.equals("2");
      
      return lld;
    }
    
  }
    
  private String[] data = new String[Data.values().length];
  private boolean insert;
  
  /**
   * Default-Konstruktor
   */
  private LeaseLogData() {/*internal use*/}
    
  /**
   * Konstruktor: Extraktion der benötigten Daten aus den DhcpData
   * @param dhcpData
   */
  public LeaseLogData(DhcpData dhcpData) {
    data[Data.IP_ADDR.ordinal()] = formatIp( dhcpData.getCiaddr() );
    data[Data.START_TM.ordinal()] = formatTm( dhcpData.getStartTime() );
    data[Data.ACT_CD.ordinal()] = inferActCd( dhcpData.getAction() );
    insert = dhcpData.isInsert();
    if( insert ) {
      data[Data.MAC_ADDR.ordinal()] = formatMac( dhcpData.getChaddr() );
      data[Data.END_TM.ordinal()] =   formatTm( dhcpData.getEndTime() );
      data[Data.IS_RESERVED.ordinal()] = dhcpData.isReserved() ? "1" : "0"; 
      data[Data.HOST_TYPE.ordinal()] =  inferHostType( dhcpData.getType() );
      String remoteId=dhcpData.getRemoteId();
      if (remoteId==null){
        remoteId=DEFAULT_REMOTE_ID;
      }
      data[Data.CM_MAC_ADDR.ordinal()] = remoteId;  
    }    
  }

  /**
   * Bestimmung des HostType
   * @param type
   * @return
   */
  private String inferHostType(String type) {
    if( type == null || type.length() == 0 ) {
      return "1"; //CPE
    }
    if( type.startsWith( "pktc") ) {
      return "3"; //MTA
    }
    if( type.startsWith( "docsis") ) {
      return "2"; //Cable Modem
    }
    //FIXME
    return "1"; //CPE;
  }

  /**
   * Bestimmung des ActivationCodes
   * @param action
   * @return
   */
  private String inferActCd(DhcpAction action) {
    switch( action ) {
    case DHCPREQUEST_NEW: return "1";
    case DHCPREQUEST_RENEW: return "2";
    case DHCPRELEASE: return "3";
    case LEASEEXPIRE: return "4";
    default:
      throw new IllegalStateException("Unexpected DhcpAction "+action); //FIXME
    }
  }
  /**
   * Ermittlung der DhcpAction aus dem gespeicheren ACT_CD für Statistik
   * @return DhcpAction
   */
  public DhcpAction getDhcpAction() {
    String actCd = get( Data.ACT_CD );
    if( actCd.equals("1") ) {
      return DhcpAction.DHCPREQUEST_NEW;
    } else if ( actCd.equals("2") ) {
      return DhcpAction.DHCPREQUEST_RENEW;
    } else if ( actCd.equals("3") ) {
      return DhcpAction.DHCPRELEASE;
    } else if ( actCd.equals("4") ) {
      return DhcpAction.LEASEEXPIRE;
    } else {
      return DhcpAction.IGNORE;
    }
  }
  
  /**
   * @param mac
   * @return
   */
  private String formatMac( MacAddress mac) {
    return mac.toHex();
  }

  /**
   * @param time
   * @return
   */
  private String formatTm(String time) {
    if( time == null || time.length() == 0 ) {
      return sdf.format(new Date());
    }
    return time;
  }

  /**
   * @param ip
   * @return
   */
  private String formatIp(IPAddress ip) {
    int[] parts = ip.toInts();    
    StringBuilder sb = new StringBuilder();
    for( int i=0; i<4; ++i ) {
      sb.append( parts[i] <16? "0":"").append( Integer.toHexString( parts[i] ) );
    }
    return sb.toString();   
  }

  /**
   * @return
   */
  public boolean isInsert() {
    return insert; 
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(100);
    sb.append(data[0]);
    for( int d=1; d<data.length; ++d ) {
      sb.append('\t');
      if( data[d] != null ) {
        sb.append(data[d]);
      }
    }
    return sb.toString();
  }

  /**
   * @param d
   * @return
   */
  public String get(Data d) {
    return data[d.ordinal()];
  }

  /**
   * @return
   */
  public static StringPersistableFactory<LeaseLogData> getFactory() {  
    return leaseLogDataFactory;
  }
  
}
