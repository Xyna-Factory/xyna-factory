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
package com.gip.xyna.xfmg.xfmon.processmonitoring.profiling;


import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.UnknownPathOnTraversalHandling;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsNodeTraversal;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.StatisticsPathPart;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PullStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.PushStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.Statistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.LongStatisticsValue;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.Handler;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.ProcessStepHandlerType;



public class ProcessProfiling {
  
  private static final WorkflowProfilingHandlers handlers = new WorkflowProfilingHandlers();
  private static final Logger logger = CentralFactoryLogging.getLogger(ProcessProfiling.class); 
  static final long MEAN_BUFFER_TIME_FRAME = 60 * 60 * 1000L;
  
  
  
  // factory statistics path:
  // XPRC, XPCE, STATS, EXECUTIONTIME , ORDERS     , {ordertype}, Application-{application}, Latest
  //                                                                                           ^ MeanAggregationBuffer lives here 
  //                                                                                             Is a PushStatistic where latest values can be pushed 
  //                                                                                       , Last5Min
  //                                                                                       , Last60Min
  //                                                                                       , Total
  //                                                                                           ^ Those 3 statistics pull data from the buffer
  //                                                            , WorkingSet
  //                                                                   ^ same layout as Applications
  //                                                            , ALL
  //                                                               ^ Aggregate over Applications & WorkingSet
  //                                    OPERATIONS, {datatype}, {operation}, ... same layout as ORDERS below this point  
  
