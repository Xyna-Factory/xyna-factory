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
package snmpTrapDemon.poolUsage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.ResultSetReaderFunction;
import com.gip.xyna.utils.db.SQLUtils;

/**
 * Abbildung der kommpletten DB-Tabelle PoolUsage samt zweier Lookups.
 * Wird für Snmp-Abfragen und die Prüfung der Schwellwertüberschreitung benötigt
 */
public class PoolUsageTable {

  private HashMap<String, String> poolTypes;
  private HashMap<String, String> sharedNetworks;
  private ArrayList<Entry> poolUsageEntries;
  
  /**
   * Lesen der DB-Daten
   * @param sqlUtils
   */
  public void read(SQLUtils sqlUtils) {
    poolTypes = new HashMap<String,String>();
    sqlUtils.query( 
        "SELECT poolTypeID, name FROM dhcp.pooltype",
        null, new ResultSetToMapReader(poolTypes) );
    sharedNetworks = new HashMap<String,String>();
    sqlUtils.query( 
        "SELECT sharedNetworkID, sharednetwork FROM dhcp.sharednetwork", 
        null, new ResultSetToMapReader(sharedNetworks) );
    poolUsageEntries = sqlUtils.query( 
        "SELECT poolID, sharedNetworkID, poolTypeID, size, used, usedFraction FROM poolusage", 
        null, new PoolUsageEntryReader() );    
  }
  
  /**
   * Hilfsobjekt zum Lesen der Tabellen-Entries
   */
  private static class PoolUsageEntryReader implements ResultSetReader<Entry> {
    public Entry read(ResultSet rs) throws SQLException {
      Entry pue = new Entry();
      pue.poolID = rs.getInt(1);
      pue.sharedNetworkID = rs.getString(2);
      pue.poolTypeID = rs.getString(3);
      pue.size = rs.getInt(4);
      pue.used = rs.getInt(5);
      pue.usedFraction = rs.getFloat(6);
      return pue;
    }
  }

  /**
   * Hilfsobjekt zum Lesen der Lookup-Maps
   */
  public class ResultSetToMapReader implements ResultSetReaderFunction {
    private HashMap<String,String> map;
    
    public ResultSetToMapReader( HashMap<String,String> map ) {
      this.map = map;
    }
    
    public boolean read(ResultSet rs) throws SQLException {
      map.put( rs.getString(1), rs.getString(2) );
      return true;
    }    
  }


  /**
   * @return
   */
  public List<Entry> getEntries() {
    return poolUsageEntries;
  }


  /**
   * @param sharedNetworkID
   * @return
   */
  public String getSharedNetwork(String sharedNetworkID) {
    return sharedNetworks.get(sharedNetworkID);
  }


  /**
   * @param poolTypeID
   * @return
   */
  public String getPoolType(String poolTypeID) {
    return poolTypes.get(poolTypeID);
  }

  /**
   * Abbildung einer Tabellenzeile aus der DB-Tabelle PoolUsage
   */
  public static class Entry {
    private int poolID;
    private String sharedNetworkID;
    private String poolTypeID;
    private int size;
    private int used;
    private float usedFraction;

    /**
     * @return the sharedNetworkID
     */
    public String getSharedNetworkID() {
      return sharedNetworkID;
    }
    /**
     * @return the poolTypeID
     */
    public String getPoolTypeID() {
      return poolTypeID;
    }
    /**
     * @return the usedFraction
     */
    public float getUsedFraction() {
      return usedFraction;
    }
    /**
     * @return the poolID
     */
    public int getPoolID() {
      return poolID;
    }
       
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append( "PoolUsageEntry(");
      sb.append(poolID).append(',');
      sb.append(sharedNetworkID).append(',');
      sb.append(poolTypeID).append(',');
      sb.append(size).append(',');
      sb.append(used).append(',');
      sb.append(usedFraction).append(')');
      return sb.toString();
    }
    /**
     * @return the size
     */
    public int getSize() {
      return size;
    }
    /**
     * @return the used
     */
    public int getUsed() {
      return used;
    }

  }


  public void readSum(SQLUtils sqlUtils) {
      poolTypes = new HashMap<String,String>();
    sqlUtils.query( 
        "SELECT poolTypeID, name FROM dhcp.pooltype",
        null, new ResultSetToMapReader(poolTypes) );
    sharedNetworks = new HashMap<String,String>();
    sqlUtils.query( 
        "SELECT sharedNetworkID, sharednetwork FROM dhcp.sharednetwork", 
        null, new ResultSetToMapReader(sharedNetworks) );
    poolUsageEntries = sqlUtils.query( 
        "SELECT poolID, sharedNetworkID, poolTypeID, size, used, usedFraction FROM poolusage WHERE poolID>=1000000", 
        null, new PoolUsageEntryReader() );    
    
  }

  
}
