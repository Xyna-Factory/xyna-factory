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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.RootOrderSuspension;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionFailedAction;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionTimedOutAction;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


/**
 *
 */
public class SuspendOrder implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
    
    Params params = new Params(allArgs);
    if( params.isInvalid() ) {
      clw.writeLineToCommandLine( params.help());
      return;
    }
       
    SuspendRootOrderData suspendRootOrderData = params.suspendRootOrderData;
   
    StringBuilder sb = new StringBuilder();
    sb.append("suspending ").append(suspendRootOrderData.getRootOrderIds());
    long timeout = suspendRootOrderData.getTimeout();
    switch( suspendRootOrderData.getSuspensionTimedOutAction() ) {
      case None:
        sb.append(" and ");
        break;
      case Interrupt:
        timeout*=2;
        sb.append(" interrupt threads and ");
        break;
      case Stop:
        timeout*=3;
        sb.append(" interrupt threads, stop threads and ");
        break;
    }
    switch( suspendRootOrderData.getSuspensionFailedAction() ) {
      case KeepSuspending:
        sb.append("keep suspending after ");
        break;
      case StopSuspending:
        sb.append("stop suspension after ");
        break;
      case UndoSuspensions:
        sb.append("undo suspension after ");
        break;
    }
    sb.append("timeout ").append(timeout).append(" ms");
    clw.writeLineToCommandLine( sb.toString() );
     
    
    SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
     SuspendRootOrderData result = srm.suspendRootOrders(suspendRootOrderData);
        
    clw.writeLineToCommandLine( "Result: "+ result.getSuspensionResult());
    
    if( ! isEmpty( result.getResumeTargets() ) ) {
      if( result.getResumeTargets().size() <= 1 ) {
        clw.writeLineToCommandLine( "ResumeTargets: "+ result.getResumeTargets());
      } else {
        clw.writeLineToCommandLine( "ResumeTargets: ");
        for( ResumeTarget rt : result.getResumeTargets() ) {
          clw.writeLineToCommandLine( "\t" + rt );
        }
      }
    }
    if( ! isEmpty( result.getMIRedirections() ) ) {
      if( result.getMIRedirections().size() <= 1 ) {
        clw.writeLineToCommandLine( "MI-Redirection: "+ miRedirectionToString(result.getMIRedirections().get(0)) );
      } else {
        clw.writeLineToCommandLine( "MI-Redirection: ");
        for( Pair<Long,Long> pair : result.getMIRedirections() ) {
          clw.writeLineToCommandLine( "\t" + miRedirectionToString(pair) );
        }
      }
    }
    
    if( ! isEmpty( result.getFailedSuspensions() ) ) {
      if( result.getFailedSuspensions().size() <= 1 ) {
        clw.writeLineToCommandLine( "Failed: "+ failureToString( result.getFailedSuspensions().entrySet().iterator().next() ) );
      } else {
        clw.writeLineToCommandLine( "Failed:");
        for( Map.Entry<Long,String> entry : result.getFailedSuspensions().entrySet() ) {
          clw.writeLineToCommandLine( "\t" + failureToString(entry) );
        }
      }
    }
    if( ! isEmpty( result.getFailedResumes() ) ) {
      if( result.getFailedResumes().size() <= 1 ) {
        clw.writeLineToCommandLine( "Failed: "+ failureToString( result.getFailedResumes().get(0) ) );
      } else {
        clw.writeLineToCommandLine( "Failed:");
        for( Triple<RootOrderSuspension, String, PersistenceLayerException> triple : result.getFailedResumes() ) {
          clw.writeLineToCommandLine( "\t" + failureToString(triple) );
        }
      }
    }
  }


  private boolean isEmpty(List<?> list) {
    return list == null || list.isEmpty();
  }

  private boolean isEmpty(Map<?,?> map) {
    return map == null || map.isEmpty();
  }

  private String miRedirectionToString(Pair<Long, Long> pair) {
    return pair.getFirst()+" has MI-Redirection "+pair.getSecond();
  }

  private String failureToString(Entry<Long, String> entry) {
    return entry.getKey()+": "+entry.getValue();
  }

  private String failureToString(Triple<RootOrderSuspension, String, PersistenceLayerException> triple) {
    StringBuilder sb = new StringBuilder();
    sb.append(triple.getFirst().getRootOrderId()).append(":");
    if( triple.getSecond() != null ) {
      sb.append(" ").append(triple.getSecond());
    }
    if( triple.getThird() != null ) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter( sw );
      triple.getThird().printStackTrace(pw);
      sb.append(" ").append(sw.toString());
    }
    return sb.toString();
  }

  public static class CLISuspensionCause extends SuspensionCause {
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
      return "CLI";
    }
  }
  
  private static class Params {
    
    SuspendRootOrderData suspendRootOrderData;
    boolean invalid = false;
    
    public Params(AllArgs allArgs) {
      suspendRootOrderData = new SuspendRootOrderData();
      suspendRootOrderData.suspensionFailedAction(SuspensionFailedAction.StopSuspending);
      suspendRootOrderData.suspensionCause(new CLISuspensionCause());
      suspendRootOrderData.failFast(true);
      parse(allArgs);
    }

    public String help() {
      return "parameters are [-timeout <seconds>] [-timeoutaction <none|interrupt|stop>] "
          +"[-failedaction <undo|stop|keep>] <rootOrderId> [<rootOrderId> ...] "
          +" defaults are timeout from xynaproperty, timeoutaction none, failedaction stop";
    }

    public boolean isInvalid() {
      return invalid;
    }

    private void parse(AllArgs allArgs) {
      try {
        Map<String,String> argsMap = allArgs.parseArgsToMap();
        if( argsMap.containsKey("-timeout") ) {
          Long timeout = Long.valueOf( argsMap.get("-timeout") );
          suspendRootOrderData.timeout( timeout, TimeUnit.SECONDS);
        }
        suspendRootOrderData.suspensionTimedOutAction( toSuspensionTimedOutAction( argsMap.get("-timeoutaction") ) );
        suspendRootOrderData.suspensionFailedAction( toSuspensionFailedAction( argsMap.get("-failedaction") ) );
      } catch( RuntimeException e ) {
        invalid = true;
      }
    }

    private SuspensionTimedOutAction toSuspensionTimedOutAction(String string) {
      String val = string == null ? "" : string.toLowerCase();
      if( "none".equals(val) ) {
        return SuspensionTimedOutAction.None;
      } else if( "interrupt".equals(val) ) {
        return SuspensionTimedOutAction.Interrupt;
      } else if( "stop".equals(val) ) {
        return SuspensionTimedOutAction.Stop;
      } else {
        return SuspensionTimedOutAction.None;
      }
    }
    
    private SuspensionFailedAction toSuspensionFailedAction(String string) {
      String val = string == null ? "" : string.toLowerCase();
      if( "stop".equals(val) ) {
        return SuspensionFailedAction.StopSuspending;
      } else if( "keep".equals(val) ) {
        return SuspensionFailedAction.KeepSuspending;
      } else if( "undo".equals(val) ) {
        return SuspensionFailedAction.UndoSuspensions;
      } else {
        return SuspensionFailedAction.StopSuspending;
      }
    }
    
  }
  
}
