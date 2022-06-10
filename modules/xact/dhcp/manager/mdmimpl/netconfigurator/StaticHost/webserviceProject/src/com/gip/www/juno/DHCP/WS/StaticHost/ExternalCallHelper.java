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
package com.gip.www.juno.DHCP.WS.StaticHost;



import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.DBSchema;
import com.gip.juno.ws.tools.DBCommands;
import com.gip.juno.ws.tools.PropertiesHandler;
import com.gip.juno.ws.tools.SQLCommand;
import com.gip.juno.ws.tools.QueryTools.DBStringReader;
import com.gip.xyna.utils.db.ResultSetReader;



public class ExternalCallHelper {

  private static final Logger logger = Logger.getLogger("StaticHost");

  private final static String WS_PROPERTY_FREE_IPS_MAX_ROWS = "cm.getfreereservedips.maxrows";

  private final static String searchLeasesByRemoteId = new StringBuilder()
                   .append("SELECT ip, dppInstance FROM ")
                     .append(DBSchema.audit.toString())
                     .append(".leasesxynaandisc WHERE remoteId=?").toString();


  static LeasesData findLeaseForRemoteId(String cpeMac) throws RemoteException {
    SQLCommand query = new SQLCommand(searchLeasesByRemoteId);
    query.addConditionParam(cpeMac);
    return new DBCommands<LeasesData>().queryOneRow(DBSchema.audit.toString(), new LeasesDataReader(), query, logger);
  }


  private final static String retrieveAllPoolRangesWithCmtsReference = new StringBuilder()
                  .append("SELECT pool.rangeStart, ")
                         .append("pool.rangeStop, ")
                         .append("pool.poolID, ")
                         .append("pool.subnetID, ")
                         .append("pool.poolTypeId, ")
                         .append("sharednetwork.sharedNetworkID AS associatedCmtsId ")
                           .append("FROM dhcp.pool ")
                             .append("LEFT JOIN subnet ON subnet.subnetID = pool.subnetID ")
                             .append("LEFT JOIN sharednetwork ON sharednetwork.sharedNetworkID = subnet.sharedNetworkID").toString();


  static List<PoolData> retrieveAllPoolRangesWithCmtsReference() throws RemoteException {
    SQLCommand query = new SQLCommand(retrieveAllPoolRangesWithCmtsReference);
    return new DBCommands<PoolData>().query(new PoolDataReader(), DBSchema.dhcp, query, logger);
  }


  private final static String searchReservedPoolsByCmtsId = new StringBuilder()
                  .append("SELECT rangeStart, rangeStop, poolID, subnetID, poolTypeID, 0 AS associatedCmtsId ")
                    .append("FROM dhcp.pool ")
                      .append("WHERE subnetID in (")
                        .append("SELECT subnetID FROM dhcp.subnet WHERE sharednetworkID = ?")
                      .append(") AND poolTypeID = (")
                        .append("SELECT poolTypeID FROM dhcp.pooltype WHERE name = 'Reserved IPs'")
                      .append(")").toString();


  static List<PoolData> retrieveReservedPoolsForCmts(int cmtsIp) throws RemoteException {
    logger.info("retrieveReservedPoolsForCmts with param: " + cmtsIp);
    SQLCommand query = new SQLCommand(searchReservedPoolsByCmtsId);
    query.addConditionParam(Integer.toString(cmtsIp));
    return new DBCommands<PoolData>().query(new PoolDataReader(), DBSchema.dhcp, query, logger);
  }


  private final static String searchStatichostIPsForPool = new StringBuilder()
                  .append("SELECT ip FROM dhcp.statichost WHERE assignedPoolID=?").toString();


  static List<String> retrieveStatichostIPsForPool(int poolId) throws RemoteException {
    SQLCommand query = new SQLCommand(searchStatichostIPsForPool);
    query.addConditionParam(Integer.toString(poolId));
    return new DBCommands<String>().query(new DBStringReader(), DBSchema.dhcp, query, logger);
  }


