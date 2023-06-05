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
package com.gip.xyna.xprc.xfractwfe.base.parallel;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.parallel.ParallelExecutor;
import com.gip.xyna.utils.parallel.ParallelTask;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.XynaExecutor.ExecutionThreadPoolExecutorWithDecreasingPrio;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_PARALLEL_EXEC_FAILED;
import com.gip.xyna.xprc.xfractwfe.ProcessAbortedException;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xpce.ordersuspension.ProcessSuspendedException;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.ResumableParallelExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Forced;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause_Multiple;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;


/**
 *
 */
public class FractalWorkflowParallelExecutor<S extends Step> implements ResumableParallelExecutor, Serializable {
  
  private static final long serialVersionUID = 1L;
  private static final Set<String> ALL_UNTREATED_LANE_IDS;
  public static final Set<String> ALL_SUSPENDED_LANE_IDS;
  static {
    HashSet<String> set1 = new HashSet<String>();
    set1.add(FractalWorkflowParallelExecutorData.ALL_UNTREATED);
    ALL_UNTREATED_LANE_IDS = Collections.unmodifiableSet(set1);
    HashSet<String> set2 = new HashSet<String>();
    set2.add(FractalWorkflowParallelExecutorData.ALL_SUSPENDED); 
    ALL_SUSPENDED_LANE_IDS = Collections.unmodifiableSet(set2);
  }
  
  private static Logger logger = CentralFactoryLogging.getLogger(FractalWorkflowParallelExecutor.class);
  
  
  protected transient ParallelExecutor parallelExecutor;
  protected String parallelExecutorId;
  private FractalWorkflowParallelExecutorData<S> data;
  protected int executionCount = 0;
  protected int priorityThreshold;
  private transient XynaTaskConsumerPreparator xynaTaskConsumerPreparator;
  protected transient ParallelismLimitation<S> parallelismLimitation;
  protected Long orderId;
  private transient ResumeHandler resumeHandler;
  private transient boolean canceled;
  private boolean compensating = false;
  
  public FractalWorkflowParallelExecutor(String parallelExecutorId, S[] steps ) {
    this(parallelExecutorId, Arrays.asList(steps) );
  }
  
  public FractalWorkflowParallelExecutor(String parallelExecutorId, List<S> steps ) {
    //id eindeutig machen
    this.parallelExecutorId = parallelExecutorId + createParallelIndexExtension(steps);
    this.data = new FractalWorkflowParallelExecutorData<S>( createSuspendableParallelTasks(steps) );
    this.resumeHandler = new ResumeHandler();
  }  
  
  
  private String createParallelIndexExtension(List<S> steps) {
    if (steps == null || steps.size() == 0) {
      return "";
    }
    //indices ohne die aktuellen indizes (falls this = PE von foreach). deshalb parent vom parent ermitteln. parent = PE-step. parent vom parent = aufrufender step
    Step step = steps.get(0).getParentStep().getParentStep();
    if (step == null || !(step instanceof FractalProcessStep)) {
      return "";
    }
    Integer[][] indices = FractalProcessStep.calculateForEachIndices((FractalProcessStep<?>) step);
    if (indices[0] == null || indices[0].length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i<indices[0].length; i++) {
      if (indices[1][i] == 1) {
        //parallel
        sb.append(".").append(indices[0][i]);
      } 
      //else: seriell - ignorieren
    }
    return sb.toString();
  }


  public interface ParallelismLimitation<S extends Step> {

    /**
     * Übergabe des ParallelExecutor zur Verwendung im ParallelismLimitationAlgorithm
     * @param parallelExecutor
     */
    void setParallelExecutor(ParallelExecutor parallelExecutor);
    
    /**
     * Task wurde suspendiert
     * @param suspendableParallelTask
     * @param suspendedException
     */
    void handleProcessSuspendedException(SuspendableParallelTask<S> suspendableParallelTask,
                                         ProcessSuspendedException suspendedException);

    /**
     * Task soll resumt werden: Wiedereinstellen in den ParallelExecutor 
     * @param taskToResume
     */
    void addTaskToParallelExecutor(SuspendableParallelTask<S> taskToResume);

