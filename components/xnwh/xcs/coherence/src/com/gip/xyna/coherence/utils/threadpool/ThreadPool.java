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
package com.gip.xyna.coherence.utils.threadpool;



import java.util.concurrent.ThreadPoolExecutor;

import com.gip.xyna.coherence.utils.threadpool.ObjectPool.ObjectFactory;



/**
 * alternative implementierung zu {@link ThreadPoolExecutor}.
 * <p>
 * diente hauptsächlich zum vergleich der performance.
 * <p>
 * ergebnis: dieser threadpool ist ca 10-20% schneller, hat aber auch weniger features. <br>
 * TODO prio4: threads auch beenden, wenn sie noch in der ausführung befindlich sind? z.b. ein flag im pool setzen, was
 * dann zurückkehrende objekte auch shutdowned.
 */
public class ThreadPool {

  private ObjectPool<ExecutorThread> pool;


  /**
   * erstellt threadpool mit fester groesse. alle threads werden bereits gestartet.
   */
  public ThreadPool(int size) {
    pool = new ObjectPool<ExecutorThread>(new ObjectFactory<ExecutorThread>() {

      public ExecutorThread create() {
        final ExecutorThread t = new ExecutorThread();
        //nach ausführung des runnables den thread wieder zum pool zurückgeben.
        t.setFinishedListener(new Runnable() {

          public void run() {
            pool.returnToPool(t);
          }

        });
        t.start();
        return t;
      }

    }, size);

  }

  /**
   * beendet alle threads, die sich im pool befinden, und die sich nicht gerade in der ausführung befinden.
   */
  public void shutdown() {
    pool.shutdown();
  }


  public void execute(final RunnableWithThreadInformation r) {
    pool.getFree().execute(r);
  }

}
