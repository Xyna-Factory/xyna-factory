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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_PLANNING_RESPONSETYPE;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.DispatcherType;
import com.gip.xyna.xprc.xfractwfe.base.AFractalWorkflowProcessor;
import com.gip.xyna.xprc.xfractwfe.base.EngineSpecificProcess;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xsched.SchedulerBean;


public class FractalPlanningProcessor extends AFractalWorkflowProcessor {

  public static final String DEFAULT_NAME = "Fractal Planning Executor";
  private final ConcurrentHashMap<Long, Pair<XynaOrderServerExtension, XynaProcess>> runningProcesses
            = new ConcurrentHashMap<Long, Pair<XynaOrderServerExtension, XynaProcess>>();


  public FractalPlanningProcessor() throws XynaException {
    super(DispatcherType.Planning);
  }

  public XynaProcess processInternally(DestinationValue dv, XynaOrderServerExtension xo) throws XynaException {

    RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    XynaProcess p = getFractalWorkflowEngine().getProcessManager().getProcess(dv, revMgmt.getRevision(xo.getDestinationKey().getRuntimeContext()));

    final Long targetId = xo.getId();
    runningProcesses.put(targetId, Pair.of(xo, p));
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
    try {
      
      GeneralXynaObject input = p.getNeededInputVarsCount() == 0 ? new Container() : xo.getInputPayload();
      GeneralXynaObject output = p.execute(input, xo);
      if (output instanceof SchedulerBean) {
        SchedulerBean sb = (SchedulerBean) output;
        xo.setSchedulerBean(sb);
      } else {
        throw new XPRC_INVALID_PLANNING_RESPONSETYPE(Long.toString(xo.getId()));
      }
    } finally {
      DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
      runningProcesses.remove(targetId);
    }

    return p;

  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void init() throws XynaException {
  }


  public void shutdown() throws XynaException {
  }


  @Override
  public EngineSpecificProcess getRunningProcessById(long orderId) {
    Pair<XynaOrderServerExtension, XynaProcess> p = runningProcesses.get(orderId);
    if (p == null) {
      return null;
    }
    return p.getSecond();
  }


  @Override
  public int getNumberOfRunningProcesses() {
    return runningProcesses.size();
  }
  
  
  @Override
  public Collection<XynaOrderServerExtension> getOrdersOfRunningProcesses() {
    Collection<XynaOrderServerExtension> orders = new ArrayList<XynaOrderServerExtension>();
    for (Pair<XynaOrderServerExtension, XynaProcess> p : runningProcesses.values()) {
      orders.add(p.getFirst());
    }
    return orders;
  }

}
