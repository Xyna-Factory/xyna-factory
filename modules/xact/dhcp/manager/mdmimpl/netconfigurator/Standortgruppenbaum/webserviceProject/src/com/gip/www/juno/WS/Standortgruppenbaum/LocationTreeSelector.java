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
package com.gip.www.juno.WS.Standortgruppenbaum;

import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.SQLUtilsCache;
import com.gip.juno.ws.tools.SQLUtilsContainerForManagement;
import com.gip.xyna.utils.db.ResultSetReader;


public class LocationTreeSelector {

  private final Logger logger;
    
  public LocationTreeSelector(Logger logger) {
    this.logger = logger;
  }
  

  private static class QueryReader implements ResultSetReader<LocationTreeQueryLine> {
    public QueryReader() { }    
    
    public LocationTreeQueryLine read(ResultSet rs) throws SQLException {
      LocationTreeQueryLine line = new LocationTreeQueryLine();
      line.standortgruppe = escapeCharsForXml(rs.getString("standortgruppe"));
      line.standort = escapeCharsForXml(rs.getString("standort"));
      line.cpedns = escapeCharsForXml(rs.getString("cpedns"));
      line.sharednetwork = escapeCharsForXml(rs.getString("sharednetwork"));
      line.subnet = escapeCharsForXml(rs.getString("subnet"));
      line.fixedattributes = escapeCharsForXml(rs.getString("fixedattributes"));
      line.attributes = escapeAttributeValueTag(rs.getString("attributes"));
      line.rangestart = escapeCharsForXml(rs.getString("rangestart"));
      line.rangestop = escapeCharsForXml(rs.getString("rangestop"));
      line.pooltype = escapeCharsForXml(rs.getString("pooltype"));
      line.hostname = escapeCharsForXml(rs.getString("hostName"));
      line.cpemac = escapeCharsForXml(rs.getString("cpe_mac"));
      line.remoteid = escapeCharsForXml(rs.getString("remoteid"));
      line.ip = escapeCharsForXml(rs.getString("ip"));
      line.deployed1 = escapeCharsForXml(rs.getString("deployed1"));
      line.deployed2 = escapeCharsForXml(rs.getString("deployed2"));
      line.configdescr = escapeCharsForXml(rs.getString("configdescr"));
      line.cpeDnsId = escapeCharsForXml(rs.getString("cpednsid"));
      line.sharedNetworkId = escapeCharsForXml(rs.getString("sharednetworkid"));
      line.standortgruppeid = escapeCharsForXml(rs.getString("standortgruppeid"));
      line.standortId = escapeCharsForXml(rs.getString("standortid"));
      line.subnetid = escapeCharsForXml(rs.getString("subnetid"));
      line.poolTypeID = escapeCharsForXml(rs.getString("poolTypeID"));
      line.poolID = escapeCharsForXml(rs.getString("poolID"));
      line.staticHostId = escapeCharsForXml(rs.getString("StaticHostID"));
      line.mask = escapeCharsForXml(rs.getString("mask"));
      line.targetState = escapeCharsForXml(rs.getString("targetState"));
      line.isDeployed = escapeCharsForXml(rs.getString("isDeployed"));
      line.useForStatistics = escapeCharsForXml(rs.getString("useForStatistics"));
      line.exclusions = escapeCharsForXml(rs.getString("exclusions"));
      line.assignedPoolID = escapeCharsForXml(rs.getString("assignedPoolID"));
      line.desiredPoolType = escapeCharsForXml(rs.getString("desiredPoolType"));
      line.hostDnsList = escapeCharsForXml(rs.getString("dns"));
      line.linkAddresses = escapeCharsForXml(rs.getString("linkAddresses"));
      line.dynamicDnsActive = escapeCharsForXml(rs.getString("dynamicDnsActive"));
      line.sharedNetworkMigrationState = escapeCharsForXml(rs.getString("sharedNetworkMigrationState"));
      line.subnetMigrationState = escapeCharsForXml(rs.getString("subnetMigrationState"));
      line.poolMigrationState = escapeCharsForXml(rs.getString("poolMigrationState"));
      return line;
    }

