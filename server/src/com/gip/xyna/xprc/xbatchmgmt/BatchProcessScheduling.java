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
package com.gip.xyna.xprc.xbatchmgmt;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.concurrent.AtomicEnum;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.parallel.ParallelExecutor;
import com.gip.xyna.utils.parallel.SimpleXynaRunnableTaskConsumerPreparator;
import com.gip.xyna.utils.timing.ExecutionPeriod.Type;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaExecutor;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaExecutor.ExecutionThreadPoolExecutorWithDecreasingPrio;
import com.gip.xyna.xprc.exceptions.XPRC_DuplicateTimeWindowNameException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowNotFoundInDatabaseException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowRemoteManagementException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowStillUsedException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.CancelMode;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement.MissingLimitationReaction;
import com.gip.xyna.xprc.xbatchmgmt.beans.SlaveExecutionPeriod;
import com.gip.xyna.xprc.xpce.planning.Capacity;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.AllOrdersList;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.capacities.MultiAllocationCapacities;
import com.gip.xyna.xprc.xsched.capacities.TransferCapacities;
import com.gip.xyna.xprc.xsched.scheduling.TrySchedule.TryScheduleResult;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder;
import com.gip.xyna.xprc.xsched.scheduling.SchedulingOrder.WaitingCause;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Start;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Window;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeWindowDefinition;


public class BatchProcessScheduling {

  private static Logger logger = CentralFactoryLogging.getLogger(BatchProcessScheduling.class);
  
  private BatchProcess batchProcess;
  private AtomicEnum<SchedulingState> schedulingState;
  private SchedulingOrder masterSchedulingOrder;
  private XynaOrderServerExtension masterOrder; //Master-Auftrag
  private SchedulingData masterSchedulingData;
  private ReentrantLock transferCapsLock = new ReentrantLock();
  private SchedulingData slaveSchedulingData; //SchedulingData. mit denen der Master die Capacities für die Slaves besorgt
  private SlaveOrderTypeInfo slaveOrderTypeInfo;
  private AtomicBoolean refreshSlaveSchedulingData = new AtomicBoolean(false); //true, wenn die Slaves mit neuen SchedulingData gescheduled werden sollen
  private String timeWindowName;
  private SlaveExecutionPeriod slaveExecutionPeriod;
  private ParallelExecutor parallelExecutor;
 
  public BatchProcessScheduling(BatchProcess batchProcess, XynaOrderServerExtension masterOrder) {
    this.batchProcess = batchProcess;
    this.schedulingState = new AtomicEnum<SchedulingState>(SchedulingState.class, SchedulingState.Init);
    setMasterOrder(masterOrder);
  }
  
  public BatchProcessScheduling(BatchProcess batchProcess, SchedulingOrder masterSchedulingOrder, XynaOrderServerExtension masterOrder) {
    this.batchProcess = batchProcess;
    this.schedulingState = new AtomicEnum<SchedulingState>(SchedulingState.class, SchedulingState.ReInit);
    setMasterSchedulingOrder(masterSchedulingOrder);
  }

  
  public enum SchedulingState {
    /**
     * Der Batch Process wurde neu angelegt.
     * In {@link BatchProcessScheduling#acknowledgeMasterBackup(boolean)} wird dann der Status 
     * auf ScheduleMaster oder Remove gewechselt.
     */
   Init,
    
    /**
     * Der Batch Process wird nach einem Neustart reinitialisiert.
     * In {@link BatchProcessScheduling#restart(SchedulingData)} wird dann der Status 
     * auf ScheduleMaster gewechselt.
     */
    ReInit,
    
    /**
     * Der Batch Process wird mit den Scheduling Data des Masters gescheduled.
     * In {@link BatchProcessScheduling#trySchedule(SchedulingOrder, boolean)} wird dann 
     * auf die Scheduling Data der Slaves gewechselt und der Status geht nach ScheduleSlaves über.
     */
    ScheduleMaster,
    
    /**
     * Es sollen neue Slaves gestartet werden. Hierzu muss der Batch Process mit den 
     * Scheduling Data der Slaves gescheduled werden.
     */
    ScheduleSlaves,
    
    /**
     * Der Batch Process soll im Scheduler ignoriert werden. Dies ist der Fall, wenn keine
     * Slaves mehr gestartet werden sollen, die gestarteten Slaves aber noch nicht alle die vom Master
     * belegten Kapazitäten abgeholt haben
     */
    Ignore,
    
    /**
     * Der Batch Process soll beim nächsten Scheduling aus dem Scheduler entfernt werden.
     * Dies ist der Fall, wenn alle Slaves gestartet worden sind, der Batch Process pausieren soll ({@link BatchProcess#pauseBatchProcess(String)})
     * oder abgebrochen wurde ({@link BatchProcess#cancelBatchProcess(CancelMode, boolean, long)}).
     */
    Remove,
    
    /**
     * Der Batch Process wurde in {@link BatchProcessScheduling#tryScheduleBatch(SchedulingOrder,boolean)} aus 
     * dem Scheduler entfernt und wartet darauf, dass er wieder eingestellt wird.
     */
    Waiting,
    
    /**
     * Alle Slaves sind fertig. In {@link BatchProcessScheduling#tryScheduleBatch(SchedulingOrder,boolean)}
     * wird daher wieder auf die Scheduling Data des Masters gewechselt und der Status geht
     * nach ExecuteMaster über.
     */
    ScheduleForExecution,
    
    /**
     * Der Master soll gestartet werden.
     */
    ExecuteMaster;
  }

