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

package com.gip.xyna.xfmg.xfmon.processmonitoring;



import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ErrorScanningLogFile;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidStatisticsPath;
import com.gip.xyna.xfmg.exceptions.XFMG_StatisticAlreadyRegistered;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownStatistic;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.RuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.StorableAggregationStatisticsPersistenceHandler.StorableStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.AggregationStatisticsFactory;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsMapper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsMapper.MapDiscoveryStatisticsValue;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.PredefinedStatisticsReducer;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsAggregator;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsMapper;
import com.gip.xyna.xfmg.xfmon.fruntimestats.aggregation.StatisticsReducer;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.PredefinedXynaStatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPath.UnknownPathOnTraversalHandling;
import com.gip.xyna.xfmg.xfmon.fruntimestats.path.StatisticsPathImpl;
import com.gip.xyna.xfmg.xfmon.fruntimestats.statistics.AggregatedStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.values.StatisticsValue;
import com.gip.xyna.xfmg.xfmon.processmonitoring.profiling.ProcessProfiling;
import com.gip.xyna.xfmg.xfmon.processmonitoring.profiling.ServiceIdentifier;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher.CallStatsType;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchiveStatisticsStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceDetails;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;



public class ProcessMonitoring extends FunctionGroup {

  public static final String DEFAULT_NAME = "Process Monitoring";


  // TODO encapsulate this stuff into another class
  private final static String SHELL = "sh";
  private final static String SHELLPARAM = "-c";
  private final static String INVERSE_GREP_PARAM = "-v";
  private final static String SCAN_PARAM_PREFIX = "') - \\[";
  private final static String SCAN_PARAM_SUFFIX = "\\] '";

  private static Runtime runtime = Runtime.getRuntime();
  private static ProcessProfiling execTime = new ProcessProfiling();


  public ProcessMonitoring() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  public void init() throws XynaException {
    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(ProcessMonitoring.class, "ProcessMonitoring").
      after(RuntimeStatistics.class).
      execAsync(new Runnable(){ public void run(){ initStatistics(); }});
  }
  
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void initStatistics() {
        StatisticsAggregator<MapDiscoveryStatisticsValue, CompleteCallStatsValue> completeCallStatsAggregator = 
          new StatisticsAggregator<PredefinedStatisticsMapper.MapDiscoveryStatisticsValue, CompleteCallStatsValue>(
                          StatisticsPathImpl.ALL,
                          new CompleteCallStatsMapper(),
                          PredefinedStatisticsReducer.DEFAULT.typeCast(CompleteCallStatsValue.class));
        StatisticsAggregator<StatisticsValue<?>, MapDiscoveryStatisticsValue> valueAggregator =
          new StatisticsAggregator<StatisticsValue<?>, PredefinedStatisticsMapper.MapDiscoveryStatisticsValue>(StatisticsPathImpl.ALL,
                          (StatisticsMapper<StatisticsValue<?>, MapDiscoveryStatisticsValue>)
                          PredefinedStatisticsMapper.MAP_DISCOVERY.typeCast(AggregationStatisticsFactory.getGeneralizedStatisticsValueClass(), MapDiscoveryStatisticsValue.class),
                          PredefinedStatisticsReducer.DEFAULT.typeCast(MapDiscoveryStatisticsValue.class));
        completeCallStatsAggregator.setNextAggregationPart(valueAggregator);
        StatisticsPath pathToIncomplete = PredefinedXynaStatisticsPath.ORDERSTATISTICS.append(StatisticsPathImpl.simplePathPart(XynaDispatcher.AGGREGATION_APPLICATION_NAME));
        StatisticsAggregator<CompleteCallStatsValue, CompleteCallStatsValue> completeAggregator =
          AggregationStatisticsFactory.completeAggregationStack(pathToIncomplete, completeCallStatsAggregator);
        AggregatedStatistics<HashMap<String, Map<String, StatisticsValue>>, CompleteCallStatsValue> completeCallStatsStatistic =
          new AggregatedStatistics<HashMap<String,Map<String, StatisticsValue>>, CompleteCallStatsValue>(PredefinedXynaStatisticsPath.ORDERSTATISTICS, completeAggregator);
        try {
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics().registerStatistic(completeCallStatsStatistic);
        } catch (XFMG_InvalidStatisticsPath e) {
          throw new RuntimeException("Invalid path for callStats",e);
        } catch (XFMG_StatisticAlreadyRegistered e) {
          logger.warn("A different statistic is already occupying the callStats path", e);
        }
  }


