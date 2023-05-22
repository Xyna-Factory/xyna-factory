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
package com.gip.xyna.xprc.xsched.scheduling;

import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xsched.SchedulingData;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.capacities.CapacityAllocationResult;


/**
 *
 */
public class TryScheduleImpl implements TrySchedule {

  public enum Type {
    Normal,
    Pause,
    Shutdown;
  }
  
  private TrySchedule tryScheduleAlgorithm;
  private XynaScheduler xynaScheduler;
  
  public TryScheduleImpl(XynaScheduler xynaScheduler, Type type) {
    this.xynaScheduler = xynaScheduler;
    useType( type );
  }
  
  public TryScheduleResult trySchedule(SchedulingOrder so) {
    return tryScheduleAlgorithm.trySchedule(so);
  }
  
  public String state() {
    return tryScheduleAlgorithm.state();
  }
  
  public void useType(Type type) {
    switch( type ) {
      case Normal:
        this.tryScheduleAlgorithm = new TryScheduleNormal(xynaScheduler);
        break;
      case Pause: 
        this.tryScheduleAlgorithm = new TryScheduleForPause(xynaScheduler);
        break;
      case Shutdown:
        this.tryScheduleAlgorithm = new TryScheduleForShutdown(xynaScheduler);
        break;
    }
  }

  
 
  public static class TryScheduleNormal extends TryScheduleAbstract {

    public TryScheduleNormal(XynaScheduler xynaScheduler) {
      super(xynaScheduler);
    }

    public TryScheduleResult trySchedule(SchedulingOrder so) {
      return tryScheduleInternal(so);
    }

    public String state() {
      return "Scheduling all orders";
    }

  }
  
  
  public static class TryScheduleForPause extends TryScheduleAbstract {

    public TryScheduleForPause(XynaScheduler xynaScheduler) {
      super(xynaScheduler);
    }

    public TryScheduleResult trySchedule(SchedulingOrder so) {
      if ( so.isMarkedAsTimedout() ) {
        return tryScheduleInternal(so);
      } else if ( so.getDestinationKey().equals(XynaDispatcher.DESTINATION_KEY_SUSPEND_ALL) ||
                  so.getDestinationKey().equals(XynaDispatcher.DESTINATION_KEY_SUSPEND) ) {
        return tryScheduleInternal(so); 
      } else if( so.hasParentOrder() ) {
        // Running workflows are allowed to finish. 
        // Therefore subworkflow orders have to pass.
        return tryScheduleInternal(so);
      } else {
        //alle anderen Auftr�ge �berspringen
        return TryScheduleResult.CONTINUE;
      }
    }

    public String state() {
      return "Scheduling only already running orders";
    }

  }
  
  
  public static class TryScheduleForShutdown extends TryScheduleAbstract {

    public TryScheduleForShutdown(XynaScheduler xynaScheduler) {
      super(xynaScheduler);
    }

    public TryScheduleResult trySchedule(SchedulingOrder so) {
      if( ! so.canBeScheduled() ) {
        //Auftrag hat Fehler Cancel, Timeout etc und soll daher beendet werden
        return tryScheduleInternal(so);
      } else if( so.getDestinationKey().equals(XynaDispatcher.DESTINATION_KEY_SUSPEND_ALL) ) {
        //Aufr�umauftrag soll laufen
        return tryScheduleInternal(so);
      } else {
        //kein anderer Auftrag soll laufen
        SchedulingData schedulingData = so.getSchedulingData();
        if (schedulingData != null && ! schedulingData.getCapacities().isEmpty() ) {
          return TryScheduleResult.getCapacityNotAvailableInstance(new CapacityAllocationResult(schedulingData.getCapacities().get(0).getCapName()));
        } else {
          return TryScheduleResult.PAUSE;
        }
      }
    }

    public String state() {
      return "Shutting down";
    }

  }


  public void notifyScheduler() {
    xynaScheduler.notifyScheduler();
  }
  
}
