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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listruntimecontextdependencies;



public class ListruntimecontextdependenciesImpl extends XynaCommandImplementation<Listruntimecontextdependencies> {
  
  private static final Comparator<RuntimeDependencyContext> COMPARATOR = new Comparator<RuntimeDependencyContext>() {

    @Override
    public int compare(RuntimeDependencyContext o1, RuntimeDependencyContext o2) {
      int i1 = o1.getRuntimeDependencyContextType().ordinal();
      int i2 = o2.getRuntimeDependencyContextType().ordinal();
      if (i1 == i2) {
        if (o1.getRuntimeDependencyContextType() == RuntimeDependencyContextType.ApplicationDefinition) {
          //guirepresentation gibt erst appname, dann workspace name aus. es soll aber nach workspacename sortiert werden
          ApplicationDefinition a1 = (ApplicationDefinition) o1;
          ApplicationDefinition a2 = (ApplicationDefinition) o2;
          String s1 = a1.getParentWorkspace().getName() + ":::" + a1.getName();
          String s2 = a2.getParentWorkspace().getName() + ":::" + a2.getName();
          return s1.toLowerCase().compareTo(s2.toLowerCase());
        } else {
          return o1.getGUIRepresentation().toLowerCase().compareTo(o2.getGUIRepresentation().toLowerCase());
        }
      }
      return i1 - i2;
    }

  };

  public void execute(OutputStream statusOutputStream, Listruntimecontextdependencies payload) throws XynaException {
    RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

    RuntimeDependencyContext owner = RuntimeContextDependencyManagement.getRuntimeDependencyContext(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    
    Map<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> dependencies =
        new TreeMap<RuntimeDependencyContext, Collection<RuntimeDependencyContext>>(COMPARATOR);
    if (owner != null) {
      //Requirements für einen bestimmten RuntimeContext
      Collection<RuntimeDependencyContext> deps = rcdMgmt.getDependencies(owner);
      dependencies.put(owner, deps);
    } else {
      //alle vorhandenen Dependencies
      dependencies.putAll(rcdMgmt.getAllDependencies());
    }

    StringBuilder output = new StringBuilder();
    if (payload.getAsTable()) {
      DependencyTableFormatter dtf = new DependencyTableFormatter(dependencies);
      dtf.writeTableHeader(output);
      dtf.writeTableRows(output);
      writeToCommandLine(statusOutputStream, output.toString());
    } else {
      for (Entry<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> entry : dependencies.entrySet()) {
        writeLineToCommandLine(statusOutputStream, entry.getKey().toString());
        List<RuntimeDependencyContext> vals = new ArrayList<>(entry.getValue());
        Collections.sort(vals, COMPARATOR);
        for (RuntimeDependencyContext dep : vals) {
          writeLineToCommandLine(statusOutputStream, "  " + dep.toString());
        }
      }
    }
  }

  private static class DependencyTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<DependencyColumn> columns;
   
    public DependencyTableFormatter(Map<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> dependencies) {
      columns = Arrays.asList(DependencyColumn.values());
      generateRowsAndHeader(dependencies);
    }

    private void generateRowsAndHeader(Map<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> dependencies) {
      header = new ArrayList<String>();
      for (DependencyColumn dc : columns) {
        header.add(dc.toString());
      }
      rows = new ArrayList<List<String>>();
      for (Entry<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> entry : dependencies.entrySet()) {
        List<RuntimeDependencyContext> vals = new ArrayList<>(entry.getValue());
        Collections.sort(vals, COMPARATOR);
        for (RuntimeDependencyContext dep : vals) {
          rows.add(generateRow(entry.getKey(), dep));
        }
      }
    }

    private List<String> generateRow(RuntimeDependencyContext owner, RuntimeDependencyContext dep) {
      List<String> row = new ArrayList<String>();
      for (DependencyColumn dc : columns) {
        row.add(dc.extract(owner, dep));
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
    
    public enum DependencyColumn {
      OwnerType {
        public String extract(RuntimeDependencyContext owner, RuntimeDependencyContext dependency) {
          return owner.getRuntimeDependencyContextType().name();
        }
        
        @Override
        public String toString() {
          return "Owner Type";
        }
      },
      OwnerName {
        public String extract(RuntimeDependencyContext owner, RuntimeDependencyContext dependency) {
          return owner.getName();
        }
        
        @Override
        public String toString() {
          return "Owner Name";
        }
      },
      OwnerAddition {
        public String extract(RuntimeDependencyContext owner, RuntimeDependencyContext dependency) {
          if (owner instanceof Application) {
            return ((Application) owner).getVersionName();
          }
          if (owner instanceof ApplicationDefinition) {
            return ((ApplicationDefinition) owner).getParentWorkspace().getName();
          }
          return "";
        }
        
        @Override
        public String toString() {
          return "Owner Version/Workspace";
        }
      },
      RequirementType {
        public String extract(RuntimeDependencyContext owner, RuntimeDependencyContext dependency) {
          return dependency.getRuntimeDependencyContextType().name();
        }
        @Override
        public String toString() {
          return "Requirement Type";
        }
      },
      RequirementName {
        public String extract(RuntimeDependencyContext owner, RuntimeDependencyContext dependency) {
          return dependency.getName();
        }
        @Override
        public String toString() {
          return "Requirement Name";
        }
      },
      RequirementAddition {
        public String extract(RuntimeDependencyContext owner, RuntimeDependencyContext dependency) {
          if (dependency instanceof Application) {
            return ((Application) dependency).getVersionName();
          }
          if (dependency instanceof ApplicationDefinition) {
            return ((ApplicationDefinition) dependency).getParentWorkspace().getName();
          }
          return "";
        }
        
        @Override
        public String toString() {
          return "Requirement Version/Workspace";
        }
      };

      public abstract String extract(RuntimeDependencyContext owner, RuntimeDependencyContext dependency);
    }

  }
}
