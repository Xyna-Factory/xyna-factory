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

package com.gip.xyna.utils.concurrent;

import java.util.concurrent.locks.ReentrantLock;

import com.gip.xyna.utils.collections.HashCodeMap;


/**
 * HashParallelReentrantLock ist ein Lock, das aus mehreren parallelen ReentrantLocks besteht.
 * Es wird automatisch eines der parallelen Locks verwendet, dieses wird anhand des 
 * �bergebenen Objects ausgesucht. Ziel ist, dass konkurrierende Operationen mit dem Object 
 * verhindert werden, gleichzeitig aber Operationen mit anderen Objects erlaubt sind.
 * <br><br>
 * Damit kann die Wirkungsweise auch so verstandenen werden, dass das �bergebene Object gelockt
 * wird. Damit sind konkurrierende Zugriffe auf das gleiche Objekt ausgeschlossen, Zugriffe auf 
 * andere Objekte aber wahrscheinlich m�glich.
 * <br>
 * <ul>
 * <li>Der Grad der Parallelit�t kann im Konstruktor eingestellt werden, Default ist 32.
 * Je h�her die Parallelit�t ist, desto eher k�nnen verschiedenen Objects parallel bearbeitet 
 * werden, der Speicherverbrauch w�chst allerdings linear an.</li>
 * <li>Zur Ermittelung des Locks wird der hashCode des �bergebenen Objects verwendet. Diese muss
 * hinreichnend gut sein, damit die gelockten Objects unterschieden werden k�nnen und die 
 * Parallelit�t erhalten bleibt.</li>
 * <li>Methoden
 * </ul>
 * <ul>
 * <li>{@link #lock(Object)} und {@link #unlock(Object)}</li>
 * <li>{@link #tryLock(Object)} und {@link #unlock(Object)}</li>
 * <li>{@link #lockAll} und {@link #unlockAll}</li>
 * <li>{@link #toString}</li>
 * </ul>
 * 
 * 
 * Aufruf:
 * <pre>
 * HashParallelReentrantLock hprl = new HashParallelReentrantLock();
 * 
 * Object objectForLock = ...
 * 
 * hprl.lock( objectForLock );
 * try {
 *   ...
 *   
 *   "exklusiver" Zugriff auf objectForLock innerhalb dieses Blocks
 *   
 * } finally {
 *   hprl.unlock( objectForLock );
 * }
 * </pre>
 */
public class HashParallelReentrantLock<T> implements ParallelLock<T> {

  private HashCodeMap<ReentrantLock> locks;
  
  /**
   * Anlegen des HashParallelReentrantLock mit Parallelit�t 32
   */
  public HashParallelReentrantLock() {
    this(32);
  }
  
  /**
   * Anlegen des HashParallelReentrantLock mit angegebener Parallelit�t
   * @param parallel
   */
  public HashParallelReentrantLock(int parallel) {
    locks = new HashCodeMap<ReentrantLock>(parallel, new HashCodeMap.Constructor<ReentrantLock>() {
      public ReentrantLock newInstance() {
        return new ReentrantLock();
      }
    });
    
  }
  
  /**
   * Lock des zum �bergebenen Object passenden Locks (entspricht dem Locken dieses Objects) 
   * @param object
   * @throws NullPointerException wenn object null ist
   */
  public void lock(T object) {
    locks.get(object).lock();
  }
  
  /**
   * Unlock des zum �bergebenen Object passenden Locks (entspricht dem Unlocken dieses Objects) 
   * @param object
   * @throws NullPointerException wenn object null ist
   * @throws IllegalMonitorStateException wenn der aktulle Thread nicht dieses Lock h�lt
   */
  public void unlock(T object) {
    locks.get(object).unlock();
  }
  
  /**
   * TryLock des zum �bergebenen Object passenden Locks (entspricht dem Locken dieses Objects) 
   * @param object
   * @return
   */
  public boolean tryLock(T object) {
    return locks.get(object).tryLock();
  }
  
  
  /**
   * Lockt alle zugrundeliegenden Locks, Freigabe �ber {@link #unlockAll}. 
   * Dies kann l�nger dauern, bis alle Locks von anderen Threads freigegeben wurden
   */
  public void lockAll() {
    boolean[] locked = new boolean[locks.size()]; //mit false korrekt belegt
    int unlocked;
    do {
      unlocked = tryLock(locked);
      if( unlocked >=0 ) {
        //Lock konnte nicht erhalten werden, nun darauf warten
        locks.get(unlocked).lock();
        locked[unlocked] = true;
      }
    } while( unlocked >= 0 );
  }
  
  /**
   * gibt alle mit {@link #lockAll} geholten Locks wieder frei
   */
  public void unlockAll() {
    for( int l=0;l<locks.size(); ++l ) {
      locks.get(l).unlock();
    }
  }
  
  /**
   * 
   */
  private int tryLock(boolean[] locked) {
    int unlocked = -1;
    for( int l=0; l<locks.size(); ++l ) {
      if( locked[l] ) {
        continue; //breits in einem der fr�heren Versuche gelockt
      }
      if( locks.get(l).tryLock() ) {
        locked[l] = true;
      } else {
        unlocked = l;
      }
    }
    return unlocked; //einer der Locks, die nicht gelockt werden konnten
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(200+locks.size()*2);
    sb.append("HashParallelReentrantLock(").append(locks.size()).append(",[");
    String sep ="";
    for( ReentrantLock l : locks.values() ) {
      sb.append(sep);
      if( l.isLocked() ) {
        if( l.isHeldByCurrentThread() ) {
          sb.append(l.getHoldCount());
        } else {
          sb.append("L");
        }
      } else {
        sb.append("u");
      }
      sep =",";
    }
    sb.append("])");
    return sb.toString();
  }

  
}
