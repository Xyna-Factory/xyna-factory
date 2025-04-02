/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;



public class ListThreadInfo implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) {
    int minStackTraceLength = 0;
    String nameFilter = null;
    if( allArgs.getArgCount() > 0 ) {
      String currentName = null;
      for (int i=0; i<allArgs.getArgCount(); i++) {
        if (i%2 == 0) {
          currentName = allArgs.getArg(i).toLowerCase();
        } else {
          if (currentName.equals("-minstacktracelength")) {
            try {
              minStackTraceLength = Integer.parseInt(allArgs.getArg(i));
            } catch ( NumberFormatException e ) {
              clw.writeLineToCommandLine("Could not parse minstacktracelength: " + allArgs.getArg(i));
            }
          } else if (currentName.equals("-name")) {
            nameFilter = allArgs.getArg(i);
          } else if (currentName.length() == 0) {
            clw.writeLineToCommandLine("Ignoring empty parameter");
          } else {
            clw.writeLineToCommandLine("Unknown parameter: " + currentName.substring(1));
          }
        }
      }
      
    }    
    ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
    long[] ids = tbean.getAllThreadIds();
    clw.writeLineToCommandLine("Threadstate @ " + Constants.defaultUTCSimpleDateFormatWithMS().format(new Date()));
    ThreadInfo[] tis = tbean.getThreadInfo(ids, 200);
    StringBuilder sb = new StringBuilder();
    int countThreadWithShortStackTrace =0;
    for (ThreadInfo ti : tis) {
      if (ti != null) { //null if thread is not active any more
        boolean matchesThreadFilter = (nameFilter == null || nameFilter.equalsIgnoreCase(ti.getThreadName()));
        if (!matchesThreadFilter) {
          continue;
        }
        if( ti.getStackTrace().length < minStackTraceLength ) {
          ++countThreadWithShortStackTrace;
          continue;
        }
        if (tbean.isThreadCpuTimeEnabled() || tbean.isThreadContentionMonitoringEnabled()) {
          sb.append("  Thread.");
          if (tbean.isThreadCpuTimeEnabled()) {
            sb.append("cputime=").append(tbean.getThreadCpuTime(ti.getThreadId())/1000000);
            sb.append("ms\n   .usertime=").append(tbean.getThreadUserTime(ti.getThreadId())/1000000);
            sb.append("ms\n   ");
          }
          if (tbean.isThreadContentionMonitoringEnabled()) {
            sb.append(".blocked=").append(ti.getBlockedTime());
            sb.append("ms\n   .waited=").append(ti.getWaitedTime());
            sb.append("ms\n");
          }
        }
        sb.append(getThreadInfo(ti));
      }
    }
    if( minStackTraceLength > 0 ) {
      clw.writeLineToCommandLine("Not listed "+countThreadWithShortStackTrace+" threads with stacktrace < "+minStackTraceLength+" lines\n" );
    }
    long[] deadlockedThreads = tbean.findMonitorDeadlockedThreads();
    if (deadlockedThreads != null && deadlockedThreads.length > 0) {
      sb.append("\n\nFound deadlocked Threads:\n");
      for (long tid : deadlockedThreads) {
        ThreadInfo ti = tbean.getThreadInfo(tid);
        sb.append(getThreadInfo(ti));
      }
    }
    clw.writeLineToCommandLine(sb.toString());
  }


  private String getThreadInfo(ThreadInfo ti) {
    //FIXME code aus java6 ThreadInfo.toString() geklaut. in java5 ist toString() sehr dürftig.
    //leider ist in java6 toString die größe des ausgegebenen stacks auf 8 begrenzt.
    StringBuilder sb =
        new StringBuilder("\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " " + ti.getThreadState());
    if (ti.getLockName() != null) {
      sb.append(" on " + ti.getLockName());
    }
    if (ti.getLockOwnerName() != null) {
      sb.append(" owned by \"" + ti.getLockOwnerName() + "\" Id=" + ti.getLockOwnerId());
    }
    if (ti.isSuspended()) {
      sb.append(" (suspended)");
    }
    if (ti.isInNative()) {
      sb.append(" (in native)");
    }
    sb.append('\n');
    int i = 0;
    StackTraceElement[] stackTrace = ti.getStackTrace();
    for (; i < stackTrace.length && i < 200; i++) {
      StackTraceElement ste = stackTrace[i];
      sb.append("\tat " + ste.toString());
      sb.append('\n');
      if (i == 0 && ti.getLockName() != null) {
        Thread.State ts = ti.getThreadState();
        switch (ts) {
          case BLOCKED :
            sb.append("\t-  blocked on " + ti.getLockName());
            sb.append('\n');
            break;
          case WAITING :
            sb.append("\t-  waiting on " + ti.getLockName());
            sb.append('\n');
            break;
          case TIMED_WAITING :
            sb.append("\t-  waiting on " + ti.getLockName());
            sb.append('\n');
            break;
          default :
        }
      }
    }
    if (i < stackTrace.length) {
      sb.append("\t...");
      sb.append('\n');
    }

    sb.append('\n');
    return sb.toString();
  }  
  
}