  static List<String> generateFreeIpsFromPoolDefinition(PoolData poolDefinition, List<String> exclusions,
                                                         int maxGeneration) {
    List<String> freeIps = new ArrayList<String>();
    IpGenerationIterator iterator = poolDefinition.getPoolIpGenerationIerator(exclusions);
    while (iterator.hasNext() && freeIps.size() < maxGeneration) {
      freeIps.add(iterator.next());
    }
    return freeIps;
  }
  
  
  static PoolData findReservedPoolByIp(String ip) throws RemoteException {
    Collection<PoolData> reservedPools = retrieveAllReservedPools();
    int[] ipParts = convertIpStringToIntParts(ip);
    for (PoolData poolData : reservedPools) {
      if (poolData.contains(ip, ipParts)) {
        return poolData;
      }
    }
    return null;
  }
  
  
  private final static String retrieveAllReservedPools = new StringBuilder()
  .append("SELECT rangeStart, rangeStop, poolID, subnetID, poolTypeID, 0 AS associatedCmtsId ")
    .append("FROM dhcp.pool ")
      .append("WHERE poolTypeID = (")
        .append("SELECT poolTypeID FROM dhcp.pooltype WHERE name = 'Reserved IPs'")
      .append(")").toString();
  
  private static List<PoolData> retrieveAllReservedPools() throws RemoteException {
    SQLCommand query = new SQLCommand(retrieveAllReservedPools);
    return new DBCommands<PoolData>().query(new PoolDataReader(), DBSchema.dhcp, query, logger);
  }

  
  private final static String queryStaticHostId = new StringBuilder()
                    .append("SELECT staticHostID FROM dhcp.statichost WHERE ip=? AND cpe_mac=?").toString();
  
  static String lookupStaticHost(String ip, String cpe_mac) throws RemoteException {
    SQLCommand query = new SQLCommand(queryStaticHostId);
    query.addConditionParam(ip);
    query.addConditionParam(cpe_mac);
    return new DBCommands<String>().queryOneRow(DBSchema.dhcp.toString(), new DBStringReader(), query, logger);
  }
  
  
  private final static String queryCpePoolId = new StringBuilder()
                  .append("SELECT poolTypeID FROM dhcp.pooltype WHERE name=?").toString();
  
  private final static String CPE_POOL_IDENTIFIER = "CPE-Pool"; 
  
  static String lookupCpePoolId() throws RemoteException {
    SQLCommand query = new SQLCommand(queryCpePoolId);
    query.addConditionParam(CPE_POOL_IDENTIFIER);
    return new DBCommands<String>().queryOneRow(DBSchema.dhcp.toString(), new DBStringReader(), query, logger);
  }

  public static class LeasesData {

    public final String ip;
    public final String dpp;


    LeasesData(String ip, String dpp) {
      this.dpp = dpp;
      this.ip = ip;
    }
  }

  public static class LeasesDataReader implements ResultSetReader<LeasesData> {

    public LeasesData read(ResultSet rs) throws SQLException {
      return new LeasesData(rs.getString("ip"), rs.getString("dppInstance"));
    }

  }

  public static class PoolData {

    public final int associatedCmtsId;
    public final int poolId;
    public final int subnetId;
    public final int poolTypeId;
    public final String rangeStart;
    public final String rangeStop;

    private final String constantPrefix;
    private int[] startParts;
    private int[] stopParts;


    PoolData(int poolId, int associatedCmtsId, int subnetId, int poolTypeId, String rangeStart, String rangeStop) {
      this.associatedCmtsId = associatedCmtsId;
      this.rangeStart = rangeStart;
      this.rangeStop = rangeStop;
      this.poolId = poolId;
      this.subnetId = subnetId;
      this.poolTypeId = poolTypeId;
      String[] fullStartParts = rangeStart.split("\\.");
      String[] fullStopParts = rangeStop.split("\\.");
      assert fullStartParts.length == fullStopParts.length;
      assert fullStopParts.length == 4;
      StringBuilder constantPrefixBuilder = new StringBuilder();
      for (int i = 0; i < 4; i++) {
        if (fullStartParts[i].equals(fullStopParts[i])) {
          constantPrefixBuilder.append(fullStartParts[i]);
          constantPrefixBuilder.append(".");
          assert i != 3; // rangeStart == rangeStop
        } else {
          startParts = new int[4 - i];
          stopParts = new int[4 - i];
          for (int j = 0; j < 4 - i; j++) {
            startParts[j] = Integer.parseInt(fullStartParts[i + j]);
            stopParts[j] = Integer.parseInt(fullStopParts[i + j]);
          }
          break;
        }
      }
      constantPrefix = constantPrefixBuilder.toString();
    }


