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
package com.gip.xyna.xfmg.xfctrl.appmgmt;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ClearWorkingSet.RevisionContentBlackWhiteList;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.ApplicationName;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


//TODO wenn mehrere workingsets unterst�tzt werden, funktioniert das so nicht mehr
public class WorkingSetBlackListXynaProperties implements RevisionContentBlackWhiteList {


  private static final Logger logger = CentralFactoryLogging.getLogger(WorkingSetBlackListXynaProperties.class);

  private static final XynaPropertyString triggerinstancesProp;
  private static final XynaPropertyString triggersProp;
  private static final XynaPropertyString filterinstancesProp;
  private static final XynaPropertyString filtersProp;
  private static final XynaPropertyString sharedLibsProp;
  private static final XynaPropertyString xmomobjectsProp;
  private static final XynaPropertyString applicationsProp;

  // ------------------ ACHTUNG: die namen dieser properties sind auch in ClearWorkingSet.xml aufgef�hrt
  
  static {
    triggerinstancesProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.triggerinstances", "");
    triggerinstancesProp
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "Comma separated list of trigger instance names to be excluded when clearing the default workspace.");
    triggersProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.triggers", "");
    triggersProp.setDefaultDocumentation(DocumentationLanguage.EN,
                                         "Comma separated list of trigger names to be excluded when clearing the default workspace.");
    filterinstancesProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.filterinstances", "");
    filterinstancesProp
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "Comma separated list of filter instance names to be excluded when clearing the default workspace.");
    filtersProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.filters", "");
    filtersProp.setDefaultDocumentation(DocumentationLanguage.EN,
                                        "Comma separated list of filter names to be excluded when clearing the default workspace.");
    xmomobjectsProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.xmomobjects", "");
    xmomobjectsProp
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "Comma separated list of fully qualified XMOM names to be excluded when clearing the default workspace.");
    applicationsProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.applications", "");
    applicationsProp.setDefaultDocumentation(DocumentationLanguage.EN,
                                             "Comma separated list of application names to be excluded when clearing the default workspace.");
    sharedLibsProp =
        new XynaPropertyString("xfmg.xfctrl.appmgmt.clearworkingset.blacklist.sharedlibs", "");
    sharedLibsProp.setDefaultDocumentation(DocumentationLanguage.EN,
                                           "Comma separated list of shared libs to be excluded when clearing the default workspace.");
  }
  
  public static void registerPropertiesDependency(String user) {
    triggerinstancesProp.registerDependency(user);
    triggersProp.registerDependency(user);
    filterinstancesProp.registerDependency(user);
    filtersProp.registerDependency(user);
    sharedLibsProp.registerDependency(user);
    xmomobjectsProp.registerDependency(user);
    applicationsProp.registerDependency(user);
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
    String[] values = property.get().trim().split("\\s*,\\s*");
    return Arrays.asList(values);
  }


  public static XynaPropertyString getTriggerinstancesprop() {
    return triggerinstancesProp;
  }


  public static XynaPropertyString getTriggersprop() {
    return triggersProp;
  }


  public static XynaPropertyString getFilterinstancesprop() {
    return filterinstancesProp;
  }


  public static XynaPropertyString getFiltersprop() {
    return filtersProp;
  }


  public static XynaPropertyString getSharedlibsprop() {
    return sharedLibsProp;
  }


  public static XynaPropertyString getXmomobjectsprop() {
    return xmomobjectsProp;
  }


  public static XynaPropertyString getApplicationsprop() {
    return applicationsProp;
  }
  
}
