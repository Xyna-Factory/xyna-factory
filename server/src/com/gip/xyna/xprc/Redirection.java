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
package com.gip.xyna.xprc;



import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess.ActiveStepType;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess.RecursionType;
import com.gip.xyna.xprc.xfractwfe.fractalworkflowexecution.fractalexecution.FractalExecutionProcessor;
import com.gip.xyna.xprc.xpce.AbstractBackupAck;
import com.gip.xyna.xprc.xpce.EngineSpecificWorkflowProcessor;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup.BackupCause;



public class Redirection extends ResponseListener implements Serializable {

  private static final long serialVersionUID = 6592076923332779062L;
  private final static Logger logger = CentralFactoryLogging.getLogger(Redirection.class);


  public static enum Answers {
    RETRY, CANCEL, IGNORE
  };


  private int failedStep;
  //there could be several SubworkflowCalls with the same ClassName, we need to check the !step.hasExecutedSuccesfull for comfirmation
  private String failedProcess;
  private String reason;
  private final boolean threadWasStoppedForcefully;
  //TODO was passiert, wenn redirectionauftrag am laufen ist, und neu gestartet wird mit der wfabstraction? für die execution braucht sie einen verweis auf die xynaorder!

  private WorkflowAbstractionLayer<RedirectionBean, RedirectionAnswer> wfAbstraction;
  private XynaOrderServerExtension parentRootOrder; //root of stuck workflowfamily TODO wird heir doppelt gespeichert: einmal reguläres OrderBackup der parentRootOrder, zum zweiten hier im MI-Auftrag


  public Redirection(int step, String process,
                     WorkflowAbstractionLayer<RedirectionBean, RedirectionAnswer> wfAbstraction, String reason,
                     boolean threadWasStoppedForcefully) {
    this.failedStep = step;
    this.failedProcess = process;
    this.reason = reason;
    this.threadWasStoppedForcefully = threadWasStoppedForcefully;
    this.wfAbstraction = wfAbstraction;
    this.singleExecutionOnly = false;
  }

  public Redirection(String reason, boolean threadWasStoppedForcefully) {
    this.failedStep = 0; //FIXME
    this.failedProcess = "test"; //FIXME
    this.reason = reason;
    this.threadWasStoppedForcefully = threadWasStoppedForcefully;
    this.singleExecutionOnly = false;

    EngineSpecificWorkflowProcessor executionProcessor = XynaFactory.getInstance().getProcessing()
        .getWorkflowEngine().getExecutionProcessor();
    FractalExecutionProcessor fep = null;
    if (executionProcessor instanceof FractalExecutionProcessor) {
      fep = (FractalExecutionProcessor)executionProcessor;
    } else {
      throw new IllegalStateException("executionProcessor is no FractalExecutionProcessor: "+executionProcessor);
    }
    this.wfAbstraction = fep.getWFAbstraction();
  }
  

  /**
   * startet den über die wfAbstraction definierten auftrag asynchron.
   * der ursprüngliche Auftrag wird erst danach fortgesetzt.
   * der redirection auftrag hat keinen parentauftrag gesetzt.
   */
  public final Long redirectOrder(XynaOrderServerExtension xo, ODSConnection con) throws PersistenceLayerException {
    parentRootOrder = xo;
    RedirectionBean in = new RedirectionBean(xo, failedStep, failedProcess, reason);

    XynaOrderServerExtension redirectionOrder = wfAbstraction.createOrder(in, parentRootOrder);

    //falls jetzt heruntergefahren wird, muss beim hochfahren nicht erneut redirected werden
    //die parentorder ist allerdings erst wieder selbst am laufen, wenn der redirectionauftrag 
    //fertig ist
    if (parentRootOrder.isSuspendedOnShutdown()) {
      parentRootOrder.setSuspended(true);
      parentRootOrder.setAttemptingSuspension(true);
      parentRootOrder.setSuspendedOnShutdown(false);
    }

    parentRootOrder.setHasBeenBackuppedAfterChange(false);
    
    RedirectionAck ack = new RedirectionAck(con);
    OrderContextServerExtension ctx = redirectionOrder.getOrderContext();
    if (ctx == null) {
      ctx = new OrderContextServerExtension(redirectionOrder);
    }
    ctx.set(OrderContextServerExtension.ACKNOWLEDGABLE_OBJECT_KEY, ack);

    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrder(redirectionOrder, this, ctx);
    ack.validate();
    return redirectionOrder.getId();
  }
  
  /*
   * fall 1: wf step wirft beim suspend einen fehler

1.1 redirectionMI -> cancel
 TODO
- fehlerzustand behalten, bzw fehler erneut werfen
  das ist nicht so einfach! man will ja auch kompensation. man müsste dazu alle steps, die einen fehler geworfen haben, erneut durchlaufen, bis auf den letzten - 
  der sollte den fehler dann wieder werfen. 
  in fractalprocessstep haben aber alle diese steps den fehler als "hab ich geworfen/gefangen" markiert (oder nicht markiert)
1.2. redirection MI -> ignore
- schritt überspringen
1.3. redirection MI -> retry
- schritt erneut ausführen

fall 2: wf step hängt weiter
2.1 redirectionMI -> cancel
- neuen fehler werfen
2.2. redirection MI -> ignore
- s.o.
2.3. redirection MI -> retry
- s.o.

   */


