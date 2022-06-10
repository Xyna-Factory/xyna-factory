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
package com.gip.xyna.xdev.xfractmod.xmdm;

import java.io.Serializable;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.TriggerInstanceIdentification;

public abstract class TriggerConnection implements Serializable {
  
  private static final long serialVersionUID = -2781599031708165512L;
  private transient EventListener<?,?> trigger;
  private boolean closed = false;
  private TriggerInstanceIdentification triggerInstanceId;
  
  public TriggerConnection() {
    
  }
  
  /**
   * beim überschreiben immer super.close() aufrufen!!
   * pflegt die anzahl der gleichzeitig über den gleichen trigger gestarteten, laufenden aufträge.
   * beim close() wird die anzahl um eins verringert.
   * 
   * das close wird automatisch aufgerufen, und muss mehrfach aufgerufen werden können.
   */
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    if (trigger != null) {
      trigger.decrementActiveEvents();
    } else {
      //TODO counter nach neustart weiterzählen? dann müsste man den trigger anderweitig identifizieren um den korrekten counter zu finden. siehe eventlistener-todo
    }
  }
  
  public void rollback() {
    
  }

  public boolean isClosed() {
    return closed;
  }


  void setTrigger(EventListener<?,?> trigger) {
    this.trigger = trigger;
    this.triggerInstanceId = trigger.getTriggerInstanceIdentification();
  }


  public EventListener<?,?> getTrigger() {
    if( trigger == null ) {
      trigger = restoreTrigger(triggerInstanceId);
    }
    return trigger;
  }
  
  private static EventListener<?, ?> restoreTrigger(TriggerInstanceIdentification triggerInstanceId) {
    if( triggerInstanceId == null ) {
      return null; //triggerInstanceId wird nun neu serialisert, 
      //alte TriggerConnection haben nach dem Deserialisieren noch keine Daten. 
      //Daher hier Ausstieg ohne Fehlermeldung
    }
    try {
      return XynaFactory.getInstance().getActivation().getActivationTrigger().getTriggerInstance(triggerInstanceId);
    } catch (Exception e) { //XACT_TriggerNotFound
      CentralFactoryLogging.getLogger(TriggerConnection.class).warn("Could not get Trigger for "+triggerInstanceId, e);
      return null;
    }
  }

}
