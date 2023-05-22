/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xsched.scheduling;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xprc.xsched.XynaScheduler;


public class XynaSchedulerCustomisationUtils {
  
  private static Logger logger = CentralFactoryLogging.getLogger(XynaSchedulerCustomisationUtils.class);
  
  /**
   * Wartet solange, bis mehr als 10% Prozent des Speichers frei sind
   */
  public static void waitForFreeMemory() {
    double freeMem = examineFreeMem();
    double waitForFree = 0.1;
    while ( freeMem < waitForFree ) {
      logger.info( "waiting for "+((int)(waitForFree*100))+"percent free memory" );
      try {
        Thread.sleep( 500 );
      }
      catch (InterruptedException e) {
        //dann halt k�rzer warten
      }
      freeMem = examineFreeMem();
    }
  }

  /**
   * ruft GC, loggt Speichernutzung, berechnet Anteil des freien Speichers
   * @return
   */
  public static double examineFreeMem() {
    Runtime runtime = Runtime.getRuntime();
    long beforeGc = System.currentTimeMillis();
    runtime.gc();
    long afterGc = System.currentTimeMillis();
    
    long mega = 1024*1024;
    long max = runtime.maxMemory();          //-Xmx oder Long.MAX_VALUE
    long allocated = runtime.totalMemory();  //total memory in th jvm 
    long free = runtime.freeMemory();        //free
    long used = allocated-free;
    
    double freeFraction = (1.0*allocated - used)/allocated;
    
    logger.info( "memory usage in megabytes: "
    +"(max="+(max/mega)+",allocated="+(allocated/mega)+",free="+(free/mega)+",used="+(used/mega)+")"
    +" => "+((int)(freeFraction*100))+" percent free;"
    +" gc took "+(afterGc-beforeGc)+" ms to run");

    return freeFraction;
  }

  /**
   * Setzt TrySchedule, so dass nur noch SuspendAllOrders laufen darf,
   * und f�hrt die XynaFactory herunter
   */
  public static void shutdownFactory() {
    XynaScheduler scheduler = XynaFactory.getInstance().getProcessing().getXynaScheduler();
    scheduler.pauseScheduling( false, true ); 
    new Thread("SchedulerAlgorithm-ShutdownFactory"){ 
      public void run() {
        XynaFactory.getInstance().shutdown();
      }
    }.start();
  }

  /**
   * wirft Throwable weiter, so dass kein "throws Throwable" n�tig ist
   * @param t
   */
  public static void throwThrowable(Throwable t) {
    if( t instanceof RuntimeException ) {
      throw (RuntimeException)t;
    }
    if( t instanceof Error ) {
      throw (Error)t;
    }
    throw new RuntimeException(t);
  }

}
