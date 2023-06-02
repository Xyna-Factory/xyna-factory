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

package com.gip.xyna.xprc.xpce;

import java.util.Collection;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.base.EngineSpecificProcess;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;


public abstract class EngineSpecificWorkflowProcessor extends FunctionGroup {

  public EngineSpecificWorkflowProcessor() throws XynaException {
    super();
  }


  public abstract void process(DestinationValue dv, XynaOrderServerExtension xo) throws XynaException;


  // TODO this should not point directly to the fractal workflow engine
  protected XynaFractalWorkflowEngine getFractalWorkflowEngine() {
    return (XynaFractalWorkflowEngine) XynaFactory.getInstance().getProcessing().getWorkflowEngine();
  }


  public abstract EngineSpecificProcess getRunningProcessById(long orderId);
  
  public abstract int getNumberOfRunningProcesses();

  public abstract Collection<XynaOrderServerExtension> getOrdersOfRunningProcesses();
}
