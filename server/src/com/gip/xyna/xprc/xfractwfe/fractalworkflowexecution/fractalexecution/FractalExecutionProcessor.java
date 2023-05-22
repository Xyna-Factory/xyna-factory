/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalexecution;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.MIAbstractionLayer;
import com.gip.xyna.xprc.RedirectionAnswer;
import com.gip.xyna.xprc.RedirectionBean;
import com.gip.xyna.xprc.WorkflowAbstractionLayer;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.DispatcherType;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xfractwfe.WorkflowInstancePool;
import com.gip.xyna.xprc.xfractwfe.base.AFractalWorkflowProcessor;
import com.gip.xyna.xprc.xfractwfe.base.EngineSpecificProcess;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess.XynaProcessState;
import com.gip.xyna.xprc.xpce.InterruptableExecutionProcessor;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspensionBackupMode;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.*;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;



public class FractalExecutionProcessor extends AFractalWorkflowProcessor implements InterruptableExecutionProcessor {

  public static final String DEFAULT_NAME = "Fractal Execution Processor";
  static {
    addDependencies(FractalExecutionProcessor.class, new ArrayList<XynaFactoryPath>(Arrays
                    .asList(new XynaFactoryPath[] {
                                    new XynaFactoryPath(XynaProcessing.class, XynaProcessCtrlExecution.class),
                                    new XynaFactoryPath(XynaProcessing.class, XynaScheduler.class)}
                    )));
  }
  
  private WorkflowAbstractionLayer<RedirectionBean, RedirectionAnswer> miAbstractionLayer;

  private final HashMap<Long, ProcessAndDestinationValueBean> runningProcesses;
  private final ReentrantLock processLock;

  // TODO use a synchronized on an object instead of a ReentrantLock so that stopAcceptingNewOrders does not
  //      have to be volatile any longer
  private volatile boolean stopAcceptingNewOrders = false;


  private static class ProcessAndDestinationValueBean {
    public final XynaOrderServerExtension xo;
    public final DestinationValue dv;
    public ProcessAndDestinationValueBean(XynaOrderServerExtension xo, DestinationValue dv) {
      this.xo = xo;
      this.dv = dv;
    }
  }


  public FractalExecutionProcessor() throws XynaException {
    super(DispatcherType.Execution);
    runningProcesses = new HashMap<Long, ProcessAndDestinationValueBean>();
    processLock = new ReentrantLock();
  }

  public static class WorkflowThreadDeath extends ThreadDeath {

    private static final long serialVersionUID = 1L;
    
  }


