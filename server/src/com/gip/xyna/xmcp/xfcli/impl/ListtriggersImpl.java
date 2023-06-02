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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.ComparatorUtils;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xact.trigger.TriggerInformation;
import com.gip.xyna.xact.trigger.TriggerInformation.TriggerInstanceInformation;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable.TriggerInstanceState;
import com.gip.xyna.xact.trigger.TriggerStorable.TriggerState;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListenerInstance;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.StringParameterFormatter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listtriggers;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class ListtriggersImpl extends XynaCommandImplementation<Listtriggers> {

  public void execute(OutputStream statusOutputStream, Listtriggers payload) throws XynaException{

    StringBuilder output = new StringBuilder();
    
    List<TriggerInformation> triggerinfo = factory.getActivationPortal().listTriggerInformation();
    if (triggerinfo.size() == 0) {
      writeToCommandLine(statusOutputStream, "No triggers registered at server.\n");
      return;
    }
    
    writeToCommandLine(statusOutputStream, "Listing deployment status information triggers...\n");

    Collections.sort(triggerinfo, new TISortByName(payload.getSortByRuntimeContext()) );
    
    if( payload.getOldFormat() ) {
      appendOldFormat( output, triggerinfo, payload);
    } else {
      if( ! payload.getInstancesOnly() ) {
        TriggerTableFormatter ttf = new TriggerTableFormatter(triggerinfo);
        ttf.writeTableHeader(output);
        ttf.writeTableRows(output);
      
        output.append("\n");
      }
      
      List<OrderedTriggerInstanceInformation> otiis = OrderedTriggerInstanceInformation.extractFromTriggers( triggerinfo );
      Collections.sort(otiis, new OTIISortByName(payload.getSortByRuntimeContext()) );
      
      TriggerInstanceTableFormatter titf = new TriggerInstanceTableFormatter(otiis);
      titf.writeTableHeader(output);
      titf.writeTableRows(output);
      
      if( payload.getVerbose() ) {
        output.append("\n");
        appendTriggerStartParameter(output, triggerinfo);
      }
      
      if( payload.getInstanceStartParameter() ) {
        output.append("\n");
        TriggerInstanceStartParameterTableFormatter tisptf = new TriggerInstanceStartParameterTableFormatter(triggerinfo);
        tisptf.writeTableHeader(output);
        tisptf.writeTableRows(output);
      }
      
      if( payload.getError() ) {
        output.append("\n");
        appendErrors( output, triggerinfo, otiis);
      }

    }
    writeToCommandLine(statusOutputStream, output.toString());
  }


  private void appendErrors(StringBuilder output, List<TriggerInformation> triggerinfo, List<OrderedTriggerInstanceInformation> triggerInstances) {
    int triggerIndex = 0;
    int triggerInstanceIndex = 0;
    for (TriggerInformation ti : triggerinfo) {
      ++triggerIndex;
      if( ti.getErrorCause() != null) {
        output.append("\nTrigger ").
              append( TriggerTableFormatter.TriggerColumn.Trigger.extract(ti) ).
              append(" (").append(triggerIndex).append(")").
              append("\n");
        output.append("    errorCause: ").append(ti.getErrorCause());
      }
    }
    for( OrderedTriggerInstanceInformation otii : triggerInstances ) {
      ++triggerInstanceIndex;
      if( otii.tii.getErrorCause() != null) {
        output.append("\nTrigger instance ").
        append( otii.getTriggerInstanceName() ).
        append(" (").append(triggerInstanceIndex).append(")").
        append("\n");
        output.append("    errorCause: ").append(otii.tii.getErrorCause());
      }
    }
  }


  private void appendOldFormat(StringBuilder output, List<TriggerInformation> triggerinfo, Listtriggers payload) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    
    for (TriggerInformation triggerInformation : triggerinfo) {
      output.append(GenerationBase.getSimpleNameFromFQName(triggerInformation.getFqTriggerClassName()));
      
      if(triggerInformation.getRuntimeContext() instanceof Application) {
        output.append(" (Applicationname: ");
        output.append(triggerInformation.getApplicationName());
        output.append(", Versionname: ");
        output.append(triggerInformation.getVersionName());
        output.append(")");
      }
      
      if(triggerInformation.getRuntimeContext() instanceof Workspace 
                      && !triggerInformation.getRuntimeContext().equals(RevisionManagement.DEFAULT_WORKSPACE)) {
        output.append(" (Workspacename: ");
        output.append(triggerInformation.getRuntimeContext().getName());
        output.append(")");
      }
      
      output.append(" named <").append(triggerInformation.getTriggerName()).append(">: ");
      output.append(triggerInformation.getDescription());
      output.append("\n");
      
      String status;
      if (triggerInformation.getTriggerState() == null) {
        status = TriggerState.OK.toString();
      } else {
        status = triggerInformation.getTriggerState().toString();
      }
      output.append("  status: " + status +"\n");
      if (payload.getError()) {
        if (triggerInformation.getErrorCause() != null) {
          output.append("    errorCause: ").append(triggerInformation.getErrorCause());
        }
      }
      
      if (payload.getVerbose() ) {
        appendTriggerStartParameter(output, triggerInformation);
      }
      
      List<TriggerInstanceInformation> triggerInstances = triggerInformation.getTriggerInstances();
      
      Long revision = revisionManagement.getRevision(triggerInformation.getRuntimeContext());
      appendTriggerInstances(output, triggerInstances, revision, payload.getError());
    }
    
  }

  private void appendTriggerStartParameter(StringBuilder output, List<TriggerInformation> triggerinfo) {
    HashMap<String, String> duplicateTrigger = new HashMap<String, String>();
    
    int triggerId = 0;
    for (TriggerInformation triggerInformation : triggerinfo) {
      ++triggerId;
      String triggerName = GenerationBase.getSimpleNameFromFQName(triggerInformation.getFqTriggerClassName());
      String triggerIdString = triggerName+" (Id "+triggerId+")";
      
      StringBuilder startParameter = new StringBuilder(); 
      appendTriggerStartParameter(startParameter, triggerInformation);
      
      String duplicateId = duplicateTrigger.get(startParameter.toString());
      if( duplicateId == null ) {
        duplicateTrigger.put(startParameter.toString(), triggerIdString );
      }
      
       if( duplicateId != null ) {
        output.append(triggerIdString).append(" has same start parameter as ").append(duplicateId).append("\n");
      } else {
        output.append(triggerIdString).append("\n").append(startParameter.toString());
      }
    }
  }

  private void appendTriggerStartParameter(StringBuilder output, TriggerInformation triggerInformation) {
    if( triggerInformation.getEnhancedStartParameter() != null ) {
      output.append("  possible start parameter as key/value pairs (key=value):\n");
      for( StringParameter<?> sp : triggerInformation.getEnhancedStartParameter() ) {
        output.append("    - ");
        StringParameterFormatter.appendStringParameter( output, sp );
        output.append("\n");
      }
    } else if (triggerInformation.getStartParameterDocumentation() != null) {
      output.append("  possible start parameter combinations:\n");
      int j = 0;
      for (String[] paras : triggerInformation.getStartParameterDocumentation()) {
        j++;
        output.append("    " + j + ". possible parameter combination:\n");
        for (int i = 0; i < paras.length; i++) {
          output.append("       - ").append(paras[i]).append("\n");
        }
      }
    } else {
      output.append("  start parameter descriptions: not available\n");
    } 
  }
  

  private void appendTriggerInstances(StringBuilder output, List<TriggerInstanceInformation> triggerInstances, long revision, boolean printError) {
    if (triggerInstances.size() > 0) {
      output.append("  Instances:\n");
      for (TriggerInstanceInformation triggerInstance : triggerInstances) {
        output.append("    - ")
              .append(triggerInstance.getTriggerInstanceName());
        if (triggerInstance.getRevision() != revision) {
          RuntimeContext rc;
          try {
            rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(triggerInstance.getRevision());
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            throw new RuntimeException(e);
          }
          output.append(" (Context=").append(rc).append(")");
        }
        if (triggerInstance.getState() == TriggerInstanceState.ENABLED) {
          EventListenerInstance eli =
                          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
                              .getEventListenerByName(triggerInstance.getTriggerInstanceName(), triggerInstance.getRevision());
          if (eli != null) {
            output.append(" [" + triggerInstance.getState() + "]");
          } else {
            output.append(" [" + triggerInstance.getState() + ", NOT RUNNING]");
          }
        } else {
          output.append(" [" + triggerInstance.getState() + "]");
        }
        
        if (triggerInstance.getStartParameter().size() > 0) {
          output.append(" (");
          Iterator<String> parmIter = triggerInstance.getStartParameter().listIterator();
          while (parmIter.hasNext()) {
            output.append(parmIter.next());
            if (parmIter.hasNext()) {
              output.append(", ");
            }
          }
          output.append(")");
          if ( ( triggerInstance.getDescription() != null ) && ( triggerInstance.getDescription().length() > 0 ) ) {
            output.append(": ")
                  .append(triggerInstance.getDescription());
          }
        }
        
        output.append("\n");
        
        if (printError) {
          if (triggerInstance.getErrorCause() != null) {
            output.append("        errorCause: ").append(triggerInstance.getErrorCause());
          }
        }
      }
    }
  }
  

  private static class OrderedTriggerInstanceInformation {
    private TriggerInstanceInformation tii;
    private int triggerId;
    private RuntimeContext rc;
    
    public OrderedTriggerInstanceInformation(TriggerInstanceInformation tii, int triggerId, RuntimeContext rc) {
      this.tii = tii;
      this.triggerId = triggerId;
      this.rc = rc;
    }

    public RuntimeContext getRuntimeContext() {
      return rc;
    }

    public String getTriggerInstanceName() {
      return tii.getTriggerInstanceName();
    }
    
    public static List<OrderedTriggerInstanceInformation> extractFromTriggers( List<TriggerInformation> triggers) {
      List<OrderedTriggerInstanceInformation> otiis = new ArrayList<OrderedTriggerInstanceInformation>();
      RevisionManagement revisionManagement =  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      int triggerId = 0; 
      for( TriggerInformation ti : triggers ) {
        ++triggerId;
        List<TriggerInstanceInformation> triggerInstances = ti.getTriggerInstances();
        if( triggerInstances != null && triggerInstances.size() > 0 ) {
          for( TriggerInstanceInformation tii : triggerInstances ) {
            RuntimeContext rc;
            try {
              rc = revisionManagement.getRuntimeContext(tii.getRevision());
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              rc = null; //TODO Fehler ausgeben?
            }
            otiis.add( new OrderedTriggerInstanceInformation(tii, triggerId, rc) );
          }
        }
      }
      return otiis;
    }

  } 
  
  private static class TISortByName implements Comparator<TriggerInformation> {

    private boolean sortByRuntimeContext;

    public TISortByName( boolean sortByRuntimeContext ) {
      this.sortByRuntimeContext = sortByRuntimeContext;
    }
    
    public int compare(TriggerInformation o1, TriggerInformation o2) {
      int comp = 0;
      if( sortByRuntimeContext ) {
        comp = ComparatorUtils.compareNullAware(o1.getRuntimeContext(), o2.getRuntimeContext(), false);
      } else {
        comp = ComparatorUtils.compareNullAware(o1.getTriggerName(), o2.getTriggerName(), false);
      }
      if( comp == 0 ) {
        if( sortByRuntimeContext ) {
          comp = ComparatorUtils.compareNullAware(o1.getTriggerName(), o2.getTriggerName(), false);
        } else {
          comp = ComparatorUtils.compareNullAware(o1.getRuntimeContext(), o2.getRuntimeContext(), false);
        }
      }
      return comp;
    }
  }
  
  private static class OTIISortByName implements Comparator<OrderedTriggerInstanceInformation> {
    private boolean sortByRuntimeContext;

    public OTIISortByName( boolean sortByRuntimeContext ) {
      this.sortByRuntimeContext = sortByRuntimeContext;
    }
    
    public int compare(OrderedTriggerInstanceInformation o1, OrderedTriggerInstanceInformation o2) {
      int comp = 0;
      if( sortByRuntimeContext ) {
        comp = ComparatorUtils.compareNullAware(o1.getRuntimeContext(), o2.getRuntimeContext(), false);
      } else {
        comp = ComparatorUtils.compareNullAware(o1.getTriggerInstanceName(), o2.getTriggerInstanceName(), false);
      }
      if( comp == 0 ) {
        if( sortByRuntimeContext ) {
          comp = ComparatorUtils.compareNullAware(o1.getTriggerInstanceName(), o2.getTriggerInstanceName(), false);
        } else {
          comp = ComparatorUtils.compareNullAware(o1.getRuntimeContext(), o2.getRuntimeContext(), false);
        }
      }
      return comp;
    }
  }

  
  
  private static class TriggerTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<TriggerColumn> columns;
    
    public TriggerTableFormatter(List<TriggerInformation> triggers) {
      columns = Arrays.asList(TriggerColumn.values());
      
      header = new ArrayList<String>();
      header.add( "Id" );
      for( TriggerColumn tc : columns ) {
        header.add( tc.toString() );
      }
      
      generateRows(triggers);
    }
    

    private void generateRows(List<TriggerInformation> triggers) {
      rows = new ArrayList<List<String>>();
      int triggerId =0;
      for( TriggerInformation ti : triggers ) {
        ++triggerId;
        List<String> row = new ArrayList<String>();
        row.add( String.valueOf(triggerId) );
        for( TriggerColumn tc : columns ) {
          row.add( tc.extract(ti) );
        }
        rows.add( row );
      }
    }
    
    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    @Override
    public List<String> getHeader() {
      return header;
    }

    
    private enum TriggerColumn {
      
      Trigger {
        public String extract(TriggerInformation ti) {
          return GenerationBase.getSimpleNameFromFQName(ti.getFqTriggerClassName());
        }
      },
      Name {
        public String extract(TriggerInformation ti) {
          return ti.getTriggerName();
        }
      },
      Description {
        public String extract(TriggerInformation ti) {
          return ti.getDescription();
        }
      },
      RuntimeContext {
        public String extract(TriggerInformation ti) {
          RuntimeContext rc = ti.getRuntimeContext();
          return rc.toString();
        }
      },
      Status {
        public String extract(TriggerInformation ti) {
          String status;
          if (ti.getTriggerState() == null) {
            status = TriggerState.OK.toString();
          } else {
            status = ti.getTriggerState().toString();
          }
          return status;
        }
      },
      Instances {
        public String extract(TriggerInformation ti) {
          return ti.getTriggerInstances() == null ? "0" : String.valueOf(ti.getTriggerInstances().size());
        }
      }
      ;
      public abstract String extract(TriggerInformation ti);
    }
  }


  private static class TriggerInstanceTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<TriggerInstanceColumn> columns;

    public TriggerInstanceTableFormatter(List<OrderedTriggerInstanceInformation> triggerInstances) {
      columns = Arrays.asList(TriggerInstanceColumn.values());

      header = new ArrayList<String>();
      header.add("Id");
      for( TriggerInstanceColumn tc : columns ) {
        header.add( tc.toString() );
      }

      generateRows(triggerInstances);
    }

    private void generateRows(List<OrderedTriggerInstanceInformation> triggerInstances) {
      rows = new ArrayList<List<String>>();
      int triggerInstanceId = 0;
      for( OrderedTriggerInstanceInformation otii : triggerInstances ) {
        ++triggerInstanceId;
        List<String> row = new ArrayList<String>();
        row.add( String.valueOf(triggerInstanceId) );
        for( TriggerInstanceColumn tc : columns ) {
          row.add( tc.extract(otii) );
        }
        rows.add( row );
      }
    }


    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    @Override
    public List<String> getHeader() {
      return header;
    }


    private enum TriggerInstanceColumn {
      Instance {
        public String extract(OrderedTriggerInstanceInformation otii) {
          return otii.tii.getTriggerInstanceName();
        }
      },
      Trigger {
        public String extract(OrderedTriggerInstanceInformation otii) {
          return "("+otii.triggerId+") " + otii.tii.getTriggerName();
        }
      },
      Description {
        public String extract(OrderedTriggerInstanceInformation otii) {
          return otii.tii.getDescription();
        }
      },
      RuntimeContext {
        public String extract(OrderedTriggerInstanceInformation otii) {
          return String.valueOf( otii.rc );
        }
      },
      Status {
        public String extract(OrderedTriggerInstanceInformation otii) {
          TriggerInstanceState state = otii.tii.getState();
          if (state == TriggerInstanceState.ENABLED) {
            EventListenerInstance<?,?> eli =
                XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution()
                .getEventListenerByName(otii.tii.getTriggerInstanceName(), otii.tii.getRevision());
            if (eli != null) {
              return state.toString();
            } else {
              return state.toString()+", NOT RUNNING";
            }
          } else {
            return state.toString();
          }
        }
      };

      public abstract String extract(OrderedTriggerInstanceInformation otii);
    }

  }

  private static class TriggerInstanceStartParameterTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;

    public TriggerInstanceStartParameterTableFormatter(List<TriggerInformation> triggers) {
      header = new ArrayList<String>();
      header.add( "Id" );
      header.add( "Instance" );
      header.add( "StartParameter" );
      generateRows(triggers);
    }


    private void generateRows(List<TriggerInformation> triggers) {
      rows = new ArrayList<List<String>>();
      int triggerInstanceId = 0;
      for( TriggerInformation ti : triggers ) {
        List<TriggerInstanceInformation> triggerInstances = ti.getTriggerInstances();
        if( triggerInstances != null && triggerInstances.size() > 0 ) {
          for( TriggerInstanceInformation tii : triggerInstances ) {
            ++triggerInstanceId;
            List<String> row = new ArrayList<String>();
            row.add(String.valueOf(triggerInstanceId) );
            row.add( tii.getTriggerInstanceName() );

            StringBuilder sb = new StringBuilder();
            String sep = "";
            for( String s : tii.getStartParameter() ) {
              sb.append(sep).append(s);
              sep = ", ";
            }
            row.add( sb.toString() );
            rows.add( row );
          }
        }
      }
    }

    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    @Override
    public List<String> getHeader() {
      return header;
    }

  }

}