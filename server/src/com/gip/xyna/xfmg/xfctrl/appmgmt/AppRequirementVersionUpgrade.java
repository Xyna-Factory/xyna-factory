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

package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.update.Version;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.RuntimeContextRequirementXmlEntry;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class AppRequirementVersionUpgrade {

  private static final Logger _logger = CentralFactoryLogging.getLogger(AppRequirementVersionUpgrade.class);
  
  
  public void execute(ODSConnection con, ApplicationXmlEntry applicationXml, ApplicationManagementImpl appMgmt,
                      PrintStream statusOutputStream) throws PersistenceLayerException {
    boolean wasChanged = false;
    List<RuntimeContextRequirementXmlEntry> modifiedList = new ArrayList<RuntimeContextRequirementXmlEntry>();
    for (RuntimeContextRequirementXmlEntry req : applicationXml.getRuntimeContextRequirements()) {
      RuntimeContextRequirementXmlEntry entry = handleRequirement(con, appMgmt, req, statusOutputStream);
      if (entry == null) {
        continue;
      }
      modifiedList.add(entry);
      if (entry != req) {
        wasChanged = true;
      }
    }
    if (wasChanged) {
      _logger.debug("Requirements were changed, replacing requirements list");
      applicationXml.getRuntimeContextRequirements().clear();
      applicationXml.getRuntimeContextRequirements().addAll(modifiedList);
    }    
  }

  
  /**
   * For every requirement: Check if required application is present;
   * if not, determine the version of the replacement
   * @throws PersistenceLayerException 
   */
  private RuntimeContextRequirementXmlEntry handleRequirement(ODSConnection con, ApplicationManagementImpl appMgmt, 
                                                              RuntimeContextRequirementXmlEntry req,
                                                              PrintStream statusOutputStream) throws PersistenceLayerException {
    String name = req.getApplication();
    String version = req.getVersion();    
    _logger.debug("Checking need for requirement upgrade for " + name + " " + version);
    if (version == null) {
      return req;
    }
    boolean exists = applicationVersionExists(name, version, con, appMgmt);
    if (exists) {
      return req;
    }
    
    List<? extends ApplicationStorable> appList = appMgmt.queryRuntimeApplicationStorableList(name, con);
    if (appList.size() == 0) {
      return req;
    }
    ApplicationStorable highest = determineHighestVersion(appList);
    if (highest == null) {
      return req;
    }
    handleWarnings(req, highest, statusOutputStream);
    RuntimeContextRequirementXmlEntry modified = new RuntimeContextRequirementXmlEntry(highest.getName(),
                                                                                       highest.getVersion(),
                                                                                       req.getWorkspace());
    _logger.debug("Determined replacement requirement: " +  modified.getApplication() + " " +
                  modified.getVersion());                                          
    return modified;    
  }
  
  
  private void handleWarnings(RuntimeContextRequirementXmlEntry req, ApplicationStorable highest,
                              PrintStream statusOutputStream) {    
    Version originalVersion = new Version(req.getVersion());
    Version highestVersion = new Version(highest.getVersion());
    if (originalVersion.isStrictlyGreaterThan(highestVersion)) {
      String warning = "Warning: Required runtime context '" + req.getApplication() + "' was downgraded from version '" + 
                       req.getVersion() + "' to '" + highest.getVersion() + "'";
      ApplicationManagementImpl.output(statusOutputStream, warning);
      _logger.warn(warning);
      return;
    } 
    boolean firstPartsEqual = false;    
    if ((originalVersion.length() > 2) && (highestVersion.length() > 2)) {
      if (originalVersion.getPart(0).equals(highestVersion.getPart(0))) {
        if (originalVersion.getPart(1).equals(highestVersion.getPart(1))) {
          firstPartsEqual = true;
        }
      }
    }
    if (!firstPartsEqual) {
      String warning = "Warning: Required runtime context '" + req.getApplication() + "' was upgraded from version '" + 
                       req.getVersion() + "' to possibly incompatible version '" + highest.getVersion() + "'";
      _logger.warn(warning);
      ApplicationManagementImpl.output(statusOutputStream, warning);
    }
  }
  
    
  private ApplicationStorable determineHighestVersion(List<? extends ApplicationStorable> appList) {
    Version currentHighest = new Version("0");
    ApplicationStorable currentSelection = null;
    
    for (ApplicationStorable app : appList) {
      String version = app.getVersion();
      _logger.debug("Checking app version: " + app.getName() + " " + version);
      if ((version == null) || (version.trim().length() < 1)) {
        continue;
      }
      Version checkVersion = new Version(app.getVersion());
      if (checkVersion.isStrictlyGreaterThan(currentHighest)) {
        currentHighest = checkVersion;
        currentSelection = app;
        _logger.debug("Found next higher version: " + app.getName() + " " + currentHighest.getString());
      }
    }
    if (currentSelection == null && appList.size() > 0) {
      currentSelection = appList.get(0);
    }
    return currentSelection;
  }
  
  
  private boolean applicationVersionExists(String name, String version, ODSConnection con, 
                                           ApplicationManagementImpl appMgmt) throws PersistenceLayerException {
    ApplicationStorable app = appMgmt.queryRuntimeApplicationStorable(name, version, con);
    if (app != null) {
      return true;
    }
    return false;
  }
  
}
