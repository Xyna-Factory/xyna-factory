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

import java.util.Arrays;
import java.util.List;


import static com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPathPart.*;


public enum PredefinedXynaStatisticsPath implements StatisticsPath {
  
  SCHEDULER(XPRC, XSCHED, CORE),
  SYSTEMINFO(XFMG, XFMON, SYSINFO),
  ORDERSTATISTICS(XPRC, XPCE, STATS, ORDERSTATS),
  EXECUTIONTIME(XPRC, XPCE, STATS, PROFILING, EXECTIME),
  CAPACITYMANAGEMENT(XPRC, XSCHED, CC, CAPACITIES),
  VETOMANAGEMENT(XPRC, XSCHED, CC, VETOS),
  THREADPOOLINFO(XFMG, XFMON, SYSINFO, THREADPOOLS),
  CONNECTIONPOOLINFO(XFWH, XSTOI, CONNECTIONPOOLS),
  DHCPTRIGGER(XACT, XTRIG, IMPLS, DHCP),
  HTTPTRIGGER(XACT, XTRIG, IMPLS, HTTP),
  ORACLEAQTRIGGER(XACT, XTRIG, IMPLS, ORACLEAQ);
  
  private List<StatisticsPathPart> parts;
  
  
  private PredefinedXynaStatisticsPath(StatisticsPathPart... parts) {
    this.parts = Arrays.asList(parts);
  }

  public List<StatisticsPathPart> getPath() {
    return parts;
  }

  public StatisticsPathPart getPathPart(int index) {
    return parts.get(index);
  }

  public StatisticsPath append(StatisticsPathPart part) {
    return new StatisticsPathImpl(parts).append(part);
  }

  public boolean isSimple() {
    return true;
  }

  public int length() {
    return parts.size();
  }

  public StatisticsPath append(StatisticsPath path) {
    return new StatisticsPathImpl(parts).append(path);
  }

  public StatisticsPath append(String part) {
    return append(StatisticsPathImpl.simplePathPart(part));
  }
  
}
