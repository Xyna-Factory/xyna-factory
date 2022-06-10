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
package com.gip.xyna.xprc.xprcods;



import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionManagement;
import com.gip.xyna.xprc.xprcods.abandonedorders.AbandonedOrdersManagement;
import com.gip.xyna.xprc.xprcods.capacitymapping.CapacityMappingDatabase;
import com.gip.xyna.xprc.xprcods.exceptionmgmt.ExceptionManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.ClusteredOrderArchive;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive;
import com.gip.xyna.xprc.xprcods.ordercontextconfiguration.OrderContextConfiguration;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;



public class XynaProcessingODS extends Section {

  public static final String DEFAULT_NAME = "Xyna Processing ODS";
  public static final int FUTUREEXECUTION_ID = XynaFactory.getInstance().getFutureExecution().nextId();

  public XynaProcessingODS() throws XynaException {
    super();
  }


  private ODS ods;


  @Override
  public void init() throws XynaException {

    ods = ODSImpl.getInstance();
    deployFunctionGroup(new ClusteredOrderArchive());
    deployFunctionGroup(new WorkflowDatabase());
    deployFunctionGroup(new ManualInteractionManagement());
    deployFunctionGroup(new CapacityMappingDatabase());
    deployFunctionGroup(new ExceptionManagement());
    XynaFactory.getInstance().getFutureExecution().execAsync(new FutureExecutionTask(FUTUREEXECUTION_ID) {

      @Override
      public void execute() {
      }
      
    });    
    deployFunctionGroup(new OrderContextConfiguration());
    deployFunctionGroup(new AbandonedOrdersManagement());

  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public OrderArchive getOrderArchive() {
    OrderArchive wfiDb = (OrderArchive) getFunctionGroup(OrderArchive.DEFAULT_NAME);
    if (wfiDb == null)
      logger.debug("Tried to access undeployed FunctionGroup " + OrderArchive.DEFAULT_NAME);

    return wfiDb;
  }


  public WorkflowDatabase getWorkflowDatabase() {
    WorkflowDatabase wfDb = (WorkflowDatabase) getFunctionGroup(WorkflowDatabase.DEFAULT_NAME);
    if (wfDb == null)
      logger.debug("Tried to access undeployed FunctionGroup " + WorkflowDatabase.DEFAULT_NAME);

    return wfDb;
  }


  public ExceptionManagement getExceptionManagement() {
    ExceptionManagement wfDb = (ExceptionManagement) getFunctionGroup(ExceptionManagement.DEFAULT_NAME);
    if (wfDb == null)
      logger.debug("Tried to access undeployed FunctionGroup " + ExceptionManagement.DEFAULT_NAME);

    return wfDb;
  }


  public ManualInteractionManagement getManualInteractionManagement() {
    ManualInteractionManagement miDb = (ManualInteractionManagement) getFunctionGroup(ManualInteractionManagement.DEFAULT_NAME);
    if (miDb == null)
      logger.debug("Tried to access undeployed FunctionGroup " + ManualInteractionManagement.DEFAULT_NAME);

    return miDb;
  }


  public CapacityMappingDatabase getCapacityMappingDatabase() {
    CapacityMappingDatabase cmDb = (CapacityMappingDatabase) getFunctionGroup(CapacityMappingDatabase.DEFAULT_NAME);
    if (cmDb == null)
      logger.debug("Tried to access undeployed FunctionGroup " + CapacityMappingDatabase.DEFAULT_NAME);

    return cmDb;
  }


  public OrderContextConfiguration getOrderContextConfiguration() {
    OrderContextConfiguration orderContextConfigInstance = (OrderContextConfiguration) getFunctionGroup(OrderContextConfiguration.DEFAULT_NAME);
    if (orderContextConfigInstance == null) {
      logger.debug("Tried to access undeployed FunctionGroup " + OrderContextConfiguration.DEFAULT_NAME);
    }
    return orderContextConfigInstance;
  }


  public AbandonedOrdersManagement getAbandonedOrdersManagement() {
    AbandonedOrdersManagement abandonedOrdersManagementInstance =
        (AbandonedOrdersManagement) getFunctionGroup(AbandonedOrdersManagement.DEFAULT_NAME);
    if (abandonedOrdersManagementInstance == null) {
      logger.debug("Tried to access undeployed FunctionGroup " + AbandonedOrdersManagement.DEFAULT_NAME);
    }
    return abandonedOrdersManagementInstance;
  }


  public ODS getODS() {
    return ods;
  }

}
