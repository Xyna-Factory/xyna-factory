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
import java.io.PrintStream;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listschedulerinfo;
import com.gip.xyna.xprc.XynaOrderInfo;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean.HistogramColumn;
import com.gip.xyna.xprc.xsched.scheduling.SchedulerInformationBean.Mode;



public class ListschedulerinfoImpl extends XynaCommandImplementation<Listschedulerinfo> {
  
  public void execute(OutputStream statusOutputStream, Listschedulerinfo payload) throws XynaException {

    SchedulerInformationBean.Mode mode = SchedulerInformationBean.Mode.Basic;
    if( payload.getVerbose() ) {
      mode = SchedulerInformationBean.Mode.Orders;
    }
    if( payload.getExtraverbose() ) {
      mode = SchedulerInformationBean.Mode.Consistent;
    }
    if( payload.getHistogram() ) {
      mode = SchedulerInformationBean.Mode.Histogram;
    }
    
    
    SchedulerInformationBean output = null;
    if(factory != null && factory.getXynaMultiChannelPortalPortal() != null ) {
      output = factory.getXynaMultiChannelPortalPortal().listSchedulerInformation(mode);
    }
    if (output == null) {
      writeLineToCommandLine(statusOutputStream, "No information available.");
      return;
    }
    
    if( mode == SchedulerInformationBean.Mode.Histogram ) {
      histogramOutput( statusOutputStream, mode, output );
    } else {
      normalOutput( statusOutputStream, mode, output );
    }
  }

  private void normalOutput(OutputStream statusOutputStream, SchedulerInformationBean.Mode mode, SchedulerInformationBean output) {
   
    int waiting = output.getWaitingForCapacity() + output.getWaitingForVeto() + output.getWaitingForUnknown();
    int newOrders = output.getCountOfOrdersInScheduler() -waiting;
    if( mode != SchedulerInformationBean.Mode.Consistent && newOrders < 0  ) {
      newOrders = 0;
    }

    writeLineToCommandLine(statusOutputStream,
      waiting + " orders are waiting in the scheduler "+
                      "(cap=" + output.getWaitingForCapacity()+
                      ", veto="+output.getWaitingForVeto()+
                      ", other="+output.getWaitingForUnknown()+")"+
                      ", and "+newOrders+" orders are new.");

    if( output.isSchedulerAlive() ) {
      StringBuilder sb = new StringBuilder();
      sb.append("Scheduler state is: ");
      sb.append(output.getSchedulerStatus()).append("; ");
      sb.append("scheduler thread is currently ").
      append(output.isCurrentlyScheduling()?"scheduling":"sleeping");
      writeLineToCommandLine(statusOutputStream, sb.toString() );
    } else {
      writeLineToCommandLine(statusOutputStream, "Scheduler state is: NO SCHEDULER THREAD!");
      long timestamp = output.getThreadDeathTimestamp();
      if( timestamp != 0 ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread died at ").append(Constants.defaultUTCSimpleDateFormat().format(new Date(timestamp)));
        Throwable cause = output.getThreadDeathCause();
        if (cause != null) {
          sb.append(" due to ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage())
              .append("\n");
        }
        writeLineToCommandLine(statusOutputStream, sb.toString() );
        if( cause != null ) {
          cause.printStackTrace( new PrintStream(statusOutputStream) );
        }
      }
    }

    writeLineToCommandLine(statusOutputStream, "Last scheduling:");
    writeLineToCommandLine(statusOutputStream,
      "\tstarted:  " + Constants.defaultUTCSimpleDateFormat().format( new Date(output.getLastScheduled())));
    writeLineToCommandLine(statusOutputStream, "\tduration: " + (output.getLastSchedulingTook() / 1000.0)
      + " seconds");
    writeLineToCommandLine(statusOutputStream, "\titerated over "+output.getLastIteratedOrders()+" and scheduled "+output.getLastScheduledOrders()+" orders");
    if( output.getLastTransportedCaps() != -1 ) {
      writeLineToCommandLine(statusOutputStream, "\ttransported "+output.getLastTransportedCaps()+" capacities to other node"); 
    }
    writeLineToCommandLine(statusOutputStream, "Overall number of scheduler runs since factory startup: "
                      + output.getTotalSchedulerRuns());
    writeLineToCommandLine(statusOutputStream, "\tin the last  5 minutes: " + output.getSchedulerRunsLast5Minutes());
    writeLineToCommandLine(statusOutputStream, "\tin the last 60 minutes: " + output.getSchedulerRunsLast60Minutes());

    if( mode != SchedulerInformationBean.Mode.Basic ) {
      if (output.getOrdersInScheduler() != null && output.getOrdersInScheduler().size() > 0) {
        writeLineToCommandLine(statusOutputStream, "Listing orders in scheduler:");

        for (XynaOrderInfo xoInfo : output.getOrdersInScheduler()) {
          appendXynaOrderInfo( statusOutputStream, xoInfo );
        }
      } else {
        writeLineToCommandLine(statusOutputStream, "No waiting orders to be listed.");
      }
    }

  }
  
  private void appendXynaOrderInfo(OutputStream statusOutputStream, XynaOrderInfo xoInfo) {
    String message = "\tID " + xoInfo.getOrderId() + " "+xoInfo.getDestinationKey().serializeToString();
    writeLineToCommandLine(statusOutputStream, message);
  }

  private void histogramOutput(OutputStream statusOutputStream, Mode mode, SchedulerInformationBean output) {
    StringBuilder sb = new StringBuilder();
    
    String sep = "#";
    for( HistogramColumn hc : HistogramColumn.values() ) {
      sb.append(sep).append(hc.name());
      if(hc.isRate() ) {
        sb.append("/second");
      }
      sep = " ";
    }
    
    //Histogramm-Reihenfolgen umdrehen, damit Ausgabe aufsteigend
    List<EnumMap<HistogramColumn, Number>> histogram = output.getHistogram();
    Collections.reverse(histogram);
  
    //Histogramm ausgeben
    appendHistogram( sb, histogram );
    sb.append("\n");
    
    writeLineToCommandLine(statusOutputStream, sb.toString());  
  }

  private void appendHistogram(StringBuilder sb, List<EnumMap<HistogramColumn, Number>> histogram60Min) {
    String sep;
    for( EnumMap<HistogramColumn, Number> row : histogram60Min ) {
      sep = "\n";
      Number width = row.get(HistogramColumn.Width);
      for( HistogramColumn hc : HistogramColumn.values() ) {
        if( hc.isValue() ) {
          sb.append(sep).append( hc.normalize( row.get(hc), width ) );
        } else {
          sb.append(sep).append( row.get(hc) );
        }
        sep = " ";
      }
    }
  }

  
}
