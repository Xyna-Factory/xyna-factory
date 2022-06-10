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
package com.gip.xyna.coherence.utils.locking;



import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.gip.xyna.coherence.CacheController;
import com.gip.xyna.coherence.coherencemachine.CoherencePayload;
import com.gip.xyna.coherence.exceptions.ObjectNotInCacheException;



/**
 * auf mehrere knoten gibt es instanzen dieser klasse, die einen eigenen lokalen kontext haben und einen gemeinsamen
 * über die beiden im konstruktor anzugebenden objectIds.<br>
 * ACHTUNG: man kann nicht einfach diese klasse serialisieren und an einen anderen knoten schicken. die idee ist gerade,
 * dass es einen lokalen kontext gibt.
 */
/*
 * TODO: andere implementierung, die für readlocks keinen netzwerkzugriff benötigt
 *  - zb, indem man beim holen des writelocks die anzahl der readlocks übers netzwerk holt und sie ansosnten nur lokal hält.
 */
public class ClusteredReadWriteLock implements ReadWriteLock {

  private class ReadLock implements Lock {

    public void lock() {
      try {
        lock(false, 0);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }


    private boolean lock(boolean tryLock, long nanos) throws InterruptedException {
      Thread t = Thread.currentThread();
      Integer i = localSharedLocksPerThread.get(t);
      if (i == null) {
        //TODO exclusively locked by this thread? => lock ok.

        try {
          if (tryLock) {
            if (!cc.tryLock(exclusiveLockObject, nanos)) {
              return false;
            }
          } else {
            cc.lock(exclusiveLockObject);
          }
          incrementSharedLockCounter();
          cc.unlock(exclusiveLockObject);
        } catch (ObjectNotInCacheException e) {
          throw new RuntimeException(e);
        }
        localSharedLocksPerThread.put(t, 1);
      } else {
        localSharedLocksPerThread.put(t, i + 1);
      }
      return true;
    }


    public void lockInterruptibly() throws InterruptedException {
      lock();
    }


    public Condition newCondition() {
      throw new RuntimeException("unsupported");
    }


    public boolean tryLock() {
      try {
        return lock(true, 0);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }


    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      return lock(true, unit.toNanos(time));
    }


    public void unlock() {
      Thread t = Thread.currentThread();
      Integer i = localSharedLocksPerThread.get(t);
      if (i == null) {
        throw new IllegalMonitorStateException("tried to unlock but did not own readlock.");
      }
      i--;
      if (i == 0) {
        decrementSharedLockCounter();
        localSharedLocksPerThread.remove(t);
      } else {
        localSharedLocksPerThread.put(t, i);
      }
    }

  }

  private class WriteLock implements Lock {

    public void lock() {
      try {
        lock(false, 0);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }


    private boolean lock(boolean tryLock, long nanos) throws InterruptedException {
      try {
        if (tryLock) {
          long t0 = System.nanoTime();
          if (!cc.tryLock(exclusiveLockObject, nanos)) {
            return false;
          } else {
            nanos -= System.nanoTime() - t0;
          }
        } else {
          cc.lock(exclusiveLockObject);
        }
        boolean noSharedLocksLeft;
        try {
          noSharedLocksLeft = waitForSharedLocksToVanish(tryLock, nanos);
        } catch (InterruptedException e) {
          cc.unlock(exclusiveLockObject);
          throw e;
        }
        if (!noSharedLocksLeft) {
          cc.unlock(exclusiveLockObject);
          return false;
        }

        //keine sharedlocks mehr unterwegs. neue kann es auch nicht geben, weil dieser thread das exclusive lock hat.
        return true;
      } catch (ObjectNotInCacheException e) {
        throw new RuntimeException(e);
      }
    }


    public void lockInterruptibly() throws InterruptedException {
      lock();
    }


    public Condition newCondition() {
      throw new RuntimeException("unsupported");
    }


    public boolean tryLock() {
      try {
        return lock(true, 0);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }


    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      return lock(true, unit.toNanos(time));
    }


    public void unlock() {
      try {
        cc.unlock(exclusiveLockObject);
      } catch (ObjectNotInCacheException e) {
        throw new RuntimeException(e);
      }
    }

  }

  private static class NumberOfNodesWithSharedLocks extends CoherencePayload {


    private static final long serialVersionUID = 1;

    private int cnt;

    public NumberOfNodesWithSharedLocks() {
    }


    public void increment() {
      cnt++;
    }


    public void decrement() {
      cnt--;
    }


    public int get() {
      return cnt;
    }


  }

  private static long[] sleepTimes;
  static {
    //5 mal 1 ms warten,
    //10 mal 2 ms warten.
    //15 mal 4 ms warten.
    //20 mal 8 ms warten.
    //16 ms warten.
    sleepTimes = new long[51];
    for (int i = 0; i<5; i++) {
      sleepTimes[i] = 1;
    }
    for (int i = 5; i<15; i++) {
      sleepTimes[i] = 2;
    }
    for (int i = 15; i<30; i++) {
      sleepTimes[i] = 4;
    }
    for (int i = 30; i<50; i++) {
      sleepTimes[i] = 8;
    }
    sleepTimes[50] = 16;
  }
  
