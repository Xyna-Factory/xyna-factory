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

package xact.XScrpt.services;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;



public class OutputGetter extends Thread {

  private static final Logger logger = CentralFactoryLogging.getLogger(OutputGetter.class);


  private final Object lock;
  private final Script script;
  public volatile boolean executionCompleted = false;


  public OutputGetter(Script scr, Object lock) {
    this.lock = lock;
    this.script = scr;
  }


  public void run() {
    super.run();
    synchronized (lock) {
      try {
        script.getOutput();
      } catch (Throwable e) {
        Department.handleThrowable(e);
        // FIXME exception handling besser!
        logger.warn("Exception while waiting for script output", e);
      } finally {
        executionCompleted = true;
        lock.notifyAll();
      }
    }
  }
}
