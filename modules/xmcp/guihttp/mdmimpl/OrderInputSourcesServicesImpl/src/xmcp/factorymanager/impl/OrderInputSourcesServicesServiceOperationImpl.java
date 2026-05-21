/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.util.xo.GenericResult;
import com.gip.xyna.xact.filter.util.xo.GenericVisitor;
import com.gip.xyna.xact.filter.util.xo.Util;
import com.gip.xyna.xact.filter.util.xo.XynaObjectJsonBuilder;
import com.gip.xyna.xact.filter.util.xo.XynaObjectVisitor;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.exceptions.XFMG_InputSourceNotUniqueException;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState.DeploymentLocation;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.selectorderinputsource.OrderInputSourceColumn;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskStatisticsParameter;
import com.gip.xyna.xprc.xfqctrl.LoadControlledCreationParameterBean;
import com.gip.xyna.xprc.xfqctrl.RateControlledCreationParameterBean;
import com.gip.xyna.xprc.xfqctrl.ordercreation.FrequencyControlledOrderCreationTask.FrequencyControlledOrderInputSourceUsingTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.ordercreation.LoadControlledOrderCreationTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.ordercreation.RateControlledOrderCreationTaskCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xmcp.RuntimeContext;
import xmcp.factorymanager.OrderInputSourcesServicesServiceOperation;
import xmcp.factorymanager.impl.converter.OrderInputSourceConverter;
import xmcp.factorymanager.orderinputsources.CreateOrderInputSourceRequest;
import xmcp.factorymanager.orderinputsources.FrequencyControlledTaskId;
import xmcp.factorymanager.orderinputsources.GenerateOrderInputRequest;
import xmcp.factorymanager.orderinputsources.GenerateOrderInputResponse;
import xmcp.factorymanager.orderinputsources.GetOrderInputSourceRequest;
import xmcp.factorymanager.orderinputsources.OrderInputCustomContainer;
import xmcp.factorymanager.orderinputsources.OrderInputSource;
import xmcp.factorymanager.orderinputsources.OrderInputSourceId;
import xmcp.factorymanager.orderinputsources.Parameter;
import xmcp.factorymanager.orderinputsources.SourceType;
import xmcp.factorymanager.orderinputsources.StartFrequencyControlledTaskParameter;
import xmcp.factorymanager.orderinputsources.exceptions.DeleteOrderInputSourceException;
import xmcp.factorymanager.orderinputsources.exceptions.GenerateOrderInputException;
import xmcp.factorymanager.orderinputsources.exceptions.LoadGeneratingOrderTypesException;
import xmcp.factorymanager.orderinputsources.exceptions.LoadOrderInputSourceException;
import xmcp.factorymanager.orderinputsources.exceptions.LoadOrderInputSourcesException;
import xmcp.factorymanager.orderinputsources.exceptions.OrderInputSourceCreateException;
import xmcp.factorymanager.orderinputsources.exceptions.OrderInputSourceNotUniqueException;
import xmcp.factorymanager.orderinputsources.exceptions.OrderInputSourceUpdateException;
import xmcp.factorymanager.orderinputsources.exceptions.StartFrequencyControlledTaskException;
import xmcp.factorymanager.shared.OrderType;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;



public class OrderInputSourcesServicesServiceOperationImpl implements ExtendedDeploymentTask, OrderInputSourcesServicesServiceOperation {

  private static final String TABLE_KEY_NAME = "name";
  private static final String TABLE_KEY_ORDER_TYPE = "orderType";
  private static final String TABLE_KEY_APPLICATION = "applicationName";
  private static final String TABLE_KEY_VERSION = "versionName";
  private static final String TABLE_KEY_WORKSPACE = "workspaceName";
  private static final String TABLE_KEY_SOURCE_TYPE = "sourceType.label";
  private static final String TABLE_KEY_STATE = "state";
  private static final String TABLE_KEY_REFERENCED_INPUT_SOURCE_COUNT = "referencedInputSourceCount";

  private static final String PARAMETER_KEY_INPUT_DATA = "inputData";

