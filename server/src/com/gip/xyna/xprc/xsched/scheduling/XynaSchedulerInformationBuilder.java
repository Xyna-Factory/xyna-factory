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
package com.gip.xyna.xprc.xsched.scheduling;

import com.gip.xyna.utils.scheduler.SchedulerInformationBuilder;


public class XynaSchedulerInformationBuilder implements SchedulerInformationBuilder<SchedulerInformationBean> {

  
  private SchedulerInformationBean sib = new SchedulerInformationBean();
  
  public SchedulerInformationBean build() {
    SchedulerInformationBean ret = sib;
    sib = new SchedulerInformationBean();
    return ret;
  }

  public SchedulerInformationBean getSchedulerInformationBean() {
    return sib;
  }

  
  public void loopEndedRegularily(boolean loopEndedRegularily) {
    sib.setLoopEndedRegularily(loopEndedRegularily);
  }

  public void scheduledOrders(int scheduled) {
    sib.setLastScheduledOrders(scheduled);
  }

  public void iteratedOrders(int iterated) {
    sib.setLastIteratedOrders(iterated);
  }

  public long timestamp(Timestamp timestamp) {
    long now = System.currentTimeMillis();
    switch( timestamp ) {
      case Start:
        sib.timestampStart(now);
        break;
      case PreparationFinished:
        sib.timestampPreparation(now);
        break;
      case SchedulingFinished:
        sib.timestampScheduling(now);
        break;
      case Finished:
        sib.timestampFinished(now);
        break;
    }
    return now;
  }


  public void waitingForTags(int waitingForTags) {
    sib.setWaitingForCapacity(waitingForTags);
  }

  public void waitingForTagNull(int waitingForTagNull) {
    sib.setWaitingForVeto(waitingForTagNull);
  }

  public void waitingForUnknown(int waitingForUnknown) {
    sib.setWaitingForUnknown(waitingForUnknown); 
  }

  public void schedulerRunNumber(long schedulerRunNumber) {
    sib.setSchedulerRunNumber(schedulerRunNumber);
  }


}
