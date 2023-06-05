/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt;



import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ExportApplicationBuildParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.BasicRMIInterFactoryLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationResult;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationTargets;



public class RuntimeContextManagementLinkProfile extends BasicRMIInterFactoryLinkProfile<RuntimeContextManagementInterface> {


  public InterFactoryLinkProfileIdentifier getIdentifier() {
    return InterFactoryLinkProfileIdentifier.RuntimeContextManagement;
  }


  protected GenericRMIAdapter<RuntimeContextManagementInterface> getAdapter() throws RMIConnectionFailureException {
    return channel.<RuntimeContextManagementInterface> getInterface(RuntimeContextManagementInterface.BINDING_NAME);
  }


  public List<RuntimeDependencyContextInformation> listRuntimeDependencyContexts(final XynaCredentials credentials,
                                                                                 final ListRuntimeDependencyContextParameter lrdcp)
      throws XFMG_NodeConnectException, XynaException {
    return exec(new RMICall<RuntimeContextManagementInterface, List<RuntimeDependencyContextInformation>>() {

      @Override
      public List<RuntimeDependencyContextInformation> exec(RuntimeContextManagementInterface rmi) throws RemoteException, XynaException {
        return rmi.listRuntimeDependencyContexts(credentials, lrdcp);
      }
    });
  }


  public void createRuntimeDependencyContext(final XynaCredentials credentials, final RuntimeDependencyContext rdc)
      throws XFMG_NodeConnectException, XynaException {
    exec(new RMICall<RuntimeContextManagementInterface, Void>() {

      @Override
      public Void exec(RuntimeContextManagementInterface rmi) throws RemoteException, XynaException {
        rmi.createRuntimeDependencyContext(credentials, rdc);
        return null;
      }
    });
  }


  public void deleteRuntimeDependencyContext(final XynaCredentials credentials, final RuntimeDependencyContext rdc)
      throws XFMG_NodeConnectException, XynaException {
    exec(new RMICall<RuntimeContextManagementInterface, Void>() {

      @Override
      public Void exec(RuntimeContextManagementInterface rmi) throws RemoteException, XynaException {
        rmi.deleteRuntimeDependencyContext(credentials, rdc);
        return null;
      }
    });
  }


  public void modifyRuntimeDependencyContext(final XynaCredentials credentials, final RuntimeDependencyContextInformation rdci)
      throws XFMG_NodeConnectException, XynaException {
    exec(new RMICall<RuntimeContextManagementInterface, Void>() {

      @Override
      public Void exec(RuntimeContextManagementInterface rmi) throws RemoteException, XynaException {
        rmi.modifyRuntimeDependencyContext(credentials, rdci);
        return null;
      }
    });
  }


  public MigrationResult migrateRuntimeContextDependencies(final XynaCredentials credentials, final RuntimeDependencyContext from,
                                                           final RuntimeDependencyContext to, final Collection<MigrationTargets> targets,
                                                           final boolean force)
      throws XFMG_NodeConnectException, XynaException {
    return exec(new RMICall<RuntimeContextManagementInterface, MigrationResult>() {

      @Override
      public MigrationResult exec(RuntimeContextManagementInterface rmi) throws RemoteException, XynaException {
        return rmi.migrateRuntimeContextDependencies(credentials, from, to, targets, force);
      }
    });
  }


  public void importApplication(final XynaCredentials credentials, final ImportApplicationParameter iap, final String fileId)
      throws XFMG_NodeConnectException, XynaException {
    exec(new RMICall<RuntimeContextManagementInterface, Void>() {

      @Override
      public Void exec(RuntimeContextManagementInterface rmi) throws RemoteException, XynaException {
        rmi.importApplication(credentials, iap, fileId);
        return null;
      }
    });
  }


  public String exportApplication(final XynaCredentials credentials, final Application application,
                                  final ExportApplicationBuildParameter eabp)
      throws XFMG_NodeConnectException, XynaException {
    return exec(new RMICall<RuntimeContextManagementInterface, String>() {

      @Override
      public String exec(RuntimeContextManagementInterface rmi) throws RemoteException, XynaException {
        return rmi.exportApplication(credentials, application, eabp);
      }
    });
  }


  public void copyApplicationIntoWorkspace(final XynaCredentials credentials, final Application from, final Workspace to)
      throws XFMG_NodeConnectException, XynaException {
    exec(new RMICall<RuntimeContextManagementInterface, Void>() {

      @Override
      public Void exec(RuntimeContextManagementInterface rmi) throws RemoteException, XynaException {
        rmi.copyApplicationIntoWorkspace(credentials, from, to);
        return null;
      }
    });
  }


}
