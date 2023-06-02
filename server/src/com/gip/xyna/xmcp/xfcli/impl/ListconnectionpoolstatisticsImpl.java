/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli.impl;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.db.ConnectionPool.ThreadInformation;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import com.gip.xyna.xmcp.xfcli.generated.Listconnectionpoolstatistics;
import com.gip.xyna.xnwh.pools.ConnectionInformation;
import com.gip.xyna.xnwh.pools.ConnectionPoolDetailInformation;
import com.gip.xyna.xnwh.pools.ConnectionPoolInformation;
import com.gip.xyna.xnwh.pools.ConnectionPoolManagement;



public class ListconnectionpoolstatisticsImpl extends XynaCommandImplementation<Listconnectionpoolstatistics> {

  public void execute(OutputStream statusOutputStream, Listconnectionpoolstatistics payload) throws XynaException {
    ConnectionPoolManagement cpm = XynaFactory.getInstance().getXynaNetworkWarehouse().getConnectionPoolManagement();
    List<ConnectionPoolDetailInformation> pools;
    if (payload.getName() == null) {
      List<ConnectionPoolInformation> cpis = cpm.listConnectionPoolInformation();
      pools = new ArrayList<ConnectionPoolDetailInformation>();
      for (ConnectionPoolInformation cpi : cpis) {
        ConnectionPoolDetailInformation i = cpm.listConnectionPoolStatistics(cpi.getName());
        if (i == null) {
          writeLineToCommandLine(statusOutputStream, "Pool " + cpi.getName() + " not found.");
          continue;
        }
        pools.add(i);
      }
    } else {
      pools = Collections.singletonList(cpm.listConnectionPoolStatistics(payload.getName()));
    }
    for (ConnectionPoolDetailInformation pool : pools) {
      writeConnectionPoolDetails(statusOutputStream, pool, payload.getThread(), payload.getSql());
    }
  }
  
  
  private void writeConnectionPoolDetails(OutputStream statusOutputStream, ConnectionPoolDetailInformation cpi, boolean listThreadInfo, boolean listSQLStatsInfo) {
    String format = " %-" + 40 + "s %-" + 15 + "s  %-" + 12 + "s %-" + 4 + "s / %-" + 4 + "s %-" + 10 + "s";
    writeLineToCommandLine(statusOutputStream, String.format(format, cpi.getName(), cpi.getPooltype(), cpi.getState(), cpi.getUsed(), cpi.getSize(), cpi.getWaitingThreadInformation().length));
    String indentation = "  ";
    String sqlStatFormat = indentation + "  %-" + 5 + "s %-" + 200 + "s";
    if (listSQLStatsInfo && cpi.getSqlStats() != null && cpi.getSqlStats().size() > 0) {
      writeLineToCommandLine(statusOutputStream, indentation + "SQL statistics:");
      SortedSet<Pair<String, Integer>> sortedSQLStats = new TreeSet<Pair<String,Integer>>(Pair.<Integer>comparatorSecond());
      for (Entry<String, Integer> entry : cpi.getSqlStats().entrySet()) {
        sortedSQLStats.add(Pair.of(entry.getKey(), entry.getValue()));
      }
      for (Pair<String, Integer> sqlStat : sortedSQLStats) {
        writeLineToCommandLine(statusOutputStream, String.format(sqlStatFormat, sqlStat.getSecond() + "x", sqlStat.getFirst()));
      }
    }
    if (listThreadInfo && cpi.getWaitingThreadInformation().length > 0) {
      writeLineToCommandLine(statusOutputStream, indentation, "Waiting threads:");
      for (ThreadInformation ti : cpi.getWaitingThreadInformation()) {
        writeThreadInfo(statusOutputStream, indentation + " ", ti);
      }
    }
    String conFormat = indentation + " %-" + 8 + "s %-" + 14 + "s  %-" + 14 + "s %-" + 18 + "s %-" + 5 + "s %-" + 50 + "s";
    if (cpi.getConnectionInformation() != null && cpi.getConnectionInformation().size() > 0) {
      writeLineToCommandLine(statusOutputStream, String.format(conFormat, "isUsed", "aquired", "validated", "last transaction", "uses", "last SQL"));
      long now = System.currentTimeMillis();
      for (ConnectionInformation ci : cpi.getConnectionInformation()) {
        writeLineToCommandLine(statusOutputStream, String.format(conFormat, ci.isUsed(),
                                                                            calcTimeDiff(now, ci.getLastAquired()),
                                                                            calcTimeDiff(now, ci.getLastValidated()),
                                                                            //(ci.getCountUses() == 0 ? "-" : ci.isLastCheckOk() ? "OK" : "NOK")
                                                                            calcTimeDiff(now, Math.max(ci.getLastCommit(), ci.getLastRollback())),
                                                                            ci.getCountUses(),
                                                                            (ci.getLastSQL() == null ? "n/a" : ci.getLastSQL())));
        if (listThreadInfo) {
          writeThreadInfo(statusOutputStream, indentation + "  ", ci.getCurrentThreadInformation());
          if (ci.getOpeningStackTrace() != null) {
            writeLineToCommandLine(statusOutputStream, "    --------- original thread state:");
            /*
             * die ersten 4 stacks sind immer:
             * 
             *  java.lang.Thread.getStackTrace(Thread.java:1436)
             *  com.gip.xyna.utils.db.ConnectionPool$PooledConnection.initializeForUse(ConnectionPool.java:826)
             *  com.gip.xyna.utils.db.ConnectionPool$PooledConnection.access$400(ConnectionPool.java:759)
             *  com.gip.xyna.utils.db.ConnectionPool.getConnection(ConnectionPool.java:478)
             */
            StackTraceElement[] steForPrint = new StackTraceElement[ci.getOpeningStackTrace().length - 3];
            System.arraycopy(ci.getOpeningStackTrace(), 3, steForPrint, 0, steForPrint.length);
            writeStackTrace(statusOutputStream, indentation + "  ", steForPrint);
          }
        }
      }
    }
  }
  
  
  // FIXME duplicated code from listconnectionpoolinfo
  private void writeThreadInfo(OutputStream statusOutputStream, String indentation, ThreadInformation ti) {
    if (ti != null) {
      writeLineToCommandLine(statusOutputStream, threadInfoToString(indentation, ti.getName(), ti.getId(), ti.getPriority(), ti.getState() ) );
      writeStackTrace( statusOutputStream, indentation + "  ", ti.getStackTrace() );
    }
  }
  
  
  private static String threadInfoToString(String indentation, String name, long id, int priority, State state) {
    StringBuilder sb = new StringBuilder(indentation);
    sb.append("  thread: ").append(name).append("/").append(id);
    sb.append("  prio = ").append(priority);
    sb.append("  state = ").append(state);
    return sb.toString();
  }
  
  
  private void writeStackTrace(OutputStream statusOutputStream, String indentation, StackTraceElement[] stackTrace ) {
    for (StackTraceElement ste : stackTrace) {
      StringBuilder sb = new StringBuilder(indentation).append("  ").append(ste);
      writeLineToCommandLine(statusOutputStream, sb.toString());
    }
  }
  
  
  public static String calcTimeDiff(long now, long old) {
    if (old == 0) {
      return "never";
    }
    long diff = (now - old) / 1000;
    if (diff <= 0) {
      return "now";
    }
    long seconds = diff % 60;
    diff -= seconds;
    diff /= 60;
    long minutes = diff % 60;
    diff -= minutes;
    diff /= 60;
    long hours = diff % 24;
    diff -= hours;
    diff /= 24;
    long days = diff;
    String ret = "";
    if (days > 0) {
      ret = days + "d";
    }
    if (hours > 0 || ret.length() > 0) {
      ret += hours + "h";
    }
    if (minutes > 0 || ret.length() > 0) {
      if ( hours > 0 ) {
        ret += padNumberWithLeadingZeroes(minutes, 2) + "m";
      } else {
        ret += minutes + "m";
      }
    }
    if (seconds > 0 || ret.length() > 0) {
      if ( hours > 0 || minutes > 0 ) {
        ret += padNumberWithLeadingZeroes(seconds, 2) + "s";
      } else {
        ret += seconds + "s";
      }
    }
    return ret;
  }
  
  
  private static String padNumberWithLeadingZeroes(long toBeAdjusted, int length) {
    String result = String.valueOf(toBeAdjusted);
    while ( result.length() < length ) {
      result = "0" + result;
    }
    return result;
  }

}