  private static final String FREQUENCY_CONTROLLED_TASK_TYPE_RATE = "Rate";
  private static final String FREQUENCY_CONTROLLED_TASK_TYPE_LOAD = "Load";


  private final OrderInputSourceManagement orderInputSourceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
  private final RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  private final OrdertypeManagement ordertypeManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();
  private final DeploymentItemStateManagement deploymentItemStateManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();

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
  public GenerateOrderInputResponse generateOrderInput(GenerateOrderInputRequest request) throws GenerateOrderInputException {
    OptionalOISGenerateMetaInformation parameters = new OptionalOISGenerateMetaInformation();
    if (request.getParameters() != null) {
      for (Parameter p : request.getParameters()) {
        parameters.setValue(p.getKey(), p.getValue());
      }
    }
    try {
      XynaOrderCreationParameter xynaOrderCreationParameter = orderInputSourceManagement.generateOrderInput(request.getOrderInputSourceId().getId(), parameters);

      RemoteXynaOrderCreationParameter rxocp = null;
      if (xynaOrderCreationParameter instanceof RemoteXynaOrderCreationParameter) {
        rxocp = (RemoteXynaOrderCreationParameter) xynaOrderCreationParameter;
        if (rxocp.getInputPayloadAsXML() == null) {
          rxocp.setInputPayload(rxocp.getInputPayload());
        }
        rxocp.removeXynaObjectInputPayload();
      } else {
        rxocp = new RemoteXynaOrderCreationParameter(xynaOrderCreationParameter);
      }

      GenerateOrderInputResponse response = new GenerateOrderInputResponse();
      OrderInputCustomContainer customContainer = new OrderInputCustomContainer();
      customContainer.setCustom0(rxocp.getCustom0());
      customContainer.setCustom1(rxocp.getCustom1());
      customContainer.setCustom2(rxocp.getCustom2());
      customContainer.setCustom3(rxocp.getCustom3());
      response.setCustomContainer(customContainer);
      response.setOrderInputSourceInstanceId(xynaOrderCreationParameter.getOrderInputSourceId());
      response.setPayload(convertInputDataFromXmlToJson(rxocp.getInputPayloadAsXML(), request.getRuntimeContext().getRevision()));
      response.setPriority(xynaOrderCreationParameter.getPriority());
      response.setTimeout(xynaOrderCreationParameter.getAbsoluteSchedulingTimeout());
      return response;
    } catch (XynaException e) {
      throw new GenerateOrderInputException(e.getMessage(), e);
    }
  }

