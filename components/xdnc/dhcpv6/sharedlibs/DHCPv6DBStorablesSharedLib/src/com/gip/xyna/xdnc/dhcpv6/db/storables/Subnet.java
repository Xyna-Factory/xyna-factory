/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xdnc.dhcpv6.db.storables;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = Subnet.COL_SUBNETID, tableName = Subnet.TABLENAME)
public class Subnet extends Storable<Subnet> {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(Subnet.class);
  
  public static final String TABLENAME = "subnet";
  public static final String COL_SUBNETID = "subnetID";
  public static final String COL_SHAREDNETWORKID = "sharedNetworkID";
  public static final String COL_SUBNET = "subnet";
  public static final String COL_FIXEDATTRIBUTES = "fixedAttributes";
  public static final String COL_ATTRIBUTES = "attributes";
  public static final String COL_MODIFICATIONTIMESTAMP = "modificationTimestamp";
  
  @Column(name = COL_SUBNETID)
  private int subnetid;
  
  @Column(name = COL_SHAREDNETWORKID)
  private int sharednetworkid;
  
  @Column(name = COL_SUBNET)
  private String subnet;
  
  @Column(name = COL_FIXEDATTRIBUTES)
  private String fixedattributes;
  
  @Column(name = COL_ATTRIBUTES)
  private String attributes;
  
  @Column(name = COL_MODIFICATIONTIMESTAMP)
  private long modificationtimestamp; 

  
  public int getSubnetID(){
    return subnetid;
  }
  
  public int getSharedNetworkID(){
    return sharednetworkid;
  }

  public String getSubnet(){
    return subnet;
  }
  
  public String getFixedAttributes(){
    return fixedattributes;
  }
  
  public String getAttributes(){
    return attributes;
  }

  public long getModificationTimestamp(){
    return modificationtimestamp;
  }
  
  @Override
  public Object getPrimaryKey() {
    return subnetid;
  }

  public void setSubnetID(int subnetid){
    this.subnetid=subnetid;
  }
  
  public void setSharedNetworkID(int sharednetworkid){
    this.sharednetworkid=sharednetworkid;
  }

  public void setSubnet(String subnet){
    this.subnet=subnet;
  }
  
  public void setFixedAttributes(String fixedattributes){
    this.fixedattributes=fixedattributes;
  }
  
  public void setAttributes(String attributes){
    this.attributes=attributes;
  }

  public void setModificationTimestamp(long modificationtimestamp){
    this.modificationtimestamp=modificationtimestamp;
  }

  
  private static class SubnetReader implements ResultSetReader<Subnet> {

    public Subnet read(ResultSet rs) throws SQLException {
      Subnet sn = new Subnet();
      Subnet.fillByResultSet(sn, rs);
      return sn;
    }

  }
  
  private static final SubnetReader reader = new SubnetReader();
  
  @Override
  public ResultSetReader<? extends Subnet> getReader() {
    return reader;
  }
  
  @Override
  public <U extends Subnet> void setAllFieldsFromData(U data2) {
    Subnet data=data2;
    subnetid = data.subnetid;
    sharednetworkid = data.sharednetworkid;
    subnet = data.subnet; 
    fixedattributes = data.fixedattributes;
    attributes = data.attributes;
    modificationtimestamp = data.modificationtimestamp;
  }

  
  
  public static void fillByResultSet(Subnet sn, ResultSet rs) throws SQLException {
    sn.subnetid = rs.getInt(COL_SUBNETID);
    sn.sharednetworkid = rs.getInt(COL_SHAREDNETWORKID);
    sn.subnet = rs.getString(COL_SUBNET);
    sn.fixedattributes = rs.getString(COL_FIXEDATTRIBUTES);
    sn.attributes = rs.getString(COL_ATTRIBUTES);
    sn.modificationtimestamp = rs.getLong(COL_MODIFICATIONTIMESTAMP);
  }

  

}
