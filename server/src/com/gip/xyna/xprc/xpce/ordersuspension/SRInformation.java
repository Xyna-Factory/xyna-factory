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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.xfractwfe.base.parallel.FractalWorkflowParallelExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.Lane.LanePart;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.ResumableParallelExecutor;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;



/**
 * SRInformation kapselt pro Auftrag alle Informationen, die während der Durchführung von 
 * Suspend und Resume nötig sind und stellt auch das Lock zu Verfügung, mit dem konkurrierende
 * Suspend/Resumes geschützt werden.
 * Diese Informationen sind:
 * <ul>
 * <li>lock: Schützt konkurrierende Zugriffe auf resumeLanes und state </li>
 * <li>parallelExecutors: Map(parallelExecutorId-&gt;ParallelExecutor), damit bei einem Resume bei
 *     laufenden Auftrag eine Lane neugestartet werden kann</li>
 * <li>resumeLanes: Set(Lane), in die alle zu resumenden Lanes geschrieben werden, damit beim 
 *     Starten eines suspendierten Auftrags die richtigen Lanes gestartet werden und beim Suspend
 *     erkannt wird, ob der Auftrag als suspendiert beendet werden muss oder sofort fortgesetzt 
 *     werden muss, weil Resumes auftraten.</li>
 * </ul>
 */
public class SRInformation {

  public enum ResumeAllLanes {
    None, //nur spezielle Lanes resumen
    FirstRun, //alle Lanes sollen resumt werden, gilt nur beim Resume aus Suspendierung 
    //heraus und kann bei handleSuspensionEvent ignoriert werden
    Always; //alle Lanes sollen resumt werden, muss bei handleSuspensionEvent beachtet werden
  }
  
  class ResumedLanes {

    //alles lanes die resumed werden sollen. nur lanes mit laneid!=null, ansonsten ist resumeAllLanes gesetzt.
    private Set<Lane> resumedLanes;
    //sollen alle lanes resumed werden (z.b. resumeorder von der cli aus)
    private ResumeAllLanes resumeAllLanes = ResumeAllLanes.None;
    //bei einer hierarchie von PEs in einem workflow soll für jeden PE nur einmal alle lanes resumed werden. hier merkt man sich die, für die das bereits getan wurde
    private Set<String> parallelExecutorsForWhichAllLanesAreResumed;


    public boolean isResumeAllLanes() {
      switch (resumeAllLanes) {
        case None :
          return false;
        case Always :
          return true;
        case FirstRun :
          return state != SRState.Suspending;
      }
      return false;
    }


    private void resumeAllLanes() {
      if (state == SRState.Suspended) {
        resumeAllLanes = ResumeAllLanes.FirstRun;
      } else {
        resumeAllLanes = ResumeAllLanes.Always;
      }
      parallelExecutorsForWhichAllLanesAreResumed = null;
    }


    public Set<String> removeResumedLanesEndingInParallelExecutor(String parallelExecutorId) {
      Set<String> lanesToBeResumed = new HashSet<String>();
      if (resumedLanes != null) {
        Iterator<Lane> it = resumedLanes.iterator();
        while (it.hasNext()) {
          Lane lane = it.next();
          boolean first = true;
          for (LanePart part : lane.getLanePartsForParallelExecutors()) {
            if (part.getParallelExecutorId().equals(parallelExecutorId)) {
              lanesToBeResumed.add(part.getLanePart());
              if (first) {
                //lane ist damit erledigt.
                it.remove();
              }
              //else: beim nächsten verschachtelten PE wieder berücksichtigen!
              break; //peid kann nur einmal in einer lane stehen
            }
            first = false;
          }
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Removed resumed Lanes for ParallelExecutor " + parallelExecutorId + ": resumedLanes=" + resumedLanes);
      }
      return lanesToBeResumed;
    }

    /*
    * PE wird fortgesetzt. zu resumende laneids ermitteln und markieren/löschen, damit sie nicht doppelt resumen 
    * beispiel:             
    *             
    *             PE3
    *             /
    *            /
    * PE1      PE2
    *  \       /
    *   \     /
    *    PERoot
    * 
    * resumedLanes={"PE3-1,PE2-5,PERoot-3", "PE1-2,PERoot-7"}
    * 
    * Falls PE2 resumed wird, "PE2-5" zurückgeben, und eintrag nicht aus resumedLanes entfernen, weil er beim PE3 noch benötigt wird
    * 
    */
    public Set<String> getLanesToBeResumedAndMarkAsResumed(String parallelExecutorId) {
      Set<String> lanesToBeResumed = removeResumedLanesEndingInParallelExecutor(parallelExecutorId);
      if (isResumeAllLanes()) {
        if (markParallelExecutorAsAllLanesResumed(parallelExecutorId)) {
          //trotzdem wichtig und richtig, oben die lanes aus den resumed lanes entfernt zu haben, die bei diesem PE enden
          return FractalWorkflowParallelExecutor.ALL_SUSPENDED_LANE_IDS;
        }
        //else: nicht resumealllanes - für diesen PE bereits früher passiert
      }

      return lanesToBeResumed;
    }


    private boolean markParallelExecutorAsAllLanesResumed(String parallelExecutorId) {
      lazyCreatePEFWALAR();
      return parallelExecutorsForWhichAllLanesAreResumed.add(parallelExecutorId);
    }


    private void lazyCreatePEFWALAR() {
      if (parallelExecutorsForWhichAllLanesAreResumed == null) {
        parallelExecutorsForWhichAllLanesAreResumed = new HashSet<String>(1);
      }
    }


    private void lazyCreateRL() {
      if (resumedLanes == null) {
        resumedLanes = new HashSet<Lane>(1);
      }
    }


    public void addLaneToResume(Lane lane) {
      if (lane.isResumeAll()) {
        resumeAllLanes();
      } else if (lane.isResumeParallelExecutor()) {
        lazyCreateRL();
        resumedLanes.add(lane);
      } else {
        //lane soll keinen ParallelExecutor resumen, daher ignorieren
        // ist wahrscheinlich ein Retry-Counter, dieser sollte validiert werden
      }
    }


    public boolean areAnyLanesToBeResumed() {
      return (resumedLanes != null && resumedLanes.size() > 0) || isResumeAllLanes();
    }


    public Set<Lane> getResumedLanes() {
      return resumedLanes == null ? Collections.<Lane> emptySet() : resumedLanes;
    }


    public String toString() {
      return "resumeAll=" + resumeAllLanes + ",resume=" + resumedLanes + ",resumedPEs=" + parallelExecutorsForWhichAllLanesAreResumed;
    }


    public void removeLaneToResume(Lane lane) {
      if (resumedLanes != null) {
        resumedLanes.remove(lane);
      }
    }


  }


