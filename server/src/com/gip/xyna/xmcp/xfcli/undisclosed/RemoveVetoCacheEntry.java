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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xprc.xsched.VetoManagement;
import com.gip.xyna.xprc.xsched.vetos.VM_SeparateThread;
import com.gip.xyna.xprc.xsched.vetos.VetoManagementAlgorithmType;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheEntry;


public class RemoveVetoCacheEntry implements CommandExecution {

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
    if( allArgs.getArgCount() == 0  ) {
      return;
    }
    List<String> vetos = new ArrayList<>(allArgs.getArgs());
    String first = vetos.get(0);
    boolean force = false;
    if( "help".equals(first) ) {
      clw.writeLineToCommandLine( "Usage: ./xynafactory.sh removevetocacheentry [-f] <vetoName1> [<vetoName2>...]" );
      clw.writeLineToCommandLine("Flag -f removes veto in all states, can do damage to VetoManagement.");
      clw.writeLineToCommandLine("Without -f vetos are removed in safe state [Remote] only.");
      return;
    } else if( "-f".equals(first) ) {
      force = true;
      vetos.remove(0);
    }
    
    int removed = 0;
    for( String vetoName : vetos ) {
      VetoCacheEntry veto = vc.get(vetoName);
      if( veto == null ) {
        clw.writeLineToCommandLine( "Veto "+vetoName +" does not exist");
      } else {
        boolean success = false;
        StringBuilder sb = new StringBuilder();
        sb.append("Veto ").append(veto);
        try {
          if( force ) {
            vc.remove(veto, veto.getState());
          } else {
            vc.removePassiveVeto(veto);
          }
          veto = vc.get(vetoName);
          success = veto == null;
        } finally {
          if( success ) {
            ++removed;
            sb.append(" removed ").append((force?" forcefully":""));
          } else {
            sb.append(" not removed ").append((force?" forcefully":"")).append(" -> ").append(veto);
          }
          clw.writeLineToCommandLine( sb.toString() );
        }
      }
    }
    if( removed > 1 ) {
      clw.writeLineToCommandLine( "Removed "+removed+" vetos" );
    }
    if( removed > 0 ) {
      //Notify, damit Scheduler Veto erneut anfordern kann
      XynaFactory.getInstance().getProcessing().getXynaScheduler().notifyScheduler();
    }
  }
  
  
}
