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
package com.gip.xyna.xprc.xpce.ordersuspension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.RetryExecutor;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.base.parallel.FractalWorkflowParallelExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.Lane.LanePart;
import com.gip.xyna.xprc.xpce.ordersuspension.SRInformation.SRState;
import com.gip.xyna.xprc.xpce.ordersuspension.SRInformationCache.ResumeLockedException;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement.BackupFailedAction;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionResult;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.ResumableParallelExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.ResumableParallelExecutor.ResumeState;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.Step;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


/**
 * SuspendResumeAlgorithm führt die Suspends und Resumes durch. Es ist so implementiert, dass es 
 * unabhängig von der XynaFactory in JUnit-Tests lauffähig ist, indem alle Abhängigkeiten 
 * entweder über Interfaces oder die generischen Typen abgegeben werden. Besonders wichtig ist da die 
 * Implementierung des Interfaces {@link SuspendResumeAdapter}. Generische Typen mussten 
 * verwendet werden, damit die XynaOrderServerExtension und die ODSConnection kein Interface 
 * implementieren müssen.
 * 
 * Ein möglicher Ablauf für einen Aufruf:
 * <ul>
 * <li>Der Auftrag startet und führt {@link #addStartedOrder(Long, Object) addStartedOrder} aus. 
 *     Durch das Anlegen des SRInformation-Eintrags ist der Auftrag nun als laufend bekannt.</li>
 * <li>Der Auftrag führt eine Parallelität aus und ruft zu Beginn 
 *     {@link #addParallelExecutor(Long, Object, ResumableParallelExecutor) addParallelExecutor} auf.
 *     Der ParallelExecutor wird für Resumes in der SRInformation hinterlegt.</li>
 * <li>In dieser Parallelität wird eine ProcessSuspendedExcpetion geworfen: Über 
 *     {@link #handleSuspensionEventInParallelStep(ProcessSuspendedException, Long, Step) handleSuspensionEventInParallelStep}
 *     wird der ParallelExecutor für Resumes in der SRInformation hinterlegt.</li>
 * <li>Ein {@link #resume(ResumeTarget) resume} findet nun die Parallelität und 
 *     kann die eine Lane fortführen.</li>
 * <li>Eine weitere Suspendierung wird von der Parallelität weitergeworfen, sie führt zum Aufruf
 *     {@link #handleSuspensionEvent(ProcessSuspendedException, Long, Object, boolean) handleSuspensionEvent}.
 *     Hier wird nun die Suspendierung vorbereitet, da keine weiteren Resumes eingegangen sind. 
 *     {@link #cleanupSuspensionData(Long) cleanupSuspensionData} entfernt den SRInformation-Eintrag.</li>
 * <li>Eine weiteres {@link #resume(ResumeTarget) resume} muss den Auftrag neu in 
 *     den Scheduler einstellen, da der Auftrag nicht mehr läuft (kein SRInformation-Eintrag gefunden).
 *     Ein neuer SRInformation-Eintrag wird angelegt.</li>
 * <li>Der Auftrag führt die Parallelität erneut aus und ruft zu Beginn (nach addParallelExecutor)
 *     {@link #getLaneIdsToResume(Long, ResumableParallelExecutor) getLaneIdsToResume} auf, nun kann 
 *     er die noch fehlenden Lanes erneut ausführen.</li> 
 * <li>Der Auftrag kann fertig ausgeführt werden. Im Cleanup wird der SRInformation-Eintrag über
 *     {@link #cleanupSuspensionData(Long) cleanupSuspensionData} wieder gelöscht</li> 
 * </ul>
 * 
 * 
 */
public class SuspendResumeAlgorithm<C,O> {

  private static Logger logger = Logger.getLogger(SuspendResumeAlgorithm.class);
  
  static DebugConcurrency testConcurrency = null; //Nur zum Testen des Verhaltens bei Concurrency

  private static final Pair<Boolean, List<ResumeTarget>> SUSPENDED = Pair.of( Boolean.TRUE, Collections.<ResumeTarget>emptyList() );
  private static final Pair<Boolean, List<ResumeTarget>> NOT_SUSPENDED = Pair.of( Boolean.FALSE, Collections.<ResumeTarget>emptyList() );
   
  /**
   * Nur zum Testen des Verhaltens bei Concurrency: 
   * hierüber können künstlich Race-Conditions erzeugt werden
   */
  static interface DebugConcurrency {
    void resume();
    void suspend();
    void suspendInParallel();
  }
  
  /**
   * Temporärer Speicher der SuspendResume-Informationen {@link SRInformation}. Dieser Speicher ist derzeit 
   * das einzige Wissen der XynaFactory über laufende Aufträge. Siehe auch LAZY_SRINFORMATION_CREATION
   */
  private SRInformationCache<C,O> srInformationCache;
  private SuspendResumeAdapter<C,O> srAdapter;
  
  public SuspendResumeAlgorithm(SuspendResumeAdapter<C,O> srAdapter) {
    this.srAdapter = srAdapter;
    srInformationCache = new SRInformationCache<C,O>(srAdapter);
    srAdapter.setSuspendResumeAlgorithm(this);
  }
  
  protected void shutdown() throws XynaException {
    Collection<RootSRInformation<O>> rootOrderInfo = srInformationCache.getSuspendedRootOrderInformation();
    Collection<O> orders = CollectionUtils.transform(rootOrderInfo, new Transformation<RootSRInformation<O>, O>() {
      public O transform(RootSRInformation<O> from) {
        return from.getOrder();
      }
    });
    srAdapter.writeOrders((C)null, orders);
  }
  
  @Override
  public String toString() {
    return "SuspendResumeAlgorithm("+srInformationCache.toString()+")";
  }
  
  /**
   * @return the srInformationCache
   * package private
   */
  SRInformationCache<C, O> getSRInformationCache() {
    return srInformationCache;
  } 
  
  /**
   * Workflow ist gestartet, dies wird nun in die srInformations eingetragen.
   * @param orderId
   * @param order
   */
  public void addStartedOrder(Long orderId, O order) {
    SRInformation srInformation = srInformationCache.getOrCreateLocked(orderId, order, SRState.Running);
    srInformation.unlock();
  }