  @Override
  public FrequencyControlledTaskId startFrequencyControlledTask(StartFrequencyControlledTaskParameter request) throws StartFrequencyControlledTaskException {
    try {
      FrequencyControlledTaskCreationParameter r = null;
      List<XynaOrderCreationParameter> orderCreationParameter = new ArrayList<>(1);

      GeneralXynaObject inputData = null;
      for (Parameter param : request.getOrderInputSource().getParameter()) {
        if(PARAMETER_KEY_INPUT_DATA.equals(param.getKey())) {
          inputData = convertInputDataFromJsonToGeneralXynaObject(param.getValue(), request.getOrderInputSource().getRevision());
          break;
        }
      }

      if (inputData != null) {
        DestinationKey destinationKey = new DestinationKey(request.getOrderInputSource().getOrderType().getType(), revisionManagement.getRuntimeContext(request.getOrderInputSource().getRevision()));
        orderCreationParameter.add(new XynaOrderCreationParameter(destinationKey, inputData));
      } else {
        orderCreationParameter.add(orderInputSourceManagement.generateOrderInput(request.getOrderInputSource().getId()));
      }

      if(FREQUENCY_CONTROLLED_TASK_TYPE_LOAD.equalsIgnoreCase(request.getType())) {
        if(request.getCreateInputOnce() != null && request.getCreateInputOnce()) {
          r = new LoadControlledOrderCreationTaskCreationParameter(
                     request.getName(), request.getNumberOfOrders(),
                     (long)request.getValue(), 1, orderCreationParameter);
        } else {
          r = new FrequencyControlledOrderInputSourceUsingTaskCreationParameter(
                     request.getName(), request.getNumberOfOrders(),
                     new long[] {request.getOrderInputSource().getId()},
                     new LoadControlledCreationParameterBean((long)request.getValue(), 1));
        }
      } else if (FREQUENCY_CONTROLLED_TASK_TYPE_RATE.equalsIgnoreCase(request.getType())) {
        if(request.getCreateInputOnce() != null && request.getCreateInputOnce()) {
          r = new RateControlledOrderCreationTaskCreationParameter(
                     request.getName(), request.getNumberOfOrders(),
                     request.getValue(), orderCreationParameter);
        } else {
          r = new FrequencyControlledOrderInputSourceUsingTaskCreationParameter(
                     request.getName(), request.getNumberOfOrders(),
                     new long[] {request.getOrderInputSource().getId()},
                     new RateControlledCreationParameterBean(request.getValue()));
        }
      }
      if(r != null) {
        FrequencyControlledTaskStatisticsParameter parameters =
            new FrequencyControlledTaskStatisticsParameter(request.getDataPointCount(), request.getDataPointDistance());
        r.setFrequencyControlledTaskStatisticsParameters(parameters);
        if(request.getDelay() != null)
          r.setDelay(request.getDelay());
        else
          r.setDelay("null");
        r.setTimezone(request.getTimezone());

        long taskId = XynaFactory.getInstance().getXynaMultiChannelPortal().startFrequencyControlledTask(r);
        return new FrequencyControlledTaskId(taskId);
      }
      throw new StartFrequencyControlledTaskException("unknown type");
    } catch (XynaException e) {
      throw new StartFrequencyControlledTaskException(e.getMessage(), e);
    }
  }

  @Override
  public void deleteOrderInputSouce(OrderInputSourceId id) throws DeleteOrderInputSourceException {
    try {
      orderInputSourceManagement.deleteOrderInputSource(id.getId());
    } catch (XynaException e) {
      throw new DeleteOrderInputSourceException(e.getMessage(), e);
    }
  }

  @Override
  public void changeOrderInputSource(OrderInputSource orderInputSource) throws OrderInputSourceUpdateException {
    try {
      OrderInputSourceStorable oiss = orderInputSourceManagement.getInputSourceByName(orderInputSource.getRevision(), orderInputSource.getName(), false);
      if(oiss == null)
        throw new OrderInputSourceUpdateException("OrderInputSource not found");

      Map<String, String> parameters = new HashMap<>(orderInputSource.getParameter().size());
      orderInputSource.getParameter().forEach(param -> {
        if(PARAMETER_KEY_INPUT_DATA.equals(param.getKey())) {
          if(orderInputSource.getSourceType().getLabel().toLowerCase().contains("constant") && param.getValue() != null){
            // Spezielle Behandlung des Prameter inputData. Das Frontend liefert Json, das Backend erwartet XML
            parameters.put(param.getKey(), convertInputDataFromJsonToGeneralXynaObject(param.getValue(), orderInputSource.getRevision()).toXml());
          } else {
            parameters.put(param.getKey(), null);
          }
        } else {
          parameters.put(param.getKey(), param.getValue());
        }
      });

      OrderInputSourceStorable newOiss = new OrderInputSourceStorable(oiss.getName(), orderInputSource.getSourceType().getName(), orderInputSource.getOrderType().getType(),
                                                                   oiss.getApplicationName(), oiss.getVersionName(), oiss.getWorkspaceName(), orderInputSource.getDocumentation(), parameters);
      orderInputSourceManagement.modifyOrderInputSource(newOiss);
    } catch (XynaException e) {
      throw new OrderInputSourceUpdateException(e.getMessage(), e);
    }
  }

