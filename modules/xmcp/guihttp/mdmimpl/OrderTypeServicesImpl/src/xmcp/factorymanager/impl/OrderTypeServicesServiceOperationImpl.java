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
package xmcp.factorymanager.impl;


import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToAddObjectToApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCapacityCardinality;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidCreationOfExistingOrdertype;
import com.gip.xyna.xfmg.exceptions.XFMG_InvalidModificationOfUnexistingOrdertype;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter.DestinationValueParameter;
import com.gip.xyna.xfmg.xods.ordertypemanagement.SearchOrdertypeParameter;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_MONITORING_TYPE;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.planning.Capacity;

import xmcp.RuntimeContext;
import xmcp.factorymanager.DestinationType;
import xmcp.factorymanager.OrderTypeServicesServiceOperation;
import xmcp.factorymanager.ParameterInheritanceRule;
import xmcp.factorymanager.impl.converter.OrderTypeConverter;
import xmcp.factorymanager.ordertypes.OrderType;
import xmcp.factorymanager.ordertypes.OrderTypeName;
import xmcp.factorymanager.ordertypes.OrderTypeTableFilter;
import xmcp.factorymanager.ordertypes.exception.CreateNewOderTypeException;
import xmcp.factorymanager.ordertypes.exception.DeleteOrderTypeException;
import xmcp.factorymanager.ordertypes.exception.LoadOrderTpeException;
import xmcp.factorymanager.ordertypes.exception.LoadOrderTypesException;
import xmcp.factorymanager.ordertypes.exception.UpdateOrderTypeException;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;


public class OrderTypeServicesServiceOperationImpl implements ExtendedDeploymentTask, OrderTypeServicesServiceOperation {
  
  private static final String TABLE_KEY_NAME = "name";
  private static final String TABLE_KEY_EXECUTION_DESTINATION_TYPE_NAME = "executionDestination.name";
  private static final String TABLE_KEY_USED_CAPACITIES = "usedCapacities";
  private static final String TABLE_KEY_DOCUMENTATION = "documentation";
  private static final String TABLE_KEY_PRIORITY = "priority";
  private static final String TABLE_KEY_MONITORING_LEVEL = "monitoringLevel";
  private static final String TABLE_KEY_PLANNING_DESTINATION = "planningDestination";
  private static final String TABLE_KEY_APPLICATION = "application";
  private static final String TABLE_KEY_VERSION = "version";
  private static final String TABLE_KEY_WORKSPACE = "workspace";
  
  private final XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();  
  private final OrdertypeManagement ordertypeManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderTypeManagement();
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
  public List<? extends DestinationType> getDestinations(RuntimeContext runtimeContext) {
    Set<Long> allRevisions = new HashSet<>();
    allRevisions.add(runtimeContext.getRevision());
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(runtimeContext.getRevision(), allRevisions);
    List<DestinationType> result = new ArrayList<>();
    HashMap<Long, List<String>> workflows = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase().getDeployedWfs();
    for (Long revision : allRevisions) {
      if(workflows.containsKey(revision)) {
        List<String> workflowNames = workflows.get(revision);
        for (String name : workflowNames) {
          result.add(new DestinationType(name, XynaOrderServerExtension.ExecutionType.XYNA_FRACTAL_WORKFLOW.getTypeAsString()));
        }
      }
    }
    return result;
  }
  