  /**
   * Workflow ist beendet, daher aus den srInformations austragen
   * @param orderId
   */
  public void cleanupSuspensionData(Long orderId) {
    if( logger.isDebugEnabled() ) {
      logger.debug("remove srInformations "+orderId);
    }
    srInformationCache.remove(orderId);
  }
  
  /**
   * Der ParallelExecutor wird für Resumes in der SRInformation hinterlegt.
   * @param order
   * @param parallelExecutor
   */
  public void addParallelExecutor(Long orderId, O order, ResumableParallelExecutor parallelExecutor) {
    if( logger.isDebugEnabled() ) {
      logger.debug("addParallelExecutor " + parallelExecutor.getParallelExecutorId());
    }
    SRInformation srInformation = srInformationCache.getOrCreateLocked(orderId, order, SRState.Running);
    try {
      srInformation.addParallelExecutor(parallelExecutor);
    } finally {
      srInformation.unlock();
    }
  }

  
  /**
   * Der ParallelExecutor wird für Resumes in der SRInformation hinterlegt. FIXME raus?
   * @param e
   * @param step
   */
  public void handleSuspensionEventInParallelStep(ProcessSuspendedException e, Long orderId, Step step) {
    if (testConcurrency != null) {
      testConcurrency.suspendInParallel();
    }
    if (logger.isDebugEnabled()) {
      logger.debug("handleSuspensionEventInParallelStep " + orderId + ", " + step.toString() + ", " + e.getSuspensionCause());
    }
  }
  
  
  /**
   * Der Workflow soll suspendiert werden. Da in der Zwischenzeit Resumes eingegangen sein können,
   * kann es sein, dass der Workflow sofort fortgesetzt werden kann. In diesem Fall wird er sofort
   * wieder dem Scheduler übergeben.
   * @param suspendedException
   * @param orderId
   * @param order
   * @return
   */
  @SuppressWarnings("unchecked")
  public Pair<Boolean,List<ResumeTarget>> handleSuspensionEvent(ProcessSuspendedException suspendedException, Long orderId, O order, boolean resumeAllowed) {
    if( testConcurrency != null ) testConcurrency.suspend();
    final SRInformation srInformation = srInformationCache.getOrCreateLocked(orderId, order, SRState.Running);
    try {
      srInformation.setState(SRState.Suspending);
      srInformation.setSuspensionCause(suspendedException.getSuspensionCause());
      //Mit dem Lock in den SRInformation und dem Umtragen des States nach Suspending
      //ist sichergestellt, dass keine weiteren Resumes zeitgleich erfolgen können.
      //Alle Resumes, die noch eingegangen sind, haben keine ausführbaren ParallelExecutoren
      //gefunden und ihre Lanes nur gesammelt.
            
      if( logger.isDebugEnabled() ) {
        logger.debug( "srInformation = "+srInformation +", SuspendedException "+suspendedException );
      }
      
      //Liegen noch wartende Resumes vor?
      boolean rescheduleImmediately = srInformation.getResumedLanes().areAnyLanesToBeResumed();
      
      if( rescheduleImmediately && resumeAllowed ) {
        //es gab Resumes, daher sollte der Auftrag fortgesetzt werden
        srInformation.setState(SRState.Resuming); //Resuming muss fortgesetzt werden
        //die resumten Einträge in srInformation.lanesToResume bleiben bestehen, da die Workflow-Ausführung 
        //genau bei diesen Schritten fortfahren muss
        if( logger.isDebugEnabled() ) {
          logger.debug("readding workflow "+order.toString());
        }
        srAdapter.rescheduleOrder(order);
        return NOT_SUSPENDED;
      } else {
  
        try {
          SuspensionCause suspensionCause = suspendedException.getSuspensionCause();
          boolean backup = suspensionCause.getOrderBackupMode() == null || suspensionCause.getOrderBackupMode().doBackup();
          RootOrderSuspension rootOrderSuspension = RootSRInformation.getRootOrderSuspension(srInformation);
          
          srAdapter.suspendOrder(order, rootOrderSuspension, suspensionCause, backup);
          
          if (backup) {
            cleanupSuspensionData(orderId);
            if( rootOrderSuspension != null ) {
              rootOrderSuspension.setSuspended();
            }
            srInformation.setState(SRState.Invalid); //srInformation muss neu gelesen werden
          } else {
            keepSuspendedOrderInMemory(order, srInformation);
            srInformation.setState(SRState.Suspended);
          }
          
        } catch( PersistenceLayerException e ) { //TODO weitere Exceptions?
          logger.warn("Order " + orderId + " was not suspended successfully. Cleaning up SRInformation.", e );
          BackupFailedAction backupFailedAction = SuspendResumeManagement.SUSPEND_RESUME_BACKUP_FAILED_ACTION.get();
          switch( backupFailedAction ) {
            case KeepRunning:
              //Suspendierung hat nicht funktioniert, aber nicht die referenz auf den auftrag ganz verlieren
              keepSuspendedOrderInMemory(order, srInformation);
              srInformation.setState(SRState.Suspended);
              break;
            case Abort:
              logger.warn("abort order "+srInformation);
              srInformation.setState(SRState.Suspended);
              srAdapter.abortOrderSuspension((RootSRInformation<O>)srInformation, orderId, e);
              return NOT_SUSPENDED;
            default:
              logger.warn( "Unexpected BackupFailedAction "+backupFailedAction+", try KeepRunning" );
              //Suspendierung hat nicht funktioniert, aber nicht die referenz auf den auftrag ganz verlieren
              keepSuspendedOrderInMemory(order, srInformation);
              srInformation.setState(SRState.Suspended);
              break;
          }
        }
        
        if( rescheduleImmediately ) {
          return Pair.of( Boolean.TRUE, srInformation.getResumeTargets() );
        } else {
          return SUSPENDED;
        }
      }
    } finally {
      srInformation.unlock();
    }
  }
  
  

  @SuppressWarnings("unchecked")
  private void keepSuspendedOrderInMemory(O order, SRInformation srInformation) {
    if ( srInformation instanceof RootSRInformation ) {
      //Referenz auf Order strong machen, damit Order nicht verloren geht
      if (!((RootSRInformation<O>)srInformation).convertOrderReferenceToStrong()) {
        ((RootSRInformation<O>)srInformation).setOrder(order);
      }
    }
  }
  
