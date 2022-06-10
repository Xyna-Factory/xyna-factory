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
package com.gip.xyna.xprc.xprcods.orderarchive;

import java.util.HashMap;

import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xprc.xpce.monitoring.MonitoringCodes;


public enum OrderInstanceSuspensionStatus implements StringSerializable<OrderInstanceSuspensionStatus> {

  SUSPENDED("Suspended", MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  WAITING_FOR_RESUME("Waiting for resume", MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  NOT_SUSPENDED("Not Suspended", MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  MANUAL_INTERACTION("Manual Interaction", MonitoringCodes.MASTER_WORKFLOW_MONITORING),
  MANUAL_INTERACTION_IN_SUBWF("Manual Interaction in Subworkflow", MonitoringCodes.TOO_HIGH);

  private static HashMap<String,OrderInstanceSuspensionStatus> map;

  private String name;
  private int minimumMonitoringLevel;
  OrderInstanceSuspensionStatus(String name, int minimumMonitoringLevel) {
    this.name = name;
    this.minimumMonitoringLevel = minimumMonitoringLevel;
  }
  
  @Override
  public String toString() {
    return name;
  }
  
  public String getName() {
    return name;
  }

  public int getMinimumMonitoringLevel() {
    return minimumMonitoringLevel;
  }

  public OrderInstanceSuspensionStatus deserializeFromString(String string) {
    return fromString(string);
  }
  
  public String serializeToString() {
    return name;
  }
  
  public static OrderInstanceSuspensionStatus fromString(String string) {
    if( string == null ) {
      return null;
    }
    OrderInstanceSuspensionStatus status = getOrCreateMap().get(string);
    if( status != null ) {
      return status;
    } else {
      throw new IllegalArgumentException( "No OrderInstanceSuspensionStatus with name \""+string+"\"");
    }
  }

  private static HashMap<String,OrderInstanceSuspensionStatus> getOrCreateMap() {
    if( map != null ) {
      return map;
    }
    HashMap<String,OrderInstanceSuspensionStatus> hm = new HashMap<String,OrderInstanceSuspensionStatus>();
    for( OrderInstanceSuspensionStatus e : values() ) {
      hm.put(e.getName(), e );
    }
    map = hm;
    return map;
  }

  
}
