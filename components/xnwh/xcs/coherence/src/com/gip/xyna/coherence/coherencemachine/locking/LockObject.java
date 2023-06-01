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

package com.gip.xyna.coherence.coherencemachine.locking;



import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.coherence.CacheControllerImpl;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectProtocol;
import com.gip.xyna.coherence.coherencemachine.locking.LockToken.LockTokenType;
import com.gip.xyna.coherence.exceptions.ClusterInconsistentException;
import com.gip.xyna.coherence.utils.debugging.Debugger;
import com.gip.xyna.coherence.utils.locking.LockingUtils;



public class LockObject implements Serializable {


  private static final long serialVersionUID = 1L;

  private static final Debugger debugger = Debugger.getDebugger();


  public static class LockRequestResponse implements Serializable {

    private static final long serialVersionUID = 5185464420218424562L;
    private static LockRequestResponse TIMEOUT = new LockRequestResponse(-1, -1);
    private static LockRequestResponse TIMEDOUT_AS_PART_OF_CIRCLE = new LockRequestResponse(-3, -3);

    public final long priority;
    public final int remoteCircleSize;


    public LockRequestResponse(long priority, int remoteCircleSize) {
      this.priority = priority;
      this.remoteCircleSize = remoteCircleSize;
    }

    public boolean isLockTimeout() {
      return priority == -1 && remoteCircleSize == -1;
    }
    
    public boolean isNodeTimedOutAsPartOfCircle() {
      return priority == -3 && remoteCircleSize == -3;
    }

  }

  public enum LockAwaitResponse {
    TIMEOUT, SUCCESS_CIRCLE_CLOSED, SUCCESS_CIRCLE_NOT_CLOSED, RECALLED;

    public static LockAwaitResponse create(LockObject lockObject) {
      if (lockObject.currentLockCircleSize == 0) {
        return SUCCESS_CIRCLE_NOT_CLOSED;
      } else {
        return SUCCESS_CIRCLE_CLOSED;
      }
    }
  }


  /**
   * lokal verwendetes lock um konsistenz in der klasse zu gewährleisten.
   */
  private transient ReentrantLock lockObjectProtection = new ReentrantLock();

  /**
   * liste der locks, von denen der höchste (meist) gewinnt und damit auf remote knoten implizit das lock besitzt.
   */
  private SortedMap<Long, LockToken> locks; // initialized lazily

  /**
   * zugrundeliegendes lokales lockobjekt um reentrancy zu realisieren
   */
  private transient ReentrantLock localRequestLock;

  /**
   * lokale oder remote priority die gerade das lock hält. dazu muss ein eintrag in der map {@link #locks} existieren
   */
  private volatile Long currentlyLockingPriority;

  private volatile long localRequestPriorityHoldingLock = -1;

  /**
   * anzahl von threads die auf entweder eines der {@link #locks} aus der map warten oder auf
   * {@link #lockCircleProtection} oder auf {@link #preliminaryLock}
   */
  private AtomicInteger waitingForNonReentrantLockTokensCount; // initialized lazily

  /**
   * anzahl von verbleibenden knoten im lockzirkel. ist ggfs anders als die größe der liste {@link #locks}, weil der
   * zirkel halb offen sein kann und sich hier bereits weitere knoten gemeldet haben.
   * <p>
   * oder es kann sein, dass der wert bereits vom gewinner an alle knoten propagiert wurde, diese aber noch nicht
   * untereinander ihre kommunikation beendet haben - im schlimmsten fall kann also sowas passieren wie
   * {@link #currentLockCircleSize} == 100, {@link #locks}.size == 0.
   */
  private volatile int currentLockCircleSize; // TODO volatile notwendigkeit überprüfen

  /**
   * anzahl von knoten, die am lock beteiligt sind. wird vom gewinnerknoten ermittelt, dann als
   * {@link #currentLockCircleSize} auf alle knoten verteilt.
   * <p>
   * TODO: eigtl braucht man den nicht und kann direkt currentLockCircleSize benutzen oder? => nein, sonst kann man
   * nicht unterscheiden, ob man bereits die lockcirclesize propagiert hat. evtl würde ein boolean ausreichen.<br>
   * ACHTUNG: hat nichts mit {@link #preliminaryLock} zu tun.
   */
  private int preliminaryCircleSize = -1;

  /**
   * zählt anzahl von zurückgenommen locks. wirkt sich auf die {@link #currentLockCircleSize} aus, die beim nächsten
   * {@link #closeLockCircle()} gesetzt wird.
   */
  private int recalledLocksCounter = 0;

  /**
   * benutzt im fall, dass {@link #currentLockCircleSize} bereits gesetzt ist, aber keine einträge in {@link #locks}
   * vorhanden sind, auf die man locken kann. die hierauf wartenden threads werden dann notified, wenn
   * {@link #currentLockCircleSize} den wert 0 erreicht.
   */
  private LockToken lockCircleProtection = new LockToken(LockTokenType.CIRCLE_PROTECTION);

  /**
   * falls ein read auf ein objekt aufgerufen wird, welches z.B. im status shared ist, müssen die remote cluster members
   * davon nichts wissen. das gleiche gilt für updates auf modified objekten.
   */
  private ReentrantLock preliminaryLock;

  // TODO volatile necessary or only accessed while holding lockObjectProtection?
  /**
   * recalled is set if this LockObject was recalled
   */
  private volatile boolean recalled = false;
 

  // kann weg
  private final long objectId; // TODO prio4: nur für logging!
  private final boolean logTrace;


  private Object readResolve() throws ObjectStreamException {
    lockObjectProtection = new ReentrantLock();
    return this;
  }


  public LockObject(long objectId) {
    // by default trace state for all IDs larger than the one that stores cluster member information
    this(objectId, objectId > CacheControllerImpl.ID_CLUSTER_MEMBERS);
  }


  LockObject(long objectId, boolean logTrace) {
    this.logTrace = logTrace;
    this.objectId = objectId;
  }


  void checkClean() {
    if (lockObjectProtection.isLocked()) {
      throw new IllegalStateException(getClass().getSimpleName() + " cannot be reinitialized, still in use");
    }
    if (localRequestLock != null && localRequestLock.isLocked()) {
      throw new IllegalStateException(getClass().getSimpleName() + " cannot be reinitialized, still in use");
    }
    if (locks != null && !locks.isEmpty()) {
      throw new IllegalStateException(getClass().getSimpleName() + " cannot be reinitialized, still in use");
    }
    if (localRequestPriorityHoldingLock != -1) {
      throw new IllegalStateException(getClass().getSimpleName() + " cannot be reinitialized, still in use");
    }
    if (currentLockCircleSize != 0) {
      throw new IllegalStateException(getClass().getSimpleName() + " cannot be reinitialized, still in use");
    }
    if (preliminaryCircleSize != -1) {
      throw new IllegalStateException(getClass().getSimpleName() + " cannot be reinitialized, still in use");
    }
  }


