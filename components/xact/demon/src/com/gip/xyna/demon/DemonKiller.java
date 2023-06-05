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
package com.gip.xyna.demon;

import org.apache.log4j.Logger;

public class DemonKiller extends Thread {
  static Logger logger = Logger.getLogger(DemonKiller.class.getName());

  private Object mutex = new Object();
  private volatile boolean killed;
  
  public DemonKiller() {
    super("DemonKiller");
  }
  
  @Override
  public void run() {
    killed = false;
    
    synchronized(mutex) {
      while( ! killed ) {
        try {
          mutex.wait();
        } catch (InterruptedException e) {
          //ignore until killed == true
        }
      }
      killJvm();//Kill the demon
    }
    
  }

  /**
   * Kill the demon
   */
  private void killJvm() {
    logger.info( "Demon will be killed in one second");
    new Thread() {
      @Override
      public void run() {
        try {
          Thread.sleep(1000); //etwas warten, damit der DemonSnmpAgent seine Antwort schicken kann
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        logger.info( "Demon will be killed now");
        System.exit(1);
      }
    }.start();
  }

  public void kill() {
    synchronized( mutex ) {
      killed = true;
      mutex.notifyAll();
    }
    killJvm();
  }
  
}
