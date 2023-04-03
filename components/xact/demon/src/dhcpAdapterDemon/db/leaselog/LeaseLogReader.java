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
package dhcpAdapterDemon.db.leaselog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import dhcpAdapterDemon.db.DBFillerStatistics;
import dhcpAdapterDemon.db.dbFiller.DBFiller;

/**
 * LeaseLogReader läuft als Thread im Hintergrund und trägt evtl. verlorengegangen 
 * LeaseLogPackets wieder in den DBFiller ein.
 * 
 * 
 * Einzuhaltende Bedingungen:
 * 1) Pakete dürfen nicht doppelt in PacketDBFiller eingetragen werden
 * 2) Pakete dürfen nicht übersehen werden
 * 3) Pakete dürfen nur eingetragen werden, wenn PacketDBFiller erfolgreich arbeitet
 * 4) Pakete dürfen nur eingetragen werden, wenn PacketDBFiller-RingBuffer weniger als halbvoll ist
 * 5) PacketDBFiller darf nicht aufgehalten werden
 *
 */
public class LeaseLogReader implements Runnable {
  static Logger logger = Logger.getLogger(LeaseLogReader.class.getName());

  private volatile boolean running;
  private Condition conditionNewData;
  private ReentrantLock lockData;
  private DBFiller<LeaseLogPacket> dbFiller;
  private String filename;
  private ArrayList<Long> timestampList;
  private AtomicInteger timestampListSize = new AtomicInteger();
  private AtomicInteger lostPacketsCounter = new AtomicInteger();
  private DBFillerStatistics dbFillerStatistics;
  private Status status = Status.WAITING;
  private volatile long newestPacket = Long.MAX_VALUE; //Long.MAX_VALUE hilft beim ersten Aufruf, falls LeaseLogDBFiller noch nicht lief
  private ArrayList<String> excludedFilenames;
  
  private static enum Status {
    WAITING, BUFFER_FULL, READING, SEARCHING}
  
  /**
   * @param dbFiller
   * @param filename 
   */
  public LeaseLogReader(DBFiller<LeaseLogPacket> dbFiller, DBFillerStatistics dbFillerStatistics, String filename) {
    this.dbFiller = dbFiller;
    this.dbFillerStatistics = dbFillerStatistics;
    this.filename = filename;
    timestampList = new ArrayList<Long>();
    lostPacketsCounter.set(1); //Vorbelegen, da bereits früher Pakete verlorengegangen sein können 
    lockData = new ReentrantLock();
    conditionNewData = lockData.newCondition();
    excludedFilenames = new ArrayList<String>();
    excludedFilenames.add("lost+found");
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    logger.debug("LeaseLogReader started");
    
    running = true;
    try {
      while( running ) {
        try { 
          lockData.lock();

          //warten, bis wirklich etwas zu tun ist
          while( ! isSomethingToBeDone() ) {
            logger.debug( "waiting for something to be done");
            status = Status.WAITING;
            conditionNewData.await();
          }

          //so, es muss was getan werden
          process();

        } catch ( InterruptedException e ) { 
          logger.error("Ignored exception",e);
        }
        finally { 
          lockData.unlock();
        }
      }
    } finally {
      running = false;
    }
  }

  
  
  /**
   * Liegt Arbeit vor?
   */
  private boolean isSomethingToBeDone() {
    if( timestampListSize.get() != 0 ) {
      return true; //alte Arbeit ist nicht fertig
    } 
    if( lostPacketsCounter.get() != 0 ) {
      return true; //es liegt neue Arbeit vor
    }
    return false; //nichts zu tun
  }

  /**
   * 
   */
  public void process() {
    logger.debug("process");
    
    if( timestampList.size() == 0 ) {
      status = Status.SEARCHING;
      logger.info( "List is empty, search for lost packets");
      //Liste ist leer, daher nach verlorenen Paketen suchen
      int lpcOld = lostPacketsCounter.getAndSet(0); //Info löschen, dass Pakete verloren sind
      if( lpcOld != 0 ) {
        //es sind Pakete verlorengegangen, daher diese suchen
        timestampList = searchLostPackets();
        timestampListSize.set(timestampList.size() );
      }
      logger.info( timestampList.size() +" lost packets found");
      logger.debug( timestampList.toString() );
    }
    
    int capacityHalf = dbFiller.getCapacity()/2;
    
    while( timestampList.size() != 0 ) {
      //alte Liste abarbeiten
      
      while( dbFiller.getWaiting() > capacityHalf ) {
        logger.debug( "waiting for full buffer in dbFiller");
        //Warten, falls der RingBuffer zu voll ist
        try {
          status = Status.BUFFER_FULL;
          conditionNewData.await(); //(notwendiger Lock wird bereits in run() geholt)
        } catch (InterruptedException e) {
          logger.error("Ignored exception",e);
        }
      }
      status = Status.READING;
      
      Long oldestTS = timestampList.remove( timestampList.size()-1 );
      timestampListSize.set(timestampList.size() );
      
      try {
        LeaseLogPacket llp = new LeaseLogPacket(dbFillerStatistics,oldestTS,filename);
        dbFiller.add(llp);
        logger.debug("Read lost packet "+llp);
      } catch (IOException e) {
        logger.error("Could not read lost packet "+oldestTS,e);
      } catch (Exception e) {
        logger.error("Could not read lost packet "+oldestTS,e);
      }
      
    }
  }


