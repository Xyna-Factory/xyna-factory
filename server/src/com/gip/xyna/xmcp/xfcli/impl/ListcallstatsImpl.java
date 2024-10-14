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

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xmcp.Channel;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listcallstats;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher.CallStatsType;



public class ListcallstatsImpl extends XynaCommandImplementation<Listcallstats> {

  public void execute(OutputStream statusOutputStream, Listcallstats payload) throws XynaException {
    Channel mcp = factory.getXynaMultiChannelPortalPortal();

    if (mcp == null) {
      writeLineToCommandLine(statusOutputStream, XynaMultiChannelPortal.DEFAULT_NAME + " is currently not available.");
      return;
    }

    FactoryRuntimeStatistics runtimeStatistics =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
    
    StatisticsValue<HashMap<String, Map<String, StatisticsValue>>> value = runtimeStatistics.getStatisticsValue(PredefinedXynaStatisticsPath.ORDERSTATISTICS, true);
    
    HashMap<String, Map<String, StatisticsValue>> localValue = null;
    HashMap<String, Map<String, StatisticsValue>> remoteValue = null;

    if (payload.getVerbose()) {
      localValue = (HashMap<String, Map<String, StatisticsValue>>) runtimeStatistics.getStatisticsValue(PredefinedXynaStatisticsPath.ORDERSTATISTICS, false).getValue();
      remoteValue = deriveRemoteValue(value.getValue(), localValue);
    }

    int maxOrderTypeStringLength = 40;
    for (String ordertype : value.getValue().keySet()) {
      if (ordertype != null) {
        maxOrderTypeStringLength = Math.max(maxOrderTypeStringLength, ordertype.length() + 2);
      }
    }

    writeLineToCommandLine(statusOutputStream, "Listing call statistics...");
    String headerFormat = "  %-" + maxOrderTypeStringLength + "s  %8s %8s %8s %8s";
    String lineFormat = "  %-" + maxOrderTypeStringLength + "s: %8d %8d %8d %8d";

    String headerLine = String.format(headerFormat, "Ordertype", "calls", "finished", "timeout", "errors");
    writeLineToCommandLine(statusOutputStream, headerLine);

    for (Entry<String, Map<String, StatisticsValue>> ordertypeStatsEntry : value.getValue().entrySet()) {

      String ordertype = ordertypeStatsEntry.getKey();
      
      if (ordertype != null) { 
        long calls = (Long) ordertypeStatsEntry.getValue().get(CallStatsType.STARTED.getPartName()).getValue();
        long finished = (Long) ordertypeStatsEntry.getValue().get(CallStatsType.FINISHED.getPartName()).getValue();
        long timeouts = (Long) ordertypeStatsEntry.getValue().get(CallStatsType.TIMEOUT.getPartName()).getValue();
        long errors = (Long) ordertypeStatsEntry.getValue().get(CallStatsType.FAILED.getPartName()).getValue();
  
        String output = String.format(lineFormat, ordertype, calls, finished, timeouts, errors);
        writeLineToCommandLine(statusOutputStream, output);
  
  
        if (payload.getVerbose()) {
          
          Map<String, StatisticsValue> localmap = localValue.get(ordertypeStatsEntry.getKey());
          Map<String, StatisticsValue> remotemap = remoteValue.get(ordertypeStatsEntry.getKey());
  
          long localcalls = 0;
          long localfinished = 0;
          long localtimeouts = 0;
          long localerrors = 0;
          if (localmap != null) {
            StatisticsValue statsvalue = localmap.get(CallStatsType.STARTED.getPartName());
            if (statsvalue != null) {
              localcalls = (Long) statsvalue.getValue();
            }
            statsvalue = localmap.get(CallStatsType.FINISHED.getPartName());
            if (statsvalue != null) {
              localfinished = (Long) statsvalue.getValue();
            }
            statsvalue = localmap.get(CallStatsType.TIMEOUT.getPartName());
            if (statsvalue != null) {
              localtimeouts = (Long) statsvalue.getValue();
            }
            statsvalue = localmap.get(CallStatsType.FAILED.getPartName());
            if (statsvalue != null) {
              localerrors = (Long) statsvalue.getValue();
            }
          }
          
          long remotecalls = 0;
          long remotefinished = 0;
          long remotetimeouts = 0;
          long remoteerrors = 0;
          if (remotemap != null) {
            StatisticsValue statsvalue = remotemap.get(CallStatsType.STARTED.getPartName());
            if (statsvalue != null) {
              remotecalls = (Long) statsvalue.getValue();
            }
            statsvalue = remotemap.get(CallStatsType.FINISHED.getPartName());
            if (statsvalue != null) {
              remotefinished = (Long) statsvalue.getValue();
            }
            statsvalue = remotemap.get(CallStatsType.TIMEOUT.getPartName());
            if (statsvalue != null) {
              remotetimeouts = (Long) statsvalue.getValue();
            }
            statsvalue = remotemap.get(CallStatsType.FAILED.getPartName());
            if (statsvalue != null) {
              remoteerrors = (Long) statsvalue.getValue();
            }
          }
          
          String outputLocal = String.format("  %" + maxOrderTypeStringLength + "s  %8d %8d %8d %8d (local)", "",
                                             localcalls, localfinished, localtimeouts, localerrors);
          writeLineToCommandLine(statusOutputStream, outputLocal);
  
          String outputRemote = String.format("  %" + maxOrderTypeStringLength + "s  %8d %8d %8d %8d (remote)", "",
                                              remotecalls, remotefinished, remotetimeouts, remoteerrors);
          writeLineToCommandLine(statusOutputStream, outputRemote);
        }
      }
    }
  }
  
  
  private HashMap<String, Map<String, StatisticsValue>> deriveRemoteValue(HashMap<String, Map<String, StatisticsValue>> all,
                                                                          HashMap<String, Map<String, StatisticsValue>> local) {
    HashMap<String, Map<String, StatisticsValue>> remote = new HashMap<String, Map<String, StatisticsValue>>();
    for (Entry<String, Map<String, StatisticsValue>> ordertypeEntry : all.entrySet()) {
      Map<String, StatisticsValue> localOrdertypeMap = local.get(ordertypeEntry.getKey());
      if (localOrdertypeMap == null) {
        remote.put(ordertypeEntry.getKey(), ordertypeEntry.getValue());
      } else {
        Map<String, StatisticsValue> remoteMap = new HashMap<String, StatisticsValue>();
        remote.put(ordertypeEntry.getKey(), remoteMap);
        for (Entry<String, StatisticsValue> valueEntry : ordertypeEntry.getValue().entrySet()) {
          Long remoteValue = (Long) valueEntry.getValue().getValue();
          Long localValue = (Long) localOrdertypeMap.get(valueEntry.getKey()).getValue();
          remoteMap.put(valueEntry.getKey(), new LongStatisticsValue(remoteValue - localValue));
        }
      }
    }
    return remote;
  }

}
