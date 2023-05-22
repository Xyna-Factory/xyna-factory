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



import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.timing.SleepCounter;


/**
 * schnelle singlethreaded verarbeitung von objekten, die �ber eine art queue doppelt gebuffered werden.
 * 
 */
//TODO mit lazy algorithm executor verheiraten?
//TODO runnable pausieren - evtl mit dem oberen todo erledigt?
public class QueueProcessor<T> implements Runnable {

  /*
   * arrayblockingqueue als singlebuffer ist viel unperformanter.
   */
  
  private static final Object IGNORE = new Object();
  private static final boolean debug = true;
  private static final Logger logger = CentralFactoryLogging.getLogger(QueueProcessor.class);
  final AtomicInteger cntL1 = new AtomicInteger(0);
  final AtomicInteger cntL2 = new AtomicInteger(0);
  final AtomicInteger cntL3 = new AtomicInteger(0);
  final AtomicInteger cntA = new AtomicInteger(0);
  final AtomicInteger cntB = new AtomicInteger(0);
  final SleepCounter sleep;


  private static class Container<T> {

    private volatile T t;
  }

  private static class Arr<T> {

    private final Container<T>[] arr;
    private final AtomicInteger idx = new AtomicInteger(0);


    @SuppressWarnings("unchecked")
    private Arr(int size) {
      arr = (Container<T>[]) new Container[size];
      for (int i = 0; i < size; i++) {
        arr[i] = new Container<T>();
      }
    }
  }

  public interface ElementProcessor<T> {

    public void process(T o);
  }


  private volatile Arr<T> active;
  private volatile Arr<T> inactive;
  private volatile boolean currentlyProcessing = false;
  private volatile boolean running = true;
  private volatile boolean shuttingDown = false;
  private final AtomicInteger entry;

  private final int size;
  private final Object waitObject = new Object();
  private final ElementProcessor<T> p;


  public QueueProcessor(int size, ElementProcessor<T> p) {
    this(size, p, new SleepCounter(Math.min(size, 1000), TimeUnit.MICROSECONDS.toNanos(100), 0, TimeUnit.NANOSECONDS, true));
  }
  
  
  public QueueProcessor(int size, ElementProcessor<T> p, SleepCounter counter) {
    if (size < 1) {
      throw new IllegalArgumentException();
    }
    this.size = size;
    active = new Arr<T>(size);
    inactive = new Arr<T>(size);
    this.p = p;
    sleep = counter;
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
      if (i >= size) {
        return false;
      }
      if (copy != active) {
        ((Container[]) copy.arr)[i].t = IGNORE;
        return false;
      }
      copy.arr[i].t = o;
      return true;
    } finally {
      entry.decrementAndGet();
    }
  }


  public void stop() {
    shuttingDown = true;
  }


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
          ((Container[]) copy.arr)[i].t = IGNORE;
          if (debug)
            cntB.incrementAndGet();
        } else {
          break;
        }
      }
      while (i >= size) {
        if (debug)
          cntL1.incrementAndGet();
        while (i >= size) {
          if (debug)
            cntL2.incrementAndGet();
          while (i >= size) {
            if (debug)
              cntL3.incrementAndGet();
            synchronized (waitObject) {
              try {
                if (currentlyProcessing && i >= size) {
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
            ((Container[]) copy.arr)[i].t = IGNORE;
            if (debug)
              cntB.incrementAndGet();
          } else {
            break;
          }
        }
      }
      copy.arr[i].t = o;
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
        Container<T>[] internalArray = copy.arr;
        currentlyProcessing = true;
        if (max > size) {
          max = size;
        }
        for (int i = 0; i < max; i++) {
          T t;
          while ((t = internalArray[i].t) == null) {
            //    LockSupport.parkNanos(1);
            //put ist noch nicht soweit
          }

          if (t != IGNORE) {
            try {
              p.process(t);
            } catch (RuntimeException e) {
              logger.debug("Error while proccesing queue element", e);
            } catch (Error e) {
              logger.debug("Error while proccesing queue element", e);
            }
          }
          internalArray[i].t = null; //sonst macht oben das checken, ob es null ist, keinen sinn
        }
        copy.idx.set(0);
        currentlyProcessing = false;
        synchronized (waitObject) {
          waitObject.notifyAll();
        }        
        sleep.reset();
      } else {
        copy.idx.set(0); //oben auf size gesetzt. nun wieder zur�ck auf 0 setzen
        try {
          sleep.sleep();
          if (shuttingDown) {
            if (entry.get() <= 0) {
              running = false;
            } // else: cycle again
          }
        } catch (InterruptedException e) {
          // we should be in the park-case and therefore uninteruptable
        }
        if (debug) {
          cntA.incrementAndGet();
        }
      }
    }
  }


}
