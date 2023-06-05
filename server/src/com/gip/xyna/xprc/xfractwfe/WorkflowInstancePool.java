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

package com.gip.xyna.xprc.xfractwfe;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;



public class WorkflowInstancePool {


  public static final int ID_DONT_REUSE_INSTANCE = -1;


  // FIXME it might be worth a thought making this a long because the integer might overflow for a large
  //       number of deployments
  private static final AtomicInteger ID = new AtomicInteger(0);


  private final int thisPoolsID;
  private final LinkedList<XynaProcess> instances;

  private final AtomicInteger currentSize;
  private final AtomicInteger maxSize;

  public WorkflowInstancePool(int maxSize) {

    thisPoolsID = ID.incrementAndGet();

    this.maxSize = new AtomicInteger(maxSize);
    this.currentSize = new AtomicInteger(0);
    instances = new LinkedList<XynaProcess>();
  }


  public void setMaxSize(int max) {
    synchronized (this) {
      this.maxSize.set(max);
      if (instances.size() > max) {
        Iterator<XynaProcess> instancesIterator = instances.iterator();
        int toBeRemoved = instances.size() - max;
        for (int i=0; i<toBeRemoved; i++) {
          instancesIterator.next();
          instancesIterator.remove();
        }
      }
    }
  }


  public XynaProcess getProcessInstance() {
    synchronized (this) {
      XynaProcess xp = instances.poll();
      if (xp != null) {
        currentSize.decrementAndGet();
      }
      return xp;
    }
  }


  public void returnProcessInstance(XynaProcess usedInstance) {

    // only get the lock if required, that is why we need AtomicInteger
    if (currentSize.get() < maxSize.get()) {

      synchronized (this) {
        // check back whether things changed since the lock has been retrieved
        if (currentSize.get() < maxSize.get()) {
          instances.add(usedInstance);
          currentSize.incrementAndGet();
          usedInstance.clear();
        }
      }
    }

  }


  public int getID() {
    return thisPoolsID;
  }

}