  private enum For {
    Slave,    //Master->Slave
    Refresh,  //Slave->Slave, prio,caps,timeconstraint können sich ändern
    FairnessOrRate; //Slave->Slave, timeconstraint kann sich ändern, entranceTimestamp ändert sich auf jeden fall
  }
  
  public SchedulingState getSchedulingState() {
    return schedulingState.get();
  }

  public SlaveOrderTypeInfo getSlaveOrderTypeInfo() {
    return slaveOrderTypeInfo;
  }
  
  /**
   * Master ist soeben in den Scheduler eingestellt worden. Damit ist hier zum ersten Mal die 
   * SchedulingOrder bekannt. 
   * @param so
   */
  public void addSchedulingOrder(SchedulingOrder so) {
    setMasterSchedulingOrder(so);
  }

  /**
   * Ersetzt die SchedulingData des Masters durch die der Slaves, falls Slaves gestartet werden
   * sollen. Wurden alle Slaves gestartet, wird wieder auf die SchedulingData des Masters
   * zurückgewechselt und der Master wird aus dem Scheduler entfernt, wenn noch nicht alle
   * Slaves beendet sind.
   * @param so
   * @param isMaster
   * @return 
   */
  public TryScheduleResult tryScheduleBatch(SchedulingOrder so, boolean isMaster) {
    if( schedulingState.get() == SchedulingState.ReInit ) {
      //es ist zu früh, der BatchProcess ist noch nicht wiederhergestellt
      //zum Canceln der Slaves deren orderIds sammeln 
      if (!isMaster) {
        batchProcess.setSlaveState(so.getOrderId(), OrderInstanceStatus.SCHEDULING);
      }
      if (logger.isTraceEnabled()) {
        logger.trace("B" + so.getOrderId() + "(" + SchedulingState.ReInit + "). CONTINUE");
      }
      return TryScheduleResult.CONTINUE;
    }
    
    if (isMaster) {
      if (schedulingState.get() == SchedulingState.Ignore) {
        if (logger.isTraceEnabled()) {
          logger.trace("B" + so.getOrderId() + "(" + SchedulingState.Ignore + "). CONTINUE");
        }
        return TryScheduleResult.CONTINUE;
      }

      if (schedulingState.get() == SchedulingState.Remove) {
        //Master soll aus dem Scheduler entfernt werden
        freeTransferableCapacities();
        boolean allSlavesFinished;
        try {
          allSlavesFinished = batchProcess.allSlavesFinished();
        } catch (Exception e) {
          logger.info("No further Batch Process inputs could be read.", e);
          // rather terminate early if input generator is throwing
          allSlavesFinished = true;
        } 
        if (allSlavesFinished && schedulingState.compareAndSet(SchedulingState.Remove, SchedulingState.ScheduleForExecution)){
          if (logger.isTraceEnabled()) {
            logger.trace("B" + so.getOrderId() + "(" + SchedulingState.Remove + ")->" + SchedulingState.ScheduleForExecution + ". CONTINUE");
          }
          return TryScheduleResult.CONTINUE;
        //könnte inzwischen schon den Status ScheduleForExecution haben, dann nicht entfernen
        } else if (schedulingState.compareAndSet(SchedulingState.Remove, SchedulingState.Waiting)){
          masterSchedulingOrder.addWaitingCause(WaitingCause.BatchProcess);
          if (logger.isTraceEnabled()) {
            logger.trace("B" + so.getOrderId() + "(" + SchedulingState.Remove + ")->" + SchedulingState.Waiting + ". CONTINUE");
          }
          return TryScheduleResult.REMOVE;
        }
      }

      if (schedulingState.get() == SchedulingState.ScheduleForExecution) {
        if (batchProcess.hasRunningSlaves()) {
          if (logger.isTraceEnabled()) {
            logger.trace("B" + so.getOrderId() + "(" + SchedulingState.ScheduleForExecution + ")runningSlaves! CONTINUE");
          }
          return TryScheduleResult.CONTINUE;
        } else {
          if (schedulingState.compareAndSet(SchedulingState.ScheduleForExecution, SchedulingState.ExecuteMaster)) { //wenn alle Slaves gestartet wurden
            //Master mit seinen eigenen SchedulingData einstellen
            schedulerMasterWith(masterSchedulingData);
            if (logger.isTraceEnabled()) {
              logger.trace("B" + so.getOrderId() + "(" + SchedulingState.ScheduleForExecution + ")->" + SchedulingState.ExecuteMaster
                  + " (changedSchedulingData->Master). REORDER");
            }
            return TryScheduleResult.REORDER;
          }
        }
      }

      //evtl. müssen die SchedulingData der Slaves aktualisiert werden
      if (schedulingState.get() == SchedulingState.ScheduleSlaves) {
        if (needsRefresh()) {
          schedulerMasterWith(slaveSchedulingData(For.Refresh));
          if (logger.isTraceEnabled()) {
            logger.trace("B" + so.getOrderId() + "(" + SchedulingState.ScheduleSlaves + ")needsRefresh!. REORDER");
          }
          return TryScheduleResult.REORDER;
        }
      }
    } else {
      //Slave geht nach Scheduling über
      batchProcess.setSlaveState(so.getOrderId(), OrderInstanceStatus.SCHEDULING);
    }
    if (logger.isTraceEnabled()) {
      logger.trace("B" + so.getOrderId() + "(" + schedulingState.get() + "). OK");
    }
    return null;
  }
  
  public void schedulerMasterWith(SchedulingData schedulingData) {
    masterSchedulingOrder.replaceSchedulingData(schedulingData);
  }

