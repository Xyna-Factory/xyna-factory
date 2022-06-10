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
package com.gip.xyna.utils.concurrent;

import java.util.concurrent.CountDownLatch;

import com.gip.xyna.utils.collections.Pair;


/**
 * JoinedExecutor führt eine Operation aus und gibt das Ergebnis an andere Threads weiter, 
 * die zeitgleich die gleiche Operation ausführen wollen.
 * <br><br>
 * Die Idee dabei ist, dass eine langdauernde Operation nicht von mehreren Thread gleichzeitig 
 * durchgeführt werden muss, wenn sie eh das gleiche Ergebnis erhalten würden. Stattdessen muss 
 * nur ein Thread die Operation ausführen, die anderen Threads warten einfach.
 * Die Threads können während der ganzen Ausführungszeit hinzukommen. Für spät hinzukommende 
 * Threads ergibt sich darüber hinaus der Vorteil, dass sie das Ergebnis wesentlich schneller 
 * erhalten, als wenn sie die Operation selbst ausführen müssten.
 * <br>
 * Voraussetzung ist, dass das Ergebnis der Operation nicht vom Thread und der genauen Startzeit abhängt.
 * <br><br>
 * Verhalten bei Exceptions:
 * <ul>
 * <li>DeclaredExceptions sind nicht zugelassen</li>
 * <li>RuntimeExceptions werden an alle wartenden Threads verteilt und weitergeworfen</li>
 * <li>Errors und übrige Throwables erhält nur der ausführende Thread; die anderen 
 *     Threads versuchen die Ausführung noch einmal</li>
 * </ul>
 * <br>
 * Mögliche Anwendungsfälle:
 * <ul>
 * <li>Datenbank-Lookup von sich selten ändernden Daten</li>
 * <li>Ermittlung des Status einer Software-Komponente</li> 
 * </ul>
 * Verwendung:
 * Die abstrakte Methode {@link #executeInternal()} muss implementiert werden.
 * Danach wird dann die Methode {@link #execute()} von den Threads gerufen.
 * <br>
 * Beispiel-Code:
 * <pre>
  private static class JoinedExecutorCounter extends JoinedExecutor&lt;Integer&gt; {
    AtomicInteger ai = new AtomicInteger(0);
    public Integer executeInternal() {
      try{ Thread.sleep(10); } catch( InterruptedException e ) {}
      return ai.getAndIncrement();
    }
  }
  
  JoinedExecutorCounter jec = new JoinedExecutorCounter();
  jec.execute(); 
  //wenn nun ein anderer Thread in den nächsten 10 ms jec.execute() aufruft, 
  //erhalten beide den gleichen Wert
 * </pre>
 */
public abstract class JoinedExecutor<R> {
  
  private volatile CountDownLatch latch;
  private volatile Result<R> lastResult;

  private static class Result<R> {
     public Result(R result) {
      this.normalResult = result;
    }
    public Result(Object dummy, boolean hasToRetry) {
      this.hasToRetry = hasToRetry;
    }
    public Result(Object dummy, RuntimeException exception) {
      this.exception = exception;
    }
    R normalResult;
    RuntimeException exception;
    boolean hasToRetry;
  }
  
  /**
   * eigentlich Ausführung der Operation
   * Behandlung der Exceptions:
   * <ul>
   * <li>DeclaredExceptions sind nicht zugelassen</li>
   * <li>RuntimeExceptions werden an alle wartenden Threads verteilt und weitergeworfen</li>
   * <li>Errors und übrige Throwables erhält nur der ausführende Thread; die anderen
   * Threads versuchen die Ausführung noch einmal</li>  
   * </ul> 
   * @return
   */
  protected abstract R executeInternal();
  
  /**
   * Ausführung der Operation.
   * Diese Methode sammelt alle Threads, die diese Methode gleichzeitig ausführen wollen. Der erste Thread
   * muss die Operation ausführen, alle anderen Threads erhalten das gleiche Ergebnis.
   * Evtl. müssen die wartenden Threads die Operation wiederholen, wenn der ausführende Thread mit einen 
   * schweren Fehler beendet wird. 
   * @return
   */
  public R execute() throws InterruptedException {
    Result<R> result = null;
    do {
      Pair<CountDownLatch,Boolean> pair = checkExecutionState();
      CountDownLatch localLatch = pair.getFirst();
      if( pair.getSecond() ) {
        //erster Thread muss Task ausführen
        result = executeAndCountDown(localLatch);
      } else {
        //weitere Threads hängen sich an und übernehmen das Ergebnis
        result = awaitExecution(localLatch);
      }
    } while( result.hasToRetry );
    if( result.exception != null ) {
      throw result.exception;
    }
    return result.normalResult;   
  }


  /**
   * Diese Methode entscheidet, welcher Thread der erste ist und legt dafür das Latch an
   * @return Pair<Latch,Boolean> mit true -> erster Thread
   */
  private Pair<CountDownLatch, Boolean> checkExecutionState() {
    CountDownLatch localLatch = latch;
    if( localLatch == null ) {
      //anscheinend der erste, daher sicher das Latch erzeugen
      synchronized (this) {
        localLatch = latch;
        if (localLatch != null) {
          //nun ist doch jemand zuvorgekommen
          return Pair.of(localLatch,Boolean.FALSE);
        } else {
          localLatch = new CountDownLatch(1);
          latch = localLatch;
          return Pair.of(localLatch,Boolean.TRUE);
        }
      }
    } else {
      return Pair.of(localLatch,Boolean.FALSE);
    }
  }
  
  /**
   * Erster Thread muss die Operation ausführen und das Latch freigeben
   * @param localLatch
   * @return
   */
  private Result<R> executeAndCountDown(CountDownLatch localLatch) {
    boolean noResult = true;
    try {
      lastResult = new Result<R>(executeInternal());
      noResult = false;
    } catch ( RuntimeException e ) {
      lastResult = new Result<R>(null, e);
      noResult = false;
    } finally {
      if( noResult ) { //tritt auf, wenn Error oder Throwable (ohne Exception oder RuntimeException) geworfen worden sind
        lastResult = new Result<R>(null, true);
      }
      localLatch.countDown();
      latch = null;
    }
    return lastResult;
  }

  /**
   * Alle anderen Threads warten auf die Freigabe des Latch und übernehmen das Ergebnis
   * @param localLatch 
   * @return
   * @throws InterruptedException 
   */
  private Result<R> awaitExecution(CountDownLatch localLatch) throws InterruptedException {
    localLatch.await();
    return lastResult;
  }
  
}