  public Pair<ResumeResult,String> resume(O order, C con) throws PersistenceLayerException {
    Long orderId = srAdapter.getOrderId(order);
    ResumeTarget target = new ResumeTarget(orderId, orderId, null);
    RootSRInformation<O> rootSRInformation = srInformationCache.getOrCreateLockedRootNotInvalid(orderId, order, SRState.Suspended);
    Pair<ResumeResult,String> pair = resumeInternalLockedRootSRInformation(rootSRInformation, Arrays.asList(target), con);
    return pair;
  }
  
  /**
   * Resume des Auftrags mit angebener OrderId und Lane.
   * @param target
   * @return Pair&lt;ResumeResult,String&gt; letzteres ist optionale Begründung
   * @throws PersistenceLayerException 
   */
  public Pair<ResumeResult,String> resume(ResumeTarget target) throws PersistenceLayerException {
    RootSRInformation<O> rootSRInformation = null;
    try {
      rootSRInformation = srInformationCache.getOrCreateLockedRootNotInvalid(target);
    } catch (OrderBackupNotFoundException e) {
      //das muss kein problem sein, sondern kann z.b. im cluster daran liegen, dass der auftrag auf dem anderen knoten läuft und noch nicht fertig suspendiert ist
      logger.debug("Could not read OrderBackup for "+target, e );
      cleanupSuspensionData(target.getRootId());
      return Pair.of(ResumeResult.Failed, SuspendResumeManagement.FAILED_ORDERBACKUP_NOT_FOUND );
    } catch (ResumeLockedException e) {
      return Pair.of(ResumeResult.Unresumeable, SuspendResumeManagement.UNRESUMABLE_LOCKED);
    }
    return resumeInternalLockedRootSRInformation(rootSRInformation, Arrays.asList(target), null);
  }
  
  /**
   * Achtung: gibt Lock frei!
   * @param srInformation
   * @param target
   * @param con
   * @return
   * @throws PersistenceLayerException 
   */
  private Pair<ResumeResult,String> resumeInternalLockedRootSRInformation(RootSRInformation<O> rootSRInformation, List<ResumeTarget> targets, C con) throws PersistenceLayerException {
    boolean startOrder = false;
    try {
      //evtl. darf RootSRInformation nicht resumt werden
      Pair<ResumeResult,String> unresumableCause = rootSRInformation.checkResume(targets,srAdapter);
      if( unresumableCause.getFirst() != null ) {
        //darf nicht resumt werden
        return unresumableCause;
      }
      if( rootSRInformation.getState() == SRState.Suspended ) {
        startOrder = true;
      }
      //alle ResumeTargets eintragen
      List<Pair<ResumeTarget,String>> failedResumes = null;
      for( ResumeTarget target : targets ) {
        do {
          ResumeTargetExecutable rte = new ResumeTargetExecutable(target, rootSRInformation);
          RetryExecutor.retryMax(3).sleep(3).execute(rte);
          if( rte.getFailure() != null ) {
            //Retries haben nicht geholfen
            if( failedResumes == null ) {
              failedResumes = new ArrayList<Pair<ResumeTarget,String>>();
            }
            failedResumes.add( Pair.of(target, rte.getFailure() ) );
          }
          target = rte.getParentTarget();
        } while( target != null );
      }
      if( failedResumes != null ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Could not resume ");
        if( failedResumes.size() == 1 ) {
          Pair<ResumeTarget, String> pair = failedResumes.get(0);
          sb.append(pair.getFirst()).append(": ").append(pair.getSecond());
        } else {
          sb.append(failedResumes.size()).append(" targets: [");
          String sep = "";
          for( Pair<ResumeTarget, String> pair : failedResumes ) {
            sb.append(sep).append(pair.getFirst()).append(": ").append(pair.getSecond());
            sep =", ";
          }
          sb.append("]");
        }
        return Pair.of(ResumeResult.Failed,sb.toString());
      }
      if( startOrder ) {
        rootSRInformation.setState(SRState.Resuming);
      }
    } finally {
      rootSRInformation.unlock();
    }
    if( startOrder ) {
      if( logger.isDebugEnabled() ) {
        logger.debug("Start order "+rootSRInformation.getOrderId() +" to resume");
      }
      srAdapter.startOrder( rootSRInformation, con);
    }
    return Pair.of(ResumeResult.Resumed, null);
  }

  /**
   * Erzeugt alle ResumeTargets auf dem Pfad rückwärts von dem übergebenen ResumeTarget
   * bis zum Root und trägt alle ResumeTargets in die dabei gefundenen oder erzeugten SRInformations ein.
   */
  private class ResumeTargetExecutable implements RetryExecutor.Executable {
    private ResumeTarget target;
    private RootSRInformation<O> rootSRInformation;
    private String failure;
    private ResumeTarget parent;
    
    public ResumeTargetExecutable(ResumeTarget target, RootSRInformation<O> rootSRInformation) {
      this.target = target;
      this.rootSRInformation = rootSRInformation;
    }

    public ResumeTarget getParentTarget() {
      return parent;
    }

    public String getFailure() {
      return failure;
    }

    public boolean execute() {
      return resumeTarget();
    }

    public boolean retry(int retry) {
      return resumeTarget();
    }

    public void failed(boolean timeout) {
    }
    
    private boolean resumeTarget() {
      SRInformation srInformation = null;
      try {
        srInformation = srInformationCache.getOrCreateLockedNotInvalid(target, rootSRInformation);
      } catch( NoSuchChildException e ) {
        failure = e.getMessage();
        return true;
      }
      try {
        SRState state = srInformation.getState();
        if( logger.isDebugEnabled() ) {
          logger.debug("resumeTarget "+target+ " for srInformation "+srInformation );
        }
        Lane lane = new Lane(target.getLaneId());
        if( !state.acceptsNewLanesToResume() ) {
          //TODO was nun?
          logger.warn("Unexpected "+srInformation +" for "+target );
          failure = "Unexpected state "+state;
          return false; //Retry
        }
        if( state == SRState.Running ) {
          ResumeState resume = resumeRunning(srInformation, lane);
          if( resume == ResumeState.Resumed ) {
            return true; //laufendes Target resumt, daher müssen Parents nicht mehr resumt werden
          } else {
            failure = "resumeRunning failed "+resume;
            return false; //Retry
          }
        } else {
          srInformation.getResumedLanes().addLaneToResume(lane);
        }
        if( state == SRState.Suspended ) {
          srInformation.setState(SRState.Resuming);
        }
        parent = srInformation.getParentResumeTarget();
        return true;
      } finally {
        srInformation.unlock();
      }
    }
  
  }

