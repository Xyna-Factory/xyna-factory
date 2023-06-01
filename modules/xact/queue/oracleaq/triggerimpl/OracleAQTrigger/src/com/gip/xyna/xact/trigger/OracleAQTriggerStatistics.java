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
package com.gip.xyna.xact.trigger;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.oracleaq.shared.SQLUtilsCreator;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.AggregationStatisticsFactory;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsNodeTraversal;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.UnknownPathOnTraversalHandling;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PullStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.Statistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StringStatisticsValue;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


/**
 *
 */
public class OracleAQTriggerStatistics {
  
  private static Logger logger = CentralFactoryLogging.getLogger(OracleAQTriggerStatistics.class);
  
  private String instanceName;
  private String queueName;
  private AtomicLong receivedCounter = new AtomicLong(0);
  private AtomicLong rejectCounter = new AtomicLong(0);
  private long lastDequeueTime = 0L;
  private SQLUtilsCreator requestConnectionPool;
  private SQLUtilsCreator responseConnectionPool;
  
  public OracleAQTriggerStatistics(String instanceName, String queueName) {
    this.instanceName = instanceName;
    this.queueName = queueName;
  }

  public void setRequestConnectionPool(SQLUtilsCreator requestConnectionPool) {
    this.requestConnectionPool = requestConnectionPool;
  }

  public void setResponseConnectionPool(SQLUtilsCreator responseConnectionPool) {
    this.responseConnectionPool = responseConnectionPool;
  }

  
  private enum OracleAQTriggerStatisticType implements StatisticsPathPart {
    INSTANCENAME("InstanceName"),
    QUEUENAME("QueueName"),
    LASTDEQUEUE("LastDequeueTime"),
    CONNECTIONS("ActiveConnections"),
    PROCESSED("ProcessedOverall");
    
    private OracleAQTriggerStatisticType(String partname) {
      this.partname = partname;
    }
 
    private final String partname;
    
    public String getPartName() {
      return partname;
    }

    public StatisticsNodeTraversal getStatisticsNodeTraversal() {
      return StatisticsNodeTraversal.SINGLE;
    }
    
  }
  
  
  private StatisticsPath instancePathPath;
  
  private StatisticsPath getInstanceBasePath(TriggerInstanceIdentification triggerId) {
    if (instancePathPath == null) {
      StatisticsPath path = PredefinedXynaStatisticsPath.ORACLEAQTRIGGER;
      if (triggerId.getRevision() == null || triggerId.getRevision() == -1L) {
        path = path.append("WorkingSet");
      } else {
        try {
          ApplicationName applicationName = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getVersionManagement().getApplicationName(triggerId.getRevision());
          path = path.append("Application-" + applicationName.getName());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("Could not find application name for revision " + triggerId.getRevision() + " using WorkingSet");
          path = path.append("WorkingSet");
        }
        
      }
      instancePathPath = path.append(triggerId.getInstanceName());
    } 
    return instancePathPath;
  }
 

  public void register(final OracleAQTrigger trigger) {
    try {
      FactoryRuntimeStatistics statistics = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
      statistics.registerStatistic(new PullStatistics<String, StringStatisticsValue>(getInstanceBasePath(trigger.getTriggerInstanceIdentification()).append(OracleAQTriggerStatisticType.INSTANCENAME)) {
        @Override
        public StringStatisticsValue getValueObject() { return new StringStatisticsValue(trigger.getTriggerInstanceIdentification().getInstanceName()); }
        @Override
        public String getDescription() { return "OracleAQTrigger instance name"; }
      });
      statistics.registerStatistic(new PullStatistics<String, StringStatisticsValue>(getInstanceBasePath(trigger.getTriggerInstanceIdentification()).append(OracleAQTriggerStatisticType.QUEUENAME)) {
        @Override
        public StringStatisticsValue getValueObject() { return new StringStatisticsValue(queueName); }
        @Override
        public String getDescription() { return "OracleAQTrigger queue name"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath(trigger.getTriggerInstanceIdentification()).append(OracleAQTriggerStatisticType.PROCESSED)) {
        @Override
        public LongStatisticsValue getValueObject() { return new LongStatisticsValue(receivedCounter.get() - rejectCounter.get()); }
        @Override
        public String getDescription() { return "The amount of events that were received and not rejected"; }
      });
      statistics.registerStatistic(new PullStatistics<Long, LongStatisticsValue>(getInstanceBasePath(trigger.getTriggerInstanceIdentification()).append(OracleAQTriggerStatisticType.CONNECTIONS)) {
        @Override
        public LongStatisticsValue getValueObject() {
            int active = requestConnectionPool.getActiveConnections();
            if( responseConnectionPool != null ) {
              active += responseConnectionPool.getActiveConnections();
            }
            return new LongStatisticsValue(Long.valueOf(active));
          }
        @Override
        public String getDescription() { return "The amount of active database connections"; }
      });
      statistics.registerStatistic(new PullStatistics<String, StringStatisticsValue>(getInstanceBasePath(trigger.getTriggerInstanceIdentification()).append(OracleAQTriggerStatisticType.LASTDEQUEUE)) {
        @Override
        public StringStatisticsValue getValueObject() { return new StringStatisticsValue(Constants.defaultUTCSimpleDateFormat().format( new Date(lastDequeueTime))); }
        @Override
        public String getDescription() { return "The last time a massage was dequeued"; }
      });
      
      
      // register aggregations over applications
      for (OracleAQTriggerStatisticType statisticType : OracleAQTriggerStatisticType.values()) {
        StatisticsPath ownPath = PredefinedXynaStatisticsPath.ORACLEAQTRIGGER.append(StatisticsPathImpl.simplePathPart("All"))
                                                                             .append(trigger.getTriggerInstanceIdentification().getInstanceName())
                                                                             .append(statisticType);
        StatisticsPath pathToAggregate = PredefinedXynaStatisticsPath.ORACLEAQTRIGGER
          .append(new StatisticsPathImpl.BlackListFilter("All"))
          .append(trigger.getTriggerInstanceIdentification().getInstanceName())
          .append(statisticType);
        Statistics aggregate = AggregationStatisticsFactory.generateDefaultAggregationStatistics(ownPath, pathToAggregate);
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(aggregate);
      }
    } catch (Exception e) {
      logger.warn("OracleAQTrigger Statistics could not be initialized. ", e);
    }
  }
  
  

  /**
   * 
   */
  public void unregister(OracleAQTrigger trigger) {
    FactoryRuntimeStatistics statistics = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
    try {
      statistics.unregisterStatistic(getInstanceBasePath(trigger.getTriggerInstanceIdentification()).append(StatisticsPathImpl.ALL));
    } catch (XFMG_InvalidStatisticsPath e) {
      logger.warn("Supplied invalid path when trying to unregister statistics",e);
    }
  }

  public void dequeueHappened(long now) {
    lastDequeueTime = now;
    receivedCounter.incrementAndGet();
  }

  public void rejectHappened() {
    rejectCounter.incrementAndGet();
  }

  
}
