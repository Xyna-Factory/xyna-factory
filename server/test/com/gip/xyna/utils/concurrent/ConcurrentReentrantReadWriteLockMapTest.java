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



import java.util.function.Consumer;

import org.junit.Assert;

import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;

import junit.framework.TestCase;



public class ConcurrentReentrantReadWriteLockMapTest extends TestCase {

  public void testKeepRemoveLockSingleThread() {
    ConcurrentReentrantReadWriteLockMapForTestExtension map = new ConcurrentReentrantReadWriteLockMapForTestExtension();
    Object obj = new Object();
    ConcurrentMapWithObjectRemovalSupport<Object, ReentrantLockWrapper> lockMap = map.getLocks();

    map.writeLock(obj);
    Assert.assertTrue("forgot about lock immediately- should keep track", lockMap.containsKey(obj));
    map.writeUnlock(obj);
    Assert.assertTrue("lock remains in map - should have been deleted", !lockMap.containsKey(obj));

    map.readLock(obj);
    Assert.assertTrue("forgot about lock - should keep track. It is still in use", lockMap.containsKey(obj));
    map.readUnlock(obj);
    Assert.assertTrue("lock remains in map - should have been deleted", !lockMap.containsKey(obj));
  }


  public void testKeepRemoveLockMultipleThreads() {
    FunctionBasedThread t1 = new FunctionBasedThread();
    FunctionBasedThread t2 = new FunctionBasedThread();

    ConcurrentReentrantReadWriteLockMapForTestExtension map = new ConcurrentReentrantReadWriteLockMapForTestExtension();
    Object obj = new Object();
    ConcurrentMapWithObjectRemovalSupport<Object, ReentrantLockWrapper> lockMap = map.getLocks();

    t1.start();
    t2.start();

    //t1 locks with readLock
    performActionInThread(t1, (v) -> map.readLock(obj));
    //t2 locks with readLock
    performActionInThread(t2, (v) -> map.readLock(obj));
    //t1 unlocks with readLock
    performActionInThread(t1, (v) -> map.readUnlock(obj));
    //check if lock is still in map
    Assert.assertTrue("lost track of lock, despite t2 still holding it", lockMap.containsKey(obj));
    //t2 unlocks with readLock
    performActionInThread(t2, (v) -> map.readUnlock(obj));
    //check if lock is no longer in map
    Assert.assertTrue("lock remained in map, despite no one holding it", !lockMap.containsKey(obj));

    t1.setStop(true);
    t2.setStop(true);

    try {
      t1.join(500l);
      t2.join(500l);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  public void testWaitForWriteLock() {
    FunctionBasedThread t1 = new FunctionBasedThread();
    FunctionBasedThread t2 = new FunctionBasedThread();

    ConcurrentReentrantReadWriteLockMapForTestExtension map = new ConcurrentReentrantReadWriteLockMapForTestExtension();
    Object obj = new Object();
    ConcurrentMapWithObjectRemovalSupport<Object, ReentrantLockWrapper> lockMap = map.getLocks();

    t1.start();
    t2.start();

    //t1 locks with writeLock
    performActionInThread(t1, (v) -> map.writeLock(obj));
    //t2 waits for writeLock
    startActionInThread(t2, (v) -> map.writeLock(obj));
    //t1 unlocks with writeLock
    performActionInThread(t1, (v) -> map.writeUnlock(obj));
    //check if lock is still in map
    Assert.assertTrue("forgot about lock, despite t2 trying to acquire it.", lockMap.containsKey(obj));
    //t2 should acquire lock now 
    waitForThreadToPerformAction(t2);
    //check if lock is still in map
    Assert.assertTrue("forgot about lock, despite t2 acquired it.", lockMap.containsKey(obj));
    //t2 unlocks with writeLock
    performActionInThread(t2, (v) -> map.writeUnlock(obj));
    //check if lock is no longer in map
    Assert.assertTrue("lock remained in map, despite no one holding it", !lockMap.containsKey(obj));


    t1.setStop(true);
    t2.setStop(true);

    try {
      t1.join(500l);
      t2.join(500l);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

  }


  //might not get it, if action returns immediately!
  //use for blocking actions
  private void startActionInThread(FunctionBasedThread thread, Consumer<Void> action) {
    thread.setAction(action);
    waitForThreadToStartAction(thread);
  }


  private void performActionInThread(FunctionBasedThread thread, Consumer<Void> action) {
    thread.setAction(action);
    waitForThreadToPerformAction(thread);
  }


  private void waitForThreadToStartAction(FunctionBasedThread thread) {
    int i = 0;
    while (i++ < 100) {
      if (thread.isPerformingAction()) {
        return;
      }
      try {
        Thread.sleep(100l);
        Thread.yield();
      } catch (InterruptedException e) {
        throw new RuntimeException();
      }
    }

    throw new RuntimeException("Thread did not start performing action!");
  }


  private void waitForThreadToPerformAction(FunctionBasedThread thread) {
    int i = 0;
    while (i++ < 100) {
      if (thread.performedAction()) {
        return;
      }
      try {
        Thread.sleep(100l);
        Thread.yield();
      } catch (InterruptedException e) {
        throw new RuntimeException();
      }
    }

    throw new RuntimeException("Thread did not perform action!");
  }


  private static class FunctionBasedThread extends Thread {

    private Consumer<Void> action;
    private boolean stop;
    private Object obj = new Object();
    private boolean performingAction;


    public void setAction(Consumer<Void> action) {
      synchronized (obj) {
        this.action = action;
      }
    }


    public boolean isPerformingAction() {
      return performingAction;
    }


    public boolean performedAction() {
      synchronized (obj) {
        return action == null;
      }
    }


    public void setStop(boolean stop) {
      this.stop = stop;
    }


    @Override
    public void run() {
      try {
        while (!stop) {
          synchronized (obj) {
            if (action != null) {
              performingAction = true;
              action.accept(null);
              performingAction = false;
              sleep(50l);
              action = null;
            }
          }
        }
      } catch (InterruptedException e) {
        return;
      }
    }

  }

}
