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
package com.gip.xyna.utils.logging;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/*
 * pruned copy of com.gip.xyna.utils.concurrent.QueueProcessor from XynaFactory
 */
public class QueueProcessor<T> implements Runnable {

  
  private static class Arr<T> {

    private final AtomicReferenceArray<T> arr;
    private final AtomicInteger idx = new AtomicInteger(0);


    private Arr(int size) {
      arr = new AtomicReferenceArray<T>(size);
    }
  }
  

  public interface ElementProcessor<T> {

    public void process(T o);
  }


  private volatile Arr<T> active;
  private volatile Arr<T> inactive;
  private volatile boolean wait;
  private volatile boolean running = true;
  private volatile boolean shuttingDown = false;
  private final AtomicInteger entry;

  private final int size;
  private final Object waitObject = new Object();
  private final ElementProcessor<T> p;
  private final int parkTime;
  private final SleepCounter sc;


  public QueueProcessor(int size, ElementProcessor<T> p) {
    if (size < 1) {
      throw new IllegalArgumentException();
    }
    this.size = size;
    active = new Arr<T>(size);
    inactive = new Arr<T>(size);
    this.p = p;
    parkTime = Math.min(size, 1000);
    sc = new SleepCounter(parkTime, TimeUnit.MILLISECONDS.toNanos(3000), 0, TimeUnit.NANOSECONDS);
    entry = new AtomicInteger(0);
  }


  public boolean offer(final T o) {
    if (o == null) {
      throw new IllegalArgumentException();
    }
    if (!running) {
      return false;
    }
    entry.incrementAndGet();
    try {
      Arr<T> copy = active;
      int i = copy.idx.getAndIncrement();
      if (i < size) {
        copy.arr.set(i, o);
        return true;
      }
      return false;
    } finally {
      entry.decrementAndGet();
    }
  }


  public void stop() {
    shuttingDown = true;
  }


  private static final Object IGNORE = new Object();


  @SuppressWarnings("unchecked")
  public void put(final T o) {
    if (o == null) {
      throw new IllegalArgumentException();
    }
    if (!running) {
      throw new IllegalStateException("QueueProcessor already shutdown");
    }

    entry.incrementAndGet();
    try {
      int i;
      Arr<T> copy;
      while (true) {
        copy = active;
        i = copy.idx.getAndIncrement();
        if (copy != active && i < size) {
          copy.arr.set(i, (T) IGNORE);
        } else {
          break;
        }
      }
      while (i >= size) {
        while (i >= size) {
          while (i >= size) {
            synchronized (this.waitObject) {
              try {
                if (wait && i >= size) {
                    waitObject.wait();
                }
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            }
            copy = active;
            i = copy.idx.get();
          }
          copy = active;
          i = copy.idx.get();
        }
        while (true) {
          copy = active;
          i = copy.idx.getAndIncrement();
          if (copy != active && i < size) {
            copy.arr.set(i, (T) IGNORE);
          } else {
            break;
          }
        }
      }
      copy.arr.set(i, o);
    } finally {
      entry.decrementAndGet();
    }
  }


  public void run() {
      while (running) {
        Arr<T> copy = active;
        /*
         * komischerweise wird der algorithmus langsamer, wenn man hier folgendes schreibt 
         * int max = copy.idx.get()
         * if (max > 0) {
         *  ...
         * }
         */
        int max = copy.idx.getAndSet(size);
        if (max > 0) {
          active = inactive;
          inactive = copy;
          AtomicReferenceArray<T> internalArray = copy.arr;
          wait = true;
          if (max > size) {
            max = size;
          }
          for (int i = 0; i < max; i++) {
            T t = internalArray.get(i);
            while (t == null) {
              //put ist noch nicht soweit
              t = internalArray.get(i);
            }
            if (t != IGNORE) {
              try {
                p.process(t);
              } catch (RuntimeException e) {
                e.printStackTrace();
              } catch (Error e) {
                e.printStackTrace();
              }
            }
            internalArray.set(i, null); //sonst macht oben das checken, ob es null ist, keinen sinn
          }
          copy.idx.set(0);
          wait = false;
          synchronized (waitObject) {
            waitObject.notifyAll();
          }
          sc.reset();
        } else {
          copy.idx.set(0);
          try {
            sc.sleep();
            if (shuttingDown) {
              if (entry.get() <= 0) {
                running = false;
              } // else: cycle again
            }
          } catch (InterruptedException e) {
            // we should be in the park-case and therefore uninteruptable
          }
      }
    }
  }


}