  protected void orderRetry() {
    //letzter processstep ist nicht fertiggelaufen, wird also jetzt wiederholt.
  }


  protected void orderCancel() {
    parentRootOrder.setCancelled(true);
  }


  protected void orderIgnore(ODSConnection defCon) throws XNWH_RetryTransactionException {
    // do nothing if we interrupted it, we gonna ignore anyway
    List<FractalProcessStep<?>> currentSteps =
        parentRootOrder.getExecutionProcessInstance().getCurrentExecutingSteps(RecursionType.INCLUDING_SUSPENDED, ActiveStepType.JAVACALL);
    for (FractalProcessStep<?> step : currentSteps) {
      step.setSuccessfullExecution(true);

      //backup the change so the resume will get it
      parentRootOrder.setHasBeenBackuppedAfterChange(false);
    }
    try {
      XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive().backup(parentRootOrder, BackupCause.SUSPENSION, defCon);
    } catch (XNWH_RetryTransactionException ctcbe) {
      throw ctcbe;
    } catch (PersistenceLayerException e) {
      // FIXME we're the ResponseListener and the order is already gone. There's nothing we can do to get to a valid checkpoint
      logger.error("Failed to reset step for retry, trying to continue execution.",e);
    }
  }


  @Override
  public void onError(XynaException[] e, OrderContext ctx) throws XNWH_RetryTransactionException {
    
    // FIXME: this treatment is neccesary during resume orders from backup. later error responses could be treated as IGNORE
    logger.error("Redirection Acknowledge returned an error!");
    
    for (XynaException xe : e) {
      logger.error(xe);
    }
  }


  @Override
  public void onResponse(GeneralXynaObject response, OrderContext ctx) throws XNWH_RetryTransactionException {
    handleResponse(wfAbstraction.createOutputData(response));
  }

  

  private void handleResponse(final RedirectionAnswer rAnswer) throws XNWH_RetryTransactionException {
    final boolean usingExternalConnection = (getDefaultConnection() != null);
    WarehouseRetryExecutableNoResult wre = new WarehouseRetryExecutableNoResult() {

      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        Answers answer = rAnswer.getAnswer();
        switch (answer) {
          case CANCEL :
            orderCancel();
            break;
          case IGNORE :
            orderIgnore(con);
            break;
          case RETRY :
            orderRetry();
            break;
          default :
            throw new RuntimeException("unsupported mi answer " + answer);
        }
        
        //ParentRootOrder nun so backuppen, dabei Redirection entfernen, da bereits ausgeführt.
        parentRootOrder.setRedirection(null);
        parentRootOrder.setHasBeenBackuppedAfterChange(false);
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
            .backup(parentRootOrder, BackupCause.SUSPENSION, con);
        
        //Fortsetzen des ursprünglichen Auftrags, beim Commit dann in den Scheduler aufnehmen
        SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
        Pair<ResumeResult, String> result = srm.resumeOrder(parentRootOrder, con);
        switch( result.getFirst() ) {
          case Resumed:
            break;
          case Unresumeable:
            srm.resumeOrderAsynchronouslyDelayed(new ResumeTarget(parentRootOrder), 0, con, true);
            break;
          case Failed:
            logger.warn("Could not resume "+parentRootOrder+": "+result );
            break;
          default:
            logger.error("Could not resume "+parentRootOrder+": "+result );
        }
        
        if( result.getFirst() != ResumeResult.Resumed ) {
          logger.warn("Could not resume "+parentRootOrder+": "+result );
        }
                
        if (!usingExternalConnection) {
          con.commit();
        }
      }
    };

    try {
      if (usingExternalConnection) {
        wre.executeAndCommit(getDefaultConnection());
      } else {
        WarehouseRetryExecutor.buildCriticalExecutor().
        connection(ODSConnectionType.DEFAULT).
        storable(OrderInstanceBackup.class).
        execute(wre);
      }
    } catch (XNWH_RetryTransactionException ctcbe) {
      if (usingExternalConnection) {
        throw ctcbe;
      } else {
        Department.handleThrowable(ctcbe);
        logger.error("Exception during execution for Redirection ResponseListener.", ctcbe);
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.error("Exception during execution for Redirection ResponseListener.", t);
    }
  }


  private static class RedirectionAck extends AbstractBackupAck {

    private static final long serialVersionUID = 1L;

    public RedirectionAck(ODSConnection con) {
      super(con);
    }


    public void backupPreFlight(XynaOrderServerExtension xose) throws PersistenceLayerException {
      xose.setHasBeenBackuppedAfterChange(false);
    }


    @Override
    protected BackupCause getBackupCause() {
      return BackupCause.ACKNOWLEDGED;
    }


    void validate() throws XNWH_RetryTransactionException {
      if (getRetryTransactionException() != null) {
        throw getRetryTransactionException();
      }
    }

  }
}