  private void setMasterOrder(XynaOrderServerExtension xo) {
    masterOrder = xo;
    setMasterSchedulingData( masterOrder.getSchedulingData() );
  }
 
  private void setMasterSchedulingOrder(SchedulingOrder so) {
    masterSchedulingOrder = so;
    AllOrdersList allOrders = XynaFactory.getInstance().getProcessing().getXynaScheduler().getAllOrdersList();
    masterOrder = allOrders.getXynaOrder(so);
    setMasterSchedulingData( so.getSchedulingData() );
  }

  private void setMasterSchedulingData(SchedulingData schedulingData) {
    this.masterSchedulingData = schedulingData;
    masterSchedulingData.setMustAcquireCapacitiesOnlyOnce(false);
    TimeConstraint masterTC = masterSchedulingData.getTimeConstraint();
    if( masterTC instanceof TimeConstraint_Window ) {
      this.timeWindowName = ((TimeConstraint_Window)masterTC).getWindowName();
    } else {
      this.timeWindowName = null;
    }
    //SchedulingData wurde hier noch nicht geschedult, deswegen kann geplante Startzeit des Masters nicht entnommen werden
  }

  /**
   * Wechselt auf die Slave SchedulingData, falls der BatchProcess mit den Master SchedulingData 
   * gescheduled wurde. Wurde bereits mit den Slave SchedulingData gescheduled, so werden Slaves
   * gestartet.
   * @param so
   * @return
   */
  public TryScheduleResult trySchedule(SchedulingOrder so, boolean isCancelled) {
    if (so.getBatchProcessMarker().isBatchProcessMaster()) {
      if (!isCancelled && schedulingState.compareAndSet(SchedulingState.ScheduleMaster, SchedulingState.ScheduleSlaves)) {
        //Slaves müssen mit eigenen SchedulingData gescheduled werden 
        so.replaceSchedulingData(slaveSchedulingData(For.Slave)); //(Master->Slave)
        if (logger.isTraceEnabled()) {
          logger.trace("S" + so.getOrderId() + "(" + SchedulingState.ScheduleMaster +")->" + SchedulingState.ScheduleSlaves + " (changedSchedulingData->Slave). REORDER");
        }
        return TryScheduleResult.REORDER;
      }
      
      if (isCancelled && schedulingState.compareAndSet(SchedulingState.ScheduleMaster, SchedulingState.ExecuteMaster)) {
        if (logger.isTraceEnabled()) {
          logger.trace("S" + so.getOrderId() + "(" + SchedulingState.ScheduleMaster +")->" + SchedulingState.ExecuteMaster + ". REORDER");
        }
        return TryScheduleResult.REORDER;
      }
      
      if (schedulingState.get() == SchedulingState.ScheduleSlaves) {
        //es müssen noch weitere Slaves gestartet werden
        parallelExecutor.addTask( new BatchProcessSlaveCreatorTask(batchProcess, parallelExecutor, so.getOrderId(), so.getSchedulingData().getMultiAllocationCapacities() ) );
        try {
          parallelExecutor.execute();
        } catch(RejectedExecutionException e ) {
          logger.info("Could not start new BatchProcessSlaveCreatorTask, threadpool is full. -> break scheduler loop", e);
          return TryScheduleResult.BREAKLOOP; //gibt capacities frei
        }

        if( slaveExecutionPeriod != null ) {
          slaveExecutionPeriod.incrementCounter();
        }
        so.replaceSchedulingData(slaveSchedulingData(For.FairnessOrRate));
        if (logger.isTraceEnabled()) {
          logger.trace("S" + so.getOrderId() + "(" + SchedulingState.ScheduleSlaves + "): started Slave. REORDER");
        }
        return TryScheduleResult.REORDER;
      }
      
      if (schedulingState.get() == SchedulingState.ExecuteMaster) {
        if (logger.isTraceEnabled()) {
          logger.trace("S" + so.getOrderId() + "(" + schedulingState.get() + "). OK");
        }
        return null;
      }
      
      if (logger.isTraceEnabled()) {
        logger.trace("S" + so.getOrderId() + "(" + schedulingState.get() + "). REORDER");
      }
      return TryScheduleResult.REORDER;
    } else {
      //Slave wechselt von Scheduling auf Running
      if (batchProcess != null) {
        batchProcess.switchToRunningSlave(so.getOrderId());
      }
      if (logger.isTraceEnabled()) {
        logger.trace("S" + so.getOrderId() + "(" + schedulingState.get() + "). OK");
      }
      return null;
    }
  }

