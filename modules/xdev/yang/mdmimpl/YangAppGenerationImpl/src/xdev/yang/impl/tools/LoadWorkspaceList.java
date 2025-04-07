/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xdev.yang.impl.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;

import xprc.xpce.Workspace;


public class LoadWorkspaceList {

  public static class WorkspaceComparator implements Comparator<Workspace> {
    @Override
    public int compare(Workspace obj1, Workspace obj2) {
      if ((obj1 == null) && (obj2 == null)) { return 0; }
      if (obj1 == null) { return -1; }
      if (obj2 == null) { return 1; }
      if ((obj1.getName() == null) && (obj2.getName() == null)) { return 0; }
      if (obj1.getName() == null) { return -1; }
      if (obj2.getName() == null) { return 1; }
      return obj1.getName().compareTo(obj2.getName());
    }
  }
  
  
  public List<? extends Workspace> execute() {
    try {
      return getWorkspaceListImpl();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  
  private List<Workspace> getWorkspaceListImpl() {
    List<Workspace> ret = new ArrayList<>();
    WorkspaceManagement workspaceMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    List<WorkspaceInformation> workspaces = workspaceMgmt.listWorkspaces(false);
    for (WorkspaceInformation workspace : workspaces) {
      Workspace.Builder builder = new Workspace.Builder();
      builder.name(workspace.getWorkspace().getName());
      ret.add(builder.instance());
    }
    Collections.sort(ret, new WorkspaceComparator());
    return ret;
  }
  
}