  public LockRequestResponse requestLock(long priority, boolean localRequest, boolean tryLock, long timeoutNanos)
                  throws ObjectDeletedWhileWaitingForLockException, InterruptedException {

    lockObjectProtection.lock();
    try {
      if (deleted) {
        throw new ObjectDeletedWhileWaitingForLockException();
      }
      if (localRequest) {
        return requestLockLocally(priority, false, tryLock, timeoutNanos);
      } else {
        return requestLockRemotely(priority, tryLock, timeoutNanos);
      }
    } finally {
      lockObjectProtection.unlock();
    }

  }


  private void lazyCreateWaitingForNonReentrantLockTokensCount() {
    if (waitingForNonReentrantLockTokensCount == null) {
      waitingForNonReentrantLockTokensCount = new AtomicInteger();
    }
  }


  /**
   * kommt zurück, falls kein preliminary lock mehr vergeben ist. ansonsten wartet es, bis das derzeitig vergebene
   * preliminary lock frei wird. <br>
   * nicht reentrant! (muss es auch nicht sein)
   * 
   * @param timeoutNano = System.nanoTime equivalent
   */
  private boolean checkPreliminaryLock(boolean tryLock, long timeoutNano) throws ObjectDeletedWhileWaitingForLockException {
    // TODO methode verallgemeinern für beliebiges checkReentrantLock
    Lock oldPreliminaryLock;
    lazyCreateWaitingForNonReentrantLockTokensCount();
    while ((oldPreliminaryLock = preliminaryLock) != null) {
      if (deleted) {
        throw new ObjectDeletedWhileWaitingForLockException();
      }
      waitingForNonReentrantLockTokensCount.incrementAndGet();
      try {
        lockObjectProtection.unlock();
        try {
          // TODO find a nicer way to get notified if the object is being deleted
          long remainingTime = 100 * LockingUtils.NANO_TO_MILLI;
          if (tryLock) {
            remainingTime = Math.max(0, timeoutNano - System.nanoTime());
          }
          while (!oldPreliminaryLock.tryLock(remainingTime, TimeUnit.NANOSECONDS)) {
            if (deleted) {
              throw new ObjectDeletedWhileWaitingForLockException();
            }
            if (tryLock) {
              remainingTime = Math.max(0, timeoutNano - System.nanoTime());
              if (remainingTime <= 100) {
                return false;
              }
            }
          }
        } catch (InterruptedException e) {
          throw new ClusterInconsistentException("Got interrupted unexpectedly while waiting for <" + objectId + ">", e);
        } finally {
          lockObjectProtection.lock();
        }
      } finally {
        waitingForNonReentrantLockTokensCount.decrementAndGet();
      }
      oldPreliminaryLock.unlock();
    }
    preliminaryLock = null;
    return true;
  }


  private void lazyCreateLocksTable() {
    if (locks == null) {
      locks = new TreeMap<Long, LockToken>();
    }
  }

  public void recallLocalPreliminaryLock() {
    lockObjectProtection.lock();
    try {
      if (!releasePreliminaryLock()) {
        throw new ClusterInconsistentException("preliminary lock must be held");
      }
    } finally {
      lockObjectProtection.unlock();
    }
  }

