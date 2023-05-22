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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ExportApplicationBuildParameter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xmcp.RMIChannelImpl;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext;
import com.gip.xyna.xprc.xprcods.orderarchive.orderbackuphelper.MigrateRuntimeContext.MigrationTargets;


public class RuntimeContextManagementLanding implements InitializableRemoteInterface, RuntimeContextManagementInterface {

  private final static LocalRuntimeContextManagementSecurity lrcms = new LocalRuntimeContextManagementSecurity();
  
  public RuntimeContextManagementLanding() {
    
  }
  
  public void init(Object... initParameters) {
  }
  
  
  private Role authenticate(XynaCredentials credentials) throws RemoteException {
    return RMIChannelImpl.authenticate(credentials);
  }
  

  
  
  public List<RuntimeDependencyContextInformation> listRuntimeDependencyContexts(XynaCredentials credentials, ListRuntimeDependencyContextParameter lrdcp)
                  throws RemoteException, XynaException {
    Role role = authenticate(credentials);
    return lrcms.listRuntimeDependencyContexts(role, lrdcp);    
  }



  public void createRuntimeDependencyContext(XynaCredentials credentials, RuntimeDependencyContext rdc)
                  throws RemoteException, XynaException {
    Role role = authenticate(credentials);
    lrcms.createRuntimeDependencyContext(role, rdc);
  }


  public void deleteRuntimeDependencyContext(XynaCredentials credentials, RuntimeDependencyContext rdc)
                  throws RemoteException, XynaException {
    Role role = authenticate(credentials);
    lrcms.deleteRuntimeDependencyContext(role, rdc);
  }


  public void modifyRuntimeDependencyContext(XynaCredentials credentials, RuntimeDependencyContextInformation rdci)
                  throws RemoteException, XynaException {
    Role role = authenticate(credentials);
    lrcms.modifyRuntimeDependencyContext(role, rdci);
  }


  public MigrateRuntimeContext.MigrationResult migrateRuntimeContextDependencies(XynaCredentials credentials, RuntimeDependencyContext from,
                                                RuntimeDependencyContext to, Collection<MigrationTargets> targets,
                                                boolean force)
                  throws RemoteException, XynaException {
    Role role = authenticate(credentials);
    return lrcms.migrateRuntimeContextDependencies(role, from, to, targets, force);
  }


  public void importApplication(XynaCredentials credentials, ImportApplicationParameter iap, String fileId)
                  throws RemoteException, XynaException {
    Role role = authenticate(credentials);
    lrcms.importApplication(role, iap, fileId);
  }


  public String exportApplication(XynaCredentials credentials, Application application, ExportApplicationBuildParameter eabp)
                  throws RemoteException, XynaException {
    Role role = authenticate(credentials);
    return lrcms.exportApplication(role, application, eabp);
  }


  public void copyApplicationIntoWorkspace(XynaCredentials credentials, Application from, Workspace to)
                  throws RemoteException, XynaException {
    Role role = authenticate(credentials);
    lrcms.copyApplicationIntoWorkspace(role, from, to);
  }


  

}
