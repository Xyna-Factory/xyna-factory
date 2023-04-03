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
package dhcpAdapterDemon.snmp;

import java.util.EnumMap;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.snmp.NextOID;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.NextOID.NoNextOIDException;
import com.gip.xyna.utils.snmp.agent.utils.ChainedOidSingleHandler;
import com.gip.xyna.utils.snmp.agent.utils.MultiIndexSnmpTable;
import com.gip.xyna.utils.snmp.agent.utils.MultiIndexSnmpTableModel;

import dhcpAdapterDemon.db.DBFillerStatistics;
import dhcpAdapterDemon.types.Database;
import dhcpAdapterDemon.types.DhcpAction;
import dhcpAdapterDemon.types.State;

public class DhcpCounter implements MultiIndexSnmpTableModel {
  
  public static final OID OID_COUNTER = new OID(".1.3.6.1.4.1.28747.1.13.1.1.1");
  
  private boolean dodns;
  private boolean dolegalintercept;

  private static enum Entry {
    DATABASE,
    DHCPACTION,
    STATE,
    COUNTER,
    COUNTER_STRING;
    

    
    public static Entry fromSnmpIndex(String index) {
      return values()[ Integer.parseInt( index ) -1 ];
    }
  }
  
  private EnumMap<Database, DBFillerStatistics> dbFillerStatistics = new EnumMap<Database, DBFillerStatistics>(Database.class);
  private static final NextOID nextOID = new NextOID( Entry.values().length, Database.values().length, DhcpAction.values().length, State.values().length );
  private MultiIndexSnmpTable snmpTable;
  
  public DhcpCounter() {
    snmpTable = new MultiIndexSnmpTable(this,OID_COUNTER);
    
    dodns = DemonProperties.getBooleanProperty("db.doDNS", true);
    dolegalintercept = DemonProperties.getBooleanProperty("db.doLegalIntercept", true);

  }

  /**
   * @param db
   * @param statistics
   */
  public void setDbFillerStatistics(Database db, DBFillerStatistics statistics) {
    if(db==Database.DNS && dodns==false)return;
    if(db==Database.LEASELOG && dolegalintercept==false)return;
    dbFillerStatistics.put( db, statistics );
  }
 
  /**
   * @return
   */
  public ChainedOidSingleHandler getOidSingleHandler() {
    return snmpTable;
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.MultiIndexSnmpTableModel#get(com.gip.xyna.utils.snmp.OID)
   */
  public Object get(OID oid) {
    Entry entry = Entry.fromSnmpIndex(oid.getIndex(0));
    Database db = Database.fromSnmpIndex(oid.getIndex(1) );
    DhcpAction dhcpAction = DhcpAction.fromSnmpIndex(oid.getIndex(2));
    State state = State.fromSnmpIndex(oid.getIndex(3));
   
    if(db==Database.DNS && dodns==false)return null;
    if(db==Database.LEASELOG && dolegalintercept==false)return null;

    
    int value;
    switch( entry ) {
    case DATABASE: return Integer.valueOf( db.ordinal()+1 );
    case DHCPACTION:return Integer.valueOf( dhcpAction.ordinal()+1 );
    case STATE:    return Integer.valueOf( state.ordinal()+1 );
    case COUNTER:
      value = dbFillerStatistics.get(db).getCounter( dhcpAction, state );
      return Integer.valueOf(value);
    case COUNTER_STRING:
      value = dbFillerStatistics.get(db).getCounter( dhcpAction, state );
      return "Counter "+db+" "+dhcpAction+" "+state+": "+value;
    default:
      return null;
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.MultiIndexSnmpTableModel#getNextOID(com.gip.xyna.utils.snmp.OID)
   */
  public OID getNextOID(OID oid) {
    try {
      return nextOID.getNext(oid);
    } catch (NoNextOIDException e) {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.MultiIndexSnmpTableModel#numIndexes()
   */
  public int numIndexes() {
    return 3; //Database, DhcpAction, State
  }

}
