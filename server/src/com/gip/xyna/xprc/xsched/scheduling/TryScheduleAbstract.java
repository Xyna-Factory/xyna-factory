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
package com.gip.xyna.xprc.xsched.scheduling;

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_Scheduler_SchedulerBeanMissingException;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessMarker;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderBackupHandling;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.capacities.CapacityAllocationResult;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintResult;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;

/**
 *
 */
public abstract class TryScheduleAbstract implements TrySchedule {

  protected static Logger logger = CentralFactoryLogging.getLogger(TryScheduleAbstract.class );
  private XynaScheduler xynaScheduler;
  
  public final static char EXECUTION_PIPELINE_SEPERATOR = '>';
  
  private final XynaPropertyBuilds<Execution> regularExecutionPipeline = 
    new XynaPropertyBuilds<TryScheduleAbstract.Execution>("xyna.xprc.xsched.enforceurgency",
                                                          new BooleanBasedExecutionPipelineBuilder(),
                                                          ExecutionPipelineConfiguration.getDefaultExecutionConfiguration().buildPipeline(TryScheduleAbstract.this))
    .setDefaultDocumentation(DocumentationLanguage.EN, "If set to true running backups will not lead to conflicting lower urgency orders being scheduled before the backupping order.")
    .setDefaultDocumentation(DocumentationLanguage.DE, "Wenn mit 'true' belegt wird ein laufendes Backup nicht dazu führen das weniger dringliche Aufträge welche die gleichen Ressourcen benötigen vorgezogen werden.");
  
  private volatile Execution batchTimingCapVetoSchedule = new Batch( regularExecutionPipeline.get() );
    
  private Execution termination = new BackupGate( new Schedule() );
  
  
  TryScheduleAbstract(XynaScheduler xynaScheduler) {
    this.xynaScheduler = xynaScheduler;
  }

  protected TryScheduleResult tryScheduleInternal(SchedulingOrder so) {
    if( so.getSchedulingData() == null ) {
      so.terminate(new XPRC_Scheduler_SchedulerBeanMissingException());
    }
    TryScheduleResult tsr = null;
    if( so.canBeScheduled() ) {
      if( so.getBatchProcessMarker() != null ) {
        tsr = batchTimingCapVetoSchedule.tryExecute(so);
      } else {
        tsr = regularExecutionPipeline.get().tryExecute(so);
      }
    } else {
      tsr = termination.tryExecute(so);
    }
    
    
    switch( tsr.getType() ) {
      case SCHEDULE:
      case DELETE:
        return tsr;
      default:
        backupOrderIfNotAlreadyDone(so);
        return tsr;
    }
    
  }
  
  /**
   * backup falls auftrag nicht gescheduled werden konnte
   */
  private void backupOrderIfNotAlreadyDone(SchedulingOrder so) {
    XynaOrderServerExtension xo = so.getXynaOrderOrNull();
    if (xo == null || 
        xo.hasBeenBackuppedInScheduler() || 
        xo.hasBeenBackuppedAfterChange() || 
        !xo.getDestinationKey().isAllowedForBackup()) {
      // wenn der auftrag bereits einmal hier durch gelaufen ist oder breits von der AllOrdersList gebackupt wurde, braucht man nicht nochmals backupen
      return;
    }
    if (XynaProperty.XYNA_BACKUP_ORDERS_WAITING_FOR_SCHEDULING.get()) {
      // TODO check if no other lane is running, another place for his check would be once a child order is finished
      try {
        OrderBackupHandling.backup(so);
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to backup order <" + so + ">", e);
      }

    }
  }
  
  
  /**
   * Kümmert sich um ein einfaches uns sicheres Ausführen von Capacity- und Veto-Allozieren 
   * und dem anschließenden Schedulen, so dass im Fehlerfall ordentlich aufgeräumt wird.
   * Das Aufräumen geschieht im finally-Block, so das es sicher gegen Exceptions ist
   *
   * Reihenfolge kann dann einfach festgelegt werden: siehe oben
   * private Execution capVetoSchedule = new CapacityAllocation( new VetoAllocation( new Schedule() ) );
   *
   */
  private abstract class Execution {
    private Execution subExecution;

