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
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import xprc.xpce.Application;


public class LoadApplicationList {

  public static class ApplicationComparator implements Comparator<Application> {
    @Override
    public int compare(Application obj1, Application obj2) {
      if ((obj1 == null) && (obj2 == null)) { return 0; }
      if (obj1 == null) { return -1; }
      if (obj2 == null) { return 1; }
      if ((obj1.getName() == null) && (obj2.getName() == null)) { return 0; }
      if (obj1.getName() == null) { return -1; }
      if (obj2.getName() == null) { return 1; }
      int val = obj1.getName().compareTo(obj2.getName());
      if (val != 0) { return val; }
      if ((obj1.getVersion() == null) && (obj2.getVersion() == null)) { return 0; }
      if (obj1.getVersion() == null) { return -1; }
      if (obj2.getVersion() == null) { return 1; }
      return obj1.getVersion().compareTo(obj2.getVersion());
    }
  }
  
  
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
    Collections.sort(ret, new ApplicationComparator());
    return ret;
  }
  
}