  /**
   * Auftrag ist am laufen
   * Fall 1: geeigneter PE ist am laufen und Task in ihm wird gestartet
   * Fall 2: Es ist kein zu benachrichtigender PE am laufen, aber es wird dafür gesorgt, dass 
   *            - der PE beim Starten das Task als zu resumen findet und
   *            - handleSuspensionEvent sieht, dass der Auftrag wieder neu zu starten ist
   *  
   * @param srInformation
   * @param laneId
   */
  private ResumeState resumeRunning(SRInformation srInformation, Lane lane) {
    if( logger.isDebugEnabled() ) {
      logger.debug("resumeRunning "+srInformation);
    }
    if( lane.getLaneId() == null ) {
      //FIXME wieso muss laufender PE nicht benachrichtigt werden?
      
      //keine Lane angegeben (evtl. gibt es nur eine), d.h alle Lanes resumen
      srInformation.getResumedLanes().addLaneToResume(lane);
      return ResumeState.Resumed;
    } else {
      //Kann evtl. eine einzelne Lane resumt werden?
      return resumeParallelExecutor(srInformation,lane);
      //sollte eigentlich immer möglich sein. In seltenen Fällen ist der ParallelExecutor 
      //noch nicht fertig initialisiert, dann sollte das Resume nach kurzer Wartezeit nochmal probiert werden
    }
  }

  /**
   * @param srInformation 
   * @param orderId
   * @param laneId
   */
  private ResumeState resumeParallelExecutor(SRInformation srInformation, Lane lane) {
    if( logger.isDebugEnabled() ) {
      logger.debug("resumeParallelExecutor("+srInformation+", lane="+lane+")" );
    }
    
    ResumableParallelExecutor pe = null;
    ResumeState resumeState = null;
    boolean resumedLeaf = true; //leaf im sinne des baumes der PEs
    for( LanePart lp : lane.getLanePartsForParallelExecutors() ) {
      pe = srInformation.getParallelExecutor(lp.getParallelExecutorId());
      if( pe == null ) {
        if( logger.isDebugEnabled() ) {
          logger.debug("ParallelExecutor "+lp.getParallelExecutorId()+" does not exist in "+srInformation);
        }
        //PE noch nicht initialisiert, dann passiert das gleich oder es gibt eine suspendierung. in beiden fällen 
        //halten wir gerade das lock und können die lane als zu resumen im srinfo objekt hinterlegen.
        srInformation.getResumedLanes().addLaneToResume(lane);
        return ResumeState.Resumed;
      } else {
        resumeState = pe.resumeTask(lp.getLanePart());
        if( resumeState == ResumeState.Resumed ) {
          if (logger.isDebugEnabled()) {
            logger.debug("Resumed Task in ParallelExecutor " + lp.getParallelExecutorId());
          }
          if (resumedLeaf) {
            srInformation.getResumedLanes().removeLaneToResume(lane);
          } else {
            srInformation.getResumedLanes().addLaneToResume(lane);
          }
          return resumeState; //Resume hat geklappt
        } else if (resumeState == ResumeState.NotFound) {
          if (logger.isDebugEnabled()) {
            logger.debug("Task is not suspended in ParallelExecutor " + lp.getParallelExecutorId() + " any more.");
          }
          //die lane ist nicht suspended. entweder ist sie noch nicht suspendiert, oder bereits von einem anderen thread resumed worden.
          //falls sie bereits von einem anderen thread resumed wurde, ist nichts zu tun, weil im kind-PE/-auftrag haben wir bereits
          //die lane als zu resumen eingetragen (passiert beim hochhangeln).
          
          //um das unterscheiden zu können, müssten wir den state der lane speichern (resuming, suspending). das ist kompliziert.
          if (true) {
          
            //stattdessen machen wir es uns (auf kosten von etwas memory speicherverbrauch) einfach und merken uns einfach immer, dass
            //die lane resumed werden muss. falls das zu häufig passiert, ist das nicht schlimm.
            //hat zur folge, dass im logfile bei ausgaben von den resumedlanes zu viele da stehen.
            
            //andere negative folgeerscheinung ist, dass das handlesuspensionevent vom auftrag immer denkt, dass es noch etwas zu resumen gibt
            //   dann wird das resume probiert und nichts gefunden. wichtig ist, dass hierbei die resumedlanes aufgeräumt werden!
            //   bei foreach-PEs ist das nicht einfach über "getLaneIdsToResume" möglich, weil die tasks des foreaches evtl gar nicht
            //   mehr gestartet werden. => behandlung dieses falles über "handleParallelExecutorFinished()"
            srInformation.getResumedLanes().addLaneToResume(lane);
            return ResumeState.Resumed;
          } else {
            //TODO (falls code-komplexität handhabbar und den aufwand wert)
           // if (getLaneState() == LaneState.Suspending) {
              //der zustand suspending muss genau dann bekannt sein, wenn der kind-pe (oder kind-auftrag) nicht resumebar war
              // (ansonsten hätten wir uns hierhin gar nicht hingehangelt)
              /*
               *            Parent PE (hier)
               *                 |   ^
               *                 |   |
               *             (1) |   | (2)
               *                 v   |
               *            Kind PE/-Auftrag
               *                   |
               *               (3) |
               *                   v
               *                 ...
               *                 
               *      LaneState
               *        (1) resuming (oder starting, für uns uninteressant, deshalb nicht zu unterscheiden)
               *        (2) suspending (oder finishing, für uns uninteressant, deshalb nicht zu unterscheiden)
               *        (3) executingchild (uninteressant, weil in dem fall würden wir hier gar nicht auf den parent-PE schauen, weil dann hätten wir 
               *            beim hochhangeln bereits beim kind-PE/-auftrag erfolgreich abgebrochen
               *            
               *            
               * D.h. bei handleSuspensionEvent und handleSuspensionEventinParallelStep state (in parentlane) auf suspending setzen
               *  und beim resume von lanes/aufträgen die suspending-lane-infos wieder clearen.
               */          
            //  srInformation.getResumedLanes().addLaneToResume(lane);
           // }
           // return ResumeState.Resumed;
          }
        } else if (resumeState == ResumeState.ParallelExecutorOverloaded) {
          return resumeState; //retry weiter oben.
        } else {
          if( logger.isDebugEnabled() ) {
            logger.debug("Could not resume ParallelExecutor "+lp.getParallelExecutorId()+": "+ resumeState);
          }
        }
      }
      //resume hat nicht geklappt -> Parent suchen, daher weiter in der Schleife
      //falls der PE wiederkommt, fragt er in SRInformation, was er resumen soll. deshalb muss sichergestellt werden,
      //dass man die information nicht vergisst
      resumedLeaf = false;
    }
    if( pe == null ) {
      //Keine LaneParts?
      if( logger.isDebugEnabled() ) {
        logger.debug("No ParallelExecutor found to restart, retrying");
      }
      return ResumeState.NoParallelExecutorFound;
    } else {
      //Das Lock in SRInformation wird noch gehalten, d.h die Lane kann sicher eingetragen 
      //werden und wird in handleSuspensionEvent dann berücksichtigt werden.
      //Daher kann hier ResumeState.Resumed gemeldet werden
      srInformation.getResumedLanes().addLaneToResume(lane);
      return ResumeState.Resumed;
    }
  }

  
  /**
   * Liefert die LaneIds, die vom ParallelExecutor benötigt werden, um nach einem Neustart des Auftrags
   * resumte Lanes wieder auszuführen.
   * @param orderId
   * @param parallelExecutor
   * @return
   */
  public Set<String> getLaneIdsToResume(Long orderId, ResumableParallelExecutor parallelExecutor) {
    SRInformation srInformation = srInformationCache.getOrCreateLocked(orderId,SRState.Unknown);
    if( srInformation == null ) {
      return FractalWorkflowParallelExecutor.ALL_SUSPENDED_LANE_IDS;
    }
    try {
      String peId = parallelExecutor.getParallelExecutorId();
      Set<String> laneIds = srInformation.getResumedLanes().getLanesToBeResumedAndMarkAsResumed(peId);
      return laneIds;
    } finally {
      srInformation.unlock();
    }
  }
  
  
  public AbortionOfSuspendedOrderResult abortSuspendedWorkflow(O order, Long rootId, boolean ignoreResourcesWhenResuming, boolean force) throws PersistenceLayerException {
    if( logger.isDebugEnabled() ) {
      logger.debug("aborting suspended Workflow "+order +" for root "+rootId );
    }
    RootSRInformation<O> rootSRInformation = null;
    Long orderId = null;
    if( order != null ) {
      orderId = srAdapter.getOrderId(order);
      rootSRInformation = srInformationCache.getOrCreateLockedRoot(srAdapter.getRootOrder(order));
    } else {
      orderId = rootId;
      try {
        rootSRInformation = srInformationCache.getOrCreateLockedRootNotInvalid(new ResumeTarget(rootId, null, null));
      } catch (OrderBackupNotFoundException e) {
        logger.warn("Could not read order "+rootId, e );
        return AbortionOfSuspendedOrderResult.RESUME_FAILED;
      } catch (ResumeLockedException e) {
        logger.warn("Could not resume locked order "+rootId, e );
        return AbortionOfSuspendedOrderResult.RESUME_FAILED_RETRY;
      }
    }
    return abortSuspendedWorkflow(rootSRInformation,orderId,force,ignoreResourcesWhenResuming,null); //TODO cause
  }
  
