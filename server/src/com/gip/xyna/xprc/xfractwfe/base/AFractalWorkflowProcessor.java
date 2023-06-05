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

package com.gip.xyna.xprc.xfractwfe.base;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_UNEXPECTED_ERROR_PROCESS;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement.DispatcherType;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalexecution.FractalExecutionProcessor;
import com.gip.xyna.xprc.xpce.EngineSpecificWorkflowProcessor;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;


public abstract class AFractalWorkflowProcessor extends EngineSpecificWorkflowProcessor {

  private volatile boolean initialized = false;

  private static volatile AFractalWorkflowProcessorProcessingCheckAlgorithm processingCheckAlgorithm = new DefaultAFractalWorkflowProcessorProcessingCheckAlgorithm();

  private XynaFractalWorkflowEngine xynaFractalWorkflowEngine = null;
  private final DispatcherType dispatcherType;

  public AFractalWorkflowProcessor(DispatcherType dispatcherType) throws XynaException {
    super();
    this.dispatcherType = dispatcherType;
  }


  private synchronized void initialize() {
    if (initialized) {
      return;
    }

    WorkflowEngine wfe = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
    if (wfe instanceof XynaFractalWorkflowEngine) {
      xynaFractalWorkflowEngine = (XynaFractalWorkflowEngine) wfe;
    } else {
      throw new IllegalStateException("Fractal Workflow Engine could not be found");
    }

    initialized = true;
  }


  public void process(DestinationValue dv, XynaOrderServerExtension xo) throws XynaException {

    if (!initialized) {
      synchronized (this) {
        if (!initialized) {
          try {
            initialize();
          } catch (IllegalStateException e) {
            throw new XPRC_UNEXPECTED_ERROR_PROCESS(xo.getDestinationKey().getOrderType(), "Could not find "
                            + XynaFractalWorkflowEngine.DEFAULT_NAME);
          }
        } else {
          if (xynaFractalWorkflowEngine == null) {
            throw new XPRC_UNEXPECTED_ERROR_PROCESS(xo.getDestinationKey().getOrderType(), "Could not find "
                            + XynaFractalWorkflowEngine.DEFAULT_NAME);
          }
        }
      }
    }

    processingCheckAlgorithm.checkOrderReadyForProcessing(xo, dispatcherType);

    XynaProcess instance = null;
    boolean caughtSuspension = false;
    try {
      instance = processInternally(dv, xo);
    } catch (ProcessSuspendedException e) {
      // actually this flag is not really required since in this case instance is null, but things are clearer this way
      // TODO we might want to return the instances to the pool here, however, since once they are serialized, the instance can be reused.
      // when does this serialization happen? we may not return the instance before serialization because in that case the instance might be
      // reused before serialization has finished and then bogus information will be written for the suspended order
      caughtSuspension = true;
      throw e;
    } catch (ThreadDeath t) {
      //wf-instanz nicht zum pool zurückgeben!
      throw t;
    } catch (XynaException e) {
      cleanup(caughtSuspension, instance, xo, dv);
      throw e;
    } catch (RuntimeException e) {
      cleanup(caughtSuspension, instance, xo, dv);
      throw e;
    } catch (Error t) {
      Department.handleThrowable(t);
      cleanup(caughtSuspension, instance, xo, dv);
      throw t;
    }

  }


  private void cleanup(boolean caughtSuspension, XynaProcess instance, XynaOrderServerExtension xo, DestinationValue dv) {
    // in the case of suspension the instance may not be returned! (see todo above)
    if (!caughtSuspension && instance != null) {
      if (!xo.getDestinationKey().isCompensate()) {
      
        //rootauftrag räumt für alle kinder mit auf.
        if (!xo.hasParentOrder()) {
          cleanupExecutionInstancesRecursively(xo, instance, dv);
        }
      }
    }
  }


  /**
   * Removes all references to used instances of XynaProcess and afterwards returns them to the ProcessManager
   */
  private void cleanupExecutionInstancesRecursively(XynaOrderServerExtension xo, XynaProcess instance,
                                                    DestinationValue dv) {

    if (this instanceof FractalExecutionProcessor) {
      List<XynaOrderServerExtension> list = instance.getAllChildOrdersRecursively();
      HashMap<DestinationValue, XynaProcess> instances = new HashMap<DestinationValue, XynaProcess>();
      for (XynaOrderServerExtension xoChild : list) {
        if (xoChild.getExecutionProcessInstance() != null) {
          instances.put(xoChild.getExecutionDestination(), xoChild.getExecutionProcessInstance());
          xoChild.setExecutionProcessInstance(null);
        } else {
          // sollte nicht passieren. diese methode wird nur ausgeführt, wenn root fertig ist. dann müssen auch alle kinder fertig sein.
          logger.warn("Instance of subworkflowcall (ID " + xoChild.getId() + ", order type "
                          + xoChild.getDestinationKey().getOrderType() + ") was null, has not been scheduled?");
        }
      }
      instances.put(dv, instance);
      xo.setExecutionProcessInstance(null);
      for (Entry<DestinationValue, XynaProcess> e : instances.entrySet()) {
        xynaFractalWorkflowEngine.getProcessManager().returnProcessInstances(e.getKey(), e.getValue());
      }
    } else {
      //TODO erweitern, wenn planning und cleanup eingebaut werden
      xynaFractalWorkflowEngine.getProcessManager().returnProcessInstances(dv, instance);
    }

  }


  public abstract XynaProcess processInternally(DestinationValue dv, XynaOrderServerExtension xo) throws XynaException;

 
  public static synchronized void setAlgorithm(AFractalWorkflowProcessorProcessingCheckAlgorithm newAlgorithm) {
    processingCheckAlgorithm = newAlgorithm;
  }
  
  public static synchronized AFractalWorkflowProcessorProcessingCheckAlgorithm getAlgorithm() {
    return processingCheckAlgorithm;
  }

}
