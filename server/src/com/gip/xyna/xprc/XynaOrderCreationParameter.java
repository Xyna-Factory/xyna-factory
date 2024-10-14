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
package com.gip.xyna.xprc;



import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension.AcknowledgableObject;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.parameterinheritance.ParameterInheritanceManagement.ParameterType;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRule;
import com.gip.xyna.xprc.xpce.parameterinheritance.rules.InheritanceRuleCollection;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;



public class XynaOrderCreationParameter implements Serializable {

  private static final long serialVersionUID = -9132940371811860102L;


  private DestinationKey destinationKey;
  private CustomStringContainer customStringContainer = new CustomStringContainer("", "", "", "");
  private transient long idOfLatestDeploymentKnownToOrder;
  private int priority = -1;
  private String sessionId;
  private Long absoluteSchedulingTimeout;
  private Long relativeSchedulingTimeout;
  private SeriesInformation seriesInformation;
  private volatile Map<String, RunnableForFilterAccess> runnablesForFilterAccess;
  private transient GeneralXynaObject inputPayload = new Container();
  private MiscellaneousDataBean dataBean;
  private ExecutionTimeoutConfiguration orderExecutionTimeout;
  private ExecutionTimeoutConfiguration workflowExecutionTimeout;
  private AcknowledgableObject acknowledgableObject;
  private TimeConstraint timeConstraint;
  private transient Role role;
  private long inputSourceId = -1;
  private Integer monitoringLevel;
  private Map<ParameterType, InheritanceRuleCollection> parameterInheritanceRules; //Parameter Vererbungsregeln

  public XynaOrderCreationParameter(DestinationKey dk, GeneralXynaObject... inputPayload) {
    if (dk == null || dk.getOrderType() == null) {
      throw new IllegalArgumentException("DestinationKey with ordertype null detected");
    }
    this.destinationKey = dk;
    this.setInputPayload(inputPayload);
    
    parameterInheritanceRules = ParameterType.createInheritanceRuleMap();
  }


  public XynaOrderCreationParameter(DestinationKey dk, Long absoluteSchedulingTimeout,
                                    GeneralXynaObject... inputPayload) {
    this(dk, inputPayload);
    this.absoluteSchedulingTimeout = absoluteSchedulingTimeout;
  }

  
  public XynaOrderCreationParameter(String orderType, GeneralXynaObject... inputPayload) {
    this(new DestinationKey(orderType), inputPayload);
  }
  

  public XynaOrderCreationParameter(String orderType, int prio, GeneralXynaObject... inputPayload) {
    this(new DestinationKey(orderType), inputPayload);
    this.priority = prio;
  }


  public XynaOrderCreationParameter(String orderType, int prio, Long absoluteSchedulingTime,
                                    GeneralXynaObject... inputPayload) {
    this(orderType, prio, inputPayload);
    this.absoluteSchedulingTimeout = absoluteSchedulingTime;
  }


  public XynaOrderCreationParameter(String orderType, int prio, String sessionId, GeneralXynaObject... inputPayload) {
    this(orderType, prio, inputPayload);
    this.sessionId = sessionId;
  }


  public XynaOrderCreationParameter(String orderType, int prio, Long absoluteSchedulingTime, String sessionId,
                                    GeneralXynaObject... inputPayload) {
    this(orderType, prio, absoluteSchedulingTime, inputPayload);
    this.sessionId = sessionId;
  }


  public XynaOrderCreationParameter(String orderType, int prio, CustomStringContainer customStrings,
                                    GeneralXynaObject... inputPayload) {
    this(orderType, prio, inputPayload);
    if (customStrings != null) {
      this.customStringContainer = customStrings;
    }
  }


  public XynaOrderCreationParameter(String orderType, int prio, Long absoluteSchedulingTime,
                                    CustomStringContainer customStrings, GeneralXynaObject... inputPayload) {
    this(orderType, prio, absoluteSchedulingTime, inputPayload);
    if (customStrings != null) {
      this.customStringContainer = customStrings;
    }
  }


  public XynaOrderCreationParameter(String orderType, int prio, CustomStringContainer customStrings, String sessionId,
                                    GeneralXynaObject... inputPayload) {
    this(orderType, prio, inputPayload);
    this.sessionId = sessionId;
    if (customStrings != null) {
      this.customStringContainer = customStrings;
    }
  }


  public XynaOrderCreationParameter(String orderType, int prio, Long absoluteSchedulingTime,
                                    CustomStringContainer customStrings, String sessionId,
                                    GeneralXynaObject... inputPayload) {
    this(orderType, prio, absoluteSchedulingTime, customStrings, inputPayload);
    this.sessionId = sessionId;
  }


