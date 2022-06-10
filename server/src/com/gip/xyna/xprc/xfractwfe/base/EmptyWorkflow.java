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
package com.gip.xyna.xprc.xfractwfe.base;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;


public class EmptyWorkflow extends XynaProcess {
  private static final long serialVersionUID = 1453878770433328822L;

  public EmptyWorkflow() {
  }


  public String getOriginalName() {
    return getClass().getName();
  }


  private static FractalProcessStep<?>[] steps = new FractalProcessStep[] { };
  private static FractalProcessStep<?>[] allSteps = steps;

  public FractalProcessStep<?>[] getStartSteps() {
    return steps;
  }

  public FractalProcessStep<?>[] getAllSteps() {
    return allSteps;
  }
  
  public FractalProcessStep<?>[] getAllLocalSteps() {
    return allSteps;
  }

  private GeneralXynaObject input;

  @Override
  protected void initializeMemberVars() {
  }
  
  public void setInputVars(GeneralXynaObject o) {
    input = o;
  }

  public GeneralXynaObject getOutput() {
    return input;
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

  public void removePreHandler(Handler h) {
    preHandlers = XynaProcess.removeHandlerFromArray(h, preHandlers);
  }

  public void removeErrorHandler(Handler h) {
    errorHandlers = XynaProcess.removeHandlerFromArray(h, errorHandlers);
  }

  protected void onDeployment() throws XynaException {
  }

  protected void onUndeployment() throws XynaException {
  }

}
