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


@Deprecated
public class XynaStatisticsLegacy extends FunctionGroup {

  public static final String DEFAULT_NAME = "XynaStatisticsLegacy";
  private Map<String, StatisticsReporterLegacy> statMap = new HashMap<String, StatisticsReporterLegacy>();

  public enum SNMPVarTypeLegacy {
    INTEGER, UNSIGNED_INTEGER, OCTET_STRING, OBJECT_IDENTIFIER, NULL, UNDEFINED
  }

  @Deprecated
  public interface StatisticsReportEntryLegacy {

    public Object getValue();


    public SNMPVarTypeLegacy getType();


    public String getDescription();
  }

  @Deprecated
  public interface StatisticsReporterLegacy {

    public StatisticsReportEntryLegacy[] getStatisticsReportLegacy();
  }

  
  @Deprecated
  public XynaStatisticsLegacy() throws XynaException {
    super();
  }

  @Deprecated
  public long getUptime() {
    return System.currentTimeMillis() - XynaFactory.STARTTIME;
  }
  

  @Deprecated
  public void registerNewStatistic(String key, StatisticsReporterLegacy evaluator) {
    // test that key and value exists and value responds to get()
    if ((key != null) && (evaluator != null) && (evaluator.getStatisticsReportLegacy() != null)) {
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


  @Deprecated
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


  @Deprecated
  public StatisticsReportEntryLegacy[] getStatistics(String key) {
    if ((key != null) && statMap.containsKey(key)) {
      if (logger.isDebugEnabled()) {
        logger.debug("reporting statistics for " + key);
      }
      return statMap.get(key).getStatisticsReportLegacy();
    }

    if (logger.isDebugEnabled()) {
      logger.debug("unable to get statistics for " + key);
    }
    return null;
  }


  @Deprecated
  public Map<String, StatisticsReportEntryLegacy[]> getStatisticsReadOnly() {
    Map<String, StatisticsReportEntryLegacy[]> readOnly = new HashMap<String, StatisticsReportEntryLegacy[]>();

    for (Entry<String, StatisticsReporterLegacy> entry : statMap.entrySet()) {
      StatisticsReportEntryLegacy[] value = entry.getValue().getStatisticsReportLegacy();
      readOnly.put(entry.getKey(), value);
    }

    return readOnly;
  }


  @Deprecated
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() {
  }


  @Override
  protected void shutdown() {
  }


}
