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

import java.util.Arrays;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xprc.xsched.VetoManagement;
import com.gip.xyna.xprc.xsched.vetos.VM_SeparateThread;
import com.gip.xyna.xprc.xsched.vetos.VetoManagementAlgorithmType;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache;


public class ListVetoCache implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
    
    VetoManagement vm = XynaFactory.getInstance().getProcessing().getXynaScheduler().getVetoManagement();
    if( vm.getAlgorithmType() != VetoManagementAlgorithmType.SeparateThread ) {
      clw.writeLineToCommandLine( "Algorithm "+vm.getAlgorithmType()+" does not use VetoCache.");
      return;
    }
    
    VM_SeparateThread vmAlgorithm = (VM_SeparateThread)vm.getVMAlgorithm(); 
    
    
    VetoCache vc = vmAlgorithm.getVetoCache();
    int size = vc.size();
    if( size == 0 ) {
      clw.writeLineToCommandLine( "No entries in vetoCache" );
      return;
    }
    
    VetoCacheTableFormatter vctf = new VetoCacheTableFormatter(vc);
    clw.writeLineToCommandLine( vctf.getRows().size() +" entries in vetoCache" );
    
    StringBuilder output = new StringBuilder();
    vctf.writeTableHeader(output);
    vctf.writeTableRows(output);
    clw.writeLineToCommandLine( output.toString() );
    
    
    
  }
  
  private static class VetoCacheTableFormatter extends TableFormatter {

    private List<List<String>> vcData;
    
    public VetoCacheTableFormatter(VetoCache vc) {
      vcData = vc.exportVetoCacheData();
    }

    @Override
    public List<List<String>> getRows() {
      return vcData;
    }

    @Override
    public List<String> getHeader() {
      return Arrays.asList("name", "status", "orderId", "binding", "orderType", "urgency", "waiting", "history");
    }
    
  }
  
}
