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
package com.gip.xyna.xprc.xsched.scheduling;

import com.gip.xyna.xprc.xsched.capacities.CapacityAllocationResult;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraintResult;
import com.gip.xyna.xprc.xsched.vetos.VetoAllocationResult;


/**
 *
 */
public interface TrySchedule {

  public TryScheduleResult trySchedule(SchedulingOrder so);
  
  public abstract String state();
  
  
  public static enum TryScheduleResultType {
    //Scheduling fertig
    SCHEDULE,        //Auftrag wurde geschedult
    DELETE,          //Auftrag ist entweder kaputt, gecancelt oder durch ein Remove aus dem Scheduler entfernt worden
    REMOVE,          //Auftrag soll aus dem Scheduler entfernt werden
    
    //Auftrag konnte nicht geschedult werden:
    CAPACITY,        //wegen Capacity
    VETO,            //wegen Veto
    TIME_CONSTRAINT, //wegen TimeConstraint
    CONTINUE,        //aus irgendeinem Grund: Fortsetzen mit nächstem Auftrag
    BREAKLOOP,       //aus irgendeinem Grund: Abbruch und Neustart der Scheduler-Schleife
    PAUSE,           //Auftrag darf nicht geschedult werden
    
    //Auftrag soll nochmal geschedult werden
    //RETRY,           //in der gleichen Scheduler-Schleife
    RETRY_NEXT,      //in der nächsten Scheduler-Schleife Try-Schedules rückgängig machen, weil in der nächsten Scheduler-Schleife der Auftrag eventuell durch einen mit höherer Urgency versteckt wird
    REORDER;         //in der nächsten Scheduler-Schleife mit anderer Urgency
  }
  
  public static class TryScheduleResult {
    
    public static final TryScheduleResult CONTINUE = new TryScheduleResult(TryScheduleResultType.CONTINUE);
    public static final TryScheduleResult REMOVE = new TryScheduleResult(TryScheduleResultType.REMOVE);
    public static final TryScheduleResult BREAKLOOP = new TryScheduleResult(TryScheduleResultType.BREAKLOOP);
    public static final TryScheduleResult DELETE = new TryScheduleResult(TryScheduleResultType.DELETE);
    public static final TryScheduleResult PAUSE = new TryScheduleResult(TryScheduleResultType.PAUSE);
    public static final TryScheduleResult SCHEDULE = new TryScheduleResult(TryScheduleResultType.SCHEDULE,false);
    public static final TryScheduleResult RETRY_NEXT = new TryScheduleResult(TryScheduleResultType.RETRY_NEXT);
    public static final TryScheduleResult REORDER = new TryScheduleResult(TryScheduleResultType.REORDER,false);
    
    protected CapacityAllocationResult car;
    protected VetoAllocationResult var;
    private final TryScheduleResultType type;
    protected TimeConstraintResult tr;
    private final boolean needsUndo;
    
    private TryScheduleResult(TryScheduleResultType type) { //nur intern verwenden!
      this(type,true);
    }
    private TryScheduleResult(TryScheduleResultType type, boolean needsUndo) { //nur intern verwenden!
      this.type = type;
      this.needsUndo = needsUndo;
    }
    
    public static TryScheduleResult getCapacityNotAvailableInstance(CapacityAllocationResult car) {
      //TODO Performance für jede Capacity cachen statt immer neue Objekte zu bauen
      TryScheduleResult ret = new TryScheduleResult(TryScheduleResultType.CAPACITY);
      ret.car = car;
      return ret;
    }

    public static TryScheduleResult getVetoNotAvailableInstance(VetoAllocationResult var) {
      TryScheduleResult ret = new TryScheduleResult(TryScheduleResultType.VETO);
      ret.var = var;
      return ret;
    }
    
    //timeconstraint sagt, dass z.b. zeitfenster gerade zu ist, aber später wieder aufgeht
    public static TryScheduleResult getScheduleLaterInstance(TimeConstraintResult tr) {
      TryScheduleResult ret = new TryScheduleResult(TryScheduleResultType.TIME_CONSTRAINT);
      ret.tr = tr;
      return ret;
    }

    
    public TryScheduleResultType getType() {
      return type;
    }


    @Override
    public String toString() {
      return "TryScheduleResult("+type+"," + car + "," + var + "," + tr +  ")";
    }

    /**
     * @return
     */
    public boolean needsUndo() {
      return needsUndo;
    }

  }

  
  
}