  private static final Logger logger = Logger.getLogger(SRInformation.class);
  
  private final Long orderId;
  private Long rootId;
  private Long parentId;
  private String parentLaneId;

  private SRState state;
  private final ReentrantLock lock;
  private final HashMap<String, ResumableParallelExecutor> parallelExecutors;
  private final ResumedLanes resumedLanes = new ResumedLanes();
  private LockInformation lastLockInformation;
  private SuspensionCause suspensionCause;
 
  public enum SRState {
    /**
     * SRInformation ist gerade erst angelegt worden, ohne dass bereits eine Suspendierung stattgefunden hat
     */
    Unknown(false),
    /**
     * Auftrag läuft derzeit
     */
    Running(true),
    /**
     * Auftrag ist supendiert
     */
    Suspended(true),
    /**
     * Auftrag ist bereits für Resume im Scheduler eingestellt, wird aber noch nicht ausgeführt.
     * Weitere Lanes zum Resumen können eingestellt werden.
     */
    Resuming(true),
    /**
     * Auftrag wird gerade suspendiert, daher kann er derzeit nicht resumt werden.
     */
    Suspending(false),
    /**
     * Auftrag ist suspendiert worden, die Daten sind veraltet. Daher sollte SRInformation verworfen 
     * werden. Dieser Status existiert nur kurze Zeit, bevor der SRInformation-Eintrag aus dem 
     * Speicher gelöscht wird.  
     */
    Invalid(false);
  
  private boolean acceptsNewLanesToResume;


  private SRState(boolean acceptsNewLanesToResume) {
    this.acceptsNewLanesToResume = acceptsNewLanesToResume;
  }

  public boolean acceptsNewLanesToResume() {
    return acceptsNewLanesToResume;
  }
 }
  
  public SRInformation(Long orderId, SRState state) {
    this.orderId = orderId;
    this.state = state;
    this.lock = new ReentrantLock();
    this.parallelExecutors = new HashMap<String, ResumableParallelExecutor>();
  }
  
  @Override
  public String toString() {
    StringBuilder sb = toStringBuilder();
    if( XynaProperty.SUSPEND_RESUME_SHOW_SRINFORMATION_LOCK_INFO.get() ) {
      sb.append(" ");
      if( lock.isLocked() && lastLockInformation != null ) {
        sb.append( "locked with holdCount=").append(lastLockInformation.getHoldCount() );
      }
    }
    return sb.toString();
  }
  
  private StringBuilder toStringBuilder() {
    StringBuilder sb = new StringBuilder();
    sb.append( state).append("#").append(parallelExecutors).append("#").append(resumedLanes);
    
    if( state != SRState.Running ) {
      if( parentId == null ) {
        sb.append("_root");
      } else {
        sb.append("_P").append(parentId).append("_L").append(parentLaneId);
      }
    }
    return sb;
  }
  
  public String asCompleteString() {
    StringBuilder sb = toStringBuilder();
    if( XynaProperty.SUSPEND_RESUME_SHOW_SRINFORMATION_LOCK_INFO.get() ) {
      sb.append(" ");
      if( lock.isLocked() && lastLockInformation != null ) {
        lastLockInformation.append(sb);
      }
    }
    return sb.toString();
  }
  

