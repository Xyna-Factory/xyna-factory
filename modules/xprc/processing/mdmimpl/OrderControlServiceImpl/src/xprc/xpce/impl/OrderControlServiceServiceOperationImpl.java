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
package xprc.xpce.impl;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xprc.OrderStatus;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DESTINATION_NOT_FOUND;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule.Builder;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.xynaobjects.Capacity;
import com.gip.xyna.xprc.xsched.xynaobjects.Priority;
import com.gip.xyna.xprc.xsched.xynaobjects.SchedulerInformation;
import com.gip.xyna.xprc.xsched.xynaobjects.TimeConstraint;
import com.gip.xyna.xprc.xsched.xynaobjects.Veto;

import xprc.xpce.CustomFields;
import xprc.xpce.InheritanceRule;
import xprc.xpce.LoggingContext;
import xprc.xpce.OrderControlServiceServiceOperation;
import xprc.xpce.OrderId;
import xprc.xpce.OrderType;
import xprc.xpce.ParameterType;
import xprc.xpce.WorkflowName;
import xprc.xpce.enums.orderhierarchy.AllParents;
import xprc.xpce.enums.orderhierarchy.Own;
import xprc.xpce.enums.orderhierarchy.Parent;
import xprc.xpce.enums.orderhierarchy.Root;
import xprc.xpce.enums.orderhierarchy.Scope;


public class OrderControlServiceServiceOperationImpl implements ExtendedDeploymentTask, OrderControlServiceServiceOperation {

  private static Logger logger = CentralFactoryLogging.getLogger(OrderControlServiceServiceOperationImpl.class);
  
  @Override
  public void onDeployment() throws XynaException {
    //nichts zu tun
  }

  @Override
  public void onUndeployment() throws XynaException {
    //nichts zu tun
  }

  @Override
  public Long getOnUnDeploymentTimeout() {
    return null;
  }

