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

package com.gip.xyna.xfmg.xfmon;



import com.gip.xyna.Section;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfmon.componentmonitoring.ComponentMonitoring;
import com.gip.xyna.xfmg.xfmon.fruntimestats.FactoryRuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.fruntimestats.RuntimeStatistics;
import com.gip.xyna.xfmg.xfmon.processmonitoring.ProcessMonitoring;



public class XynaFactoryMonitoring extends Section {

  public static final String DEFAULT_NAME = "Xyna Factory Monitoring";

  private RuntimeStatistics runtimeStatistics;
  private ProcessMonitoring processMonitoring;

  public XynaFactoryMonitoring() throws XynaException {
    super();
  }


  @Override
  public void init() throws XynaException {
    processMonitoring = new ProcessMonitoring();
    deployFunctionGroup(processMonitoring);
    deployFunctionGroup(new ComponentMonitoring());
    runtimeStatistics = new RuntimeStatistics();
    deployFunctionGroup(runtimeStatistics);
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public FactoryRuntimeStatistics getFactoryRuntimeStatistics() {
    return runtimeStatistics;
  }
  
  public ProcessMonitoring getProcessMonitoring() {
    return processMonitoring;
  }
  
}