  private final CacheController cc;
  private final long exclusiveLockObject;
  private final long numberOfNodesWithSharedLocks;
  private final Map<Thread, Integer> localSharedLocksPerThread;
  private final AtomicInteger localSharedLockCount;
  private final ReadLock readLock;
  private final WriteLock writeLock;


  public ClusteredReadWriteLock(CacheController cc) {
    this(cc, cc.create(new CoherencePayload()), cc.create(new NumberOfNodesWithSharedLocks()));
  }


  public ClusteredReadWriteLock(CacheController cc, long exclusiveLockObject, long numberOfNodesWithSharedLocksObject){
    this.cc = cc;
    this.exclusiveLockObject = exclusiveLockObject;
    this.localSharedLocksPerThread = new ConcurrentHashMap<Thread, Integer>();
    this.numberOfNodesWithSharedLocks = numberOfNodesWithSharedLocksObject;
    readLock = new ReadLock();
    writeLock = new WriteLock();
    localSharedLockCount = new AtomicInteger(0);
  }


  public long getIdForExclusiveLockObject() {
    return exclusiveLockObject;
  }


  public long getIdForNumberOfNodesWithSharedLocksObject() {
    return numberOfNodesWithSharedLocks;
  }

  private boolean waitForSharedLocksToVanish(boolean tryLock, long nanos) throws InterruptedException {
    //TODO eigene sharedlocks abziehen, für den fall, dass lock upgrade funktionieren soll.
    if (tryLock) {
    //gibt es noch lokale locks?
      long endTime = System.nanoTime() + nanos;
      while (localSharedLockCount.get() > 0) {
        synchronized (readLock) {
          if (localSharedLockCount.get() > 0) {
            if (!LockingUtils.wait(readLock, endTime)) {
              return false;
            }
          }
        }
      }

    //gibt es noch remote locks?
      try {
        while (true) {
          NumberOfNodesWithSharedLocks n = (NumberOfNodesWithSharedLocks) cc.read(numberOfNodesWithSharedLocks);
          if (n.get() == 0) {
            break;
          } else {
            nanos = endTime - System.nanoTime();
            if (nanos <= 100) {
              return false;
            }
            if (nanos > 1000000) {
              Thread.sleep(1);
            }
          }
        }
      } catch (ObjectNotInCacheException e) {
        throw new RuntimeException(e);
      }
    } else {
      //gibt es noch lokale locks?
     
      while (localSharedLockCount.get() > 0) {
        synchronized (readLock) {
          if (localSharedLockCount.get() > 0) {
            readLock.wait();
          }
        }
      }

      //gibt es noch remote locks?
      try {
        int sleepTimeIndex = 0;
        while (true) {
          NumberOfNodesWithSharedLocks n = (NumberOfNodesWithSharedLocks) cc.read(numberOfNodesWithSharedLocks);
          if (n.get() == 0) {
            break;
          } else {
            //hier würde man sich gerne benachrichtigen lassen, wenn das objekt geupdated wird.
            //das problem ist, dass wenn das coherenceobjekt remote geupdated wird, ist es modified, und hier (lokal) invalid.
            //in dem fall wird die payload genullt und folglich wird die benachrichtigung (onChange) nicht in dem lokalen
            //objekt ankommen.
            
            //inkrementell immer länger werdend warten, ob das lock inzwischen freigeworden ist.
            Thread.sleep(sleepTimes[sleepTimeIndex++]);
            if (sleepTimeIndex > sleepTimes.length-1) {
              sleepTimeIndex--;
            }
          }
        }
      } catch (ObjectNotInCacheException e) {
        throw new RuntimeException(e);
      }

    }
    return true;
  }


  private void incrementSharedLockCounter() {
    int oldValue = localSharedLockCount.getAndIncrement();
    if (oldValue == 0) {
      try {
        cc.lock(numberOfNodesWithSharedLocks);
        NumberOfNodesWithSharedLocks n = (NumberOfNodesWithSharedLocks) cc.read(numberOfNodesWithSharedLocks);
        n.increment();
        cc.update(numberOfNodesWithSharedLocks, n);
        cc.unlock(numberOfNodesWithSharedLocks);
      } catch (ObjectNotInCacheException e) {
        throw new RuntimeException(e);
      }
    }
  }


  private void decrementSharedLockCounter() {
    int newValue = localSharedLockCount.decrementAndGet();
    if (newValue == 0) {
      synchronized (readLock) {
        readLock.notify(); //kann nur ein thread am warten sein.
      }
      try {
        cc.lock(numberOfNodesWithSharedLocks);
        NumberOfNodesWithSharedLocks n = (NumberOfNodesWithSharedLocks) cc.read(numberOfNodesWithSharedLocks);
        n.decrement();
        cc.update(numberOfNodesWithSharedLocks, n);
        cc.unlock(numberOfNodesWithSharedLocks);
      } catch (ObjectNotInCacheException e) {
        throw new RuntimeException(e);
      }
    }
  }


  public Lock readLock() {
    return readLock;
  }


  public Lock writeLock() {
    return writeLock;
  }


  public void removeFromCluster() {
    try {
      cc.delete(exclusiveLockObject);
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }
    try {
      cc.delete(numberOfNodesWithSharedLocks);
    } catch (ObjectNotInCacheException e) {
      throw new RuntimeException(e);
    }
  }
}
