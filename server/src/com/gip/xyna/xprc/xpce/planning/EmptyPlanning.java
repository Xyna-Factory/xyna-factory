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
package com.gip.xyna.xprc.xpce.planning;



import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.Handler;
import com.gip.xyna.xprc.xfractwfe.base.Scope;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xsched.SchedulerBean;



// FIXME dependency nach XFractWFE! Klasse nach XFractWFE verschieben könnte funktionieren (Abwärtskompatibilität
//       beachten!), verschiebt aber das Problem nur auf die Referenz auf diese Klasse
public class EmptyPlanning extends XynaProcess {

  private static final long serialVersionUID = -8738033258678732122L;

  private static Handler[] preHandlers = new Handler[0];
  private static Handler[] errorHandlers = new Handler[0];
  private static Handler[] postHandlers = new Handler[0];

  private static final ReentrantLock handlerLock = new ReentrantLock();


  public EmptyPlanning() {
  }


  public String getOriginalName() {
    return EmptyPlanning.class.getName();
  }


  private static FractalProcessStep<?>[] steps = new FractalProcessStep[] {};
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


  protected void setVars(XynaObject o) {
  }


  public SchedulerBean getOutput() {
    return new SchedulerBean();
  }


  @Override
  protected void initializeMemberVars() {
  }


  public void addPreHandler(Handler h) {
    handlerLock.lock();
    try {
      preHandlers = XynaProcess.addHandlerToArray(h, preHandlers);
    } finally {
      handlerLock.unlock();
    }
  }


  public void addErrorHandler(Handler h) {
    handlerLock.lock();
    try {
      errorHandlers = XynaProcess.addHandlerToArray(h, errorHandlers);
    } finally {
      handlerLock.unlock();
    }
  }


  public void addPostHandler(Handler h) {
    try {
      postHandlers = XynaProcess.addHandlerToArray(h, postHandlers);
    } finally {
      handlerLock.unlock();
    }
  }


  public Handler[] getPreHandlers() {
    return preHandlers;
  }


  public Handler[] getErrorHandlers() {
    return errorHandlers;
  }


  public Handler[] getPostHandlers() {
    return postHandlers;
  }


  public void removePostHandler(Handler h) {
    try {
      postHandlers = XynaProcess.removeHandlerFromArray(h, postHandlers);
    } finally {
      handlerLock.unlock();
    }
  }


  public void removeErrorHandler(Handler h) {
    try {
      errorHandlers = XynaProcess.removeHandlerFromArray(h, errorHandlers);
    } finally {
      handlerLock.unlock();
    }
  }


  public void removePreHandler(Handler h) {
    try {
      preHandlers = XynaProcess.removeHandlerFromArray(h, preHandlers);
    } finally {
      handlerLock.unlock();
    }
  }


  protected void onDeployment() throws XynaException {
  }


  protected void onUndeployment() throws XynaException {
  }


  public Scope getParentScope() {
    return null;
  }


  public void setInputVars(GeneralXynaObject o) throws XynaException {
  }

}
