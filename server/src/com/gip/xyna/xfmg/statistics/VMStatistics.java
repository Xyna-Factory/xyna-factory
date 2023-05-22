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

package com.gip.xyna.xfmg.statistics;

import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PullStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;


public class VMStatistics {

  public static final int FUTUREEXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();
  
  public VMStatistics() {
    FutureExecution fe = XynaFactory.getInstance().getFutureExecution();
    fe.execAsync(new FutureExecutionTask(VMStatistics.FUTUREEXECUTION_ID) {

      @Override
      public int[] after() {
        return new int[] {XynaStatistics.FUTUREEXECUTION_ID};
      }
      
      @Override
      public void execute() {
        init();
      }
      
    });

  }


  void init() {
    StatisticsPath basePath = PredefinedXynaStatisticsPath.SYSTEMINFO;

    PullStatistics<Long, LongStatisticsValue> maxHeapSize =
        new PullStatistics<Long, LongStatisticsValue>(basePath.append("JVMMaxHeapSize")) {
          @Override
          public LongStatisticsValue getValueObject() {
            Long value = Runtime.getRuntime().maxMemory();
            return new LongStatisticsValue(value / 1024);
          }
          @Override
          public String getDescription() {
            return "Maximum heap size of the JVM in kilo bytes [kB].";
          }
        };
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(maxHeapSize);
    } catch (XFMG_InvalidStatisticsPath e) {
      throw new RuntimeException("", e);
    } catch (XFMG_StatisticAlreadyRegistered e) {
    }

    PullStatistics<Long, LongStatisticsValue> currentUsedHeapSize =
        new PullStatistics<Long, LongStatisticsValue>(basePath.append("JVMCurrentUsedHeap")) {
          @Override
          public LongStatisticsValue getValueObject() {
            Long value = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            return new LongStatisticsValue(value / 1024);
          }
          @Override
          public String getDescription() {
            return "Amount of JVM heap space currently used in kilo bytes [kB].";
          }
        };
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(currentUsedHeapSize);
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("", e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
      }

    PullStatistics<Long, LongStatisticsValue> currentHeapSize =
        new PullStatistics<Long, LongStatisticsValue>(basePath.append("JVMCurrentHeapSize")) {
          @Override
          public LongStatisticsValue getValueObject() {
            Long value = Runtime.getRuntime().totalMemory();
            return new LongStatisticsValue(value / 1024);
          }
          @Override
          public String getDescription() {
            return "Current heap size of the JVM in kilo bytes [kB].";
          }
        };
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(currentHeapSize);
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("", e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
      }

    PullStatistics<Long, LongStatisticsValue> uptime =
        new PullStatistics<Long, LongStatisticsValue>(basePath.append("UpTime")) {
          @Override
          public LongStatisticsValue getValueObject() {
            Long value = System.currentTimeMillis() - XynaFactory.STARTTIME;
            return new LongStatisticsValue(value / 1000);
          }
          @Override
          public String getDescription() {
            return "Uptime of the VM in seconds [s].";
          }
        };
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(uptime);
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("", e);
      } catch (XFMG_StatisticAlreadyRegistered e) {
      }

  }


  public void shutdown() {
  }

}