  public XynaProcess processInternally(DestinationValue dv, XynaOrderServerExtension xo) throws XynaException {
    final XynaProcess p;
    if (xo.getExecutionProcessInstance() != null) {
      //resume oder compensate
      p = xo.getExecutionProcessInstance();
    } else {
      RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      p = getFractalWorkflowEngine().getProcessManager().getProcess(dv, revMgmt.getRevision(xo.getDestinationKey().getRuntimeContext()));
      xo.setExecutionProcessInstance(p);
    }

    final Long targetOrderId = xo.getId();
    processLock.lock();
    try {
      if (stopAcceptingNewOrders) {
        if (logger.isDebugEnabled()) {
          logger.debug("Tried to process execution order " + xo.getId() + " after shutdown attempt, suspending order manually");
        }
        SuspensionCause cause = new SuspensionCause_ShutDown(true, xo.getId());
        cause.setSuspensionOrderBackupMode(SuspensionBackupMode.BACKUP);
        throw new ProcessSuspendedException(cause); //Keine LaneId!
      }
      runningProcesses.put(targetOrderId, new ProcessAndDestinationValueBean(xo, dv));
    } finally {
      processLock.unlock();
    }
    
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
    boolean td = false;
    try {
      if (xo.getDestinationKey().isCompensate()) {

        if (stopAcceptingNewOrders) {
          if (logger.isDebugEnabled()) {
            logger.debug("Tried to process execution order " + xo.getId() + " after shutdown attempt, suspending order manually");
          }
          throw new ProcessSuspendedException(new SuspensionCause_Manual(true, xo.getId()));
        }
        xo.getExecutionProcessInstance().compensate();
        return null;
      } else {
        final boolean hasWorkflowExecutionTimeout = xo.getWorkflowExecutionTimeout() != null;

        if (hasWorkflowExecutionTimeout) {
          ((XynaOrderServerExtension) xo).calculateExecutionTimeoutFromWorkflowTimeout();
        }

        if (xo.getOrderExecutionTimeout() != null) {
          //falls notwendig, sofort abbrechen, ansonsten asynchron
          if (xo.getOrderExecutionTimeout().getRelativeTimeoutForNowIn(TimeUnit.MILLISECONDS) <= 0) {
            p.setAbortionException(new ProcessAbortedException(xo.getId(), AbortionCause.TIME_TO_LIVE_EXPIRATION));
            p.setState(XynaProcessState.ABORTING);
          } else {
            XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getOrderExecutionTimeoutManagement()
                .registerOrderTimeout(xo);
          }
        }

        GeneralXynaObject response = p.execute(xo.getInputPayload(), xo);
        xo.setOutputPayload(response);
        return p;
      }
    } catch (WorkflowThreadDeath e) {
      //kein finally ausf�hren - das hat bereits der ersatzthread gemacht
      td = true;
      throw e;
    } finally {
      DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
      if (!td) {
        // TODO what about exceptions during the following code passage?
        processLock.lock();
        try {
          runningProcesses.remove(targetOrderId);
        } finally {
          processLock.unlock();
        }
      }
    }
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void init() throws XynaException {
    miAbstractionLayer = new MIAbstractionLayer();
  }


  public void shutdown() throws XynaException {
  }

  public WorkflowAbstractionLayer<RedirectionBean, RedirectionAnswer> getWFAbstraction() {
    return miAbstractionLayer;
  }


  public void stopAcceptingNewOrders() {
    stopAcceptingNewOrders = false;
  }


  public int getNumberOfRunningProcesses() {
    processLock.lock();
    try {
      return runningProcesses.size();
    } finally {
      processLock.unlock();
    }
  }


  public EngineSpecificProcess getRunningProcessById(long orderId) {
    processLock.lock();
    try {
      ProcessAndDestinationValueBean bean = runningProcesses.get(orderId);
      if (bean != null) {
        // make sure the instance wont be reused since it is not clear what happens to the object
        // outside of this class
        bean.dv.setPoolId(WorkflowInstancePool.ID_DONT_REUSE_INSTANCE);
        return bean.xo.getExecutionProcessInstance();
      } else {
        return null;
      }
    } finally {
      processLock.unlock();
    }
  }


  @Override
  public Collection<XynaOrderServerExtension> getOrdersOfRunningProcesses() {
    processLock.lock();
    try {
      Collection<XynaOrderServerExtension> orders = new ArrayList<XynaOrderServerExtension>();
      for (ProcessAndDestinationValueBean runningProcess : runningProcesses.values()) {
        orders.add(runningProcess.xo);
      }
      return orders;
    } finally {
      processLock.unlock();
    }
  }  


  public List<XynaOrderServerExtension> getRunningProcessesFor(String ordertypeToSuspend, Long revision) {
    List<XynaOrderServerExtension> relevantOrders = new ArrayList<XynaOrderServerExtension>();
    DestinationKey dk;
    try {
      dk = new DestinationKey(ordertypeToSuspend, XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
          .getRevisionManagement().getRuntimeContext(revision));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    processLock.lock();
    try {
      for (ProcessAndDestinationValueBean processBean : runningProcesses.values()) {
        if (processBean == null) {
          continue;
        }
        XynaOrderServerExtension xo = processBean.xo;
        if (xo.getDestinationKey().equals(dk)) {
          if (logger.isTraceEnabled()) {
            logger.trace("Found an affected order: " + xo.getId() + " - "
                + xo.getDestinationKey().getOrderType());
          }
          if (!xo.hasParentOrder()) { //don't suspend child orders
            logger.trace("order has no parent, going to suspend it");
            relevantOrders.add(xo);
          } else {
            logger.trace("order has a parent, not going to suspend it");
          }
        }
      }
    } finally {
      processLock.unlock();
    }
    return relevantOrders;
  }


  public Collection<? extends XynaOrderServerExtension> getRunningProcessesFrom(Set<Long> revisions) {
    List<XynaOrderServerExtension> relevantOrders = new ArrayList<XynaOrderServerExtension>();
    
    processLock.lock();
    try {
      for (ProcessAndDestinationValueBean processBean : runningProcesses.values()) {
        if (processBean == null) {
          continue;
        }
        XynaOrderServerExtension xo = processBean.xo;
        if (revisions.contains(xo.getRevision())) {
          if (logger.isTraceEnabled()) {
            logger.trace("Found an affected order: " + xo.getId() + " - "
                + xo.getDestinationKey());
          }
          if (!xo.hasParentOrder()) { //don't suspend child orders
            logger.trace("order has no parent, going to suspend it");
            relevantOrders.add(xo);
          } else {
            logger.trace("order has a parent, not going to suspend it");
          }
        }
      }
    } finally {
      processLock.unlock();
    }
    return relevantOrders;
  }

}
