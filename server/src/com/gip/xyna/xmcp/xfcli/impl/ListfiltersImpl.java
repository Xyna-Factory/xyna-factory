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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.ComparatorUtils;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xact.trigger.FilterInformation;
import com.gip.xyna.xact.trigger.FilterInformation.FilterInstanceInformation;
import com.gip.xyna.xact.trigger.FilterInstanceStorable.FilterInstanceState;
import com.gip.xyna.xact.trigger.FilterStorable.FilterState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.StringParameterFormatter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listfilters;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class ListfiltersImpl extends XynaCommandImplementation<Listfilters> {

  public void execute(OutputStream statusOutputStream, Listfilters payload) throws XynaException {

    StringBuilder output = new StringBuilder();

    List<FilterInformation> filterInfo = factory.getActivationPortal().listFilterInformation();
    
    if (filterInfo.isEmpty() ) {
      output.append("No filters registered at server\n");
      writeToCommandLine(statusOutputStream, output.toString());
      return; 
    }
    
    writeToCommandLine(statusOutputStream, "Listing deployment status information filters...\n");
    Collections.sort(filterInfo, new SortByName(payload.getSortByRuntimeContext()) );
    
    if( payload.getOldFormat() ) {
      appendOldFormat( output, filterInfo, payload);
    } else {
      if( ! payload.getInstancesOnly() ) {
        FilterTableFormatter ftf = new FilterTableFormatter(filterInfo);
        ftf.writeTableHeader(output);
        ftf.writeTableRows(output);
        output.append("\n");
      }
      
      List<OrderedFilterInstanceInformation> ofiis = OrderedFilterInstanceInformation.extractFromFilters( filterInfo );
      Collections.sort(ofiis, new OFIISortByName(payload.getSortByRuntimeContext()) );
       
      FilterInstanceTableFormatter fitf = new FilterInstanceTableFormatter(ofiis);
      fitf.writeTableHeader(output);
      fitf.writeTableRows(output);
      
      
      if( payload.getVerbose() ) {
        output.append("\n");
        appendFilterConfigurationParameter(output, filterInfo);
      }
      
      if( payload.getInstanceConfigurationParameter() ) {
        output.append("\n");
        FilterInstanceConfigurationParameterTableFormatter ficptf = new FilterInstanceConfigurationParameterTableFormatter(ofiis);
        ficptf.writeTableHeader(output);
        ficptf.writeTableRows(output);
      }
      
      if( payload.getError() ) {
        output.append("\n");
        appendErrors( output, filterInfo, ofiis);
      }
    }
    writeLineToCommandLine(statusOutputStream, output.toString());
  }
  
  private void appendOldFormat(StringBuilder output, List<FilterInformation> filterInfo, Listfilters payload) {
    for (FilterInformation filterInformation : filterInfo) {
      output.append(GenerationBase.getSimpleNameFromFQName(filterInformation.getFqFilterClassName()));

      if(filterInformation.getRuntimeContext() instanceof Application) {
        output.append(" (Applicationname: ");
        output.append(filterInformation.getApplicationName());
        output.append(", Versionname: ");
        output.append(filterInformation.getVersionName());
        output.append(")");
      }
      
      if (filterInformation.getRuntimeContext() instanceof Workspace 
                      && !filterInformation.getRuntimeContext().equals(RevisionManagement.DEFAULT_WORKSPACE)) {
        output.append(" (Workspacename: ");
        output.append(filterInformation.getRuntimeContext().getName());
        output.append(")");
      }
      
      output.append(" named <").append(filterInformation.getFilterName()).append(">");
      if (filterInformation.getDescription() != null && filterInformation.getDescription().length() > 0) {
        output.append(": ").append(filterInformation.getDescription());
      }
      output.append("\n");
      
      String status;
      if (filterInformation.getFilterState() == null) {
        status = FilterState.OK.toString();
      } else {
        status = filterInformation.getFilterState().toString();
      }
      output.append("  status: " + status +"\n");
      
      if (payload.getError() && filterInformation.getErrorCause() != null) {
        output.append("    errorCause: ").append(filterInformation.getErrorCause());
      }
      appendOldFormatFilterInstances(output, filterInformation, filterInformation.getFilterInstances(), payload.getError() );
      
    }
  }

  private void appendOldFormatFilterInstances(StringBuilder output, FilterInformation filterInformation, 
      List<FilterInstanceInformation> filterInstances, boolean withError) {
    if (filterInstances == null || filterInstances.isEmpty() ) {
      return;
    }
    output.append("  Instances:\n");
    for( FilterInstanceInformation filterInstance : filterInstances ) {
      output.append("    - ").append(filterInstance.getFilterInstanceName());
      RuntimeContext rc;
      try {
        rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(filterInstance.getRevision());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new RuntimeException(e);
      }
      if (!rc.equals(filterInformation.getRuntimeContext())) {
        output.append(" (Context=").append(rc).append(")");
      }
      output.append(" [" + filterInstance.getState() + "] on ")
      .append(filterInstance.getTriggerInstanceName());

      if(filterInstance.isOptional()) {
        output.append(" (optional)");
      }

      output.append("\n"); 

      if (withError && filterInstance.getErrorCause() != null) {
        output.append("        errorCause: ").append(filterInstance.getErrorCause());
      }
    }
  }

  private void appendFilterConfigurationParameter(StringBuilder output, List<FilterInformation> filterInfo) {
    HashMap<String, String> duplicateFilter = new HashMap<>();
    boolean noConfiguration = true;
    int filterId = 0;
    for (FilterInformation fi : filterInfo) {
      ++filterId;
      String filterName = GenerationBase.getSimpleNameFromFQName(fi.getFqFilterClassName());
      String filterIdString = filterName+" (Id "+filterId+")";
      
      StringBuilder configurationParameter = new StringBuilder(); 
      appendFilterConfigurationParameter(configurationParameter, fi);
      
      if( configurationParameter.length() == 0 ) {
        continue;
      }
      noConfiguration = false;
      String duplicateId = duplicateFilter.get(configurationParameter.toString());
      if( duplicateId == null ) {
        duplicateFilter.put(configurationParameter.toString(), filterIdString );
      }
      
      if( duplicateId != null ) {
        output.append(filterIdString).append(" has same start parameter as ").append(duplicateId).append("\n");
      } else {
        output.append(filterIdString).append("\n").append(configurationParameter.toString());
      }
    }
    if( noConfiguration ) {
      output.append("No configurable filters.");
    }
  }

  private void appendFilterConfigurationParameter(StringBuilder output, FilterInformation filterInformation) {
    List<StringParameter<?>> cps = filterInformation.getConfigurationParameter();
    if( cps == null || cps.isEmpty() ) {
      return;
    }
    output.append("  possible configuration parameter as key/value pairs (key=value):\n");
    for( StringParameter<?> sp : cps ) {
      output.append("    - ");
      StringParameterFormatter.appendStringParameter( output, sp );
      output.append("\n");
    }
  }

  private void appendErrors(StringBuilder output, List<FilterInformation> filterInfo, List<OrderedFilterInstanceInformation> ofiis) {
    boolean noError = true;
    int filterIndex = 0;
    int filterInstanceIndex = 0;
    for (FilterInformation fi : filterInfo) {
      ++filterIndex;
      if( fi.getErrorCause() != null) {
        noError = false;
        output.append("\nFilter ").
              append( FilterTableFormatter.FilterColumn.CLASS_NAME.extract(fi) ).
              append(" (").append(filterIndex).append(")").
              append("\n");
        output.append("    errorCause: ").append(fi.getErrorCause());
      }
    }
    for ( OrderedFilterInstanceInformation ofii : ofiis ) {
      ++filterInstanceIndex;
      if( ofii.fii.getErrorCause() != null) {
        noError = false;
        output.append("\nFilter instance ").
        append( FilterInstanceTableFormatter.FilterInstanceColumn.INSTANCE.extract(ofii) ).
        append(" (").append(filterInstanceIndex).append(")").
        append("\n");
        output.append("    errorCause: ").append(ofii.fii.getErrorCause());
      }
    }
    if( noError ) {
      output.append("No errors.");
    }
  }

  private static class SortByName implements Comparator<FilterInformation> {
    private boolean sortByRuntimeContext;

    public SortByName( boolean sortByRuntimeContext ) {
      this.sortByRuntimeContext = sortByRuntimeContext;
    }
    
    public int compare(FilterInformation o1, FilterInformation o2) {
      int comp = 0;
      if( sortByRuntimeContext ) {
        comp = ComparatorUtils.compareNullAware(o1.getRuntimeContext(), o2.getRuntimeContext(), false);
      } else {
        comp = ComparatorUtils.compareNullAware(o1.getFilterName(), o2.getFilterName(), false);
      }
      if( comp == 0 ) {
        if( sortByRuntimeContext ) {
          comp = ComparatorUtils.compareNullAware(o1.getFilterName(), o2.getFilterName(), false);
        } else {
          comp = ComparatorUtils.compareNullAware(o1.getRuntimeContext(), o2.getRuntimeContext(), false);
        }
      }
      return comp;
    }
  }

  private static class OFIISortByName implements Comparator<OrderedFilterInstanceInformation> {
    private boolean sortByRuntimeContext;

    public OFIISortByName( boolean sortByRuntimeContext ) {
      this.sortByRuntimeContext = sortByRuntimeContext;
    }
    
    public int compare(OrderedFilterInstanceInformation o1, OrderedFilterInstanceInformation o2) {
      int comp = 0;
      if( sortByRuntimeContext ) {
        comp = ComparatorUtils.compareNullAware(o1.getRuntimeContext(), o2.getRuntimeContext(), false);
      } else {
        comp = ComparatorUtils.compareNullAware(o1.getFilterInstanceName(), o2.getFilterInstanceName(), false);
      }
      if( comp == 0 ) {
        if( sortByRuntimeContext ) {
          comp = ComparatorUtils.compareNullAware(o1.getFilterInstanceName(), o2.getFilterInstanceName(), false);
        } else {
          comp = ComparatorUtils.compareNullAware(o1.getRuntimeContext(), o2.getRuntimeContext(), false);
        }
      }
      return comp;
    }
  }

  private static class OrderedFilterInstanceInformation {
    private FilterInstanceInformation fii;
    private int filterId;
    private RuntimeContext rc;
    
    public OrderedFilterInstanceInformation(FilterInstanceInformation fii, int filterId, RuntimeContext rc) {
      this.fii = fii;
      this.filterId = filterId;
      this.rc = rc;
    }

    public RuntimeContext getRuntimeContext() {
      return rc;
    }

    public String getFilterInstanceName() {
      return fii.getTriggerInstanceName();
    }
    
    public static List<OrderedFilterInstanceInformation> extractFromFilters( List<FilterInformation> filters) {
      List<OrderedFilterInstanceInformation> ofiis = new ArrayList<>();
      RevisionManagement revisionManagement =  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      int filterId = 0; 
      for( FilterInformation fi : filters ) {
        ++filterId;
        List<FilterInstanceInformation> filterInstances = fi.getFilterInstances();
        if( filterInstances != null && ! filterInstances.isEmpty() ) {
          for( FilterInstanceInformation fii : filterInstances ) {
            RuntimeContext rc;
            try {
              rc = revisionManagement.getRuntimeContext(fii.getRevision());
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              rc = null; //TODO Fehler ausgeben?
            }
            ofiis.add( new OrderedFilterInstanceInformation(fii, filterId, rc) );
          }
        }
      }
      return ofiis;
    }

  } 


  
  private static class FilterTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<FilterColumn> columns;
    
    public FilterTableFormatter(List<FilterInformation> filters) {
      columns = Arrays.asList(FilterColumn.values());
      
      header = new ArrayList<>();
      header.add( "Id" );
      for( FilterColumn fc : columns ) {
        header.add( fc.getDisplayName() );
      }
      
      generateRows(filters);
    }

    private void generateRows(List<FilterInformation> filters) {
      rows = new ArrayList<>();
      int filterId =0;
      for( FilterInformation fi : filters ) {
        ++filterId;
        List<String> row = new ArrayList<>();
        row.add( String.valueOf(filterId) );
        for( FilterColumn fc : columns ) {
          row.add( fc.extract(fi) );
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

    
    private enum FilterColumn {
      
      FILTER_NAME("FilterName") {
        public String extract(FilterInformation fi) {
          return fi.getFilterName();
        }
      },
      CLASS_NAME("ClassName") {
        public String extract(FilterInformation fi) {
          return GenerationBase.getSimpleNameFromFQName(fi.getFqFilterClassName());
        }
      },
      DESCRIPTION("Description") {
        public String extract(FilterInformation fi) {
          return fi.getDescription();
        }
      },
      RUNTIME_CONTEXT("RuntimeContext") {
        public String extract(FilterInformation fi) {
          RuntimeContext rc = fi.getRuntimeContext();
          return rc.toString();
        }
      },
      STATUS("Status") {
        public String extract(FilterInformation ti) {
          String status;
          if (ti.getFilterState() == null) {
            status = FilterState.OK.toString();
          } else {
            status = ti.getFilterState().toString();
          }
          return status;
        }
      },
      INSTANCES("Instances") {
        public String extract(FilterInformation fi) {
          return fi.getFilterInstances() == null ? "0" : String.valueOf(fi.getFilterInstances().size());
        }
      }
      ;
      
      private String displayName;
      private FilterColumn(String displayName) {
        this.displayName = displayName;
      }
      public String getDisplayName() {
        return displayName;
      }
      public abstract String extract(FilterInformation fi);
    }
  }

  private static class FilterInstanceTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<FilterInstanceColumn> columns;

    public FilterInstanceTableFormatter(List<OrderedFilterInstanceInformation> ofiis) {
      columns = Arrays.asList(FilterInstanceColumn.values());

      header = new ArrayList<>();
      header.add( "Id" );
      for( FilterInstanceColumn tc : columns ) {
        header.add( tc.getDisplayName() );
      }
      
      generateRows(ofiis);
    }


    private void generateRows(List<OrderedFilterInstanceInformation> ofiis) {
      rows = new ArrayList<>();
      int filterInstanceId = 0;
      for( OrderedFilterInstanceInformation ofii : ofiis ) {
        ++filterInstanceId;
        List<String> row = new ArrayList<>();
        row.add(String.valueOf(filterInstanceId) );
        for( FilterInstanceColumn fic : columns ) {
          row.add( fic.extract(ofii) );
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


    private enum FilterInstanceColumn {

      INSTANCE("Instance") {
        public String extract(OrderedFilterInstanceInformation ofii) {
          return ofii.fii.getFilterInstanceName();
        }
      },
      TRIGGER_INSTANCE("TriggerInstance") {
        public String extract(OrderedFilterInstanceInformation ofii) {
          return ofii.fii.getTriggerInstanceName();
        }
      },
      FILTER("Filter") {
        public String extract(OrderedFilterInstanceInformation ofii) {
          return "("+ofii.filterId + ") "+ofii.fii.getFilterName();
        }
      },
      DESCRIPTION("Description") {
        public String extract(OrderedFilterInstanceInformation ofii) {
          return ofii.fii.getDescription();
        }
      },
      RUNTIME_CONTEXT("RuntimeContext") {
        public String extract(OrderedFilterInstanceInformation ofii) {
          return String.valueOf( ofii.getRuntimeContext() );
        }
      },
      STATUS("Status") {
        public String extract(OrderedFilterInstanceInformation ofii) {
          FilterInstanceState state = ofii.fii.getState();
          return state.toString();
        }
      },
      OPTIONAL("Optional") {
        public String extract(OrderedFilterInstanceInformation ofii) {
          return ofii.fii.isOptional() ? "optional" : "";
        }
      },
      
      ;
      private String displayName;
      private FilterInstanceColumn(String displayName) {
        this.displayName = displayName;
      }
      public String getDisplayName() {
        return displayName;
      }
      public abstract String extract(OrderedFilterInstanceInformation ofii);
    }

  }

  private static class FilterInstanceConfigurationParameterTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;

    public FilterInstanceConfigurationParameterTableFormatter(List<OrderedFilterInstanceInformation> ofiis) {
      header = new ArrayList<>();
      header.add( "Id" );
      header.add( "Instance" );
      header.add( "ConfigurationParameter" );
      generateRows(ofiis);
    }


    private void generateRows(List<OrderedFilterInstanceInformation> ofiis) {
      rows = new ArrayList<>();
      int filterInstanceId = 0;
      for(OrderedFilterInstanceInformation ofii : ofiis ) {
        ++filterInstanceId;
        List<String> row = new ArrayList<>();
        row.add(String.valueOf(filterInstanceId) );
        row.add( ofii.getFilterInstanceName() );
        row.add( generateConfigurationString(ofii.fii.getConfiguration()) );
        rows.add( row );
      }
    }

    private String generateConfigurationString(List<String> configuration) {
      if ( configuration == null ) {
        return "";
      }
      StringBuilder sb = new StringBuilder();
      String sep = "";
      for( String s : configuration ) {
        sb.append(sep).append(s);
        sep = ", ";
      }
      return sb.toString();
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