  @Override
  public void changeOrderType(OrderType orderType) throws UpdateOrderTypeException {    
    try {
      ordertypeManagement.modifyOrdertype(createOrderTypeParameter(orderType));
    } catch (PersistenceLayerException | XFMG_InvalidModificationOfUnexistingOrdertype | XFMG_InvalidCapacityCardinality | XPRC_INVALID_MONITORING_TYPE e) {
      throw new UpdateOrderTypeException(e.getMessage(), e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new UpdateOrderTypeException("RuntimeContext not found: " + e.getMessage(), e);
    }
  }

  @Override
  public void createOrderType(OrderType orderType) throws CreateNewOderTypeException {
    try {
      ordertypeManagement.createOrdertype(createOrderTypeParameter(orderType));
    } catch (PersistenceLayerException | XFMG_InvalidCreationOfExistingOrdertype | XFMG_FailedToAddObjectToApplication | XPRC_INVALID_MONITORING_TYPE e) {
      throw new CreateNewOderTypeException(e.getMessage(), e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new CreateNewOderTypeException("RuntimeContext not found: " + e.getMessage(), e);
    }
  }
  
  /**
   * Convert OrderType to OrdertypeParameter
   * @param orderType
   * @return
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY
   * @throws XPRC_INVALID_MONITORING_TYPE
   */
  private OrdertypeParameter createOrderTypeParameter(OrderType orderType) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XPRC_INVALID_MONITORING_TYPE {
    
    Integer priority = null;
    if (orderType.getPriority() != null &&
        orderType.getPriority() >= 0) {
      priority = orderType.getPriority();
    }
    
    Set<Capacity> requiredCapacities = new HashSet<>();
    if(orderType.getRequiredCapacities() != null)
      orderType.getRequiredCapacities().forEach(cap -> requiredCapacities.add(new Capacity(cap.getName(), cap.getCardinality())));
    
    List<InheritanceRule> inheritanceRules = new ArrayList<>();
    
    if(orderType.getPrecedence() != null) {
    //Precedence angegeben, daher eine Regel erzeugen
      inheritanceRules.add(ParameterType.MonitoringLevel.createInheritanceRuleBuilder(orderType.getMonitoringLevel())
                               .precedence(orderType.getPrecedence())
                               .build());
    } else {
      //statisches Monitoringlevel
      if(orderType.getMonitoringLevel() != null && !orderType.getMonitoringLevel().matches("^-\\d+$")) {
        inheritanceRules.add(ParameterType.MonitoringLevel.createInheritanceRuleBuilder(orderType.getMonitoringLevel())
                                 .build());
      }
    }
        
    if(orderType.getParameterInheritanceRules() != null) {
      for (ParameterInheritanceRule rule: orderType.getParameterInheritanceRules()) {
        InheritanceRule inheritanceRule = InheritanceRule.createMonitoringLevelRule(rule.getValue())
            .childFilter(rule.getFilter())
            .precedence(rule.getPrecedence())
            .build();
        inheritanceRules.add(inheritanceRule);
      }
    }
    Map<ParameterType, List<InheritanceRule>> parameterInheritanceRules = new EnumMap<>(ParameterType.class);
    parameterInheritanceRules.put(ParameterType.MonitoringLevel, inheritanceRules);
    
    OrdertypeParameter ordertypeParameter = new OrdertypeParameter();
    ordertypeParameter.setOrdertypeName(orderType.getName());
    
    if(orderType.getPlanningDestination() != null && orderType.getPlanningDestination().getName() != null && orderType.getPlanningDestinationIsCustom())
      ordertypeParameter.setCustomPlanningDestinationValue(createDestinationValueParameter(orderType.getPlanningDestination()));
    else
      ordertypeParameter.setCustomPlanningDestinationValue(new DestinationValueParameter("DefaultPlanning", XynaOrderServerExtension.ExecutionType.XYNA_FRACTAL_WORKFLOW.getTypeAsString()));
    
    ordertypeParameter.setCustomExecutionDestinationValue(createDestinationValueParameter(orderType.getExecutionDestination()));
    
    if(orderType.getCleanupDestination() != null && orderType.getCleanupDestination().getName() != null)
      ordertypeParameter.setCustomCleanupDestinationValue(createDestinationValueParameter(orderType.getCleanupDestination()));
    else
      ordertypeParameter.setCustomCleanupDestinationValue(new DestinationValueParameter(
          XynaDispatcher.DESTINATION_EMPTY_WORKFLOW.getFQName(), XynaOrderServerExtension.ExecutionType.XYNA_FRACTAL_WORKFLOW.getTypeAsString()));

    ordertypeParameter.setRequiredCapacities(requiredCapacities);
    ordertypeParameter.setDocumentation(orderType.getDocumentation());
    if(orderType.getPriorityIsCustom())
      ordertypeParameter.setCustomPriority(priority);
    else
      ordertypeParameter.setCustomPriority(null);
    ordertypeParameter.setOrdertypeName(orderType.getName());
    ordertypeParameter.setRuntimeContext(revisionManagement.getRuntimeContext(orderType.getRuntimeContext().getRevision()));
    ordertypeParameter.setParameterInheritanceRules(parameterInheritanceRules);
    
    return ordertypeParameter;
  }
  
  private DestinationValueParameter createDestinationValueParameter(DestinationType destination) {
    if(destination == null)
      return null;
    return new DestinationValueParameter(destination.getName(), destination.getType());
  }
  
  @Override
  public void deleteOrderType(RuntimeContext runtimeContext, OrderTypeName orderTypeName) throws DeleteOrderTypeException {
    try {
      com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeParameter ordertypeParameter = ordertypeManagement.getOrdertype(orderTypeName.getName(), revisionManagement.getRuntimeContext(runtimeContext.getRevision()));
      ordertypeManagement.deleteOrdertype(ordertypeParameter);
    } catch (PersistenceLayerException e) {
      throw new DeleteOrderTypeException(e.getMessage(), e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new DeleteOrderTypeException("Ordertype " +  orderTypeName.getName() + " not found.", e);
    }
  }
  
  @Override
  public OrderType getOrderTypeDetails(RuntimeContext runtimeContext, OrderTypeName orderTypeName) throws LoadOrderTpeException {
    try {
      return OrderTypeConverter.convert(ordertypeManagement.getOrdertype(orderTypeName.getName(), revisionManagement.getRuntimeContext(runtimeContext.getRevision())), true);
    } catch (PersistenceLayerException e) {
      throw new LoadOrderTpeException(e.getMessage(), e);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new LoadOrderTpeException("Ordertype " +  orderTypeName.getName() + " not found.", e);
    }
  }

  @Override
  public List<? extends OrderType> getListEntries(TableInfo tableInfo, OrderTypeTableFilter filter) throws LoadOrderTypesException {   
    TableHelper<OrderType, TableInfo> tableHelper = TableHelper.<OrderType, TableInfo>init(tableInfo)
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
        .addSelectFunction(TABLE_KEY_DOCUMENTATION, OrderType::getDocumentation)
        .addSelectFunction(TABLE_KEY_EXECUTION_DESTINATION_TYPE_NAME, x -> (x.getExecutionDestination() != null) ? x.getExecutionDestination().getName() : "")
        .addSelectFunction(TABLE_KEY_MONITORING_LEVEL, OrderType::getMonitoringLevel)
        .addSelectFunction(TABLE_KEY_NAME, OrderType::getName)
        .addSelectFunction(TABLE_KEY_PLANNING_DESTINATION, x -> x.getPlanningDestination() != null ? x.getPlanningDestination().getName() : "")
        .addSelectFunction(TABLE_KEY_PRIORITY, OrderType::getPriority)
        .addSelectFunction(TABLE_KEY_USED_CAPACITIES, OrderType::getUsedCapacities)
        .addSelectFunction(TABLE_KEY_APPLICATION, OrderType::getApplication)
        .addSelectFunction(TABLE_KEY_VERSION, OrderType::getVersion)
        .addSelectFunction(TABLE_KEY_WORKSPACE, OrderType::getWorkspace);
    
    try {
      List<OrdertypeParameter> orderTypes = multiChannelPortal.listOrdertypes(SearchOrdertypeParameter.all());
      
      List<OrderType> result = orderTypes.stream()
          .map((in) -> OrderTypeConverter.convert(in, filter.getShowPath()))
          .filter(tableHelper.filter())
          .collect(Collectors.toList());
      tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (PersistenceLayerException e) {
      throw new LoadOrderTypesException(e.getMessage(), e);
    }
  }
 
}
