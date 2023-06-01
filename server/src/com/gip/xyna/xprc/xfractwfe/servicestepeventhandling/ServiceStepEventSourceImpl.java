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
package com.gip.xyna.xprc.xfractwfe.servicestepeventhandling;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_TTLExpirationBeforeHandlerRegistration;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.events.AbortServiceStepEvent;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.events.ServiceStepEvent;


public class ServiceStepEventSourceImpl implements ServiceStepEventSource {
  
  private Map<Class<? extends ServiceStepEvent>, ServiceStepEventHandler> eventClassHandler =
                    new HashMap<Class<? extends ServiceStepEvent>, ServiceStepEventHandler>();
    
  private XynaOrderServerExtension executingOrder;
  
  
  public ServiceStepEventSourceImpl() {
  }
  
  
  public ServiceStepEventSourceImpl(XynaOrderServerExtension executingOrder) {
    this.executingOrder = executingOrder;
  }


  public void listenOnAbortEvents(ServiceStepEventHandler<AbortServiceStepEvent> serviceStepEventHandler)
      throws XPRC_TTLExpirationBeforeHandlerRegistration {
    if (executingOrder != null && executingOrder.getOrderExecutionTimeout() != null
        && executingOrder.getOrderExecutionTimeout().getRelativeTimeoutForNowIn(TimeUnit.SECONDS) < 1) {
      String formattedExpirationTimestamp =
          Constants.defaultUTCSimpleDateFormat().format(new Date(executingOrder.getExecutionTimeoutTimestamp()));
      throw new XPRC_TTLExpirationBeforeHandlerRegistration(formattedExpirationTimestamp);
    } else {
      listenOnEvent(AbortServiceStepEvent.class, serviceStepEventHandler);
    }
  }
  
  
  private <T extends ServiceStepEvent> void listenOnEvent(Class<? extends T> clazz, ServiceStepEventHandler<T> eventHandler) {
    eventClassHandler.put(clazz, eventHandler);
  }
  
  
  public boolean hasHandlerFor(Class<? extends ServiceStepEvent> serviceStepEventClass) {
    return eventClassHandler.containsKey(serviceStepEventClass);
  }
  
  
  public void dispatchEvent(ServiceStepEvent serviceStepEvent) { 
    ServiceStepEventHandler eventHandler = eventClassHandler.get(serviceStepEvent.getClass());
    if (eventHandler != null) {
      eventHandler.handleServiceStepEvent(serviceStepEvent);
    }
  }
  


}
