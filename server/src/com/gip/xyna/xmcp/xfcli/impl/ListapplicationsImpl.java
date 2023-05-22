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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Version;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Filter;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listapplications;



public class ListapplicationsImpl extends XynaCommandImplementation<Listapplications> {
  
  private static final Comparator<ApplicationInformation> COMP_APPINFO = new Comparator<ApplicationInformation>() {

    public int compare(ApplicationInformation o1, ApplicationInformation o2) {
      if (o1.getName().equals(o2.getName())) {
        if (o1.getVersion() == null) {
          return 1;
        }
        return compareVersions(o1.getVersion(), o2.getVersion());
      }          
      return o1.getName().compareTo(o2.getName());
    }

    private int compareVersions(String version1, String version2) {
      if (version1.equals(version2)) {
        return 0;
      }
      Version v1 = new Version(version1);
      Version v2 = new Version(version2);
      if (v1.isStrictlyGreaterThan(v2)) {
        return 1;
      }
      return -1;
    }
    
  };
  

  public void execute(OutputStream statusOutputStream, Listapplications payload) throws XynaException {
    
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    
    List<ApplicationInformation> appsinfo = applicationManagement.listApplications(true, payload.getVerbose());
    
    if( appsinfo.size() == 0 ) {
      if( payload.getAsTable() ) {
        ApplicationTableFormatter atf = new ApplicationTableFormatter(appsinfo);
        writeLineToCommandLine(statusOutputStream, atf.writeTableHeader() );
      } else {
        writeLineToCommandLine(statusOutputStream, "No applications defined.");
      }
      return;
    }
    
    //Filtern der gefunden ApplicationInformation
    List<ApplicationInformation> filteredApps = CollectionUtils.filter(appsinfo, new ApplicationFilter(payload) );
    
    if( filteredApps.size() == 0 ) {
      if( payload.getAsTable() ) {
        ApplicationTableFormatter atf = new ApplicationTableFormatter(appsinfo);
        writeLineToCommandLine(statusOutputStream, atf.writeTableHeader() );
      } else {
        writeLineToCommandLine(statusOutputStream, "No applications matching given criteria found.");
      }
      return;
    }
    
    //Anzeige der verbliebenen Applications
    StringBuilder output = new StringBuilder();
    if( payload.getAsTable() ) {
      Collections.sort(filteredApps, COMP_APPINFO);
      ApplicationTableFormatter atf = new ApplicationTableFormatter(filteredApps);
      atf.writeTableHeader(output);
      atf.writeTableRows(output);
    } else {
      Map<RuntimeContext, ArrayList<ApplicationInformation>> grouped = 
          CollectionUtils.group(filteredApps, new ParentWorkspaceGrouping() );
      
      List<RuntimeContext> parentWorkspaces = new ArrayList<RuntimeContext>(grouped.keySet());
      Collections.sort(parentWorkspaces);

      //Application-Definitionen
      for(RuntimeContext rc : parentWorkspaces) {
        if (rc instanceof Workspace) {
          output.append(rc).append(":\n");
          appendApplications(output, grouped.get(rc));
        }
      }
      
      //RuntimeApplications
      output.append("RuntimeApplications:\n");
      appendApplications(output, grouped.get(new Application("RuntimeApplication", "")));
    }
    writeToCommandLine(statusOutputStream, output.toString());
  }

  
  private void appendApplications(StringBuilder bld, List<ApplicationInformation> appsinfo) {
    if (appsinfo == null) {
      bld.append(" none\n");
      return;
    }
    Collections.sort(appsinfo, COMP_APPINFO);
    
    for(ApplicationInformation appInfo : appsinfo) {
      String state = getState(appInfo);
      bld.append("  '").append(appInfo.getName()).append("' '").append(appInfo.getVersion()).append("' (")
      .append(appInfo.getObjectCount()).append(" objects + dependencies), STATUS: '").append(state).append("'");

      if(appInfo.getComment() != null) {
        bld.append(" - '").append(appInfo.getComment()).append("'");
      }
      
      bld.append("\n");
    }
  }
  
  
  private static String getState(ApplicationInformation appInfo) {
    //wegen Abw�rtskompatibilit�t Zustand WORKINGCOPY behalten
    if (appInfo instanceof ApplicationDefinitionInformation) {
      switch (appInfo.getState()) {
        case OK:
          return ApplicationState.WORKINGCOPY.toString();
        case WARNING:
        case ERROR:
          return ApplicationState.WORKINGCOPY.toString() + " " + appInfo.getState().toString();
        default:
          return appInfo.getState().toString();
      }
    } else {
      return appInfo.getState().toString();
    }
  }
  
