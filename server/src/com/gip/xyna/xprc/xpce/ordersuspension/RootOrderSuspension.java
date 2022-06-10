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
package com.gip.xyna.xprc.xpce.ordersuspension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


/**
 * RootOrderSuspension hilft bei der Suspendierung eines Auftrags beginnend beim Root-Auftrag, 
 * also in der umgekehrten Richtung wie die normalen Suspendierungen.<br>
 * <br>
 * Zum Suspendieren wird eine RootOrderSuspension-Instanz wird dabei in die RootProcessData-Instanz
 * des zu suspendierenden Auftrags eingetragen. Daraufhin führen alle ausgeführten Steps im 
 * gesamten Auftrag die Methoden {@link #shouldSuspend()} und {@link #suspend(ResumeTarget)} auf. 
 * <br>
 * Über die Methode {@link #setSuspended(Long)} werden alle suspendierten Subworkflows gemeldet,
 * bei der Suspendierung des Root-XynaProcess wird der Status auf "Suspended" gewechselt und das 
 * CountDownLatch "suspensionLatch" gelöst.
 * <br>
 * Über {@link #awaitSuspension()}, {@link #awaitSuspension(long)} und 
 * {@link #awaitSuspension(long, TimeUnit)} kann auf die erfolgreiche Durchführung der 
 * Suspendierung samt OrderBackup gewartet werden.
 * <br>
 * Falls die Suspendierung abgebrochen werden soll, muss die Methode {@link #undoSuspension()} 
 * aufgerufen werden. Diese sorgt dafür, dass {@link #shouldSuspend()} und {@link #suspend(ResumeTarget)}
 * keine weiteren Suspendierungen durchführen. Es wird ein CountDownLatch "resumeLatch" gestartet, 
 * welches dafür sorgt, dass {@link #setSuspended(Long)} warten muss. Nun können über 
 * {@link #getResumeTargets()} die bereits suspendierten Lanes ausgegeben werden (und dann außerhalb 
 * für das Resume in die SRInformations eingetragen werden). Über {@link #continueResume()} wird dann 
 * das Latch "resumeLatch" gelöst, die Suspendierungen können fortgesetzt werden. Diese sehen dann 
 * die Resumes, der Auftrag läuft also weiter.
 */
public class RootOrderSuspension {

  private static Logger logger = CentralFactoryLogging.getLogger(RootOrderSuspension.class);
  private volatile State state;
  private SuspensionCause suspensionCause;
  private volatile List<ResumeTarget> resumeTargets;
  private CountDownLatch suspensionLatch;
  private CountDownLatch resumeLatch;
  private Long rootOrderId;
  private ReentrantLock lock;
  private ManualInteractionData manualInteractionData;

  
  private RootOrderSuspension(Long rootOrderId, State state) {
    this.rootOrderId = rootOrderId;
    this.state = state;
    this.lock = new ReentrantLock();
  }
  /**
   * Konstruktor zur eigentlichen Verwendung: Suspendierung der RootOrder
   * @param rootOrderId
   * @param suspensionCause
   */
  public RootOrderSuspension(Long rootOrderId, SuspensionCause suspensionCause) {
    this(rootOrderId, State.Suspending);
    this.suspensionCause = suspensionCause;
    this.suspensionLatch = new CountDownLatch(1);
  }
  
  /**
   * Konstruktor zum einfachen Resume einer RootOrder
   * @param rootOrderId
   * @param resumeTargets
   */
  public RootOrderSuspension(Long rootOrderId, ArrayList<ResumeTarget> resumeTargets) {
    this(rootOrderId,State.Suspended);
    this.resumeTargets = resumeTargets;
  }

  public enum State {
    Suspending,     //es werden weitere ProcessSuspendedExceptions geworfen und ResumeTargets gesammelt
    RootSuspending, //Suspendierung ist bis zum Root propagiert, nun fehlt hauptsächlich Backup der RootOrder
    Suspended,      //RootOrder wurde suspendiert, Backup ist geschrieben -> fertig
    
    Resuming,   //wegen Undo werden alle ResumeTargets wieder eingestellt, daher darf keine Suspendierung 
                //vorgenommen werden, um möglichst einfach das Resume zu starten
    Resumed,    //Auftrag ist Resumed -> fertig
    
  }
    
  @Override
  public String toString() {
    return "RootOrderSuspension("
        +rootOrderId+","
        +state+","
        +(resumeTargets==null?0:resumeTargets.size())
        +")";
  }
  
  /**
   * Von FractalProcessStep via XynaProcess und RootProcessData vor jeder Ausführung gerufen.
   * @return
   */
  public boolean shouldSuspend() {
    return state == State.Suspending;
  }
  