  public void enableServiceProfiling(ServiceIdentifier id) throws XPRC_DESTINATION_NOT_FOUND, XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    if (id.isWorkflowIdentifier()) {
      enableWorkflowProfiling(id);
    } else {
      throw new UnsupportedOperationException("ServiceOperation profiling is currently not suported");
    }
  }
  
  
  public void disableServiceProfiling(ServiceIdentifier id) throws XPRC_DESTINATION_NOT_FOUND {
    if (id.isWorkflowIdentifier()) {
      disableWorkflowProfiling(id);
    } else {
      throw new UnsupportedOperationException("ServiceOperation profiling is currently not suported");
    }
  }
  
  
  public void enableWorkflowProfiling(ServiceIdentifier wfId) throws XPRC_DESTINATION_NOT_FOUND, XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    StatisticsPath path = getWorkflowExecutionTimeStatisticsPath(wfId.getOrdertype(), wfId.getAdjustedApplicationNameForStatistics());
    MeanExecutionTimeStatistics statistics = new MeanExecutionTimeStatistics(path.append(ProfilingStatisticTypes.LATEST));
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(statistics);
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(
      new PullStatistics<Double, MeanAggregationStatisticsValue>(path.append(ProfilingStatisticTypes.LAST5MIN)) {
        @Override
        public MeanAggregationStatisticsValue getValueObject() {
          StatisticsPath pathToBuffer = new StatisticsPathImpl(path.getPath().subList(0, path.getPath().size() - 1));
          pathToBuffer = pathToBuffer.append(ProfilingStatisticTypes.LATEST); // TODO cache
          Statistics bufferStats;
          try {
            bufferStats = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().getStatistic(pathToBuffer);
          } catch (XFMG_InvalidStatisticsPath e) {
            // should not happen as we registered above
            throw new RuntimeException("MainProfiling-Statistic could not be retrieved.", e);
          }
          if (bufferStats != null && bufferStats instanceof MeanExecutionTimeStatistics) {
            return ((MeanExecutionTimeStatistics)bufferStats).getMeanAggregationBuffer().getFromTimespan(5 * 60 * 1000);
          } else {
            throw new RuntimeException("No Profiling-Statistic under: " + pathToBuffer);
          }
        }
        @Override
        public String getDescription() { return ""; }
      });
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(
      new PullStatistics<Double, MeanAggregationStatisticsValue>(path.append(ProfilingStatisticTypes.LAST60MIN)) {
        @Override
        public MeanAggregationStatisticsValue getValueObject() {
          StatisticsPath pathToBuffer = new StatisticsPathImpl(path.getPath().subList(0, path.getPath().size() - 1));
          pathToBuffer = pathToBuffer.append(ProfilingStatisticTypes.LATEST); // TODO cache
          Statistics bufferStats;
          try {
            bufferStats = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().getStatistic(pathToBuffer);
          } catch (XFMG_InvalidStatisticsPath e) {
            throw new RuntimeException("MainProfiling-Statistic could not be retrieved.", e);
          }
          if (bufferStats != null && bufferStats instanceof MeanExecutionTimeStatistics) {
            return ((MeanExecutionTimeStatistics)bufferStats).getMeanAggregationBuffer().getFromTimespan(59 * 60 * 1000);
          } else {
            throw new RuntimeException("No buffer stats under: " + pathToBuffer);
          }
        }
        @Override
        public String getDescription() { return ""; }
      });
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(
      new PullStatistics<Double, MeanAggregationStatisticsValue>(path.append(ProfilingStatisticTypes.TOTAL)) {
        @Override
        public MeanAggregationStatisticsValue getValueObject() {
          StatisticsPath pathToBuffer = new StatisticsPathImpl(path.getPath().subList(0, path.getPath().size() - 1));
          pathToBuffer = pathToBuffer.append(ProfilingStatisticTypes.LATEST); // TODO cache
          Statistics bufferStats;
          try {
            bufferStats = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().getStatistic(pathToBuffer);
          } catch (XFMG_InvalidStatisticsPath e) {
            throw new RuntimeException("MainProfiling-Statistic could not be retrieved.", e);
          }
          if (bufferStats != null && bufferStats instanceof MeanExecutionTimeStatistics) {
            return ((MeanExecutionTimeStatistics)bufferStats).getMeanAggregationBuffer().getTotal();
          } else {
            throw new RuntimeException("No buffer stats under: " + pathToBuffer);
          }
        }
        @Override
        public String getDescription() { return ""; }
      });
    
    // generate aggregation ?
    
    for (DestinationKey dk : wfId.getDestinationKeysForWorkflowIdentifier()) {
      DestinationValue dv = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionDestination(dk);
      if (dv.getDestinationType() != ExecutionType.XYNA_FRACTAL_WORKFLOW) {
        throw new RuntimeException("Destination is not a Workflow");
      }
      
      XynaFactory.getInstance().getProcessing().getWorkflowEngine().getStepHandlerManager().addHandler(dk, dv, ProcessStepHandlerType.POSTHANDLER, handlers.stopHandler);
      XynaFactory.getInstance().getProcessing().getWorkflowEngine().getStepHandlerManager().addHandler(dk, dv, ProcessStepHandlerType.PREHANDLER, handlers.startHandler);
      XynaFactory.getInstance().getProcessing().getWorkflowEngine().getStepHandlerManager().addHandler(dk, dv, ProcessStepHandlerType.ERRORHANDLER, handlers.errorHandler);
      // TODO addUnDeployment handler for that wf?
    }
  }
  
  
  public void disableWorkflowProfiling(ServiceIdentifier wfId) throws XPRC_DESTINATION_NOT_FOUND {
    for (DestinationKey dk : wfId.getDestinationKeysForWorkflowIdentifier()) {
      DestinationValue dv = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaExecution().getExecutionDestination(dk);
      if (dv.getDestinationType() != ExecutionType.XYNA_FRACTAL_WORKFLOW) {
        throw new RuntimeException("Destination is not a Workflow");
      }
      
      XynaFactory.getInstance().getProcessing().getWorkflowEngine().getStepHandlerManager().removeHandler(dk, dv, ProcessStepHandlerType.POSTHANDLER, handlers.stopHandler);
      XynaFactory.getInstance().getProcessing().getWorkflowEngine().getStepHandlerManager().removeHandler(dk, dv, ProcessStepHandlerType.PREHANDLER, handlers.startHandler);
      XynaFactory.getInstance().getProcessing().getWorkflowEngine().getStepHandlerManager().removeHandler(dk, dv, ProcessStepHandlerType.ERRORHANDLER, handlers.errorHandler);
    }
    
    StatisticsPath path = getWorkflowExecutionTimeStatisticsPath(wfId.getOrdertype(), wfId.getAdjustedApplicationNameForStatistics());
    for (ProfilingStatisticTypes type : ProfilingStatisticTypes.values()) {
      try {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().unregisterStatistic(path.append(type));
      } catch (XFMG_InvalidStatisticsPath e) {
        throw new RuntimeException("",e);
      }
    }
    
    // unregister aggregation once we create it
  }
  
  
  /*public void enableOperationProfiling(String datatypename, String operationname, String applicationName) {
    // TODO
    // find all wfs calling the operation
    // find all xmlIds of calls within those wfs
    // add Handler for those wfs that check the xmlId
  }
  
  
  public void disableOperationProfiling(String datatypename, String operationname, String applicationName) {
    // TODO
  }*/
  
  
  private static StatisticsPath getWorkflowExecutionTimeStatisticsPath(String ordertype, String applicationName) {
    return PredefinedXynaStatisticsPath.EXECUTIONTIME.append(PredefinedXynaStatisticsPathPart.WORKFLOW).append(applicationName).append(ordertype);
  }
  
  
  private static enum ProfilingStatisticTypes implements StatisticsPathPart {
    LATEST("Latest"),
    LAST5MIN("Last5Min"),
    LAST60MIN("Last60Min"),
    TOTAL("Total");

    private final String partname;
    
    private ProfilingStatisticTypes(String partname) {
      this.partname = partname;
    }
    
    public String getPartName() {
      return partname;
    }

    public StatisticsNodeTraversal getStatisticsNodeTraversal() {
      return StatisticsNodeTraversal.SINGLE;
    }

    public UnknownPathOnTraversalHandling getUnknownPathHandling() {
      return UnknownPathOnTraversalHandling.THROW_IF_ANY;
    }
    
  }
  
  
  private static class WorkflowProfilingHandlers {
    
    private final Handler startHandler;
    private final Handler stopHandler;
    private final Handler errorHandler;
    private final Map<Long, Long> orderIdToStartTimeMap = new HashMap<Long, Long>();
    
    private WorkflowProfilingHandlers() {
      startHandler = new Handler() {
        @Override
        public void handle(XynaProcess process, FractalProcessStep<?> pstep) {
          if (isStart(process, pstep)) {
            Long orderid = process.getCorrelatedXynaOrder().getId();
            orderIdToStartTimeMap.put(orderid, System.currentTimeMillis());
          }
        }
      };
      stopHandler = new Handler() {
        @Override
        public void handle(XynaProcess process, FractalProcessStep<?> pstep) {
          if (isStop(process, pstep)) {
            endMeasurement(process);
          }
        }
      };
      errorHandler = new Handler() {
        
        @Override
        public void handle(XynaProcess process, FractalProcessStep<?> pstep) {
          if (isOneOfStartSteps(process, pstep)) {
            endMeasurement(process);
          }
        }
      };
      
    }
    
    
    private void endMeasurement(XynaProcess process) {
      Long orderid = process.getCorrelatedXynaOrder().getId();
      Long starttime = orderIdToStartTimeMap.remove(orderid);
      if (starttime != null) {
        Long duration = System.currentTimeMillis() - starttime;
        
        StatisticsPath path = getWorkflowExecutionTimeStatisticsPath(process.getCorrelatedXynaOrder().getDestinationKey().getOrderType(),
                                                                     ServiceIdentifier.adjustApplicationNameForStatistics(process.getCorrelatedXynaOrder().getDestinationKey().getRuntimeContext()))
                                                                     .append(ProfilingStatisticTypes.LATEST);
        try {
          Statistics stats = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().getStatistic(path);
          if (stats != null && stats instanceof PushStatistics) {
            ((PushStatistics)stats).pushValue(new LongStatisticsValue(duration));
          } else {
            logger.warn("Could not retrieve profiling statistic.");
          }
        } catch (XFMG_InvalidStatisticsPath e) {
          logger.warn("Could not retrieve profiling statistic.",e);
        }
        
      }
    }
    
    private static boolean isStart(XynaProcess process, FractalProcessStep<?> pstep) {
      return process.getStartSteps()[0] == pstep;
    }
    
    private static boolean isStop(XynaProcess process, FractalProcessStep<?> pstep) {
      return process.getStartSteps()[process.getStartSteps().length - 1] == pstep;
    }
    
    private static boolean isOneOfStartSteps(XynaProcess process, FractalProcessStep<?> pstep) {
      for (FractalProcessStep<?> startStep : process.getStartSteps()) {
        if (startStep == pstep) {
          return true;
        }
      }
      return false;
    }
    
  }
  
  
}
