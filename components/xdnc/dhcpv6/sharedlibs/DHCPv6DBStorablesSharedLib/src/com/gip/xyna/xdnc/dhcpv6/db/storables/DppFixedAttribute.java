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

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = DppFixedAttribute.COL_ATTRIBUTEID, tableName = DppFixedAttribute.TABLENAME)
public class DppFixedAttribute extends Storable<DppFixedAttribute> {

  public static final String TABLENAME = "dppfixedattribute";
  public static final String COL_ATTRIBUTEID = "dppFixedAttributeID";
  public static final String COL_NAME = "name";
  public static final String COL_ETH0 = "eth0";
  public static final String COL_ETH1 = "eth1";
  public static final String COL_ETH2 = "eth2";
  public static final String COL_ETH2v6 = "eth2v6";
  public static final String COL_ETH3 = "eth3";
  public static final String COL_DOMAINNAME = "domainName";
  public static final String COL_FAILOVER = "failover";
  public static final String COL_ETH1PEER = "eth1peer";
  
  @Column(name = COL_ATTRIBUTEID)
  private int dppFixedAttributeID;
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_ETH0)
  private String eth0;
  
  @Column(name = COL_ETH1)
  private String eth1;
  
  @Column(name = COL_ETH2)
  private String eth2;
  
  @Column(name = COL_ETH2v6)
  private String eth2v6;
  
  @Column(name = COL_ETH3)
  private String eth3;
  
  @Column(name = COL_DOMAINNAME)
  private String domainName;
  
  @Column(name = COL_ETH1PEER)
  private String eth1peer;
  
  @Column(name = COL_FAILOVER)
  private String failover;
  
  public int getDppFixedAttributeID(){
    return dppFixedAttributeID;
  }
  
  public String getName(){
    return name;
  }
  
  public String getEth0(){
    return eth0;
  }
  
  public String getEth1(){
    return eth1;
  }
  
  public String getEth2(){
    return eth2;
  }
  
  public String getEth2v6(){
    return eth2v6;
  }
  
  public String getEth3(){
    return eth3;
  }
  
  public String getDomainName(){
    return domainName;
  }
  
  public String getEth1peer(){
    return eth1peer;
  }
  
  public String getFailover(){
    return failover;
  }
  
  @Override
  public Object getPrimaryKey() {
    return dppFixedAttributeID;
  }

  private static class DppFixedAttributeReader implements ResultSetReader<DppFixedAttribute> {

    public DppFixedAttribute read(ResultSet rs) throws SQLException {
      DppFixedAttribute a = new DppFixedAttribute();
      DppFixedAttribute.fillByResultSet(a, rs);
      return a;
    }

  }
  
  private static final DppFixedAttributeReader reader = new DppFixedAttributeReader();
  
  @Override
  public ResultSetReader<? extends DppFixedAttribute> getReader() {
    return reader;
  }

  @Override
  public <U extends DppFixedAttribute> void setAllFieldsFromData(U data2) {
    DppFixedAttribute data = data2;
    dppFixedAttributeID = data.dppFixedAttributeID;
    eth0 = data.eth0;
    eth1 = data.eth1;
    eth2 = data.eth2;
    eth2v6 = data.eth2v6;
    eth3 = data.eth3;
    name = data.name;
    domainName = data.domainName;
    eth1peer = data.eth1peer;
    failover = data.failover;    
  }
  
  public static void fillByResultSet(DppFixedAttribute a, ResultSet rs) throws SQLException {
    a.dppFixedAttributeID = rs.getInt(COL_ATTRIBUTEID);
    a.name = rs.getString(COL_NAME);
    a.domainName = rs.getString(COL_DOMAINNAME);
    a.failover = rs.getString(COL_FAILOVER);
    a.eth0 = rs.getString(COL_ETH0);
    a.eth1 = rs.getString(COL_ETH1);
    a.eth2 = rs.getString(COL_ETH2);
    a.eth2v6 = rs.getString(COL_ETH2v6);
    a.eth3 = rs.getString(COL_ETH3);
    a.eth1peer = rs.getString(COL_ETH1PEER);
  }

  

}