    private String escapeCharsForXml(String input) {
      if (input == null) {
        return input;
      }
      if (input.trim().length() < 1) {
        return input;
      }
      String ret = input.replace("&", "&amp;");
      ret = ret.replace("<", "&lt;");
      ret = ret.replace(">", "&gt;");
      ret = ret.replace("\"", "&quot;");
      ret = ret.replace("'", "&apos;");
      return ret;
    }
    
    private String escapeAttributeValueTag(String input) {
      if (input == null) {
        return input;
      }
      if (input.trim().length() < 1) {
        return input;
      }
      String ret = input.replace("<", "&ltt;");
      ret = ret.replace(">", "&gtt;");
      return ret;
    }
  }
  
  /*private final static String locationTreeSelect = new StringBuilder()
      .append("select distinct * from ( ")
      .append("select sg.name  as Standortgruppe, st.name as Standort, cpe.cpeDns, ")
      .append("cpe.cpeDnsId, sn.sharedNetworkId, sn.linkAddresses, sg.standortgruppeid, sn.standortid, ")
      .append("s.subnetid, s.mask, p.poolTypeID, p.poolID, NULL as staticHostID, ")
      .append("sn.sharedNetwork, s.subnet, s.fixedAttributes, s.attributes, ")
      .append("p.rangeStart, p.rangeStop, pt.name as PoolType, ")
      .append("p.targetState, p.isDeployed, p.useForStatistics, p.exclusions, ")
      .append("NULL as cpe_mac, NULL as hostName, NULL as ip, NULL as remoteId, NULL as deployed1, NULL as deployed2,")
      .append("NULL as configDescr, NULL as dynamicDnsActive, NULL as dns, NULL as assignedPoolID, NULL as desiredPoolType ")
      .append("from subnet s ")
      .append("left outer join pool p on p.subnetid = s.subnetid ")
      .append("left outer join pooltype pt on pt.poolTypeID = p.poolTypeID ")
      .append("right outer join sharednetwork sn on sn.sharedNetworkID = s.sharedNetworkID ")
      .append("left outer join standort st on st.standortID = sn.standortID ")
      .append("left outer join cpedns cpe on sn.cpednsID = cpe.cpeDnsID ")
      .append("right outer join standortgruppe sg on sg.standortgruppeid = ")
      .append("st.standortgruppeid ")
      .append("union ")
      .append("select sg.name as Standortgruppe, st.name as Standort, cpe.cpeDns, ")
      .append("cpe.cpeDnsId, sn.sharedNetworkId, sn.linkAddresses, sg.standortgruppeid, sn.standortid, ")
      .append("s.subnetid,  s.mask, p.poolTypeID as poolTypeID, p.poolID as poolID, h.staticHostID, ")
      .append("sn.sharedNetwork, s.subnet, s.fixedAttributes, s.attributes, p.rangeStart as rangeStart, ")
      .append("p.rangeStop as rangeStop, pt.name as PoolType, ")
      .append("p.targetState, p.isDeployed, p.useForStatistics, p.exclusions, ")
      .append("h.cpe_mac, h.hostName, h.ip, h.remoteId, h.deployed1, h.deployed2, h.configdescr, h.dynamicDnsActive, h.dns, h.assignedPoolID, h.desiredPoolType ")
      .append("from subnet s ")
      .append("left outer join pool p on p.subnetid = s.subnetid ")
      .append("left outer join pooltype pt on pt.poolTypeID = p.poolTypeID ")
      .append("right outer  join statichost h on h.assignedPoolID = p.poolID ")
      .append("right outer  join sharednetwork sn on sn.sharedNetworkID = s.sharedNetworkID ")
      .append("left outer  join standort st on st.standortID = sn.standortID ")
      .append("left outer  join cpedns cpe on sn.cpednsID = cpe.cpeDnsID ")
      .append("right outer  join standortgruppe sg on sg.standortgruppeid = ")
      .append("st.standortgruppeid  ) subselect ")
      .append("order by subselect.Standortgruppe, subselect.Standort, subselect.sharedNetwork, ")
      .append("subselect.subnet, subselect.rangeStart, subselect.ip")
      .toString();*/
  