  private static class ParentWorkspaceGrouping implements Transformation<ApplicationInformation, RuntimeContext> {
    private static Application RUNTIME = new Application("RuntimeApplication", "");
    
    public RuntimeContext transform(ApplicationInformation from) {
      if (from instanceof ApplicationDefinitionInformation) {
        return ((ApplicationDefinitionInformation)from).getParentWorkspace();
      }
      return RUNTIME;
    }
    
  }

  private static class ApplicationFilter implements Filter<ApplicationInformation> {

    private boolean hideDefinitions;
    private Pattern namePattern;
    private Pattern workspacePattern;

    public ApplicationFilter(Listapplications payload) {
      this.namePattern = createPattern(payload.getApplicationName());
      this.workspacePattern = createPattern(payload.getWorkspaceName());
      this.hideDefinitions = payload.getHideDefinitions();
    }

    private Pattern createPattern(String pattern) {
      if( pattern == null ) {
        return null;
      }
      return Pattern.compile("\\Q"+pattern+"\\E");
    }

    public boolean accept(ApplicationInformation value) {
      boolean isDefinition = value instanceof ApplicationDefinitionInformation;
      if( isDefinition && hideDefinitions ) {
        return false; //keine Definition gew�nscht
      }
      if( namePattern != null && ! namePattern.matcher( value.getName() ).matches() ) {
        return false; //Name passt nicht
      }
      if( workspacePattern != null ) {
        if( isDefinition ) {
          String workspaceName = ((ApplicationDefinitionInformation)value).getParentWorkspace().getName();
          if( ! workspacePattern.matcher( workspaceName ).matches() ) {
            return false; //Workspace passt nicht
          }
        } else {
          return false; //nach Workspace gesucht, d.h, nur Definitions ausgeben
        }
      }
      return true;
    }
    
  }
  
 
  private static class ApplicationTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<ApplicationColumn> columns;
   
    public ApplicationTableFormatter(List<ApplicationInformation> applications) {
      columns = Arrays.asList(ApplicationColumn.values() );
      /*
      if( additionalArguments == null ) {
        columns = Arrays.asList(ApplicationColumn.values() );
      } else {
        if(additionalArguments.get(0).startsWith("tableFormat") ) {
          columns = new ArrayList<ApplicationColumn>();
        
        
          columns.add(ApplicationColumn.ApplicationName);
          columns.add(ApplicationColumn.VersionName);
          columns.add(ApplicationColumn.Status);
        } else {
          columns = Arrays.asList(ApplicationColumn.values() );
        }
      }*/
      
      generateRowsAndHeader(applications);
    }

    private void generateRowsAndHeader(List<ApplicationInformation> applications) {
      header = new ArrayList<String>();
      for( ApplicationColumn ac : columns ) {
        header.add( ac.toString() );
      }
      rows = new ArrayList<List<String>>();
      for( ApplicationInformation ai : applications ) {
        rows.add( generateRow(ai) );
      }
    }

    private List<String> generateRow(ApplicationInformation ai) {
      List<String> row = new ArrayList<String>();
      for( ApplicationColumn ac : columns ) {
        row.add( ac.extract(ai) );
      }
      return row;
    }

    public List<String> getHeader() {
      return header;
    }
    
    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    
    public enum ApplicationColumn {
      ApplicationName {
        public String extract(ApplicationInformation ai) {
          return ai.getName();
        }
      },
      VersionName {
        public String extract(ApplicationInformation ai) {
          return ai.getVersion();
        }
      },
      Workspace {
        public String extract(ApplicationInformation ai) {
          if( ai instanceof ApplicationDefinitionInformation ) {
            return ((ApplicationDefinitionInformation)ai).getParentWorkspace().getName();
          } else {
            return "-";
          }
        }
      },
      Status {
        public String extract(ApplicationInformation ai) {
          return getState(ai);
        }
      },
      Objects {
        public String extract(ApplicationInformation ai) {
          return String.valueOf(ai.getObjectCount());
        }
      },
      Revision {
        public String extract(ApplicationInformation ai) {
          try {
            if( ai instanceof ApplicationDefinitionInformation ) {
              long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(((ApplicationDefinitionInformation) ai).getParentWorkspace());
              return String.valueOf(rev );
            } else {
              long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(ai.getName(), ai.getVersion(),  null);
              return String.valueOf(rev);
            }
          } catch( Exception e ) {
            return "-";
          }
        }
      };

      public abstract String extract(ApplicationInformation ai);
    }

  }
  
  
}