  public TryScheduleResult tryScheduleMasterFailed() {
    while (true) {
      SchedulingState state = schedulingState.get();
      switch (state) {
        case ExecuteMaster :
        case ScheduleForExecution :
          //master abbrechen
          if (logger.isTraceEnabled()) {
            logger.trace("F" + masterOrder.getId() + "(" + schedulingState.get() + "). OK");
          }
          return null;
        case ScheduleMaster : //das ist z.b. der fall, wenn der batchprozess beim einstellen in scheduler bereits timeout hat, dann kommt er nie zum schedulen der slaves
        case ScheduleSlaves :
        case Remove :
          if (!schedulingState.compareAndSet(state, SchedulingState.ScheduleForExecution)) {
            continue;
          }
          //beim nächsten schedulingdurchlauf geht es nach "ExecuteMaster" (vgl tryScheduleBatch())
          
          if (masterSchedulingOrder.isMarkedAsTimedout()) {
            batchProcess.setTimeoutTransiently();
          }
          schedulerMasterWith(masterSchedulingData);
          //master unabhängig von zeitfenster starten
          masterSchedulingData.setTimeConstraint(TimeConstraint.immediately());
          if (logger.isTraceEnabled()) {
            logger.trace("F" + masterOrder.getId() + "(?)->" + schedulingState.get() + ": changedSchedulingData->Master/Immediate. REORDER");
          }
          return TryScheduleResult.REORDER;
        case Waiting :
          //sollte nie aufgerufen werden
          if (!schedulingState.compareAndSet(SchedulingState.Waiting, SchedulingState.ScheduleForExecution)) {
            continue;
          }
          
          if (masterSchedulingOrder.isMarkedAsTimedout()) {
            batchProcess.setTimeoutTransiently();
          }
          masterSchedulingOrder.removeWaitingCause(WaitingCause.BatchProcess);
          schedulerMasterWith(masterSchedulingData);
          //master unabhängig von zeitfenster starten
          masterSchedulingData.setTimeConstraint(TimeConstraint.immediately());
          //beim nächsten schedulingdurchlauf geht es nach "ExecuteMaster" (vgl tryScheduleBatch())
          if (logger.isTraceEnabled()) {
            logger.trace("F" + masterOrder.getId() + "(" + SchedulingState.Waiting + ")->" + SchedulingState.ScheduleForExecution
                + ": changeSchedulingData->Master/Immediate. REORDER");
          }
          return TryScheduleResult.REORDER;
        case Ignore :          
          /*
           * das kann passieren, wenn der master gerade noch nicht fertig ist. in diesem fall ist es am besten
           * darauf zu warten, dass er von alleine fertig wird.
           * sollte diese methode wegen einem expliziten cancel aufgerufen worden sein, sollte der benutzer
           * einfach die noch laufenden slaves killen, damit der master dann laufen kann.
           */
          if (logger.isTraceEnabled()) {
            logger.trace("F" + masterOrder.getId() + "(" + schedulingState.get() + ": RETRY_NEXT");
          }
          return TryScheduleResult.RETRY_NEXT;
        case Init :
        case ReInit :
        default :
          //nicht erwartet, aber lieber nichts loggen. mit CONTINUE macht man es nicht schlimmer, aber auch nicht besser.
          if (logger.isTraceEnabled()) {
            logger.trace("F" + masterOrder.getId() + "(" + schedulingState.get() + "): CONTINUE");
          }
          return TryScheduleResult.CONTINUE;
      }
    }
  }


  private SchedulingData slaveSchedulingData(For reason) {
    if( slaveSchedulingData == null ) {
      slaveSchedulingData = createSlaveSchedulingData();
    }
    //Eventuelle Änderungen aus slaveOrderTypeInfo nachtragen
    if( slaveOrderTypeInfo.hasChangedPriority() ) {
      slaveSchedulingData.setPriority(slaveOrderTypeInfo.getPriority());
    }
    if( slaveOrderTypeInfo.hasChangedCapacities() ) {
      slaveSchedulingData.setMultiAllocationCapacities(getMultiAllocationCapacities());
    }    
    
    switch( reason ) {
      case Slave:
        //obiges createSlaveSchedulingData() sollte ausreichen
        break;
      case Refresh:
        //obige Änderungen reichen
        break;
      case FairnessOrRate:
        slaveSchedulingData.setTimeConstraint( createSlaveTimeConstraintFor(reason) );
        //EntranceTimestamp ändern, so dass auch bei TimeConstraint.immediately() die StartTime geändert wird.
        //Grund: neue Slaves sollen ihre Capacity fair erhalten, d.h nicht mit der hohen Urgency des ersten Slaves
        slaveSchedulingData.setEntranceTimestamp(System.currentTimeMillis());
        break;
      default:
        logger.warn("Unexpected reason for slaveSchedulingData " + reason);
    }
    return slaveSchedulingData;
  }
 
  /**
   * Legt die SchedulingData für einen Slave an. Die SchedulingData für den Master
   * müssen schon gesetzt sein, da hieraus Daten übernommen werden.
   * @return
   */
  public SchedulingData createSlaveSchedulingData() {
    if (logger.isDebugEnabled()) {
      logger.debug("masterSchedulingData " + masterSchedulingData);
    }
    SchedulingData slaveSchedulingData = new SchedulingData(masterSchedulingData.getTimeConstraintData().getEntranceTimestamp());
    slaveSchedulingData.setMultiAllocationCapacities(getMultiAllocationCapacities());
    slaveSchedulingData.setMustAcquireCapacitiesOnlyOnce(false);
    slaveSchedulingData.setPriority(slaveOrderTypeInfo.getPriority());
    slaveSchedulingData.setTimeConstraint( createSlaveTimeConstraintFor(For.Slave) );
    return slaveSchedulingData;
  }
  
  /**
   * Bestimmt die Kapazitäten, die die Slaves benötigen.
   * @return
   * @throws PersistenceLayerException 
   */
  private MultiAllocationCapacities getMultiAllocationCapacities() {
    List<Capacity> caps = slaveOrderTypeInfo.getCapacities();
    MultiAllocationCapacities mac = new MultiAllocationCapacities(caps);
    //es sollen maximal so viele Kapazitäten allokiert werden, wie Slaves parallel
    //gestartet werden dürfen
    mac.setMaxAllocation(batchProcess.getMaxParallelism());
    mac.setTransferable(true);
    return mac;
  }

  public TimeConstraint createSlaveTimeConstraintFor(For reason) {
    TimeConstraint_Start slaveTC = null;
    if( slaveExecutionPeriod == null ) {
      slaveTC = TimeConstraint.immediately();
    } else {
      slaveTC = TimeConstraint.at( getNextSlaveExecution(reason) );
    }
    if( timeWindowName != null ) {
      return slaveTC.withTimeWindow(timeWindowName);
    } else {
      return slaveTC;
    }
  }
  

