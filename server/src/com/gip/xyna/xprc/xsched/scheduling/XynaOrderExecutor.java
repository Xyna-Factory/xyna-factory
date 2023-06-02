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
package com.gip.xyna.xprc.xsched.scheduling;

import java.util.concurrent.RejectedExecutionException;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xods.priority.PriorityManagement;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ProcessingStage;
import com.gip.xyna.xprc.XynaRunnable;
import com.gip.xyna.xprc.exceptions.XPRC_PROCESS_CANCELLED;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.BatchProcessRestarter;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler;
import com.gip.xyna.xprc.xpce.execution.MasterWorkflowPostScheduler.Mode;


/**
 * XynaOrderExecutor stellt {@link com.gip.xyna.xprc.XynaRunnable XynaRunnables} in den 
 * {@link com.gip.xyna.xprc.XynaExecutor XynaExecutor} ein, um damit die folgenden 
 * Aufgaben zu erledigen:
 * <ul>
 * <li> {@link #startOrder} </li>
 * <li> {@link #terminateOrder} </li>
 * <li> {@link #cancelOrder} </li>
 * </ul>
 * <br>
 * Die Rückgabe dieser Methoden sollte meist <code>true</code> sein. <code>false</code> bedeutet, 
 * dass der ThreadPool im XynaExecutor überlastet ist und der Aufruf daher nochmal (nach einer Pause)
 * wiederholt werden muss. Dabei wird zuerst versucht, ThreadPools mit niedrigeren Prioritäten zu 
 * verwenden.
 * 
 * <br>
 * Derzeit verwendet von {@link com.gip.xyna.xprc.xsched.scheduling.TryScheduleAbstract TryScheduleAbstract} 
 * und {@link com.gip.xyna.xprc.xsched.OrderSeriesManagement OrderSeriesManagement}.
 */
public class XynaOrderExecutor {

  
  /**
   * Start der XynaOrder: Ausführung des MasterWorkflowPostScheduler im XynaExecutor
   * Achtung: dieses direkte Starten sollte nur durch den Scheduler aufgerufen werden
   * @param schedulingOrder
   * @return false, wenn Ausführung nicht möglich war und der Aufruf wiederholt werden muss
   */
  public static boolean startOrder(SchedulingOrder schedulingOrder) {
    CentralFactoryLogging.logOrderTiming(schedulingOrder.getOrderId(), "start execution thread");
    return tryExecuteXynaRunnable( new MasterWorkflowPostScheduler(schedulingOrder, Mode.Normal),
                                   schedulingOrder.getSchedulingData().getPriority(), false );
  }
  
  /**
   * Start der XynaOrder: Ausführung des MasterWorkflowPostScheduler im XynaExecutor
   * Achtung: dieses direkte Starten sollte nur durch den Scheduler aufgerufen werden
   * @param xynaOrder
   * @param priority
   * @return false, wenn Ausführung nicht möglich war und der Aufruf wiederholt werden muss
   */
  public static boolean startOrder(XynaOrderServerExtension xynaOrder, int priority) {
    CentralFactoryLogging.logOrderTiming(xynaOrder.getId(), "start execution thread");
    return tryExecuteXynaRunnable( new MasterWorkflowPostScheduler(xynaOrder), PriorityManagement.restrictPriorityToThreadPriorityBounds(priority), false );
  }
  
  
  /**
   * Startet einen BatchProcess neu
   * @param batchProcessRestarter
   */
  public static void restartBatchProcess( BatchProcessRestarter batchProcessRestarter) {
    tryExecuteXynaRunnable(batchProcessRestarter, batchProcessRestarter.getPriority(), false );
  }
  

  public static boolean terminateOrder(SchedulingOrder so) {
    return tryExecuteXynaRunnable( new MasterWorkflowPostScheduler(so, Mode.Terminate), 
                                   so.getSchedulingData().getPriority(), true );
  }


  /**
   * Abbruch der XynaOrder durch Cancel
   * @return false, wenn Ausführung nicht möglich war und der Aufruf wiederholt werden muss
   */
  public static boolean cancelOrder(XynaOrderServerExtension xynaOrder) {
    return cancelOrder(xynaOrder, false);
  }
  

  /**
   * Abbruch der XynaOrder durch Cancel
   * @return false, wenn Ausführung nicht möglich war und der Aufruf wiederholt werden muss
   */
  public static boolean cancelOrder(XynaOrderServerExtension xo, boolean ignoreCapacitiesAndVetos) {
    xo.setCancelled(true);
    xo.addException(new XPRC_PROCESS_CANCELLED(xo.getId()), ProcessingStage.OTHER);
    if (xo.mustDeploymentCounterBeCountDown()) {
      //das pendant zu XynaProcessCtrlExecution.startOrder
      DeploymentManagement.getInstance().countDownOrderThatKnowsAboutDeployment(xo.getIdOfLatestDeploymentFromOrder());
      xo.setDeploymentCounterCountDownDone();
    }
    return tryExecuteXynaRunnable(new MasterWorkflowPostScheduler(
                                                                  xo,
                                                                  ignoreCapacitiesAndVetos ? Mode.TerminateAndIgnoreCapacitiesAndVetos : Mode.Terminate),
                                  xo.getSchedulingData().getPriority(), true);
  }

  
  /**
   * Einstellen des XynaRunnable mit der Priorität <code>prio</code> in den XynaExecutor
   * Falls der ThreadPool des XynaExecutors zu der angegebenen Priorität belegt ist, 
   * wird versucht, den ThreadPool zur nächstniedrigeren Priorität zu belegen.
   * @param xr
   * @param prio
   * @return false, wenn Ausführung nicht möglich war
   */
  private static boolean tryExecuteXynaRunnable(XynaRunnable xr, int prio, boolean withCleanupThreadPool) {
    try {
      if (withCleanupThreadPool) {
        XynaExecutor.getInstance().executeRunnableWithCleanupThreadpool( xr );
      } else {
        XynaExecutor.getInstance().executeRunnableWithExecutionThreadpool( xr, prio );
      }
      return true;
    } catch( RejectedExecutionException e ) {
      if( prio > Thread.MIN_PRIORITY && !withCleanupThreadPool) {
        return tryExecuteXynaRunnable( xr, prio -1, false );
      } else {
        return false;
      }
    }
  }

  
}
