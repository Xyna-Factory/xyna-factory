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
package com.gip.xyna.utils.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * AdministrativeParallelLock ist eine {@link ParallelLock}-Implementation, bei der Lock-Objecte auch
 * administrativ gesperrt werden können. 
 * Administrative bedeutet dabei, dass diese Lock-Objecte keine Locks tragen und daher auch von anderen 
 * Threads entfernt werden können. Für die auf eine normales Lock wartenden Threads ist es nicht unterscheidbar,
 * ob das Lock, an dem sie warten, administrative gesperrt ist oder nicht: sie werden auch vom Freigeben
 * des administrativen Locks geweckt.
 * 
 * Dieses Lock ist nicht reentrant!
 */
public class AdministrativeParallelLock<T> implements ParallelLock<T> {

  /**
   * TRUE = administrativ gelockt, FALSE = normal gelockt
   */
  ConcurrentHashMap<T,Boolean> locks = new ConcurrentHashMap<T,Boolean>();
  volatile CountDownLatch latch = new CountDownLatch(1);
  ParallelLock<T> parallelLock;
  
  public AdministrativeParallelLock() {
    this.parallelLock = new HashParallelReentrantLock<T>(32);
  }
  
  
  public List<T> administrativeLock(T ... locks) {
    return administrativeLock(Arrays.asList(locks));
  }
                                    
  public List<T> administrativeLock(Collection<T> list) {
    List<T> alreadyLocked = null;
    for( T t : list ) {
      Boolean b = locks.putIfAbsent(t, Boolean.TRUE );
      if( b == null ) {
        //Lock gesetzt
      } else {
        if( b ) {
          //MultiLock erneut gesetzt
        } else {
          //bereits richtig gelockt
          if( alreadyLocked == null ) {
            alreadyLocked = new ArrayList<T>();
          }
          alreadyLocked.add(t);
        }
      }
    }
    return alreadyLocked != null ? alreadyLocked : Collections.<T>emptyList();
  }
  
  public List<T> administrativeUnlock(T ... locks) {
    return administrativeUnlock(Arrays.asList(locks));
  }
 
  public List<T> administrativeUnlock(Collection<T> list) {
    List<T> otherLocked = null;
    for( T t : list ) {
      boolean removed = locks.remove(t, Boolean.TRUE );
      if( ! removed ) {
        if( locks.get(t) == null ) {
          //MultiLock erneut entfernt
        } else {
          //durch richtigen Lock gelockt
          if( otherLocked == null ) {
            otherLocked = new ArrayList<T>();
          }
          otherLocked.add(t);
        }
      }
    }
    //nun Latch austauschen
    CountDownLatch oldLatch = latch;
    latch = new CountDownLatch(1);
    oldLatch.countDown();
    return otherLocked != null ? otherLocked : Collections.<T>emptyList();
  }
  
  
  public void lock(T object) {
    boolean alreadyLocked = false;
    while( true ) {
      CountDownLatch localLatch = latch;
      Boolean locked = locks.putIfAbsent(object, Boolean.FALSE);
      if( locked == null ) {
        //es gab noch kein Lock
        if( !alreadyLocked ) {
          parallelLock.lock(object);
        }
        return;
      } else {
        if( locked ) {
          //Object gehört MultiLocker, daher am Latch warten
          if( alreadyLocked ) {
            //Lock freigeben, damit anderer eine Chance hat
            parallelLock.unlock(object);
            alreadyLocked = false;
          }
          try {
            // if the localLatch is already stale we'll retry immediately
            localLatch.await(250, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            //weiter warten
          }
        } else {
          //Object gehört anderem Locker, daher am parallelLock warten
          if( alreadyLocked ) {
            //Lock erhalten, aber anderer hat Object in Map gesetzt und hat sein Lock nicht erhalten,
            //weil er zu langsam war. Daher nun nett sein und Lock freigeben, damit der andere 
            //sein Lock erhält.
            parallelLock.unlock(object);
            alreadyLocked = false;
            Thread.yield(); //anderen Thread vorlassen
            continue;
          }
          parallelLock.lock(object);
          alreadyLocked = true;
        }
      }
    }
  }
  
  /**
   * @param waitForHashCollisions falls true, wird bei einer Hash-Kollision (anderes Objekt mit gleichem Hash ist gerade gelockt) auf das gelockte Objekt gewartet.
   *   Das bedeutet, dass tryLock dann nicht sofort zurück kommt. 
   * @return 
   */
  public boolean tryLock(T object, boolean waitForHashCollisions) {
    Boolean locked = locks.putIfAbsent(object, Boolean.FALSE);
    if( locked == null ) {
      //es gab noch kein Lock
      if (waitForHashCollisions) {
        parallelLock.lock(object);
        return true;
      } 
      
      if (parallelLock.tryLock(object)) {
        return true; //Lock erhalten
      } else {
        locks.remove(object);
        return false; //parallelLock nicht erhalten
      }
    }
    return false; //Object nicht in Map eintragbar, gilt also als gelockt
  }
  
  public boolean tryLock(T object) {
    return tryLock(object, false);
  }
    
  public void unlock(T object) {
    parallelLock.unlock(object);
    boolean removed = locks.remove(object, Boolean.FALSE);
    if( ! removed ) {
      throw new IllegalStateException("hold lock but not in map ");
    }
  }
  
  @Override
  public String toString() {
    return "AdministrativeParallelLock("+parallelLock+","+locks+")";
  }


  public Set<T> getAdministrativeLocks() {
    Set<T> adminLocks = new HashSet<T>();
    for( Map.Entry<T,Boolean> entry : locks.entrySet() ) {
      if( entry.getValue() ) {
        adminLocks.add(entry.getKey());
      }
    }
    return adminLocks;
  }
  
}