    public Execution(Execution subExecution) {
      this.subExecution = subExecution;
    }
        
    public TryScheduleResult tryExecute(SchedulingOrder so ) {
      TryScheduleResult tsr = null;
      try {
        if( so.canBeScheduled() || subExecution == null ) {
          //nur ausführen, wenn Order geschedult werden kann, letzte Execution (subExecution == null) muss trotzdem ausführen
          tsr = execute(so);
        }
        if( tsr == null ) {
          //es ist kein Fehler aufgetreten, daher subExecution ausführen
          if( subExecution != null ) {
            tsr = subExecution.tryExecute(so);
          }
        }
      } finally {
        try {
          if( tsr == null ) {
            //irgendwo ist eine Exception aufgetreten, deswegen ist tsr == null
            undo(so);
          } else if( tsr.needsUndo() ) {
            //oder ein regulärer Fehler mit gesetztem tsr
            undo(so);
          } else {
            //Scheduling war erfolgreich
            finalize(so);
          }
        } catch( Exception e ) {
          logger.warn("Exception in finally block while trySchedule "+so.getOrderId(), e);
          return TryScheduleResult.RETRY_NEXT;
        }
      }
      return tsr;
    }
    
    protected abstract TryScheduleResult execute(SchedulingOrder so);
    
    protected abstract void undo(SchedulingOrder so);
    
    protected void finalize(SchedulingOrder so) {
      //leere Standard-Implementierung 
    }
  }
  
  private class VetoAllocation extends Execution {

    private boolean needsVetos; //TODO unschön, dass in einer nicht-auftragsabhängigen Klasse temporär Daten 
    //zu einem Auftrag gespeichert werden. Ist aber derzeit kein Problem, da nur der Scheduler seriell eine Instanz
    //dieser Klasse verwendet.
    
    public VetoAllocation(Execution subExecution) {
      super(subExecution);
    }

    @Override
    protected TryScheduleResult execute(SchedulingOrder so) {
      List<String> vetos = so.getVetos();
      needsVetos = ! vetos.isEmpty();
      if( needsVetos ) {
        needsVetos = so.getSchedulingData().isNeedsToAcquireVetosOnNextScheduling(); //TODO nötig?
      }
      if( needsVetos ) {
        //logger.debug( "VetoAllocation for xynaOrder "+so.getOrderId() );
        VetoAllocationResult var = xynaScheduler.getVetoManagement().allocateVetos(so.getOrderInformation(), vetos, so.getCurrentUrgency());
        if (! var.isAllocated()) {
          if( var.getXynaException() != null ) {
            so.terminate( var.getXynaException() );
            return null;
          }
          return TryScheduleResult.getVetoNotAvailableInstance(var);
        }
      }
      return null;
    }

    @Override
    protected void undo(SchedulingOrder so) {
      if( needsVetos ) {
        if( logger.isDebugEnabled() ) {
          logger.debug( "undo VetoAllocation for xynaOrder "+so.getOrderId());
        }
        xynaScheduler.getVetoManagement().undoAllocation(so.getOrderInformation(), so.getVetos());
      }
    }
    
    @Override
    protected void finalize(SchedulingOrder so) {
      if( needsVetos ) {
        if( logger.isDebugEnabled() ) {
          logger.debug( "finalize VetoAllocation for xynaOrder "+so.getOrderId());
        }
        xynaScheduler.getVetoManagement().finalizeAllocation(so.getOrderInformation(), so.getVetos());
      }
    }
    
  }
  
  private class CapacityAllocation extends Execution {
    
    private boolean needsCapacities; //TODO unschön, dass in einer nicht-auftragsabhängigen Klasse temporär Daten 
    //zu einem Auftrag gespeichert werden. Ist aber derzeit kein Problem, da nur der Scheduler seriell eine Instanz
    //dieser Klasse verwendet.
    
    public CapacityAllocation(Execution subExecution) {
      super(subExecution);
    }