  private long getNextSlaveExecution(For reason) {
    long startTimestamp = 0; //Startzeitpunkt ermitteln, auf die sich die Slave-Startzeit bezieht
    if( timeWindowName != null ) {
      if( reason == For.Slave ) {
        startTimestamp = masterSchedulingData.getTimeConstraintData().getWindowIsOpenSince();
      } else {
        startTimestamp = slaveSchedulingData.getTimeConstraintData().getWindowIsOpenSince();
      }
    } else {
      startTimestamp = masterSchedulingData.getTimeConstraintData().getStartTimestamp();
    }
    slaveExecutionPeriod.reInit( startTimestamp );
    int cnt = slaveExecutionPeriod.getCounter();
    long next = slaveExecutionPeriod.next(System.currentTimeMillis());
    logger.debug( "SlaveExecutionPeriod: next slave scheduling at "+next+";  reason "+reason+"  "+cnt+": " + startTimestamp%100000 +"  " +System.currentTimeMillis()%100000 );
    return next;
  }

  /**
   * Gibt alle transferierbaren Kapazitäten frei
   */
  public void freeTransferableCapacities () {
    XynaScheduler scheduler = getXynaScheduler();
    transferCapsLock.lock();
    try{
      scheduler.getCapacityManagement().freeTransferableCapacities(masterOrder);
    } finally {
      transferCapsLock.unlock();
    }
    
    scheduler.notifyScheduler();
  }

  
  private XynaScheduler getXynaScheduler() {
    return XynaFactory.getInstance().getProcessing().getXynaScheduler();
  }
  
  /**
   * Ist Auftrag fortsetzbar?
   * @return
   */
  public boolean canBeContinued() {
    return schedulingState.isIn( SchedulingState.Init,    //BP ist pausiert gestartet
                                 SchedulingState.Remove, //BP soll noch entfernt werden, ist aber noch nicht geschedult
                                 SchedulingState.Waiting ); //BP ist entfernt worden
  }
  
  /**
   * Ist Auftrag cancelbar? 
   * @return
   */
  public boolean canBeCanceled() {
    return ! schedulingState.isIn( SchedulingState.ScheduleForExecution, //wartet noch auf letztes Scheduling
                                   SchedulingState.ExecuteMaster ); //Master wird bereits ausgeführt
  }

  public void restart(SchedulingData newMasterSchedulingData) throws XynaException {
    setMasterSchedulingData( newMasterSchedulingData );
    schedulerMasterWith(newMasterSchedulingData);
    
    //Master befindet sich schon in der Execution-Phase
    if (batchProcess.isCanceled() || batchProcess.allSlavesFinished()) {
      schedulingState.set(SchedulingState.ExecuteMaster);
    }
    
    if (batchProcess.isPaused()) {
      //BatchProcess ist gerade pausiert
      schedulingState.set(SchedulingState.Remove);
    }
    
    schedulingState.compareAndSet(SchedulingState.ReInit, SchedulingState.ScheduleMaster);
    if (logger.isTraceEnabled()) {
      logger.trace("R" + masterOrder.getId() + "(" + SchedulingState.ReInit + ")->" + SchedulingState.ScheduleMaster);
    }
    
    getXynaScheduler().notifyScheduler();
  }

  public boolean pause(boolean freeCaps) {
    if (schedulingState.isIn( SchedulingState.ReInit, SchedulingState.Init ) ) {
      if (logger.isTraceEnabled()) {
        logger.trace("P" + masterOrder.getId() + "(" + SchedulingState.ReInit + ")->" + SchedulingState.Init);
      }
      return true; //Initialisierungsphase
    } else if ( schedulingState.compareAndSet(SchedulingState.ScheduleMaster, SchedulingState.Remove) ) {
      if (logger.isTraceEnabled()) {
        logger.trace("P" + masterOrder.getId() + "(" + SchedulingState.ScheduleMaster + ")->" + SchedulingState.Remove);
      }
      return true;
    } else if( schedulingState.compareAndSet(SchedulingState.ScheduleSlaves, SchedulingState.Remove)) {
      if (freeCaps) {
        freeTransferableCapacities();
      }
      if (logger.isTraceEnabled()) {
        logger.trace("P" + masterOrder.getId() + "(" + SchedulingState.ScheduleSlaves + ")->" + SchedulingState.Remove);
      }
      return true;
    } else {
      return false;
    }
  }


  /**
   * @return true, wenn Slaves abgebrochen werden sollen
   */
  public boolean cancel() {
    if (schedulingState.compareAndSet(SchedulingState.ScheduleMaster, SchedulingState.ExecuteMaster)) {
      //Wenn der BatchProcess noch nicht dabei ist Slaves zu starten, soll er direkt in die Execution-Phase übergehen
      new AllSlavesStartedRunnable().run(); //so tun, als ob alle Slaves gestartet worden wären
      if (logger.isTraceEnabled()) {
        logger.trace("C" + masterOrder.getId() + "(" + SchedulingState.ScheduleMaster + ")->" + SchedulingState.ExecuteMaster);
      }
    } else {
      //ansonsten sollen keine neuen Slaves gestartet werden
      schedulingState.compareAndSet(SchedulingState.ScheduleSlaves, SchedulingState.Remove);
      schedulingState.compareAndSet(SchedulingState.Ignore, SchedulingState.Remove);
      if (logger.isTraceEnabled()) {
        logger.trace("C" + masterOrder.getId() + "(" + SchedulingState.ScheduleSlaves + "/" + SchedulingState.Ignore + ")->"
            + SchedulingState.Remove);
      }

      //Jetzt noch warten, bis der ParallelExecutor keine weiteren Slaves erzeugt
      if (parallelExecutor != null) {
        parallelExecutor.setExecutionFinishedRunnable(new AllSlavesStartedRunnable());
        parallelExecutor.executeAndAwaitUninterruptable();
      } else {
        logger.warn("cancel batch process, although parallelExecutor not initialized");
      }
      
      //es könnte laufende Slaves geben, dies prüfen
      if (schedulingState.get() == SchedulingState.Remove || schedulingState.get() == SchedulingState.Waiting) {
        return true;
      }
    }
    return false;
  }

