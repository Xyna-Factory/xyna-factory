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
package com.gip.xyna.xfmg.xfctrl.threadmgmt.util;

import java.io.OutputStream;
import java.lang.Thread.State;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.AlgorithmState;

public abstract class ManagedPlainThreadFromPausableRunnable extends ManagedPausableRunnable {
  
  private final static long TERMINATION_TIMEOUT = 5000;
  
  private final String name; 
  protected volatile Thread thread;
  
  
  public ManagedPlainThreadFromPausableRunnable(String name, List<StringParameter<?>> parameter) {
    super(parameter);
    this.name = name;
    thread = new Thread(this, name);
  }
  
  public ManagedPlainThreadFromPausableRunnable(String name) {
    this(name, List.of());
  }

  
  public AlgorithmState getStatus() {
    return AlgorithmState.getByJavaState(thread);
  }

  
  @Override
  public boolean start(Map<String, Object> parameter, OutputStream statusOutputStream) {
    if (super.start(parameter, statusOutputStream) && 
        AlgorithmState.getByJavaState(thread) == AlgorithmState.NOT_RUNNING) {
      if (thread.getState() == State.TERMINATED) {
        thread = new Thread(this, name);
      }
      thread.start();
      return true;
    } else {
      super.stop(); // just in case super.start() succeeded
      return false;
    }
  }
  
  
  public String getName() {
    return name;
  }
  
  
  protected boolean waitForTermination() {
    if (thread.isAlive()) {
      try {
        thread.join(TERMINATION_TIMEOUT);
        Thread.yield();
        return !thread.isAlive();
      } catch (InterruptedException e) {
        return false;
      }
    } else {
      return true;      
    }
  }
    protected ErrorHandling handle(Throwable t) {
    return ErrorHandling.ABORT;
  }
  
  
}
