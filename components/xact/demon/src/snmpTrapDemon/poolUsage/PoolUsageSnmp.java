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

import java.util.HashMap;
import java.util.Set;

import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.utils.SparseSnmpTableModel;


/**
 * Realisierung der Snmp-Tabelle PoolUsage
 */
public class PoolUsageSnmp implements SparseSnmpTableModel {

  public enum Type { ALL, SUM } //da wenige im Wesentlichen identische Typen, 
                                //wegen Übersichtlichkeit keine Ableitungshierarchie, sondern Tool

  
  private interface Tool{
    public Object get(int row, int column);
    public int getColumnCount();
    public OID getStandardOid();
  }
    

  private Tool tool=null;
  public PoolUsageSnmp(Type t){
    switch (t){
      case ALL: tool=new AllTool();break;
      case SUM: tool=new SumTool();break;
    }
  }

  private HashMap<Integer,PoolUsageTable.Entry> poolUsageEntriesByPoolId = new HashMap<Integer,PoolUsageTable.Entry>();
  private PoolUsageTable poolUsageTable;
  private PoolUsageThreshold poolUsageThreshold;
  
  /**
   * Übergabe der aktualisierten Tabellendaten
   * @param poolUsageTable
   */
  public void setPoolUsageTable(PoolUsageTable poolUsageTable) {
    this.poolUsageTable = poolUsageTable;
    HashMap<Integer,PoolUsageTable.Entry> newMap = new HashMap<Integer,PoolUsageTable.Entry>();
    for( PoolUsageTable.Entry pue : poolUsageTable.getEntries() ) {
      newMap.put( pue.getPoolID(), pue );
    }
    poolUsageEntriesByPoolId = newMap;
  }
  
  /**
   * Übergabe der aktualisierten poolUsageThreshold zur Ermittlung der konfigurierten Schwellwerte
   * @param poolUsageThreshold
   */
  public void setPoolUsageThreshold(PoolUsageThreshold poolUsageThreshold) {
    this.poolUsageThreshold = poolUsageThreshold;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.SnmpTableModel#canSet(int, int)
   */
  public boolean canSet(int row, int column) {
    return false;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.SnmpTableModel#get(int, int)
   */
  public Object get(int row, int column) {
    return tool.get(row, column);
  }
    

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.SnmpTableModel#getColumnCount()
   */
  public int getColumnCount() {
    return tool.getColumnCount();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.SnmpTableModel#getRowCount()
   */
  public int getRowCount() {
    return poolUsageEntriesByPoolId.size();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.SnmpTableModel#set(int, int, java.lang.Object)
   */
  public boolean set(int row, int column, Object o) {
    return false;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.SparseSnmpTableModel#getRowIndexSet()
   */
  public Set<Integer> getRowIndexSet() {
    return poolUsageEntriesByPoolId.keySet();
  }
  
  public OID getStandardOid() {    
    return tool.getStandardOid();
  }

  
  private class AllTool implements Tool{

    public final OID OID_POOLUSAGE = new OID(".1.3.6.1.4.1.28747.1.13.3.1.1.1");
                                               
    


    private final int INDEX = 1;
    private final int CMTS = 2;
    private final int POOLTYPE = 3;
    private final int SIZE = 4;
    private final int USEDSIZE = 5;
    private final int USAGE_ALL = 6;
    private final int THRESHOLD = 7;


    public Object get(int row, int column) {
      PoolUsageTable.Entry pue = poolUsageEntriesByPoolId.get( row );
      if( pue == null ) {
        return null;
      }

      switch( column ) {
        case INDEX: return Integer.valueOf(pue.getPoolID() );
        case CMTS: return poolUsageTable.getSharedNetwork( pue.getSharedNetworkID() );
        case POOLTYPE: return poolUsageTable.getPoolType( pue.getPoolTypeID() );
        case SIZE: return  pue.getSize();
        case USEDSIZE: return pue.getUsed();
        case USAGE_ALL: return Integer.valueOf( (int)(1000* pue.getUsedFraction() ) );
        case THRESHOLD: 
          if( pue.getPoolID() < PoolUsage.POOL_SUM_OFFSET ) {
            return Integer.valueOf(0);
          } else {
            return Integer.valueOf( (int)(1000* poolUsageThreshold.getThreshold( pue.getSharedNetworkID(), pue.getPoolTypeID() )) );
          }
      }
      return null;
    }

    public int getColumnCount() {
      return THRESHOLD;
    }

    public OID getStandardOid() {
      return OID_POOLUSAGE;
    }
  }

  private class SumTool implements Tool{

    public final OID OID_POOLUSAGE_SUM = new OID(".1.3.6.1.4.1.28747.1.13.3.1.1.2");

    private final int INDEX = 1;
    private final int CMTS = 2;
    private final int POOLTYPE = 3;
    private final int USAGE_SUM = 4; //Achtung, bei SUM andere Indexierung als bei ALL

    public Object get(int row, int column) {
      PoolUsageTable.Entry pue = poolUsageEntriesByPoolId.get( row );
      if( pue == null) {
        return null;
      }

      switch( column ) {
        case INDEX: return Integer.valueOf(pue.getPoolID() );
        case CMTS: return poolUsageTable.getSharedNetwork( pue.getSharedNetworkID() );
        case POOLTYPE: return poolUsageTable.getPoolType( pue.getPoolTypeID() );
        case USAGE_SUM: return Integer.valueOf( (int)(1000* pue.getUsedFraction() ) );
      }
      return null;
    }

    public int getColumnCount() {
      return USAGE_SUM;
    }

    public OID getStandardOid() {
      return OID_POOLUSAGE_SUM;
    }

  }

}
