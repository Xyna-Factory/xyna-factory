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

import java.text.DecimalFormat;
import java.util.EnumMap;

import com.gip.xyna.utils.snmp.NextOID;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.NextOID.NoNextOIDException;
import com.gip.xyna.utils.snmp.agent.utils.ChainedOidSingleHandler;
import com.gip.xyna.utils.snmp.agent.utils.MultiIndexSnmpTable;
import com.gip.xyna.utils.snmp.agent.utils.MultiIndexSnmpTableModel;

import dhcpAdapterDemon.db.DBFillerStatistics;
import dhcpAdapterDemon.db.dbFiller.DBFiller.Status;
import dhcpAdapterDemon.types.Database;
import dhcpAdapterDemon.types.Type;

public class DhcpDatabases implements MultiIndexSnmpTableModel {
  
  public static final OID OID_DATABASES  = new OID(".1.3.6.1.4.1.28747.1.13.1.3");

  private EnumMap<Database, DBFillerStatistics> dbFillerStatistics = new EnumMap<Database, DBFillerStatistics>(Database.class);
  private static final NextOID nextOID = new NextOID( Type.values().length, 1, Entry.values().length, Database.values().length );
  private MultiIndexSnmpTable snmpTable;

  
  private static enum Entry {
    DATABASE,
    STATE,
    RECONNECTED,
    BUFFER_SIZE,
    BUFFER_USED,
    BULK_COMMIT,
    LAST_SQL_EXCEPTION,
    CONNECT_STRING,
    DEADLOCK; 
    
    public static Entry fromSnmpIndex(String index) {
      return values()[ Integer.parseInt( index ) -1 ];
    }

  }
  
  public DhcpDatabases() {
    snmpTable = new MultiIndexSnmpTable(this,OID_DATABASES);
  }
  
  /**
   * @param db
   * @param statistics
   */
  public void setDbFillerStatistics(Database db, DBFillerStatistics statistics) {
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
    Type  type = Type.fromSnmpIndex(oid.getIndex(0));
    Entry entry = Entry.fromSnmpIndex(oid.getIndex(2));
    Database db = Database.fromSnmpIndex(oid.getIndex(3));
    switch( entry ) {
    case DATABASE:
      return Integer.valueOf( db.ordinal()+1 );
    case STATE:
      Status status = dbFillerStatistics.get(db).getStatus();
      return getEntry( db, type, status.toString(), status.toInt() );
    case RECONNECTED:
      return getEntry( db, type, "number of reconnects", dbFillerStatistics.get(db).getNumReconnects() );     
    case BUFFER_SIZE:
      return getEntry( db, type, "size of buffer", dbFillerStatistics.get(db).getBufferSize() );
    case BUFFER_USED:
      return getEntry( db, type, "current usage of buffer", dbFillerStatistics.get(db).getWaiting() );
    case BULK_COMMIT:
      float abcs = dbFillerStatistics.get(db).getAverageBulkCommitSize();
      if( type == Type.INT ) {
        return Integer.valueOf( (int)(100*abcs) );
      } else {
        DecimalFormat df = new DecimalFormat("#####0.00");
        return db+", average bulk size on commit: "+ df.format(abcs);
      }
    case LAST_SQL_EXCEPTION:
      String lastEx = dbFillerStatistics.get(db).getLastException();
      if( type == Type.INT ) {
        return Integer.valueOf( lastEx == null ? 0 : 1 );
      } else {
        return db+", lastException: "+lastEx;
      }
    case CONNECT_STRING:
      if( type == Type.INT ) {
        return Integer.valueOf( 0 );
      } else {
        return db+", "+dbFillerStatistics.get(db).getConnectString();
      }      
    case DEADLOCK:
      return getEntry( db, type, "number of deadlocks", dbFillerStatistics.get(db).getNumDeadlocks() );
    default:
      return null;
    }
  }

  /**
   * @param db
   * @param type
   * @param name
   * @param value
   * @return
   */
  private Object getEntry(Database db, Type type, String name, int value) {
    if( type == Type.INT ) {
      return Integer.valueOf( value );
    } else {
      return db+", "+name+": "+value;
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
    return 3; //fix, Entry, Database
  }

}
