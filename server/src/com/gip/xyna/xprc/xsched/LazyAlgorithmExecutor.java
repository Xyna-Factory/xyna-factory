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
package com.gip.xyna.xprc.xsched;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.concurrent.ReusableCountDownLatch;

/**
 * Führt einen eventuell langlaufenden Algorithmus in einem eigenen Thread solange nacheinander aus, bis
 * keine Requests mehr kommen, dass er ausgeführt werden soll. Dann schläft der Thread, bis neue Requests
 * kommen.
 * Der Algorithmus wird nicht so oft ausgeführt wie die Anzahl der Requests, sondern die Ausführung ist zeitlich 
 * an die Requests gebunden.
 * D.h. nach jedem Request läuft der Algorithmus mindestens einmal vollständig durch, aber nicht unbedingt zweimal.
 * 
 * Der Algorithmus kann zur Laufzeit ausgetauscht werden, was dazu führt, dass baldmöglichst zum anderen Algorithmus gewechselt wird.
 * 
 * Man kann konfigurieren, dass der Algorithmus periodisch aufgerufen wird, auch wenn kein normaler Request ankommt.
 * 
 * Es wird nicht garantiert, dass der Algorithm nicht läuft, wenn keine Requests kommen (vgl spurious wakeups. könnte man ändern...)
 */
public class LazyAlgorithmExecutor<ALG extends Algorithm> implements Runnable {

  private static final Logger logger = CentralFactoryLogging.getLogger(LazyAlgorithmExecutor.class);

  private volatile boolean threadMayRun = false;
  private boolean threadIsAsleep = false;
  private int loopsWithoutSleep = 0;
  private volatile boolean isPaused = false;
  private Thread thread;
  //regelt zugriff auf threadIsAsleep und goToSleep
  private Object sleepLock = new Object();
  private ReusableCountDownLatch awaitExecution = new ReusableCountDownLatch(1);
  private volatile ALG algorithm;
  private AtomicInteger notifiedCount = new AtomicInteger();
  private volatile boolean algorithmChangeRequested = false;
  private String name;
  private String sleepMessage;
  private String wokenMessage;
  private Runnable initialization;
  private Throwable threadDeathCause;
  private long threadDeathTimestamp;
  private final long periodicWakeupInterval; //0 = kein periodic wakeup

  public LazyAlgorithmExecutor(String name) {
    this(name, 0);
  }

  public LazyAlgorithmExecutor(String name, long periodicWakeupInterval) {
    this.periodicWakeupInterval = periodicWakeupInterval;
    this.name = name;
    sleepMessage = name + " goes to sleep.";
    wokenMessage = name + " has been woken up.";
  }

  /**
   * stoppt thread gracefully, d.h. er läuft seine jetzige schleife zuende und beendet sich danach.
   */
  public void stopThread() {
    threadMayRun = false;
    requestExecution();
  }


  /**
   * thread schläft solange bis er durch unPause aufgeweckt wird
   */
  public void pauseExecution() {
    //TODO schleife unterbrechen!?
    //thread kann nicht interrupted werden
    isPaused = true;
  }


  public void unPauseExecution() {
    isPaused = false;
    requestExecution();
  }


  /**
   * ausführung fordern. ausführung sofern nicht bereits laufend.
   * @return true, falls ausführung direkt beginnt. false falls nicht, weil z.B. noch laufend.
   */
  public boolean requestExecution() {
    if( notifiedCount.getAndIncrement() == 0 ) {
      //interrupted thread, so dass er erneut läuft
      synchronized (sleepLock) {
        if (threadIsAsleep && !isPaused) {
          sleepLock.notify(); //schlafenden thread interrupten
          return true;
        } else { //TODO isPaused ??
          //entweder vor loopAsLongAsRequested() => alles ok, das passiert dann gleich
          //oder am ende davon/kurz danach (bevor das sleeplock geholt wird)
          loopsWithoutSleep = Math.max(1, loopsWithoutSleep);
        }
      }
      return false;
    } else {
      // ansonsten läuft er noch... => signalisierung, dass er nochmal laufen soll
      return false;
    }
    
  }

  /**
   * Achtung: kehrt evtl. nach einer bereits laufenden Ausführung zurück
   * @throws InterruptedException
   */
  public void awaitExecution() throws InterruptedException {
    CountDownLatch cdl = awaitExecution.prepareLatch();
    requestExecution();
    cdl.await();
  }
  
  /**
   * Achtung: kehrt evtl. nach einer bereits laufenden Ausführung zurück
   * @throws InterruptedException
   */
  public boolean awaitExecution(long timeout, TimeUnit unit) throws InterruptedException {
    CountDownLatch cdl = awaitExecution.prepareLatch();
    requestExecution();
    return cdl.await(timeout,unit);
  }
  
  public boolean awaitTermination(long timeout) throws InterruptedException {
    Thread threadCopy = thread;
    if (threadCopy == null ||
        !threadCopy.isAlive()) {
      return true;
    } else {
      threadCopy.join(timeout);
      return threadCopy.isAlive();
    }
  }
  
