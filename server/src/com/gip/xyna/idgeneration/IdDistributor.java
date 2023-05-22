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
package com.gip.xyna.idgeneration;



import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.timing.SleepCounter;



/**
 * vergabe von ids, die in bl�cken mit konstanter gr��e angeordnet sind. 
 * 
 * bl�cke sind immer an die blockgr��e angepasst, d.h. bl�cke enden immer mit einer zahl, die modulo 
 * blockgr��e == -1 ist.
 * 
 * falls ein block abgearbeitet wurde, ist die im konstruktor �bergeben idsource daf�r verantwortlich,
 * den startpunkt des n�chsten blocks zu liefern.
 * 
 * klasse ist threadsafe, d.h. es ist keine weitere synchronisierung notwendig. 
 * 
 * auf die idsource greift immer nur genau ein thread gleichzeitig zu.
 */
public class IdDistributor {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(IdDistributor.class);


  public interface IDSource {

    /**
     * darf fehler werfen oder lange dauern
     * 
     * @param time die startzeit in millis seit 1970, seitdem der thread auf eine id wartet 
     */
    public long getNextBlockStart(IdDistributor iddistr, long time);
  }

  private static class Block {

    public final AtomicLong nextId;
    private final long blockEnd; //letzte g�ltige id des blocks


    public Block(long nextId, long blockEnd) {
      this.nextId = new AtomicLong(nextId);
      this.blockEnd = blockEnd;
    }
  }


  protected final long blockSize;
  private final IDSource source;
  private final AtomicReference<Block> block;
  private boolean initializedFromPrefetcher = true; // changed for tests


  public IdDistributor(IDSource source, long blocksize) {
    this.blockSize = blocksize;
    this.source = source;
    block = new AtomicReference<>(new Block(1, 0)); //der erste block ist immer bereits beendet: es muss also lazy erstmal die IDSource aufgerufen werden, sobald die erste id gezogen werden soll
  }


  /**
   * setzt die aktuelle id auf den gew�nschten wert und gibt die vorherige zur�ck
   */
  public long getAndSet(long newId, long blockEnd) {
    Block oldBlock = block.getAndSet(new Block(newId, blockEnd));
    return oldBlock.nextId.getAndSet(Integer.MIN_VALUE);
    //TODO das ist etwas unsch�n, aber irgendwie sollte verhindert werden, dass noch g�ltige ids gezogen werden,ein thread
    //noch den alten block hat
  }


  /**
   * �ndert nichts an der aktuellen id, gibt eins weniger als die n�chste id aus.
   */
  public long getCurrent() {
    return block.get().nextId.get() - 1; //get() ist die n�chste id. die n�chste kann au�erhalb des blocks liegen - die aktuelle ist eine zur�ck. wenn gerade ein blockwechsel war, ist es trotzdem nicht per se falsch
  }


  public long getCurrentBlockEnd() {
    return block.get().blockEnd;
  }

  

  /**
   * erh�ht die id um eins und gibt die vorherige id zur�ck. falls das blockende erreicht ist, wird der n�chste block begonnen 
   */
  public long getNext() {
    /*
     * Anforderungen bzgl Synchronizit�t
     * - Bei Blockende nicht weiterlesen
     * - Bei Blockende darauf warten, dass ein Thread die prefetchedid holt
     * - Achtung: Thread kann Fehler beim Prefetch haben -> Anderer Thread muss PrefetchedId holen
     */
    Block currentBlock = block.get();
    long blockEndLocal = currentBlock.blockEnd;
    long result = currentBlock.nextId.getAndIncrement();
    if (result > blockEndLocal) {
      long startTime = System.currentTimeMillis(); //f�r ordentliche berechnung des timeouts
      SleepCounter sleepCnt = null;
      while (result > blockEndLocal) {
        //der thread, der die id holt, die 1 zu hoch ist, ist der zust�ndige
        if (result == blockEndLocal + 1 && 
            block.get() == currentBlock) {
          //dieser thread soll die n�chste id vom prefetcher holen. falls er einen timeout hat, 
          //wird blockEnd hochgez�hlt, damit der n�chste thread seine aufgabe �bernimmt.
          boolean success = false;
          try {
            long next = source.getNextBlockStart(this, startTime);
            success = true;
            if (!initializedFromPrefetcher || blockEndLocal > 0) {
              blockEndLocal = next - (next % blockSize) + blockSize - 1; //next muss kein blockanfang sein
              block.set(new Block(next + 1, blockEndLocal));
            }
            result = next;
          } finally {
            if (!success) {
              /*
               * n�chster thread soll die rolle �bernehmen. blockEnd wird hochgesetzt, und nextId wird wieder auf blockEnd 
               * gesetzt, damit es genau wieder einen thread gibt, der eine id hat, die eins zu hoch ist
               */

              //blockEnd=999 result=1000 nextId=1003 blockEndLocal=999
              block.set(new Block(blockEndLocal + 2, blockEndLocal + 1));
              //blockEnd=1000 nextId=1001
              /*
               * jeder wartende thread muss durch den block, dass current != block.get(), d.h. die wartenden 
               * threads fangen an mit nextId=1001
               */
            }
          }
        } else if (currentBlock != block.get()) {
          //ok, es hat sich was getan, neu versuchen
          currentBlock = block.get();
          blockEndLocal = currentBlock.blockEnd;
          result = currentBlock.nextId.getAndIncrement();
        } else {
          //warten, dass sich etwas tut
          if (sleepCnt == null) {
            sleepCnt = new SleepCounter(1, 5000, 10, TimeUnit.MILLISECONDS, true);
          }
          try {
            sleepCnt.sleep();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
          //dann wieder am anfang der whileschleife erneut probieren
        }
      }
    }
    return result;
  }


}
