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

import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.update.Version;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationState;

/*
 * sp�tere evaluierung, welcher runtimecontext "wirklich" der richtige ist. dieser runtimecontext ist prinzipiell nur ein zwischenzustand
 */
public abstract class DynamicRuntimeContext extends RuntimeContext {
  

  public static DynamicRuntimeContext useLatestVersion(final String applicationName) {
    return new DynamicRuntimeContext(applicationName) {

      private static final long serialVersionUID = 1L;


      @Override
      public RuntimeContext evaluate() {
        ApplicationManagementImpl ami =
            (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
        List<ApplicationInformation> listApplications = ami.listApplications(false, false);
        ApplicationInformation latest = null;
        Version latestVersion = null;
        for (ApplicationInformation ai : listApplications) {
          if (!(ai instanceof ApplicationDefinitionInformation || ai.getState() == ApplicationState.WORKINGCOPY) && ai.getName().equals(applicationName)) {
            Version v = new Version(ai.getVersion());
            if (latestVersion == null || v.isStrictlyGreaterThan(latestVersion)) {
              latestVersion = v;
              latest = ai;
            }
          }
        }
        if (latestVersion == null) {
          return null;
        }
        return new Application(latest.getName(), latest.getVersion());
      }
      
      public String toString() {
        return "latest version of '" +  applicationName + "'";
      }
    };
  }

  private static final long serialVersionUID = 1L;

  public DynamicRuntimeContext(String applicationName) {
    super(applicationName);
  }

  public String serializeToString() {
    throw new RuntimeException("not supported");
  }

  public int compareTo(RuntimeContext o) {
    throw new RuntimeException("not supported");
  }

  @Override
  public RuntimeContextType getType() {
    throw new RuntimeException("not supported");
  }

  @Override
  public String getGUIRepresentation() {
    throw new RuntimeException("not supported");
  }
  
  public abstract RuntimeContext evaluate();

}