  public enum AbortionOfSuspendedOrderResult {
    SUCCESS, RESUME_FAILED, RESUME_FAILED_RETRY, RESUME_FAILED_WRONG_BINDING;
  }
  
  public interface DoResume<C> {

    Pair<ResumeResult, String> resume(C con) throws PersistenceLayerException;
    
  }
  
  protected AbortionOfSuspendedOrderResult abortSuspendedWorkflow(final RootSRInformation<O> rootSRInformation, final long orderId, boolean force, boolean ignoreResourcesWhenResuming, Throwable cause) throws PersistenceLayerException {
    final AtomicBoolean rootSRInformationIsLocked = new AtomicBoolean(true);
    try {
      final long rootId = rootSRInformation.getRootId();

      SRState state = rootSRInformation.getState();
      if( state != SRState.Suspended ) {
        if (logger.isInfoEnabled()) {
          logger.info("Could not abort order "+orderId+" in state "+state+ " "+rootSRInformation);
        }
        if( ! force ) {
          return AbortionOfSuspendedOrderResult.RESUME_FAILED_RETRY;
        }
      } else {
        if( logger.isDebugEnabled() ) {
          logger.debug("rootSRInformation found "+rootSRInformation );
        }
      }

      //alle Resume-Quellen vernichten, damit kein weiteres Resume erfolgt
      Pair<ResumeResult, String> pair = srAdapter.abortSuspendedOrder(new DoResume<C>() {

        @Override
        public Pair<ResumeResult, String> resume(C con) throws PersistenceLayerException {
          try {
            return resumeInternalLockedRootSRInformation(rootSRInformation, Arrays.asList(new ResumeTarget(rootId, orderId)), con);
          } finally {
            rootSRInformationIsLocked.set(false);
          }
        }
      }, rootSRInformation, orderId, ignoreResourcesWhenResuming);

      if (pair.getFirst() == ResumeResult.Resumed) {
        return AbortionOfSuspendedOrderResult.SUCCESS;
      } else {
        logger.warn("Resume failed: " + pair);
        return pair
            .getFirst() == ResumeResult.Unresumeable ? AbortionOfSuspendedOrderResult.RESUME_FAILED : AbortionOfSuspendedOrderResult.RESUME_FAILED_RETRY;
      }

    } finally {
      if (rootSRInformationIsLocked.get()) {
        rootSRInformation.unlock();
      }
    }
  }
  
