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
package com.gip.xyna.xsor.protocol;



import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import com.gip.xyna.xsor.common.XSORUtil;



/**
 * verwendet in xcmemory um das lock sichtbar zu machen, ohne die queue in das java.util package packen zu m�ssen
 */
public class LinkedBlockingDequeWithLockAccess extends LinkedBlockingDeque<byte[]> {

  private static final byte[] SPECIAL = new byte[0];
  private static final long serialVersionUID = 1L;
  
  private Lock l;
  private LinkedBlockingDequeWithLockAccess lastSent;
  private final Set<byte[]> objectIds = new TreeSet<byte[]>(new Comparator<byte[]>() {

    public int compare(byte[] o1, byte[] o2) {
      int lengthDiff = o1.length - o2.length;
      if (lengthDiff == 0) {
        for (int i = 0; i < o1.length; i++) {
          int diff = o1[i] - o2[i];
          if (diff != 0) {
            return diff;
          }
        }
        return 0;
      } else {
        return lengthDiff;
      }
    }

  });


  public LinkedBlockingDequeWithLockAccess(int length) {
    super(length);
  }


  public LinkedBlockingDequeWithLockAccess() {
  }


  public Lock getLock() {
    if (l == null) {
      Field f;
      try {
        f = LinkedBlockingDeque.class.getDeclaredField("lock");
      } catch (SecurityException e) {
        throw new RuntimeException(e);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      f.setAccessible(true);
      try {
        l = (Lock) f.get(this);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    return l;
  }


  /**
   * falls lastSent voll ist, wird nichts gepollt
   */
  public byte[] pollAndMoveToLastSent(LinkedBlockingDequeWithLockAccess lastSent) {
    Lock lock = getLock();
    lock.lock();
    boolean locked = true;
    try {
      this.lastSent = lastSent;
      byte[] nextItem = poll();
      if (nextItem == null) {
        return null;
      }
      if (!lastSent.offer(nextItem)) { //gelockt, deshalb nicht blockierend
        addFirst(nextItem);
        lock.unlock();
        locked = false;
        try {
          if (!lastSent.offer(nextItem, 100, TimeUnit.MILLISECONDS)) { //ungelockt mit der gefahr, dass nextItem sp�ter nicht mehr das erste item in der queue ist
            return null;
          }
          //hat doch noch funktioniert.
          lock.lock();
          locked = true;
          byte[] currentNextItem = poll();
          if (currentNextItem != nextItem) {
            //inzwischen ist ein neues item in die queue eingef�gt worden. r�ckg�ngig machen und nochmal probieren...
            if (currentNextItem != null) {
              //wenn es nicht mehr da ist, macht das nichts
              addFirst(currentNextItem);
            }
            lastSent.removeLastOccurrence(nextItem);
            return null;
          }
        } catch (InterruptedException e) {
          return null;
        }
      }
      return nextItem;
    } finally {
      if (locked) {
        lock.unlock();
      }
    }
  }


  public void addFirstAndRemoveFromLastSent(byte[] nextItem, LinkedBlockingDequeWithLockAccess lastSent) {
    Lock lock = getLock();
    lock.lock();
    try {
      this.lastSent = lastSent;
      addFirst(nextItem);
      lastSent.remove(nextItem);
    } finally {
      lock.unlock();
    }
  }


  public boolean containsObjectId(byte[] objectID) {
    Lock lock = getLock();
    lock.lock();
    try {
      return objectIds.contains(objectID) || (lastSent != null && lastSent.containsObjectId(objectID));
    } finally {
      lock.unlock();
    }
  }


  //verwendete methoden, die objekte �ndern FIXME andere methoden verbieten oder implementieren


  @Override
  public void clear() {
    Lock lock = getLock();
    lock.lock();
    try {
      objectIds.clear();
      super.clear();
    } finally {
      lock.unlock();
    }
  }


  @Override
  public boolean remove(Object o) {
    Lock lock = getLock();
    lock.lock();
    try {
      byte[] b = (byte[]) o;
      objectIds.remove(getObjectId(b));
      return super.remove(b);
    } finally {
      lock.unlock();
    }
  }


  private byte[] getObjectId(byte[] payload) {
    if (payload.length < 41) {
      return SPECIAL;
    }
    int objectIdLength = XSORUtil.getInt(37, payload);
    byte[] objectIDAsBytes = Arrays.copyOfRange(payload, 41, 41 + objectIdLength);
    return objectIDAsBytes;
  }


  @Override
  public void addFirst(byte[] e) {
    Lock lock = getLock();
    lock.lock();
    try {
      objectIds.add(getObjectId(e));
      super.addFirst(e);
    } finally {
      lock.unlock();
    }
  }


  @Override
  public boolean offer(byte[] e) {
    Lock lock = getLock();
    lock.lock();
    try {
      if (super.offer(e)) {
        objectIds.add(getObjectId(e));
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }


  @Override
  public byte[] poll() {
    Lock lock = getLock();
    lock.lock();
    try {
      byte[] ret = super.poll();
      if (ret != null) {
        objectIds.remove(getObjectId(ret));
      }
      return ret;
    } finally {
      lock.unlock();
    }
  }


  @Override
  public boolean removeLastOccurrence(Object o) {
    Lock lock = getLock();
    lock.lock();
    try {
      byte[] b = (byte[]) o;
      objectIds.remove(getObjectId(b));
      return super.removeLastOccurrence(b);
    } finally {
      lock.unlock();
    }
  }


  @Override
  public boolean offer(byte[] e, long timeout, TimeUnit unit) throws InterruptedException {
    byte[] id = getObjectId(e);
    Lock lock = getLock();
    lock.lock();
    try {
      objectIds.add(id); //evtl noch gar nicht in der liste drin, macht aber nichts
    } finally {
      lock.unlock();
    }
    if (!super.offer(e, timeout, unit)) {
      lock.lock();
      try {
        objectIds.remove(id);
      } finally {
        lock.unlock();
      }
      return false;
    }
    return true;
  }


}
