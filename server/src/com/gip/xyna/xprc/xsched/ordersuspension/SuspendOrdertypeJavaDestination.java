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
package com.gip.xyna.xprc.xsched.ordersuspension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_UNSUPPORTED_FEATURE;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_INPUT_PARAMETER_TYPE;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalexecution.FractalExecutionProcessor;
import com.gip.xyna.xprc.xpce.EngineSpecificWorkflowProcessor;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionFailedAction;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionSucceededAction;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionTimedOutAction;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


public class SuspendOrdertypeJavaDestination extends JavaDestination {

  private static final long serialVersionUID = -1735590502272607246L;
  private static Logger logger = CentralFactoryLogging.getLogger(SuspendOrdertypeJavaDestination.class);
  
  public static final String SUSPEND_ORDERTYPE_DESTINATION = "com.gip.xyna.SuspendOrdertype";

  public SuspendOrdertypeJavaDestination() {
    super(SUSPEND_ORDERTYPE_DESTINATION);
  }

  @Override
  public GeneralXynaObject exec(XynaOrderServerExtension xose, GeneralXynaObject input) throws XynaException {
    if (!(input instanceof SuspendOrdertypeBean)) {
      throw new XPRC_INVALID_INPUT_PARAMETER_TYPE("1", SuspendOrdertypeBean.class.getName(),
                                                  input.getClass().getName());
    }

    final SuspendOrdertypeBean bean = (SuspendOrdertypeBean) input;

    EngineSpecificWorkflowProcessor executionProcessor = XynaFactory.getInstance().getProcessing()
                    .getWorkflowEngine().getExecutionProcessor();

    FractalExecutionProcessor fep = null;
    
    if (executionProcessor instanceof FractalExecutionProcessor) {
      fep = (FractalExecutionProcessor)executionProcessor;
    } else {
      throw new XDEV_UNSUPPORTED_FEATURE("Suspending orders using the workflow engine " + 
          XynaFactory.getInstance().getProcessing().getWorkflowEngine().getClass().getSimpleName());
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("receiving suspendordertype for ordertype: " + bean.getOrdertypeToSuspend() + " forcingInterrupt: "
          + bean.isInterruptingStuckOrders());
    }
    
    List<XynaOrderServerExtension> relevantOrders = fep.getRunningProcessesFor( bean.getOrdertypeToSuspend(), bean.getRevision() );
    
    suspend( bean, relevantOrders );
     
    //TODO nochmal kontrollieren, dass fep.getRunningProcessesFor( bean.getOrdertypeToSuspend(), bean.getRevision() ); nun leer ist?
    
    return bean;
  }

  private void suspend(SuspendOrdertypeBean bean, List<XynaOrderServerExtension> relevantOrders ) {
    SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
    Map<Long,XynaOrderServerExtension> suspendOrders = new HashMap<Long,XynaOrderServerExtension>();
    
    for( XynaOrderServerExtension xo : relevantOrders ) { 
      suspendOrders.put( xo.getRootOrder().getId(), xo.getRootOrder());
    }
    logger.info("trying to suspend following rootOrders: "+ suspendOrders.keySet() );
    SuspendRootOrderData suspendRootOrderData = 
        new SuspendRootOrderData(suspendOrders.keySet())
    .suspensionCause(new SuspendOrdertypeSuspensionCause() )
    .suspensionSuccededAction(bean.isKeepUnresumeable() ? SuspensionSucceededAction.KeepUnresumeable : SuspensionSucceededAction.None)
    .suspensionTimedOutAction(bean.isInterruptingStuckOrders() ? SuspensionTimedOutAction.Interrupt : SuspensionTimedOutAction.None)
    .suspensionFailedAction(SuspensionFailedAction.UndoSuspensions);
     
    SuspendRootOrderData result = srm.suspendRootOrders(suspendRootOrderData);

    bean.addResumeTargets( result.getResumeTargets() );
    switch( result.getSuspensionResult() ) {
      case Suspended:
        logger.info("all orders are suspended: "+ result.getResumeTargets().size()+" resumetargets" );
        bean.setSuccess(true);
        bean.addSuspendedRootOrderIds(suspendOrders.keySet());
        break;
      case Timeout:
        logger.warn("suspendRootOrders has timeout" ); //wegen Undo sind alle Aufträge betroffen
        bean.setSuccess(false);
        break;
      case Failed:
        logger.warn("suspendRootOrders failed for following rootOrderIds: "+result.getFailedSuspensions().keySet() 
                    +" and could not resume "+result.getResumeTargets().size()+" targets" );
        //TODO ResumeTargets ausgeben?
        bean.addSuspendedRootOrderIds(result.getFailedRootOrderIds());
        bean.setSuccess(false);
        break;
      default:
        logger.warn("Unexpected SuspensionResult "+result);
        bean.setSuccess(false);
    }
  }

  public static class SuspendOrdertypeSuspensionCause extends SuspensionCause {
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
      return "SUSPEND_ORDERTYPE";
    }
  }

}