  @Override
  public void shutdown() throws XynaException {
  }


  public Map<String, OrderArchiveStatisticsStorable> getStatistics() {
    return getStatistics(XynaDispatcher.WORKING_SET_APPLICATION_NAME);
  }

  @SuppressWarnings({"unchecked"})
  public Map<String, OrderArchiveStatisticsStorable> getStatistics(String application) {
    FactoryRuntimeStatistics runtimeStatistics =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryMonitoring().getFactoryRuntimeStatistics();
    
    StatisticsPath pathToIncompleteAggregationStack = XynaDispatcher.getBaseCallStatsPath();
    
    StatisticsAggregator<StorableStatisticsValue<OrderArchiveStatisticsStorable>, StorableStatisticsValue<OrderArchiveStatisticsStorable>> noReduction = 
      new StatisticsAggregator<StorableStatisticsValue<OrderArchiveStatisticsStorable>, StorableStatisticsValue<OrderArchiveStatisticsStorable>>(
                      StatisticsPathImpl.ALL,
                      (StatisticsMapper<StorableStatisticsValue<OrderArchiveStatisticsStorable>, StorableStatisticsValue<OrderArchiveStatisticsStorable>>)
                      PredefinedStatisticsMapper.DIRECT.typeCast(AggregationStatisticsFactory.getGeneralizedStatisticsValueClass(), AggregationStatisticsFactory.getGeneralizedStatisticsValueClass()),
                      (StatisticsReducer<StorableStatisticsValue<OrderArchiveStatisticsStorable>>) PredefinedStatisticsReducer.NO_REDUCTION.typeCast(AggregationStatisticsFactory.getGeneralizedStatisticsValueClass()));
    StatisticsAggregator<MapDiscoveryStatisticsValue, StorableStatisticsValue<OrderArchiveStatisticsStorable>> storableAggregator = 
      new StatisticsAggregator<MapDiscoveryStatisticsValue, StorableStatisticsValue<OrderArchiveStatisticsStorable>>(
                      new StatisticsPathImpl.WhiteListFilter(UnknownPathOnTraversalHandling.THROW_IF_ALL,
                                                             (application == null || application.equals(XynaDispatcher.WORKING_SET_APPLICATION_NAME)) ?
                                                                             XynaDispatcher.WORKING_SET_APPLICATION_NAME :
                                                                             XynaDispatcher.APPLICATION_NAME_PREFIX + application),
                      new OrderArchiveStatisticsStorableFromMapMapper(),
                      (StatisticsReducer<StorableStatisticsValue<OrderArchiveStatisticsStorable>>) PredefinedStatisticsReducer.NO_REDUCTION.typeCast(AggregationStatisticsFactory.getGeneralizedStatisticsValueClass()));
    StatisticsAggregator<StatisticsValue<?>, MapDiscoveryStatisticsValue> storableValuesAggregator =
      new StatisticsAggregator<StatisticsValue<?>, MapDiscoveryStatisticsValue>(
                      StatisticsPathImpl.ALL,
                      (StatisticsMapper<StatisticsValue<?>, MapDiscoveryStatisticsValue>) PredefinedStatisticsMapper.MAP_DISCOVERY.typeCast(AggregationStatisticsFactory.getGeneralizedStatisticsValueClass(), MapDiscoveryStatisticsValue.class),
                      PredefinedStatisticsReducer.DEFAULT.typeCast(MapDiscoveryStatisticsValue.class));
    noReduction.setNextAggregationPart(storableAggregator);
    storableAggregator.setNextAggregationPart(storableValuesAggregator);
    StatisticsAggregator<StorableStatisticsValue<OrderArchiveStatisticsStorable>, StorableStatisticsValue<OrderArchiveStatisticsStorable>> completeAggregator = 
      AggregationStatisticsFactory.completeAggregationStack(pathToIncompleteAggregationStack, noReduction);

    Collection<StorableStatisticsValue<OrderArchiveStatisticsStorable>> aggregatedValues;
    try {
      aggregatedValues = runtimeStatistics.getAggregatedValue(completeAggregator);
    } catch (XFMG_InvalidStatisticsPath e) {
      // should not happen
      throw new RuntimeException("Could not retrieve callStats", e);
    } catch (XFMG_UnknownStatistic e) {
      throw new RuntimeException("",e);
    }
    Map<String, OrderArchiveStatisticsStorable> result = new HashMap<String, OrderArchiveStatisticsStorable>();
    for (StorableStatisticsValue<OrderArchiveStatisticsStorable> storableStatisticsValue : aggregatedValues) {
      result.put(storableStatisticsValue.getValue().getOrderType(), storableStatisticsValue.getValue());
    }
    return result;
  }
  
  
  private static class OrderArchiveStatisticsStorableFromMapMapper implements StatisticsMapper<MapDiscoveryStatisticsValue, StorableStatisticsValue<OrderArchiveStatisticsStorable>> {

