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
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_UNSUPPORTED_FEATURE;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_INPUT_PARAMETER_TYPE;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xpce.EngineSpecificWorkflowProcessor;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionFailedAction;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionSucceededAction;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionTimedOutAction;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Manual;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_ShutDown;



public class SuspendAllOrdersJavaDestination extends JavaDestination {

  private static final long serialVersionUID = -1735590502272607246L;
  private static Logger logger = CentralFactoryLogging.getLogger(SuspendAllOrdersJavaDestination.class);

  public static final String SUSPEND_ALL_DESTINATION = "com.gip.xyna.SuspendAllOrders";


  public SuspendAllOrdersJavaDestination() {
    super(SUSPEND_ALL_DESTINATION);
  }


  @Override
  public GeneralXynaObject exec(XynaOrderServerExtension xose, final GeneralXynaObject input) throws XynaException {

    if (!(input instanceof SuspendAllOrdersBean)) {
      throw new XPRC_INVALID_INPUT_PARAMETER_TYPE("1", SuspendAllOrdersBean.class.getName(), input.getClass().getName());
    }

    SuspendAllOrdersBean bean = (SuspendAllOrdersBean) input;

    Pair<Integer, Integer> pair = suspendAll(bean.isSuspendForShutdown(), xose.getIdOfLatestDeploymentFromOrder());
    if( pair.getFirst() == 0 ) {
      bean.setRequestSucceeded(true);
    } else {
      bean.setRequestSucceeded(false);
      logger.warn( "SuspendAll did not succeed: "+pair.getFirst()+" of "+pair.getSecond()+" orders are not suspended");
      if( bean.isSuspendForShutdown() ) {
        logger.info( "retrying to suspend all...");
        Pair<Integer, Integer> retry = suspendAll(bean.isSuspendForShutdown(), xose.getIdOfLatestDeploymentFromOrder() );
        if( retry.getFirst() == 0 ) {
          logger.info( "retry to suspendAll: suspend of "+retry.getSecond()+" orders succeeded" );
          bean.setRequestSucceeded(true);
        } else {
          logger.warn( "retry to suspendAll did not succeed: "+pair.getFirst()+" of "+pair.getSecond()+" orders are not suspended");
          bean.setRequestSucceeded(false);
          //TODO weitere Retry?
        }
      }
    }
    return bean;
  }
  
  private Pair<Integer,Integer> suspendAll(boolean suspendForShutdown, long deploymentId) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XDEV_UNSUPPORTED_FEATURE {
    EngineSpecificWorkflowProcessor executionProcessor =
        XynaFactory.getInstance().getProcessing().getWorkflowEngine().getExecutionProcessor();
    
    //warten bis Aufträge sicher den Scheduler verlassen haben und im ExecutionProcessor angekommen sind
    waitForUnreachableOrders(deploymentId); 

    SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
    Map<Long,XynaOrderServerExtension> suspendOrders = new HashMap<Long,XynaOrderServerExtension>();
    
    for( XynaOrderServerExtension xo : executionProcessor.getOrdersOfRunningProcesses() ) {
      if (suspendForShutdown) {
        //set a flag for it
        xo.setSuspendedOnShutdown(true); //FIXME wofür?
      }
      suspendOrders.put( xo.getRootOrder().getId(), xo.getRootOrder());
    }
    logger.info("trying to suspend following rootOrders: "+ suspendOrders.keySet() );

    SuspendRootOrderData suspendRootOrderData = new SuspendRootOrderData(suspendOrders.keySet());
    if( suspendForShutdown ) {
      suspendRootOrderData.suspensionCause(new SuspensionCause_ShutDown())
      .suspensionSuccededAction(SuspensionSucceededAction.KeepUnresumeable)
      .suspensionTimedOutAction(SuspensionTimedOutAction.Stop)
      .suspensionFailedAction(SuspensionFailedAction.KeepSuspending);
    } else {
      //TODO wie könnte das gebraucht werden?
      suspendRootOrderData.suspensionCause(new SuspensionCause_Manual())
      .suspensionSuccededAction(SuspensionSucceededAction.None)
      .suspensionTimedOutAction(SuspensionTimedOutAction.None)
      .suspensionFailedAction(SuspensionFailedAction.UndoSuspensions);     
    }
     
    SuspendRootOrderData result = srm.suspendRootOrders(suspendRootOrderData);
    switch( result.getSuspensionResult() ) {
      case Suspended:
        logger.info("all orders are suspended: "+ result.getResumeTargets().size()+" resumetargets" );
        break;
      case Timeout:
        logger.warn("suspendRootOrders has timeout" ); 
        break;
      case Failed:
        logger.warn("suspendRootOrders failed for following rootOrderIds: "+result.getFailedSuspensions().keySet() 
                    +" and could not resume "+result.getResumeTargets().size()+" targets" );
        break;        
      default:
        logger.warn("Unexpected SuspensionResult "+result);
        return Pair.of(suspendOrders.size(), suspendOrders.size());
    }
    
    if( suspendForShutdown ) {
      try {
        srm.shutdown();
      } catch (XynaException e) {
        logger.error("Error during shutdown of " + SuspendResumeManagement.class.getName(), e);
      }
    }
    
    return Pair.of(result.getFailedSuspensions().size(), suspendOrders.size());
  }


  private void waitForUnreachableOrders(long deploymentId) {
    //Unreachable Orders sind hier eigentlich nur die Aufträge, die den Scheduler verlassen haben,
    //aber noch nicht im ExecutionProcessor angekommen sind. Diese Aufträge würden nicht entdeckt 
    //werden und daher während ihrer Ausführung abgebrochen werden, ihr OrderBackup aber wäre noch 
    //auf dem vorigen Stand. Diese Aufträge würden dann nach dem Start der Factroy oder nach der 
    //OrderMigration teilweise doppelt ausgeführt werden.  
    //Hier wird nun der Auftragszähler aus dem DeploymentManagement dafür zweckentfremdet.
    //Dieser Auftragszähler zählt auch noch Aufträge an anderen Stellen mit, dies sollte jedoch
    //eher positive Auswirkungen haben: So werden z.B. JavaDestinations noch komplett ausgeführt.
    
    //eigener Auftrag SuspendAllOrders wird mitgezählt, dieser soll jetzt jedoch ignoriert werden
    DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(deploymentId);
    try {
      
      //DeploymentId erhöhen, damit waitForUnreachableOrders() bisherige Aufträge als alt ansieht und darauf wartet
      DeploymentManagement.getInstance().propagateDeployment(); //TODO anderes waitForUnreachableOrders
      try {
        //auf alle versteckten Aufträge warten
        DeploymentManagement.getInstance().waitForUnreachableOrders();
        if( logger.isDebugEnabled() ) {
          logger.debug( DeploymentManagement.getInstance().getOrderAndDeploymentCounterState("waitForUnreachableOrders") );
        }
      } catch (Exception e ) { //XPRC_TimeoutWhileWaitingForUnaccessibleOrderException, RuntimeException wegen Interrupt
        logger.warn( "Waiting for unreachable Orders failed, some running orders may not be suspended correctly", e);
        logger.warn( DeploymentManagement.getInstance().getOrderAndDeploymentCounterState("waitForUnreachableOrders") );
      }
    } finally {
      //eigenen Auftrag SuspendAllOrders wieder mitzählen
      DeploymentManagement.getInstance().countOrderThatKnowsAboutDeployment(deploymentId);
    }
  }
  
}