  private final static String locationTreeSelect = new StringBuilder()
  .append("select distinct * from ( ")
  .append("select sg.name  as Standortgruppe, st.name as Standort, cpe.cpeDns, ")
  .append("cpe.cpeDnsId, sn.sharedNetworkId, sn.migrationState as sharedNetworkMigrationState, sn.linkAddresses, sg.standortgruppeid, sn.standortid, ")
  .append("s.subnetid, s.mask, s.migrationState as subnetMigrationState, p.poolTypeID, p.poolID, p.migrationState as poolMigrationState, NULL as staticHostID, ")
  .append("sn.sharedNetwork, s.subnet, s.fixedAttributes, s.attributes, ")
  .append("p.rangeStart, p.rangeStop, pt.name as PoolType, ")
  .append("p.targetState, p.isDeployed, p.useForStatistics, p.exclusions, ")
  .append("NULL as cpe_mac, NULL as hostName, NULL as ip, NULL as remoteId, NULL as deployed1, NULL as deployed2,")
  .append("NULL as configDescr, NULL as dynamicDnsActive, NULL as dns, NULL as assignedPoolID, NULL as desiredPoolType ")
  .append("from subnet s ")
  .append("left outer join pool p on p.subnetid = s.subnetid ")
  .append("left outer join pooltype pt on pt.poolTypeID = p.poolTypeID ")
  .append("right outer join sharednetwork sn on sn.sharedNetworkID = s.sharedNetworkID ")
  .append("left outer join standort st on st.standortID = sn.standortID ")
  .append("left outer join cpedns cpe on sn.cpednsID = cpe.cpeDnsID ")
  .append("right outer join standortgruppe sg on sg.standortgruppeid = ")
  .append("st.standortgruppeid ")
  .append("union ")
  .append("select sg.name as Standortgruppe, st.name as Standort, cpe.cpeDns, ")
  .append("cpe.cpeDnsId, sn.sharedNetworkId, sn.migrationState as sharedNetworkMigrationState, sn.linkAddresses, sg.standortgruppeid, sn.standortid, ")
  .append("s.subnetid,  s.mask, s.migrationState as subnetMigrationState, p.poolTypeID as poolTypeID, p.poolID as poolID, p.migrationState as poolMigrationState, h.staticHostID, ")
  .append("sn.sharedNetwork, s.subnet, s.fixedAttributes, s.attributes, p.rangeStart as rangeStart, ")
  .append("p.rangeStop as rangeStop, pt.name as PoolType, ")
  .append("p.targetState, p.isDeployed, p.useForStatistics, p.exclusions, ")
  .append("h.cpe_mac, h.hostName, h.ip, h.remoteId, h.deployed1, h.deployed2, h.configdescr, h.dynamicDnsActive, h.dns, h.assignedPoolID, h.desiredPoolType ")
  .append("from subnet s ")
  .append("left outer join pool p on p.subnetid = s.subnetid ")
  .append("left outer join pooltype pt on pt.poolTypeID = p.poolTypeID ")
  .append("right outer  join statichost h on h.assignedPoolID = p.poolID ")
  .append("right outer  join sharednetwork sn on sn.sharedNetworkID = s.sharedNetworkID ")
  .append("left outer  join standort st on st.standortID = sn.standortID ")
  .append("left outer  join cpedns cpe on sn.cpednsID = cpe.cpeDnsID ")
  .append("right outer  join standortgruppe sg on sg.standortgruppeid = ")
  .append("st.standortgruppeid  ) subselect ")
  .append("order by subselect.Standortgruppe, subselect.Standort, subselect.sharedNetwork, ")
  .append("subselect.subnet, subselect.rangeStart, subselect.ip")
  .toString();

  private final static String countUnpooledHosts = new StringBuilder()
                  .append("select count(*) from statichost ")
                    .append("where assignedPoolID IS NULL")
                    .toString();
  