    @Override
    protected TryScheduleResult execute(SchedulingOrder so) {
      needsCapacities = so.getSchedulingData().isNeedsToAcquireCapacitiesOnNextScheduling(); //TODO nötig?
      if( needsCapacities ) { 
        CapacityAllocationResult car = xynaScheduler.getCapacityManagement().allocateCapacities(so.getOrderInformation(), so.getSchedulingData() );
        if (! car.isAllocated()) {
          if( car.getXynaException() != null ) {
            so.terminate(car.getXynaException());
            return null;
          }
          return TryScheduleResult.getCapacityNotAvailableInstance(car);
        }
      }
      return null;
    }

    @Override
    protected void undo(SchedulingOrder so) {
      if( needsCapacities ) { 
        if( logger.isDebugEnabled() ) {
          logger.debug( "undo CapacityAllocation for xynaOrder "+so.getOrderId());
        }
        xynaScheduler.getCapacityManagement().undoAllocation(so.getOrderInformation(), so.getSchedulingData() );
      }
    }

  }
  
  private class TimeConstraint extends Execution {

    private boolean needsToCheckTimeConstraint; //TODO unschön, dass in einer nicht-auftragsabhängigen Klasse temporär Daten 
    //zu einem Auftrag gespeichert werden. Ist aber derzeit kein Problem, da nur der Scheduler seriell eine Instanz
    //dieser Klasse verwendet.
    
    public TimeConstraint(Execution subExecution) {
      super(subExecution);
    }

    @Override
    protected TryScheduleResult execute(SchedulingOrder so) {
      needsToCheckTimeConstraint = so.getSchedulingData().needsToCheckTimeConstraintOnNextScheduling();
      if( needsToCheckTimeConstraint ) {
        TimeConstraintResult tr = xynaScheduler.getTimeConstraintManagement().checkTimeConstraint(so);
        if( tr.isExecutable() ) {
          return null;
        } else {
          /*
           * - falls timeout -> timeout setzen + return null
           * - falls "soll später gescheduled werden" -> return "TimeConstraint + Remove" (oder TimeConstraint + Continue (falls einfach nochmal probiert werden soll))
           * - TimeConstraint konnte nicht ausgewertet werden -> auch schedulelater?
           */
          if( tr.scheduleLater() ) {
            return TryScheduleResult.getScheduleLaterInstance(tr);
          } else {
            so.timeout(tr.getXynaException());
          }
        }
      }
      return null;
    }

    @Override
    protected void undo(SchedulingOrder so) {
      //nichts zu tun
    }
  
  }
  
  private class Batch extends Execution {

    public Batch(Execution subExecution) {
      super(subExecution);
    }

    @Override
    protected TryScheduleResult execute(SchedulingOrder so) {
      BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
      BatchProcessMarker marker = so.getBatchProcessMarker();
      if(marker == null){
        return null; //einfach überspringen, sollte so eh nicht aufgerufen werden
      }
      TryScheduleResult tsr = bpm.tryScheduleBatch(so);
      if( tsr != null ) {
        so.setOrderStatus(OrderInstanceStatus.WAITING_FOR_BATCH_PROCESS);
      }
      return tsr;
    }

    
    @Override
    protected void undo(SchedulingOrder so) {
      //nichts zu tun
    }
  }


  private class Schedule extends Execution {
    
    public Schedule() {
      super(null);
    }
    
    @Override
    protected TryScheduleResult execute(SchedulingOrder so) {
      if( so.canBeScheduled() ) {
        return startOrder(so);
      } else {
        return terminateOrder(so);
      }
    }

    private TryScheduleResult startOrder(SchedulingOrder so) {
      //Behandlung der BatchProcessMaster
      BatchProcessMarker marker = so.getBatchProcessMarker();
      if (marker != null) {
        BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
        TryScheduleResult tsr = bpm.trySchedule(so);
        if (tsr != null) {
          return tsr;
        }
      }
      return startOrder( so, false);
    }
   
