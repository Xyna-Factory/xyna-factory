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
package com.gip.xyna.demon.worker;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockingSlavePool<Tool,Work> extends SlavePool<Tool,Work> {

  public BlockingSlavePool( SlaveInitializer<Tool> si, int corePoolSize, int maxPoolSize ) {
    super(si,corePoolSize,maxPoolSize);
  }
  private AtomicInteger counterBlocked = new AtomicInteger();
   
  @Override
  public void execute( SlaveWork<Tool,Work> sw ) {
    pcRequested.increment();
    RunnableSlaveWork<Tool,Work> rsw = new RunnableSlaveWork<Tool,Work>( this, sw );
    try {
      threadPoolExecutor.execute( rsw );
    } catch( RejectedExecutionException e ) {
      retryExecution( rsw );
    }
  }


  private void retryExecution(RunnableSlaveWork<Tool,Work> rsw) {
    boolean executed = false;
    do {
      try {
        counterBlocked.incrementAndGet();
        //System.err.println( "blocked "+counterBlocked.getInt() );
        Thread.sleep(5);
      } catch (InterruptedException e1) {
        //Ignorieren
      }
      try {
        threadPoolExecutor.execute( rsw );
        executed = true;
      } catch( RejectedExecutionException e ) {
        executed = false;
      }
    } while( ! executed );
    
  }
  
}