    /**
     * Wartet, bis Limitation != 0 ist (es dürfen wieder Tasks ausgeführt werden)
     * @return true: !=0; false: Abbruch des Wartens
     */
    boolean awaitLimitationNotZero();
    
  }

  
  public FractalWorkflowParallelExecutor<S> init(XynaProcess process, 
                   ParallelismLimitation<S> parallelismLimitation ) {
    this.orderId = process.getCorrelatedXynaOrder().getId();
    this.priorityThreshold = Integer.MIN_VALUE;
    
    XynaOrderServerExtension order = process.getCorrelatedXynaOrder();
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().
      getSuspendResumeManagement().addParallelExecutor(orderId, order, this);
    
    //Anpassung XynaTaskConsumerPreparator
    xynaTaskConsumerPreparator = new XynaTaskConsumerPreparator();
    
    //Anpassung BeforeAndAfterExecution
    xynaTaskConsumerPreparator.setProcess(process);
    boolean addOrderContext = XynaFactory.getInstance().getProcessing().getXynaProcessingODS()
        .getOrderContextConfiguration()
        .isDestinationKeyConfiguredForOrderContextMapping(order.getDestinationKey(), false);
    
    if( addOrderContext ) {
      xynaTaskConsumerPreparator.createOrderContextMapping(process.getCorrelatedXynaOrder().getOrderContext());
    }
    String ndcString = order.getLoggingDiagnosisContext(XynaProperty.XYNA_CREATE_LOG4J_DIAG_CONTEXT.get() );
    xynaTaskConsumerPreparator.createLoggingContext(ndcString);
    
    if( parallelismLimitation == null ) {
      this.parallelismLimitation = new NoParallelismLimitation<S>();
    } else {
      this.parallelismLimitation = parallelismLimitation;
    }
    if (process.isProcessAborted()) {
      cancel();
    } else {
      canceled = false;
    }
    
    return this;
  }
  
  private List<SuspendableParallelTask<S>> createSuspendableParallelTasks(List<S> steps) {
    List<SuspendableParallelTask<S>> suspendableParallelTasks = new ArrayList<SuspendableParallelTask<S>>();
    for( int i=0; i<steps.size(); ++i) {
      S step = steps.get(i);
      String laneId = parallelExecutorId+"-"+i;
      step.setLaneId(laneId);
      SuspendableParallelTask<S> spt = new SuspendableParallelTask<S>(laneId,step,this);      
      suspendableParallelTasks.add( spt );
    }
    return suspendableParallelTasks;
  }
  
  
  public void execute() throws XynaException {
    parallelExecutor = createParallelExecutor();
    //gleichzeitige Resumes akzeptieren
    resumeHandler.acceptResumes(); 
    
    Set<String> laneIds = ALL_UNTREATED_LANE_IDS;
    if( executionCount != 0 ) {
      laneIds = getLaneIdsFromSuspendResumeManagement(orderId);
    }  

    data.addTasksToParallelExecutor(parallelExecutor, laneIds);

    if (logger.isDebugEnabled()) {
      logger.debug("Executing PE " + orderId + ", " + parallelExecutorId + ", laneIds=" + laneIds + ", #tasks=" + parallelExecutor.size());
      logger.debug("saved tasks=" + data);
    }

    ++executionCount;
    
    //Starten des ParallelExecutor und auf vollständige Bearbeitung warten
    do {
      //gleichzeitige Resumes verweigern
      //es kann gerade wieder ein Resume gestartet worden sein
      waitForParallelExecutor();
    } while (!resumeHandler.denyResumes()); 

    //1) //Evtl. konnten normale Aufträge nicht bearbeitet werden, weil keine Threads mehr laufen durften. 
    //Die zugehörigen LaneIds werden nun für den nächsten Start des ParallelExecutors gespeichert 
    data.changeLowPriorityTasksToUntreated(parallelExecutor, SuspendableParallelTask.PRIORITY_RESUME);

    //noch einmal warten, damit auch die letzten Resumes fertig sind
    waitForParallelExecutor();
    
    //nun läuft kein Auftrag mehr
    
    //2) Falls Suspendierungen vorliegen, wird eine SuspendedException geworfen.
    if( data.hasSuspendedTasks() ) {
      handleSuspensions();
    }
    //logger.info("FractalWorkflowParallelExecutor " + parallelExecutor.isExecuting() );
    
    //3) Sicherstellen, dass alle Tasks gelaufen sind:
    if( ! data.checkAllTasksHasFinished() ) {
      if( ! canceled ) {
        logger.warn("FractalWorkflowParallelExecutor has no suspensions, but not all tasks are finished:" + parallelExecutor.isExecuting() );
        logger.warn( data.getTasks() + " " + data.checkAllTasksHasFinished());
        throw new ProcessSuspendedException(new SuspensionCause_Forced()); //FIXME was nun?
      } else {
        //Im gecancelten Zustand ist es normal, dass manche Tasks nicht bearbeitet worden sind
      }
    }

    handleParallelExecutorFinished();
    
    //4) Alle Tasks sind nun beendet, daher gesammelte Exceptions weiterwerfen oder erfolgreich beenden
    handleExceptions();
    
  }

