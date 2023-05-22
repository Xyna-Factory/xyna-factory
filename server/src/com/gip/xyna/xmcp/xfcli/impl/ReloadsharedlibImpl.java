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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_ClassLoaderRedeploymentException;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.OrderEntryInterfacesCouldNotBeClosedException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Reloadsharedlib;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class ReloadsharedlibImpl extends XynaCommandImplementation<Reloadsharedlib> {

  private static final Logger logger = CentralFactoryLogging.getLogger(ReloadsharedlibImpl.class);


  public void execute(OutputStream statusOutputStream, Reloadsharedlib payload) {
    
    Long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      writeLineToCommandLine(statusOutputStream, "The specified application/version or workspace does not exist.");
      return;
    }
    
    CommandControl.tryLock(CommandControl.Operation.SHAREDLIB_RELOAD, revision);
    CommandControl.unlock(CommandControl.Operation.SHAREDLIB_RELOAD, revision); //kurz danach wird writelock geholt, das kann nicht upgegraded werden
    try {
      factory.getFactoryManagementPortal().getXynaFactoryControl().getClassLoaderDispatcher()
          .reloadSharedLib(payload.getSharedLibName(), revision);
      writeLineToCommandLine(statusOutputStream, "Reloading finished. Dependent triggers and filters were redeployed." +
           " Please check their status with listtriggers and listfilters.");
    } catch (XFMG_ClassLoaderRedeploymentException e) {
      logger.warn("Failed to reload shared lib " + payload.getSharedLibName(), e);
      writeLineToCommandLine(statusOutputStream, "Failed to reload shared lib " + payload.getSharedLibName());
      e.printStackTrace(new PrintStream(statusOutputStream));
    } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
      writeLineToCommandLine(statusOutputStream, "Specified shared lib <" + payload.getSharedLibName() + "> does not exist.");
    } catch (OrderEntryInterfacesCouldNotBeClosedException e) {
      logger.warn("Failed to reload shared lib " + payload.getSharedLibName(), e);
      writeLineToCommandLine(statusOutputStream, "Failed to reload shared lib " + payload.getSharedLibName());
      e.printStackTrace(new PrintStream(statusOutputStream));
    }
  }

}