  /**
   * Übertragen der vom Master reservierten Kapazitäten auf den Slave
   * @param xo
   * @param transferCapacities 
   */
  public void transferCapacities(XynaOrderServerExtension xo, TransferCapacities transferCapacities) {
    transferCapsLock.lock();
    try {
      if (!getXynaScheduler().getCapacityManagement().transferCapacities(xo, transferCapacities)) {
        //unexpected. kann das auftreten, wenn jemand im richtigen moment kapazitätskardinalitäten ändert?
        logger.warn("Could not transfer capacities from " + transferCapacities.getFromOrderId() + " to " + xo.getId());
      }
    } finally {
      transferCapsLock.unlock();
    }
  }

  /**
   * Setzt den SchedulingState auf Ignore, falls keine weiteren Slaves mehr gestartet werden sollen
   * @param inputGeneratorHasNext
   * @return
   */
  public boolean canSlaveBeStarted(boolean inputGeneratorHasNext) {
    if (!inputGeneratorHasNext) { //es gibt keine Input-Daten mehr
      //es sollen keine weiteren Slaves gestartet werden
      if( schedulingState.compareAndSet(SchedulingState.ScheduleSlaves, SchedulingState.Ignore) ) {
        if (logger.isTraceEnabled()) {
          logger.trace(masterOrder.getId() + "(" + SchedulingState.ScheduleSlaves + ")->" + SchedulingState.Ignore + ": allSlavesStarted");
        }
        parallelExecutor.setExecutionFinishedRunnable(new AllSlavesStartedRunnable() );
        if (batchProcess.getStarted() == 0) {
          //es sind überhaupt keine Input-Daten vorhanden, dadurch werden keine Slaves
          //gestartet und somit kein SlaveResponseListener aufgerufen, daher hier den
          //Master wieder in den Scheduler einstellen
          terminateSlaves();
        }
      }
      return false;
    }
    
    //Batch Process wurde evtl. inzwischen abgebrochen oder pausiert
    if (schedulingState.get() != SchedulingState.ScheduleSlaves) {
      return false;
    }
    
    return true;
  }
  
  /**
   * Dieses Runnable wird ausgeführt, wenn alle Slaves gestartet wurden
   */
  private class AllSlavesStartedRunnable implements Runnable {
    public void run() {
      //wenn alle Slaves gestartet wurden, müssen die vom Master bereits belegten Kapazitäten
      //wieder freigeben werden und der BatchProcess aus dem Scheduler entfernt werden    
      
      //es kann bereits rescheduleMaster (durch SlaveResponseListener) aufgerufen worden sein, dann passiert hier kein zustandsübergang
      if (schedulingState.compareAndSet(SchedulingState.Ignore, SchedulingState.Remove)) {
        if (logger.isTraceEnabled()) {
          logger.trace(masterOrder.getId() + "(" + SchedulingState.Ignore + ")->" + SchedulingState.Remove + ": allSlaveTasksFinished");
        }
      }
      freeTransferableCapacities();
      batchProcess.setAllSlavesStarted();
    }
  }
  
  public OrderInstanceStatus getOrderStatus(boolean masterInCleanup) {
    if( masterInCleanup ) {
      return OrderInstanceStatus.RUNNING_CLEANUP;
    }
    if (masterSchedulingOrder == null) {
      if( schedulingState.is(SchedulingState.Init) ) {
        return OrderInstanceStatus.RUNNING_PLANNING;
      } else {
        SchedulingOrder so = getXynaScheduler().getAllOrdersList().getSchedulingOrder(batchProcess.getBatchProcessId());
        if( so != null ) {
          setMasterSchedulingOrder(so);
          return masterSchedulingOrder.getOrderStatus();
        } else {
          //sollte nicht vorkommen
          return OrderInstanceStatus.INITIALIZATION; //RUNNING_PLANNING;
        }
      }
    } else {
      return masterSchedulingOrder.getOrderStatus();
    }
  }

  /**
   * Gibt die restlichen für Slaves reservierten Capacities frei und stellt den Master wieder
   * in den Scheduler ein
   */
  public void terminateSlaves() {
    freeTransferableCapacities();
    
    //BatchProcess kann im Status ScheduleMaster sein (z.B. wenn nach einem Neustart der letzte
    //Slave vor dem Master geschedult wird und fertig ist, bevor der Master den Zustand 
    //ScheduleMaster verlässt)
    if (schedulingState.compareAndSet(SchedulingState.ScheduleMaster, SchedulingState.Ignore) && logger.isTraceEnabled()) {
      logger.trace(masterOrder.getId() + "(" + SchedulingState.ScheduleMaster + ")->" + SchedulingState.Ignore);
    }

    rescheduleMaster(true, false);
  }
  