  //von tests überschrieben
  protected void handleParallelExecutorFinished() {
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().handleParallelExecutorFinished(orderId, this);
  }

  public void compensate() throws XynaException {
    if( ! compensating ) {
      data.compensate();
      executionCount = 0;
      compensating = true;
    }
    execute();
  }
  
  @Override
  public String toString() {
    return "FractalWorkflowParallelExecutor("+parallelExecutorId+","+data.getTasks().size()+")";
  }
  
  
  /**
   * 
   */
  private void waitForParallelExecutor() {
    boolean executeAgain = false;
    do {
      try {
        parallelExecutor.executeAndAwait();
      } catch (InterruptedException e) {
       // muss hier nicht abgebrochen werden?? oder kann man sich wirklich immer darauf verlassen, dass alle steps fertig werdne
        //weiterwarten
      }
      if( parallelExecutor.isExecuting() ) {
        if( logger.isDebugEnabled() ) {
          logger.debug("parallelExecutor isExecuting");
        }
        executeAgain = true;
      } else {
        if( parallelExecutor.size() != 0 && parallelExecutor.hasExecutableTasks() ) {
          //es gibt noch Tasks, die ausgeführt werden müssen
          if( logger.isDebugEnabled() ) {
            logger.debug("parallelismLimitation="+parallelismLimitation );
          }
          executeAgain = parallelismLimitation.awaitLimitationNotZero();
        } else {
          //keine Tasks mehr, daher Ende
          executeAgain = false; 
        }
      }
      if( logger.isDebugEnabled() ) {
        logger.debug("executeAgain="+executeAgain );
      }
    } while( executeAgain );
  }

  protected Set<String> getLaneIdsFromSuspendResumeManagement(long orderId) {
    return XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().
    getSuspendResumeManagement().getLaneIdsToResume(orderId, this);
  }


  /**
   * @return
   */
  protected ParallelExecutor createParallelExecutor() {
    if( parallelExecutor == null ) {
      this.parallelExecutor = new ParallelExecutor(new ExecutionThreadPoolExecutorWithDecreasingPrio(Thread.currentThread().getPriority()));
    } else {
      //Dies ist anscheinend ein Subworkflow, dessen Parent nicht suspendiert wurde, daher ist der 
      //ParallelExecutor nicht durch die Serialisierung verloren gegangen
    }
    parallelExecutor.setPriorityThreshold(priorityThreshold);
    
    xynaTaskConsumerPreparator.setMainThread(Thread.currentThread());
    parallelExecutor.setTaskConsumerPreparator(xynaTaskConsumerPreparator);
    
    parallelismLimitation.setParallelExecutor(parallelExecutor);
    return parallelExecutor;
  }

  /**
   * @throws XynaException 
   * 
   */
  private void handleSuspensions() {
    if( logger.isDebugEnabled() ) {
      logger.debug("handleSuspensions("+data+")" );
    }
    
    //Zuerst müssen Suspendierungen behandelt werden: Falls Suspendierungen vorliegen, wird eine SuspendedException
    //geworfen. Die gefangenen Exception bleiben gespeichert, bis alle Lanes komplett ausgeführt worden sind
    if( data.hasSuspendedTasks() ) {
      SuspensionCause sc = data.combinedSuspensionCauses();
      ProcessSuspendedException suspendedException = new ProcessSuspendedException(sc);
      if (logger.isDebugEnabled()) {
        int size = 1;
        if( sc instanceof SuspensionCause_Multiple ) {
          size = ((SuspensionCause_Multiple)sc).size();
        }
        logger.debug("Found "+size+" suspended tasks, throwing SuspendedException "+suspendedException );
      }
      throw suspendedException;
    }
  }
  
  private void handleExceptions() throws XynaException {
    //es gibt keine Suspendierung, daher nun gefangene Exceptions werfen
    if( data.hasThrowables() ) {
      throw combinedThrowables( data.getThrowables(), data.getXynaExceptions() );
    }
    if( data.hasXynaExceptions() ) {
      throw combinedXynaExceptions( data.getXynaExceptions() );
    }
  }


    