  private final static String selectAllForUnpooledHosts = new StringBuilder()
                  .append("select standortgruppe.name as standortgruppe, ")
                    .append("standort.name as standort, ")
                    .append("cpedns.cpeDns as cpedns, ")
                    .append("sharednetwork.sharedNetwork as sharednetwork, ")
                    .append("'' as linkAddresses, ")
                    .append("subnet.subnet as subnet, ")
                    .append("'' as fixedattributes, ")
                    .append("'' as attributes, ")
                    .append("'' as rangeStart, ")
                    .append("'' as rangeStop, ")
                    .append("'' as sharedNetworkMigrationState, ")
                    .append("'' as subnetMigrationState, ")
                    .append("'' as poolMigrationState, ")
                    .append("NULL as pooltype, ")
                    .append("staticHost.hostName as hostname, ")
                    .append("staticHost.cpe_mac as cpe_mac, ")
                    .append("staticHost.remoteId as remoteid, ")
                    .append("staticHost.ip as ip, ")
                    .append("staticHost.deployed1 as deployed1, ")
                    .append("staticHost.deployed2 as deployed2, ")
                    .append("statichost.configDescr as configdescr, ")
                    .append("statichost.assignedPoolID as assignedPoolID, ")
                    .append("statichost.desiredPoolType as desiredPoolType, ")
                    .append("statichost.dynamicDnsActive as dynamicDnsActive, ")
                    .append("dns as dns, ")
                    .append("sharednetwork.cpednsID as cpednsid, ")
                    .append("subnet.sharedNetworkID as sharedNetworkid, ")
                    .append("standort.standortGruppeID as standortGruppeid, ")
                    .append("sharednetwork.standortID as standortid, ")
                    .append("statichost.subnetID as subnetid, ")
                    .append("NULL as poolTypeID, ")
                    .append("NULL as poolID, ")
                    .append("staticHost.staticHostID as StaticHostID, ")
                    .append("subnet.mask as mask, ")
                    .append("'' as targetState, ")
                    .append("'no' as isDeployed, ")
                    .append("'no' as useForStatistics, ")
                    .append("'' as exclusions ")
                      .append("from statichost ")
                        .append("left outer join subnet on subnet.subnetID = statichost.subnetID ")
                        .append("left outer join sharednetwork on sharednetwork.sharedNetworkID = subnet.sharedNetworkID ")
                        .append("left outer join cpedns on cpedns.cpeDnsID = sharednetwork.cpednsID ")
                        .append("left outer join standort on standort.standortID = sharednetwork.standortID ")
                        .append("left outer join standortgruppe on standortgruppe.standortGruppeID = standort.standortGruppeID ")
                          .append("where assignedPoolID IS NULL").toString();
  
  
  
  public final static void main(String... args) {
    System.out.println(locationTreeSelect);
  }
  
  public List<LocationTreeQueryLine> query() throws RemoteException {    
    SQLUtilsContainerForManagement container = SQLUtilsCache.getForManagement(DBSchema.dhcp, logger);
    SQLCommand command = new SQLCommand();
    command.sql = locationTreeSelect;
    QueryReader reader = new QueryReader();
    List<LocationTreeQueryLine> ret = new DBCommands<LocationTreeQueryLine>().query(reader, command, container, logger);
    return ret;
  }
  
  
  
  
  public List<LocationTreeQueryLine> queryUnpooledHosts() throws RemoteException {
    SQLUtilsContainerForManagement container = SQLUtilsCache.getForManagement(DBSchema.dhcp, logger);
    SQLCommand countCommand = new SQLCommand();
    countCommand.sql = countUnpooledHosts;
    Integer countUnpooledHosts = new DBCommands<Integer>().queryOneRow(new CountReader(), countCommand, container, logger);
    if (countUnpooledHosts != null) {
      container = SQLUtilsCache.getForManagement(DBSchema.dhcp, logger);
      SQLCommand searchCommand = new SQLCommand();
      searchCommand.sql = selectAllForUnpooledHosts;
      List<LocationTreeQueryLine> unpooledHosts = new DBCommands<LocationTreeQueryLine>().query(new QueryReader(), searchCommand, container, logger);
      return unpooledHosts;
    } else {
      return null;
    }
    
  }
  
  
  private static class CountReader implements ResultSetReader<Integer> {

    public Integer read(ResultSet rs) throws SQLException {
      return rs.getInt(1);
    }
    
  }
}
