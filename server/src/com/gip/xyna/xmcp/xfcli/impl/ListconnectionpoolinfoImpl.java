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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionInformation;
import com.gip.xyna.utils.db.ConnectionPool.ThreadInformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listconnectionpoolinfo;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache;
import com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.CachedConnectionInformation;



public class ListconnectionpoolinfoImpl extends XynaCommandImplementation<Listconnectionpoolinfo> {

  public void execute(OutputStream statusOutputStream, Listconnectionpoolinfo payload) throws XynaException {
    boolean showSingleConnectionData = payload.getVerbose();
    boolean showThreadData =  payload.getExtraverbose();
    if( showThreadData ) {
      showSingleConnectionData = true;
    }
    ConnectionPool[] conPools = ConnectionPool.getAllRegisteredConnectionPools();
    Arrays.sort( conPools, new PoolNameComparator() );
    
    long now = System.currentTimeMillis();
    for (ConnectionPool pool : conPools) {
      StringBuilder sb = new StringBuilder();
      sb.append(" o ConnectionPool ").append(pool.getId()).append("\n");
      writeToCommandLine(statusOutputStream, sb.toString());
      ConnectionStatistics connectionStatistics = new ConnectionStatistics(pool, now, showThreadData);
      appendPoolStatistics(statusOutputStream, pool, connectionStatistics);
      if( showSingleConnectionData ) {
        appendSQLStatistics(statusOutputStream, pool);
        appendThreadInformation(statusOutputStream, pool, showThreadData);
        appendConnectionStatistics(statusOutputStream, connectionStatistics);
      }
    }
    
    writeConnectionCacheInfo(statusOutputStream, showThreadData);
  }

  private void appendPoolStatistics(OutputStream statusOutputStream, ConnectionPool pool,
                                    ConnectionStatistics connectionStatistics) {
    StringBuilder sb = new StringBuilder();
    sb.append("   ");
    sb.append("size = ").append(connectionStatistics.totalCons).append("  ");
    sb.append("used = ").append(connectionStatistics.usedCons).append("  ");
    sb.append("pool = ").append(pool.toString() );
    sb.append("\n");
    writeToCommandLine(statusOutputStream, sb.toString());
  }
  
  private void appendSQLStatistics(OutputStream statusOutputStream, ConnectionPool pool) {
    StringBuilder sb = new StringBuilder();
    sb.append("    SQL Statistics:\n");
    Map<String, Integer> sqlStats = pool.getSQLStatistics();
    for (String sql : sqlStats.keySet()) {
      sb.append("      - ").append(sqlStats.get(sql)).append("x ").append(sql).append("\n");
    }
    writeToCommandLine(statusOutputStream, sb.toString());
  }

  private void appendThreadInformation(OutputStream statusOutputStream, ConnectionPool pool, boolean showThreadData) {
    ThreadInformation[] tis = pool.getWaitingThreadInformation();
    StringBuilder sb = new StringBuilder();
    sb.append("   waiting threads = ").append(tis.length).append("\n");
    writeToCommandLine(statusOutputStream, sb.toString());
    if (showThreadData) {
      for (ThreadInformation ti : tis) {
        writeThreadInfo(statusOutputStream, "     ", ti);
      }
    }
  }
  
