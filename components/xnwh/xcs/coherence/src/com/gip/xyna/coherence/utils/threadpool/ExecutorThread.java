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
package com.gip.xyna.coherence.utils.threadpool;



/**
 * kapselt einen thread, der solange aktiv bleibt, bis {@link #shutdown()} aufgerufen wird. in dieser zeit kann
 * sequentiell so oft wie gewünscht ein runnable in dem thread mittels {@link #execute(Runnable)} ausgeführt werden.
 * sequentiell bedeutet in diesem fall, dass erneut {@link #execute(Runnable)} aufgerufen werden kann, sobald das
 * vorherige Runnable ausgeführt worden ist. Dies kann man z.B. dadurch erkennen, dass der FinishedListener aufgerufen
 * wird. <br>
 * TODO:<br>
 * - fehlerbehandlung (was wenn eines der runnables fehler wirft?) <br>
 * - synchronisierung (was, wenn man execute aufruft, während der thread bereits in der ausführung begriffen ist <br>
 * - zugrundeliegenden thread lazy starten, sobald das erste mal execute aufgerufen wird <br>
 * - thread nicht suspenden (wait), wenn eh gleich der nächste request kommt. evtl ist es performanter, ihn dann aktiv warten zu lassen oder mit kurzen sleeps?
 */
public class ExecutorThread implements Runnable, Shutdownable {

  private Thread t;
  private Object lock = new Object();
  private volatile Runnable toExecute;
  private volatile boolean alive = true;
  private Runnable finalizer;


  public ExecutorThread() {
    t = new Thread(this);
  }


  /**
   * startet den zugrundeliegenden thread
   */
  public void start() {
    t.start();
  }


  /**
   * code to run after execution finished
   */
  public void setFinishedListener(Runnable r) {
    finalizer = r;
  }


  public void execute(Runnable r) {
    toExecute = r;
    synchronized (lock) {
      lock.notify();
    }
  }


  public void run() {
    while (true) {
      while (toExecute == null && alive) {
        synchronized (lock) {
          if (toExecute == null && alive) {
            try {
              lock.wait();
            } catch (InterruptedException e) {
              //TODO prio5: InterruptedException behandeln? Im Moment stellt der Thread einfach die Arbeit ein.
              // Das ist möglicherweise schon das, was man hier erwartet.
            }
          }
        }
      }
      if (alive) {
        try {
          Runnable temp = toExecute;
          toExecute = null;
          temp.run();
        } finally {
          if (finalizer != null) {
            finalizer.run();
          }
        }
      } else {
        return;
      }
    }
  }


  public void shutdown() {
    alive = false;
    synchronized (lock) {
      lock.notify();
    }
  }
}
