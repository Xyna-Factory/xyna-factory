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
package com.gip.xyna.coherence.utils.threadpool;



import java.util.LinkedList;


/**
 * einfacher pool mit fester anzahl an elementen.<p>
 * beim shutdown des pools werden die objekte auch geshutdowned.
 * TODO: <br>
 * - methoden um mehrere objekte auf einmal zu entnehmen und wieder reinzustecken, um locks/notifys zu sparen <br>
 * - garantie der reihenfolge, in der wartende threads objekte bekommen? konfigurierbar <br>
 */
public class ObjectPool<A extends Shutdownable> {

  private LinkedList<A> freeObjects = new LinkedList<A>();
  private Object lock = new Object();


  public interface ObjectFactory<A> {

    public A create();
  }


  public ObjectPool(ObjectFactory<A> factory, int size) {
    for (int i = 0; i < size; i++) {
      freeObjects.add(factory.create());
    }
  }

  /**
   * gibt naechstes freies objekt aus pool zurueck.
   * wartet, bis ein objekt frei wird, falls derzeit keins frei ist.
   */
  public A getFree() {
    synchronized (lock) {
      while (freeObjects.size() == 0) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          //FIXME prio4: InterruptedException behandeln? Gegenwärtig wird dann potentiell eine NoSuchElementException geworfen.
        }
      }
      return freeObjects.remove();
    }
  }


  public void returnToPool(A object) {
    synchronized (lock) {
      freeObjects.add(object);
      lock.notify();
    }
  }


  public void shutdown() {
    for (A a : freeObjects) {
      a.shutdown();
    }
    freeObjects = null;
  }

}
