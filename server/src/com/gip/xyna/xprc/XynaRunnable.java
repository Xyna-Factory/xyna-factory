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

package com.gip.xyna.xprc;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



public abstract class XynaRunnable implements Runnable {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(XynaRunnable.class);

  private Thread executingThread;
  private String caller;

  public XynaRunnable() {
    this.caller = "UNKNOWN";
  }
  

  public XynaRunnable(String caller) {
    this.caller = caller;
  }


  void setExecutingThread(Thread t) {
    if (executingThread != null && t != null) {
      //xyna runnables können wegen der thread-referenzen nicht parallel wiederverwendet werden.
      //siehe bugz 12663. das führt zu ganz fiesen problemen.

      //TODO eigentlich hilft die abfrage so nicht immer, weil executingThread nicht volatile ist.
      //aber lohnt es sich deshalb, hier eine threadsichere abfrage einzubauen?
      throw new RuntimeException("xyna runnable is still in use " + executingThread + " /" + t + "/" + this);
    }
    if (logger.isTraceEnabled()) {
      logger.trace("set executing thread " + t + " in " + this);
    }
    this.executingThread = t;
  }


  Thread getExecutingThread() {
    return this.executingThread;
  }


  String getCaller() {
    return caller;
  }

  /**
   * wird aufgerufen, wenn dieses Runnable abgebrochen wurde, weil es zu lange in der ThreadPoolExecutor-Queue gewartet hatte. (~RingBuffer-Strategy)
   */
  public void rejected() {
    throw new UnsupportedOperationException();
  }
  
  /**
   * ist rejected() implementiert/supported
   */
  public boolean isRejectable() {
    return false;
  }
  
  public String toString() {
    return caller+"("+super.toString()+")";
  }

  /**
   * wird von XynaThreadPoolExecutor aufgerufen, bevor RejectionHandler ausgeführt wird. 
   * ACHTUNG: Falls false zurückgegeben wird, wird dem Einsteller des Tasks in den Threadpool nicht Bescheid gegeben, dass das Task nicht ausgeführt wird.
   */
  public boolean mayCallRejectionHandlerOnRejection() {
    return true;
  }

}
