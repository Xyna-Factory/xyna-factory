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


public abstract class PausableRunnable implements Runnable {
  
  protected volatile boolean runToggle;
  
  
  protected PausableRunnable() {
    runToggle = false;
  }
  
  public boolean start() {
    if (runToggle) {
      return false;
    }
    runToggle = true;
    return true;
  }
  
  public boolean stop() {
    if (!runToggle) {
      return false;
    }
    runToggle = false;
    return waitForTermination();
  }
  
  public boolean isRunning() {
    return runToggle;
  }
  
  
  protected abstract void runOnce() throws Exception ;
  
  protected abstract boolean waitForTermination();

}
