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
package com.gip.xyna.utils.scheduler;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.scheduler.UrgencyOrderList.Urgency;


/**
 * Einfache Implementierung der unwichtigen Anpassungsmöglichkeiten aus SchedulerCustomisation.
 *
 */
public abstract class SimpleSchedulerCustomisation<O,I> implements SchedulerCustomisation<O,I> {


  public abstract SchedulerInformationBuilder<I> getInformationBuilder();

  public Pair<BlockingQueue<Urgency<O>>, BlockingQueue<Urgency<O>>> createQueues() {
    BlockingQueue<Urgency<O>> entrance;
    BlockingQueue<Urgency<O>> reorder;
    
    entrance = new LinkedBlockingQueue<Urgency<O>>();
    reorder = new LinkedBlockingQueue<Urgency<O>>();
    
    return Pair.of(entrance, reorder);
  }
  
  public void setScheduler(Scheduler<O, I> scheduler) {
  }

  public abstract long calculateUrgency(O order);
  
  public abstract ScheduleResult trySchedule(Urgency<O> uo);

  public void beforeScheduling(long urgency) {
  }

  public void postparation() {
  }

  public void preparation() {
  }

  public void endScheduling() {
  }

  public void beginScheduling() {
  }

  public void handleThrowable(Throwable t) {
  }

  public abstract long getOrderId(O order);

}
