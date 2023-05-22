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

package com.gip.xyna.coherence.utils.threadpool;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.coherence.CacheControllerImpl;
import com.gip.xyna.coherence.utils.logging.LoggerFactory;



public class ThreadPoolExecutorWithThreadlocalsCleanup extends ThreadPoolExecutor {

  private static final Logger logger = LoggerFactory.getLogger(CacheControllerImpl.class);


  private static Field threadLocalsField;
  private static Field tidField;
  private static Method createTidMethod;
  static {
    try {
      threadLocalsField = Thread.class.getDeclaredField("threadLocals");
      threadLocalsField.setAccessible(true);
      tidField = Thread.class.getDeclaredField("tid");
      tidField.setAccessible(true);
      createTidMethod = Thread.class.getDeclaredMethod("nextThreadID");
      createTidMethod.setAccessible(true);
    } catch (SecurityException e) {
      throw new RuntimeException("Failed to initialize " + ThreadPoolExecutorWithThreadlocalsCleanup.class.getName(), e);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException("Failed to initialize " + ThreadPoolExecutorWithThreadlocalsCleanup.class.getName(), e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Failed to initialize " + ThreadPoolExecutorWithThreadlocalsCleanup.class.getName(), e);
    }
  }

  public ThreadPoolExecutorWithThreadlocalsCleanup(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                                   TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                                   ThreadFactory threadFactory) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
  }


  public ThreadPoolExecutorWithThreadlocalsCleanup(int i, int j, int k, TimeUnit seconds,
                                                   SynchronousQueue<Runnable> synchronousQueue) {
    super(i, j, k, seconds, synchronousQueue);
  }


  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    ((RunnableWithThreadInformation) r).setExecutingThread(t);
  }


  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    Thread currentThread = ((RunnableWithThreadInformation) r).getExecutingThread();
    ((RunnableWithThreadInformation) r).setExecutingThread(null);
    try {
      //workaround daf�r, dass reentrantreadwritelock (und ggfs andere klassen, die threadlocal benutzen) ihren kontext
      //daraus nicht korrekt entfernen und dadurch unkontrolliert der ben�tigte speicherplatz anwachsen kann
      threadLocalsField.set(currentThread, null);
      
      //workaround daf�r, dass in reentrantreadwritelock ein cache f�r objekte die threadlokal gehalten werden benutzt wird.
      //wenn man dies nicht macht, ist der cache != dem threadlokal gehaltenen objekt und es kann illegalmonitorstateexceptions geben.
      //usecase: thread1 macht readlock.lock(), readlock.unlock() => cache ist vorhanden mit holdcount = 0
      //         dann ist thread zuende und threadlocalmap wird geleert
      //         dann macht thread1 readlock.lock => gecachter holdcount = 1
      //         dann kommt thread2 macht readlock.lock => cache wird entfernt und durch einen f�r thread2 ersetzt => information von holdcount = 1 geht verloren.
      //         thread1 readlock.unlock => illegalmonitorstateexception, weil holdcount in threadlocal = 0.
      //mit der neuen threadid wird forciert, dass der cache nicht nach wiederverwendet werden kann, falls der thread aus dem threadpool
      //einmal fertiggelaufen war und beim erneuten execute das gleiche reentrantlock benutzt.
      //das verhalten ist dann genauso wie man es ohne threadpoolbenutzung erwarten w�rde (immer andere threadids)
      long newTid = (Long)createTidMethod.invoke(null);
      tidField.set(currentThread, newTid);
    } catch (IllegalArgumentException e) {
      logger.error("Failed to reset thread local field and id", e);
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      logger.error("Failed to reset thread local field and id", e);
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      logger.error("Failed to reset thread local field and id", e);
      throw new RuntimeException(e);
    }
    
  }
  
}