  /**
   * Stellt den Master wieder mit seinen eigenen SchedulingData in den Scheduler ein.
   * @param executeMaster true, wenn der Master in die Execution-Phase übergehen soll
   */
  public void rescheduleMaster(boolean executeMaster, boolean ignoreTimeConstraintsOfMaster) {
    XynaScheduler scheduler = getXynaScheduler();
    AllOrdersList allOrders = scheduler.getAllOrdersList();
    
    if (ignoreTimeConstraintsOfMaster) {
      masterSchedulingData.setTimeConstraint(TimeConstraint.immediately());
      getXynaScheduler().changeSchedulingParameter(masterSchedulingOrder.getOrderId(), masterSchedulingData, true );
    }
    
    SchedulingState newState = executeMaster ? SchedulingState.ScheduleForExecution : SchedulingState.ScheduleMaster;
    
    if (schedulingState.compareAndSet(SchedulingState.Init, newState) ) {
      //Auftrag wartet noch vor erstem Scheduling,
      //daher kann und muss masterSchedulingData nicht gesetzt werden
      if (logger.isTraceEnabled()) {
        logger.trace(masterOrder.getId() + "(" + SchedulingState.Init + ")->" + newState + ": rescheduleMaster");
      }
      return;
    }
    
    //Master muss im Scheduler neu einsortiert werden, da er evtl. noch auf Capacities
    //der Slaves wartet, die er jetzt gar nicht mehr braucht
    if (schedulingState.compareAndSet(SchedulingState.Ignore, newState)
        || schedulingState.compareAndSet(SchedulingState.Remove, newState)) {
      if (!ignoreTimeConstraintsOfMaster) { //sonst oben bereits passiert
        //auf Master SchedulingData wechseln und im Scheduler neu einsortieren      
        getXynaScheduler().changeSchedulingParameter(masterSchedulingOrder.getOrderId(), masterSchedulingData, true );
      }
      if (logger.isTraceEnabled()) {
        logger.trace(masterOrder.getId() + "(" + SchedulingState.Ignore + "/" + SchedulingState.Remove + ")->" + newState
            + ": rescheduleMaster");
      }
      return;
    }
    
    //wenn er aus dem Scheduler entfernt wurde, muss er wieder eingestellt werden
    /*
     * falls Master bereits ausgetimed ist, ist ScheduleMaster am sinnvollsten, damit er dann im tryScheduleMasterFailed den Timeout löscht
     *   (falls man direkt nach ExecuteMaster gehen würde, würde der Timeout durchschlagen und damit der Master-Workflow nicht mehr durchgeführt werden)
     * falls Master gecancelt ist, ist wird oben im tryschedule bemerkt, dass nicht wieder slaves gescheduled werden
     * falls Master keinen Fehler hat, geht er wieder automatisch über ScheduleSlave korrekt weiter
     */
    newState = SchedulingState.ScheduleMaster;
    if (schedulingState.compareAndSet(SchedulingState.Waiting, newState)) {
      //auf Master SchedulingData wechseln
      schedulerMasterWith(masterSchedulingData);
      //und wieder in den Scheduler einstellen
      allOrders.continueBatchProcess(masterSchedulingOrder,true);
      if (logger.isTraceEnabled()) {
        logger.trace(masterOrder.getId() + "(" + SchedulingState.Waiting + ")->" + newState + ": rescheduleMaster");
      }
      return;
    }
  }
  
  
  /**
   * Setzt das Flag, das beim nächsten Scheduling die Slaves neue SchedulingData brauchen
   * und notified den Scheduler.
   */
  public void refreshSlaveSchedulingData() {
    refreshSlaveSchedulingData.set(true);
    slaveOrderTypeInfo.orderTypeChanged();
    getXynaScheduler().notifyScheduler();
  }

  public boolean needsRefresh() {
    return refreshSlaveSchedulingData.compareAndSet(true, false);
  }

  public TimeConstraint changeMasterTimeConstraint(TimeConstraint plannedTC, TimeWindowDefinition newTWD ) {
    Long orderId = masterSchedulingOrder.getOrderId();
    TimeConstraint oldTC = masterSchedulingOrder.getSchedulingData().getTimeConstraint();
    
    Long batchProcessId = orderId;
    
    removeTimeWindow(batchProcessId);
    
    TimeConstraint newTC = createTimeWindow(batchProcessId,plannedTC, newTWD);
    
    if( oldTC.toString().equals(newTC.toString()) ) {
      //keine Änderung
      return oldTC;
    } else {
      if( logger.isDebugEnabled() ) {
        logger.debug( "changeMasterTimeConstraint "+oldTC+" -> "+ newTC);
      }
      //im XynaScheduler reschedulen, damit Änderungen wirksam werden, wenn Auftrag bereits wartet
      getXynaScheduler().changeSchedulingParameter(orderId, newTC);
      setMasterSchedulingData(masterSchedulingOrder.getSchedulingData());
    }
    return newTC;
  }

