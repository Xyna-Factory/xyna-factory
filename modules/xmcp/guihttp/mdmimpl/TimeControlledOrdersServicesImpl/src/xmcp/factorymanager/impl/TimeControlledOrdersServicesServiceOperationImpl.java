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
package xmcp.factorymanager.impl;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.utils.timing.ExecutionPeriod.Type;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcess;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInput;
import com.gip.xyna.xprc.xbatchmgmt.beans.SlaveExecutionPeriod;
import com.gip.xyna.xprc.xbatchmgmt.input.InputGeneratorData;
import com.gip.xyna.xprc.xbatchmgmt.input.InputGeneratorData.InputGeneratorType;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessArchiveStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRestartInformationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable.BatchProcessState;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Start;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Start_Timeout;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Window;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.RestrictionBasedTimeWindow;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeWindow;

import xmcp.factorymanager.TimeControlledOrdersServicesServiceOperation;
import xmcp.factorymanager.impl.converter.TimeWindowConverter;
import xmcp.factorymanager.impl.converter.payload.GenericResult;
import xmcp.factorymanager.impl.converter.payload.GenericVisitor;
import xmcp.factorymanager.impl.converter.payload.Util;
import xmcp.factorymanager.impl.converter.payload.XynaObjectJsonBuilder;
import xmcp.factorymanager.impl.converter.payload.XynaObjectVisitor;
import xmcp.factorymanager.shared.OrderCustoms;
import xmcp.factorymanager.shared.OrderDestination;
import xmcp.factorymanager.shared.OrderExecutionTime;
import xmcp.factorymanager.timecontrolledorders.TCOExecutionRestriction;
import xmcp.factorymanager.timecontrolledorders.TCOId;
import xmcp.factorymanager.timecontrolledorders.TCOTableFilter;
import xmcp.factorymanager.timecontrolledorders.TimeControlledOrder;
import xmcp.factorymanager.timecontrolledorders.TimeControlledOrderTableEntry;
import xmcp.factorymanager.timecontrolledorders.exceptions.CreateTCOException;
import xmcp.factorymanager.timecontrolledorders.exceptions.KillTCOException;
import xmcp.factorymanager.timecontrolledorders.exceptions.LoadTCODetailsException;
import xmcp.factorymanager.timecontrolledorders.exceptions.LoadTCOsException;
import xmcp.factorymanager.timecontrolledorders.exceptions.UpdateTCOException;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;
import xmcp.zeta.TableHelper.Filter;


public class TimeControlledOrdersServicesServiceOperationImpl implements ExtendedDeploymentTask, TimeControlledOrdersServicesServiceOperation {
  
  private static final String TABLE_PATH_ID = "id.id";
  private static final String TABLE_PATH_NAME = "name";
  private static final String TABLE_PATH_APPLICATION = "application";
  private static final String TABLE_PATH_VERSION = "version";
  private static final String TABLE_PATH_WORKSPACE = "workspace";
  private static final String TABLE_PATH_ORDER_TYPE = "orderDestination.orderType";
  private static final String TABLE_PATH_START_TIME = "startTime";
  private static final String TABLE_PATH_INTERVAL = "interval";
  private static final String TABLE_PATH_STATUS = "status";
  
  private static final Logger logger = CentralFactoryLogging.getLogger(TimeControlledOrdersServicesServiceOperationImpl.class);
  
  private static final ZoneId FACTORY_ZONE_ID = ZoneId.of("UTC");
  
  private final BatchProcessManagement batchProcessManagement = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();

