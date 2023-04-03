/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon.db.dbFiller;

import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.DemonProperties;
import com.gip.xyna.utils.snmp.SnmpAccessData;
import com.gip.xyna.utils.snmp.manager.SnmpContextImplApache;

public class TrapSender implements Runnable {
  final static Logger logger = Logger.getLogger(TrapSender.class);
  private static final String OID_leaseLogTrap = ".1.3.6.1.4.1.28747.1.13.1.4.2";
  
  private SnmpAccessData snmpAccessData;
  private volatile boolean running;
  private volatile String trapData;
  private final ReentrantLock lock = new ReentrantLock(); 
  private final Condition nextTrap = lock.newCondition();
  private long startTime;
  private String name; 
  
  /**
   * @param snmpAccessData
   */
  public TrapSender(String name, SnmpAccessData snmpAccessData) {
    this.name = name;
    this.snmpAccessData = snmpAccessData;
    startTime = DemonProperties.getLongProperty(DemonProperties.START_TIME);
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    logger.info("New thread "+name+" has started");
    SnmpContextImplApache trapsender = null;
    try {
      try {
        trapsender = new SnmpContextImplApache(snmpAccessData);
      } catch (IOException e) {
        //SnmpContext kann nicht angelegt werden
        logger.error( "Error while initializing TrapSender",e );
      }

      while( running ) {
        sendToAndWait(trapsender);
      }
    } finally {
      if( trapsender != null ) {
        trapsender.close();
      }
      running = false;
      logger.info("Thread "+name+" has finished");
    }
  }

  
  /**
   * @param trapsender
   */
  private void sendToAndWait(SnmpContextImplApache trapsender) {
    lock.lock();
    try  { 
      //Traps verschicken
      if( trapData != null ) {
        long upTime = System.currentTimeMillis()-startTime;
        trapsender.trap(OID_leaseLogTrap, upTime, null, name);
        trapData = null;
      }
      //warten auf nächste Trap
      while( running && trapData == null ) {
        try {
          nextTrap.await();
        } catch (InterruptedException e) {
          //ignorieren, einfach weiter warten
        } 
      } 
    } 
    finally { 
      lock.unlock(); 
    }
  }

  /**
   * 
   */
  public void stop() {
    if( running ) {
      running = false;
      wakeUp();
    }
  }

  /**
   * 
   */
  private void wakeUp() {
    lock.lock(); 
    try  { 
      nextTrap.signalAll(); 
    } 
    finally { 
      lock.unlock(); 
    } 
  }

  /**
   * @param trapData
   */
  public void sendTrap(@SuppressWarnings("hiding") String trapData) {
    this.trapData = trapData;
    logger.info( "Send trap "+trapData);
    if( ! running ) {
      running = true;
      new Thread(this,name).start();
    } else {
      wakeUp();
    }
  }

}
