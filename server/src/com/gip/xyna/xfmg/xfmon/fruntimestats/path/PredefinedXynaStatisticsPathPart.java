/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfmon.fruntimestats.path;

import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsNodeTraversal;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.UnknownPathOnTraversalHandling;


public enum PredefinedXynaStatisticsPathPart implements StatisticsPathPart {
  // NEW FORMAT EXAMPLE:
  // old: XPCE.Stats.OrderStatistics.*.OrderType
  //                                 ^ specific ordertype name
  // new: XPCE.Stats.OrderStatistics.All.*.OrderType <-- aggregation over all apps
  //      XPCE.Stats.OrderStatistics.Application-{X}.*.OrderType <-- data for a specific app
  //                                              ^ only being the app-name, not app&version or revision
  // XPCE.Stats.OrderStatistics.All.*.OrderType could be created as aggregation over XPCE.Stats.OrderStatistics.*.{!All}.OrderType
  ALL("All"),
  APPLICATION("Application"), 
  XPRC("XPRC"), XSCHED("XSched"), CORE("Core") ,
                                  CC("CC"), CAPACITIES("Capacities"),
                                            VETOS("Capacities"),
                XPCE("XPCE"), STATS("Stats"), ORDERSTATS("OrderStatistics"),
                                              PROFILING("Profiling"), EXECTIME("ExecutionTime"), WORKFLOW("Workflow"),
                                                                                                 OPERATION("Operation"),
  XFMG("XFMG"), XFMON("XFMon"), SYSINFO("SysInfo"), THREADPOOLS("ThreadPools"),
  XFWH("XFWH"), XSTOI("XStoI"), CONNECTIONPOOLS("ConnectionPools"),
  XACT("XACT"), XTRIG("XTrig"), IMPLS("Impls"), DHCP("DHCP")
                                              , HTTP("HTTP")
                                              , ORACLEAQ("OracleAQ");
  
  private final String name;
  
  private PredefinedXynaStatisticsPathPart(String name) {
    this.name = name;
  }

  public String getPartName() {
    return name;
  }

  public StatisticsNodeTraversal getStatisticsNodeTraversal() {
    return StatisticsNodeTraversal.SINGLE;
  }
  
  @Override
  public String toString() {
    return name;
  }

}