  public TimeConstraint createTimeWindow(Long batchProcessId, TimeConstraint timeContraint, TimeWindowDefinition timeWindowDefinition) {
    
    if( timeWindowDefinition == null ) {
      //nichts zu tun; TimeConstraint bleibt unverändert
      if( timeContraint == null ) {
        return TimeConstraint.immediately(); //ist äquivalent
      } else {
        return timeContraint;
      }
    }
    String windowName = "batchProcess-"+batchProcessId;
    
    TimeConstraintWindowDefinition definition = 
        new TimeConstraintWindowDefinition(windowName,
                                           "TimeWindow for BatchProcess "+batchProcessId,
                                           timeWindowDefinition,
                                           false );
    try {
      getXynaScheduler().getTimeConstraintManagement().addTimeWindow(definition);
      
    } catch (XPRC_DuplicateTimeWindowNameException e) {
      throw new RuntimeException(e); //wird nur in Memory ersetzt -> unerwartet
    } catch (PersistenceLayerException e) { 
      throw new RuntimeException(e); //wird nicht in DB gespeichert
    } catch (XPRC_TimeWindowNotFoundInDatabaseException e) {
      throw new RuntimeException(e); //wird nicht in DB gespeichert
    } catch (XPRC_TimeWindowRemoteManagementException e) {
      //wird nicht remote benötigt, daher nicht schlimm
      logger.warn("Could not create timwindow remotely", e );
    }
    //TimeConstraint origMasterTC = timeContraint;
    TimeConstraint newMasterTC;
    if( timeContraint instanceof TimeConstraint_Start ) {
      newMasterTC = ((TimeConstraint_Start)timeContraint).withTimeWindow(windowName);
    } else if( timeContraint instanceof TimeConstraint_Window ) {
      TimeConstraint_Window old = (TimeConstraint_Window)timeContraint;
      newMasterTC = new TimeConstraint_Window(old.getBeforeTimeConstraint(),windowName,old.getInnerTimeConstraint());
    } else if( timeContraint == null ) {
      newMasterTC = TimeConstraint.schedulingWindow(windowName);
    } else {
      throw new RuntimeException("Unexpected TimeConstraint "+timeContraint);
    }
    return newMasterTC;
  }
  
  public TimeWindowDefinition getTimeWindowDefinition() {
    if( timeWindowName == null ) {
      return null;
    } else {
      TimeConstraintWindowDefinition def = getXynaScheduler().getTimeConstraintManagement().getDefinition(timeWindowName);
      if( def == null ) {
        logger.warn( "TimeConstraintManagement does not know timewindow "+timeWindowName);
        return null;
      } else {
        return def.getTimeWindowDefinition();
      }
    }
  }

  public void removeTimeWindow(Long batchProcessId) {
    String windowName = "batchProcess-"+batchProcessId;
    try {
      getXynaScheduler().getTimeConstraintManagement().removeTimeWindow(windowName, true);
    } catch (PersistenceLayerException e) {
      //unerwartet, da TimeWindows für BatchProcesse nicht gespeichert werden
      throw new RuntimeException(e);
    } catch (XPRC_TimeWindowStillUsedException e) {
      //wegen force=true unerwartet
      throw new RuntimeException(e);
    }
  }

  public void acknowledgeMasterBackup(boolean paused) {
    SchedulingState newState = paused ? SchedulingState.Remove : SchedulingState.ScheduleMaster;
    schedulingState.compareAndSet(SchedulingState.Init, newState);
    if (logger.isTraceEnabled()) {
      logger.trace("A" + masterOrder.getId() + "(" + SchedulingState.Init + ")->" + newState);
    }
  }

  public TimeConstraint prepareTimeContraint(Long batchProcessId, TimeConstraint timeConstraint,
                                   TimeWindowDefinition timeWindowDefinition) {
    TimeConstraint tc = createTimeWindow(batchProcessId,timeConstraint,timeWindowDefinition);
    masterOrder.getSchedulingData().setTimeConstraint(tc);
    setMasterSchedulingData(masterOrder.getSchedulingData());
    return tc;
  }


  public void createParallelExecutorAndCheckLimitation(SlaveOrderTypeInfo slaveOrderTypeInfo, SlaveExecutionPeriod slaveExecutionPeriod, 
                            Integer total, int maxParallelism) {
   
    //ParallelExecutor anlegen
    int prio = slaveOrderTypeInfo.getPriority();
    parallelExecutor = new ParallelExecutor(new ExecutionThreadPoolExecutorWithDecreasingPrio(prio));
    parallelExecutor.setThreadLimit(maxParallelism);
    parallelExecutor.setTaskConsumerPreparator( new SimpleXynaRunnableTaskConsumerPreparator(true) );
    
    //Limitierung prüfen und speichern
    this.slaveExecutionPeriod = checkLimitation(batchProcess.getBatchProcessId(), slaveOrderTypeInfo, slaveExecutionPeriod, total);
    this.slaveOrderTypeInfo = slaveOrderTypeInfo;
  }

  private SlaveExecutionPeriod checkLimitation(Long batchProcessId, SlaveOrderTypeInfo slaveOrderTypeInfo, SlaveExecutionPeriod slaveExecutionPeriod, Integer total) {
    boolean hasCapacities = ! slaveOrderTypeInfo.getCapacities().isEmpty();
    boolean hasSlaveExecutionPeriod = slaveExecutionPeriod != null; //TODO interval > 0 ?
    boolean hasSmallTotal = total != null && total > 0 && total < 100;
    if( hasCapacities || hasSlaveExecutionPeriod || hasSmallTotal ) {
      //BatchProcess ist limitiert
    } else {
      //BatchProcess ist nicht limitiert, dies kann zu Problemen führen (zu viel Last, BatchProcess kann abbrechen)
      MissingLimitationReaction reaction = XynaProperty.BATCH_NO_LIMITATION_REACTION.get();
      logger.info("BatchProcess "+batchProcessId+" has no limitation, reaction: "+reaction);
      switch(reaction) {
        case Fail:
          throw new IllegalArgumentException( "BatchProcess has no limitation!");
        case SlowDown:
          return new SlaveExecutionPeriod(Type.FixedDate_CatchUpImmediately, 100);
        case Unlimited:
          break;
        default:
          throw new IllegalArgumentException( "BatchProcess has no limitation!");
      }
    }
    return slaveExecutionPeriod;
  }
  
  
}
