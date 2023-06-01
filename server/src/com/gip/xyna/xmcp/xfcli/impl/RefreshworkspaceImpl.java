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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BatchRepositoryEvent;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateManagement;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemStateReport;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Refreshworkspace;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.WorkflowProtectionMode;

public class RefreshworkspaceImpl extends XynaCommandImplementation<Refreshworkspace> {
  

  public void execute(OutputStream statusOutputStream, Refreshworkspace payload) throws XynaException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Workspace workspace;
    if (payload.getWorkspace() == null ||
        payload.getWorkspace().length() <= 0) {
      workspace = RevisionManagement.DEFAULT_WORKSPACE;
    } else {
      workspace = new Workspace(payload.getWorkspace());
    }
    long revision = revisionManagement.getRevision(workspace);
    final String savedMdmDir = RevisionManagement.getPathForRevision(PathType.XMOM, revision, false);
    Collection<String> allObjectNames;
    if (payload.getApplicationName() == null ||
        payload.getApplicationName().length() <= 0) {
      List<File> files = FileUtils.getMDMFiles(new File(savedMdmDir), new ArrayList<File>());
      allObjectNames = CollectionUtils.transformAndSkipNull(files, new Transformation<File, String>() {
        public String transform(File from) {
          String xmlName = from.getPath().substring(savedMdmDir.length() + 1).replaceAll(Constants.FILE_SEPARATOR, ".");
          xmlName = xmlName.substring(0, xmlName.length() - ".xml".length());
          if (GenerationBase.isReservedServerObjectByFqOriginalName(xmlName)) {
            return null;
          } else {
            return xmlName;
          }
        }
      });
    } else {
      String applicationName = payload.getApplicationName();
      String version = null; //workingset
      ApplicationManagementImpl appMgmt = (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
      List<ApplicationEntryStorable> appEntries =
                      appMgmt.listApplicationDetails(applicationName, version, true, Collections.<String>emptyList(), revision);
      if (appEntries == null) {
        writeLineToCommandLine(statusOutputStream, "Unknown application definition: '" + payload.getApplicationName() + "'");
        return;
      }
      allObjectNames = CollectionUtils.transformAndSkipNull(appEntries, new Transformation<ApplicationEntryStorable, String>() {
        public String transform(ApplicationEntryStorable from) {
          switch (from.getTypeAsEnum()) {
            case DATATYPE :
            case WORKFLOW :
            case EXCEPTION :
              if (GenerationBase.isReservedServerObjectByFqOriginalName(from.getName())) {
                return null;
              } else {
                return from.getName();
              }
            default :
              return null;
          }
        }
      });
    }
    allObjectNames = new HashSet<String>(allObjectNames);
    TemporarySessionAuthentication tsa =
        TemporarySessionAuthentication.tempAuthWithUniqueUserAndOperationLock("RefreshWS", TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE, revision,
                                                                              CommandControl.Operation.XMOM_SAVE);
    tsa.initiate();
    try {
      XynaMultiChannelPortal portal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
      writeLineToCommandLine(statusOutputStream, "Refreshing " + allObjectNames.size() + " items:");
      BatchRepositoryEvent repositoryEvent = new BatchRepositoryEvent(revision);
      try {
        for (String objectName : allObjectNames) {
          File file = new File (GenerationBase.getFileLocationOfXmlNameForSaving(objectName, revision) + ".xml");
          if (file.exists()) {
            String xml = FileUtils.readFileAsString(file);
            writeLineToCommandLine(statusOutputStream, "  " + objectName);
            try {
              portal.saveMDM(xml, true, tsa.getUsername(), tsa.getSessionId(), revision, repositoryEvent, true, true);
            } catch (Exception e) {
              writeLineToCommandLine(statusOutputStream, "    Failure: " + e.getMessage());
              writeStackTraceToCommandLine(statusOutputStream, e.getStackTrace());
            }
          }
        }
      } finally {
        repositoryEvent.execute("Refreshed all xmom objects in workspace.");
      }
      if (payload.getDeploy()) {
        writeLineToCommandLine(statusOutputStream, "Scanning for objects to deploy:");
        WorkflowProtectionMode protMode = WorkflowProtectionMode.BREAK_ON_USAGE;
        if (payload.getProtectionMode() != null && 
            payload.getProtectionMode().length() > 0) {
          try {
            protMode = WorkflowProtectionMode.getByIdentifier(payload.getProtectionMode());
          } catch (IllegalArgumentException e) {
            writeLineToCommandLine(statusOutputStream, "Unknown workflow protection mode: '" + payload.getProtectionMode() + "'");
            return;
          }
        }
        DeploymentItemStateManagement dism = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDeploymentItemStateManagement();
        List<GenerationBase> toDeploy = new ArrayList<GenerationBase>();
        for (String objectName : allObjectNames) {
          DeploymentItemState dis = dism.get(objectName, revision);
          if (dis != null) {
            DeploymentItemStateReport disr = dis.getStateReport();
            if (dis.exists()) {
              if (payload.getForce()) {
                writeLineToCommandLine(statusOutputStream, "  " + objectName + " is " + disr.getState());
                toDeploy.add(GenerationBase.getInstance(dis.getType(), objectName, revision));
              } else {
                switch (disr.getState()) {
                  case CHANGED :
                  case SAVED :
                    writeLineToCommandLine(statusOutputStream, "  " + objectName + " is " + disr.getState());
                    toDeploy.add(GenerationBase.getInstance(dis.getType(), objectName, revision));
                    break;
                  default :
                    // ntbd
                    break;
                }
              }
            }
          }
        }
        if (toDeploy.size() > 0) {
          writeLineToCommandLine(statusOutputStream, "Deploying " + toDeploy.size() + " object" + (toDeploy.size() <= 1 ? "" : "s"));
          for (GenerationBase gb : toDeploy) {
            gb.setDeploymentComment("CLI refreshworkspace");
          }
          GenerationBase.deploy(toDeploy, DeploymentMode.codeChanged, false, protMode);
        } else {
          writeLineToCommandLine(statusOutputStream, "No deploymentes necessary ");
        }
      }
    } finally {
      tsa.destroy();
    }
    
  }
  
}
