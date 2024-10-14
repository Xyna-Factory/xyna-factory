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

package com.gip.xyna.xprc.xpce.monitoring;

import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.xprc.XynaOrder;


public class MonitoringCodes {

  /**
   * No audit data will be created at all.
   */
  public static final Integer NO_MONITORING = 0;

  /**
   * Audit data will only be created if an error occurs.
   */
  public static final Integer ERROR_MONITORING = 5;

  /**
   * Audit data will be created. After creation, the only update to the captured data is performed after finishing the Cleanup stage. Includes ERROR_MONITORING=5.
   */
  public static final Integer START_STOP_MONITORING = 10;

  /**
   * Audit data will be created. Every Master Workflow state change results in an update to the captured data, especially to the "last update" timestamp. Includes START_STOP_MONITORING=10.
   */
  public static final Integer MASTER_WORKFLOW_MONITORING = 15;

  /**
   * Runtime = STEP_MONITORING, Archive on error =  STEP_MONITORING &amp; on success = NO_MONITORING 
   */
  public static final Integer STEP_MONITORING_ON_ERROR_NO_ARCHIVE = 17;
  
  /**
   * Runtime = STEP_MONITORING, Archive on error =  STEP_MONITORING &amp; on success = START_STOP_MONITORING
   */
  public static final Integer STEP_MONITORING_ON_ERROR_START_STOP_ARCHIVE = 18;
  
  /**
   * Audit data will be created. Input, Output and Error information for every workflow step will be captured and will be added to the audit data. Includes MASTER_WORKFLOW_MONITORING=15.
   */
  public static final Integer STEP_MONITORING = 20;

  
  /**
   * Konstante für ein MinimumMonitoringLevel, die dazu führt, dass kein valides MonitoringLevel passt
   */
  public static final Integer TOO_HIGH = 25;

  
  public static Set<Integer> getAllValidMonitoringLevels() {
    Set<Integer> allSet = new HashSet<Integer>();
    allSet.add(NO_MONITORING);
    allSet.add(ERROR_MONITORING);
    allSet.add(START_STOP_MONITORING);
    allSet.add(MASTER_WORKFLOW_MONITORING);
    allSet.add(STEP_MONITORING_ON_ERROR_NO_ARCHIVE);
    allSet.add(STEP_MONITORING_ON_ERROR_START_STOP_ARCHIVE);
    allSet.add(STEP_MONITORING_ON_ERROR_START_STOP_ARCHIVE);
    allSet.add(STEP_MONITORING);
    return allSet;
  }
  

  public static int getMonitoringLevelForArchiving(XynaOrder order) {
    Integer monitoringCode = order.getMonitoringCode();
    boolean hasError = order.hasError();
    if (monitoringCode == null) {
      return NO_MONITORING;
    } else if (monitoringCode.equals(STEP_MONITORING_ON_ERROR_NO_ARCHIVE)) {
      if (hasError) {
        return STEP_MONITORING;  
      } else {
        return NO_MONITORING;
      }
    } else if (monitoringCode.equals(STEP_MONITORING_ON_ERROR_START_STOP_ARCHIVE)) {
      if (hasError) {
        return STEP_MONITORING;  
      } else {
        return START_STOP_MONITORING;
      }
    } else {
      return monitoringCode;
    }
  }
  
  
  public static int getMonitoringLevelForRuntime(XynaOrder order) {
    Integer monitoringCode = order.getMonitoringCode();
    if (monitoringCode == null) {
      return NO_MONITORING;
    } else if (monitoringCode <= MASTER_WORKFLOW_MONITORING) {
      return monitoringCode.intValue();
    } else if (monitoringCode >= TOO_HIGH) {
      return NO_MONITORING;
    } else {
      return STEP_MONITORING;
    }
  }


  public static boolean isValid(int val) {
    switch (val) {
      case 0 :
      case 5 :
      case 10 :
      case 15 :
      case 17 :
      case 18 :
      case 20 :
        return true;
      default :
        return false;
    }
  }
  
}
