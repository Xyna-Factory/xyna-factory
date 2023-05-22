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



import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter.CronLikeOrderCreationParameterBuilder;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeOrderStorageException;
import com.gip.xyna.xprc.exceptions.XPRC_CronLikeSchedulerException;
import com.gip.xyna.xprc.exceptions.XPRC_CronRemovalException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidCronLikeOrderParametersException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.OnErrorAction;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrderInformation;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeScheduler;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.RestrictionBasedTimeWindow;

import xmcp.RuntimeContext;
import xmcp.factorymanager.CronLikeOrderServicesServiceOperation;
import xmcp.factorymanager.cronlikeorders.CronLikeOrder;
import xmcp.factorymanager.cronlikeorders.CronLikeOrderId;
import xmcp.factorymanager.cronlikeorders.exceptions.CreateCronLikeOrderException;
import xmcp.factorymanager.cronlikeorders.exceptions.DeleteCronLikeOrderException;
import xmcp.factorymanager.cronlikeorders.exceptions.LoadCronLikeOrderException;
import xmcp.factorymanager.cronlikeorders.exceptions.LoadCronLikeOrdersException;
import xmcp.factorymanager.cronlikeorders.exceptions.UpdateCronLikeOrderException;
import xmcp.factorymanager.impl.converter.TimeWindowConverter;
import xmcp.factorymanager.impl.converter.payload.GenericResult;
import xmcp.factorymanager.impl.converter.payload.GenericVisitor;
import xmcp.factorymanager.impl.converter.payload.Util;
import xmcp.factorymanager.impl.converter.payload.XynaObjectJsonBuilder;
import xmcp.factorymanager.impl.converter.payload.XynaObjectVisitor;
import xmcp.factorymanager.shared.OrderCustoms;
import xmcp.factorymanager.shared.OrderDestination;
import xmcp.factorymanager.shared.OrderExecutionTime;
import xmcp.factorymanager.shared.TimeWindow;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;
import xmcp.zeta.TableHelper.Filter;



public class CronLikeOrderServicesServiceOperationImpl implements ExtendedDeploymentTask, CronLikeOrderServicesServiceOperation {

  private static final String TABLE_PATH_ID = "iD";
  private static final String TABLE_PATH_NAME = "name";
  private static final String TABLE_PATH_APPLICATION = "application";
  private static final String TABLE_PATH_VERSION = "version";
  private static final String TABLE_PATH_WORKSPACE = "workspace";
  private static final String TABLE_PATH_ORDER_TYPE = "destination.orderType";
  private static final String TABLE_PATH_START_TIME = "executionTime.startTime";
  private static final String TABLE_PATH_TIMEZONE = "executionTime.timezone";
  private static final String TABLE_PATH_STATUS = "status";
  
  private static final String STATUS_ACTIVE = "Active";
  private static final String STATUS_DISABLED = "Disabled";