  private void appendConnectionStatistics(OutputStream statusOutputStream, ConnectionStatistics connectionStatistics ) {
    writeToCommandLine(statusOutputStream, connectionStatistics.header );
    for( ConnectionStatistics.CSEntry cse : connectionStatistics.entries ) {
      writeToCommandLine(statusOutputStream, cse.statistics );
      if( cse.threadInfo != null ) {
        writeLineToCommandLine(statusOutputStream, cse.threadInfo );
        writeStackTrace( statusOutputStream, "      ", cse.stackTrace );
      }
      if (cse.stackTraceOriginal != null) {
        writeLineToCommandLine(statusOutputStream, "    --------- original thread state:");
        writeStackTrace( statusOutputStream, "      ", cse.stackTraceOriginal );
      }
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

  //this adds leading zeroes to get the correct order
  private static String padNumberWithLeadingZeroes(long toBeAdjusted, int length) {
    String result = String.valueOf(toBeAdjusted);
    
    while ( result.length() < length ) {
      result = "0" + result;
    }
    
    return result;
  }
  
  
  private void writeConnectionCacheInfo(OutputStream statusOutputStream, boolean showThreadData) {
    List<CachedConnectionInformation> infos = CentralComponentConnectionCache.getInstance().getConnectionCacheInformation();
    writeLineToCommandLine(statusOutputStream, "Got information for " + infos.size() + " dedicated connections.");
    for (CachedConnectionInformation cci : infos) {
      StringBuilder sb = new StringBuilder();
      sb.append(cci.getIdentifier())
        .append(" [")
        .append(cci.getType())
        .append("] - tables:");
      for (String table : cci.getTables()) {
        sb.append(" ");
        sb.append(table);
      }
      writeLineToCommandLine(statusOutputStream, sb.toString());
      if (showThreadData) {
        writeThreadInfo(statusOutputStream, "    ", cci.getThreadinfo());
      }      
    }
  }
  

  private void writeThreadInfo(OutputStream statusOutputStream, String indentation, ThreadInformation ti) {
    if (ti != null) {
      writeLineToCommandLine(statusOutputStream, threadInfoToString(indentation, ti.getName(), ti.getId(), ti.getPriority(), ti.getState() ) );
      writeStackTrace( statusOutputStream, indentation, ti.getStackTrace() );
    }
  }
  
  private void writeThreadInfo(OutputStream statusOutputStream, String indentation, com.gip.xyna.xnwh.persistence.CentralComponentConnectionCache.ThreadInformation ti) {
    if (ti != null) {
      writeLineToCommandLine(statusOutputStream, threadInfoToString(indentation, ti.getName(), ti.getId(), ti.getPriority(), ti.getState() ) );
      writeStackTrace( statusOutputStream, indentation, ti.getStackTrace() );
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
  
  
  
  private static class ConnectionStatistics {
    
    public static class CSEntry {
      String statistics;
      String threadInfo;
      StackTraceElement[] stackTrace;
      StackTraceElement[] stackTraceOriginal;
    }

    private ArrayList<CSEntry> entries;
    int totalCons = 0;
    int usedCons = 0;
    String header = "   connection statistics: in use - last aquired - connection last validated - last commit/rollback - count used - last SQL\n";
    
    public ConnectionStatistics(ConnectionPool pool, long now, boolean showThreadData) {
      entries = new ArrayList<CSEntry>();
      for (ConnectionInformation ci : pool.getConnectionStatistics()) {
        ++totalCons;
        if( ci.isInUse() ) {
          ++usedCons;
        }
        entries.add( createCSEntry(ci, now, showThreadData) );
      }
    }

    private CSEntry createCSEntry(ConnectionInformation ci, long now, boolean verbose) {
      CSEntry cse = new CSEntry();
      StringBuilder sb = new StringBuilder("    - ")
      .append(ci.isInUse())
      .append(" ").append(calcTimeDiff(now, ci.getAquiredLast()))
      .append(" ").append(calcTimeDiff(now, ci.getLastCheck()))
      .append(" ").append((ci.getCntUsed() == 0 ? "-" : ci.isLastCheckOk() ? "OK" : "NOK"))
      .append(" ").append(calcTimeDiff(now, Math.max(ci.getLastCommit(), ci.getLastRollback())))
      .append(" ").append(ci.getCntUsed())
      .append(" ").append((ci.getLastSQL() == null ? "n/a" : ci.getLastSQL()))
      .append("\n");
      cse.statistics = sb.toString();
      if( verbose ) {
        ThreadInformation ti = ci.getCurrentThread();
        if( ti != null ) {
          cse.threadInfo = threadInfoToString("    ", ti.getName(), ti.getId(), ti.getPriority(), ti.getState() );
          cse.stackTrace = ti.getStackTrace();
        }
        if (ci.getStackTraceWhenThreadGotConnection() != null) {
          /*
           * die ersten 4 stacks sind immer:
           * 
           *  java.lang.Thread.getStackTrace(Thread.java:1436)
           *  com.gip.xyna.utils.db.ConnectionPool$PooledConnection.initializeForUse(ConnectionPool.java:826)
           *  com.gip.xyna.utils.db.ConnectionPool$PooledConnection.access$400(ConnectionPool.java:759)
           *  com.gip.xyna.utils.db.ConnectionPool.getConnection(ConnectionPool.java:478)
           */
          cse.stackTraceOriginal = new StackTraceElement[ci.getStackTraceWhenThreadGotConnection().length - 3];
          System.arraycopy(ci.getStackTraceWhenThreadGotConnection(), 3, cse.stackTraceOriginal, 0, cse.stackTraceOriginal.length);
        }
      }
      return cse;
    }
    
  } 
  
  
  private static class PoolNameComparator implements Comparator<ConnectionPool> {
    public int compare(ConnectionPool cp1, ConnectionPool cp2) {
      return cp1.getId().compareTo(cp2.getId());
    }
  }
  
}
