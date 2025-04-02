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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import xprc.xpce.Application;


public class LoadApplicationList {

  public List<? extends Application> execute() {
    try {
      return getApplicationListImpl();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  
  private List<Application> getApplicationListImpl() throws PersistenceLayerException {
    List<Application> ret = new ArrayList<>();
    ApplicationManagement mgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    List<ApplicationInformation> applications = mgmt.listApplications();
    for (ApplicationInformation application : applications) {
      if (application instanceof ApplicationDefinitionInformation) { continue; }
      Application.Builder builder = new Application.Builder();
      builder.name(application.getName())
             .version(application.getVersion());
      ret.add(builder.instance());
    }
    return ret;
  }
  
}
