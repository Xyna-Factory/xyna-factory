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
package com.gip.xyna.utils.collections;



import java.util.concurrent.atomic.AtomicInteger;



/**
 * oberklasse zur verwendung mit {@link ConcurrentMapWithObjectRemovalSupport}.
 */
public abstract class ObjectWithRemovalSupport {

  private final AtomicInteger safety = new AtomicInteger(0);


  /**
   * @return true, falls object nicht entfernt werden soll. es wird sichergestellt, dass es nicht entfernt wird
   */
  // >=0 -> ++ return true,  <0 -> return false
  boolean markForUsage() {
    int oldVal = safety.get();
    while (oldVal >= 0) {
      if (!safety.compareAndSet(oldVal, oldVal + 1)) {
        oldVal = safety.get();
      } else {
        return true;
      }
    }
    return false;
  }


  /**
   * @return true, falls object von niemandem mehr in benutzung ist und deshalb entfernt werden kann.
   *  es wird sichergestellt, dass nach dem entfernen nichts mehr geändert wird
   */
  // 0 -> -1 return true, != 0 -> return false
  boolean markForDeletion() {
    int oldVal = safety.get();
    while (oldVal == 0) {
      if (!safety.compareAndSet(0, -1)) {
        oldVal = safety.get();
      } else {
        return true;
      }
    }
    return false;
  }


  void unmarkForUsage() {
    if (safety.decrementAndGet() < 0) {
      throw new RuntimeException();
    }
  }


  void unmarkForDeletion() {
    if (safety.get() != -1) {
      throw new RuntimeException();
    }
    safety.set(0);
  }

  /**
   * Wird ggfs mehrfach aufgerufen. Objekt wird nur aus Map entfernt, wenn diese Methode true zurück gibt.
   */
  protected abstract boolean shouldBeDeleted();


  protected void onDeletion() {
    //kann überschrieben werden
  }

}
