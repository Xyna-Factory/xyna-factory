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
package com.gip.xyna.xprc.xsched.timeconstraint;

import java.io.Serializable;

import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint.TimeConstraint_Window;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindow;


/**
 * Mutable Klasse, die die aus der immutable Klasse {@link TimeConstraint} berechneten Timestamps 
 * sowie weitere Daten dazu aufbewahrt.
 */
public class TimeConstraintData implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private TimeConstraint definition;
  private long entranceTimestamp;
  private boolean calculated;
  private Long schedulingTimeout;
  private long startTimestamp;
  private String windowName;
  private boolean recalculatedForWindow;
  private transient boolean schedulingTimeoutMonitored; //TODO transient hilft, dass SchedulingTimeout nach einer OrderMigration klappt. Dies sollte besser realisiert werden!
  private long windowIsOpenSince; //Timestamp, seit dem das zugeh�rige Zeitfenster offen ist
  
  
  public TimeConstraintData(long entranceTimestamp, TimeConstraint definition) {
    this.entranceTimestamp = entranceTimestamp;
    this.definition = definition;
  }
  public TimeConstraintData(long entranceTimestamp) {
    this.entranceTimestamp = entranceTimestamp;
  }
  
  @Override
  public String toString() {
    return "TimeConstraintData("+entranceTimestamp+","+definition+")->startTimestamp="+startTimestamp+",schedulingTimeout="+schedulingTimeout;
  }
  
  public TimeConstraint getDefinition() {
    return definition;
  }
  
  public void setDefinition(TimeConstraint definition) {
    this.definition = definition;
    this.calculated = false; //Neuberechnung ist erforderlich
    this.schedulingTimeoutMonitored = false; //Neueintrag ist erforderlich
  }
  
  public long getEntranceTimestamp() {
    return entranceTimestamp;
  }
  
  public void setEntranceTimestamp(long entranceTimestamp) {
    this.entranceTimestamp = entranceTimestamp;
    this.calculated = false; //Neuberechnung ist erforderlich
  }
  
  /**
   * @return
   */
  public Long getSchedulingTimeout() {
    if( ! calculated ) {
      calculateSchedulingTimeoutAndStartTime();
    }
    return schedulingTimeout;
  }
  
  public long getStartTimestamp() {
    if( ! calculated ) {
      calculateSchedulingTimeoutAndStartTime();
    }
    return startTimestamp;
  }
  
  public String getTimeWindowName() {
    if( ! calculated ) {
      calculateSchedulingTimeoutAndStartTime();
    }
    return windowName;
  }
  

  private void calculateSchedulingTimeoutAndStartTime() {
    if( definition != null ) {
      startTimestamp = definition.startTimestamp(entranceTimestamp);
      schedulingTimeout = definition.schedulingTimeout(entranceTimestamp);
      if( definition instanceof TimeConstraint_Window ) {
        windowName = ((TimeConstraint_Window)definition).getWindowName();
      }
    } else {
      startTimestamp = entranceTimestamp;
    }
    calculated = true;
  }

  
  public boolean isConfigured() {
    return definition != null;
  }
  
  public boolean hasTimeout(long now) {
    if( getSchedulingTimeout() != null ) {
      return schedulingTimeout.longValue() < now;
    }
    return false;
  }
  
  public boolean hasToWaitForStartTime(long now) {
    return getStartTimestamp() > now;
  }
  
  public void recalculateInWindow(TimeConstraintWindow window) {
    if( ! recalculatedForWindow ) {
      TimeConstraint tcInWindow = null;
      if( definition instanceof TimeConstraint_Window ) {
        tcInWindow = ((TimeConstraint_Window)definition).getInnerTimeConstraint();
      }
      if( tcInWindow != null ) {
        long open = window.getOpenSince();
        startTimestamp = tcInWindow.startTimestamp(open);
        schedulingTimeout = tcInWindow.schedulingTimeout(open);
        if( schedulingTimeout != null ) {
          schedulingTimeoutMonitored = false;
        }
      }
      recalculatedForWindow = true; 
    }
  }
  
  public boolean isSchedulingTimeoutMonitored() {
    return schedulingTimeoutMonitored;
  }
  
  
  public void setSchedulingTimeoutMonitored(boolean schedulingTimeoutMonitored) {
    this.schedulingTimeoutMonitored = schedulingTimeoutMonitored;
  }
  
  public void setWindowIsOpenSince(long windowIsOpenSince) {
    this.windowIsOpenSince = windowIsOpenSince;
  }
  
  public long getWindowIsOpenSince() {
    return windowIsOpenSince;
  }

  
}
