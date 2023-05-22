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
package com.gip.xyna.xfmg.xfctrl.revisionmgmt;

import com.gip.xyna.utils.StringUtils;


public class Application extends RuntimeContext implements RuntimeDependencyContext {

  private static final String GUI_REPRESENTATION_APPLICATION = "A:\"";
  private static final String GUI_REPRESENTATION_VERSION = "\"-V:\"";
  private static final String GUI_REPRESENTATION_END = "\"";

  private static final long serialVersionUID = 1L;
  
  private final String versionName;
  
  
  public Application(String applicationName, String versionName) {
    super(applicationName);
    this.versionName = versionName;
  }


  public String getVersionName() {
    return versionName;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((versionName == null) ? 0 : versionName.hashCode());
    return result;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (getClass() != obj.getClass())
      return false;
    Application other = (Application) obj;
    if (versionName == null) {
      if (other.versionName != null)
        return false;
    } else if (!versionName.equals(other.versionName))
      return false;
    return true;
  }


  @Override
  public String toString() {
    return "Application '" + getName() + "', Version '" + versionName + "'";
  }
  
  
  public int compareTo(RuntimeContext o) {
    if (o instanceof Application) {
      int compareResult = getName().compareTo(o.getName());
      if (compareResult == 0) {
        if (versionName != null && ((Application) o).versionName != null) {
          compareResult = versionName.compareTo(((Application) o).versionName);
        } else {
          if (((Application) o).versionName == null) {
            return -1;
          }
          if (versionName == null) {
            return 1;
          }
        }
      }
      return compareResult;
    }
    
    return super.compareToClass(o.getClass());
  }


  public String serializeToString() {
    return StringUtils.mask(getName(),'/')+"/"+ versionName;
  }
  
  @Override
  public RuntimeContextType getType() {
    return RuntimeContextType.Application;
  }

  @Override
  public String getGUIRepresentation() {
    return GUI_REPRESENTATION_APPLICATION + getName() + GUI_REPRESENTATION_VERSION + getVersionName() + GUI_REPRESENTATION_END;
  }

  public static Application getFromGUIRepresentation(String guiRepresentation) {
    if (!guiRepresentation.startsWith(GUI_REPRESENTATION_APPLICATION) || !guiRepresentation.contains(GUI_REPRESENTATION_VERSION) || !guiRepresentation.endsWith(GUI_REPRESENTATION_END)) {
      return null;
    }

    try {
      final int nameStartIdx = GUI_REPRESENTATION_APPLICATION.length();
      final int nameEndIdx = guiRepresentation.indexOf(GUI_REPRESENTATION_VERSION);
      final int versionStartIdx = guiRepresentation.lastIndexOf(GUI_REPRESENTATION_VERSION) + GUI_REPRESENTATION_VERSION.length();
      final int versionEndIdx = guiRepresentation.length() - GUI_REPRESENTATION_END.length();

      return new Application(guiRepresentation.substring(nameStartIdx, nameEndIdx), guiRepresentation.substring(versionStartIdx, versionEndIdx));
    } catch (Exception e) {
      return null;
    }
  }

  public String getAdditionalIdentifier() {
    return versionName;
  }
  
}
