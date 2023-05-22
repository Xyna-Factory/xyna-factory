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



/**
 *
 */
public enum MasterWorkflowStatus {
  
  FINISHED( OrderInstanceStatus.FINISHED, true ),
  XYNA_ERROR( OrderInstanceStatus.XYNA_ERROR, false ),
  SCHEDULING_TIME_OUT( OrderInstanceStatus.SCHEDULING_TIME_OUT, false ),
  CANCELED( OrderInstanceStatus.CANCELED, false );
   
  private String name;
  private boolean succeeded;

  private MasterWorkflowStatus( OrderInstanceStatus status, boolean succeeded ) {
    this.name = status.getName();
    this.succeeded = succeeded;
  }
  
   
  public boolean isFailed() {
    return ! succeeded;
  }
  
  public boolean isSucceeded() {
    return succeeded;
  }
  
  public String getName() {
    return name;
  }

  public static MasterWorkflowStatus fromName( String name ) {
    for( MasterWorkflowStatus e : values() ) {
      if( e.name.equals(name) ) {
        return e;
      }
    }
    return null;
  }
  
}
