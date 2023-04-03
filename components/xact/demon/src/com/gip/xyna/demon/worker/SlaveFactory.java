/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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

import java.util.concurrent.atomic.AtomicInteger;


public class SlaveFactory<Tool> implements java.util.concurrent.ThreadFactory {

  public static class SlaveThread<Tool> extends Thread {
    
    private Runnable runnable;
    private Tool tool;
    private final SlaveInitializer<Tool> slaveInitializer;
    private final int number;
    
    public SlaveThread(String name, SlaveInitializer<Tool> slaveInitializer, int number, Runnable runnable ) {
      super(name);
      this.slaveInitializer = slaveInitializer;
      this.number = number;
      this.runnable = runnable;
    }
    
   

    public String getAdditional() {
      return getName();
    }
    
    @Override
    public void run() {
      tool = slaveInitializer.create(number);
      if( runnable != null ) {
        runnable.run();
      }
      slaveInitializer.destroy(tool,number);
    }

    public Tool getTool() {
      return tool;
    }

    
  }

  private SlaveInitializer<Tool> slaveInitializer;
  private final AtomicInteger slaveNumber = new AtomicInteger(1);
  private String namePrefix;
  
  public SlaveFactory(SlaveInitializer<Tool> slaveInitializer, String namePrefix) {
    this.slaveInitializer = slaveInitializer;
    this.namePrefix = namePrefix;
  }
  
  public SlaveFactory(SlaveInitializer<Tool> slaveInitializer) {
    this.slaveInitializer = slaveInitializer;
    this.namePrefix = slaveInitializer.getThreadNamePrefix();
  }

  public SlaveThread<Tool> newThread(Runnable runnable) {
    int sn = slaveNumber.getAndIncrement();
    return new SlaveThread<Tool>( namePrefix+"-"+sn, slaveInitializer, sn, runnable );
  }

  
}