  public XynaOrderCreationParameter(XynaOrderCreationParameter xocp) {
    this.absoluteSchedulingTimeout = xocp.absoluteSchedulingTimeout;
    this.acknowledgableObject = xocp.acknowledgableObject;
    this.customStringContainer = xocp.customStringContainer;
    this.dataBean = xocp.dataBean;
    this.destinationKey = xocp.destinationKey;
    this.inputPayload = xocp.inputPayload;
    this.orderExecutionTimeout = xocp.orderExecutionTimeout;
    this.priority = xocp.priority;
    this.relativeSchedulingTimeout = xocp.relativeSchedulingTimeout;
    this.role = xocp.role;
    this.runnablesForFilterAccess = xocp.runnablesForFilterAccess;
    this.seriesInformation = xocp.seriesInformation;
    this.sessionId = xocp.sessionId;
    this.timeConstraint = xocp.timeConstraint;
    this.workflowExecutionTimeout = xocp.workflowExecutionTimeout;
    this.inputSourceId = xocp.inputSourceId;
    this.monitoringLevel = xocp.monitoringLevel;
    this.parameterInheritanceRules = xocp.parameterInheritanceRules;
  }


  public void setDestinationKey(DestinationKey destinationKey) {
    this.destinationKey = destinationKey;
  }


  public DestinationKey getDestinationKey() {
    return destinationKey;
  }


  public void setOrderType(String orderType) {
    this.destinationKey = new DestinationKey(orderType);
  }


  public String getOrderType() {
    if (destinationKey == null) {
      return null;
    }
    return destinationKey.getOrderType();
  }

  public final void setInputPayloadDirectly(GeneralXynaObject ... payload) {
    if (payload == null) {
      this.inputPayload = new Container();
    } else if (payload.length == 1) {
      this.inputPayload = payload[0];
    } else {
      this.inputPayload = new Container(payload);
    }
  }

  public void setInputPayload(GeneralXynaObject payload) {
    if (payload != null) {
      this.inputPayload = payload;
    } else {
      this.inputPayload = new Container();
    }
  }


  public void setInputPayload(GeneralXynaObject... payload) {
    setInputPayloadDirectly(payload);    
  }


  public GeneralXynaObject getInputPayload() {
    return inputPayload;
  }


  public void addInputPayload(GeneralXynaObject newInput) {
    if (inputPayload == null) {
      inputPayload = newInput;
    } else if (inputPayload instanceof Container) {
      ((Container) inputPayload).add(newInput);
    } else {
      inputPayload = new Container(inputPayload, newInput);
    }
  }


  public void setCustom0(String value) {
    customStringContainer.setCustom1(value);
  }


  public void setCustom1(String value) {
    customStringContainer.setCustom2(value);
  }


  public void setCustom2(String value) {
    customStringContainer.setCustom3(value);
  }


  public void setCustom3(String value) {
    customStringContainer.setCustom4(value);
  }


  public String getCustom0() {
    return customStringContainer.getCustom0();
  }


  public String getCustom1() {
    return customStringContainer.getCustom1();
  }


  public String getCustom2() {
    return customStringContainer.getCustom2();
  }


  public String getCustom3() {
    return customStringContainer.getCustom3();
  }


  public CustomStringContainer getCustomStringContainer() {
    return customStringContainer;
  }


  public void setCustomStringContainer(CustomStringContainer newContainer) {
    customStringContainer = newContainer;
  }

  
  public void setSessionId(String value) {
    sessionId = value;
  }
  
  public String getSessionId() {
    return sessionId;
  }
  
  
  public void setIdOfLatestDeploymentKnownToOrder(long latestId) {
    idOfLatestDeploymentKnownToOrder = latestId;
  }
  

  public long getIdOfLatestDeploymentKnownToOrder() {
    return idOfLatestDeploymentKnownToOrder;
  }


  public int getPriority() {
    return priority;
  }


  public void setPriority(int i) {
    priority = i;
  }


  /**
   * @deprecated use setTimeConstraint( TimeConstraint.immediately().withAbsoluteSchedulingTimeout(schedulingTimeout) )
   */
  @Deprecated
  public void setAbsoluteSchedulingTimeout(Long schedulingTimeout) {
    this.absoluteSchedulingTimeout = schedulingTimeout;
  }


  public Long getAbsoluteSchedulingTimeout() {
    return absoluteSchedulingTimeout;
  }

  
  /**
   * @deprecated use setTimeConstraint( TimeConstraint.immediately().withSchedulingTimeout(5000) )
   */
  @Deprecated
  public void setRelativeSchedulingTimeout(Long schedulingTimeout) {
    this.relativeSchedulingTimeout = schedulingTimeout;
  }


