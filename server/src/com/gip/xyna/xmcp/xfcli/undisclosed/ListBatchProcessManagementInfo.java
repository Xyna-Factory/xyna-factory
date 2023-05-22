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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessManagementInformation;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRuntimeInformationStorable.BatchProcessState;


/**
 *
 */
public class ListBatchProcessManagementInfo implements CommandExecution {

   public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
     
     BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();
     
     BatchProcessManagementInformation bpmi = bpm.getBatchProcessManagementInformation();
     
     List<BatchProcessInformation> bpis = bpmi.getBatchProcessInformations();
     if( bpis == null || bpis.isEmpty() ) {
       clw.writeString("currently no batch process is running\n");
       return; 
     }
     int size = bpis.size();
     clw.writeString("currently "+bpis.size()+" batch process"+(size>1?"es are":" is")+" running\n");
     
     String indent = "\t";
     
     for( BatchProcessInformation bpi : bpis ) {
       BatchProcessRuntimeInformationStorable info = bpi.getRuntimeInformation();
       StringBuilder sb = new StringBuilder();
       sb.append(bpi.getBatchProcessId()).append(":").append(indent).append(bpi.getLabel()).append("\n");
       sb.append(indent).append(bpi.getBatchProcessStatus()).append(" (").
         append("batchProcessState=").append(info.getState()).append(", ").
         append("orderState=").append(bpi.getArchive().getOrderStatus()).append(", ").
         append("schedulingState=").append(bpi.getSchedulingState()).append(")\n");
       if (info.getState() == BatchProcessState.PAUSED) {
         sb.append(indent).append("pauseCause=").append(info.getPauseCause()).append("\n");
       }
       sb.append(indent).append("total=").append(bpi.getArchive().getTotal()).
          append(", finished=").append(info.getFinished()).
          append(", failed=").append(info.getFailed()).
          append(", running=").append(info.getRunning()).
          append(", canceled=").append(bpi.getCanceled()).
          append("\n");
       
       clw.writeString(sb.toString());
     }
     
  }
  
}