  /**
   * kann nur einmal gestartet werden. falls der thread noch existiert, macht dieser aufruf nichts.
   * falls algorithm null ist, wird versucht der alte algorithm zu benutzen. falls keiner vorhanden, wird ein fehler geworfen.
   */
  public synchronized void startNewThread(ALG algorithm) {
    if (thread == null) {
      if (algorithm == null) {
        if (this.algorithm == null) {
          throw new RuntimeException("can not start new thread without algorithm.");
        }
      } else {
        this.algorithm = algorithm;
      }
      threadMayRun = true;
      thread = new Thread(this, name + "Thread");
      try {
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
      } catch (Throwable t) {
        handleThrowable(t);
        thread = null;
      }
    }
  }
  
  /**
   * kann nur einmal gestartet werden. falls der thread noch existiert, macht dieser aufruf nichts.
   * falls algorithm null ist, wird versucht der alte algorithm zu benutzen. falls keiner vorhanden, wird ein fehler geworfen.
   * Setzt Runnable initialization, der dirket nach dem Start des Thread einmal ausgeführt wird
   * @param initialization
   * @param algorithm
   */
  public synchronized void startNewThread(Runnable initialization, ALG algorithm) {
    this.initialization = initialization;
    startNewThread(algorithm);
  }


  /**
   * falls der alte algorithmus noch am laufen ist, wird er noch fertig ausgeführt und erst der nächste durchlauf
   * passiert mit dem neuen algorithmus
   */
  public void changeAlgorithm(ALG algorithm) {
    if (algorithm != this.algorithm) {
      this.algorithm = algorithm;
      //entweder ist noch alter algorithm am laufen, oder er ist am schlafen.
      //falls am laufen, wird er durch das flag unterbrochen und soll dann wieder mit dem neuen algo loslaufen
      synchronized (sleepLock) {
        if (threadIsAsleep && !isPaused) {
          sleepLock.notify(); //schlafenden thread interrupten
          algorithmChangeRequested = false;
        } else { //TODO isPaused ??
          //entweder am laufen => ok, wird unterbrochen und läuft dann mit neuem gleich nochmal
          //oder kurz vorher => läuft mit neuem algorithmus los und wird dann unterbrochen, geht aber nicht schlafen und läuft dann nochmal
          //oder danach => läuft direkt nochmal mit neuem algorithmus, wird unterbrochen, und geht dann nochmal nicht schlafen
          algorithmChangeRequested = true;
          loopsWithoutSleep = Math.max(2, loopsWithoutSleep);
        }
      }
    } else {
      algorithmChangeRequested = false;
    }
  }


  private void loopAsLongAsRequested() {
    notifiedCount.getAndIncrement();//immer mindestens einmal laufen
    while( notifiedCount.getAndSet(0) > 0 && !algorithmChangeRequested ) {
      notifiedCount.getAndIncrement(); //damit in requestExecution() Thread nicht aufgeweckt wird
      
      algorithm.exec();
      
      awaitExecution.countDown();
      notifiedCount.getAndDecrement(); //runtersetzen, damit keine weitere Ausführung passiert ausser requestExecution() gerufen
    }
    if (algorithmChangeRequested) {
      algorithmChangeRequested = false;
      notifiedCount.set(1);
    }
  }


  public void run() {
    threadDeathCause = null;
    threadDeathTimestamp = 0;
    try {
      if (logger.isDebugEnabled()) {
        logger.debug(name + " started.");
      }
      if( initialization != null ) {
        initialization.run();
        if (logger.isDebugEnabled()) {
          logger.debug("initialized");
        }
        initialization = null;
      }
      
      while (threadMayRun) {
        loopAsLongAsRequested();
        synchronized (sleepLock) {
          if (threadMayRun) {
            if (loopsWithoutSleep > 0) {
              loopsWithoutSleep--;
              continue;
            }
            logger.trace(sleepMessage);
            threadIsAsleep = true;
            sleepLock.wait(periodicWakeupInterval);
            threadIsAsleep = false;
            logger.trace(wokenMessage);
          }
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug(name + " was shut down.");
      }
    } catch (Throwable t) {
      handleThrowable(t);
    } finally {
      thread = null; //damit der thread wieder gestartet werden kann
    }
  }

  private void handleThrowable(Throwable t) {
    Department.handleThrowable(t);
    onFatalError(t);
    threadDeathTimestamp = System.currentTimeMillis();
    threadDeathCause = t;
  }

  public ALG getCurrentAlgorithm() {
    return algorithm;
  }
  
  
  public boolean threadIsAsleep() {
    return threadIsAsleep;
  }
  
  public boolean isPaused() {
    return isPaused;
  }
  
  public boolean isRunning() {
    return (thread != null);
  }
  
  public Throwable getThreadDeathCause() {
    return threadDeathCause;
  }
  
  public long getThreadDeathTimestamp() {
    return threadDeathTimestamp;
  }  
  
  public void onFatalError(Throwable t) {
    
  }

  public StackTraceElement[] getThreadState() {
    if (thread == null) {
      return new StackTraceElement[0];
    }
    return thread.getStackTrace();
  }
  
};
