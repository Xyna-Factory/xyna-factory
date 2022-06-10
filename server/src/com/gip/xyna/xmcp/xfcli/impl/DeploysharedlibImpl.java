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
import java.io.PrintStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.OrderEntryInterfacesCouldNotBeClosedException;
import com.gip.xyna.xfmg.xfctrl.classloading.SharedLibDeploymentAlgorithm;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Deploysharedlib;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class DeploysharedlibImpl extends XynaCommandImplementation<Deploysharedlib> {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(DeploysharedlibImpl.class);

  public void execute(OutputStream statusOutputStream, Deploysharedlib payload) throws XynaException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision;
    try {
      revision = revisionManagement.getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      writeLineToCommandLine(statusOutputStream, "The specified application/version or workspace does not exist.");
      return;
    }
    String sharedLibName  = payload.getSharedLibName();
    if (sharedLibName == null || sharedLibName.length() <= 0) {
      writeLineToCommandLine(statusOutputStream, "Could not deploy sharedLib, no name given.");
      return;
    }
    
    if (revisionManagement.isWorkspaceRevision(revision)) {
      File savedSharedLibFolder = new File(RevisionManagement.getPathForRevision(PathType.SHAREDLIB, revision, false) + payload.getSharedLibName());
      if (!savedSharedLibFolder.exists()) {
        writeLineToCommandLine(statusOutputStream, "No saved version of " + payload.getSharedLibName() + " could be found, reloading deployed shared lib.");
      }
    }
    
    CommandControl.tryLock(CommandControl.Operation.SHAREDLIB_RELOAD, revision);
    CommandControl.unlock(CommandControl.Operation.SHAREDLIB_RELOAD, revision); //kurz danach wird writelock geholt, das kann nicht upgegraded werden
    try {
      SharedLibDeploymentAlgorithm.deploySharedLib(sharedLibName, revision);
      writeLineToCommandLine(statusOutputStream, "Deployment finished. Dependent triggers and filters were redeployed." +
                      " Please check their status with listtriggers and listfilters.");
    } catch (XFMG_ClassLoaderRedeploymentException e) {
      logger.warn("Failed to deploy shared lib " + payload.getSharedLibName(), e);
      writeLineToCommandLine(statusOutputStream, "Failed to deploy shared lib " + payload.getSharedLibName());
      e.printStackTrace(new PrintStream(statusOutputStream));
    } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
      writeLineToCommandLine(statusOutputStream, "Specified shared lib <" + payload.getSharedLibName() + "> does not exist.");
    } catch (OrderEntryInterfacesCouldNotBeClosedException e) {
      logger.warn("Failed to deploy shared lib " + payload.getSharedLibName(), e);
      writeLineToCommandLine(statusOutputStream, "Failed to deploy shared lib " + payload.getSharedLibName());
      e.printStackTrace(new PrintStream(statusOutputStream));
    }
    
  }

}
