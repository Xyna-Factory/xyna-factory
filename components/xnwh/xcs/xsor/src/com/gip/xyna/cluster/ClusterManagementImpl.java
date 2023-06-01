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
package com.gip.xyna.cluster;



import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.debug.Debugger;



/**
 * implementierung des zustandsautomaten des clusterstates.
 * 
 * beschreibung:
 * der clusterstate ändert sich im normalfall wie folgt:
 * INIT -> STARTUP (getriggert über setState) -> ein SYNC-Zustand -> CONNECTED -> SHUTDOWN.
 * 
 * änderungen in der erreichbarkeit werden nur über {@link #setOtherNodeAvailable(boolean)} durchgeführt.
 * falls beim senden timeouts passieren, wird solange wiederholt bis die erreichbarkeit sich geändert hat.
 * 
 * bei jeder clusterstate änderung werden alle registrierten {@link StateChangeHandler} aufgerufen. bevor der
 * clusterstate den ziel-wert erreicht, wird für alle registrierten {@link StateChangeHandler} parallel die methode
 * {@link StateChangeHandler#readyForStateChange(ClusterState, ClusterState)} aufgerufen. wenn alle 
 * {@link StateChangeHandler} geantwortet haben, wird die stateänderung sichtbar gemacht und dann wieder für 
 * alle parallel die methode {@link StateChangeHandler#onChange(ClusterState, ClusterState)} aufgerufen.
 * 
 * 
 * 
 * verwendung für entwickler:
 * - es muss eine komponente geben, die den availability-change an diese klasse weiterleitet.
 * - die kommunikation mit dem entfernten knoten muss implementiert werden. beim empfangen an {@link #getThisNodeImpl()} weiterleiten,
 * beim senden wird immer die im konstruktor übergebene implementierung des {@link ClusterNodeRemoteInterface} verwendet.
 * 
 */
/*
 * TODO: sync-zustände aus der klasse rausnehmen und separat behandeln als zwischenzustand vor connected,
 */
public class ClusterManagementImpl implements ClusterManagement, ClusterNodeRemoteInterface {

  private static final Debugger debugger = Debugger.getInstance();
  private static final long TIMEOUT_WAITING_FOR_AVAILABLE_MS = 30000; //TODO konfigurierbar?!


  /**
   * führt statechanges durch, die sich durch änderungen in der erreichbarkeit ergeben
   */
  private class AvailabilityChangeHandler implements Runnable {

    public void run() {
      availabilityLastChangedTo = available;
      while (availabilityChangeHandlerIsRunning) {
        //darauf warten, notifiziert zu werden
        synchronized (availabilityLock) {
          while (available == availabilityLastChangedTo && availabilityChangeHandlerIsRunning) {
            try {
              debugger.trace("going to sleep");
              availabilityLock.wait();
            } catch (InterruptedException e) {
              if (!availabilityChangeHandlerIsRunning) {
                return;
              }
            }
            debugger.trace("woke up");
          }
        }

        //so lange changeAvailability aufrufen, bis available konsistent gesetzt wurde.
        while (true) {
          boolean localAvailable = available;
          if (localAvailable != availabilityLastChangedTo) {
            availabilityLastChangedTo = localAvailable;
            try {
              doChangeAvailability();
            } catch (Throwable t) {
              debugger.error("error during changeAvailability", t);
            }
          }
          synchronized (availabilityLock) {
            if (available == availabilityLastChangedTo) {
              break;
            }
            //else nochmal ändern.
          }
        }
      }
    }

  }

  /**
   * führt die asynchron durchzuführenden statechanges durch
   */
  private class StateChanger implements Runnable {

    public void run() {
      while (stateChangerThreadIsRunning) {
        synchronized (stateChangerThreadLock) {
          while (nextState == null && stateChangerThreadIsRunning) {
            try {
              debugger.trace("going to sleep");
              stateChangerThreadLock.wait();
            } catch (InterruptedException e) {
            }
            debugger.trace("woke up");
          }
        }
        ClusterState stateToChangeTo;
        synchronized (stateChangerThreadLock) {
          stateToChangeTo = nextState;
          nextState = null;
        }
        if (stateToChangeTo != null) {
          try {
            changeStateInternally(stateToChangeTo, false);
          } catch (Throwable t) {
            debugger.error("error during state change", t);
          }
        }
      }
    }

  }


  private final ReentrantLock stateChangeLock = new ReentrantLock();
  //beides synchronisiert durch statechangelock:
  private volatile ClusterState currentState;
  private volatile boolean wasMaster; //der letzte zustand, der zwischen master/slave unterscheidet war master.

  private volatile ClusterState nextState; //für asynchrone zustandsänderungen merkt man sich den als nächstes durchzuführenden statechange

  private final Object stateChangerThreadLock = new Object();
  private volatile boolean stateChangerThreadIsRunning;
  private final Thread stateChangerThread;
  private AtomicLong stateModificationCount = new AtomicLong(0);

  private final List<StateChangeHandler> handlers; //registrierte handler