  @Override
  public OrderInputSource getOrderInputSource(GetOrderInputSourceRequest request) throws LoadOrderInputSourceException {
    try {
      OrderInputSourceStorable oiss = orderInputSourceManagement.getInputSourceByName(request.getRevision(), request.getInputSourceName(), true);
      if(oiss == null)
        throw new LoadOrderInputSourceException("OrderInputSource not found");
      OrderInputSource orderInputSource = OrderInputSourceConverter.convert(oiss);

      // Spezielle Behandlung des Prameter inputData. Das Backend liefert XML, das Frontend erwartet Json
      orderInputSource.getParameter().stream()
        .filter(param -> PARAMETER_KEY_INPUT_DATA.equals(param.getKey()))
        .forEach(param -> {
          try {
            if(param.getValue() != null && orderInputSource.getSourceType() != null
                && orderInputSource.getSourceType().getLabel() != null
                && orderInputSource.getSourceType().getLabel().toLowerCase().contains("constant"))
              param.setValue(convertInputDataFromXmlToJson(param.getValue(), request.getRevision()));
            else
              param.setValue(null);
          } catch (XPRC_XmlParsingException | XPRC_InvalidXMLForObjectCreationException | XPRC_MDMObjectCreationException e) {
            param.setValue(null);
          }
        });
      return orderInputSource;
    } catch (PersistenceLayerException e) {
      throw new LoadOrderInputSourceException(e.getMessage(), e);
    }
  }

  private String convertInputDataFromXmlToJson(String xml, long revision) throws XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    if (xml == null || xml.isEmpty()) {
      return null;
    }

