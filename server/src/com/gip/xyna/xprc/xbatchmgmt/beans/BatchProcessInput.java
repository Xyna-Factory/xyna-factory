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
package com.gip.xyna.xprc.xbatchmgmt.beans;

import java.io.Serializable;

import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xprc.xbatchmgmt.input.InputGeneratorData;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeWindowDefinition;


public class BatchProcessInput implements Serializable{

  private static final long serialVersionUID = 1L;

  private String label; //Bezeichnung des Batch Processes 
  private String component; //Die zugehörige Komponente, z.B. XDNC.XFwMgmt
  private RemoteXynaOrderCreationParameter masterOrder; //XynaOrderCreationParameter des Masters (enthält die TimeConstraints) 
  private String slaveOrderType; //OrderType der Slaves
  private AbsRelTime slaveOrderExecTimeout; //OrderExecutionTimeout der Slaves 
  private AbsRelTime slaveWorkflowExecTimeout; //WorkflowExecutionTimeout der Slaves 
  private TimeConstraint slaveTimeConstraint; //TimeConstraint der Slaves 
  private InputGeneratorData inputGeneratorData; //Daten für den Input Generator
  private SlaveExecutionPeriod slaveExecutionPeriod; //Wiederholungen ser Slave-Starts
  private int maxParallelism; // maximale Anzahl an Slaves, die gleichzeitig eingestellt werden
  private boolean paused; //true, wenn der BatchProcess im pausierten Zustand gestartet werden soll
  private TimeWindowDefinition timeWindowDefinition; //wenn Zeitfenster nur für den BatchProcess angelegt wird, wird hier die Definition übergeben
  private String guiRepresentationData; //beliebiger String, zur freien Verwendung für den WebService
  
  
  public RemoteXynaOrderCreationParameter getMasterOrder() {
    return masterOrder;
  }
  
  public void setMasterOrder(RemoteXynaOrderCreationParameter masterOrder) {
    this.masterOrder = masterOrder;
  }
  
  public String getLabel() {
    return label;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public String getComponent() {
    return component;
  }
  
  public void setComponent(String component) {
    this.component = component;
  }
    
  public String getSlaveOrderType() {
    return slaveOrderType;
  }
  
  public void setSlaveOrderType(String slaveOrderType) {
    this.slaveOrderType = slaveOrderType;
  }
  
  public AbsRelTime getSlaveOrderExecTimeout() {
    return slaveOrderExecTimeout;
  }

  public void setSlaveOrderExecTimeout(AbsRelTime slaveOrderExecTimeout) {
    this.slaveOrderExecTimeout = slaveOrderExecTimeout;
  }

  public AbsRelTime getSlaveWorkflowExecTimeout() {
    return slaveWorkflowExecTimeout;
  }

  public void setSlaveWorkflowExecTimeout(AbsRelTime slaveWorkflowExecTimeout) {
    this.slaveWorkflowExecTimeout = slaveWorkflowExecTimeout;
  }

  public TimeConstraint getSlaveTimeConstraint() {
    return slaveTimeConstraint;
  }

  public void setSlaveTimeConstraint(TimeConstraint slaveTimeConstraint) {
    this.slaveTimeConstraint = slaveTimeConstraint;
  }

  public InputGeneratorData getInputGeneratorData() {
    return inputGeneratorData;
  }

  public void setInputGeneratorData(InputGeneratorData inputGeneratorData) {
    this.inputGeneratorData = inputGeneratorData;
  }
  
  public SlaveExecutionPeriod getSlaveExecutionPeriod() {
    return slaveExecutionPeriod;
  }
  
  public void setSlaveExecutionPeriod(SlaveExecutionPeriod slaveExecutionPeriod) {
    this.slaveExecutionPeriod = slaveExecutionPeriod;
  }
  
  public int getMaxParallelism() {
    return maxParallelism;
  }

  public void setMaxParallelism(int maxParallelism) {
    this.maxParallelism = maxParallelism;
  }

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  public TimeWindowDefinition getTimeWindowDefinition() {
    return timeWindowDefinition;
  }

  public void setTimeWindowDefinition(TimeWindowDefinition timeWindowDefinition) {
    this.timeWindowDefinition = timeWindowDefinition;
  }

  public String getGuiRepresentationData() {
    return guiRepresentationData;
  }

  public void setGuiRepresentationData(String guiRepresentationData) {
    this.guiRepresentationData = guiRepresentationData;
  }
}
