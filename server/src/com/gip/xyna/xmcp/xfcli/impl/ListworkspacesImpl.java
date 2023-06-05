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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listworkspaces;



public class ListworkspacesImpl extends XynaCommandImplementation<Listworkspaces> {

  public void execute(OutputStream statusOutputStream, Listworkspaces payload) throws XynaException {
    WorkspaceManagement workspaceManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    List<WorkspaceInformation> workspaces = workspaceManagement.listWorkspaces(true);
    
    StringBuilder output = new StringBuilder();
    if( payload.getAsTable() ) {
      WorkspaceTableFormatter wtf = new WorkspaceTableFormatter(workspaces);
      wtf.writeTableHeader(output);
      wtf.writeTableRows(output);
    } else {
      for (WorkspaceInformation workspace : workspaces) {
        output.append(workspace.getWorkspace().getName())
        .append(", STATUS: '").append(workspace.getState()).append("'").append("\n");
      }
    }
    writeLineToCommandLine(statusOutputStream, output.toString());
  }

  private static class WorkspaceTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<WorkspaceColumn> columns;


    public WorkspaceTableFormatter(List<WorkspaceInformation> workspaces) {
      columns = Arrays.asList(WorkspaceColumn.values() );
      generateRowsAndHeader(workspaces);
    }

    private void generateRowsAndHeader(List<WorkspaceInformation> workspaces) {
      header = new ArrayList<String>();
      for( WorkspaceColumn wc : columns ) {
        header.add( wc.toString() );
      }
      rows = new ArrayList<List<String>>();
      for( WorkspaceInformation wi : workspaces ) {
        rows.add( generateRow(wi) );
      }
    }

    private List<String> generateRow(WorkspaceInformation wi) {
      List<String> row = new ArrayList<String>();
      for( WorkspaceColumn wc : columns ) {
        row.add( wc.extract(wi) );
      }
      return row;
    }
    
    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    
    @Override
    public List<String> getHeader() {
      return header;
    }

    private enum WorkspaceColumn {
      
      Name {
        public String extract(WorkspaceInformation wi) {
          return wi.getWorkspace().getName();
        }
      },
      Revision {
        public String extract(WorkspaceInformation wi) {
          try {
            long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(wi.getWorkspace());
            return String.valueOf(rev );
          } catch( Exception e ) {
            return "-";
          }
        }
      },
      Status {
        public String extract(WorkspaceInformation wi) {
          return String.valueOf( wi.getState() );
        }
      },
      Problems {
        public String extract(WorkspaceInformation wi) {
          if( wi.getProblems() == null ) {
            return "";
          }
          return String.valueOf( wi.getProblems().size() );
        }
      },
      /*
      OrderEntrances {
        public String extract(WorkspaceInformation wi) {
          if( wi.getOrderEntrances() == null ) {
            return "";
          }
          return String.valueOf( wi.getOrderEntrances().size() );
        }
      },*/
      Requirements {
        public String extract(WorkspaceInformation wi) {
          if( wi.getRequirements() == null ) {
            return "";
          }
          return String.valueOf( wi.getRequirements().size() );
          //return String.valueOf( wi.getRequirements() );
        }
      },
      
      ;
      
      public abstract String extract(WorkspaceInformation wi);
    }
    
  }

}