  /**
   * Suspendieren der übergebenen Root-Aufträge
   * @param suspendRootOrderData
   * @return gleiches suspendRootOrderData, mit Result-Daten befüllt
   */
  public SuspendRootOrderData suspendRootOrders(SuspendRootOrderData suspendRootOrderData) {
    if( logger.isDebugEnabled() ) {
      logger.debug( "Trying to suspend with "+suspendRootOrderData );
    }
    long started = System.currentTimeMillis();
    
    //Dafür sorgen, dass suspendierte Aufträge nicht wieder resumt werden
    srInformationCache.addUnresumeableOrders(suspendRootOrderData.getRootOrderIds());
    
    //Suspendieren aller rootOrderIds
    SuspendRootOrders suspendRootOrders = new SuspendRootOrders(suspendRootOrderData.getRootOrderIds(),
                                                                suspendRootOrderData.getSuspensionCause(),
                                                                suspendRootOrderData.isFailFast() );
    //Ausführen der Suspendierungen mit Retries
    RetryExecutor.retryUntil(started + suspendRootOrderData.getTimeout()).sleep(50).execute(suspendRootOrders);
    
    Map<Long,String> suspensionFailed = suspendRootOrders.getSuspensionFailed();
    List<RootOrderSuspension> suspending = suspendRootOrders.getSuspendingOrders();
    List<RootOrderSuspension> allOrders = suspendRootOrders.getAllOrders();
        
    if( suspensionFailed.isEmpty() ) {
      //bei allen Aufträgen konnte Suspendierung begonnen werden (oder waren bereits suspendiert oder fertig)
      
      //sind die Aufträge nun alle suspendiert?       
      if( ! suspending.isEmpty() ) {
        //es konnten nicht alle suspendiert werden
        suspending = handleSuspensionTimedOut(suspendRootOrderData, suspending, started);
      }
      
      if( suspending.isEmpty() ) {
        suspendRootOrderData.addResumeTargets(allOrders);
        suspendRootOrderData.suspensionResult(SuspensionResult.Suspended);
        switch( suspendRootOrderData.getSuspensionSucceededAction() ) {
          case None:
            //OrderBackupLocks freigeben
            srInformationCache.removeUnresumableOrders(suspendRootOrderData.getRootOrderIds());
            break;
          case KeepUnresumeable:
            break;
          default:
            logger.warn("Unexpected SuspensionSucceededAction "+suspendRootOrderData.getSuspensionSucceededAction());
            //OrderBackupLocks freigeben
            srInformationCache.removeUnresumableOrders(suspendRootOrderData.getRootOrderIds());
        }
      } else {
        if( logger.isDebugEnabled() ) {
          logger.debug( suspending.size() +" of "
              +suspendRootOrderData.getRootOrderIds().size()+" orders are not suspended yet -> "
              +suspendRootOrderData.getSuspensionFailedAction());
        }
        handleSuspensionFailed(suspendRootOrderData, allOrders, SuspensionResult.Timeout);
      }
    } else {
      //es konnten nicht alle Suspendierungen begonnen werden, da Aufträge im falschen Zustand waren
      logger.warn( suspensionFailed.size() +" of "
          +suspendRootOrderData.getRootOrderIds().size()+" orders could not be suspended -> "
          +suspendRootOrderData.getSuspensionFailedAction());
      suspendRootOrderData.failedSuspensions(suspensionFailed);
      handleSuspensionFailed(suspendRootOrderData, allOrders, SuspensionResult.Failed);
    }
    return suspendRootOrderData;
  }


  /**
   * @param suspendRootOrderData
   * @param suspendingOrders
   * @param suspensionResult
   */
  private void handleSuspensionFailed(SuspendRootOrderData suspendRootOrderData,
                                      List<RootOrderSuspension> suspendingOrders, SuspensionResult suspensionResult) {
    //Auf jeden Fall die OrderBackupLocks freigeben
    srInformationCache.removeUnresumableOrders(suspendRootOrderData.getRootOrderIds());
    
    switch( suspendRootOrderData.getSuspensionFailedAction() ) {
      case UndoSuspensions:
        //Wenn nicht alle supendiert wurden, müssen die Suspendierungen rückgängig gemacht werden
        List<Triple<RootOrderSuspension,String,PersistenceLayerException>> failedResumes = 
            srAdapter.resumeRootOrdersWithRetries(suspendingOrders);
            
        if( failedResumes.isEmpty() ) {
          suspendRootOrderData.resumeTargets(Collections.<ResumeTarget>emptyList());
          suspendRootOrderData.suspensionResult(suspensionResult);
        } else {
          //Liste mit ResumeTarget der fehlgeschlagenen Suspend/Resumes erstellen
          for( Triple<RootOrderSuspension,String,PersistenceLayerException> triple : failedResumes ) {
            suspendRootOrderData.addResumeTargets(triple.getFirst() );
          }
          suspendRootOrderData.failedResumes(failedResumes);
          suspendRootOrderData.suspensionResult(SuspensionResult.Failed);
        }
        break;
      case StopSuspending:
        for( RootOrderSuspension ros : suspendingOrders) {
          ros.continueResume(); //keine weiteren Suspendierungen mehr
          if( ros.isSuspending() ) {
            suspendRootOrderData.addSuspensionNotFinished(ros.getRootOrderId());
          }
        }
        suspendRootOrderData.addResumeTargets(suspendingOrders);
        suspendRootOrderData.suspensionResult(suspensionResult);
        break;
      case KeepSuspending:
        //kein Undo der Suspendierungen gewünscht, daher nun Rückgabe der Aufträge, die noch nicht suspendiert sind
        for( RootOrderSuspension ros : suspendingOrders) {
          if( ros.isSuspending() ) {
            suspendRootOrderData.addSuspensionNotFinished(ros.getRootOrderId());
          }
        }
        suspendRootOrderData.addResumeTargets(suspendingOrders);
        suspendRootOrderData.suspensionResult(suspensionResult);
        break;
      default:
        throw new IllegalArgumentException("Unexpected SuspensionFailedAction "+suspendRootOrderData.getSuspensionFailedAction());
    }

  }