  private final RevisionManagement revisionManagement =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  private final CronLikeScheduler cronLikeScheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler().getCronLikeScheduler();


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
  public CronLikeOrderId createCronLikeOrder(CronLikeOrder cronLikeOrder) throws CreateCronLikeOrderException {
    try {
      DestinationKey destinationKey =
          new DestinationKey(cronLikeOrder.getDestination().getOrderType(),
                             revisionManagement.getRuntimeContext(cronLikeOrder.getDestination().getRuntimeContext().getRevision()));
      String calendarDefinition = null;
      if (cronLikeOrder.getExecutionTime().getTimeWindow() != null) {
        calendarDefinition = TimeWindowConverter.convertTimeWindow(cronLikeOrder.getExecutionTime().getTimeWindow(),
                                                                   cronLikeOrder.getExecutionTime().getTimezone(),
                                                                   cronLikeOrder.getExecutionTime().getConsiderDST());
      }
      CronLikeOrderCreationParameterBuilder<? extends CronLikeOrderCreationParameter> builder = 
        CronLikeOrderCreationParameter.newClocpForCreate(destinationKey)
                                      .calendarDefinition(calendarDefinition)
                                      .custom0(cronLikeOrder.getCronLikeOrderCustoms().getCustom0())
                                      .custom1(cronLikeOrder.getCronLikeOrderCustoms().getCustom1())
                                      .custom2(cronLikeOrder.getCronLikeOrderCustoms().getCustom2())
                                      .custom3(cronLikeOrder.getCronLikeOrderCustoms().getCustom3())
                                      .enabled(cronLikeOrder.getEnabled())
                                      .label(cronLikeOrder.getName())
                                      .onError(OnErrorAction.valueOf(cronLikeOrder.getOnerror()))
                                      .startTime(cronLikeOrder.getExecutionTime().getStartTime())
                                      .timeZoneId(cronLikeOrder.getExecutionTime().getTimezone())
                                      .useDST(cronLikeOrder.getExecutionTime().getConsiderDST() != null ? cronLikeOrder.getExecutionTime().getConsiderDST() : Boolean.FALSE);
      if (cronLikeOrder.getPayload() != null && cronLikeOrder.getPayload().trim().length() > 0) { 
        GeneralXynaObject payload = convertInputDataFromJsonToGeneralXynaObject(cronLikeOrder.getPayload(), cronLikeOrder.getDestination().getRuntimeContext().getRevision());
        builder.inputPayload(payload);
      }
      CronLikeOrderCreationParameter parameter = builder.build();
      com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder order = cronLikeScheduler.createCronLikeOrder(parameter, null);
      return new CronLikeOrderId(order.getId());
    } catch (XNWH_RetryTransactionException | XPRC_CronLikeSchedulerException e) {
      throw new CreateCronLikeOrderException(e.getMessage(), e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new CreateCronLikeOrderException("Runtimecontext not found: " + e.getMessage(), e);
    }
  }


  @Override
  public void deleteCronLikeOrder(CronLikeOrderId id) throws DeleteCronLikeOrderException {
    try {
      CronLikeOrderInformation cronLikeOrderInformation = cronLikeScheduler.getOrderInformation(id.getId());
      if (cronLikeOrderInformation == null)
        throw new DeleteCronLikeOrderException("Cron-like order not found");
      XynaFactory.getInstance().getXynaMultiChannelPortal().removeCronLikeOrder(id.getId());
    } catch (XPRC_CronLikeOrderStorageException | XPRC_CronRemovalException e) {
      throw new DeleteCronLikeOrderException(e.getMessage(), e);
    }
  }


  @Override
  public void updateCronLikeOrder(CronLikeOrder cronLikeOrder) throws UpdateCronLikeOrderException {
    try {
      CronLikeOrderInformation cronLikeOrderInformation = cronLikeScheduler.getOrderInformation(cronLikeOrder.getID());
      if (cronLikeOrderInformation == null)
        throw new UpdateCronLikeOrderException("Cron-like order not found");
      DestinationKey destinationKey =
          new DestinationKey(cronLikeOrder.getDestination().getOrderType(),
                             revisionManagement.getRuntimeContext(cronLikeOrder.getDestination().getRuntimeContext().getRevision()));
      String calendarDefinition = null;
      if (cronLikeOrder.getExecutionTime().getTimeWindow() != null) {
        calendarDefinition = TimeWindowConverter.convertTimeWindow(cronLikeOrder.getExecutionTime().getTimeWindow(),
                                                                   cronLikeOrder.getExecutionTime().getTimezone(),
                                                                   cronLikeOrder.getExecutionTime().getConsiderDST());
      }
      
      CronLikeOrderCreationParameter clocp = CronLikeOrderCreationParameter.newClocpForModify()
        .calendarDefinition(calendarDefinition)
        .custom0(cronLikeOrder.getCronLikeOrderCustoms().getCustom0())
        .custom1(cronLikeOrder.getCronLikeOrderCustoms().getCustom1())
        .custom2(cronLikeOrder.getCronLikeOrderCustoms().getCustom2())
        .custom3(cronLikeOrder.getCronLikeOrderCustoms().getCustom3())
        .destinationKey(destinationKey)
        .enabled(cronLikeOrder.getEnabled())
        .inputPayload(convertInputDataFromJsonToGeneralXynaObject(cronLikeOrder.getPayload(), cronLikeOrder.getDestination().getRuntimeContext().getRevision()))
        .label(cronLikeOrder.getName())
        .onError(OnErrorAction.valueOf(cronLikeOrder.getOnerror()))
        .startTime(cronLikeOrder.getExecutionTime().getStartTime())
        .timeZoneId(cronLikeOrder.getExecutionTime().getTimezone())
        .useDST(cronLikeOrder.getExecutionTime().getConsiderDST() != null ? cronLikeOrder.getExecutionTime().getConsiderDST() : Boolean.FALSE)
        .build();
      
      cronLikeScheduler.modifyTimeControlledOrder(cronLikeOrder.getID(), clocp);
      
    } catch (XPRC_CronLikeOrderStorageException | XPRC_InvalidCronLikeOrderParametersException | XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new UpdateCronLikeOrderException(e.getMessage(), e);
    }
  }


  @Override
  public CronLikeOrder getCronLikeOrder(CronLikeOrder cronLikeOrder) throws LoadCronLikeOrderException {
    try {
      CronLikeOrder result = convert(cronLikeScheduler.getOrderInformation(cronLikeOrder.getID()), true);
      if (result == null)
        throw new LoadCronLikeOrderException("Cron-like order not found");
      return result;
    } catch (XPRC_CronLikeOrderStorageException | XPRC_XmlParsingException | XPRC_InvalidXMLForObjectCreationException
        | XPRC_MDMObjectCreationException e) {
      throw new LoadCronLikeOrderException(e.getMessage(), e);
    }
  }


  public List<? extends CronLikeOrder> getListEntries(TableInfo tableInfo) throws LoadCronLikeOrdersException {
    Function<TableInfo, List<Filter>> filterFunction = ti -> ti.getColumns().stream()
        .filter(tableColumn -> !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0)
        .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter())).collect(Collectors.toList());

