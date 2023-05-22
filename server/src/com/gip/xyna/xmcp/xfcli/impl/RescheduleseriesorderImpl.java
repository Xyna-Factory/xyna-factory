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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Rescheduleseriesorder;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation.Problem;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation.Solution;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable.OrderStatus;



public class RescheduleseriesorderImpl extends XynaCommandImplementation<Rescheduleseriesorder> {

  public void execute(OutputStream statusOutputStream, Rescheduleseriesorder payload) throws XynaException {
    
    long orderId = Long.parseLong( payload.getOrderId() );
    boolean force = payload.getForce();
    
    RescheduleSeriesOrderInformation output = null;
    if(factory != null && factory.getXynaMultiChannelPortalPortal() != null ) {
      output = factory.getXynaMultiChannelPortalPortal().rescheduleSeriesOrder( orderId, force );
    } else {
      writeLineToCommandLine(statusOutputStream, "No factory available.");
      return;
    }
    
    if( output == null ) {
      writeLineToCommandLine(statusOutputStream, "No information available.");
      return;
    }
    
    OrderStatus status = output.getOrderStatus();
    if( status != null ) {
      writeLineToCommandLine(statusOutputStream, "Order "+ payload.getOrderId()+" was found in state "+status+"." );
    } else {
      writeLineToCommandLine(statusOutputStream, "Order "+ payload.getOrderId()+" was not found." );
    }
    
    Problem problem = output.getProblem();
    if( problem != null ) {
      writeProblemToCommandLine( statusOutputStream, problem, output );
    } else {
      Solution solution = output.getSolution();
      writeSolutionToCommandLine( statusOutputStream, solution, output );
    }
        
  }

  /**
   * @param statusOutputStream
   * @param solution
   */
  private void writeSolutionToCommandLine(OutputStream statusOutputStream, Solution solution, RescheduleSeriesOrderInformation output) {
    String comment = output.getComment();
    if( solution == null ) {
      if( comment == null ) {
        writeLineToCommandLine(statusOutputStream, "No solution found." );
      } else {
        writeLineToCommandLine(statusOutputStream, comment );
      }
      return; 
    }
    switch( solution ) {
      case Disappeared:
        writeLineToCommandLine(statusOutputStream, "Problem seems to have disappeared." );
        break;
      case OrderFinished:
        writeLineToCommandLine(statusOutputStream, "Order could be finished." );
        break;
      case OrderStarted:
        writeLineToCommandLine(statusOutputStream, "Order could be started." );
        break;
      case None:
        if( comment == null ) {
          writeLineToCommandLine(statusOutputStream, "No solution possible." );
        } else {
          writeLineToCommandLine(statusOutputStream, "No solution possible: "+comment );
        }
        break;
      case SuccessorsStarted:
        writeLineToCommandLine(statusOutputStream, "Successors started." );
        break;
      case MissingSeriesInformationInserted:
        writeLineToCommandLine(statusOutputStream, "Missing "+comment+" was inserted." );
        break;
      case RemovedFromOSM:
        writeLineToCommandLine(statusOutputStream, "Order is removed from OrderSeriesManagement." );
        break;
      case NoProblem:
        writeLineToCommandLine(statusOutputStream, "This seems to be no problem at all." );
        break;
      default:
        writeLineToCommandLine(statusOutputStream, "Solution: "+solution );
    }
  }

  /**
   * @param statusOutputStream
   * @param problem
   */
  private void writeProblemToCommandLine(OutputStream statusOutputStream, Problem problem, RescheduleSeriesOrderInformation output) {
    String comment = output.getComment();
    switch( problem ) {
      case MissingSeriesInformation:
        writeLineToCommandLine(statusOutputStream, "No SeriesInformation found.");
        writeLineToCommandLine(statusOutputStream, "Try \"force\" to create new SeriesInformation. Warning: new SeriesInformation may not have predecessor/successor-information.");
        writeLineToCommandLine(statusOutputStream, "rescheduleseriesorder has to be called again to start order." ); 
        break;
      case MissingSeriesInformation_UnreadableXynaOrder:
        writeLineToCommandLine(statusOutputStream, "No SeriesInformation and XynaOrder found.");
        writeLineToCommandLine(statusOutputStream, "Try \"force\" to remove from OrderSeriesManagement.");
        break;
      case DuplicateCorrelationId:
        writeLineToCommandLine(statusOutputStream, "Waiting order has duplicate correlationId." ); 
        break;
      case OrderNotFound:
        writeLineToCommandLine(statusOutputStream, "No information in OrderArchive found, order is not waiting in OrderSeriesManagement.");
        writeLineToCommandLine(statusOutputStream, "Try \"force\" to behave as order were succeeded.");
        break;
      case UnexpectedOrderState:
        writeLineToCommandLine(statusOutputStream, "Order in OrderArchive has unexpected orderState.");
        break;
      case MissingPredecessors:
        writeLineToCommandLine(statusOutputStream, comment+" predecessors are missing.");
        writeLineToCommandLine(statusOutputStream, "Try \"force\" to start order anyway.");
        break;
      case WaitingPredecessors:
        writeLineToCommandLine(statusOutputStream, comment+" predecessors are waiting.");
        writeLineToCommandLine(statusOutputStream, "Call rescheduleseriesorder for predecessors or try \"force\" to start order anyway.");
        break;
      case OrderSeemsRunning:
        writeLineToCommandLine(statusOutputStream, "Order seems to be running now.");
        break;
      case OrderAlreadyFinished:
        writeLineToCommandLine(statusOutputStream, "Order is already finished.");
        break;
      case OtherBinding:
        writeLineToCommandLine(statusOutputStream, "Order belongs to other binding.");
        writeLineToCommandLine(statusOutputStream, "Call rescheduleseriesorder on other node.");
        break;
      case Unimplemented:
        writeLineToCommandLine(statusOutputStream, "No Solution implemented.");
        break;
      default:
        writeLineToCommandLine(statusOutputStream, "Problem: "+problem ); 
    }
    
  }

}