    public boolean contains(String ip, int[] ipParts) {
      if (ip.startsWith(constantPrefix)) {
        for (int i = 0; i < stopParts.length; i++) {
          int startComparision = ipParts[i + 4 - startParts.length] - startParts[i];
          int stopComparision = ipParts[i + 4 - stopParts.length] - stopParts[i];
          // System.out.println("startComparision: " + startComparision + " <= " + ipParts[i+4-startParts.length] + " - " + startParts[i]);
          // System.out.println("stopComparision: " + stopComparision + " <= " + ipParts[i+4-stopParts.length] + " - " + stopParts[i]);
          if (startComparision > 0 && stopComparision < 0) {
            return true;
          } else if (startComparision == 0 || stopComparision == 0) {
            // continue
          } else {
            return false;
          }
        }
        return true;
      } else {
        return false;
      }
    }


    IpGenerationIterator getPoolIpGenerationIerator(List<String> ipExlusions) {
      return new IpGenerationIterator(constantPrefix, startParts, stopParts, ipExlusions);
    }


    @Override
    public String toString() {
      return new StringBuilder("Pool[").append(poolId).append("]:").append(rangeStart).append("<->").append(rangeStop)
                      .toString();
    }
  }


  private static class IpGenerationIterator implements Iterator<String> {

    private final String constantPrefix;
    private final int[] rangeStop;
    private final List<String> exclusions;
    private int[] next;


    IpGenerationIterator(String constantPrefix, int[] rangeStart, int[] rangeStop, List<String> exclusions) {
      this.constantPrefix = constantPrefix;
      this.rangeStop = rangeStop;
      this.exclusions = exclusions;
      next = new int[rangeStart.length];
      System.arraycopy(rangeStart, 0, next, 0, rangeStart.length);
      skipExclusions();
    }


    private void skipExclusions() {
      String next = buildNext();
      while (exclusions.contains(next) && hasNext()) {
        incrementNext();
        if (hasNext()) {
          next = buildNext();
        }
      }
    }


    private void incrementNext() {
      if (hasNext()) {
        for (int i = next.length - 1; i >= 0; i--) {
          if (Arrays.equals(next, rangeStop)) {
            break;
          } else if (next[i] + 1 > 255) {
            next[i] = 0;
          } else {
            next[i]++;
            return;
          }
        }
        next = null;
      }
    }


    // @Override
    public boolean hasNext() {
      return next != null;
    }


    // @Override
    public String next() {
      String next = buildNext();
      incrementNext();
      if (hasNext()) {
        skipExclusions();
      }
      return next;
    }


    private String buildNext() {
      StringBuilder nextBuilder = new StringBuilder(constantPrefix);
      for (int i = 0; i < next.length; i++) {
        nextBuilder.append(next[i]);
        if (i + 1 < next.length) {
          nextBuilder.append(".");
        }
      }
      return nextBuilder.toString();
    }


    // @Override
    public void remove() {
      ; // ignore
    }

  }


  public static class PoolDataReader implements ResultSetReader<PoolData> {

    public PoolData read(ResultSet rs) throws SQLException {
      return new PoolData(rs.getInt("poolID"), rs.getInt("associatedCmtsId"), rs.getInt("subnetID"), rs.getInt("poolTypeID"), rs.getString("rangeStart"),
                          rs.getString("rangeStop"));
    }

  }


  static int[] convertIpStringToIntParts(String ip) {
    String[] ipSplit = ip.split("\\.");
    int[] ipParts = new int[ipSplit.length];
    for (int i = 0; i < ipParts.length; i++) {
      ipParts[i] = Integer.parseInt(ipSplit[i]);
    }
    return ipParts;
  }


  static int getFreeIpMaxRows() {
    try {
      int maxRows = Integer.parseInt(PropertiesHandler.getProperty(PropertiesHandler.getWsProperties(),
                                                                   WS_PROPERTY_FREE_IPS_MAX_ROWS, logger));
      if (maxRows <= 0) {
        return 250;
      } else {
        return maxRows;
      }
    } catch (Throwable t) {
      return 250;
    }
  }
}
