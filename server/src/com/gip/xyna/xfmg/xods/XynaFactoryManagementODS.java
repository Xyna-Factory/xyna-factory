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

package com.gip.xyna.xfmg.xods;



import com.gip.xyna.FutureExecution;
import com.gip.xyna.FutureExecutionTask;
import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.statistics.XynaStatistics;
import com.gip.xyna.xfmg.statistics.XynaStatisticsLegacy;
import com.gip.xyna.xfmg.xods.configuration.ClusteredConfiguration;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.ordertypemanagement.OrdertypeManagement;
import com.gip.xyna.xfmg.xods.priority.PriorityManagement;



public class XynaFactoryManagementODS extends Section {

  public static final String DEFAULT_NAME = "Xyna Factory Management ODS";


  public XynaFactoryManagementODS() throws XynaException {
    super();
  }


  @Override
  public void init() throws XynaException {
    deployFunctionGroup(new ClusteredConfiguration());
    deployFunctionGroup(new OrdertypeManagement());
    deployFunctionGroup(new PriorityManagement());
    deployFunctionGroup(new OrderInputSourceManagement());
    FutureExecution fe = XynaFactory.getInstance().getFutureExecution();
    fe.execAsync(new FutureExecutionTask(XynaStatistics.FUTUREEXECUTION_ID) {

      @Override
      public void execute() {
        //damit andere sich hier hinter hängen können
        try {
          deployFunctionGroup(new XynaStatistics());
          deployFunctionGroup(new XynaStatisticsLegacy());
        } catch (XynaException e) {
          throw new RuntimeException(e);
        }
      }
      
    });
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public Configuration getConfiguration() {
    return (Configuration) getFunctionGroup(Configuration.DEFAULT_NAME);
  }


  public OrdertypeManagement getOrderTypeManagement() {
    return (OrdertypeManagement) getFunctionGroup(OrdertypeManagement.DEFAULT_NAME);
  }


  public XynaStatistics getXynaStatistics() {
    return (XynaStatistics) getFunctionGroup(XynaStatistics.DEFAULT_NAME);
  }
  
  
  public PriorityManagement getPriorityManagement() {
    return (PriorityManagement) getFunctionGroup(PriorityManagement.DEFAULT_NAME);
  }


  public OrderInputSourceManagement getOrderInputSourceManagement() {
    return (OrderInputSourceManagement) getFunctionGroup(OrderInputSourceManagement.DEFAULT_NAME);
  }
}