  public Long getRelativeSchedulingTimeout() {
    return relativeSchedulingTimeout;
  }


  public void setSeriesInformation(SeriesInformation si) {
    this.seriesInformation = si;
  }


  public SeriesInformation getSeriesInformation() {
    return seriesInformation;
  }


  public RunnableForFilterAccess getRunnableForFilterAccess(String key) {
    return runnablesForFilterAccess == null ? null : runnablesForFilterAccess.get(key);
  }


  public void addRunnableForFilterAccess(String key, RunnableForFilterAccess runnable) {
    if (runnablesForFilterAccess == null) {
      synchronized (this) {
        if (runnablesForFilterAccess == null) {
          runnablesForFilterAccess = new ConcurrentHashMap<String, RunnableForFilterAccess>();
        }
      }
    }
    runnablesForFilterAccess.put(key, runnable);
  }


  Map<String, RunnableForFilterAccess> getAllRunnablesForFilterAccess() {
    return runnablesForFilterAccess;
  }


  public MiscellaneousDataBean getDataBean() {
    return dataBean;
  }


  public void setDataBean(MiscellaneousDataBean dataBean) {
    this.dataBean = dataBean;
  }
  
  
  @Deprecated
  public ExecutionTimeoutConfiguration getExecutionTimeoutConfiguration() {
    return orderExecutionTimeout;
  }


  @Deprecated
  public void setExecutionTimeoutConfiguration(ExecutionTimeoutConfiguration orderExecutionTimeout) {
    this.orderExecutionTimeout = orderExecutionTimeout;
  }
  
  
  public ExecutionTimeoutConfiguration getOrderExecutionTimeoutConfiguration() {
    return orderExecutionTimeout;
  }


  public void setOrderExecutionTimeoutConfiguration(ExecutionTimeoutConfiguration orderExecutionTimeout) {
    this.orderExecutionTimeout = orderExecutionTimeout;
  }
  
  public ExecutionTimeoutConfiguration getWorkflowExecutionTimeoutConfiguration() {
    return workflowExecutionTimeout;
  }


  public void setWorkflowExecutionTimeoutConfiguration(ExecutionTimeoutConfiguration workflowExecutionTimeout) {
    this.workflowExecutionTimeout = workflowExecutionTimeout;
  }


  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
    s.writeObject(new SerializableClassloadedXynaObject(inputPayload));
  }


  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    SerializableClassloadedXynaObject o = (SerializableClassloadedXynaObject) s.readObject();
    inputPayload = o.getXynaObject();
    if (XynaFactory.isFactoryServer() && !XynaFactory.getInstance().isStartingUp()) {
      try {
        idOfLatestDeploymentKnownToOrder = DeploymentManagement.getInstance().getLatestDeploymentId();
      } catch (Throwable t) {
        idOfLatestDeploymentKnownToOrder = 0;
      }
    } else {
      idOfLatestDeploymentKnownToOrder = 0;
    }
  }


  public AcknowledgableObject getAcknowledgableObject() {
    return acknowledgableObject;
  }


  
  public void setAcknowledgableObject(AcknowledgableObject acknowledgableObject) {
    this.acknowledgableObject = acknowledgableObject;
  }

  
  /**
   * setzt TimeConstraint 
   * @param timeConstraint
   */
  public void setTimeConstraint(TimeConstraint timeConstraint) {
    this.timeConstraint = timeConstraint;
  }
  
  public TimeConstraint getTimeConstraint() {
    return timeConstraint;
  }
  
  
  public void setTransientCreationRole(Role role) {
    this.role = role;
  }
  
  public Role getTransientCreationRole() {
    return role;
  }
  
  protected void setDestinationKeyNull() {
    destinationKey = null;
  }


  protected void setInputPayloadNull() {
    inputPayload = null;
  }

  public void setOrderInputSourceId(long inputSourceId) {
    this.inputSourceId = inputSourceId;
  }

  public long getOrderInputSourceId() {
    return inputSourceId;
  }
  
  /**
   * @param monitoringlevel null falls nicht gesetzt
   */
  public void setMonitoringLevel(Integer monitoringlevel) {
    this.monitoringLevel = monitoringlevel;
  }

  /**
   * @return null falls nicht gesetzt
   */
  public Integer getMonitoringLevel() {
    return monitoringLevel;
  }
  
  public void addParameterInheritanceRule(ParameterType type, InheritanceRule inheritanceRule) {
    parameterInheritanceRules.get(type).add(inheritanceRule);
  }

  public Map<ParameterType, InheritanceRuleCollection> getParameterInheritanceRules() {
    return parameterInheritanceRules;
  }
}