  @Override
  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }

  @Override
  public OrderId getOrderId(XynaOrderServerExtension correlatedXynaOrder) {
    long orderId = correlatedXynaOrder.getId();
    return new OrderId(orderId);
  }

  @Override
  public OrderType getOrderType(XynaOrderServerExtension correlatedXynaOrder) {
    String orderType = correlatedXynaOrder.getDestinationKey().getOrderType();
    return new OrderType(orderType);
  }

  @Override
  public OrderId getRootOrderId(XynaOrderServerExtension correlatedXynaOrder) {
    long rootOrderId = correlatedXynaOrder.getRootOrder().getId();
    return new OrderId(rootOrderId);
  }

  @Override
  public void setLoggingContext(XynaOrderServerExtension correlatedXynaOrder, LoggingContext loggingContext) {
    String logString = loggingContext.getLoggingContext();
    correlatedXynaOrder.getOrderContext().setLoggingDiagnosisContext(logString);
  }
  
  /** 
   * Create a new SchedulerInformation in planning phase (with Capacities from configured OrderType).
   */
  @Override
  public SchedulerInformation createSchedulerInformation(XynaOrderServerExtension correlatedXynaOrder) {
    //Im MasterWorkflowPreScheduler.xynaPlanning() sind bereits alle SchedulingData gesetzt bis auf die Capacities.
    //Daher können fast alle Daten direkt geholt werden.
    SchedulerInformation schedulerInformation = convertSchedulingData(correlatedXynaOrder.getSchedulingData());
    
    //Das Planning ist für das Setzen der Capacities zuständig, daher werden hier nun die 
    //Capacities aus der CapacityMappingDatabase geholt.
    List<com.gip.xyna.xprc.xpce.planning.Capacity> capacities =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
            .getCapacities(correlatedXynaOrder.getDestinationKey());
    schedulerInformation.setCapacities(createCapacityList(capacities));
    
    //Aus dem Filter können derzeit keine Capacities und Vetos übertragen werden, daher werden hier nun auch 
    //keine konfigurierten Capacities überschrieben.
    
    if( logger.isDebugEnabled() ) {
      logger.debug( "createSchedulerInformation " + schedulerInformation);
    }
    return schedulerInformation;
  }

  /** 
   * Get current SchedulerInformation in execution phase.
   */
  @Override
  public SchedulerInformation getCurrentSchedulerInformation(XynaOrderServerExtension correlatedXynaOrder) {
    SchedulerInformation schedulerInformation = convertSchedulingData(correlatedXynaOrder.getSchedulingData());
    if( logger.isDebugEnabled() ) {
      logger.debug( "getCurrentSchedulerInformation " + schedulerInformation);
    }
    return schedulerInformation;
  }

  private SchedulerInformation convertSchedulingData(SchedulingData schedulingData) {
    SchedulerInformation schedulerInformation = new SchedulerInformation();
    //derzeit gesetzte Werte aus SchedulingData
    schedulerInformation.setCapacities(createCapacityList(schedulingData.getCapacities()));
    schedulerInformation.setVetos(createVetoList(schedulingData.getVetos()));
    schedulerInformation.setPriority( new Priority( schedulingData.getPriority() ) );
    schedulerInformation.setTimeConstraint( TimeConstraint.fromDefinition( schedulingData.getTimeConstraint() ) );
    return schedulerInformation;
  }

  @Override
  public void addParameterInheritanceRule(XynaOrderServerExtension correlatedXynaOrder, ParameterType parameterType, InheritanceRule inheritanceRule) {
    if (parameterType != null && inheritanceRule != null) {
      com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType type
          = com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType.valueOf(parameterType.getClass().getSimpleName());
      Builder builder = type.createInheritanceRuleBuilder(inheritanceRule.getValue())
                            .precedence(inheritanceRule.getPrecedence());

      if (inheritanceRule.getChildFilter() != null) {
        builder.childFilter(inheritanceRule.getChildFilter());
      }

      correlatedXynaOrder.addParameterInheritanceRule(type, builder.build());
      if (type == com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType.MonitoringLevel) {
        try {
          Integer oldML = correlatedXynaOrder.getMonitoringCode();
          if (oldML != null) {
            correlatedXynaOrder.setMonitoringLevelAlreadyDiscovered(false);
          }
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher().dispatch(correlatedXynaOrder);
          Integer newML = correlatedXynaOrder.getMonitoringCode();
          if (oldML != null && !oldML.equals(newML)) {
            if (oldML >= 10 && newML < 10) {
              //achtung, evtl wird der orderarchive-eintrag beim archivieren nicht entfernt.
              //=> deshalb jetzt entfernen
              try {
                XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().deleteFromOrderArchive(correlatedXynaOrder.getId());
              } catch (Throwable t) {
                Department.handleThrowable(t);
                //TODO Exception nicht loggen, sondern als Warnung an XynaOrder anhängen
                logger.warn("Could not delete orderinstance for " + correlatedXynaOrder, t);
              }
            } else if (oldML < 10 && newML >= 10) {
              if (correlatedXynaOrder.getExecutionProcessInstance() != null) {
                //zu spät - es kann kein eintrag angelegt werden.
                logger.warn("Can not change monitoringlevel of order already running in workflow.");
                //zurücksetzen
                correlatedXynaOrder.setMonitoringLevel(oldML);
              } else {
                //orderarchiveeintrag jetzt anlegen!
                setMasterWorkflowStatus(correlatedXynaOrder, OrderInstanceStatus.RUNNING_PLANNING);
              }
            }
          }
        } catch (XPRC_DESTINATION_NOT_FOUND e) {
          logger.warn("Could not set monitoring level of order " + correlatedXynaOrder, e);
        }
      }
    }
  }


  private void setMasterWorkflowStatus(XynaOrderServerExtension xo, OrderInstanceStatus orderInstanceStatus) {
    try {
      OrderStatus orderStatus = XynaFactory.getInstance().getProcessing().getOrderStatus();
      orderStatus.changeMasterWorkflowStatus(xo, orderInstanceStatus, null);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      //TODO Exception nicht loggen, sondern als Warnung an XynaOrder anhängen
      logger.warn("Could not write orderinstance status " + orderInstanceStatus + " for " + xo, t);
    }
  }
  
  private List<Veto> createVetoList(List<String> vetos) {
    if( vetos == null ) {
      return null;
    }
    ArrayList<Veto> vxos = new ArrayList<Veto>(vetos.size());
    for( String v : vetos ) {
      vxos.add( new Veto(v) );
    }
    return vxos;
  }
  
  private List<Capacity> createCapacityList(List<com.gip.xyna.xprc.xpce.planning.Capacity> capacities) {
    if( capacities == null ) {
      return null;
    }
    ArrayList<Capacity> cxos = new ArrayList<Capacity>(capacities.size());
    for( com.gip.xyna.xprc.xpce.planning.Capacity c : capacities ) {
      cxos.add( new Capacity(c.getCapName(), c.getCardinality()) );
    }
    return cxos;
  }

  @Override
  public CustomFields getCustomFields(XynaOrderServerExtension xose) {
    CustomFields fields = new CustomFields();
    fields.setCustom1(xose.getCustom0());
    fields.setCustom2(xose.getCustom1());
    fields.setCustom3(xose.getCustom2());
    fields.setCustom4(xose.getCustom3());
    return fields;
  }

  @Override
  public void setCustomFields(XynaOrderServerExtension xose, CustomFields fields) {
    if (fields.getCustom1() != null) {
      xose.setCustom0(fields.getCustom1());
    }
    if (fields.getCustom2() != null) {
      xose.setCustom1(fields.getCustom2());
    }
    if (fields.getCustom3() != null) {
      xose.setCustom2(fields.getCustom3());
    }
    if (fields.getCustom4() != null) {
      xose.setCustom3(fields.getCustom4());
    }
  }

  @Override
  public CustomFields getCustomFieldsFromScope(XynaOrderServerExtension xose, Scope scope) {
    if( scope instanceof Own ) {
      return getCustomFields(xose);
    } else if( scope instanceof Parent ) {
      return getCustomFields(xose.getParentOrder());
    } else if( scope instanceof Root ) {
      return getCustomFields(xose.getRootOrder());
    } else {
      throw new IllegalArgumentException("Unexpected Scope "+ scope);
    }
  }

  @Override
  public void setCustomFieldsForScope(XynaOrderServerExtension xose, CustomFields fields, Scope scope) {
    if( scope instanceof Own ) {
      setCustomFields(xose,fields);
    } else if( scope instanceof Parent ) {
      setCustomFields(xose.getParentOrder(), fields);
    } else if( scope instanceof Root ) {
      setCustomFields(xose.getRootOrder(), fields);
    } else if( scope instanceof AllParents ) {
      XynaOrderServerExtension parent = xose;
      while( parent != null ) {
        setCustomFields(parent, fields);
        parent = parent.getParentOrder();
      }
    } else {
      throw new IllegalArgumentException("Unexpected Scope "+ scope);
    }
  }

  @Override
  public WorkflowName getOwnFqClassName() {
    return new WorkflowName(com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.childOrderStorageStack.get().getCorrelatedXynaOrder().getExecutionProcessInstance().getClass().getName());

  }

  @Override
  public WorkflowName getOwnXMLName() {
    return new WorkflowName(com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.childOrderStorageStack.get().getCorrelatedXynaOrder().getExecutionProcessInstance().getOriginalName());
  }



}
