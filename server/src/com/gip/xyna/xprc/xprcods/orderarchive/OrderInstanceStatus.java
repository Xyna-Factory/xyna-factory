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

package com.gip.xyna.xprc.xprcods.orderarchive;

import java.util.HashMap;

import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;


public enum OrderInstanceStatus implements StringSerializable<OrderInstanceStatus> {
  
  INITIALIZATION("Initialization", StatusGroup.Accepted,MonitoringCodes.START_STOP_MONITORING),
  //
  RUNNING_PLANNING("Processing Planning Stage", StatusGroup.Planning, MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  FINISHED_PLANNING("Finished Planning Stage", StatusGroup.Planning, MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  //
  SCHEDULING("Scheduling", StatusGroup.Scheduling, MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  SCHEDULING_CAPACITY("Scheduling for Capacity", StatusGroup.Scheduling, MonitoringCodes.STEP_MONITORING),
  SCHEDULING_VETO("Scheduling for Veto", StatusGroup.Scheduling, MonitoringCodes.STEP_MONITORING),
  //
  WAITING_FOR_CAPACITY("Waiting for Capacity", StatusGroup.Scheduling, MonitoringCodes.STEP_MONITORING),
  WAITING_FOR_VETO("Waiting for Veto", StatusGroup.Scheduling, MonitoringCodes.STEP_MONITORING),
  WAITING_FOR_LOCK("Waiting for Lock", StatusGroup.Waiting, MonitoringCodes.STEP_MONITORING),
  WAITING_FOR_PREDECESSOR("Waiting for Predecessor", StatusGroup.Waiting, MonitoringCodes.STEP_MONITORING),
  WAITING_FOR_TIMECONSTRAINT("Waiting for TimeConstraint", StatusGroup.Waiting, MonitoringCodes.STEP_MONITORING),
  WAITING_FOR_BATCH_PROCESS("Waiting for BatchProcess", StatusGroup.Waiting, MonitoringCodes.STEP_MONITORING),
  WAITING_FOR_DEPLOYMENT("Waiting for Deployment", StatusGroup.Waiting, MonitoringCodes.STEP_MONITORING),
  //
  RUNNING("Running", StatusGroup.Running, MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  RUNNING_EXECUTION("Processing Execution Stage", StatusGroup.Running, MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  RUNNING_CLEANUP("Processing Cleanup Stage", StatusGroup.Running, MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  //
  FINISHED_EXECUTION("Finished Execution Stage", StatusGroup.Cleanup, MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  FINISHED_CLEANUP("Finished Cleanup Stage", StatusGroup.Cleanup, MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  //
  FINISHED("Finished", StatusGroup.Succeeded, MonitoringCodes.START_STOP_MONITORING),
  //
  XYNA_ERROR("XynaException", StatusGroup.Failed, MonitoringCodes.ERROR_MONITORING),
  RUNTIME_ERROR("Error", StatusGroup.Failed, MonitoringCodes.ERROR_MONITORING),
  RUNTIME_EXCEPTION("RuntimeException", StatusGroup.Failed, MonitoringCodes.ERROR_MONITORING),
  SCHEDULING_TIME_OUT("Scheduling timeout", StatusGroup.Failed, MonitoringCodes.ERROR_MONITORING),
  CANCELED("Canceled", StatusGroup.Failed, MonitoringCodes.ERROR_MONITORING );
  //  
  
  private String name;
  private StatusGroup statusGroup;
  private int minimumMonitoringLevel;
  
  private static HashMap<String,OrderInstanceStatus> map;

  private OrderInstanceStatus( String name, StatusGroup statusGroup, Integer minimumMonitoringLevel ) {
    this.name = name;
    this.statusGroup = statusGroup;
    this.minimumMonitoringLevel = minimumMonitoringLevel;
  } 

  public enum StatusGroup {
    Accepted(true,false),
    Planning(true,false),
    Scheduling(true,false),
    Waiting(true,false), 
    Running(true,false),
    Cleanup(true,false),
    Succeeded(false,true),
    Failed(false,true);
    private boolean active;
    private boolean finished;
    private StatusGroup(boolean active, boolean finished) {
      this.active = active;
      this.finished = finished;
    }
    public boolean isActive() {
      return active;
    }
    public boolean isFinished() {
      return finished;
    }
  };
 
  /**
   * Ist Auftrag noch aktiv? (derzeit ! isFinished() ) 
   * @return
   */
  public boolean isActive() {
    return statusGroup.isActive();
  }
  
  /**
   * Ist Auftrag mit Fehler beendet?
   * @return
   */
  public boolean isFailed() {
    return statusGroup == StatusGroup.Failed;
  }
  
  /**
   * Ist Auftrag erfolgreich beendet?
   * @return
   */
  public boolean isSucceeded() {
    return statusGroup == StatusGroup.Succeeded;
  }
 
  /**
   * Ist Auftrag beendet? Egal ob erfolgreich oder mit Fehler
   * @return
   */
  public boolean isFinished() {
    return statusGroup.isFinished();
  }
  
  /**
   * Ist Auftrag im Scheduler?
   * StatusGroup in (Scheduling,Waiting,Accepted)
   * (Accepted wird hinzugenommen, damit Abfragen im Scheduler einfacher werden, INITIALIZATION ist da normaler Anfangsstatus)
   * @return
   */
  public boolean isInScheduler() {
    return statusGroup == StatusGroup.Scheduling 
        || statusGroup == StatusGroup.Waiting 
        || statusGroup == StatusGroup.Accepted;  
  }
  
  public String getName() {
    return name;
  }
  
  public int getMinimumMonitoringLevel() {
    return minimumMonitoringLevel;
  }
  
  public StatusGroup getStatusGroup() {
    return statusGroup;
  }
  
  public OrderInstanceStatus deserializeFromString(String string) {
    return fromString(string);
  }
  
  public String serializeToString() {
    return name;
  }
  
  public static OrderInstanceStatus fromString(String string) {
    OrderInstanceStatus status = getOrCreateMap().get(string);
    if( status != null ) {
      return status;
    } else {
      throw new IllegalArgumentException( "No OrderInstanceStatus with name \""+string+"\"");
    }
  }

  private static HashMap<String,OrderInstanceStatus> getOrCreateMap() {
    if( map != null ) {
      return map;
    }
    HashMap<String,OrderInstanceStatus> hm = new HashMap<String,OrderInstanceStatus>();
    for( OrderInstanceStatus e : values() ) {
      hm.put(e.getName(), e );
    }
    map = hm;
    return map;
  }

  public static OrderInstanceStatus failed(XynaExceptionInformation xei) {
    if (xei.isCausedByRuntimeException()) {
      return OrderInstanceStatus.RUNTIME_EXCEPTION;
    } else if (xei.isCausedByError()) {
      return OrderInstanceStatus.RUNTIME_ERROR;
    } else {
      return OrderInstanceStatus.XYNA_ERROR;
    }
  }

}
