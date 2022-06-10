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

package com.gip.xyna.xfmg.statistics;



import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;



public class XynaStatistics extends FunctionGroup {

  public static final String DEFAULT_NAME = "XynaStatistics";
  public static final int FUTUREEXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  private Map<String, StatisticsReporter> statMap = new HashMap<String, StatisticsReporter>();
  private VMStatistics vmStat = null;
  
  
  public interface StatisticsReportEntry {   
    
    static final String DEFAULT_DESCRIPTION = "no description";
    
    public Object getValue();
    public String getValuePath();
    public default String getDescription() {
      return DEFAULT_DESCRIPTION;
    };
  }

  
  public interface StatisticsReporter {
    public StatisticsReportEntry[] getStatisticsReport();
  }
  
  
  public XynaStatistics() throws XynaException {
    super();
  }
  
  
  public void registerNewStatistic(String key, StatisticsReporter evaluator) {
    // test that key and value exists and value responds to get()
    if ((key != null) && (evaluator != null) && (evaluator.getStatisticsReport() != null)) {
      if (!statMap.containsKey(key)) {
        statMap.put(key, evaluator);
        if (logger.isDebugEnabled()) {
          logger.debug("registered statistics for " + key);
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("statistics for " + key + " already registered");
        }
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("unable to register statistics for " + key);
      }
    }
  }


  public void unregisterStatistics(String key) {
    if ((key != null) && statMap.containsKey(key)) {
      statMap.remove(key);
      if (logger.isDebugEnabled()) {
        logger.debug("unregistered statistics for " + key);
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("unable to unregister statistics for " + key + " (statistics for " + key + " not found)");
      }
    }
  }


  public Object getStatistics(String key) {
    if ( key == null ) {
      return null;
    }
    
    for (String mapKey : statMap.keySet()) {
      if ( key.startsWith(mapKey) ) {
        StatisticsReporter sr = statMap.get(mapKey);
        StatisticsReportEntry[] report = sr.getStatisticsReport();
        
        for ( int i = 0; i < report.length; i++ ) {
          if ( key.equals( report[i].getValuePath() ) ) {
            return report[i].getValue();
          }
        }
      }
    }
    
    logger.debug( "Could not read statistics for " + key );
    return null;
  }


  public Map<String, StatisticsReportEntry[]> getStatisticsReadOnly() {
    Map<String, StatisticsReportEntry[]> readOnly = new HashMap<String, StatisticsReportEntry[]>();

    for (Entry<String, StatisticsReporter> entry : statMap.entrySet()) {
      StatisticsReportEntry[] value = entry.getValue().getStatisticsReport();
      readOnly.put(entry.getKey(), value);
    }

    return readOnly;
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() {
    vmStat = new VMStatistics();
  }


  @Override
  protected void shutdown() {
    if (vmStat != null) {
      vmStat.shutdown();
    }
  }

  
}