  /**
   * @param suspendRootOrderData
   * @return
   */
  private List<RootOrderSuspension> handleSuspensionTimedOut(SuspendRootOrderData suspendRootOrderData,
                                           List<RootOrderSuspension> suspendingOrders, long startSuspension) {
    long absoluteTimeout;
    switch( suspendRootOrderData.getSuspensionTimedOutAction() ) {
      case None:
        return suspendingOrders;
      case Interrupt:
        absoluteTimeout = startSuspension + 2*suspendRootOrderData.getTimeout();
        terminateSuspendingOrders(startSuspension, suspendingOrders, false);
        return waitForSuspension(absoluteTimeout, suspendingOrders, "interrupt");
      case Stop:
        absoluteTimeout = startSuspension + 2*suspendRootOrderData.getTimeout();
        terminateSuspendingOrders(startSuspension, suspendingOrders, false);
        suspendingOrders = waitForSuspension(absoluteTimeout, suspendingOrders, "interrupt");
        if( suspendingOrders.isEmpty() ) {
          return suspendingOrders;
        }
        absoluteTimeout = startSuspension + 3*suspendRootOrderData.getTimeout();
        terminateSuspendingOrders(startSuspension, suspendingOrders,true);
        return waitForSuspension(absoluteTimeout, suspendingOrders, "stop");
      default:
        throw new IllegalArgumentException("Unexpected SuspensionTimedOutAction "+suspendRootOrderData.getSuspensionTimedOutAction());
    }
  }

  private void terminateSuspendingOrders(long startSuspension, List<RootOrderSuspension> suspendingOrders, boolean stop) {
    if( logger.isDebugEnabled() ) {
      logger.debug( "Trying to "+(stop?"stop ":"interrupt ")+suspendingOrders.size()+" orders");
    }
    for( RootOrderSuspension ros : suspendingOrders) {
      if( ros.isSuspending() ) {
        RootSRInformation<O> rootSRInformation = srInformationCache.getLockedRootOrNullIfInvalid(ros.getRootOrderId());
        if( rootSRInformation == null ) {
          continue;
        }
        try {
          srAdapter.interruptProcess( rootSRInformation, ros, stop );
        } finally {
          rootSRInformation.unlock();
        }
      }
    }
  }
  
  private List<RootOrderSuspension> waitForSuspension(long absoluteTimeout, List<RootOrderSuspension> suspendingOrders, String action) {
    SuspendRootOrders suspendRootOrders = new SuspendRootOrders(suspendingOrders);
    if( logger.isDebugEnabled() ) {
      logger.debug("waiting again for "+action);
    }
    RetryExecutor.retryUntil(absoluteTimeout).sleep(50).execute(suspendRootOrders);
    if( suspendRootOrders.getSuspendingOrders().isEmpty() ) {
      if( logger.isDebugEnabled() ) {
        logger.debug(action+" to suspend succeeded");
      }
    } else {
      logger.info(action+" to suspend failed for "+suspendRootOrders.getSuspendingOrders().size()+" orders");
    }
    return suspendRootOrders.getSuspendingOrders();
  }

  private class SuspendRootOrders implements RetryExecutor.Executable {

    private List<Long> suspensionNotStarted;   //Suspension noch nicht begonnen
    private SuspensionCause suspensionCause;
    private List<RootOrderSuspension> allOrders = null; //alle begonnenen Suspendierungen
    private List<RootOrderSuspension> suspendingOrders = null; //noch nicht fertig suspendierte
    private boolean failFast;
    private Map<Long,String> suspensionFailed = null;
     
    public SuspendRootOrders(Set<Long> ordersToSuspend, SuspensionCause suspensionCause, boolean failFast) {
      this.suspensionNotStarted = new LinkedList<Long>(ordersToSuspend);
      this.suspensionCause = suspensionCause;
      this.allOrders = new ArrayList<RootOrderSuspension>();
      this.suspendingOrders = new LinkedList<RootOrderSuspension>();
      this.failFast = failFast;
    }

    public Map<Long, String> getSuspensionFailed() {
      if( suspensionFailed == null ) {
        if( suspensionNotStarted.isEmpty() ) {
          return Collections.emptyMap();
        }
        suspensionFailed = new HashMap<Long,String>();
      }
      for( Long rootOrderId : suspensionNotStarted ) {
        suspensionFailed.put(rootOrderId, "Suspension not started");
      }
      return suspensionFailed;
    }

    public SuspendRootOrders(List<RootOrderSuspension> suspendingOrders) {
      this.suspensionNotStarted = Collections.emptyList();
      this.allOrders = Collections.emptyList();
      this.suspendingOrders = new LinkedList<RootOrderSuspension>(suspendingOrders);
    }

    public List<RootOrderSuspension> getAllOrders() {
      return allOrders;
    }

    public List<RootOrderSuspension> getSuspendingOrders() {
      return suspendingOrders;
    }

    public boolean execute() {
      if( ! suspensionNotStarted.isEmpty() ) {
        if( logger.isDebugEnabled() ) {
          logger.debug("Starting RetryExecutor to start suspension of "+suspensionNotStarted.size()+" root orders" ); 
        }
        //beim ersten Mal continueSuspension nicht ausführen
        startSuspension();
      } else {
        if( logger.isDebugEnabled() ) {
          logger.debug("Starting RetryExecutor to continue suspension of "+suspendingOrders.size()+" suspending root orders" ); 
        }
        continueSuspension();
      }
      return suspensionNotStarted.isEmpty() && suspendingOrders.isEmpty();
    }
    
    public boolean retry(int retry) {
      if( logger.isTraceEnabled() ) {
        logger.trace("retry("+suspensionNotStarted.size()+","+suspendingOrders.size()+")");
      }
      if( ! suspensionNotStarted.isEmpty() ) {
        startSuspension();
      }
      if( ! suspendingOrders.isEmpty() ) {
        continueSuspension();
      }
      return suspensionNotStarted.isEmpty() && suspendingOrders.isEmpty();
    }
    
    public void failed(boolean timeout) {
      //nichts zu tun
      if( logger.isDebugEnabled() ) {
        logger.debug("SuspendRootOrders could not suspend all root orders: suspension not started: "+suspensionNotStarted.size()
                     +", not suspended yet: "+suspendingOrders.size());
      }
    }
   
    private void startSuspension() {
      Iterator<Long> iter = suspensionNotStarted.iterator();
      while( iter.hasNext() ) {
        RootOrderSuspension rootOrderSuspension = new RootOrderSuspension(iter.next(), suspensionCause);
        if( suspendTwoMethods(rootOrderSuspension, true) ) {
          allOrders.add(rootOrderSuspension);
          if( rootOrderSuspension.isSuspending() ) {
            suspendingOrders.add(rootOrderSuspension);
          }
          iter.remove();
        }
      }
    }
    
