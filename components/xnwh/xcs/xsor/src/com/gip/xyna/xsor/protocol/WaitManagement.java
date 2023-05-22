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

package com.gip.xyna.xsor.protocol;



import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.xsor.common.Interconnectable;
import com.gip.xyna.xsor.common.ReplyCode;



public class WaitManagement {

  private static final Logger logger = Logger.getLogger(WaitManagement.class.getName());

  private static long NOT_STRICTLY_TIMEOUT = 100;
  private static long STRICTLY_TIMEOUT = 5 * 60 *1000;
  
  //Globale Timeouts setzen, z.B. zum Aufheben von Deadlocks
  public static void setTimeouts(long notStrictlyTimeout, long strictlyTimeout ){
    NOT_STRICTLY_TIMEOUT=notStrictlyTimeout;
    STRICTLY_TIMEOUT=strictlyTimeout;
  }

  private ConcurrentHashMap<Integer, Object> ht = new ConcurrentHashMap<Integer, Object>();

  public void register(Integer corrID, int objectIndex) {
    ht.put(corrID, new Integer(objectIndex));
  }


  public ReplyCode waitFor(int corrId, boolean isStrictlyCoherent) {
    Object o = ht.get(corrId);
    if (o instanceof ReplyCode) {
      ht.remove(corrId);
      return (ReplyCode) o;
    }
    //o ist ein Integer!
    final long start = System.currentTimeMillis();
    while (true) {
      synchronized (o) {
        Object possiblyChangedObject = ht.get(corrId);
        if (possiblyChangedObject instanceof ReplyCode) {
          ht.remove(corrId);
          return (ReplyCode) possiblyChangedObject;
        }


        if (!isStrictlyCoherent && NOT_STRICTLY_TIMEOUT <= 0) {
          ht.remove(corrId);
          return ReplyCode.TIMEOUT;
        }

        if (NOT_STRICTLY_TIMEOUT > 1) {
          try {
            o.wait(NOT_STRICTLY_TIMEOUT);//verlassen per notify oder timeout
          } catch (InterruptedException e) {
          }
        }
        // TODO get object again here to not timeout with an object present?

        long currentTime = System.currentTimeMillis();
        if (!isStrictlyCoherent && currentTime >= start + NOT_STRICTLY_TIMEOUT) {
          ht.remove(corrId);
          return ReplyCode.TIMEOUT;
        }

        if (currentTime >= start + STRICTLY_TIMEOUT) {
          logger.error("WAIT FOR TIMEOUT " + (STRICTLY_TIMEOUT / 60 / 1000) + " MINUTES: DEADLOCK PRESUMED corrid="
              + corrId + ";objectIndex=" + o.toString(), new Exception("DEADLOCK"));
          ht.remove(corrId);
          return ReplyCode.DEADLOCK_DETECTED;
        }

        if (possiblyChangedObject != o) {
          o = possiblyChangedObject; // reset for synchronized
        }
      }
      if (NOT_STRICTLY_TIMEOUT == 1) {
        Thread.yield();
      }
    }
  }


  public void storeReplyCodeAndNotifyWaiting(int corrId, ReplyCode replyCode, Interconnectable ccMemory) {
    Integer corrIdBoxed = corrId;
    Object objectIndex = ht.put(corrIdBoxed, replyCode);
    if (objectIndex != null) {
      if (objectIndex instanceof Integer) {
        synchronized (objectIndex) {
          objectIndex.notifyAll();
        }
      } else if (objectIndex instanceof ReplyCode) {
        ReplyCode oldReplyCode = (ReplyCode)objectIndex;
        if (oldReplyCode == ReplyCode.CLUSTERSTATECHANGE) {
          //ok, da war der clusterstatechange schneller. macht aber nichts
          //notifikation hat dann bereits stattgefunden
        } else {
          logger.warn("got duplicate ReplyCode old=" + objectIndex + ", new=" + replyCode + " for corrId = " + corrId);
        }
      }
    } else {
      //keiner wartet - z.b. weil vorher doNotWaitFor aufgerufen wurde
      ht.remove(corrIdBoxed);
    }
  }


  public void doNotWaitFor(int corrId) {
    ht.remove(corrId);
  }


  public void notifyAllWaitingThreadsWithClusterStateChange() {
    for (Entry<Integer, Object> entry : ht.entrySet()) {
      Object o = entry.getValue();
      if (o instanceof Integer) {
        if (!ht.replace(entry.getKey(), o, ReplyCode.CLUSTERSTATECHANGE)) {
          //anderer replycode vorgefunden => ok 
        }
        synchronized (o) {
          o.notifyAll();
        }
      }
    }
  }


}