  private void notifyLockTokenAndDebug(final LockToken releasedLocalLockToken, final long prio) {
    if (releasedLocalLockToken.notifyLock()) {
      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug(new Object() {
          @Override
          public String toString() {
            return "Notifying prio <" + prio + ">-lock";
          }
        });
      }
    } else {
      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug(new Object() {
          private String releasedTokenStr = releasedLocalLockToken.toString();
          @Override
          public String toString() {
            return "No one is waiting for prio " + prio + "-lock, <" + releasedTokenStr + ">, removing lock token";
          }
        });
      }
    }
  }


  public boolean recallLocalLockRequest(long priority, int lockCircleSize) {
    boolean countedInLockCircle = false;
    lockObjectProtection.lock();
    try {
      if (currentlyLockingPriority != null && currentlyLockingPriority == priority) {
        // state ist genauso als wäre das lock erfolgreich.
        releaseLockLocally(false);
      } else {
        // anderer thread hält das lock
        localRequestLock.unlock();
        localRequestPriorityHoldingLock = -1;
        
        LockToken removedLockToken = locks.remove(priority);
        notifyLockTokenAndDebug(removedLockToken, priority);
      }
      if (preliminaryLock != null) {
        throw new ClusterInconsistentException("preliminary lock may not be set");
      }
      preliminaryLock = new ReentrantLock();
      preliminaryLock.lock();

      // lockcirclesize anpassen
      if (currentLockCircleSize > 0) { // lokal kann kein alter lockzirkel bestehen, deshalb ist keine unterscheidung
                                       // zwischen 1 und >1 notwendig.
        currentLockCircleSize--;
        if (currentLockCircleSize != 0) {
          recalled = true;
        }
        countedInLockCircle = true;
      } else {
        /*
         * falls lockzirkelgröße gleich 0 ist, kann es sein, dass man beim warten auf ein preliminary lock ein timeout
         * hatte und gar kein lockzirkel existiert. in dem fall darf man den counter nicht hochzählen. da die remote
         * knoten den lokalen lockrequest (der recalled werden soll) erst bemerken (und zum lockzirkel dazuzählen), wenn
         * sie den lokalen knoten versuchen zu locken, muss man hier nur überprüfen, ob bereits ein remote request
         * angekommen ist
         */
        if (currentlyLockingPriority != null) {
          if (debugger.isEnabled()) {
            traceState();
            debugger.debug("incrementing recalled locks counter");
          }
          recalledLocksCounter++;
          recalled = true;
          countedInLockCircle = true;
        }
      }
      
      // see recallRemoteLock
      if (currentLockCircleSize == 1) {
        if (recalledLocksCounter ==  lockCircleSize) {
          recalledLocksCounter = 0;
        }
      }

      if (debugger.isEnabled() && logTrace) {
        traceState();
        final boolean finalCountedInLockCircle = countedInLockCircle;
        debugger.debug(new Object() {
          @Override
          public String toString() {
            return "recalled local lock. countedInLockCircle = " + finalCountedInLockCircle;
          }
        });
      }
    } finally {
      lockObjectProtection.unlock();
    }
    return countedInLockCircle;
  }


  /**
   * räumt das lockobjekt derart auf, dass der vorher hier angekommene lockrequest entfernt wird. z.b. falls bei einem
   * knoten ein lock-timeout passiert ist.
   */
  public void recallRemoteLockRequest(long priority, long priorityOfLockInOldLockCircle, boolean countedInLockCircle, int lockCircleSize) {
    lockObjectProtection.lock();
    try {
      if (currentlyLockingPriority != null && currentlyLockingPriority.longValue() == priority) {
        // remote state ist genauso als wäre das lock erfolgreich.
        releaseLockRemotely(false, priority);
      } else {
        // jemand anderes hält das lock
        if (locks != null) { // kann null sein, wenn in diesem knoten der timeout passiert ist
          LockToken removedRemoteToken = locks.remove(priority);
          if (removedRemoteToken != null) {
            notifyLockTokenAndDebug(removedRemoteToken, priority);
          }
        }
      }


      /*
       * fallunterscheidung für circlesize anpassungen: 
       * 1. alter lockzirkel, d.h. lockrequest gehört nicht zum
       * lockzirkel. => der timeout, der zum recall führte ist passiert, als auf einen alten lockzirkel gewartet wurde
       * 2. lockrequest gehört zum lockzirkel. => der timeout ist passiert, als auf ein element des aktuellen
       * lockzirkels gewartet wurde. oder aus 3. entstanden 3a. es gibt keinen lockzirkel => es wurde auf ein remote
       * preliminary lock gewartet, dass nicht geupgradet wurde zu einem richtigen lock. kann sich zu 2. wandeln, wenn
       * das preliminary lock geupgradet wird. 3b. es gibt keinen lockzirkel => es wurde auf ein lock eines alten
       * lockzirkels gewartet in fällen 2 und 3 kann es sein, dass der lockzirkel noch nicht geschlossen ist. fall 1
       * kann nur sein, wenn circlesize = 1 ist. => circlesize = 1 => fall 1 oder fall 2. circlesize > 1 => fall 2
       * circlesize = 0 => fall 1 oder fall 3.
       */
      if (currentLockCircleSize > 1) {
        currentLockCircleSize--;
      } else if (currentLockCircleSize == 1) {
        /*
         * 1. kein alter lockzirkel da gewesen => prioOfLockCircle = -1 2. alter lockzirkel immer noch aktiv =>
         * prioOfLockCircle gesetzt und noch vorhanden (es kann nicht passieren, dass der alte lockzirkel weg ist, und
         * ein neues lock mit der gleichen priority dazugekommen ist) 3. alter lockzirkel inzwischen weg =>
         * prioOfLockCircle gesetzt, nicht mehr vorhanden
         */

        if (priorityOfLockInOldLockCircle > -1) {
          if (currentlyLockingPriority != null && currentlyLockingPriority == priorityOfLockInOldLockCircle) {
            // alter lockzirkel noch aktiv => dort wird man nicht mitgezählt.
            // im neuen zirkel wird man aber mitgezählt
            if (countedInLockCircle) {
              recalledLocksCounter++;
            }
          } else {
            // alter lockzirkel inzwischen weg => dann gehört man offenbar zum neuen und ist das letzte zirkelmitglied
            currentLockCircleSize = 0;
            recalledLocksCounter = 0;
            recalled = false;
            lockCircleProtection.notifyLock();
            lockCircleProtection = new LockToken(LockTokenType.CIRCLE_PROTECTION);            
          }
        } else {
          // kein alter lockzirkel, sondern neuer - dann ist der aktuelle thread das letzte zirkelmitglied.
          currentLockCircleSize = 0;
          recalledLocksCounter = 0;
          recalled = false;
          lockCircleProtection.notifyLock();
          lockCircleProtection = new LockToken(LockTokenType.CIRCLE_PROTECTION);
        }
      } else {
        /*
         * falls lockzirkelgröße gleich 0 ist, kann es sein, dass man beim warten auf ein preliminary lock ein timeout
         * hatte und gar kein lockzirkel existiert. in dem fall darf man den counter nicht hochzählen, bzw
         * lockzirclesize darf später nicht runtergezählt werden. falls beim entfernen des lokalen locks nicht in
         * lockzirkel gewesen, dann jetzt immer noch nicht.
         */
        if (countedInLockCircle) {
          recalledLocksCounter++;
        }
      }
      
      if (recalled) {
        //if we were building a new lockCircle and everybody timed out, reset recalledCounter
        if (recalledLocksCounter == lockCircleSize) {
          if (currentLockCircleSize == 1) {
            // if we were building a new lockCircle (waiting for old) and everybody timed out, reset recalledCounter
//            recalledLocksCounter = 0;
            debugger.debug("reset recalledCounter - building a new lockCircle (waiting for old)");
          }
          recalledLocksCounter = 0;
          recalled = false;
          debugger.debug("reset recalledCounter - building a new lockCircle while circle was already closed");
        } else if (preliminaryCircleSize == -1 && // getRemoteLocks timed out while waiting for prelim
            recalledLocksCounter == lockCircleSize - 1) { // everybody else timed out as well
          recalledLocksCounter = 0;
          recalled = false;
          debugger.debug("reset recalledCounter - building a new lockCircle while circle was never closed");
        }
        lockCircleProtection.notifyLock();
        lockCircleProtection = new LockToken(LockTokenType.CIRCLE_PROTECTION);
        
        if (debugger.isEnabled() && logTrace) {
          traceState();
          final long finalPriorityOfLockInOldLockCircle = priorityOfLockInOldLockCircle;
          final boolean finalCountedInLockCircle = countedInLockCircle;
          debugger.debug(new Object() {

            @Override
            public String toString() {
              return "recalled remote lock. priorityOfLockInOldLockCircle = " + finalPriorityOfLockInOldLockCircle
                  + ", countedInLockCircle = " + finalCountedInLockCircle;
            }
          });
        }
      }
    } finally {
      lockObjectProtection.unlock();
    }
  }


  private LockRequestResponse requestLockRemotely(final long priority, boolean tryLock, long timeoutNano) 
      throws ObjectDeletedWhileWaitingForLockException {

    if (!checkPreliminaryLock(tryLock, timeoutNano)) {
      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug("got timeout waiting for preliminary lock");
      }
      return LockRequestResponse.TIMEOUT;
    }
    
    // update set of held locks
    lazyCreateLocksTable();
    LockToken shouldNotExist = locks.put(priority, new LockToken(LockTokenType.STANDARD));
    if (shouldNotExist != null) {
      locks.put(priority, shouldNotExist);
      if (debugger.isEnabled() && logTrace) {
        traceState();
      }
      throw new IllegalStateException("requestLock has been called twice for the same object id and priority <"
                      + priority + ">");
    }
        
    setCurrentlyLockedPriorityForRemoteLockRequest(priority);
    // I'm remotely requesting a lock that already timedout
    if (recalled) {
      return LockRequestResponse.TIMEDOUT_AS_PART_OF_CIRCLE;
    }
    if (localRequestLock != null && localRequestLock.isLocked()) {
      
      if (currentLockCircleSize > 0) {
        if (debugger.isEnabled() && logTrace) {
          traceState();
          debugger.debug(new Object() {

            private int curSize = currentLockCircleSize;

            @Override
            public String toString() {
              return "Local lock is locked and the lock circle size is currently set to <" + curSize + ">";
            }
          });
        }
        return new LockRequestResponse(localRequestPriorityHoldingLock, currentLockCircleSize);
      }

      if (priority > localRequestPriorityHoldingLock) {
        if (debugger.isEnabled() && logTrace) {
          traceState();
          debugger.debug(new Object() {
            private long _prio = priority;
            private long _localprio = localRequestPriorityHoldingLock;
            @Override
            public String toString() {
              return "Got remote lock since requested prio <" + _prio + "> is bigger than local request prio <"
                              + _localprio + ">";
            }
          });
        }
        return new LockRequestResponse(InterconnectProtocol.SUCCESSFUL_LOCK_BY_COMPARISON, 0);
      } else {
        if (debugger.isEnabled() && logTrace) {
          traceState();
          debugger.debug(new Object() {
            private long _prio = priority;
            private long _localprio = localRequestPriorityHoldingLock;
            @Override
            public String toString() {
              return "Returning local request prio since requested prio <" + _prio
                              + "> is smaller than local request prio <" + _localprio + ">";
            }
          });
        }
        return new LockRequestResponse(localRequestPriorityHoldingLock, 0);
      }

    } else { // node is not locally locked
      
      if (debugger.isEnabled() && logTrace) {
        traceState();
        if (currentlyLockingPriority == priority) {
          debugger.debug("Got remote lock since no local lock exists");
        } else {
          debugger.debug("Didn't get remote lock since other higher remote lock exists");
        }
      }

      return new LockRequestResponse(InterconnectProtocol.SUCCESSFUL_LOCK_NO_COMPARISON_REQ, 0);
    }

  }


  private void setCurrentlyLockedPriorityForRemoteLockRequest(long priority) {
    if (currentLockCircleSize == 1 && currentlyLockingPriority != null) {
      // nothing to be done if the currently locking prio is set because that is the one that is set within the
      // old lock circle and should not be overwritten
    } else if (currentlyLockingPriority == null || priority > currentlyLockingPriority) {
      currentlyLockingPriority = priority;
    }
  }


  private LockRequestResponse requestLockLocally(final long priority, boolean preliminary, boolean tryLock,
                                                 long timeoutNano) throws ObjectDeletedWhileWaitingForLockException,
                  InterruptedException {
    
    if (recalled) { // this node was already part of the current circle and is not allowed to enter again
      if (!waitForLockTokenToBeNotified(lockCircleProtection, tryLock, timeoutNano)) {
        return LockRequestResponse.TIMEOUT;
      }
    }

    // support reentrant locking
    if (localRequestLock != null && localRequestLock.isHeldByCurrentThread()) {
      localRequestLock.lock();
      // the "lock circle is closed" attribute is not used in this case
      return new LockRequestResponse(InterconnectProtocol.RESPONSE_REENTRANT_LOCAL_LOCK, 0);
    }

    if (preliminaryLock != null && preliminaryLock.isHeldByCurrentThread()) {
      preliminaryLock.lock();
      return new LockRequestResponse(InterconnectProtocol.RESPONSE_REENTRANT_LOCAL_LOCK, 0);
    }

    // wait until there are no more locks blocking the local lock
    try {
      while (true) {

        if (deleted) {
          throw new ObjectDeletedWhileWaitingForLockException();
        }

        if (locks != null && !locks.isEmpty()) {
          long firstKey = locks.firstKey();
          LockToken lock = locks.get(firstKey);
          if (!waitForLockTokenToBeNotified(lock, tryLock, timeoutNano)) {
            return LockRequestResponse.TIMEOUT;
          }
          // continue here to perform the deleted check once more. if otherwise the object would have been
          // deleted and the circle size is > 0, the following lock circle protection will never be passed
          // since the lock circle is not counted down for deleted objects.
          continue;
        }

        // check for existing lock circles remaining to be resolved
        if (currentLockCircleSize > 0) {
          if (!waitForLockTokenToBeNotified(lockCircleProtection, tryLock, timeoutNano)) {
            return LockRequestResponse.TIMEOUT;
          }
          continue; // see continue above for comments
        }

        if (preliminaryLock != null) {
          if (!checkPreliminaryLock(tryLock, timeoutNano)) {
            return LockRequestResponse.TIMEOUT;
          }
          continue; // see continue above for comments
        }

        break;

      } // end while

    } catch (InterruptedException e) {
      handleInterruptedException(e);
    }

    if (preliminary) {
      preliminaryLock = new ReentrantLock(); // (LockTokenType.PRELIMINARY);
      preliminaryLock.lock();
      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug("Got preliminary lock.");
      }

      return new LockRequestResponse(InterconnectProtocol.SUCCESSFUL_LOCK_NO_COMPARISON_REQ, 0);
    } else {
      // now get the actual lock object that is used to implement local reentrancy and create a lock token
      lazyCreateLocalRequestLock();
      localRequestLock.lock();
      currentlyLockingPriority = priority;
      lazyCreateLocksTable();
      locks.put(priority, new LockToken(LockTokenType.STANDARD));
      localRequestPriorityHoldingLock = priority;

      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug(new Object() {
          @Override
          public String toString() {
            return "Got lock locally with priority <" + priority + ">";
          }
        });
      }

      // return the default response since the lock is not reentrant
      return new LockRequestResponse(InterconnectProtocol.SUCCESSFUL_LOCK_NO_COMPARISON_REQ, 0);
    }
  }


  private void lazyCreateLocalRequestLock() {
    if (localRequestLock == null) {
      localRequestLock = new ReentrantLock();
    }
  }


  private void handleInterruptedException(InterruptedException e) {
    if (debugger.isEnabled() && logTrace) {
      traceState();
    }
    throw new RuntimeException("Got interrupted unexpectedly while waiting for local lock", e);
  }


  private boolean waitForLockTokenToBeNotified(final LockToken lock, boolean tryLock, long timeoutNano)
                  throws InterruptedException, ObjectDeletedWhileWaitingForLockException {
    lazyCreateWaitingForNonReentrantLockTokensCount();
    waitingForNonReentrantLockTokensCount.incrementAndGet();
    try {
      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug(new Object() {
          private String lockStr = lock.toString();
          @Override
          public String toString() {
            return "Waiting for lock <" + lockStr + ">";
          }
        });
      }
      lock.numberOfWaitingThreads.incrementAndGet();
      lockObjectProtection.unlock();
      try {
        synchronized (lock) {
          if (!lock.isNotified()) {
            if (tryLock) {
              if (!LockingUtils.wait(lock, timeoutNano)) {
                if (deleted) {
                  throw new ObjectDeletedWhileWaitingForLockException();
                }
                return false;
              }
            } else {
              lock.wait();
            }
            // TODO muss man notified-flag in locktoken auf false stellen??
          }
        }
      } finally {
        lockObjectProtection.lock();
        lock.numberOfWaitingThreads.decrementAndGet();
      }

      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug(new Object() {
          private String lockStr = lock.toString();         
          private final boolean recalledState = recalled;
          private final boolean fdeleted = deleted;
          @Override
          public String toString() {
            return "finished waiting for lock <" + lockStr + ">, deleted=" + fdeleted + ", recalled=" + recalledState;
          }
        });
      }

    } finally {
      waitingForNonReentrantLockTokensCount.decrementAndGet();
    }
    if (deleted) {
      throw new ObjectDeletedWhileWaitingForLockException();
    }

    return true;
  }


  public LockAwaitResponse awaitLock(final long priorityToWaitOn, boolean tryLock, long timeoutNano)
                  throws ObjectDeletedWhileWaitingForLockException, InterruptedException {
    /* @Depreacted ?
     * FIXME - falls der thread auf den gewartet wurde, wegen eines timeouts verschwindet, muss das await nun umziehen
     *         und auf die nächsthöhere priority warten 
     * - falls die prio auf die gewartet werden soll, bereits nicht mehr existiert, könnte sie aber durch einen timeout verschwunden sein.
     *   dann hat man das lock nicht und muss auf die nächsthöhere priority warten.
     */

    lockObjectProtection.lock();
    try {
      if (deleted) {
        throw new ObjectDeletedWhileWaitingForLockException();
      }
      
      // this cannot be a local call since the local calls just use requestLock
      final LockToken lockToken = locks != null ? locks.get(priorityToWaitOn) : null;
      if (lockToken == null) {
        if (debugger.isEnabled() && logTrace) {
          traceState();
          debugger.debug(new Object() {
            private long _priorityToWaitOn = priorityToWaitOn;
            final boolean recalledState = recalled;
            @Override
            public String toString() {
              return "Prio <" + _priorityToWaitOn + ">-lock has already been released (recalled="+recalledState+"), nothing to wait on";
            }
          });
        }
        // this can happen if the lock has already been released or recalled
        if (recalled) {
          return LockAwaitResponse.RECALLED;
        } else {
          return LockAwaitResponse.create(this);
        }
      }
      if (localRequestPriorityHoldingLock != priorityToWaitOn) {
        traceState();
        throw new ClusterInconsistentException("Can not wait on remote prio " + priorityToWaitOn
                        + "-lock for object id <" + objectId + ">");
      }

      try {
          do {
            if (!waitForLockTokenToBeNotified(lockToken, tryLock, timeoutNano)) {
              return LockAwaitResponse.TIMEOUT;
            }
            // the lock is removed once it is released, so if it still exists this is
            // expected to be due to a "spurious wakeup"
          } while (locks.containsKey(priorityToWaitOn)); // "locks" cannot be null here, see above
          if (recalled) {
            if (debugger.isEnabled()) {
              
              debugger.debug(new Object() {
                final boolean recalledState = recalled;
                @Override
                public String toString() {
                  return "recalledState: " + recalledState + " returning recalled answer";
                }
              });
            }
            return LockAwaitResponse.RECALLED;
          }
      } catch (InterruptedException e) {
        handleInterruptedException(e);
      } catch (ObjectDeletedWhileWaitingForLockException e) {
        if (debugger.isEnabled() && logTrace) {
          traceState();
          debugger.debug(new Object() {
            private long _priorityToWaitOn = priorityToWaitOn;
            @Override
            public String toString() {
              return "Prio <" + _priorityToWaitOn + ">-lock is obsolete since the object has been deleted";
            }
          });
        }
        throw e;
      }

      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug(new Object() {
          private String _lock = lockToken.toString();
          private Long _currentlyLockingPriority = currentlyLockingPriority;
          @Override
          public String toString() {
            return "Finished waiting for prio <" + priorityToWaitOn + ">-lock " + _lock + ", now locked: prio <"
                            + _currentlyLockingPriority + ">";
          }
        });
      }

    } finally {
      // at this point the thread will always be holding the lock
      lockObjectProtection.unlock();
    }

    return LockAwaitResponse.create(this);
  }

  public boolean releaseLock(boolean local, boolean countdownCircleOfTrustCounter) {
    return releaseLock(local, countdownCircleOfTrustCounter, localRequestPriorityHoldingLock);
  }


  public boolean releaseLock(boolean local, boolean countdownCircleOfTrustCounter, long priorityToRelease) {

    lockObjectProtection.lock();
    try {

      if (local) {
        boolean noLockToBeReleasedLocally = preliminaryLock == null &&
                        (locks == null || locks.isEmpty() || currentlyLockingPriority == null);
        if (noLockToBeReleasedLocally) {
          traceState();
          throw new ClusterInconsistentException("no lock to be released");
        }
        releaseLockLocally(countdownCircleOfTrustCounter);
      } else {
        boolean noLockToBeReleasedRemotely = locks == null || locks.isEmpty() || currentlyLockingPriority == null;
        if (noLockToBeReleasedRemotely) {
          traceState();
          throw new ClusterInconsistentException("no lock to be released");
        }
        releaseLockRemotely(countdownCircleOfTrustCounter, priorityToRelease);
      }

      return isInUse();

    } finally {
      lockObjectProtection.unlock();
    }

  }


  private boolean releasePreliminaryLock() {
    if (preliminaryLock != null) {
      // TODO prio5: performance: die reihenfolge mit null setzen mag hier eigentlich gar nicht wichtig sein,
      // weil kein anderer relevanter thread das preliminary lock checkt, ohne auch das lockobjectprotection
      // lock zu besitzen.
      ReentrantLock oldPreliminaryLock = preliminaryLock;
      preliminaryLock = null;

      // lock bekommen. aufs preliminary lock wartende threads sollen jetzt aufwachen und auf das normale lock warten.
      oldPreliminaryLock.unlock();
      if (oldPreliminaryLock.isHeldByCurrentThread()) {
        preliminaryLock = oldPreliminaryLock;
      }
      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug("released preliminary lock.");
      }
      return true; // preliminary lock freigegeben, fertig
    }
    return false;
  }


  private void releaseLockLocally(boolean countdownCircleOfTrustCounter) {

    if (releasePreliminaryLock()) {
      return;
    }

    if (localRequestLock == null) {
      throw new IllegalMonitorStateException();
    }

    // this is the local lock that is supposed to be released
    localRequestLock.unlock();

    // check whether the lock is reentrant
    if (!localRequestLock.isLocked()) {
      // = isLockedByCurrentThread(), since otherwise an IllegalMonitorStateException would have occurred during
      // unlock();
      // isLocked is cheaper

      // if locks is null here, this is a bug and the NPE is ok
      final LockToken releasedLocalLockToken = locks.remove(localRequestPriorityHoldingLock);
      final long oldLockingPrio = localRequestPriorityHoldingLock; // TODO prio4: die Variable existiert hier nur für logging!
      currentlyLockingPriority = locks.isEmpty() ? null : locks.lastKey();
      localRequestPriorityHoldingLock = -1;
      
      // count down the lock circle size if necessary
      // muss zb nicht gemacht werden, wenn das objekt shared ist und man ausschliesslich lokales lock hat
      if (countdownCircleOfTrustCounter) {
        decrementLockCircleSize();
      }
      if (currentLockCircleSize < 0) {
        traceState();
        throw new ClusterInconsistentException(
                                               "circle of trust is negative while releasing lock lockally for object id <"
                                                               + objectId + ">");
      } else if (currentLockCircleSize == 0) {
        lockCircleProtection.notifyLock();
        lockCircleProtection = new LockToken(LockTokenType.CIRCLE_PROTECTION);
      }
      
      notifyLockTokenAndDebug(releasedLocalLockToken, oldLockingPrio);

    } else if (debugger.isEnabled() && logTrace) {
      traceState();
      debugger.debug(new Object() {
        private Long _currentlyLockingPriority = localRequestPriorityHoldingLock;
        @Override
        public String toString() {
          return "Prio <" + _currentlyLockingPriority + ">-lock is still locked by same thread.";
        }
      });
    }
  }


  private void decrementLockCircleSize() {
    currentLockCircleSize--;
    if (currentLockCircleSize == 0) {
      recalled = false;
    }
  }


  private void releaseLockRemotely(boolean countdownCircleOfTrustCounter, long priorityToRelease) {

    // "locks" cannot be null here if the releaseLock method isnt called due to a bug
    final LockToken removedRemoteToken = locks.remove(priorityToRelease);
    final long oldLockingPrio = priorityToRelease; // TODO prio4: die Variable existiert hier nur für logging!
    currentlyLockingPriority = locks.isEmpty() ? null : locks.lastKey();
    if (countdownCircleOfTrustCounter) {
      decrementLockCircleSize();
    }
    if (currentLockCircleSize < 0) {
      traceState();
      throw new ClusterInconsistentException("circle of trust is negative while releasing lock remotely"
                      + " for object id <" + objectId + ">");
    } else if (currentLockCircleSize == 0) {
      lockCircleProtection.notifyLock();
      lockCircleProtection = new LockToken(LockTokenType.CIRCLE_PROTECTION);
    }

    if (removedRemoteToken.notifyLock()) {
      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug(new Object() {
          @Override
          public String toString() {
            return "Notifying prio <" + oldLockingPrio + ">-lock";
          }
        });
      }
    } else {
      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug(new Object() {
          private String removedTokenStr = removedRemoteToken.toString();
          @Override
          public String toString() {
            return "No one is waiting for prio " + oldLockingPrio + "-lock, <" + removedTokenStr
                            + ">, only removing lock token";
          }
        });
      }
    }
  }


  public boolean isInUse() {
    return preliminaryLock != null
                    || (locks != null && !locks.isEmpty())
                    || (localRequestLock != null && (localRequestLock.hasQueuedThreads() || localRequestLock.isLocked()))
                    || (waitingForNonReentrantLockTokensCount != null && waitingForNonReentrantLockTokensCount.get() > 0)
                    || currentLockCircleSize > 0;
    // die anzahl der auf die individuellen locks wartenden threads muss hier nicht mit angegeben werden, weil
    // schon in {@link #waitingForNonReentrantLockTokensCount} beinhaltet.
  }


  public boolean isLocallyReentrant() {
    lockObjectProtection.lock();
    try {
      return generalLocalHoldCount() > 1;
    } finally {
      lockObjectProtection.unlock();
    }
  }


  private int generalLocalHoldCount() {
    int holdCount = 0;
    if (localRequestLock != null) {
      holdCount = localRequestLock.getHoldCount();
    }
    if (holdCount == 0 && preliminaryLock != null) {
      holdCount = preliminaryLock.getHoldCount();
    }
    return holdCount;
  }


  public boolean isHeldLocally() {
    lockObjectProtection.lock();
    try {
      return generalLocalHoldCount() > 0;
    } finally {
      lockObjectProtection.unlock();
    }
  }


  public int closeLockCircle() {
    lockObjectProtection.lock();
    try {
      if (currentLockCircleSize > 0) {
        traceState();
        throw new ClusterInconsistentException("circle of trust has already been closed for object id <" + objectId
                        + ">");
      }
      if (preliminaryCircleSize < 1) {
        traceState();
        throw new ClusterInconsistentException("preliminary size of circle of trust has not been set before closing"
                        + " for object id <" + objectId + ">");
      }
      int originalLockCircleSize = preliminaryCircleSize;
      currentLockCircleSize = preliminaryCircleSize - recalledLocksCounter;
      recalledLocksCounter = 0;
      recalled = false;
      preliminaryCircleSize = -1;

      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug(new Object() {
          private int currentCircleSize = currentLockCircleSize;
          @Override
          public String toString() {
            return "circle of trust closed (circle size = " + currentCircleSize + ")";
          }
        });
      }
      return originalLockCircleSize;
    } finally {
      lockObjectProtection.unlock();
    }
  }


  private static class LockObjectDebug {

    private long _objectId;
    private Long _currentlyLockingPriority;
    private String _localRequestLock;
    private int _waitingForNonReentrantLockTokensCount;
    private long _localRequestPriorityHoldingLock;
    private int _currentLockCircleSize;
    private int _preliminaryCircleSize;
    private int _recalledLocksCounter;
    private boolean _recalledState;
    private String _preliminaryLock;
    private String _lockCircleProtection;
    private Map<Long, String> _locks;


    private LockObjectDebug(LockObject lockObject) {
      _locks = new HashMap<Long, String>();
      if (lockObject.locks != null) {
        for (Entry<Long, ?> e : lockObject.locks.entrySet()) {
          _locks.put(e.getKey(), e.getValue().toString());
        }
      }
      _objectId = lockObject.objectId;
      _currentlyLockingPriority = lockObject.currentlyLockingPriority;
      _localRequestLock = lockObject.localRequestLock == null ? null : lockObject.localRequestLock.toString();
      _waitingForNonReentrantLockTokensCount = lockObject.waitingForNonReentrantLockTokensCount != null ? lockObject.waitingForNonReentrantLockTokensCount
                      .get() : 0;
      _localRequestPriorityHoldingLock = lockObject.localRequestPriorityHoldingLock;
      _currentLockCircleSize = lockObject.currentLockCircleSize;
      _preliminaryCircleSize = lockObject.preliminaryCircleSize;
      _preliminaryLock = lockObject.preliminaryLock == null ? null : lockObject.preliminaryLock.toString();
      _lockCircleProtection = lockObject.lockCircleProtection == null ? null : lockObject.lockCircleProtection
                      .toString();
      _recalledLocksCounter = lockObject.recalledLocksCounter;
      _recalledState = lockObject.recalled;
    }


    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder(500);
      writeState(sb, _objectId, _locks, _currentlyLockingPriority, _localRequestLock,
                 _waitingForNonReentrantLockTokensCount, _localRequestPriorityHoldingLock, _currentLockCircleSize,
                 _preliminaryCircleSize, _recalledLocksCounter, _recalledState, _preliminaryLock, _lockCircleProtection);
      return sb.toString();
    }
  }


  private void traceState() {
    lockObjectProtection.lock();
    debugger.debug(new LockObjectDebug(this));
    checkConsistency();
    lockObjectProtection.unlock();
  }


  private static void writeState(StringBuilder sb, long _objectId, Map<Long, ?> _locks, Long _currentlyLockingPriority,
                                 String _localRequestLock, int _waitingForNonReentrantLockTokensCount,
                                 long _localRequestPriorityHoldingLock, int _currentLockCircleSize,
                                 int _preliminaryCircleSize, int _recalledLocksCounter, boolean _recalledState,
                                 String _preliminaryLock, String _lockCircleProtection) {
    sb.append("\n-------- ").append(_objectId).append(" -------\n");
    sb.append("| locks = ");
    for (Entry<Long, ?> e : _locks.entrySet()) {
      sb.append(e.getKey()).append(" ->").append(e.getValue());
      sb.append(", ");
    }
    sb.append("\n");
    sb.append("| cL = ").append(_currentlyLockingPriority).append("\n");
    sb.append("| lL ").append(_localRequestLock).append("\n");
    sb.append("| wLcnt: ").append(_waitingForNonReentrantLockTokensCount).append("\n");
    sb.append("| lRprio: ").append(_localRequestPriorityHoldingLock).append("\n");
    sb.append("| lock circle: ").append(_currentLockCircleSize).append("\n");
    sb.append("| preliminary circle size: ").append(_preliminaryCircleSize).append("\n");
    sb.append("| recalled locks counter: ").append(_recalledLocksCounter).append("\n");
    sb.append("| recalled : ").append(_recalledState).append("\n");
    sb.append("| preliminary lock: ").append(_preliminaryLock).append("\n");
    sb.append("| lockcircle lock: ").append(_lockCircleProtection).append("\n");
    sb.append("---------------\n");
  }


  private void checkConsistency() {
    int inconsistent = -1;
    int locksSize = locks != null ? locks.size() : 0;
    if (locksSize > 0 && currentlyLockingPriority == null) {
      inconsistent = 0;
    }
    if (locksSize == 0 && currentlyLockingPriority != null) {
      inconsistent = 1;
    }
    if (localRequestLock != null && localRequestLock.isLocked()) {
      if (localRequestPriorityHoldingLock < 0 || locksSize == 0) {
        inconsistent = 2;
      }
    } else {
      if (localRequestPriorityHoldingLock >= 0) {
        inconsistent = 3;
      }
    }
    if (localRequestPriorityHoldingLock >= 0) {
      if (locks == null || !locks.containsKey(localRequestPriorityHoldingLock)) {
        inconsistent = 4;
      }
    }
    if (waitingForNonReentrantLockTokensCount != null && waitingForNonReentrantLockTokensCount.get() < 0) {
      inconsistent = 5;
    }
    if (currentLockCircleSize < 0) {
      inconsistent = 6;
    }
    if (currentLockCircleSize > 1 && locksSize > currentLockCircleSize) {
      //zirkel kann noch nicht halboffen sein. kleiner kann locksSize sein, falls die requests noch nicht angekommen sind.
      //größer ist auf jeden fall falsch
      
      // Inkonsistenzstatus auskommentiert.
      // Beispielszenario bei dem Konsistenzprüfung fehlschlägt, obwohl Zustand nicht falsch ist:
      // Es gibt 3 Nodes, es entsteht ein Lockzirkel aus zwei Nodes, wobei bei einem Node das Lock austimed. Der Node, der gewonnen hat,
      // bekommt den recall allerdings noch nicht mit und versendet nach Abarbeitung sein release. Der dritte Node, der beim Lockzirkel
      // außen vor blieb, hat recall und release bereits erhalten und macht nun einen neuen Zirkel auf.
      
      //inconsistent = 7;
      debugger.debug("checkConsistency() detect locksSize > currentLockCircleSize - perhaps an inconsistent, perhaps not");

    }
    if (currentLockCircleSize > 0 && preliminaryCircleSize > 0) {
      inconsistent = 8;
    }
    if (recalledLocksCounter < 0) {
      inconsistent = 9;
    }
    if (recalledLocksCounter - preliminaryCircleSize > 0 && preliminaryCircleSize > 0) {
      inconsistent = 10;
    }
    if (inconsistent > -1) {
      throw new ClusterInconsistentException("inconsistent lockstate type=" + inconsistent + " for object <" + objectId
                      + ">");
    }
  }


  public boolean isLockCircleClosed() {
    if (debugger.isEnabled() && logTrace) {
      traceState();
      debugger.debug("called isLockCircleClosed");
    }
    return currentLockCircleSize > 0;
  }


  public void setPreliminaryLockCircleSize(final int i, boolean optional) {
    lockObjectProtection.lock();
    try {
      if (debugger.isEnabled() && logTrace) {
        traceState();
        final int finalRecalledLocksCounter = recalledLocksCounter;
        debugger.debug(new Object() {
          @Override
          public String toString() {
            return "setPreliminaryLockCircleSize to " + i + ". recalledLockCounter=" + finalRecalledLocksCounter;
          }
        });
      }
      if (this.currentLockCircleSize > 0) {
        if (optional) {
          return;
        } else {
          traceState();
          throw new ClusterInconsistentException("Tried to set preliminary lock "
                          + "circle size while circle is already closed for object id <" + objectId + ">");
        }
      }
      this.preliminaryCircleSize = i;
    } finally {
      lockObjectProtection.unlock();
    }
  }


  public LockObject createCopyForNewClusterNode() {
    LockObject o = new LockObject(objectId, logTrace);
    o.currentLockCircleSize = currentLockCircleSize;
    o.currentlyLockingPriority = currentlyLockingPriority;
    o.lockCircleProtection = new LockToken(LockTokenType.CIRCLE_PROTECTION);
    if (locks != null && !locks.isEmpty()) {
      // TODO prio5: it might be more efficient to use the clone method of TreeMap
      o.locks = new TreeMap<Long, LockToken>();
      for (Long prio : locks.keySet()) {
        o.locks.put(prio, new LockToken(LockTokenType.STANDARD));
      }
    }
    // the foreign/new cluster member can never be the lock circle winner and will thus
    // never access the preliminary lock circle size
    o.preliminaryCircleSize = -1;
    // FIXME recalledLocksCounter??
    return o;
  }


  public CoherenceLockState getLockState() {
    CoherenceLockState state = new CoherenceLockState();
    state.objectId = objectId;
    state.currentLockCircleSize = currentLockCircleSize;
    state.currentlyLockingPriority = currentlyLockingPriority;
    state.lockedPriorities = locks != null ? Collections.unmodifiableSet(locks.keySet()) : new HashSet<Long>();
    state.preliminaryLockCircleSize = preliminaryCircleSize;
    state.localRequestPriorityHoldingLock = localRequestPriorityHoldingLock;
    return state;
  }


  /**
   * non reentrant lock
   */
  public LockRequestResponse requestPreliminaryLock(boolean tryLock, long nanoTimeout) throws ObjectDeletedWhileWaitingForLockException {
    lockObjectProtection.lock();
    try {
      if (deleted) {
        throw new ObjectDeletedWhileWaitingForLockException();
      }
      try {
        return requestLockLocally(-1, true, tryLock, nanoTimeout);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } finally {
      lockObjectProtection.unlock();
    }
  }


  public void upgradePreliminaryLock(long priority) throws ObjectDeletedWhileWaitingForLockException {
    lockObjectProtection.lock();
    try {
      if (preliminaryLock == null) {
        traceState();
        throw new ClusterInconsistentException("can not upgrade nonexistant preliminary lock.");
      }
      if (deleted) {
        throw new ObjectDeletedWhileWaitingForLockException();
      }
      ReentrantLock oldPreliminaryLock = preliminaryLock;
      preliminaryLock = null;

      int holdCount = oldPreliminaryLock.getHoldCount();
      try {
        // reentrancy behandeln. wenn man das preliminary lock mehrfach hatte, muss man auch das "normale" lock mehrfach holen.
        for (int i = 0; i < holdCount; i++) {
          requestLockLocally(priority, false, false, -1);
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
        // FIXME kann es passieren, dass man erst einen fehler im reentrancy-fall hat? muss man dann locks wieder freigeben?
      } finally {
        // lock bekommen. aufs preliminary lock wartende threads sollen jetzt aufwachen und auf das normale lock warten.
        // auch wenn eine ObjectDeletedWhileWaitingForLockException geworfen wird.
        for (int i = 0; i < holdCount; i++) {
          oldPreliminaryLock.unlock();
        }
      }

      if (debugger.isEnabled() && logTrace) {
        traceState();
        debugger.debug("Upgraded lock.");
      }

    } finally {
      lockObjectProtection.unlock();
    }
  }


  @Override
  public String toString() {
    lockObjectProtection.lock();
    try {
      return new LockObjectDebug(this).toString();
    } finally {
      lockObjectProtection.unlock();
    }
  }


  public boolean isLockPreliminary() {
    // kein lockobjectprotection notwendig, weil das nur beim unlock aufgerufen wird, und dann kein
    // anderer thread das preliminarylock holen kann.
    return preliminaryLock != null;
  }


  public int closeLockCircleIfNecessaryAndGetCircleSize() {
    lockObjectProtection.lock();
    try {
      if (!isLockCircleClosed() && preliminaryCircleSize > 0 && !isLocallyReentrant()) {
        return closeLockCircle();
      } else {
        if (debugger.isEnabled()) {
          debugger.debug(new Object() {
            final boolean isLockCircleClosed = isLockCircleClosed();
            final int finalPreliminaryCircleSize = preliminaryCircleSize;
            final boolean isLocallyReentrant = isLocallyReentrant();
            @Override
            public String toString() {
              return "Decision to not close circle based on: isLockCircleClosed=" +isLockCircleClosed + 
                     ", preliminaryCircleSize=" +finalPreliminaryCircleSize+ ", isLocallyReentrant=" +isLocallyReentrant;
            }
          });
        }
        return -1;
      }
    } finally {
      lockObjectProtection.unlock();
    }
  }


  private volatile boolean deleted;


  public void setDeleted() {
    lockObjectProtection.lock();
    try {
      deleted = true;
      if (locks != null && locks.size() > 0) {
        for (LockToken token : locks.values()) {
          token.notifyLock();
        }
        if (lockCircleProtection != null) {
          lockCircleProtection.notifyLock();
        }
      }
    } finally {
      lockObjectProtection.unlock();
    }
  }


  public boolean isDeleted() {
    return deleted;
  }
  
  
  public long getLocalRequestPriority() {
    return localRequestPriorityHoldingLock;
  }
  

}