    @SuppressWarnings("rawtypes")
    public StorableStatisticsValue<OrderArchiveStatisticsStorable> map(MapDiscoveryStatisticsValue in, String nodename) {
      OrderArchiveStatisticsStorable storable = new OrderArchiveStatisticsStorable();
      for (Entry<StatisticsPath, StatisticsValue> entry : in.getValue().entrySet()) {
        if (entry.getKey().getPathPart(entry.getKey().length() - 1).getPartName().equals(XynaDispatcher.ORDERTYPE_STATISTICS_PATH_PART_NAME)) {
          storable.setOrderType((String) entry.getValue().getValue());
        } else if (entry.getKey().getPathPart(entry.getKey().length() - 1).equals(CallStatsType.FAILED)) {
          storable.setErrors((Long) entry.getValue().getValue());
        } else if (entry.getKey().getPathPart(entry.getKey().length() - 1).equals(CallStatsType.FINISHED)) {
          storable.setFinished((Long) entry.getValue().getValue());
        } else if (entry.getKey().getPathPart(entry.getKey().length() - 1).equals(CallStatsType.STARTED)) {
          storable.setCalls((Long) entry.getValue().getValue());
        } else if (entry.getKey().getPathPart(entry.getKey().length() - 1).equals(CallStatsType.TIMEOUT)) {
          storable.setTimeouts((Long) entry.getValue().getValue());
        } 
      }
      return new StorableStatisticsValue<OrderArchiveStatisticsStorable>(storable);
    }
    
  }


  enum StatisticsFilter {
    CALL, FINISHED, ERROR, TIMEOUT
  };


  public HashMap<String, Long> getCallStatistics() {
    return filterStatistics(StatisticsFilter.CALL, getStatistics());
  }


  public HashMap<String, Long> getFinishedStatistics() {
    return filterStatistics(StatisticsFilter.FINISHED, getStatistics());
  }


  public HashMap<String, Long> getErrorStatistics() {
    return filterStatistics(StatisticsFilter.ERROR, getStatistics());
  }


  public HashMap<String, Long> getTimeoutStatistics() {
    return filterStatistics(StatisticsFilter.TIMEOUT, getStatistics());
  }


  private HashMap<String, Long> filterStatistics(StatisticsFilter filter, Map<String, OrderArchiveStatisticsStorable> fullStatistics) {
    if (fullStatistics == null) {
      return new HashMap<String, Long>();
    }
    HashMap<String, Long> filteredMap = new HashMap<String, Long>();
    for (Entry<String, OrderArchiveStatisticsStorable> entry : fullStatistics.entrySet()) {
      switch (filter) {
        case CALL :
          filteredMap.put(entry.getKey(), entry.getValue().getCalls());
          break;
        case FINISHED :
          filteredMap.put(entry.getKey(), entry.getValue().getFinished());
          break;
        case ERROR :
          filteredMap.put(entry.getKey(), entry.getValue().getErrors());
          break;
        case TIMEOUT :
          filteredMap.put(entry.getKey(), entry.getValue().getTimeouts());
          break;
      }
    }
    return filteredMap;
  }


  public Map<Long, OrderInstance> getAllRunningProcesses(long offset, int count) throws PersistenceLayerException {
    OrderArchive currPrcDb = (OrderArchive) XynaFactory.getInstance().getProcessing()
                    .getSection(XynaProcessingODS.DEFAULT_NAME).getFunctionGroup(OrderArchive.DEFAULT_NAME);
    return currPrcDb.getAllInstances(offset, count);
  }