  /**
   * @param throwables
   * @param xynaExceptions
   * @return
   */
  private RuntimeException combinedThrowables(List<Throwable> throwables, List<XynaException> xynaExceptions) {
    ProcessAbortedException pae = findProcessAbortedException(throwables);
    if( pae != null ) {
      return pae;
    }
    throwables.addAll(xynaExceptions);
    RuntimeException re = new RuntimeException("Error during parallel execution",
                               new XPRC_PARALLEL_EXEC_FAILED().initCauses(throwables
                                   .toArray(new Throwable[throwables.size()])));
    return re;
  }
  
  /**
   * @param throwables
   * @return
   */
  private ProcessAbortedException findProcessAbortedException(List<Throwable> throwables) {
    ProcessAbortedException pae = null;
    for (Throwable t : throwables) {
      if (t instanceof ProcessAbortedException) {
        // it is intended that if there are multiple aborts then we will throw one with cause USER_KILL
        if ((pae == null) || (((ProcessAbortedException) t).getAbortionCause() == AbortionCause.MANUALLY_ISSUED)) {
          pae = (ProcessAbortedException) t;
        }
      }
    }
    return pae;
  }

  /**
   * @param xynaExceptions
   * @return
   */
  private XynaException combinedXynaExceptions(List<XynaException> xynaExceptions) {
    if (xynaExceptions.size() == 1) {
      return xynaExceptions.get(0);
    }
    //FIXME hier "verschwinden" evtl modellierte fehler, so dass man sie danach nicht mehr fangen kann.
    XynaException xe =
        new XPRC_PARALLEL_EXEC_FAILED().initCauses(xynaExceptions.toArray(new Throwable[xynaExceptions.size()]));
    return xe;
  }
  

  /**
   * @param suspendableParallelTask
   * @param suspendedException
   * @return 
   */
  public SuspensionCause handleProcessSuspendedException(SuspendableParallelTask<S> suspendableParallelTask, ProcessSuspendedException suspendedException) {
    if( logger.isDebugEnabled() ) {
      logger.debug( suspendedException + " in Lane "+suspendableParallelTask.getLaneId() );
    }
    
    data.addSuspension( suspendableParallelTask.getLaneId(), suspendableParallelTask, suspendedException );
    handleProcessSuspendedException(suspendedException, orderId, suspendableParallelTask.getStep());
  
    parallelismLimitation.handleProcessSuspendedException(suspendableParallelTask,suspendedException);
    return suspendedException.getSuspensionCause();
  }
  
  /**
   * Zum Testen überschreibbar
   */
  protected void handleProcessSuspendedException(ProcessSuspendedException suspendedException, long orderId, S step) {
    if( step instanceof FractalProcessStep<?> ) {
      if( ((FractalProcessStep<?>)step).getProcess().getRootProcessData().shouldSuspend() ) {
        cancel();
      }
    }
    XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement().
    handleSuspensionEventInParallelStep(suspendedException, orderId, step);
  }

  public void handleXynaExceptions(SuspendableParallelTask<S> suspendableParallelTask, XynaException xynaException) {
    data.addXynaException( suspendableParallelTask.getLaneId(), xynaException);
  }

  public void handleThrowable(SuspendableParallelTask<S> suspendableParallelTask, Throwable throwable) {
    data.addThrowable( suspendableParallelTask.getLaneId(), throwable);
    Department.handleThrowable(throwable);
  }

 
 
  public ResumeState resumeTask(String laneId) {
    return resumeHandler.resumeTask(laneId);
  }


  private class ResumeTask implements ParallelTask {

    private SuspendableParallelTask<S> taskToResume;

    public ResumeTask(SuspendableParallelTask<S> taskToResume) {
      this.taskToResume = taskToResume;
    }

    public int getPriority() {
      return SuspendableParallelTask.PRIORITY_RESUME;
    }

    public void execute() {
      while( ! taskToResume.isSuspended() && ! taskToResume.hasFinished() ) {
        try {
          Thread.sleep(3);
        } catch (InterruptedException e) {
          //dann halt kürzer warten
        }
      }
      if( taskToResume.isSuspended() ) {
        parallelismLimitation.addTaskToParallelExecutor(taskToResume);
      } else {
        //FIXME
        logger.error( "ResumeTask failed to resume "+taskToResume );
      }
    }
    
  }
  

  private class ResumeHandler {

    AtomicInteger currentResumes = new AtomicInteger();
    AtomicBoolean acceptResumes = new AtomicBoolean(false);
    
    public void acceptResumes() {
      //acceptResumes auf true setzen
      acceptResumes.compareAndSet(false, true);
    }

