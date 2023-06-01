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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listorderseriesinfo;
import com.gip.xyna.xprc.xsched.orderseries.OSMTaskConsumer;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation.TaskConsumerState;
import com.gip.xyna.xprc.xsched.orderseries.OrderSeriesManagementInformation.WaitingOrder;



public class ListorderseriesinfoImpl extends XynaCommandImplementation<Listorderseriesinfo> {

  public void execute(OutputStream statusOutputStream, Listorderseriesinfo payload) throws XynaException {
    OrderSeriesManagementInformation.Mode mode = OrderSeriesManagementInformation.Mode.Basic;
    if( payload.getVerbose() ) {
      mode = OrderSeriesManagementInformation.Mode.Orders;
    }
    if( payload.getExtraverbose() ) {
      mode = OrderSeriesManagementInformation.Mode.Predecessors;
    }
    
    OrderSeriesManagementInformation output = null;
    if(factory != null && factory.getXynaMultiChannelPortalPortal() != null ) {
      output = factory.getXynaMultiChannelPortalPortal().listOrderSeriesManagementInformation(mode);
    }
    if (output == null) {
      writeLineToCommandLine(statusOutputStream, "No information available.");
      return;
    }
    
    writeLineToCommandLine(statusOutputStream,
      output.getWaitingOrders() + " orders are waiting in the order series management for predecessors.");
    if( output.getReadyOrders() != 0 ) {
      writeLineToCommandLine(statusOutputStream,
        output.getReadyOrders() + " orders are waiting in the order series management to be prescheduled.");
    }
    
    writeLineToCommandLine(statusOutputStream,
      output.getCacheSize() + " seriesInformationStorables are cached, "
      +output.getPredecessorTreesSize() + " predecessors are cached and "
      +output.getCurrentTasks()+" task are waiting to be executed"
      +(output.getTaskConsumerState() != TaskConsumerState.Finished ?"": " but taskConsumer is not running")
      +"." );
    
    
    if( output.getTasksCount() != null ) {
      List<Pair<String, Integer>> tasks = output.getTasksCount();
       if( tasks.isEmpty() ) {
        writeLineToCommandLine(statusOutputStream,"Processed OSMTasks: none" );
      } else {
        Collections.sort( tasks, Collections.reverseOrder( Pair.<String,Integer>comparator(false) ) );
        StringBuilder sb = new StringBuilder();
        String sep = "Processed OSMTasks: ";
        for( Pair<String,Integer> pair : tasks ) {
          sb.append(sep);
          sep = "; ";
          sb.append(pair.getFirst()).append(": ").append(pair.getSecond());
        }
        writeLineToCommandLine(statusOutputStream, sb.toString() );
      }
    }
    if( output.getFailedTasks() != null ) {
      int size = output.getFailedTasks().size();
      if( size != 0 ) {
        boolean maxed = size == OSMTaskConsumer.MAX_FAILED_TASKS;
        
        writeLineToCommandLine(statusOutputStream, "Tasks are failed for the "
          + (maxed?"last "+size:"" )
          + " correlationIds "+output.getFailedTasks()
        );
      }
    }
    
    
    if( output.getWaitingOrderList() != null ) {
      for( WaitingOrder wo : output.getWaitingOrderList() ) {
        writeLineToCommandLine(statusOutputStream, writeWaitingOrder( wo ) );
      }
    }
  }

  /**
   * @param wo
   * @return
   */
  private String writeWaitingOrder(WaitingOrder wo) {
    if( wo.getCorrelationId() == null ) {
      return String.valueOf( wo.getId() );
    }
    StringBuilder sb = new StringBuilder(50);
    sb.append( wo.getId() ).append( "(").append( wo.getCorrelationId() ).append(")");
    //has predecessor");
    int binding = wo.getBinding();
    if( wo.getPredecessors() != null ) {
      String sep = " has predecessor: ";
      for( WaitingOrder pre : wo.getPredecessors() ) {
        sb.append(sep);
        appendPredecessor(sb, pre, binding );
        sep = ", ";
      }
    }
    return sb.toString();
  }

  /**
   * @param sb
   * @param pre
   */
  private void appendPredecessor(StringBuilder sb, WaitingOrder pre, int binding) {
    sb.append( pre.getCorrelationId() );
    if( pre.getId() != WaitingOrder.UNKNOWN_ID ) {
      sb.append(" with id ").append(pre.getId());
      if( pre.getBinding() != binding ) {
        sb.append(" on other node");
      }
    }
    
  }
  
  

}
