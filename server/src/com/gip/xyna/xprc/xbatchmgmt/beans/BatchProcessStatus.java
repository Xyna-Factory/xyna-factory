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
package com.gip.xyna.xprc.xbatchmgmt.beans;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.selection.WhereClauseStringTransformation;
import com.gip.xyna.xnwh.selection.WhereClauseStringTransformation.Transformation;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;


public enum BatchProcessStatus {
  Planning,  //MasterWorkflow ist in Planning-Phase
  Waiting,   //BatchProcess wartet auf TimeConstraint, (seltener auf Capacities, Vetos), Wiederherstellung des BatchProcesses
  Running,   //BatchProcess schedult gerade / BatchProcessMaster l�uft
  Disabled,  //BatchProcess ist pausiert
  Cancelled, //BatchProcess wurde abgebrochen
  Failed,    //BatchProcessMaster wurde mit Fehler beendet
  Finished;  //BatchProcessMaster wurde erfolgreich ausgef�hrt
  
  
  public static BatchProcessStatus from(OrderInstanceStatus archiveStatus, OrderInstanceStatus currentStatus ) {
    if( archiveStatus == null || archiveStatus == OrderInstanceStatus.SCHEDULING ) {
      if( currentStatus == null ) {
        return Running;
      } else if( currentStatus.getStatusGroup() == OrderInstanceStatus.StatusGroup.Waiting ) {
        return Waiting;
      } else if( currentStatus.getStatusGroup() == OrderInstanceStatus.StatusGroup.Scheduling ) {
        return Running;
      } else if( currentStatus == OrderInstanceStatus.RUNNING_PLANNING ) {
        return Planning;
      } else {
        return Running;
      }
    } else if( archiveStatus == OrderInstanceStatus.WAITING_FOR_BATCH_PROCESS ) {
      return Disabled;
    } else if( archiveStatus == OrderInstanceStatus.CANCELED ) {
      return Cancelled;
    } else if( archiveStatus == OrderInstanceStatus.FINISHED ) {
      return Finished;
    } else if( archiveStatus.getStatusGroup() == OrderInstanceStatus.StatusGroup.Failed) {
      return Failed;
    } else {
      return null; //FIXME
    }
    
  }

  public static Transformation getStatusTransformation() {
    return statusTransformation;
  }
  
  private static StatusTransformation statusTransformation = new StatusTransformation();
  
  private static class StatusTransformation implements WhereClauseStringTransformation.Transformation {
   private static final long serialVersionUID = 1L;

    public List<String> transform(String value) {
      BatchProcessStatus bps;
      try {
        if (value.startsWith("\"") &&
            value.endsWith("\"") &&
            value.length() > 3) {
          value = value.substring(1, value.length()-1);
        }
        bps = BatchProcessStatus.valueOf(value);
      } catch( IllegalArgumentException e ) {
        //unpassenden Wert weitergeben, liefert wahrscheinlich kein Ergebnis
        return stringList(value);
      }
      switch( bps ) {
        case Planning:
        case Waiting:
        case Running:
          return stringList(OrderInstanceStatus.SCHEDULING);
        case Disabled:
          return stringList(OrderInstanceStatus.WAITING_FOR_BATCH_PROCESS);
        case Cancelled:
          return stringList(OrderInstanceStatus.CANCELED);
        case Failed:
          return stringList(OrderInstanceStatus.RUNTIME_ERROR, OrderInstanceStatus.RUNTIME_EXCEPTION, OrderInstanceStatus.XYNA_ERROR );
        case Finished:
          return stringList(OrderInstanceStatus.FINISHED);
        default:
          //unpassenden Wert weitergeben, liefert wahrscheinlich kein Ergebnis
          return stringList(value);
      }
    }

    private List<String> stringList(OrderInstanceStatus ...status) {
      List<String> list = new ArrayList<String>();
      for( OrderInstanceStatus ois : status ) {
        list.add(ois.getName());
      }
      return list;
    }

    private List<String> stringList(String value) {
      List<String> list = new ArrayList<String>();
      list.add(value);
      return list;
    }
    
  }

}
