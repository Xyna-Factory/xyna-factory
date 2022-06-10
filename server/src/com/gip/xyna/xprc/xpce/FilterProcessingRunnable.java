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
package com.gip.xyna.xprc.xpce;



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.xact.trigger.ReceiveControlAlgorithm;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.xprc.XynaRunnable;



public class FilterProcessingRunnable extends XynaRunnable {

  private static final Logger logger = CentralFactoryLogging.getLogger(FilterProcessingRunnable.class);

  private final EventListener<? super TriggerConnection, ?> el;
  private final TriggerConnection tc;


  public FilterProcessingRunnable(EventListener<?, ?> el, TriggerConnection tc) {
    super("FilterProcessing");
    this.el = (EventListener<? super TriggerConnection, ?>) el;
    this.tc = tc;
  }


  @Override
  public String toString() {
    return "FilterProcessing(" + el.getClass().getSimpleName() + ")";
  }


  public void run() {
    logger.debug("Opening FilterProcessingThread");
    try {
      el.processFilters(tc);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      logger.warn("unexpected error during filter processing", t);
    }
  }


  public void rejected() {
    if (logger.isDebugEnabled()) {
      logger.debug("FilterProcessingRunnable was rejected " + tc);
    }
    try {
      el.onProcessingRejectedProxy(ReceiveControlAlgorithm.SERVER_LOAD_TOO_HIGH_MESSAGE, tc);
    } catch (Throwable t) {
      Department.handleThrowable(t);
      if (tc != null) {
        try {
          el.onProcessingRejectedProxy("unexpected error: " + t.getMessage(), tc);
        } catch (Throwable t1) {
          Department.handleThrowable(t1);
          logger.warn("could not reject event " + tc, t1);
        }
      }
      logger.error("unexpected error during execution of trigger.", t);
    }
  }


  @Override
  public boolean isRejectable() {
    return true;
  }

}