  public synchronized ResumableParallelExecutor getParallelExecutor(String peId) {
    return parallelExecutors.get(peId);
  }

  /**
   * @return false, falls PE bereits vorhanden
   */
  public synchronized boolean addParallelExecutor(ResumableParallelExecutor parallelExecutor) {
    return parallelExecutors.put(parallelExecutor.getParallelExecutorId(), parallelExecutor) != parallelExecutor;
  }   
    

  public SRState getState() {
    return state;
  }
  
  public Long getOrderId() {
    return orderId;
  }

  public void setState(SRState state) {
    this.state = state;
  }
  
  public SuspensionCause getSuspensionCause() {
    return suspensionCause;
  }
  
  public void setSuspensionCause(SuspensionCause suspensionCause) {
    this.suspensionCause = suspensionCause;
  }
  
  public void lock() {
    lock.lock();
    if( XynaProperty.SUSPEND_RESUME_SHOW_SRINFORMATION_LOCK_INFO.get() ) {
      if( lastLockInformation == null ) {
        lastLockInformation = new LockInformation(lock);
      } else {
        lastLockInformation.lock(lock);
      }
    }
  }
  
  public void unlock() {
    lock.unlock();
    if( XynaProperty.SUSPEND_RESUME_SHOW_SRINFORMATION_LOCK_INFO.get() ) {
      if( lastLockInformation != null ) {
        lastLockInformation.unlock(lock);
      }
    }
  }
  
  public Long getRootId() {
    return rootId;
  }
  public void setRootId(Long rootId) {
    this.rootId = rootId;
  }

  public Long getParentId() {
    return parentId;
  }
  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }
  
  public String getParentLaneId() {
    return parentLaneId;
  }
  
  public void setParentLaneId(String parentLaneId) {
    this.parentLaneId = parentLaneId;
  }
  
  public ResumeTarget getParentResumeTarget() {
    if( parentId == null ) {
      return null; //Ohne Parent kein ParentResumeTarget
    }
    return new ResumeTarget(rootId, parentId, parentLaneId);
  }

  public List<ResumeTarget> getResumeTargets() {
    ArrayList<ResumeTarget> resumeTargets = new ArrayList<ResumeTarget>();
    if( resumedLanes.isResumeAllLanes() ) {
      if( parentId != null ) {
        resumeTargets.add( new ResumeTarget(rootId,parentId,parentLaneId) );
      } else {
        resumeTargets.add( new ResumeTarget(rootId,orderId,null) );
      }
      return resumeTargets;
    }
    for( Lane lane : resumedLanes.getResumedLanes() ) {
      resumeTargets.add( new ResumeTarget(rootId,orderId,lane.toString()));
    }
    return resumeTargets;
  }
  
  private static class LockInformation {

    private int holdCount;
    
    private static class ThreadInfo {
      public ThreadInfo() {
        Thread t = Thread.currentThread();
        this.threadName = t.getName();
        this.stackTrace = t.getStackTrace();
      }
      String threadName;
      StackTraceElement[] stackTrace;
      public void appendTo(StringBuilder sb) {
        sb.append(threadName).append("\n");
        for (int i=0; i < stackTrace.length; i++) {
          sb.append("\tat ").append(stackTrace[i]).append("\n");
        }
      }
    }
    
    private List<ThreadInfo> threadInfos = new ArrayList<ThreadInfo>();

    public LockInformation(ReentrantLock lock) {
      lock(lock);
    }

    public int getHoldCount() {
      return holdCount;
    }

    public void lock(ReentrantLock lock) {
      this.holdCount = lock.getHoldCount();
      threadInfos.add( new ThreadInfo() );
    }

    public void unlock(ReentrantLock lock) {
      this.holdCount = lock.getHoldCount();
      if( this.holdCount == 0 ) {
        threadInfos.clear();
      }
    }

    public void append(StringBuilder sb) {
      sb.append( "locked with holdCount=").append(holdCount).append(": ");
      if( threadInfos.size() == 1 ) {
        threadInfos.get(0).appendTo(sb);
      } else {
        for( int t=0; t<threadInfos.size(); ++t ) {
          sb.append( "\tLock ").append(t+1).append("/").append(threadInfos.size()).append(": ");
          threadInfos.get(t).appendTo(sb);
        }
      }
    }
    
  }

  public ResumedLanes getResumedLanes() {
    return resumedLanes;
  }

  public void removeParallelExecutor(String parallelExecutorId) {
    parallelExecutors.remove(parallelExecutorId);
    //TODO nur nötig, weil man redundante resumedlanes hat (vgl. SuspendResumeAlgorithm.resumeParallelExecutor Kommentar)
    resumedLanes.removeResumedLanesEndingInParallelExecutor(parallelExecutorId);
  }
  
}
