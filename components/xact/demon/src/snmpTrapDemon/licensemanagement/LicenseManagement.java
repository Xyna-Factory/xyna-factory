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
package snmpTrapDemon.licensemanagement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;



import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.failover.Failover;
import com.gip.xyna.utils.db.failover.FailoverDBConnectionData;
import com.gip.xyna.utils.snmp.OID;
import com.gip.xyna.utils.snmp.agent.utils.AbstractOidSingleHandler.SnmpCommand;
import com.gip.xyna.utils.snmp.agent.utils.OidSingleHandler;
import com.gip.xyna.utils.snmp.varbind.*;


import dhcpAdapterDemon.db.SQLUtilsLoggerImpl;

/*
 * unter OID .1.3.6.1.4.1.28747.1.11.9.1.1.0-3
 * 
 
 */

public class LicenseManagement implements OidSingleHandler {
  
  private SQLUtils sqlUtils;
  private Failover failover;
  private SQLUtilsLoggerImpl sqlUtilsLogger;
  private FailoverDBConnectionData dbConData;
  
  private ArrayList<Data> dataList =null;
  long lastread=0;
    
  private SQLUtils getSqlUtils() {
    boolean isValid = false;
    if( sqlUtils != null ) {
      try {
        isValid = sqlUtils.getConnection().isValid(0);
      } catch (SQLException e) {
        sqlUtilsLogger.logException(e);
        isValid = false;
      }
    }
    if( isValid ) {
      sqlUtils = failover.checkAndRecreate( sqlUtils, sqlUtilsLogger );
    } else {
      sqlUtils = failover.recreateSQLUtils(sqlUtils, sqlUtilsLogger);
    }
    return sqlUtils;
  }
  
  public static final OID OID_LICENSE_MANAGEMENT    = new OID(".1.3.6.1.4.1.28747.1.11.9.1");
  public static final OID OID_WALK_END = new OID(".1.3.6.1.4.1.28747.1.11.9.2");
  static Logger logger = Logger.getLogger(LicenseManagement.class.getName());
  
  
  private static class Data {
    int count;
    String name;
  }
  private static class Reader implements ResultSetReader<Data> {

    public Data read(ResultSet rs) throws SQLException {
      Data data=new Data();
      data.count=rs.getInt(1);
      data.name=rs.getString(2);
      return data;
    }
  }
  

  public VarBind get(OID oid, int arg1) {
    if (lastread+1000*60*5<System.currentTimeMillis()||dataList==null){
      lastread=System.currentTimeMillis();
      String tableName = DemonProperties.getProperty("db.snmpTrap.db.tablename");
      if(tableName == null) {
        throw new RuntimeException("Property 'db.snmpTrap.db.tablename' not set.");
      }
      String sql = "SELECT count(*),devicestatus0 from " + tableName + ".device group by devicestatus0";
      dataList =getSqlUtils().query( sql, new Parameter(), new Reader() ); 
    }
    for(Data data:dataList){
        if(data.name != null) { // there might be entries with null values
            if (data.name.equals("installing") && oid.toString().endsWith("0")){
                return new IntegerVarBind(OID_LICENSE_MANAGEMENT.getOid()+".0",data.count);
            }
            if (data.name.equals("working") && oid.toString().endsWith("1")){
                return new IntegerVarBind(OID_LICENSE_MANAGEMENT.getOid()+".1",data.count);
            }
            if (data.name.equals("retiring") && oid.toString().endsWith("2")){
                return new IntegerVarBind(OID_LICENSE_MANAGEMENT.getOid()+".2",data.count);
            }      
            if (data.name.equals("planning") && oid.toString().endsWith("3")){
                return new IntegerVarBind(OID_LICENSE_MANAGEMENT.getOid()+".3",data.count);
            }      
        }
    }
    return null;
  }

  public VarBind getNext(OID oid, VarBind arg1, int i) {   

    int index = 1+Integer.parseInt(oid.subOid(oid.length()-1).toString().replaceAll("\\.", ""));    
    if( index > 3)  {
        return new IntegerVarBind( OID_WALK_END.getOid(),0);
    }   
    if (oid.length()==OID_LICENSE_MANAGEMENT.length()){
      index=0;
    }
    OID responseOid =new OID(OID_LICENSE_MANAGEMENT.toString()+"."+index);
    return get(responseOid,i);
  }

  public void inform(OID arg0, int arg1) {    
  }

  public boolean matches(SnmpCommand snmpCommand, OID oid) {
    if( ! oid.startsWith(OID_LICENSE_MANAGEMENT) ) {
      return false;
    }
    switch( snmpCommand ) {
      case GET: return true;
      case GET_NEXT: return true;
    }
    return false;
  }

  public void set(OID arg0, VarBind arg1, int arg2) {
  }

  public void trap(OID arg0, int arg1) {    
  }
  
  public LicenseManagement(){
    DBConnectionData cd = DemonProperties.getDBProperty( "snmpTrap" );
    dbConData = DemonProperties.getFailoverDBProperty( "snmpTrap", cd );
    
    failover = dbConData.createNewFailover();  
    sqlUtilsLogger= new SQLUtilsLoggerImpl(logger);
  }
  
}