    private TryScheduleResult terminateOrder(SchedulingOrder so) {
      
      BatchProcessMarker marker = so.getBatchProcessMarker();
      if (marker != null) {
        BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
        TryScheduleResult tsr = bpm.tryScheduleMasterFailed(so);
        if (tsr != null) {
          //Slave-Scheduling wird abgebrochen, aber Master wird rescheduled
          so.clearFailure();
          return tsr;
        } 
        //else: BatchprocessMaster soll abgebrochen werden
      }
      
      return startOrder( so, true);
    }

    
    private TryScheduleResult startOrder(SchedulingOrder so, boolean terminateOrder) {
      
      if (logger.isDebugEnabled()) {
        logger.debug("going to "+(!terminateOrder?"start ":"terminate ")+ so+ " (priority=" + so.getSchedulingData().getPriority() + ")");
      }
      boolean success = !terminateOrder ? XynaOrderExecutor.startOrder(so) : XynaOrderExecutor.terminateOrder(so);
      if (success) {
        so.setOrderStatus(OrderInstanceStatus.RUNNING);
        return TryScheduleResult.SCHEDULE;
      } else {
        int waitTime = 50;
        if (logger.isInfoEnabled()) {
          logger.info("order " + so.getOrderId()
            + " could not be scheduled because there are no more threads available in execution threadpool.");
          if (logger.isDebugEnabled()) {
            logger.debug("scheduling will pause for " + waitTime + "ms and then continue");
          }
        }
        try {
          Thread.sleep(waitTime);
        } catch (InterruptedException e1) {
          Thread.currentThread().interrupt();
        }
        return TryScheduleResult.BREAKLOOP;
      }
    }

    @Override
    protected void undo(SchedulingOrder so) {
      //hier ist leider kein undo möglich
    }
    
  }
  
  
  private class BackupGate extends Execution {

    public BackupGate(Execution subExecution) {
      super(subExecution);
    }

    @Override
    protected TryScheduleResult execute(SchedulingOrder so) {
      XynaOrderServerExtension xo = so.getXynaOrderOrNull();
      if (xo != null && OrderBackupHandling.isBackupInProgress(xo.getRootOrder())) {
        return TryScheduleResult.RETRY_NEXT;
      } else {
        return null; // continue with sub
      }
    }

    @Override
    protected void undo(SchedulingOrder so) {
      // undo nicht sinvoll
    }
    
  }
  
  
  private static enum ExecPipePart {
    CAPACITY_ALLOCATION("capa", CapacityAllocation.class) {
      public Execution wrap(Execution subExecution, TryScheduleAbstract enclosingInstance) { return enclosingInstance.new CapacityAllocation(subExecution); }
    },
    VETO_ALLOCATION("veto", VetoAllocation.class) {
      public Execution wrap(Execution subExecution, TryScheduleAbstract enclosingInstance) { return enclosingInstance.new VetoAllocation(subExecution); }
    },
    BACKUP_GATE("back", BackupGate.class) {
      public Execution wrap(Execution subExecution, TryScheduleAbstract enclosingInstance) { return enclosingInstance.new BackupGate(subExecution); }
    };
    
    private final String stringRepresentation;
    private final Class<? extends Execution> executionClass;
    
    private ExecPipePart(String stringRepresentation, Class<? extends Execution> executionClass) {
      this.stringRepresentation = stringRepresentation;
      this.executionClass = executionClass;
    }
    
    public String getStringRepresentation() {
      return stringRepresentation;
    }
    
    public abstract Execution wrap(Execution subExecution, TryScheduleAbstract enclosingInstance);
    
    public static ExecPipePart fromExecution(Execution execution) {
      for (ExecPipePart part : values()) {
        if (part.executionClass.isInstance(execution)) {
          return part;
        }
      }
      return null;
    }
    
  }
  
  
  private static enum ExecutionPipelineConfiguration {

