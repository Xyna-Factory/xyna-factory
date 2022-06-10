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
package com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalplanning;



import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.Handler;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xsched.SchedulerBean;



public class DefaultPlanning extends XynaProcess {

  private static final long serialVersionUID = -2675958085032874167L;
  protected static final Logger logger = CentralFactoryLogging.getLogger(DefaultPlanning.class);


  private static FractalProcessStep<?>[] steps = new FractalProcessStep[] {};
  private static FractalProcessStep<?>[] allSteps = steps;


  public DefaultPlanning() {
  }


  @Override
  public String getOriginalName() {
    return getClass().getName();
  }


  public FractalProcessStep<?>[] getStartSteps() {
    return steps;
  }


  public FractalProcessStep<?>[] getAllSteps() {
    return allSteps;
  }

  public FractalProcessStep<?>[] getAllLocalSteps() {
    return allSteps;
  }


  public void setInputVars(GeneralXynaObject o) {
  }


  public SchedulerBean getOutput() {

    logger.debug("Gathering output of default planning workflow");

    // capacities hängen standardmässig am execution-wf
    DestinationKey key = getCorrelatedXynaOrder().getDestinationKey();
    List<Capacity> caps = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getCapacityMappingDatabase()
                    .getCapacities(key);

    if (caps != null) {
      return new SchedulerBean(caps);
    }
    else {
      return new SchedulerBean();
    }

  }


  @Override
  protected void initializeMemberVars() {
  }


  private static Handler[] preHandlers = new Handler[0];
  private static Handler[] errorHandlers = new Handler[0];
  private static Handler[] postHandlers = new Handler[0];
  

  public void addPreHandler(Handler h) {
    preHandlers = XynaProcess.addHandlerToArray(h, preHandlers);
  }


  public void addErrorHandler(Handler h) {
    errorHandlers = XynaProcess.addHandlerToArray(h, errorHandlers);
  }


  public void addPostHandler(Handler h) {
    postHandlers = XynaProcess.addHandlerToArray(h, postHandlers);
  }


  public Handler[] getPreHandlers() {
    return preHandlers;
  }


  public Handler[] getPostHandlers() {
    return postHandlers;
  }


  public Handler[] getErrorHandlers() {
    return errorHandlers;
  }


  public void removePostHandler(Handler h) {
    postHandlers = XynaProcess.removeHandlerFromArray(h, postHandlers);
  }


  public void removeErrorHandler(Handler h) {
    errorHandlers = XynaProcess.removeHandlerFromArray(h, errorHandlers);
  }


  public void removePreHandler(Handler h) {
    preHandlers = XynaProcess.removeHandlerFromArray(h, preHandlers);
  }


  @Override
  protected void onDeployment() throws XynaException {
  }


  @Override
  protected void onUndeployment() throws XynaException {
  }

}
