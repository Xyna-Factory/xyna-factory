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

package com.gip.xyna.xact.trigger;

import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;


/**
 * Beschr�nkung von Events direkt beim Trigger
 */
public class ReceiveControlAlgorithm {

  public static final String SERVER_LOAD_TOO_HIGH_MESSAGE = "The request cannot be processed due to high server load.";

  //TODO persistieren beim serverneustart (siehe triggerconnection-todo)
  private AtomicLong currentActiveEvents = new AtomicLong(0);
  private volatile long maxReceives = -1;
  private boolean rejectRequestsAfterMaxReceives = false;

  /**
   * Darf ein Receive durchgef�hrt werden?
   * @return
   */
  public boolean canReceive() {
    if ( maxReceives <= -1) {
      return true; //unbegrenzte Receives
    }
    if( currentActiveEvents.get() < maxReceives ) {
      return true; //maxReceives ist nicht �berschritten
    }
    
    if (rejectRequestsAfterMaxReceives) {
      return true; //darf gelesen werden, muss aber evtl. rejected werden
    } else {
      return false; //kein autoreject, daher darf kein Receive durchgef�hrt werden
    }
  }

  /**
   * ReceiveControlAlgorithm wird benachrichtigt, dass ein Receive stattgefunden hat.
   * R�ckgabe ist, ob Event abgelehnt werden muss.
   * @return null, wenn Event akzeptiert wird, ansonsten Begr�ndung
   */
  public String notifyReceive() {
    long cnt = currentActiveEvents.incrementAndGet();
    if ( maxReceives <= -1) {
      return null; //unbegrenzte Receives
    }
    if( cnt > maxReceives ) {
      return SERVER_LOAD_TOO_HIGH_MESSAGE;
    }
    return null;
  }

  public void decrementActiveEvents() {
    currentActiveEvents.decrementAndGet();
    long l = 0;
    while ((l = currentActiveEvents.get()) < 0) {
      //das versteckt fehler, ist aber notwendig, damit die konfiguration von maxTriggerEvents m�glich ist, w�hrend der trigger l�uft.
      currentActiveEvents.compareAndSet(l, 0);
    }
  }

  /**
   * bewirkt, dass {@link #notifyReceive()} erst wieder aufgerufen wird, wenn weniger als die angegebene zahl von 
   * events noch nicht verarbeitet wurde. <br>
   * verarbeitet bedeutet, dass auf die zugeh�rige {@link TriggerConnection} die methode {@link TriggerConnection#close()}
   * aufgerufen wurde.<br>
   * dies geschieht automatisch am ende der methoden {@link EventListener#onProcessingRejected(String, TriggerConnection)}, {@link EventListener#onNoFilterFound(TriggerConnection)},
   * {@link ConnectionFilter#onError(XynaException[], com.gip.xyna.xprc.xpce.OrderContext)} und 
   * {@link ConnectionFilter#onResponse(GeneralXynaObject, com.gip.xyna.xprc.xpce.OrderContext)}.<br>
   * kann auch manuell im filter aufgerufen werden, um die beschr�nkung f�r einen auftrag zu umgehen.
   * @param maxReceivesInParallel set to -1 to deactivate this feature. -1 also is the default. 0 means, the trigger doesnt produce any events.
   */
  public void setMaxReceivesInParallel(long maxReceivesInParallel) {
    if (maxReceives == -1) {
      currentActiveEvents.set(0); //falls der trigger bereits im receive h�ngt, wird danach auf -1 runtergesetzt, das ist in decrementActiveEvents() abgefangen.
    }
    maxReceives = maxReceivesInParallel;
  }

  public void setRejectRequestsAfterMaxReceives(boolean rejectRequestsAfterMaxReceives) {
    this.rejectRequestsAfterMaxReceives = rejectRequestsAfterMaxReceives;
  }

  public boolean isRejectRequestsAfterMaxReceives() {
    return rejectRequestsAfterMaxReceives;
  }

  public long getMaxReceivesInParallel() {
    return maxReceives;
  }

  /**
   * @return
   */
  public long getCurrentActiveEvents() {
    return currentActiveEvents.get();
  }

}
