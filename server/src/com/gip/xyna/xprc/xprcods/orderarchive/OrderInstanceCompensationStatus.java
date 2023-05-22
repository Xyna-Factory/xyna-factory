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


public enum OrderInstanceCompensationStatus implements StringSerializable<OrderInstanceCompensationStatus> {

  RUNNING("Processing Compensation"),
  FINISHED("Finished Compensation"),
  ERROR("Compensation failed");
  
  private static HashMap<String,OrderInstanceCompensationStatus> map;

  private String name;
  OrderInstanceCompensationStatus(String name) {
    this.name = name;
  }
  
  @Override
  public String toString() {
    return name;
  }

  public String getName() {
    return name;
  }
  
  public int getMinimumMonitoringLevel() {
    return MonitoringCodes.MASTER_WORKFLOW_MONITORING;
  }
  
  public OrderInstanceCompensationStatus deserializeFromString(String string) {
    return fromString(string);
  }
  
  public String serializeToString() {
    return name;
  }
  
  public static OrderInstanceCompensationStatus fromString(String string) {
    if( string == null ) {
      return null;
    }
    OrderInstanceCompensationStatus status = getOrCreateMap().get(string);
    if( status != null ) {
      return status;
    } else {
      throw new IllegalArgumentException( "No OrderInstanceCompensationStatus with name \""+string+"\"");
    }
  }

  private static HashMap<String,OrderInstanceCompensationStatus> getOrCreateMap() {
    if( map != null ) {
      return map;
    }
    HashMap<String,OrderInstanceCompensationStatus> hm = new HashMap<String,OrderInstanceCompensationStatus>();
    for( OrderInstanceCompensationStatus e : values() ) {
      hm.put(e.getName(), e );
    }
    map = hm;
    return map;
  }

  public static OrderInstanceCompensationStatus finished(boolean hasError) {
    if( hasError ) {
      return ERROR;
    } else {
      return FINISHED;
    }
  }

}