    final TableHelper<CronLikeOrder, TableInfo> tableHelper =
        TableHelper.<CronLikeOrder, TableInfo> init(tableInfo).limitConfig(TableInfo::getLimit).sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        }).filterConfig(filterFunction).addSelectFunction(TABLE_PATH_ID, CronLikeOrder::getID)
            .addSelectFunction(TABLE_PATH_NAME, CronLikeOrder::getName)
            .addSelectFunction(TABLE_PATH_APPLICATION, CronLikeOrder::getApplication)
            .addSelectFunction(TABLE_PATH_VERSION, CronLikeOrder::getVersion)
            .addSelectFunction(TABLE_PATH_WORKSPACE, CronLikeOrder::getWorkspace)
            .addSelectFunction(TABLE_PATH_ORDER_TYPE, x -> x.getDestination().getOrderType())
            .addSelectFunction(TABLE_PATH_START_TIME, x -> x.getExecutionTime().getStartTime())
            .addSelectFunction(TABLE_PATH_TIMEZONE, x -> x.getExecutionTime().getTimezone())
            .addSelectFunction(TABLE_PATH_STATUS, CronLikeOrder::getStatus);

    try {
      CronLikeOrderSelectImpl select =
          (CronLikeOrderSelectImpl) new CronLikeOrderSelectImpl().selectApplicationName().selectVersionName().selectWorkspaceName()
              .selectStatus().selectOrdertype().selectStarttime().selectTimeZoneID().selectLabelName().selectId().selectEnabled();

      List<TableHelper.Filter> filters = filterFunction.apply(tableInfo);
      filters.forEach(f -> addWhereClause(tableHelper, select, f));

      List<CronLikeOrderInformation> cronLikeOrders =
          cronLikeScheduler.searchCronLikeOrders(select, (tableInfo.getLimit() != null) ? tableInfo.getLimit() : -1).getResult();
      List<CronLikeOrder> result = cronLikeOrders.stream().map(cronLikeOrderInformation -> {
        try {
          return convert(cronLikeOrderInformation, false);
        } catch (XPRC_XmlParsingException | XPRC_InvalidXMLForObjectCreationException | XPRC_MDMObjectCreationException e) {
          throw new RuntimeException(e.getMessage(), e);
        }
      }).filter(tableHelper.filter()).collect(Collectors.toList());
      tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (PersistenceLayerException e) {
      throw new LoadCronLikeOrdersException(e.getMessage(), e);
    }
  }


  private void addWhereClause(final TableHelper<CronLikeOrder, TableInfo> tableHelper, CronLikeOrderSelectImpl select,
                              TableHelper.Filter filter) {
    switch (filter.getPath()) {
      case TABLE_PATH_NAME :
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.addWhereClause(select.newWC().whereLabel().isLike(f));
          } catch (XNWH_WhereClauseBuildException e) {
            // nothing
          }
        });
        break;
      case TABLE_PATH_APPLICATION :
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.newWC().whereApplicationname().isLike(f);
          } catch (XNWH_WhereClauseBuildException e) {
            // nothing
          }
        });
        break;
      case TABLE_PATH_VERSION :
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.newWC().whereVersionname().isLike(f);
          } catch (XNWH_WhereClauseBuildException e) {
            // nothing
          }
        });
        break;
      case TABLE_PATH_WORKSPACE :
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.newWC().whereWorkspacename().isLike(f);
          } catch (XNWH_WhereClauseBuildException e) {
            // nothing
          }
        });
        break;
      case TABLE_PATH_ORDER_TYPE :
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.newWC().whereOrdertype().isLike(f);
          } catch (XNWH_WhereClauseBuildException e) {
            // nothing
          }
        });
        break;
      case TABLE_PATH_START_TIME :
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.newWC().whereStartTime().isLike(f);
          } catch (XNWH_WhereClauseBuildException e) {
            // nothing
          }
        });
        break;
      case TABLE_PATH_STATUS :
        TableHelper.prepareQueryFilter(filter.getValue()).forEach(f -> {
          try {
            select.newWC().whereStatus().isLike(f);
          } catch (XNWH_WhereClauseBuildException e) {
            // nothing
          }
        });
        break;
      default :
        break;
    }
  }


  /**
   * Konvertiert eine CronLikeOrderInformation in eine CronLikeOrder.
   * @param in CronLikeOrderInformation aus dem Storage
   * @param addPayload Wenn true, so wird der Payload von XML in Json konvertiert.
   * @return CronLikeOrder zur Verwendung im Frontend
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY
   * @throws XPRC_XmlParsingException
   * @throws XPRC_InvalidXMLForObjectCreationException
   * @throws XPRC_MDMObjectCreationException
   */
  private CronLikeOrder convert(CronLikeOrderInformation in, boolean addPayload)
      throws XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    if (in == null)
      return null;

    CronLikeOrder r = new CronLikeOrder();
    r.setApplication(in.getApplicationName());
    if (in.getCronLikeOrderCustoms() != null) {
      OrderCustoms customs =
          new OrderCustoms(in.getCronLikeOrderCustoms().getCustom0(), in.getCronLikeOrderCustoms().getCustom1(),
                                   in.getCronLikeOrderCustoms().getCustom2(), in.getCronLikeOrderCustoms().getCustom3());
      r.setCronLikeOrderCustoms(customs);
    }
    TimeWindow timeWindow = null;
    if (in.getCalendarDefinition() != null) {
      timeWindow =
          TimeWindowConverter.convert(new RestrictionBasedTimeWindow.RestrictionBasedTimeWindowDefinition(in.getCalendarDefinition(), in
              .getTimeZoneID(), (in.getConsiderDaylightSaving() != null) ? in.getConsiderDaylightSaving() : Boolean.FALSE));
      if(timeWindow instanceof xmcp.factorymanager.shared.RestrictionBasedTimeWindow) {}
    }
    OrderExecutionTime executionTime =
        new OrderExecutionTime(in.getTimeZoneID(), in.getStartTime(), in.getConsiderDaylightSaving(), timeWindow, null);
    r.setExecutionTime(executionTime);

    RuntimeContext runtimeContext = null;
    if (in.getRuntimeContext() != null) {
      if (in.getRuntimeContext() instanceof Workspace)
        runtimeContext = convert((Workspace) in.getRuntimeContext());
      else if (in.getRuntimeContext() instanceof Application) {
        runtimeContext = convert((Application) in.getRuntimeContext());
      }
    }

    OrderDestination destination = new OrderDestination(in.getTargetOrdertype(), runtimeContext);
    r.setDestination(destination);

    r.setErrorMessage(in.getErrorMessage());
    r.setEnabled(in.isEnabled());
    r.setID(in.getId());
    r.setNextExecutionTime(in.getNextExecution());
    if (in.getOnError() != null)
      r.setOnerror(in.getOnError().name());
    
    if(in.isEnabled() != null && in.isEnabled())
      r.setStatus(STATUS_ACTIVE);
    else
      r.setStatus(STATUS_DISABLED);
    
    r.setVersion(in.getVersionName());
    r.setWorkspace(in.getWorkspaceName());
    r.setName(in.getLabel());

    if (addPayload && in.getPayload() != null && runtimeContext != null) {
      r.setPayload(convertInputDataFromXmlToJson(in.getPayload(), runtimeContext.getRevision()));
    }
    return r;
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


  private String convertInputDataFromXmlToJson(String xml, long revision)
      throws XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    if (xml == null)
      return null;
    GeneralXynaObject generalXynaObject = XynaObject.generalFromXml(xml, revision);
    XynaObjectJsonBuilder builder = new XynaObjectJsonBuilder(revision);
    return builder.buildJson(generalXynaObject);
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

}