    private void continueSuspension() {
      Iterator<RootOrderSuspension> iter = suspendingOrders.iterator();
      while( iter.hasNext() ) {
        RootOrderSuspension rootOrderSuspension = iter.next();
        if( rootOrderSuspension.isSuspended() ) {
          iter.remove();
        } else if ( rootOrderSuspension.isSuspending() ) {
          suspendTwoMethods(rootOrderSuspension, false);
        } else {
          //Status ist RootSuspending, daher noch kurz warten, bis Root komplett suspendiert
        }
      }
    }

    private boolean suspendTwoMethods(RootOrderSuspension rootOrderSuspension, boolean startOrContinue ) {
      Pair<RootSRInformation<O>, SRState> pair = srInformationCache.getRootLockedInWantedStateOrNullOtherwise( rootOrderSuspension.getRootOrderId(), EnumSet.of(SRState.Running,SRState.Resuming) );
      try {
        RootSRInformation<O> srInformation = pair.getFirst();
        if( srInformation != null ) {
          if( startOrContinue ) {
            return startSuspension(rootOrderSuspension, srInformation);
          } else {
            return srAdapter.suspendInScheduler(srInformation, rootOrderSuspension);
          }
        } else {
          //alle anderen States sind unerwartet (auch Suspended)
          String failure = null;
          if( pair.getSecond() == SRState.Unknown ) {
            failure = "SRInformation not found: Order is not running";
          } else {
            failure = "SRInformation in state "+pair.getSecond()+" could not be suspended";
          }
          if( failFast ) {
            if( logger.isDebugEnabled() ) {
              logger.debug("Could not suspend "+rootOrderSuspension.getRootOrderId()+": "+failure);
            }
            addSuspensionFailed(rootOrderSuspension,failure);
            return true;
          } else {
            logger.warn("Could not suspend "+rootOrderSuspension.getRootOrderId()+": "+failure);
            return false;
          }
        }
      } finally {
        if( pair.getFirst() != null ) {
          pair.getFirst().unlock();
        }
      }
    }

    /**
     * @param rootOrderSuspension
     * @param string
     */
    private void addSuspensionFailed(RootOrderSuspension rootOrderSuspension, String failure) {
      if( suspensionFailed == null ) {
        suspensionFailed = new HashMap<Long,String>();
      }
      suspensionFailed.put(rootOrderSuspension.getRootOrderId(), failure);
      rootOrderSuspension.continueResume();
    }

    private boolean startSuspension(RootOrderSuspension rootOrderSuspension, RootSRInformation<O> rootSRInformation) {
      if( srAdapter.suspendInExecution(rootSRInformation,rootOrderSuspension) ) {
        rootSRInformation.setRootOrderSuspension(rootOrderSuspension);
        srAdapter.suspendInScheduler(rootSRInformation, rootOrderSuspension);
        return true;
      } else {
        //Process nicht gefunden, sollte nicht auftreten können, da Root-Auftrag ja nicht eben erst in die Execution gelangte
        return false;
      }
    }
  
  }

  public enum ResumeResult {
    Failed,       //Misserfolg
    Resumed,      //Erfolg
    Unresumeable; //nicht resumebar (MI-Redirection, administrativ gesperrt) 
  }

  /**
   * @param rootOrderId
   * @param targets
   * @param con
   * @return
   * @throws PersistenceLayerException (Zugriff auf OrderBackup, Status umtragen) 
   */
  public Pair<ResumeResult,String> resumeRootOrder(Long rootOrderId, List<ResumeTarget> targets, C con) throws PersistenceLayerException {
    ResumeTarget rootTarget = new ResumeTarget(rootOrderId,rootOrderId,null);
    //OrderBackup-Suche ist für die bereits suspendierten Aufträge nötig
    RootSRInformation<O> rootSRInformation;
    if (logger.isDebugEnabled()) {
      logger.debug("Resuming root order "+rootOrderId+" with targets "+targets);
    }
    try {
      rootSRInformation = srInformationCache.getOrCreateLockedRootNotInvalid(rootTarget);
    } catch (OrderBackupNotFoundException e) {
      //Suspendierung nicht geklappt, deswegen fertig geworden?
      return Pair.of(ResumeResult.Resumed,"OrderBackupNotFound"); //TODO prüfen, ob korrekt?
    } catch (ResumeLockedException e) {
      return Pair.of(ResumeResult.Unresumeable, SuspendResumeManagement.UNRESUMABLE_LOCKED );
    }
    return resumeInternalLockedRootSRInformation(rootSRInformation, targets, con);
  }

  /**
   * @param rootOrderTargets
   * @return
   */
  public Map<Long,Pair<String, PersistenceLayerException>> resumeRootOrdersWithRetries(Map<Long, ArrayList<ResumeTarget>> rootOrderTargets) {
    List<RootOrderSuspension> rootOrderSuspensions = new ArrayList<RootOrderSuspension>();
    for( Map.Entry<Long, ArrayList<ResumeTarget>> entry : rootOrderTargets.entrySet() ) {
      RootOrderSuspension ros = new RootOrderSuspension(entry.getKey(),entry.getValue());
      rootOrderSuspensions.add(ros);
    }
    List<Triple<RootOrderSuspension, String, PersistenceLayerException>> failed = 
        srAdapter.resumeRootOrdersWithRetries(rootOrderSuspensions);
    if( failed.isEmpty() ) {
      return Collections.emptyMap();
    } else {
      Map<Long,Pair<String, PersistenceLayerException>> result = new HashMap<Long,Pair<String, PersistenceLayerException>>();
      for( Triple<RootOrderSuspension, String, PersistenceLayerException> t : failed ) {
        result.put( t.getFirst().getRootOrderId(), Pair.of(t.getSecond(), t.getThird()) );
      }
      return result;
    }
  }

  public void handleParallelExecutorFinished(Long orderId, String parallelExecutorId) {
    if (logger.isDebugEnabled()) {
      logger.debug("ParallelExecutor finished: " + parallelExecutorId);
    }
    SRInformation srInformation = srInformationCache.getOrCreateLocked(orderId, SRState.Running);
    try {
      srInformation.removeParallelExecutor(parallelExecutorId);
    } finally {
      srInformation.unlock();
    }
  }

}
