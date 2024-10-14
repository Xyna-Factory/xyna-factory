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

package com.gip.xyna.xnwh.persistence;




import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.db.ConnectionPool;
import com.gip.xyna.utils.db.ConnectionPool.ConnectionInformation;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.SNMPVarTypeLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReportEntryLegacy;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy.StatisticsReporterLegacy;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PullStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.IntegerStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;



public class ConnectionPoolStatistics {

  private List<ConnectionPoolStatisticsHelper> registeredPools = new ArrayList<ConnectionPoolStatisticsHelper>();


  ConnectionPoolStatistics() {
    ConnectionPool[] conPools = ConnectionPool.getAllRegisteredConnectionPools();
    
    // FIXME: ConnectionPools created afterwards will not show up in the statistics until a restart is issued
    for (ConnectionPool pool : conPools) {
      ConnectionPoolStatisticsHelper statHelper = new ConnectionPoolStatisticsHelper(pool);
      registeredPools.add(statHelper);

      String application = null;

      final ConnectionPool poolCopy = pool;
      
      StatisticsPath basePath = PredefinedXynaStatisticsPath.CONNECTIONPOOLINFO;

      PullStatistics<String, StringStatisticsValue> name =
          new PullStatistics<String, StringStatisticsValue>(basePath.append(pool.getId()).append("Name")) {
            @Override
            public StringStatisticsValue getValueObject() {
              return new StringStatisticsValue(poolCopy.getId());
            }
            @Override
            public String getDescription() {
              return "";
            }
          };
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(name);
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("",e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
        // ntbd
      }

      PullStatistics<Integer, IntegerStatisticsValue> total =
          new PullStatistics<Integer, IntegerStatisticsValue>(basePath.append(pool.getId()).append("Total")) {
            @Override
            public IntegerStatisticsValue getValueObject() {
              return new IntegerStatisticsValue(poolCopy.getConnectionStatistics().length);
            }
            @Override
            public String getDescription() {
              return "";
            }
          };
          try {
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(total);
          } catch (XFMG_InvalidStatisticsPath e) {
            throw new RuntimeException("",e);
          } catch (XFMG_StatisticAlreadyRegistered e) {
            // ntbd
          }

      PullStatistics<Long, LongStatisticsValue> inUse =
          new PullStatistics<Long, LongStatisticsValue>(basePath.append(pool.getId()).append("InUse")) {
            @Override
            public LongStatisticsValue getValueObject() {
              Long usedCount = 0L;
              for (ConnectionInformation ci : poolCopy.getConnectionStatistics()) {
                if (ci.isInUse()) {
                  usedCount++;
                }
              }
              return new LongStatisticsValue(usedCount);
            }
            @Override
            public String getDescription() {
              return "";
            }
          };
          try {
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(inUse);
          } catch (XFMG_InvalidStatisticsPath e) {
            throw new RuntimeException("",e);
          } catch (XFMG_StatisticAlreadyRegistered e) {
            // ntbd
          }

      // this only exists for legacy snmp support
      XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy()
          .registerNewStatistic(pool.getId(), statHelper);
    }
  }


  void shutdown() {
    for (ConnectionPoolStatisticsHelper statHelper : registeredPools) {
      // why should we?
      /*XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics()
          .unregisterStatistics("XFWH.XStoI.ConnectionPools." + statHelper.getPoolID() + ".Name");
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics()
          .unregisterStatistics("XFWH.XStoI.ConnectionPools." + statHelper.getPoolID() + ".Total");
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics()
          .unregisterStatistics("XFWH.XStoI.ConnectionPools." + statHelper.getPoolID() + ".InUse");*/

      // this only exists for legacy snmp support
      XynaFactory.getInstance().getFactoryManagement().getXynaStatisticsLegacy()
          .unregisterStatistics(statHelper.getPoolID());
    }

    registeredPools.clear();
  }


  private static class ConnectionPoolStatisticsHelper implements StatisticsReporterLegacy {

    private ConnectionPool pool;


    ConnectionPoolStatisticsHelper(ConnectionPool pool) {
      this.pool = pool;
    }


    String getPoolID() {
      return pool.getId();
    }


    public StatisticsReportEntryLegacy[] getStatisticsReportLegacy() {
      StatisticsReportEntryLegacy[] report = new StatisticsReportEntryLegacy[2];
      report[0] = new StatisticsReportEntryLegacy() {

        public Object getValue() {
          Long totalCount = (long) pool.getConnectionStatistics().length;
          return totalCount;
        }


        public SNMPVarTypeLegacy getType() {
          return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
        }


        public String getDescription() {
          return "Total count of connections in pool";
        }
      };

      report[1] = new StatisticsReportEntryLegacy() {

        public Object getValue() {
          Long usedCount = 0L;

          for (ConnectionInformation ci : pool.getConnectionStatistics()) {
            if (ci.isInUse()) {
              usedCount++;
            }
          }
          
          return usedCount;
        }


        public SNMPVarTypeLegacy getType() {
          return SNMPVarTypeLegacy.UNSIGNED_INTEGER;
        }


        public String getDescription() {
          return "Count of connections in use";
        }
      };

      return report;
    }
  }
}
