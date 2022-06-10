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
package com.gip.xyna.xprc.xsched.timeconstraint;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;


public class TimeConstraintResult {

  public static final TimeConstraintResult SUCCESS = new TimeConstraintResult(false, true, true, null);
  public static final TimeConstraintResult WAIT_MISSING = new TimeConstraintResult(false, false, false, OrderInstanceStatus.WAITING_FOR_TIMECONSTRAINT);
  public static final TimeConstraintResult WAIT_NOT_OPEN = new TimeConstraintResult(false, false, true, OrderInstanceStatus.WAITING_FOR_TIMECONSTRAINT);
  public static final TimeConstraintResult WAIT_STARTTIME = new TimeConstraintResult(false, false, true, OrderInstanceStatus.WAITING_FOR_TIMECONSTRAINT);
  public static final TimeConstraintResult CONTINUE = new TimeConstraintResult(false, false, false, null);
  public static final TimeConstraintResult TIMED_OUT = new TimeConstraintResult(true, false, false, null);
      
  private XynaException xynaException = null;
  private boolean isExecutable;
  private boolean isTimedOut;
  private boolean removeFromScheduler;
  private OrderInstanceStatus status;

 
  public TimeConstraintResult(boolean isTimedOut, boolean isExecutable, boolean removeFromScheduler, OrderInstanceStatus status) {
    this.isTimedOut = isTimedOut;
    this.isExecutable = isExecutable;
    this.removeFromScheduler = removeFromScheduler;
    this.status = status;
  }

  public TimeConstraintResult(XynaException xynaException) {
    this.isExecutable = false;
    this.xynaException = xynaException;
  }

  public boolean isTimedOut() {
    return isTimedOut;
  }
  
  public boolean isExecutable() {
    return isExecutable;
  }

  public XynaException getXynaException() {
    return xynaException;
  }

  public boolean removeFromScheduler() {
    return removeFromScheduler;
  }

  public OrderInstanceStatus getOrderInstanceStatus() {
    return status;
  }

  public boolean scheduleLater() {
    return ! isExecutable && !isTimedOut && xynaException == null;
  }

}