  /**
   * Von FractalProcessStep via XynaProcess und RootProcessData vor jeder Ausführung gerufen.
   * Wirft die ProcessSuspendedException
   * @param resumeTarget
   */
  public ProcessSuspendedException suspend(ResumeTarget resumeTarget) {
    //logger.debug("suspend("+resumeTarget+")");
    while( true ) {
      State localState = state;
      switch( localState ) {
        case Suspending:
          lock.lock();
          try {
            if( state == State.Suspending ) {
              if( resumeTargets == null ) {
                resumeTargets = new ArrayList<ResumeTarget>();
              }
              resumeTargets.add(resumeTarget);
              //ProcessSuspendedException für SuspendResumeManagement.handleSuspensionEvent(..)
              return new ProcessSuspendedException(suspensionCause);
            } else {
              break; //retry
            }
          } finally {
            lock.unlock();
          }
        default:
          return null;//keine Suspendierung
      }
    }
  }
  
  /**
   * Wird gerufen, falls ein Resume den Auftrag suspendierend vorfindet 
   * @param targets
   * @return true, wenn ResumeTargets eingetragen wurden
   */
  public boolean addResumeTargets(List<ResumeTarget> targets) {
    //logger.debug("addResumeTargets("+targets+")");
    while( true ) {
      State localState = state;
      switch( localState ) {
        case Suspending:
        case RootSuspending:
          if( addResumeTargetsInternal(targets,localState) ) {
            return true;
          } else {
            break; //retry
          }
        case Suspended:
          //unerwartet
          return false;
        case Resuming:
        case Resumed:
          //Resume soll ausgeführt werden
          return false;
      }
    }
  }

  private boolean addResumeTargetsInternal(List<ResumeTarget> targets, State expected) {
    lock.lock();
    try {
      if( state == expected ) {
        if( resumeTargets == null ) {
          resumeTargets = new ArrayList<ResumeTarget>();
        }
        resumeTargets.addAll(targets);
        return true;
      } else {
        return false;
      }
    } finally {
      lock.unlock();
    }
  }
  
  /**
   * Warten auf die Suspendierung
   * @param timeout
   * @param unit
   * @return true, wenn suspendiert; false wenn Timeout erreicht
   * @throws InterruptedException
   */
  public boolean awaitSuspension(long  timeout, TimeUnit unit) throws InterruptedException {
    return suspensionLatch.await(timeout, unit);
  }

  /**
   * Warten auf die Suspendierung
   * @throws InterruptedException
   */
  public void awaitSuspension() throws InterruptedException {
    suspensionLatch.await();
  }
      
  /**
   * Warten auf die Suspendierung
   * @param absoluteTimeout
   * @return true, wenn suspendiert; false wenn Timeout erreicht
   * @throws InterruptedException 
   */
  public boolean awaitSuspension(long absoluteTimeout) throws InterruptedException {
    long timeout = absoluteTimeout-System.currentTimeMillis();
    if( timeout >= 0 ) {
      return suspensionLatch.await(timeout, TimeUnit.MILLISECONDS );
    }
    return false;
  }

  
  /**
   * Von XynaProcess via RootProcessData beim Fangen der ProcessSuspendedException vor der Ausführung 
   * der eigentlichen Suspendierung gerufen.
   * @param orderId suspendierter Auftrag
   */
  public void setSuspended(Long orderId) {
    State localState = state;
    switch( localState ) {
      case Suspending:
        if( rootOrderId.equals(orderId) ) {
          lock.lock();
          try {
            if( state == State.Suspending ) {
              state = State.RootSuspending;
            }
          } finally {
            lock.unlock();
          }
        }
        break;
      case RootSuspending:
        logger.warn("Unexpected state "+localState+" for "+this);
        break;
      case Resuming:
        while( state != State.Resumed ) {
          try {
            resumeLatch.await();
          } catch (InterruptedException e) {
            //nochmal warten
          }
        }
        break;
      case Resumed:
        //Suspendierung kann immer noch auftreten, nachdem Suspendierung rückgängig gemacht wurde
        break;
      default:
        logger.warn("Unexpected state "+localState+" for "+this);
    }
  }
  
  /**
   * 
   */
  public void setSuspended() {
    State localState = state;
    if( localState == State.RootSuspending ) {
      lock.lock();
      try {
        if( state == State.RootSuspending ) {
          state = State.Suspended;
          suspensionLatch.countDown();
          return;
        }
      } finally {
        lock.unlock();
      }
    }
    logger.warn("setSuspended in state "+state+"!");
  }

  
  