  /**
   * @return
   */
  private ArrayList<Long> searchLostPackets() {
    long current = Long.valueOf(newestPacket);
    ArrayList<Long> tsl = readTimestampList(); //Lesen der Timestamps aus den Dateinamen
    ArrayList<Long> btsl = readBufferedTimestampList(); //Lesen der Timestamps aus dem DBFiller-RingBuffer
    
    logger.debug( "Found "+tsl.size()+" packets");
    logger.debug( "Found "+btsl.size()+" buffered packets");
    
    Collections.sort(tsl); //sortieren ein Reihenfolge alt...jung
    
    //neuesten Eintrag aus tsl entfernen, (diese ist wahrscheinlich noch nicht in btsl enthalten) 
    for( int p=tsl.size()-1; p>=0; --p ) {
      if( tsl.get(p).longValue() >= current ) {
        tsl.remove(p);
      }
    }
    
    //alle bereits im Buffer btsl enthaltenen Timestamps löschen, damit diese nicht doppelt eingetragen werden
    tsl.removeAll(btsl);
        
    //Liste umdrehen, damit einfaches Entfernen am Ende möglich wird
    Collections.reverse(tsl);
    return tsl;
  }

  /**
   * @return
   */
  private ArrayList<Long> readTimestampList() {
    File file = new File(filename);
    String prefix = file.getName();
    int prefixLength = prefix.length();
    File parent = new File(file.getParent());
    
    ArrayList<Long> tsList = new ArrayList<Long>();
    
    for( String fn : parent.list() ) {
      if( excludedFilenames.contains(fn) ) {
        //Date wird ignoriert
        continue;
      }
      try {
        String num = fn.substring(prefixLength,fn.lastIndexOf('.'));
        tsList.add( Long.valueOf(num) );
      } catch( RuntimeException e ) {
        logger.error( "File "+parent+"/"+fn+" has unexpected name");
      }
    }
    return tsList;
  }
  
  /**
   * @return
   */
  private ArrayList<Long> readBufferedTimestampList() {
    ArrayList<LeaseLogPacket> rbData = new ArrayList<LeaseLogPacket>();
    dbFiller.getRingBufferCopy( rbData );
    ArrayList<Long> tsList = new ArrayList<Long>();
    
    for( LeaseLogPacket llp : rbData ) {
      if( llp != null ) {
        tsList.add( llp.getCreationTime() );
      }
    }
    return tsList;
  }

  
  /**
   * @return
   */
  public String getName() {
    return "LeaseLogReader";
  }

  /**
   * 
   */
  public void terminate() {
    running = false;
  }

  /**
   * @param statusLogger
   */
  public void logStatus(Logger statusLogger) {
    StringBuilder sb = new StringBuilder();
    sb.append("LeaseLogReader is ");
    if( ! running ) { sb.append("not "); }
    sb.append("running in state ").append(status);
    sb.append(" with ").append(timestampList.size()).append(" files to process");
    
    statusLogger.info( sb.toString() );
  }

  /**
   * 
   */
  public void setInsertPossible() {
    if( isSomethingToBeDone() ) {
      wakeUp();     
    }
  }

  /**
   * Aufwecken des schlafenden Threads über ein Signal
   */
  private void wakeUp() {
    if( lockData.tryLock() ) {
      //Lock war erfolgreich
      try {
        conditionNewData.signal();
      } finally {
        lockData.unlock();
      }
    }
  }

  /**
   * Ein weiteres LeaseLogPacket konnte nicht in die DB eingetragen werden 
   */
  public void incrementLostPacketCounter() {
    lostPacketsCounter.incrementAndGet();
  }

  /**
   * @param newestPacket
   */
  public void setNewestPacket(long newestPacket) {
    this.newestPacket = newestPacket;    
  }

  
  
}
