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
package com.gip.xyna.coherence.utils.locking;



public class LockingUtils {

  public static final int NANO_TO_MILLI = 1000000;


  /**
   * warte mit einem object.wait auf dem �bergebenen objekt, bis notify passiert, oder der �bergebene zeitpunkt erreicht
   * ist.
   */
  public static boolean wait(Object o, long nanoTimeStamp) throws InterruptedException {
    long nanos = nanoTimeStamp - System.nanoTime();
    if (nanos <= 100) {
      return false;
    }
    long ms = nanos / NANO_TO_MILLI;
    int nanoRemaining = (int) (nanos - ms * NANO_TO_MILLI);
    o.wait(ms, nanoRemaining);
    return true;
  }
}