  private final RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    
  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }
  
  @Override
  public void updateTCO(TimeControlledOrder tco) throws UpdateTCOException {
    try {
      Long batchProcessId = tco.getId().getId();
      Boolean success = batchProcessManagement.modifyBatchProcess(batchProcessId, convert(tco));
      BatchProcess bp = batchProcessManagement.getBatchProcess(batchProcessId);

      if (success && bp.isPaused() == tco.getEnabled()) {
        if (tco.getEnabled()) {
          success = batchProcessManagement.continueBatchProcess(batchProcessId);
        } else {
          success = batchProcessManagement.pauseBatchProcess(batchProcessId);
        }
      }
      
      if(!success) {
        throw new UpdateTCOException("Update wasn't successfully.");
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw new UpdateTCOException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public TimeControlledOrder getTCODetails(TCOId tcoId) throws LoadTCODetailsException {
    try {
      BatchProcessInformation bpf = batchProcessManagement.getBatchProcessInformation(tcoId.getId());
      if(bpf != null) {
        return convert(bpf);
      } else {
        throw new LoadTCODetailsException("Time controlled order not found.");
      }
    } catch(XynaException ex) {
      logger.error(ex.getMessage(), ex);
      throw new LoadTCODetailsException(ex.getMessage(), ex);
    }
  }
  
  @Override
  public TCOId createTCO(TimeControlledOrder tco) throws CreateTCOException {
    try {
      Long id = batchProcessManagement.startBatchProcess(convert(tco));
      return new TCOId(id);
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      throw new CreateTCOException(ex.getMessage(), ex);
    }
  }
  
  private TimeControlledOrder convert(BatchProcessInformation input) {
    if(input == null) {
      return null;
    }
    TimeControlledOrder tco = new TimeControlledOrder();
    tco.setId(new TCOId(input.getBatchProcessId()));
    tco.setName(input.getLabel());
    tco.setArchived(input.getMasterOrderCreationParameter() == null);
    
    TCOExecutionRestriction executionRestriction = new TCOExecutionRestriction();
    
    OrderExecutionTime oet = new OrderExecutionTime();
    DestinationKey destinationKey = null;
    
    if(input.getArchive() != null) {
      BatchProcessArchiveStorable archiv = input.getArchive();
      tco.setOrderCustoms(new OrderCustoms(archiv.getCustom0(), archiv.getCustom1(), archiv.getCustom2(), archiv.getCustom3()));
      executionRestriction.setMaximumExcecutions(archiv.getTotal());
      destinationKey = archiv.getDestinationKey();
    }
    
    executionRestriction.setExecutionTimeout(null); // TODO Ist das wirklich immer null?
    executionRestriction.setTreatTimeoutsAsError(true); // TODO Ist das wirklich immer true?
    
    if(destinationKey != null) {
      tco.setOrderDestination(new OrderDestination(destinationKey.getOrderType(), convert(destinationKey.getRuntimeContext())));
    }
    
    String timeZone = null;
    Boolean considerDST = null;
    
    if(input.getRestartInformation() != null) {
      BatchProcessRestartInformationStorable restartInfo = input.getRestartInformation();
      tco.setFilterCriteria(restartInfo.getInputQuery());
      tco.setSortCriteria(restartInfo.getInputSortCriteria());
      tco.setStorableFqn(restartInfo.getInputStorable());
      
      if(restartInfo.getGuiRepresentationData() != null) {
        String[] split = restartInfo.getGuiRepresentationData().split("@");
        if(split.length == 2) {
          timeZone = split[0];
          considerDST = Boolean.parseBoolean(split[1]);
        }
        oet.setConsiderDST(considerDST);
        oet.setTimezone(timeZone);
      }
      
      if(restartInfo.getConstantInput() != null && !restartInfo.getConstantInput().isEmpty() && destinationKey != null) {
        try {
          tco.setInputPayload(convertInputDataFromXmlToJson(restartInfo.getConstantInput(), revisionManagement.getRevision(destinationKey.getRuntimeContext())));
        } catch (XPRC_XmlParsingException | XPRC_InvalidXMLForObjectCreationException | XPRC_MDMObjectCreationException
            | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.error(e.getMessage(), e);
        }
      }
      if(restartInfo.getTimeWindowDefinition() != null) {
        oet.setTimeWindow(TimeWindowConverter.convert(restartInfo.getTimeWindowDefinition()));
      }
      if(restartInfo.getSlaveTimeConstraint() instanceof TimeConstraint_Start_Timeout) {
        TimeConstraint_Start_Timeout tct = (TimeConstraint_Start_Timeout)restartInfo.getSlaveTimeConstraint();
        Long serverEndtime = tct.getSchedulingTimeout().getAbsoluteTime();
        
        oet.setEndTime(convertTimestampToTimeZone(serverEndtime, FACTORY_ZONE_ID, ZoneId.of(timeZone)));
      }
      if(restartInfo.getSlaveExecutionPeriod() != null) {
        executionRestriction.setExecutionInterval(restartInfo.getSlaveExecutionPeriod().getInterval());
      }
      
      if(restartInfo.getMasterSchedulingData() != null) {
        SchedulingData schedulingData = restartInfo.getMasterSchedulingData();
        if(schedulingData.getTimeConstraint() != null) {
          TimeConstraintData tcd = new TimeConstraintData(schedulingData.getTimeConstraint());
          oet.setStartTime(convertTimestampToTimeZone(tcd.startTime, FACTORY_ZONE_ID, ZoneId.of(timeZone)));
          oet.setEndTime(convertTimestampToTimeZone(tcd.endTime, FACTORY_ZONE_ID, ZoneId.of(timeZone)));
        }
      }
    }
    
    if(input.getRuntimeInformation() != null) {
      if(input.getRuntimeInformation().getState() == BatchProcessState.PAUSED) {
        tco.setEnabled(false);
      } else {
        tco.setEnabled(true);
      }
    }
    
    if(input.getMasterOrderCreationParameter() != null) {
      RemoteXynaOrderCreationParameter ocp = input.getMasterOrderCreationParameter();
      tco.setOrderCustoms(new OrderCustoms(ocp.getCustom0(), ocp.getCustom1(), ocp.getCustom2(), ocp.getCustom3()));
      destinationKey = ocp.getDestinationKey();
      executionRestriction.setSchedulingTimeout(ocp.getAbsoluteSchedulingTimeout());

      TimeConstraintData tcd = new TimeConstraintData(ocp.getTimeConstraint());
      oet.setStartTime(convertTimestampToTimeZone(tcd.startTime, FACTORY_ZONE_ID, ZoneId.of(timeZone)));
      oet.setEndTime(convertTimestampToTimeZone(tcd.endTime, FACTORY_ZONE_ID, ZoneId.of(timeZone)));
    }
    
    tco.setPlanningHorizon(oet);
    tco.setTCOExecutionRestriction(executionRestriction);
    
    return tco;
  }
  
  private static class TimeConstraintData {
    
    private Long startTime;
    private Long endTime;
    
    
    public TimeConstraintData(TimeConstraint tc) {
      if(tc instanceof TimeConstraint_Start) {
        TimeConstraint_Start tcs = (TimeConstraint_Start)tc;
        startTime = tcs.getStart().getAbsoluteTime();
      }
      if(tc instanceof TimeConstraint_Start_Timeout) {
        TimeConstraint_Start_Timeout tcst = (TimeConstraint_Start_Timeout)tc;
        endTime = tcst.getSchedulingTimeout().getAbsoluteTime();
      }
      if (tc instanceof TimeConstraint_Window) {
        TimeConstraint_Window tcw = (TimeConstraint_Window)tc;
        TimeConstraintData d = new TimeConstraintData(tcw.getBeforeTimeConstraint());
        startTime = d.startTime;
        endTime = d.endTime;
      }
    }
    
  }
  
  private Long convertTimestampToTimeZone(Long timestamp, ZoneId sourceTimeZone, ZoneId destinationTimeZone) {
    
    if(timestamp == null || sourceTimeZone == null || destinationTimeZone == null) {
      return timestamp;
    }
    
    int srcOffsetSeconds = sourceTimeZone.getRules().getOffset(Instant.now()).getTotalSeconds();
    int dstOffsetSeconds = destinationTimeZone.getRules().getOffset(Instant.now()).getTotalSeconds();
    
    long utcTimestamp = timestamp - srcOffsetSeconds * 1000;
    return utcTimestamp + dstOffsetSeconds * 1000;
  }

  
  private BatchProcessInput convert(TimeControlledOrder tco) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    BatchProcessInput batchProcessInput = new BatchProcessInput();
    batchProcessInput.setLabel(tco.getName());
    batchProcessInput.setInputGeneratorData(generateInputGeneratorData(tco));
    batchProcessInput.setMasterOrder(generateMasterOrderCreationParameter(tco));
    batchProcessInput.setMaxParallelism(1);
    batchProcessInput.setSlaveOrderType(tco.getOrderDestination().getOrderType());
    batchProcessInput.setPaused(!tco.getEnabled());
    
    OrderExecutionTime orderExecutionTime = tco.getPlanningHorizon(); 
    if(orderExecutionTime != null) {
      if(orderExecutionTime.getTimeWindow() != null) {
        batchProcessInput.setTimeWindowDefinition(TimeWindowConverter.convertTimeWindow(
                                                        orderExecutionTime.getTimeWindow(), 
                                                        orderExecutionTime.getTimezone(),
                                                        orderExecutionTime.getConsiderDST())
        );
      }
      batchProcessInput.setGuiRepresentationData(orderExecutionTime.getTimezone() + "@" + Boolean.toString(orderExecutionTime.getConsiderDST()));
    }
    
    TCOExecutionRestriction executionRestriction = tco.getTCOExecutionRestriction();
    TimeConstraint_Start slaveTimeConstraint = TimeConstraint.immediately();
    if(executionRestriction != null) {
      if (executionRestriction.getSchedulingTimeout() != null) {
        slaveTimeConstraint = slaveTimeConstraint.withSchedulingTimeout(executionRestriction.getSchedulingTimeout(), TimeUnit.MILLISECONDS);
        batchProcessInput.setSlaveWorkflowExecTimeout(new AbsRelTime(executionRestriction.getSchedulingTimeout(), true));
      }
      if(executionRestriction.getExecutionInterval() != null) {
        batchProcessInput.setSlaveExecutionPeriod(new SlaveExecutionPeriod(Type.FixedInterval, executionRestriction.getExecutionInterval()));
      }
    }
    batchProcessInput.setSlaveTimeConstraint(slaveTimeConstraint);
    
    return batchProcessInput;
  }
  
  private  RemoteXynaOrderCreationParameter generateMasterOrderCreationParameter(TimeControlledOrder tco) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    DestinationKey dk = new DestinationKey(XynaProperty.BATCH_DEFAULT_MASTER.get());
    dk.setRuntimeContext(revisionManagement.getRuntimeContext(tco.getOrderDestination().getRuntimeContext().getRevision()));
    RemoteXynaOrderCreationParameter rxocp = new RemoteXynaOrderCreationParameter(dk);
    rxocp.setCustom0(tco.getOrderCustoms().getCustom0());
    rxocp.setCustom1(tco.getOrderCustoms().getCustom1());
    rxocp.setCustom2(tco.getOrderCustoms().getCustom2());
    rxocp.setCustom3(tco.getOrderCustoms().getCustom3());
    
        
    if(tco.getPlanningHorizon() != null) {
      OrderExecutionTime oet = tco.getPlanningHorizon();
      
      TimeConstraint_Start tc = TimeConstraint.immediately();
      if(oet.getStartTime() != null) {
        tc = TimeConstraint.at(convertTimestampToTimeZone(oet.getStartTime(), ZoneId.of(oet.getTimezone()), FACTORY_ZONE_ID));
      }
      if(oet.getEndTime() != null) {
        tc = tc.withAbsoluteSchedulingTimeout(convertTimestampToTimeZone(oet.getEndTime(), ZoneId.of(oet.getTimezone()), FACTORY_ZONE_ID));
      }
      rxocp.setTimeConstraint(tc);
    } else {
      rxocp.setTimeConstraint(TimeConstraint.immediately());
    }
    
    return rxocp;
  }
  
  private InputGeneratorData generateInputGeneratorData(TimeControlledOrder tco) {
    InputGeneratorData inputGeneratorData;
    if (tco.getInputPayload() == null && tco.getStorableFqn() == null) {
      inputGeneratorData = new InputGeneratorData(InputGeneratorType.Constant);
    } else if (tco.getStorableFqn() != null) {
      inputGeneratorData = new InputGeneratorData(InputGeneratorType.OnTheFly);
    } else {
      inputGeneratorData = new InputGeneratorData(InputGeneratorType.Constant);
    }

    if (tco.getTCOExecutionRestriction() != null) {
      TCOExecutionRestriction executionRestriction = tco.getTCOExecutionRestriction();
      inputGeneratorData.setMaximumInputs((int)executionRestriction.getMaximumExcecutions());
    }

    if (tco.getInputPayload() == null && tco.getStorableFqn() == null) {
      return inputGeneratorData;
    }

    if (inputGeneratorData.getInputGeneratorType() == InputGeneratorType.OnTheFly) {
      inputGeneratorData.setQuery(tco.getFilterCriteria());
      inputGeneratorData.setSortCriteria(tco.getSortCriteria());
      inputGeneratorData.setStorable(tco.getStorableFqn());
    }

    if (tco.getInputPayload() != null && !tco.getInputPayload().isEmpty()) {
      GeneralXynaObject xo = convertInputDataFromJsonToGeneralXynaObject(tco.getInputPayload(), tco.getOrderDestination().getRuntimeContext().getRevision());
      if (!(xo instanceof Container)) {
        xo = new Container(xo);
      }
      inputGeneratorData.setConstantInput(xo.toXml());
    }

    return inputGeneratorData;
  }
  
  private GeneralXynaObject convertInputDataFromJsonToGeneralXynaObject(String json, long revision) {
    if (json == null)
      return null;
    try {
      JsonParser jp = new JsonParser();
      GenericResult genericResult = jp.parse(json, new GenericVisitor());
      Util.distributeMetaInfo(genericResult, revision);
      XynaObjectVisitor xov = new XynaObjectVisitor();
      genericResult.visit(xov, Collections.singletonList(XynaObjectVisitor.META_TAG));
      return xov.getAndReset();
    } catch (InvalidJSONException | UnexpectedJSONContentException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  private String convertInputDataFromXmlToJson(String xml, long revision)
      throws XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    if (xml == null)
      return null;
    GeneralXynaObject generalXynaObject = XynaObject.generalFromXml(xml, revision);
    XynaObjectJsonBuilder builder = new XynaObjectJsonBuilder(revision);
    return builder.buildJson(generalXynaObject);
  }
  
  @Override
  public void killTCO(TCOId tcoId) throws KillTCOException {
    try {
      boolean success = batchProcessManagement.cancelBatchProcess(tcoId.getId(), CancelMode.WAIT, -1L);
      if(!success) {
        throw new KillTCOException("Deletion was unsuccessful");
      }
    } catch (PersistenceLayerException e) {
      logger.error(e.getMessage(), e);
      throw new KillTCOException(e.getMessage(), e);
    }
  }
  
  public List<? extends TimeControlledOrderTableEntry> getTCOs(TableInfo tableInfo, TCOTableFilter filter) throws LoadTCOsException {
    Function<TableInfo, List<Filter>> filterFunction = ti -> ti.getColumns().stream()
        .filter(tableColumn -> !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0)
        .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter())).collect(Collectors.toList());

    final TableHelper<TimeControlledOrderTableEntry, TableInfo> tableHelper =
        TableHelper.<TimeControlledOrderTableEntry, TableInfo> init(tableInfo).limitConfig(TableInfo::getLimit).sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        }).filterConfig(filterFunction).addSelectFunction(TABLE_PATH_ID, x -> x.getId().getId())
            .addSelectFunction(TABLE_PATH_NAME, TimeControlledOrderTableEntry::getName)
            .addSelectFunction(TABLE_PATH_APPLICATION, TimeControlledOrderTableEntry::getApplication)
            .addSelectFunction(TABLE_PATH_VERSION, TimeControlledOrderTableEntry::getVersion)
            .addSelectFunction(TABLE_PATH_WORKSPACE, TimeControlledOrderTableEntry::getWorkspace)
            .addSelectFunction(TABLE_PATH_ORDER_TYPE, x -> x.getOrderDestination().getOrderType())
            .addSelectFunction(TABLE_PATH_START_TIME, TimeControlledOrderTableEntry::getStartTime)
            .addSelectFunction(TABLE_PATH_INTERVAL, TimeControlledOrderTableEntry::getInterval)
            .addSelectFunction(TABLE_PATH_STATUS, TimeControlledOrderTableEntry::getStatus);
    try {
      BatchProcessSelectImpl select = new BatchProcessSelectImpl();
      
      List<TableHelper.Filter> filters = filterFunction.apply(tableInfo);
      filters.forEach(f -> addWhereClause(tableHelper, select, f));
      
      List<BatchProcessInformation> batchProcessInformations = batchProcessManagement.searchBatchProcesses(select, (tableInfo.getLimit() != null) ? tableInfo.getLimit() : -1).getResult();
      List<TimeControlledOrderTableEntry> result = batchProcessInformations.stream()
          .filter(bi -> filter.getShowArchived() != null && (bi.getMasterOrderCreationParameter() != null || filter.getShowArchived()))
          .map(this::convertToTableEntry)
          .filter(tableHelper.filter())
          .collect(Collectors.toList());
      tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (PersistenceLayerException ex) {
      throw new LoadTCOsException(ex.getMessage(), ex);
    }
  }
  
  private TimeControlledOrderTableEntry convertToTableEntry(BatchProcessInformation i) {
    if(i == null) {
      return null;
    }
    TimeControlledOrderTableEntry o = new TimeControlledOrderTableEntry();
    o.setApplication(i.getApplication());
    o.setId(new TCOId(i.getBatchProcessId()));
    o.setName(i.getLabel());
    o.setStatus(i.getBatchProcessStatus() != null ? i.getBatchProcessStatus().name() : "");
    o.setVersion(i.getVersion());
    o.setWorkspace(i.getWorkspace());
    o.setArchived(i.getMasterOrderCreationParameter() == null);
    
    
    /*
     * MasterOrderCreationParameter sind nur gesetzt, wenn der BatchProzess noch aktiv ist
     * RestartInformation sind immer gesetzt
     */
    
    RemoteXynaOrderCreationParameter masterOrderCreationParameter = i.getMasterOrderCreationParameter();
    BatchProcessRestartInformationStorable restartInfo = i.getRestartInformation();
    BatchProcessArchiveStorable archive = i.getArchive();

    OrderDestination orderType = new OrderDestination();
    
    if(masterOrderCreationParameter != null) {
      if(masterOrderCreationParameter.getTimeConstraint() != null) {
        if(masterOrderCreationParameter.getTimeConstraint() instanceof TimeConstraint_Start_Timeout) {
          TimeConstraint_Start_Timeout timeConstraintStartTimeout = (TimeConstraint_Start_Timeout)masterOrderCreationParameter.getTimeConstraint();
          o.setStartTime(generateAbsRelTime(timeConstraintStartTimeout.getStart()));
        } else if(masterOrderCreationParameter.getTimeConstraint() instanceof TimeConstraint_Start) {
          TimeConstraint_Start timeConstraintStart = (TimeConstraint_Start)masterOrderCreationParameter.getTimeConstraint();
          o.setStartTime(generateAbsRelTime(timeConstraintStart.getStart()));
        } else if (masterOrderCreationParameter.getTimeConstraint() instanceof TimeConstraint_Window) {
          TimeConstraint_Window timeConstraintWindow = (TimeConstraint_Window)masterOrderCreationParameter.getTimeConstraint();
          o.setStartTime(generateAbsRelTime(timeConstraintWindow.getBeforeTimeConstraint().getStart()));
        }
      }
      
    } else {
      if(restartInfo.getMasterSchedulingData() != null) {
        if(restartInfo.getMasterSchedulingData().getTimeConstraint() instanceof TimeConstraint_Start) {
          TimeConstraint_Start timeConstraintStart = (TimeConstraint_Start)restartInfo.getMasterSchedulingData().getTimeConstraint();
          o.setStartTime(generateAbsRelTime(timeConstraintStart.getStart()));
        } else if (restartInfo.getMasterSchedulingData().getTimeConstraint() instanceof TimeConstraint_Window) {
          TimeConstraint_Window timeConstraintWindow = (TimeConstraint_Window)i.getRestartInformation().getMasterSchedulingData().getTimeConstraint();
          o.setStartTime(generateAbsRelTime(timeConstraintWindow.getBeforeTimeConstraint().getStart()));
        }
      }
    }
    if(archive != null) {
      orderType.setOrderType(archive.getDestinationKey() != null ? archive.getDestinationKey().getOrderType() : "");
    }
    orderType.setRuntimeContext(convert(i.getRuntimeContext()));
    o.setOrderDestination(orderType);
    
    StringBuilder interval = new StringBuilder();
    
    if(restartInfo  != null) {
      if(i.getRestartInformation().getTimeWindowDefinition() != null) {
        TimeWindow timeWindow = i.getRestartInformation().getTimeWindowDefinition().constructTimeWindow();
        if(timeWindow instanceof RestrictionBasedTimeWindow) {
          RestrictionBasedTimeWindow restrictionBasedTimeWindow = (RestrictionBasedTimeWindow)timeWindow;
          xmcp.factorymanager.shared.RestrictionBasedTimeWindow guiRBT =  (xmcp.factorymanager.shared.RestrictionBasedTimeWindow) TimeWindowConverter.convert(restrictionBasedTimeWindow.getDefinition());
          String readableRecurringInterval = "";
          try {
            readableRecurringInterval = TimeWindowConverter.generateReadableRecurringInterval(guiRBT);
          } catch (Exception ex) {
            readableRecurringInterval = restrictionBasedTimeWindow.toString();
          }
          interval.append("Time window: ").append(readableRecurringInterval);
        }
      }
      if(restartInfo.getSlaveExecutionPeriod() != null) {
        interval.append(interval.length() > 0 ? "; " : "").append("Execution restriction: ");
        if(restartInfo.getSlaveExecutionPeriod().getType() == Type.FixedInterval) {
          interval.append(convertMillis(restartInfo.getSlaveExecutionPeriod().getInterval()));
        }
      }
    }
    o.setInterval(interval.toString());
    return o;
  }  

  private String convertMillis(final long millis) {
    if (millis < 0) {
      return "";
    }

    long days = TimeUnit.MILLISECONDS.toDays(millis);
    long remainder = millis - TimeUnit.DAYS.toMillis(days);
    long hours = TimeUnit.MILLISECONDS.toHours(remainder);
    remainder -= TimeUnit.HOURS.toMillis(hours);
    long minutes = TimeUnit.MILLISECONDS.toMinutes(remainder);
    remainder -= TimeUnit.MINUTES.toMillis(minutes);
    long seconds = TimeUnit.MILLISECONDS.toSeconds(remainder);
    remainder -= TimeUnit.SECONDS.toMillis(seconds);

    StringBuilder sb = new StringBuilder();
    if(days > 0) {
      sb.append(days).append(" d");
    }
    if(hours > 0) {
      sb.append(sb.length() > 0 ? " " : "").append(hours).append(" h");
    }
    if(minutes > 0) {
      sb.append(sb.length() > 0 ? " " : "").append(minutes).append(" min");
    }
    if(seconds > 0) {
      sb.append(sb.length() > 0 ? " " : "").append(seconds).append(" sec");
    }
    if(remainder > 0) {
      sb.append(sb.length() > 0 ? " " : "").append(remainder).append(" ms");
    }

    return (sb.toString());
  }
  
  private static String generateAbsRelTime(AbsRelTime time) {
    if (time == null) {
      return null;
    }
    String timeString;
    if (time.isRelative()) {
      GregorianCalendar cal = new GregorianCalendar();
      cal.setTimeInMillis(time.getTime());
      StringBuilder sb = new StringBuilder("P");
      String year = "0000";
      if (cal.get(Calendar.YEAR) > 1970) {
        year = String.valueOf(cal.get(Calendar.YEAR) - 1970);
      }
      while (year.length() < 4) {
        year = "0" + year;
      }
      sb.append(year).append("-");
      String month = "00";
      if (cal.get(Calendar.MONTH) > 1) {
        month = String.valueOf(cal.get(Calendar.MONTH) - 1);
      }
      if (month.length() == 1) {
        month = "0" + month;
      }
      sb.append(month).append("-");
      String day = "00";
      if (cal.get(Calendar.DAY_OF_MONTH) > 1) {
        day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH) - 1);
      }
      if (day.length() == 1) {
        day = "0" + day;
      }
      sb.append(day).append("T");
      String hour = "00";
      if (cal.get(Calendar.HOUR_OF_DAY) > 1) {
        day = String.valueOf(cal.get(Calendar.HOUR_OF_DAY) - 1);
      }
      if (hour.length() == 1) {
        hour = "0" + hour;
      }
      sb.append(hour).append(":");
      String min = String.valueOf(cal.get(Calendar.MINUTE));
      if (min.length() == 1) {
        min = "0" + min;
      }
      sb.append(min).append(":");
      String sec = String.valueOf(cal.get(Calendar.SECOND));
      if (sec.length() == 1) {
        sec = "0" + sec;
      }
      sb.append(sec).append(",");
      String millis = String.valueOf(cal.get(Calendar.MILLISECOND));
      while (millis.length() < 3) {
        millis = 0 + millis;
      }
      timeString = sb.append(millis).toString();
    } else {
      DateFormat format = getDefaultDateFormat();
      timeString = format.format(new Date(time.getTime()));
    }
    return timeString;
  }
  
  private static SimpleDateFormat getDefaultDateFormat() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); 
    sdf.setTimeZone(TimeZone.getTimeZone(Constants.DEFAULT_TIMEZONE));
    sdf.setLenient(false);
    return sdf;
  }
  
  private xmcp.RuntimeContext convert(com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext rtc){
    if (rtc instanceof Workspace)
      return convert((Workspace) rtc);
    else if (rtc instanceof Application) {
      return convert((Application) rtc);
    }
    return null;
  }
  
  private xmcp.Application convert(com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application app) {
    xmcp.Application application = new xmcp.Application();
    application.setName(app.getName());
    application.setType(app.getRuntimeDependencyContextType().name());
    application.setVersionName(app.getVersionName());
    try {
      application.setRevision(revisionManagement.getRevision(app));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      application.setRevision(null);
    }
    return application;
  }


  private xmcp.Workspace convert(Workspace workspace) {
    xmcp.Workspace w = new xmcp.Workspace();
    w.setName(workspace.getName());
    try {
      w.setRevision(revisionManagement.getRevision(workspace));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      w.setRevision(null);
    }
    w.setType(workspace.getRuntimeDependencyContextType().name());
    return w;
  }
  
  private void addWhereClause(final TableHelper<TimeControlledOrderTableEntry, TableInfo> tableHelper, BatchProcessSelectImpl select, TableHelper.Filter filter) {
    switch (filter.getPath()) {
      case TABLE_PATH_APPLICATION:
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.addWhereClause(select.whereApplication().isLike(f));
          } catch (XNWH_WhereClauseBuildException ex) {
            logger.error(ex.getMessage(), ex);
          }
        });
        break;
      case TABLE_PATH_ID:
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.addWhereClause(select.whereBatchProcessId().isLike(f));
          } catch (XNWH_WhereClauseBuildException ex) {
            logger.error(ex.getMessage(), ex);
          }
        });
        break;
      case TABLE_PATH_NAME:
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.addWhereClause(select.whereLabel().isLike(f));
          } catch (XNWH_WhereClauseBuildException ex) {
            logger.error(ex.getMessage(), ex);
          }
        });
        break;
      case TABLE_PATH_ORDER_TYPE:
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.addWhereClause(select.whereSlaveOrderType().isLike(f));
          } catch (XNWH_WhereClauseBuildException ex) {
            logger.error(ex.getMessage(), ex);
          }
        });
        break;
      case TABLE_PATH_STATUS:
        break;
      case TABLE_PATH_VERSION:
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.addWhereClause(select.whereVersion().isLike(f));
          } catch (XNWH_WhereClauseBuildException ex) {
            logger.error(ex.getMessage(), ex);
          }
        });
        break;
      case TABLE_PATH_WORKSPACE:
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.addWhereClause(select.whereWorkspace().isLike(f));
          } catch (XNWH_WhereClauseBuildException ex) {
            logger.error(ex.getMessage(), ex);
          }
        });
        break;
    }
  }

}
