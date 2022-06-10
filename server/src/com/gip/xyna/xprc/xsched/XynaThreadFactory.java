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

package com.gip.xyna.xprc.xsched;



import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;



public class XynaThreadFactory implements ThreadFactory {

  final int priority;
  final String name;
  private final static AtomicLong count = new AtomicLong(0);


  public XynaThreadFactory(String name, int priority) {
    this.name = name;
    this.priority = priority;
  }
  
  public XynaThreadFactory(int priority) {
    this("Worker", priority);
  }


  public Thread newThread(final Runnable r) {
    // classloaderleaks verhindern: vergleiche http://wasdynacache.blogspot.de/2012/01/websphere-classloader-memory-leak.html
    // oder im internet nach "classloader protectiondomain thread" suchen...
    return AccessController.doPrivileged(new PrivilegedAction<Thread>() {

      public Thread run() {
        Thread t = new Thread(r);
        long nextCount = count.getAndIncrement();
        if (t.getPriority() != priority) {
          t.setPriority(priority);
        }
        t.setName("Xyna " + name + " (prio " + priority + ", count " + nextCount + ")");
        return t;
      }
    });
  }

}