    public boolean denyResumes() {
      //acceptResumes auf false setzen, aber nur, wenn currentResumes == 0 ist
      while( currentResumes.get() == 0 ) {
        acceptResumes.compareAndSet(true, false);
        if( currentResumes.get() == 0 ) {
          return true;
        } else {
          acceptResumes.compareAndSet(false, true);
        }
      }
      return false;
    }

    
    public ResumeState resumeTask(String laneId) {
      if( ! acceptResumes.get() ) {
        return ResumeState.NotRunning;
      }
      currentResumes.getAndIncrement();
      if( ! acceptResumes.get() ) {
        currentResumes.getAndDecrement();
        return ResumeState.NotRunning;
      }
      //Resumes werden akzeptiert, currentResumes ist inkrementiert. Daher kann Resume beginnen
      try {
        return resumeTaskInternally(laneId);
      } finally {
        currentResumes.getAndDecrement();
      }
    }
    
    private ResumeState resumeTaskInternally(String laneId) {
      SuspendableParallelTask<S> taskToResume = data.removeSuspendedTask(laneId);
      if( taskToResume == null ) {
        return ResumeState.NotFound;
      }
      if( logger.isDebugEnabled() ) {
        logger.debug( "resumeTaskInternally "+taskToResume );
      }
    
      ParallelTask taskToStart = null;
      
      if( taskToResume.isSuspended() ) {
        taskToStart = taskToResume;
        parallelismLimitation.addTaskToParallelExecutor(taskToResume);
      } else {
        //Task ist noch nicht fertig suspendiert, soll aber resumt werden
        //Daher muss nun kurz gewartet werden mit dem Einstellen. Gleichzeitig soll aber der 
        //ParallelExecutor am laufen gehalten werden.
        taskToStart = new ResumeTask(taskToResume);
        parallelExecutor.addTask( taskToStart );
      }

      try {
        parallelExecutor.execute();
      } catch( RejectedExecutionException e ) {
        if( parallelExecutor.isExecuting() ) {
          //ignorieren, der Task wird noch ausgeführt werden
        } else {
          //hier nicht warten. weil weiter oben im stack locks gehalten werden
          return ResumeState.ParallelExecutorOverloaded;
        }
      }
      return ResumeState.Resumed;
    }

    
  }

  
  public Long getOrderId() {
    return orderId;
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.ParallelExecutor#getParallelExecutorId()
   */
  public String getParallelExecutorId() {
    return parallelExecutorId;
  }
  
  
  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xpce.ordersuspension.interfaces.ResumableParallelExecutor#await()
   */
  public boolean await() throws InterruptedException {
    if( parallelExecutor != null ) {
      parallelExecutor.await();
      return true;
    }
    return false;
  }
 
  
  /**
   * Abbrechen der weiteren Task-ausführung, nur Resumes sind noch zulässig
   */
  public void cancel() {
    this.priorityThreshold = SuspendableParallelTask.PRIORITY_RESUME;
    if( parallelExecutor != null ) {
      if( logger.isDebugEnabled() ) {
        logger.debug("cancel "+ parallelExecutor );
      }
      parallelExecutor.setPriorityThreshold(priorityThreshold);
    } else {
      logger.info("could not cancel non existing parallelExecutor, set priorityThreshold for later execution" );
    }
    canceled = true;
  }

  
  /**
   * Mitteilung, welche threads hängen.
   * @param hangingThreads
   * @return mainthread, falls mainthread hängt
   */
  public Thread isMainThreadHanging(Set<Thread> hangingThreads) {
    if( parallelExecutor.isExecuting() ) {
      Thread t = xynaTaskConsumerPreparator.getMainThread();
      if( t == null ) {
        return null;
      }
      if( hangingThreads.contains(t) ) {
        return t;
      }
    }
    return null;
  }

  /**
   * TODO hasStarted oder isRunning?
   * @param pt
   * @return
   */
  public boolean hasStarted(ParallelTask pt) {
    if( pt instanceof SuspendableParallelTask ) {
      @SuppressWarnings("unchecked")
      SuspendableParallelTask<S> spt = (SuspendableParallelTask<S>)pt;
      return spt.hasStarted();
    } else {
      return false; //Dieser FractalWorkflowParallelExecutor startet nur SuspendableParallelTasks
    }
  }

  public boolean startTaskWithNewThread(ParallelTask task) {
    if( parallelExecutor == null ) {
      return false;
    }
    parallelExecutor.addTask(task);
    try {
      parallelExecutor.execute();
      return true;
    } catch( RejectedExecutionException e ) {
      return false;
    }
  }

  
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    this.resumeHandler = new ResumeHandler();
  }


  private void writeObject(ObjectOutputStream s) throws IOException {
    s.defaultWriteObject();
  }


}