  //threadpool für die parallele durchführung von statechangehandlern
  private final ThreadPoolExecutor threadpool = new ThreadPoolExecutor(2, 5, 20, TimeUnit.SECONDS,
                                                                       new LinkedBlockingQueue<Runnable>(20),
                                                                       new ThreadFactory() {
                                                                         private AtomicInteger cnt = new AtomicInteger(0);
                                                                         public Thread newThread(Runnable r) {
                                                                           return new Thread(r, "StateChangeHandlerExecutorThread-"
                                                                                                 + cnt.getAndIncrement());
                                                                         }
                                                                       });
  private final ThreadPoolExecutor threadpoolForAfterStateChangeHandlers =
      new ThreadPoolExecutor(1, 3, 20, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(20), new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
          return new Thread(r, "AfterStateChangeExecutorThread-" + cnt.getAndIncrement());
        }
      });

  private boolean availabilityLastChangedTo; //muss nicht volatile sein, weil wird nur von einem thread gelesen udn geschrieben.
  private final Object availabilityLock = new Object();
  private volatile boolean available = false;
  private volatile boolean availabilityChangeHandlerIsRunning;
  private final Thread availabilityChangeHandlerThread;

  private final ClusterNodeRemoteInterface otherNode;

  private volatile boolean otherNodeSyncFinished; //immer auf false setzen, wenn der andere einen zu sync auffordert oder wenn man selbst den anderen zu sync auffordert.
  private volatile SyncType syncStateOfOtherNode;

  /**
   * diese zahl ist die anzahl der syncFinished-aufrufe aus der applikation, auf die gewartet wird vor dem
   * zustandsübergang nach CONNECTED. 
   */
  private int syncFinishedConditions = 0;
  private final AtomicInteger syncFinishedCounter = new AtomicInteger(0);
  private long availabilityDelayMs;

  /**
   * @param remoteInterface implementierung des remoteInterfaces des anderen knoten: wie erreicht man den anderen knoten?
   * @param wasMasterBefore true, falls  dieser knoten früher master gewesen ist.
   * @param availabilityDelayMs verzögerung von STARTUP -> NEVERCONNECTED, falls anderer knoten nicht erreichbar ist. bedingt durch 
   *        "lag" des erreichbarkeitdaemons
   */
  public ClusterManagementImpl(ClusterNodeRemoteInterface remoteInterface, boolean wasMasterBefore, long availabilityDelayMs) {
    this.availabilityDelayMs = availabilityDelayMs;
    currentState = ClusterState.INIT;
    handlers = new ArrayList<StateChangeHandler>();
    otherNode = remoteInterface;
    wasMaster = wasMasterBefore;

    availabilityChangeHandlerThread =
        new Thread(new AvailabilityChangeHandler(), AvailabilityChangeHandler.class.getSimpleName() + "-Thread");
    availabilityChangeHandlerThread.setDaemon(true);
    availabilityChangeHandlerIsRunning = true;
    availabilityChangeHandlerThread.start();

    stateChangerThread = new Thread(new StateChanger(), StateChanger.class.getSimpleName() + "-Thread");
    stateChangerThread.setDaemon(true);
    stateChangerThreadIsRunning = true;
    stateChangerThread.start();
  }


  public ClusterNodeRemoteInterface getThisNodeImpl() {
    return this;
  }


  /**
   * per default wird der übergang von SYNC_X auf CONNECTED versucht durchzuführen, sobald alle statechangehandler 
   * ausgeführt wurden.
   * über die methode {@link #registerSyncFinishedCondition()} wird eine
   * zusätzliche bedingung registriert, auf die gewartet wird, bis versucht wird auf CONNECTED zu wechseln. 
   * 
   * zu dem zeitpunkt, wo die bedingung erfüllt ist, muss {@link #notifySyncFinishedCondition()} aufgerufen werden.
   * 
   * dies ist besser als einfach zu verlangen, dass der synchronisierungsvorgang der applikation synchron im statechangehandler
   * zu passieren hat.
   * 
   * TODO nicht threadsicher! muss es aber derzeit auch nicht sein.
   */
  public void registerSyncFinishedCondition() {
    syncFinishedConditions++;
  }


  public void notifySyncFinishedCondition() {
    debugger.debug("notifySyncFinishedCondition called " + (syncFinishedConditions - syncFinishedCounter.get() + 1)
        + " / " + syncFinishedConditions);
    if (currentState.isSync()) {
      syncFinishedCounter.decrementAndGet();
      synchronized (syncFinishedCounter) {
        syncFinishedCounter.notify();
      }
    }
  }


  public ClusterState getCurrentState() {
    return currentState;
  }


  /**
   * merkt sich den asynchron zu setzenden nächsten clusterstate. überschreibt weitere clusterstates, 
   * die sich in der warteschlange befinden.
   * unterbricht nicht änderungen, die bereits durchgeführt werden. 
   */
  private void changeStateInternallyAsync(final ClusterState newState) {
    debugger.debug(new Object() {

      public String toString() {
        return "async state change request to state " + newState;
      }
    });
    debugger.trace("requested by this stack: ", new Exception());
    synchronized (stateChangerThreadLock) {
      if (currentState == newState) {
        nextState = null;
        return;
      }
      nextState = newState;
      stateChangerThreadLock.notify();
    }
  }


  /**
   * synchrone durchführung eines statechanges.
   * besonderheiten: 
   * - nach STARTUP gehts direkt weiter mit dem versuch zu synchronisieren
   * - nach synchronisation gehts direkt weiter mit dem versuch nach CONNECTED zu gehen
   * - falls SHUTDOWN, wird dies dem anderen knoten mitgeteilt (falls möglich)
   */
  private void changeStateInternally(final ClusterState newState, boolean syncAfterChangeHandlerExecution) {
    if (currentState == newState || nextState == newState) {
      return;
    }
    debugger.trace(new Object() {

      public String toString() {
        return "called changeState to " + newState + " by:";
      }
    }, new Exception());

    stateChangeLock.lock();
    try {
      if (currentState == newState || nextState == newState) {
        return;
      } else if (currentState == ClusterState.CONNECTED && newState.isSync()) {
        return; //already done.
      }
      debugger.info(new Object() {

        public String toString() {
          return "checking for statechange to " + newState;
        }
      });

      synchronized (handlers) {
        final CountDownLatch latch = new CountDownLatch(handlers.size());
        //parallel aufrufen in eigenen threads 
        for (final StateChangeHandler handler : handlers) {
          Runnable r = new Runnable() {

            public void run() {
              try {
                while (!handler.readyForStateChange(currentState, newState)) {
                  try {
                    Thread.sleep(50);
                  } catch (InterruptedException e) {
                  }
                }
              } catch (Throwable t) {
                debugger.warn("error executing StateChangeHandler.isReadyForChange() " + handler + " (" + currentState
                    + " -> " + newState + ")", t);
              }
              latch.countDown();
            }

          };
          boolean success = false;
          while (!success) {
            try {
              threadpool.execute(r);
              success = true;
            } catch (RejectedExecutionException e) {
              try {
                Thread.sleep(100);
              } catch (InterruptedException e1) {
              }
            }
          }
        }
        try {
          latch.await();
        } catch (InterruptedException e) {
        }
      }
      debugger.info(new Object() {

        public String toString() {
          return "changing state to " + newState;
        }
      });

      final ClusterState oldState = currentState;

      //dinge, die vor dem ändern des zustands passieren müssen, aber nach den clusterstatechangehandlern.
      if (newState == ClusterState.SHUTDOWN) {
        shutdownProcess();
      }

      setWasMaster(newState);
      currentState = newState;
      if (currentState.isSync()) {
        syncFinishedCounter.set(syncFinishedConditions);
      }
      stateModificationCount.incrementAndGet();
      synchronized (handlers) {
        final CountDownLatch latch = new CountDownLatch(handlers.size());
        //parallel aufrufen in eigenen threads 
        for (final StateChangeHandler handler : handlers) {
          Runnable r = new Runnable() {

            public void run() {
              try {
                handler.onChange(oldState, newState);
              } catch (Throwable t) {
                debugger.warn("error executing StateChangeHandler.onChange() " + handler + " (" + oldState + " -> "
                    + newState + ")", t);
              }
              latch.countDown();
            }

          };
          boolean success = false;
          while (!success) {
            try {
              threadpool.execute(r);
              success = true;
            } catch (RejectedExecutionException e) {
              try {
                Thread.sleep(100);
              } catch (InterruptedException e1) {
              }
            }
          }
        }
        try {
          latch.await();
        } catch (InterruptedException e) {
        }
      }
    } finally {
      stateChangeLock.unlock();
    }

    Runnable r = new Runnable() {

      public void run() {
        if (newState == ClusterState.STARTUP) {
          //nach startup gehts automatisch weiter richtung sync
          afterStartupProcess();
        } else if (newState.isSync()) {
          tryConnectAfterSync();
        }

      }
      
    };
    if (syncAfterChangeHandlerExecution) {
      r.run();
    } else {
      threadpoolForAfterStateChangeHandlers.execute(r);
    }
  }


  private void shutdownProcess() {
    stopAvailabilityChangeHandlerThread();
    switch (currentState) {
      case SYNC_SLAVE :
      case SYNC_PARTNER :
      case CONNECTED :
        //dem anderen knoten bescheid geben
        try {
          otherNode.changeState(ClusterState.DISCONNECTED_MASTER);
        } catch (TimeoutException e) {
          //pech, der andere wirds von allein merken.
        } catch (RuntimeException e) {
          //pech, mehr kann man da jetzt auch nicht tun
          debugger.error("could not trigger remote state change to " + ClusterState.DISCONNECTED_MASTER, e);
        }
        break;
      case SYNC_MASTER :
        //dem anderen knoten bescheid geben
        try {
          otherNode.changeState(ClusterState.SHUTDOWN); //ohne vernünftige daten (als slave) kann der andere nicht leben
        } catch (TimeoutException e) {
          //pech, der andere wirds von allein merken.
        } catch (RuntimeException e) {
          //pech, mehr kann man da jetzt auch nicht tun
          debugger.error("could not trigger remote state change to " + ClusterState.SHUTDOWN, e);
        }
        break;
      case DISCONNECTED :
      case NEVER_CONNECTED :
      case DISCONNECTED_MASTER :
      case INIT :
      case SHUTDOWN :
      case STARTUP :
      default :
        //ntbd
    }
  }


  private void stopAvailabilityChangeHandlerThread() {
    availabilityChangeHandlerIsRunning = false;
    synchronized (availabilityLock) {
      availabilityLock.notify();
    }
  }


  private void stopStateChangerThread() {
    stateChangerThreadIsRunning = false;
    synchronized (stateChangerThreadLock) {
      stateChangerThreadLock.notify();
    }
  }


  private void tryConnectAfterSync() {
    debugger.debug(new Object() {

      private final ClusterState l_currentState = currentState;
      private final SyncType l_syncStateOfOtherNode = syncStateOfOtherNode;


      public String toString() {
        return "tryConnectAfterSync currentState=" + l_currentState + ", syncStateOfOtherNode="
            + l_syncStateOfOtherNode;
      }
    });

    synchronized (syncFinishedCounter) {
      while (syncFinishedCounter.get() > 0 && currentState.isSync()) {
        try {
          syncFinishedCounter.wait(100);
        } catch (InterruptedException e) {
        }
      }
    }
    if (!currentState.isSync()) {
      debugger.debug("sync could not be finished");
      return;
    }
    debugger.debug("application sync is finished");

    //darauf warten, bis der andere knoten sich meldet und bescheid gibt, dass sync fertig ist. oder bis available auf false wechselt.
    //sync fertig bedeutet, dass sync-handler alle abgeschlossen sind.

    /*
     * achtung: der master darf nicht auf disconnect_master zurückspringen, wenn unklar ist, ob der andere schon auf connected war.
     * weil das würde dazu führen, dass ein knoten master ist und der andere trotzdem läuft (disconnected).
     * 
     * was aber passieren darf ist, dass der master auf disconnected geht und der slave auf shutdown.
     * 
     * => man darf erst otherNode.syncFinished() aufrufen, wenn der andere das ebenso signalisiert hat.
     * ausserdem muss man danach direkt auf CONNECTED oder DISCONNECTED wechseln.
     * 
     * daraus ergibt sich das problem: wenn beide SYNC_MASTER sind, gibt es ein deadlock
     * => nur oben beschriebenen algorithmus verwenden, wenn man selbst SYNC_MASTER ist, und der andere nicht auch SYNC_MASTER.
     */

    int retryCnt = 0;
    boolean gotTimeout = true;
    while (gotTimeout) {
      try {
        if (!(currentState == ClusterState.SYNC_MASTER && !otherNodeIsSyncMaster())) {
          //für sync_master erst nachdem der slave fertig ist.
          otherNode.syncFinished();
        } else {
          debugger
              .debug("Found current state SYNC_MASTER and other node's state not to be SYNC_MASTER => waiting to signal syncFinished until other node's notify.");
        }
        gotTimeout = false;

        while (!otherNodeSyncFinished) {
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
          }
          if (!available || !currentState.isSync()) {
            break;
          }
        }
      } catch (TimeoutException e) {
        if (retryCnt++ > 0) {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e1) {
          }
        }
        if (available && currentState.isSync()) {
          //retry
        } else {
          break;
        }
      }
    }

    stateChangeLock.lock();
    try {
      if (currentState.isSync()) {
        if (available) {
          if (currentState == ClusterState.SYNC_MASTER && !otherNodeIsSyncMaster()) {
            debugger
                .debug("Found current state SYNC_MASTER and other node's state not to be SYNC_MASTER => sending syncFinished now.");
            //der andere (slave) muss mit sync fertig sein.

            //in diesem block hat man das statechangelock und ist deshalb sicher,
            //dass kein anderer thread den übergang nach disconnected_master vollzieht, und hier trotzdem 
            //syncFinished aufgerufen wird.
            retryCnt = 0;
            gotTimeout = true;
            while (gotTimeout) {
              try {
                otherNode.syncFinished();
                gotTimeout = false;
                changeStateInternally(ClusterState.CONNECTED, false);
              } catch (TimeoutException e) {
                if (retryCnt++ > 0) {
                  stateChangeLock.unlock();
                  try {
                    Thread.sleep(200);
                  } catch (InterruptedException e1) {
                  } finally {
                    stateChangeLock.lock();
                  }
                }
                //erneut prüfen, weil das lock freigegeben war.
                if (available && currentState == ClusterState.SYNC_MASTER && !otherNodeIsSyncMaster()) {
                  continue; //ok retry.
                }
                debugger.info("syncFinished could not be sent.");
                changeStateInternally(ClusterState.DISCONNECTED, false);
              }
            }
          } else {
            changeStateInternally(ClusterState.CONNECTED, false);
          }
        } else {
          // wird von availabilitycheck gehandelt
        }
      } else {
        //ok, wurde bereits von einem anderen prozess geändert. passt.
      }
    } finally {
      syncStateOfOtherNode = null;
      stateChangeLock.unlock();
    }
  }


  private boolean otherNodeIsSyncMaster() {
    if (syncStateOfOtherNode == null) {
      throw new RuntimeException();
    }
    return syncStateOfOtherNode == SyncType.WAS_MASTER_BEFORE;
  }
  
  public SyncType getOtherNodesSyncType() {
    return syncStateOfOtherNode;
  }


  /**
   * setzt das wasMaster-flag passend zum übergebenen state.
   */
  private void setWasMaster(ClusterState newState) {
    switch (newState) {
      case CONNECTED :
      case DISCONNECTED :
      case NEVER_CONNECTED :
        wasMaster = false;
        break;
      case DISCONNECTED_MASTER :
        wasMaster = true;
        break;
      case INIT :
      case SHUTDOWN :
      case STARTUP :
      case SYNC_MASTER :
      case SYNC_SLAVE :
      case SYNC_PARTNER :
      default :
        //ntbd
    }
  }


  /**
   * wechsel zum übergebenen state, falls erlaubt.
   * erlaubt sind:
   * - DISCONNECTED_MASTER,
   * - DISCONNECTED
   * - SHUTDOWN
   * - STARTUP 
   */
  public void changeState(ClusterState newState) {

    /* 
     * setState wird aufgerufen von
     * 
     * nach deployment: clusterprovider -> startup 
     * 
     * administrativ von applikation:
     * godmode -> disconnected_master
     * 
     * ruft sich selbst auf:
     * herunterfahren -> shutdown
     * remote-aufruf shutdown -> disconnected_master/disconnected
     * 
     */

    switch (newState) {
      case DISCONNECTED :
      case DISCONNECTED_MASTER :
      case SHUTDOWN :
        if (currentState != ClusterState.INIT) {   
          //erlaubt
          break;
        } else {
          throw new RuntimeException("state " + newState + " may not be set explicitly in state " + currentState + ".");
        }
      case STARTUP :
        if (currentState == ClusterState.INIT) {
          break;
        }
      case CONNECTED :
      case NEVER_CONNECTED :
      case INIT :
      case SYNC_MASTER :
      case SYNC_SLAVE :
      case SYNC_PARTNER :
      default :
        //nicht erlaubt von extern zu setzen
        throw new RuntimeException("state " + newState + " may not be set explicitly in state " + currentState + ".");
    }

    if (newState == ClusterState.SHUTDOWN) {
      changeStateInternally(newState, false);
    } else {
      changeStateInternallyAsync(newState);
    }
  }


  private void afterStartupProcess() {
    stateChangeLock.lock();
    try {
      if (currentState == ClusterState.STARTUP) {
        if (wasMaster) {
          if (available) {
            otherNodeSyncFinished = false;
            SyncResponse response;
            int retryCnt = 0;
            boolean gotTimeout = true;
            while (gotTimeout) {
              try {
                stateChangeLock.unlock();
                try {
                  response = otherNode.syncWasMaster();
                } finally {
                  stateChangeLock.lock();
                }
                gotTimeout = false;
                syncStateOfOtherNode = response.getTypeOfSync();
                switch (response.getTypeOfSync()) {
                  case IMPOSSIBLE :
                    changeStateInternallyFreeLockTemporarily(ClusterState.DISCONNECTED_MASTER);
                    break;
                  case WAS_MASTER_BEFORE :
                  case WAS_CONNECTED_BEFORE :
                  case WAS_NEVER_CONNECTED_BEFORE :
                    if (currentState == ClusterState.STARTUP) {
                      changeStateInternallyFreeLockTemporarily(ClusterState.SYNC_MASTER);
                    }
                    break;
                  default :
                    throw new RuntimeException("unsupported state " + response.getTypeOfSync());
                }
              } catch (TimeoutException e) {
                if (retryCnt++ > 0) {
                  stateChangeLock.unlock();
                  try {
                    Thread.sleep(200);
                  } catch (InterruptedException e1) {
                  } finally {
                    stateChangeLock.lock();
                  }
                }
                if (available) {
                  if (currentState == ClusterState.STARTUP) {
                    //nochmal probieren
                    continue;
                  }
                } else {
                  if (currentState == ClusterState.STARTUP) {
                    changeStateInternallyFreeLockTemporarily(ClusterState.DISCONNECTED_MASTER);
                    return;
                  }
                }
              }
            }
          } else {
            changeStateInternallyFreeLockTemporarily(ClusterState.DISCONNECTED_MASTER);
          }
        } else {
          //nicht master
          if (available) {
            otherNodeSyncFinished = false;
            SyncResponse response;
            int retryCnt = 0;
            boolean gotTimeout = true;
            while (gotTimeout) {
              try {
                stateChangeLock.unlock();
                try {
                  response = otherNode.syncWasNeverConnectedBefore();
                } finally {
                  stateChangeLock.lock();
                }
                gotTimeout = false;
                syncStateOfOtherNode = response.getTypeOfSync();
                switch (response.getTypeOfSync()) {
                  case IMPOSSIBLE :
                    if (currentState == ClusterState.STARTUP) {
                      changeStateInternallyFreeLockTemporarily(ClusterState.NEVER_CONNECTED);
                    }
                    break;
                  case WAS_MASTER_BEFORE :
                    if (currentState == ClusterState.STARTUP) {
                      changeStateInternallyFreeLockTemporarily(ClusterState.SYNC_SLAVE);
                    }
                    break;
                  case WAS_CONNECTED_BEFORE :
                  case WAS_NEVER_CONNECTED_BEFORE :
                    if (currentState == ClusterState.STARTUP) {
                      changeStateInternallyFreeLockTemporarily(ClusterState.SYNC_PARTNER);
                    }
                    break;
                  default :
                    throw new RuntimeException("unsupported state " + response.getTypeOfSync());
                }
              } catch (TimeoutException e) {
                if (retryCnt++ > 0) {
                  stateChangeLock.unlock();
                  try {
                    Thread.sleep(200);
                  } catch (InterruptedException e1) {
                  } finally {
                    stateChangeLock.lock();
                  }
                }
                if (available) {
                  if (currentState == ClusterState.STARTUP) {
                    //nochmal probieren
                    continue;
                  } else {
                    //nicht mehr startup
                  }
                } else {
                  if (currentState == ClusterState.STARTUP) {
                    changeStateInternallyFreeLockTemporarily(ClusterState.NEVER_CONNECTED);
                    return;
                  } else {
                    //nicht mehr startup;
                  }
                }
              }
            }
          } else {
            final Timer t = new Timer(true);
            TimerTask task = new TimerTask() {
              
              @Override
              public void run() {
                stateChangeLock.lock();
                try {
                  if (currentState == ClusterState.STARTUP) {
                    if (available) {
                      //erreichbar geworden, wird von availabilitychangethread erledigt.
                    } else {
                      //ok immer noch nicht erreichbar -> never connected
                      changeStateInternallyFreeLockTemporarily(ClusterState.NEVER_CONNECTED);
                    }
                  }
                } finally {
                  stateChangeLock.unlock();
                  t.cancel(); //timerthread verschwinden lassen
                }
              }
            };
            t.schedule(task, availabilityDelayMs);
            //TODO task canceln, wenn nicht mehr benötigt. ist aber nicht so wichtig, weil dann das task eh nichts mehr tut...
          }
        }
      }
    } finally {
      stateChangeLock.unlock();
    }

  }


  private void changeStateInternallyFreeLockTemporarily(ClusterState newState) {
    stateChangeLock.unlock();
    try {
      changeStateInternally(newState, false);
    } finally {
      stateChangeLock.lock();
    }
  }


  public void registerStateChangeHandler(StateChangeHandler handler) {
    synchronized (handlers) {
      handlers.add(handler);
    }
  }


  public void setOtherNodeAvailable(boolean availableNew) {
    synchronized (availabilityLock) {
      this.available = availableNew; //wird immer direkt sichtbar. 
      availabilityLock.notifyAll();
    }
  }


  private void doChangeAvailability() {
    debugger.info(new Object() {

      private final boolean localCopyOfFlag = availabilityLastChangedTo;


      public String toString() {
        return "availability changed to " + localCopyOfFlag;
      }
    });
    if (availabilityLastChangedTo) {
      ClusterState currentStateLocalCopy;
      stateChangeLock.lock(); //locken, damit man hier auf ggfs laufende statechanges warten muss
      try {
        currentStateLocalCopy = currentState;
      } finally {
        stateChangeLock.unlock();
      }
      switch (currentStateLocalCopy) {
        case NEVER_CONNECTED :
          trySyncWasNeverConnectedBefore();
          break;
        case DISCONNECTED :
          trySyncWasConnectedBefore();
          break;
        case DISCONNECTED_MASTER :
          trySyncWasMaster();
          break;
        case STARTUP :
          //wird von afterstartupprozess erledigt, ausser der wartet gerade auf den timer, dann muss man das selbst erledigen.
          //in beiden fällen ist es nicht schlimm, einfach den afterstartupprozess nochmal aufzurufen, weil das dort
          //synchronisiert ist und nochmal den state abfragt.
          afterStartupProcess();
          break;
        default :
          //ntbd
      }
    } else {
      stateChangeLock.lock();
      try {
        switch (currentState) {
          case CONNECTED :
          case SYNC_PARTNER :
            changeStateInternally(ClusterState.DISCONNECTED, false);
            break;
          case SYNC_MASTER :
            changeStateInternally(ClusterState.DISCONNECTED_MASTER, false);
            break;
          case SYNC_SLAVE :
            changeStateInternally(ClusterState.SHUTDOWN, false);
            break;
          default :
            //ntbd
        }
      } finally {
        stateChangeLock.unlock();
      }
    }

  }


  private void waitForAvailable() throws TimeoutException {
    if (!available) {
      final long tMax = System.currentTimeMillis() + TIMEOUT_WAITING_FOR_AVAILABLE_MS;
      while (!available && System.currentTimeMillis() < tMax) {
        synchronized (availabilityLock) {
          if (!available) {
            try {
              availabilityLock.wait(Math.max(1, tMax - System.currentTimeMillis()));
            } catch (InterruptedException e) {
            }
          }
        }
      }
      if (!available) {
        throw new TimeoutException();
      } else {
        debugger.debug(new Object() {

          private final long diff = System.currentTimeMillis() - tMax + TIMEOUT_WAITING_FOR_AVAILABLE_MS;


          public String toString() {
            return "waited " + diff + "ms for availability.";
          }
        });
      }
    }
  }

  /**
   * sync neverconnected ~ transferalldata
   */
  private void trySyncWasNeverConnectedBefore() {
    otherNodeSyncFinished = false;
    int retryCnt = 0;
    SyncResponse r;
    boolean gotTimeout = true;
    while (gotTimeout) {
      try {
        r = otherNode.syncWasNeverConnectedBefore();
        gotTimeout = false;
        ClusterState copyOfCurrentState;
        stateChangeLock.lock();
        try {
          copyOfCurrentState = currentState;
          syncStateOfOtherNode = r.getTypeOfSync();
        } finally {
          stateChangeLock.unlock();
        }
        if (copyOfCurrentState == ClusterState.NEVER_CONNECTED) {
          switch (r.getTypeOfSync()) {
            case IMPOSSIBLE :
              //ntbd
              break;
            case WAS_MASTER_BEFORE :
              changeStateInternally(ClusterState.SYNC_SLAVE, false);
              break;
            case WAS_NEVER_CONNECTED_BEFORE :
            case WAS_CONNECTED_BEFORE :
              changeStateInternally(ClusterState.SYNC_PARTNER, false);
              break;
            default :
              throw new RuntimeException("unsupported state " + r.getTypeOfSync());
          }
        }
        
      } catch (TimeoutException e) {
        if (retryCnt++ > 0) {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e1) {
          }
        }
        if (available && currentState == ClusterState.NEVER_CONNECTED) {
          continue;
        } else {
          return; //nicht nochmal versuchen
        }
      }
    }
  }


  /**
   * sync was neverconnected
   */
  public SyncResponse syncWasNeverConnectedBefore() throws TimeoutException {
    waitForAvailableAndGetLock();
    try {
      otherNodeSyncFinished = false;
      syncStateOfOtherNode = SyncType.WAS_NEVER_CONNECTED_BEFORE;
      SyncType state = SyncType.IMPOSSIBLE;
      switch (currentState) {
        case DISCONNECTED_MASTER :
          state = SyncType.WAS_MASTER_BEFORE;
          changeStateInternallyAsync(ClusterState.SYNC_MASTER);
          break;
        case STARTUP :
          if (wasMaster) {
            state = SyncType.WAS_MASTER_BEFORE;
            changeStateInternallyAsync(ClusterState.SYNC_MASTER);
          } else {
            state = SyncType.WAS_NEVER_CONNECTED_BEFORE;
            changeStateInternallyAsync(ClusterState.SYNC_PARTNER);
          }
          break;
        case SYNC_MASTER :
          //so lassen
          state = SyncType.WAS_MASTER_BEFORE;
          break;
        case NEVER_CONNECTED :
        case SYNC_SLAVE :
          state = SyncType.WAS_NEVER_CONNECTED_BEFORE;
          changeStateInternallyAsync(ClusterState.SYNC_PARTNER);
          break;
        case CONNECTED :
        case DISCONNECTED :
          state = SyncType.WAS_NEVER_CONNECTED_BEFORE;
          changeStateInternallyAsync(ClusterState.SYNC_PARTNER);
          break;
        case SYNC_PARTNER :
          //so lassen
          state = SyncType.WAS_NEVER_CONNECTED_BEFORE;
          break;
        case INIT :
        case SHUTDOWN :
          //response auf IMPOSSIBLE belassen.
          break;
        default :
          throw new RuntimeException("unsupported state " + currentState);
      }
      return new SyncResponse(state);
    } finally {
      stateChangeLock.unlock();
    }
  }


  /**
   * sync was master ~ deletealldata
   */
  private void trySyncWasMaster() {
    otherNodeSyncFinished = false;
    int retryCnt = 0;
    SyncResponse r;
    boolean gotTimeout = true;
    while (gotTimeout) {
      try {
        r = otherNode.syncWasMaster();
        gotTimeout = false;
        if (r.getTypeOfSync() == SyncType.IMPOSSIBLE) {
          return;
        }
        ClusterState copyOfCurrentState;
        stateChangeLock.lock();
        try {
          syncStateOfOtherNode = r.getTypeOfSync();
          copyOfCurrentState = currentState;
        } finally {
          stateChangeLock.unlock();
        }
        if (copyOfCurrentState == ClusterState.DISCONNECTED_MASTER) {
          changeStateInternally(ClusterState.SYNC_MASTER, false);
        }
      } catch (TimeoutException e) {
        if (retryCnt++ > 0) {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e1) {
          }
        }
        if (available && currentState == ClusterState.DISCONNECTED_MASTER) {
          continue;
        } else {
          return; //nicht nochmal versuchen
        }
      }
    }
  }

  /**
   * wartet auf available und statechangelock maximal {@link #TIMEOUT_WAITING_FOR_AVAILABLE_MS} ms
   * falls eines von beiden in der zeit nicht bekommen werden kann, wird ein fehler geworfen.
   */
  private void waitForAvailableAndGetLock() throws TimeoutException {
    long t0 = System.currentTimeMillis();
    waitForAvailable();
    try {
      if (!stateChangeLock.tryLock(Math.max(0, TIMEOUT_WAITING_FOR_AVAILABLE_MS - System.currentTimeMillis() + t0), TimeUnit.MILLISECONDS)) {
        throw new TimeoutException();
      }
    } catch (InterruptedException e) {
      throw new TimeoutException();
    }
  }

  public SyncResponse syncWasMaster() throws TimeoutException {
    waitForAvailableAndGetLock();
    try {
      otherNodeSyncFinished = false;
      syncStateOfOtherNode = SyncType.WAS_MASTER_BEFORE;
      SyncType state = SyncType.IMPOSSIBLE;
      switch (currentState) {
        case NEVER_CONNECTED :
          state = SyncType.WAS_NEVER_CONNECTED_BEFORE;
          changeStateInternallyAsync(ClusterState.SYNC_SLAVE);
          break;
        case DISCONNECTED :
          state = SyncType.WAS_CONNECTED_BEFORE;
          changeStateInternallyAsync(ClusterState.SYNC_SLAVE);
          break;
        case DISCONNECTED_MASTER :
          state = SyncType.WAS_MASTER_BEFORE;
          changeStateInternallyAsync(ClusterState.SYNC_MASTER);
          break;
        case STARTUP :
          if (wasMaster) {
            state = SyncType.WAS_MASTER_BEFORE;
            changeStateInternallyAsync(ClusterState.SYNC_MASTER);
          } else {
            state = SyncType.WAS_NEVER_CONNECTED_BEFORE;
            changeStateInternallyAsync(ClusterState.SYNC_SLAVE);
          }
          break;
        case SYNC_MASTER :
          //so lassen
          state = SyncType.WAS_MASTER_BEFORE;
          break;
        case SYNC_SLAVE :
          //so lassen
          state = SyncType.WAS_NEVER_CONNECTED_BEFORE;
          break;
        case SYNC_PARTNER :
          throw new RuntimeException("unexpected state " + currentState);
        case CONNECTED :
        case INIT :
        case SHUTDOWN :
          //response auf IMPOSSIBLE belassen.
          break;
        default :
          throw new RuntimeException("unsupported state " + currentState);
      }
      //eigentlich muss man bei dieser antwort nur unterscheiden ob impossible oder nicht, weil der andere knoten die antwort nie unterscheidet
      return new SyncResponse(state);
    } finally {
      stateChangeLock.unlock();
    }
  }


  /**
   * sync was connected before ~ transferpendingdata
   */
  private void trySyncWasConnectedBefore() {
    otherNodeSyncFinished = false;
    SyncResponse r;
    int retryCnt = 0;
    boolean gotTimeout = true;
    while (gotTimeout) {
      try {
        r = otherNode.syncWasConnectedBefore();
        gotTimeout = false;
        ClusterState copyOfCurrentState;
        stateChangeLock.lock();
        try {
          copyOfCurrentState = currentState;
          syncStateOfOtherNode = r.getTypeOfSync();
        } finally {
          stateChangeLock.unlock();
        }
        if (copyOfCurrentState == ClusterState.DISCONNECTED) {
          switch (r.getTypeOfSync()) {
            case IMPOSSIBLE :
              //ntbd
              break;
            case WAS_MASTER_BEFORE :
              changeStateInternally(ClusterState.SYNC_SLAVE, false);
              break;
            case WAS_NEVER_CONNECTED_BEFORE :
            case WAS_CONNECTED_BEFORE :
              changeStateInternally(ClusterState.SYNC_PARTNER, false);
              break;
            default :
              throw new RuntimeException("unsupported state " + r.getTypeOfSync());
          }
        }

      } catch (TimeoutException e) {
        if (retryCnt++ > 0) {
          try {
            Thread.sleep(200);
          } catch (InterruptedException e1) {
          }
        }
        if (available && currentState == ClusterState.DISCONNECTED) {
          continue;
        } else {
          return; //nicht nochmal versuchen
        }
      }
    }

  }


  public SyncResponse syncWasConnectedBefore() throws TimeoutException {
    waitForAvailableAndGetLock();
    try {
      otherNodeSyncFinished = false;
      syncStateOfOtherNode = SyncType.WAS_CONNECTED_BEFORE;
      SyncType state = SyncType.IMPOSSIBLE;
      switch (currentState) {
        case DISCONNECTED_MASTER :
          state = SyncType.WAS_MASTER_BEFORE;
          changeStateInternallyAsync(ClusterState.SYNC_MASTER);
          break;
        case STARTUP :
          if (wasMaster) {
            state = SyncType.WAS_MASTER_BEFORE;
            changeStateInternallyAsync(ClusterState.SYNC_MASTER);
          } else {
            state = SyncType.WAS_NEVER_CONNECTED_BEFORE;
            changeStateInternallyAsync(ClusterState.SYNC_PARTNER);
          }
          break;
        case SYNC_MASTER :
          //so lassen
          state = SyncType.WAS_MASTER_BEFORE;
          break;
        case SYNC_SLAVE :
        case NEVER_CONNECTED :
          state = SyncType.WAS_NEVER_CONNECTED_BEFORE;
          changeStateInternallyAsync(ClusterState.SYNC_PARTNER);
          break;
        case CONNECTED :
        case DISCONNECTED :
          state = SyncType.WAS_CONNECTED_BEFORE;
          changeStateInternallyAsync(ClusterState.SYNC_PARTNER);
          break;
        case SYNC_PARTNER :
          //so lassen
          state = SyncType.WAS_CONNECTED_BEFORE;
          break;
        case INIT :
        case SHUTDOWN :
          //response auf IMPOSSIBLE belassen.
          break;
        default :
          throw new RuntimeException("unsupported state " + currentState);
      }
      return new SyncResponse(state);
    } finally {
      stateChangeLock.unlock();
    }
  }


  public boolean getWasMaster() {
    return wasMaster;
  }


  public void shutdown() {
    stopAvailabilityChangeHandlerThread();
    stopStateChangerThread();
    threadpool.shutdown();
  }


  public void syncFinished() throws TimeoutException {
    otherNodeSyncFinished = true;
  }


}
