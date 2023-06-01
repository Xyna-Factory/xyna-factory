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
package com.gip.xyna.xfmg.xfctrl.workspacemgmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ClearWorkingSet.RevisionContentBlackWhiteList;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class WorkspaceBlackListXynaProperties implements RevisionContentBlackWhiteList{
  private static final Logger logger = CentralFactoryLogging.getLogger(WorkspaceBlackListXynaProperties.class);

  private final XynaPropertyString triggerinstancesProp;
  private final XynaPropertyString triggersProp;
  private final XynaPropertyString filterinstancesProp;
  private final XynaPropertyString filtersProp;
  private final XynaPropertyString sharedLibsProp;
  private final XynaPropertyString xmomobjectsProp;
  private final XynaPropertyString applicationsProp;

  // ------------------ ACHTUNG: die namen dieser properties sind auch in ClearWorkspace.xml aufgeführt
  
  public WorkspaceBlackListXynaProperties(String workspaceName) {
    triggerinstancesProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.triggerinstances."+workspaceName, "");
    triggerinstancesProp
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "Comma separated list of trigger instance names to be excluded when clearing workspace '" + workspaceName +"'.");
    triggersProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.triggers."+workspaceName, "");
    triggersProp.setDefaultDocumentation(DocumentationLanguage.EN,
                                         "Comma separated list of trigger names to be excluded when clearing workspace '" + workspaceName +"'.");
    filterinstancesProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.filterinstances."+workspaceName, "");
    filterinstancesProp
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "Comma separated list of filter instance names to be excluded when clearing workspace '" + workspaceName +"'.");
    filtersProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.filters."+workspaceName, "");
    filtersProp.setDefaultDocumentation(DocumentationLanguage.EN,
                                        "Comma separated list of filter names to be excluded when clearing workspace '" + workspaceName +"'.");
    xmomobjectsProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.xmomobjects."+workspaceName, "");
    xmomobjectsProp
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "Comma separated list of fully qualified XMOM names to be excluded when clearing workspace '" + workspaceName +"'.");
    applicationsProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.applications."+workspaceName, "");
    applicationsProp.setDefaultDocumentation(DocumentationLanguage.EN,
                                             "Comma separated list of application names to be excluded when clearing workspace '" + workspaceName +"'.");
    sharedLibsProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.sharedlibs."+workspaceName, "");
    sharedLibsProp.setDefaultDocumentation(DocumentationLanguage.EN,
                                           "Comma separated list of shared libs to be excluded when clearing workspace '" + workspaceName +"'.");
  }
  
  public void registerPropertiesDependency(String user) {
    triggerinstancesProp.registerDependency(user);
    triggersProp.registerDependency(user);
    filterinstancesProp.registerDependency(user);
    filtersProp.registerDependency(user);
    sharedLibsProp.registerDependency(user);
    xmomobjectsProp.registerDependency(user);
    applicationsProp.registerDependency(user);
  }

  public void unregisterProperties() {
    triggerinstancesProp.unregister();
    triggersProp.unregister();
    filterinstancesProp.unregister();
    filtersProp.unregister();
    sharedLibsProp.unregister();
    xmomobjectsProp.unregister();
    applicationsProp.unregister();
  }

  
  public List<String> getTriggerInstanceNames() {
    return getPropertyAsList(triggerinstancesProp);
  }


  public List<String> getFilterInstanceNames() {
    return getPropertyAsList(filterinstancesProp);
  }


  public List<String> getXMOMObjects() {
    return getPropertyAsList(xmomobjectsProp);
  }


  public List<String> getSharedLibs() {
    return getPropertyAsList(sharedLibsProp);
  }


  public List<String> getTriggersNames() {
    return getPropertyAsList(triggersProp);
  }


  public List<String> getFilterNames() {
    return getPropertyAsList(filtersProp);
  }


  public List<ApplicationName> getApplications() {
    List<String> apps = getPropertyAsList(applicationsProp);
    List<ApplicationName> appNames = new ArrayList<ApplicationName>();
    if (apps.size() > 0) {
      ODS ods = ODSImpl.getInstance();
      Collection<ApplicationStorable> allApplications;
      try {
        ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
        try {
          allApplications = con.loadCollection(ApplicationStorable.class);
        } finally {
          con.closeConnection();
        }
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
      for (String app : apps) {
        boolean found = false;
        for (ApplicationStorable a : allApplications) {
          if (app.equals(a.getName()) && a.isApplicationDefinition()) {
            appNames.add(new ApplicationName(app, a.getVersion()));
            found = true;
            break;
          }
        }
        if (!found) {
          logger.warn("Did not find application '" + app + "' defined in any workingset.");
        }
      }
    }
    return appNames;
  }

  /**
   * Zerlegt eine BlackList-XynaProperty in einzelne Werte. Als Trennzeichen werden Kommata
   * verwendet. Leerzeichen vor und hinter einem Komma werden ignoriert.
   * @param property
   * @return
   */
  private List<String> getPropertyAsList(XynaPropertyString property) {
    String s = property.get().trim();
    if (s.length() == 0) {
      return Collections.emptyList();
    }
    String[] values = s.split("\\s*,\\s*");
    return Arrays.asList(values);
  }
}
