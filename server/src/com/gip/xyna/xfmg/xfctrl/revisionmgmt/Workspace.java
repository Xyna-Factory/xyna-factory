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
package com.gip.xyna.xfmg.xfctrl.revisionmgmt;

import com.gip.xyna.utils.StringUtils;


public class Workspace extends RuntimeContext implements RuntimeDependencyContext {

  private static final String GUI_REPRESENTATION_WORKSPACE = "WS:\"";
  private static final String GUI_REPRESENTATION_END = "\"";

  private static final long serialVersionUID = 1L;

  public Workspace(String workspaceName) {
    super(workspaceName);
  }
  

  public int compareTo(RuntimeContext o) {
    if (o instanceof Workspace) {
      if (this.equals(RevisionManagement.DEFAULT_WORKSPACE)) {
        if (o.equals(RevisionManagement.DEFAULT_WORKSPACE)) {
          return 0;
        } else {
          return -1;
        }
      }
      
      if (o.equals(RevisionManagement.DEFAULT_WORKSPACE)) {
        return 1;
      }
      
      return this.getName().compareTo(o.getName());
    }
    
    return super.compareToClass(o.getClass());
  }


  public String serializeToString() {
    return StringUtils.mask(getName(),'/');
  }

  @Override
  public String toString() {
    return "Workspace '" + getName() + "'";
  }
  
  @Override
  public RuntimeContextType getType() {
    return RuntimeContextType.Workspace;
  }
  
  @Override
  public String getGUIRepresentation() {
    return GUI_REPRESENTATION_WORKSPACE + getName() + GUI_REPRESENTATION_END;
  }

  public static Workspace getFromGUIRepresentation(String guiRepresentation) {
    if (!guiRepresentation.startsWith(GUI_REPRESENTATION_WORKSPACE) || !guiRepresentation.endsWith(GUI_REPRESENTATION_END)) {
      return null;
    }

    try {
      final int nameStartIdx = GUI_REPRESENTATION_WORKSPACE.length();
      final int nameEndIdx = guiRepresentation.length() - GUI_REPRESENTATION_END.length();

      return new Workspace(guiRepresentation.substring(nameStartIdx, nameEndIdx));
    } catch (Exception e) {
      return null;
    }
  }


  public String getAdditionalIdentifier() {
    return null;
  }

}