  /**
   * Abbruch der Suspendierung: Status ist nun Resuming -&gt; keine neuen Suspendierungen;
   * CountDownLatch hält aktuelle Supendierungen auf.
   * @return aktueller Status
   */
  public State undoSuspension() {
    while( true ) { 
      State localState = state;
      switch( localState ) {
        case Suspending:
        case RootSuspending:
          if( undoSuspensionInternal(localState) ) {
            return state;
          } else {
            break; //Retry
          }
        case Suspended:
          return state; //zu spät, ist bereits suspendiert 
        default:
          logger.warn("Unexpected state "+localState+" for "+this); 
          return state;
      }
    }
  }

  private boolean undoSuspensionInternal(State expected) {
    lock.lock();
    try {
      if( state == expected ) {
        state = State.Resuming;
        resumeLatch = new CountDownLatch(1);
        return true;
      } else {
        return false; //Retry
      }
    } finally {
      lock.unlock();
    }
  }
  
  /**
   * Ausgabe aller resumtem Lanes
   * @return
   */
  public List<ResumeTarget> getResumeTargets() {
    lock.lock();
    try {
      if( resumeTargets == null ) {
        return Collections.emptyList();
      } else {
        return resumeTargets;
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Fortsetzen der wartenden Suspendierungen, dabei werden auch die zuvor 
   * eingestellten Resumes berücksichtigt.
   */
  public void continueResume() {
    while( true ) { 
      State localState = state;
      switch( localState ) {
        case Suspended:
          return; //zu spät, war bereits suspendiert
        case Resumed:
          return; //zu spät, war bereits resumed
        case Resuming:
        case Suspending:
        case RootSuspending:
          if( continueResumeInternal( localState ) ) {
            return;
          } else {
            break; //Retry
          }
        default:
          logger.warn("Unexpected state "+localState+" for "+this);
          if( continueResumeInternal( localState ) ) {
            return;
          } else {
            break; //Retry
          }
      }
    }
  }

  private boolean continueResumeInternal(State expected) {
    lock.lock();
    try {
      if( state == expected ) {
        state = State.Resumed;
        if( expected == State.Resuming ) {
          resumeLatch.countDown();
        }
        return true;
      } else {
        return false; //Retry
      }
    } finally {
      lock.unlock();
    }
  }

  public Long getRootOrderId() {
    return rootOrderId;
  }
  
  public SuspensionCause getSuspensionCause() {
    return suspensionCause;
  }

  public boolean isSuspended() {
    return state == State.Suspended;
  }

  public boolean isSuspending() {
    return state == State.Suspending;
  }

  public void addFailedInterruptOrStop(ResumeTarget resumeTarget) {
    getOrCreateManualInteractionData().addTarget(resumeTarget);
  }
  
  private ManualInteractionData getOrCreateManualInteractionData() {
    lock.lock();
    try {
      if( manualInteractionData == null ) {
        manualInteractionData = new ManualInteractionData();
      }
      return manualInteractionData;
    } finally {
      lock.unlock();
    }
  }
  
  public ManualInteractionData getManualInteractionData() {
    return manualInteractionData;
  }
  
  public void setTerminateThreadCount(boolean stopForcefully, int terminated) {
    getOrCreateManualInteractionData().setTerminateThreadCount(stopForcefully, terminated);
  }
  
  public boolean isMINecessary() {
    if( manualInteractionData == null ) {
      return false;
    } else {
      return manualInteractionData.isMINecessary();
    }
  }
  
  public class ManualInteractionData {
    private boolean stopForcefully;
    private int interruptedThreadCount;
    private int stoppedThreadCount;
    private Long miOrderId;

    public String getMIMessage() {
      StringBuilder sb = new StringBuilder();
      sb.append("While suspension of order ").append(rootOrderId).append(" with cause ").append(suspensionCause.getName());
      sb.append(" ").append(interruptedThreadCount).append( interruptedThreadCount == 1 ? " thread was" : " threads were")
        .append(" interrupted");
      if( stoppedThreadCount > 0 ) {
        sb.append(" and ").append(stoppedThreadCount).append( stoppedThreadCount == 1 ? " thread was" : " threads were")
        .append(" forcefully stopped");
      }
      sb.append(".");
      return sb.toString();
    }

    public void setTerminateThreadCount(boolean stopForcefully, int terminated) {
      if( stopForcefully ) {
        stoppedThreadCount = terminated;
        this.stopForcefully = true;
      } else {
        interruptedThreadCount = terminated;
      }
    }

    public void addTarget(ResumeTarget resumeTarget) {
      //FIXME was damit anfangen!
    }

    public boolean isStopForcefully() {
      return stopForcefully;
    }

    public boolean isMINecessary() {
      return stoppedThreadCount > 0 || interruptedThreadCount > 0;
    }

    public Long getMIOrderId() {
      return miOrderId;
    }

    public void setMIOrderId(Long miOrderId) {
      this.miOrderId = miOrderId;
    }
    
  }

}