    CAPA_VETO_BACK(ExecPipePart.CAPACITY_ALLOCATION, ExecPipePart.VETO_ALLOCATION, ExecPipePart.BACKUP_GATE),
    VETO_CAPA_BACK(ExecPipePart.VETO_ALLOCATION, ExecPipePart.CAPACITY_ALLOCATION, ExecPipePart.BACKUP_GATE),
    BACK_CAPA_VETO(ExecPipePart.BACKUP_GATE, ExecPipePart.CAPACITY_ALLOCATION, ExecPipePart.VETO_ALLOCATION),
    BACK_VETO_CAPA(ExecPipePart.BACKUP_GATE, ExecPipePart.VETO_ALLOCATION, ExecPipePart.CAPACITY_ALLOCATION);
    
    
    private final ExecPipePart[] parts;
    private final String stringRepresentation;
    
    private ExecutionPipelineConfiguration(ExecPipePart... parts) {
      this.parts = parts;
      String s = "";
      for (int i = 0; i < parts.length; i++) {
        s += parts[i].getStringRepresentation();
        if (i+1<parts.length) {
          s += ">";
        }
      }
      stringRepresentation = s;
    }
    
    public Execution buildPipeline(TryScheduleAbstract enclosingInstance) {
      Execution pipelineRoot = enclosingInstance.new Schedule();
      for (int i = parts.length - 1; i >= 0; i--) {
        pipelineRoot = parts[i].wrap(pipelineRoot, enclosingInstance);
      }
      return enclosingInstance.new TimeConstraint( pipelineRoot );
    }
    
    public String getStringRepresentation() {
      return stringRepresentation;
    }
    
    public static ExecutionPipelineConfiguration getDefaultExecutionConfiguration() {
      return CAPA_VETO_BACK;
    }
    
    public static ExecutionPipelineConfiguration fromString(String stringRepresentation) {
      if (stringRepresentation != null) {
        String lowerCasedRepresentation = stringRepresentation.toLowerCase();
        for (ExecutionPipelineConfiguration pipeline : values()) {
          if (pipeline.stringRepresentation.equals(lowerCasedRepresentation)) {
            return pipeline;
          }
        }
      }
      throw new IllegalArgumentException("Configuration " + stringRepresentation + " not supported.");
    }
    
    
    public static ExecutionPipelineConfiguration fromEnforceUrgencyingString(String booleanString) {
      boolean enforceUrgency = Boolean.parseBoolean(booleanString);
      if (enforceUrgency) {
        return CAPA_VETO_BACK;
      } else {
        return BACK_CAPA_VETO;
      }
    }
    
    
    public static ExecutionPipelineConfiguration fromPipeline(Execution pipeline) {
      StringBuilder sb = new StringBuilder();
      if (pipeline instanceof TimeConstraint) {
        while (!(pipeline.subExecution instanceof Schedule)) {
          pipeline = pipeline.subExecution;
          ExecPipePart part = ExecPipePart.fromExecution(pipeline);
          if (part == null) {
            break;
          }
          sb.append(part.getStringRepresentation());
          if (!(pipeline.subExecution instanceof Schedule)) {
            sb.append(EXECUTION_PIPELINE_SEPERATOR);
          }
        }
        return fromString(sb.toString());
      }
      return getDefaultExecutionConfiguration();
    }
    
    
    public boolean isUrgencyEnforcing() {
      return parts[parts.length - 1] == ExecPipePart.BACKUP_GATE;
    }
    
  }
  
  
  private class BooleanBasedExecutionPipelineBuilder implements Builder<Execution> {

    public Execution fromString(String string)
                    throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
      try {
        ExecutionPipelineConfiguration configuration = ExecutionPipelineConfiguration.fromEnforceUrgencyingString(string);
        Execution pipeline = configuration.buildPipeline(TryScheduleAbstract.this);
        batchTimingCapVetoSchedule = new Batch(pipeline);
        return pipeline;
      } catch (IllegalArgumentException e) {
        throw new com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException(e);
      }
    }

    public String toString(Execution value) {
      ExecutionPipelineConfiguration configuration = ExecutionPipelineConfiguration.fromPipeline(value);
      if (configuration.isUrgencyEnforcing()) {
        return Boolean.TRUE.toString();
      } else {
        return Boolean.FALSE.toString();
      }
    }
    
  }
  
  
}