    GeneralXynaObject generalXynaObject = XynaObject.generalFromXml(xml, revision);
    XynaObjectJsonBuilder builder = new XynaObjectJsonBuilder(revision);
    return builder.buildJson(generalXynaObject);
  }

  @Override
  public void createOrderInputSource(CreateOrderInputSourceRequest request) throws OrderInputSourceCreateException, OrderInputSourceNotUniqueException {
    try {
      String applicationName = null;
      String versionName = null;
      String workspaceName = null;
      if(request.getRuntimeContext() instanceof xmcp.Application) {
        Application application = revisionManagement.getApplication(request.getRuntimeContext().getRevision());
        applicationName = application.getName();
        versionName = application.getVersionName();
      } else if (request.getRuntimeContext() instanceof xmcp.Workspace) {
        Workspace workspace = revisionManagement.getWorkspace(request.getRuntimeContext().getRevision());
        workspaceName = workspace.getName();
      }
      Map<String, String> parameters = new HashMap<>(request.getParameter().size());
      request.getParameter().stream().forEach(param -> {
        if(PARAMETER_KEY_INPUT_DATA.equals(param.getKey())) {
          if(request.getSourceType().getName().contains("Constant")) {
            // Spezielle Behandlung des Prameter inputData. Das Frontend liefert Json, das Backend erwartet XML
            parameters.put(param.getKey(), convertInputDataFromJsonToGeneralXynaObject(param.getValue(), request.getRuntimeContext().getRevision()).toXml());
          } else {
            parameters.put(param.getKey(), null);
          }
        } else {
          parameters.put(param.getKey(), param.getValue());
        }
      });
      OrderInputSourceStorable oiss =
          new OrderInputSourceStorable(request.getName(),
                                       request.getSourceType().getName(),
                                       request.getOrderType(), applicationName, versionName, workspaceName, request.getDocumentation(),
                                       parameters);
      orderInputSourceManagement.createOrderInputSource(oiss);
    } catch (XFMG_InputSourceNotUniqueException e) {
      throw new OrderInputSourceNotUniqueException(e);
    } catch (XynaException e) {
      throw new OrderInputSourceCreateException(e.getMessage(), e);
    }
  }

  private GeneralXynaObject convertInputDataFromJsonToGeneralXynaObject(String json, long revision) {
    if(json == null)
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

  @Override
  public List<SourceType> getOrderSourceTypes() {
    List<PluginDescription> pluginDescriptions = orderInputSourceManagement.listOrderInputSourceTypes();
    return pluginDescriptions.stream()
        .map(plugin -> OrderInputSourceConverter.createSourceTypeFromName(plugin.getName()))
        .collect(Collectors.toList());
  }

  @Override
  public List<OrderType> getGeneratingOrderTypes(RuntimeContext guiRuntimeContext, OrderType executionOrderType) throws LoadGeneratingOrderTypesException {
    try {
      com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(guiRuntimeContext.getRevision());
      List<OrdertypeParameter> ordertypeParameters = ordertypeManagement.listOrdertypes(SearchOrdertypeParameter.hierarchy(runtimeContext));

      OrdertypeParameter executionOrdertypeParameter = null;
      for (OrdertypeParameter otp : ordertypeParameters) {
        if(otp.getOrdertypeName() != null && otp.getOrdertypeName().equals(executionOrderType.getName())) {
          executionOrdertypeParameter = otp;
          break;
        }
      };
      if(executionOrdertypeParameter == null)
        throw new Exception("Execution Ordertype not found");

      if(executionOrdertypeParameter.getExecutionDestinationValue().getDestinationTypeEnum() == ExecutionType.XYNA_FRACTAL_WORKFLOW) {
        DeploymentItemState deploymentItemStateExecutionWorkflow = deploymentItemStateManagement.get(executionOrdertypeParameter.getExecutionDestinationValue().getFullQualifiedName(), guiRuntimeContext.getRevision());


        return ordertypeParameters.stream()
            .filter(otp -> {
              if(otp.getExecutionDestinationValue() != null && otp.getExecutionDestinationValue().getDestinationTypeEnum() == ExecutionType.XYNA_FRACTAL_WORKFLOW) {
                try {
                  DeploymentItemState deploymentItemStateGeneratingWorkflow = deploymentItemStateManagement.get(otp.getExecutionDestinationValue().getFullQualifiedName(), revisionManagement.getRevision(otp.getRuntimeContext()));
                  return matchOutputParameterToInputParameter(deploymentItemStateGeneratingWorkflow, deploymentItemStateExecutionWorkflow);
                } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
                  // nothing
                }
              }
              return false;
            })
            .map(otp -> {
              OrderType ot = new OrderType();
              ot.setName(otp.getOrdertypeName());
              return ot;
            })
            .sorted((o1, o2) -> {
                if(o1 == null && o2 == null)
                  return 0;
                if(o1 == null)
                  return 1;
                if(o2 == null)
                  return -1;
                return o1.getName().compareToIgnoreCase(o2.getName());
            })
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (Exception e) {
      throw new LoadGeneratingOrderTypesException(e.getMessage(), e);
    }
  }

  /**
   * checks if the output parameters of the generating workflow matches the input parameters of the workflow to be executed
   * @param generatingWorkflow
   * @param executionWorkflow
   * @return
   */
  private boolean matchOutputParameterToInputParameter(DeploymentItemState generatingWorkflow, DeploymentItemState executionWorkflow) {
    if(generatingWorkflow == null || executionWorkflow == null)
      return false;
    Set<OperationInterface> publishedInterfacesExecutionWorkflow = executionWorkflow.getPublishedInterfaces(OperationInterface.class, DeploymentLocation.DEPLOYED);
    if (publishedInterfacesExecutionWorkflow.size() == 1) {
      Set<OperationInterface> publishedInterfacesGeneratingWorkflow = generatingWorkflow.getPublishedInterfaces(OperationInterface.class, DeploymentLocation.DEPLOYED);
      if(publishedInterfacesGeneratingWorkflow.size() == 1) {
        // vergleichen, dass output auf input passt
        OperationInterface wfWithOutputInterface = publishedInterfacesGeneratingWorkflow.iterator().next();
        OperationInterface wfWithInputInterface = publishedInterfacesExecutionWorkflow.iterator().next();

        return mayCall(wfWithOutputInterface, wfWithInputInterface);
      }
    }
    return false;
  }

  /**
   * checks if the OperationInterface to be called matches the calling OperationInterface
   * @param caller
   * @param interfaceToCall
   * @return
   */
  private boolean mayCall(OperationInterface caller, OperationInterface interfaceToCall) {
    //the caller can have the additional parameter xprc.xpce.OrderCreationParameter that has to be ignored in the comparison

    List<TypeInterface> input = new ArrayList<>();
    for (TypeInterface ti : caller.getOutput()) {
      if (!ti.getName().equals("xprc.xpce.OrderCreationParameter")) {
        input.add(ti);
      }
    }
    OperationInterface tmp = OperationInterface.of(null, input, null);
    try {
      return interfaceToCall.matches(tmp);
    } catch (Throwable t) {
      return false;
    }
  }

  @Override
  public List<? extends OrderInputSource> getListEntries(TableInfo tableInfo) throws LoadOrderInputSourcesException {

    final TableHelper<OrderInputSource, TableInfo> tableHelper = TableHelper.<OrderInputSource, TableInfo>init(tableInfo)
        .limitConfig(TableInfo::getLimit)
        .sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        })
        .filterConfig(ti ->
          ti.getColumns().stream()
          .filter(tableColumn ->
            !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
          )
          .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))
          .collect(Collectors.toList())
        )
        .addSelectFunction(TABLE_KEY_NAME, OrderInputSource::getName)
        .addSelectFunction(TABLE_KEY_ORDER_TYPE, OrderInputSource::getOrderType)
        .addSelectFunction(TABLE_KEY_REFERENCED_INPUT_SOURCE_COUNT, x -> Integer.valueOf(x.getReferencedInputSourceCount()))
        .addSelectFunction(TABLE_KEY_STATE, OrderInputSource::getState)
        .addSelectFunction(TABLE_KEY_APPLICATION, OrderInputSource::getApplicationName)
        .addSelectFunction(TABLE_KEY_VERSION, OrderInputSource::getVersionName)
        .addSelectFunction(TABLE_KEY_SOURCE_TYPE, x -> x.getSourceType().getName())
        .addSelectFunction(TABLE_KEY_WORKSPACE, OrderInputSource::getWorkspaceName)
        .addTableToDbMapping(TABLE_KEY_NAME, OrderInputSourceStorable.COL_NAME)
        .addTableToDbMapping(TABLE_KEY_ORDER_TYPE, OrderInputSourceStorable.COL_ORDERTYPE)
        .addTableToDbMapping(TABLE_KEY_APPLICATION, OrderInputSourceStorable.COL_APPLICATIONNAME)
        .addTableToDbMapping(TABLE_KEY_VERSION, OrderInputSourceStorable.COL_VERSIONNAME)
        .addTableToDbMapping(TABLE_KEY_SOURCE_TYPE, OrderInputSourceStorable.COL_TYPE)
        .addTableToDbMapping(TABLE_KEY_WORKSPACE, OrderInputSourceStorable.COL_WORKSPACENAME)
        .addTableToDbMapping(TABLE_KEY_STATE, OrderInputSourceColumn.STATE.getColumnName())  // OrderInputSourceStorable.COL_STATE exisitiert nicht
        .addTableToDbMapping(TABLE_KEY_REFERENCED_INPUT_SOURCE_COUNT, OrderInputSourceColumn.REFERENCE_COUNT.getColumnName());  // OrderInputSourceStorable.COL_REFERENCED_INPUT_SOURCE_COUNT existiert nicht


    List<OrderInputSource> result = new ArrayList<>();
    try {
      SearchRequestBean srb = tableHelper.createSearchRequest(ArchiveIdentifier.orderInputSource);
      String selection = srb.getSelection();
      selection += ", " + OrderInputSourceStorable.COL_ID;
      srb.setSelection(selection);

      SearchResult<?> inputSources = orderInputSourceManagement.searchInputSources(srb);
      if(inputSources == null || inputSources.getCount() == 0)
        return result;
      @SuppressWarnings("unchecked")
      List<OrderInputSourceStorable> allOrderInputSources = (List<OrderInputSourceStorable>) inputSources.getResult();

      // convert and filter
      result = allOrderInputSources.stream()
        .map(OrderInputSourceConverter::convert)
        .filter(tableHelper.filter())
        .collect(Collectors.toList());

      tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (PersistenceLayerException | XNWH_SelectParserException | XNWH_InvalidSelectStatementException e) {
      throw new LoadOrderInputSourcesException(e.getMessage(), e);
    }
  }
}