  public OrderInstanceDetails getRunningProcessDetails(Long id) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    OrderArchive currPrcDb = (OrderArchive) XynaFactory.getInstance().getProcessing()
                    .getSection(XynaProcessingODS.DEFAULT_NAME).getFunctionGroup(OrderArchive.DEFAULT_NAME);
    return currPrcDb.getCompleteOrder(id);
  }


  public Map<Long, CronLikeOrderInformation> getAllCronLikeOrders(long maxRows) throws XPRC_CronLikeSchedulerException {
    return XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler()
                    .getAllCronLikeOrderInformation(maxRows);
  }


  public CronLikeOrderInformation getCronLikeOrderInformation(long id) throws XPRC_CronLikeOrderStorageException {
    return XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler().getOrderInformation(id);
  }


  public String[] scanLogForLinesOfOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                  throws XFMG_ErrorScanningLogFile {

    if (!XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.get()) {
      return new String[0];
    }

    File logFile = retrieveLogFile();
    String grepCommand = generateGrepCommand(orderId, logFile, excludes);
    return grepLinesFromLog(grepCommand, logFile.getParentFile(), lineOffset, maxNumberOfLines);

  }


  public String retrieveLogForOrder(long orderId, int lineOffset, int maxNumberOfLines, String... excludes)
                  throws XFMG_ErrorScanningLogFile {
    if (! XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.get() ) {
      return "";
    }

    File logFile = retrieveLogFile();
    String grepCommand = generateGrepCommand(orderId, logFile, excludes);
    String[] lines = grepLinesFromLog(grepCommand, logFile.getParentFile(), lineOffset, maxNumberOfLines);
    StringBuilder logExcerpt = new StringBuilder();
    for (String line : lines) {
      logExcerpt.append(line).append("\n");
    }
    return logExcerpt.toString();
  }

  
  private String generateGrepCommand(long orderId, File logFile, String... excludes) {
    String propertyValue = XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.PATH_TO_SCANGREP);
    String pathToGrep;
    if (propertyValue == null) {
      pathToGrep = XynaProperty.PATH_TO_SCANGREP_DEFAULT_VALUE;
    } else {
      pathToGrep = propertyValue;
    }

    StringBuilder grepCommand = new StringBuilder(pathToGrep).append(" ");
    grepCommand.append(SCAN_PARAM_PREFIX);
    grepCommand.append(orderId);
    grepCommand.append(SCAN_PARAM_SUFFIX);
    grepCommand.append(" ");
    grepCommand.append(logFile.getName());
    grepCommand.append(" ");

    if (excludes != null && excludes.length > 0) {
      for (int i = 0; i < excludes.length; i++) {
        grepCommand.append("|").append(pathToGrep).append(" ").append(INVERSE_GREP_PARAM).append(" ")
                        .append(excludes[i]);
      }
    }
    
    return grepCommand.toString();
  }


  private File retrieveLogFile() throws XFMG_ErrorScanningLogFile {
    String propertyValue = XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.PATH_TO_LOG);
    String pathToLog;
    if (propertyValue == null) {
      pathToLog = XynaProperty.PATH_TO_LOG_DEFAULT_VALUE;
    } else {
      pathToLog = propertyValue;
    }

    File logFile = new File(pathToLog);
    if (!logFile.exists() || logFile.isDirectory()) {
      throw new XFMG_ErrorScanningLogFile("Invalid logFile specified: " + pathToLog);
    }
    
    return logFile;
  }


  private String[] grepLinesFromLog(String grepCommand, File workingDir, int offset, int maxNumberOfLines)
      throws XFMG_ErrorScanningLogFile {
    Process p = null;
    List<String> outputLines = new ArrayList<String>();
    List<String> errorLines = new ArrayList<String>();
    try {
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("building process with: " + grepCommand);
        }
        p = runtime.exec(new String[] {SHELL, SHELLPARAM, grepCommand.toString()}, null, workingDir);

        String line = null;
        int linesRead = 0;
        int linesUnread = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
          while ((line = bufferedReader.readLine()) != null && (linesRead < maxNumberOfLines || maxNumberOfLines == -1)) {
            //logger.debug("adding outputLine: " + line);
            if (linesUnread >= offset) {
              outputLines.add(line);
              linesRead++;
            } else {
              linesUnread++;
            }
          }
        }

        //logger.debug("read lines: " + linesRead + " lines not read: " + linesUnread);

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
          line = null;
          while ((line = bufferedReader.readLine()) != null) {
            errorLines.add(line);
          }
        }

      } catch (IOException e) {
        throw new XFMG_ErrorScanningLogFile("Error while executing grep command.", e);
      }

      try {
        p.waitFor();
      } catch (InterruptedException e) {
        logger.warn("Grep was interrupted while waiting", e);
        p.destroy();
      }
    } finally {
      if (p != null) {
        try {
          p.getInputStream().close();
        } catch (IOException e) {
        }
        try {
          p.getErrorStream().close();
        } catch (IOException e) {
        }
        try {
          p.getOutputStream().close();
        } catch (IOException e) {
        }
      }
    }

    if (errorLines.size() == 0) {
      return outputLines.toArray(new String[outputLines.size()]);
    } else {
      StringBuilder errorReason = new StringBuilder();
      for (String errorLine : errorLines) {
        if (!errorLine.contains("couldn't set locale correctly")) { //ignoring this error from solaris os with sh -c "<command>"
          errorReason.append(errorLine);
        }
      }
      if (errorReason.toString().equals("")) {
        return outputLines.toArray(new String[outputLines.size()]);
      } else {
        throw new XFMG_ErrorScanningLogFile(errorReason.toString());
      }

    }
  }
  
  
  public void enableServiceProfiling(ServiceIdentifier serviceId) throws XPRC_DESTINATION_NOT_FOUND, XFMG_InvalidStatisticsPath, XFMG_StatisticAlreadyRegistered {
    execTime.enableServiceProfiling(serviceId);
  }
  
  
  public void disableServiceProfiling(ServiceIdentifier serviceId) throws XPRC_DESTINATION_NOT_FOUND {
    execTime.disableServiceProfiling(serviceId);
  }
  
  
  @SuppressWarnings("rawtypes")
  public static class CompleteCallStatsValue implements StatisticsValue<HashMap<String, Map<String, StatisticsValue>>> {

    private static final long serialVersionUID = 5251397584093235986L;
    private HashMap<String, Map<String, StatisticsValue>> completeCallStats;
    
    private CompleteCallStatsValue(HashMap<String, Map<String, StatisticsValue>> completeCallStats) {
      this.completeCallStats = completeCallStats;
    }
    
    public HashMap<String, Map<String, StatisticsValue>> getValue() {
      return completeCallStats;
    }

    @SuppressWarnings("unchecked")
    public void merge(StatisticsValue<HashMap<String, Map<String, StatisticsValue>>> otherValue) {
      for (Entry<String, Map<String, StatisticsValue>> entry : otherValue.getValue().entrySet()) {
        Map<String, StatisticsValue> subMap = completeCallStats.get(entry.getKey());
        if (subMap == null) {
          completeCallStats.put(entry.getKey(), entry.getValue());
        } else {
          for (Entry<String, StatisticsValue> subEntry : entry.getValue().entrySet()) {
            StatisticsValue value = subMap.get(subEntry.getKey());
            if (value == null) {
              subMap.put(subEntry.getKey(), subEntry.getValue());
            } else {
              value.merge(subEntry.getValue());
            }
          }
        }
      }
    }

    public StatisticsValue<HashMap<String, Map<String, StatisticsValue>>> deepClone() {
      return this; // TODO clone
    }
    
  }
  
  
  private static class CompleteCallStatsMapper implements StatisticsMapper<MapDiscoveryStatisticsValue, CompleteCallStatsValue> {

    @SuppressWarnings("rawtypes")
    public CompleteCallStatsValue map(MapDiscoveryStatisticsValue in, String nodename) {
      HashMap<String, Map<String, StatisticsValue>> resultMap = new HashMap<String, Map<String, StatisticsValue>>();
      String ordertype = null;
      Map<String, StatisticsValue> innerMap = new HashMap<String, StatisticsValue>();
      for (Entry<StatisticsPath, StatisticsValue> discoveredEntry : in.getValue().entrySet()) {
        if (discoveredEntry.getKey().getPathPart(discoveredEntry.getKey().length() - 1).getPartName().equals(XynaDispatcher.ORDERTYPE_STATISTICS_PATH_PART_NAME)) {
          ordertype = (String) discoveredEntry.getValue().getValue();
        } else {
          for (CallStatsType type : CallStatsType.values()) {
            if (discoveredEntry.getKey().getPathPart(discoveredEntry.getKey().length() - 1).getPartName().equals(type.getPartName())) {
              innerMap.put(type.getPartName(), discoveredEntry.getValue());
              break;
            }
          }
        }
      }
      if (ordertype == null) {
        throw new RuntimeException("OrderStatistic values did not contain a necessary OrderType-Element.");
      }
      resultMap.put(ordertype, innerMap);
      return new CompleteCallStatsValue(resultMap);
    }
    
  }

}
