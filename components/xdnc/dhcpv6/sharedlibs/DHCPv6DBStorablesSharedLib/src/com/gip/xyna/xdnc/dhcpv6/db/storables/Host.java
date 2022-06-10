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

@Persistable(primaryKey = Host.COL_HOSTID, tableName = Host.TABLENAME)
public class Host extends Storable<Host> implements Comparable<Host>{
  
  public static final String TABLENAME = "host";
  public static final String COL_HOSTID = "hostID";
  public static final String COL_MAC = "mac";
  public static final String COL_HOSTNAME = "hostName";
  public static final String COL_REMOTEID = "agentRemoteId";
  public static final String COL_IP = "assignedIp";
  public static final String COL_PREFIX = "prefixlength";
  public static final String COL_POOLID = "assignedPoolID";
  public static final String COL_SUBNET = "subnetOfPool";
  public static final String COL_DESIREDPOOLTYPE = "desiredPoolType";
  public static final String COL_DYNDNS = "dynamicDnsActive";
  public static final String COL_DEPLOYMENTSTATE = "deploymentState";
  public static final String COL_CONFIGDESCR = "configDescr";
  public static final String COL_CMTSIP = "cmtsip";
  
  private static final String IPv6ADDRESSLENGTH = "128";

  
  @Column(name = COL_HOSTID)
  private int hostID;
  
  @Column(name = COL_CMTSIP)
  private String cmtsip;
  
  @Column(name = COL_MAC)
  private String mac;
  
  @Column(name = COL_HOSTNAME)
  private String hostName;
  
  @Column(name = COL_REMOTEID)
  private String agentRemoteId;
  
  @Column(name = COL_IP)
  private String assignedIp;
  
  @Column(name = COL_PREFIX)
  private String prefixlength;
  
  @Column(name = COL_POOLID)
  private int assignedPoolID;
  
  @Column(name = COL_SUBNET)
  private int subnetOfPool;
  
  @Column(name = COL_DESIREDPOOLTYPE)
  private int desiredPoolType;
  
  @Column(name = COL_DYNDNS)
  private String dynamicDnsActive;
  
  @Column(name = COL_DEPLOYMENTSTATE)
  private String deploymentState;
  
  @Column(name = COL_CONFIGDESCR)
  private String configDescr;
  
  public Host(){
    
  }
  
  public int getHostID(){
    return hostID;
  }
  
  public String getCmtsip() {
    return cmtsip;
  }

  public void setCmtsip(String cmtsip) {
    this.cmtsip = cmtsip;
  }

  public String getMac(){
    return mac;
  }
  
  public String getHostName(){
    return hostName;
  }
  
  public String getAgentRemoteId(){
    return agentRemoteId;
  }
  
  public String getAssignedIp(){
    return assignedIp;
  }
  
  public String getPrefixlength(){
    return prefixlength;
  }
  
  public int getAssignedPoolID(){
    return assignedPoolID;
  }
  
  public int getSubnetOfPool(){
    return subnetOfPool;
  }
  
  public int getDesiredPoolType(){
    return desiredPoolType;
  }
  
  public String getDynamicDnsActive(){
    return dynamicDnsActive;
  }
  
  public String getDeploymentState(){
    return deploymentState;
  }
  
  public String getConfigDescr(){
    return configDescr;
  }
  
  @Override
  public Object getPrimaryKey() {
    return hostID;
  }
  
  private static class HostReader implements ResultSetReader<Host> {

    public Host read(ResultSet rs) throws SQLException {
      Host h = new Host();
      Host.fillByResultSet(h, rs);
      return h;
    }
  }
  
  public static void fillByResultSet(Host h, ResultSet rs) throws SQLException {
    h.agentRemoteId = rs.getString(COL_REMOTEID);
    h.assignedIp = rs.getString(COL_IP);
    h.assignedPoolID = rs.getInt(COL_POOLID);
    h.configDescr = rs.getString(COL_CONFIGDESCR);
    h.deploymentState = rs.getString(COL_DEPLOYMENTSTATE);
    h.desiredPoolType = rs.getInt(COL_DESIREDPOOLTYPE);
    h.dynamicDnsActive = rs.getString(COL_DYNDNS);
    h.hostID = rs.getInt(COL_HOSTID);
    h.hostName = rs.getString(COL_HOSTNAME);
    h.mac = rs.getString(COL_MAC);
    h.prefixlength = rs.getString(COL_PREFIX);
    h.subnetOfPool = rs.getInt(COL_SUBNET);
    h.cmtsip = rs.getString(COL_CMTSIP);
  }
  
  private static final HostReader reader = new HostReader();

  @Override
  public ResultSetReader<? extends Host> getReader() {
    return reader;
  }

  @Override
  public <U extends Host> void setAllFieldsFromData(U data2) {
    Host data=data2;
    agentRemoteId = data.agentRemoteId;
    assignedIp = data.assignedIp;
    assignedPoolID = data.assignedPoolID;
    configDescr = data.configDescr;
    deploymentState = data.deploymentState;
    desiredPoolType = data.desiredPoolType;
    dynamicDnsActive = data.dynamicDnsActive;
    hostID = data.hostID;
    hostName = data.hostName;
    mac = data.mac;
    prefixlength = data.prefixlength;
    subnetOfPool = data.subnetOfPool;
    cmtsip = data.cmtsip;
    
  }

  public int compareTo(Host otherHost) {
    //TODO: wie steht prefixlength in Tabelle: 64 oder /64?
    if (otherHost.getAssignedIp().equals("") || otherHost.getAssignedIp()== null){
      if (getAssignedIp()=="" || getAssignedIp()== null){
        return 0;//beiden Hosts wurde keine IP-Adresse zugewiesen
      }
      return -1;//assignedIP < otherHost.assignedIP, da otherHost.assignedIP leer, assignedIp nicht
    }
    if (getAssignedIp().equals("") || getAssignedIp()== null){
      return 1;//assignedIP > otherHost.assignedIP, da assignedIP leer, otherHost nicht
    }
    // in beiden assignedIps steht etwas drin
    String otherPrefixlength = otherHost.getPrefixlength();
    if (otherPrefixlength == null || otherPrefixlength.equals("")){
      otherPrefixlength = IPv6ADDRESSLENGTH;
    }
    if (prefixlength == null || prefixlength.equals("")){
      prefixlength = IPv6ADDRESSLENGTH;
    }
    IP otherIP = new IP(otherHost.getAssignedIp(), Integer.parseInt(otherPrefixlength));
    IP thisIP = new IP(assignedIp, Integer.parseInt(prefixlength));
    return thisIP.compareTo(otherIP);
  }

}
