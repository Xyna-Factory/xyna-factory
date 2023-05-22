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

import java.rmi.RemoteException;
import java.util.EnumSet;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotImportApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotRemoveApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStartApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CouldNotStopApplication;
import com.gip.xyna.xfmg.exceptions.XFMG_CronLikeOrderCopyException;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateVersionForApplicationName;
import com.gip.xyna.xfmg.exceptions.XFMG_RunningOrdersException;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.InitializableRemoteInterface;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.CopyCLOResult;
import com.gip.xyna.xfmg.xfctrl.appmgmt.OrderEntrance.OrderEntranceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain;
import com.gip.xyna.xprc.exceptions.XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState;
import com.gip.xyna.xprc.xfractwfe.OrdersInUse;
import com.healthmarketscience.rmiio.RemoteInputStream;


public class ApplicationRemoteImpl implements ApplicationRemoteInterface, InitializableRemoteInterface {
  
  ApplicationManagementImpl applicationManagementImpl;

  public ApplicationState getApplicationStateRemotely(String applicationName, String versionName) throws RemoteException {
    return applicationManagementImpl.getApplicationStateRemotely(applicationName, versionName);
  }

  public Boolean isApplicationInUseRemotely(String applicationName, String versionName, boolean force) throws RemoteException, XFMG_RunningOrdersException {
    return applicationManagementImpl.isApplicationInUseRemotely(applicationName, versionName, force);
  }

  public String startApplicationRemotely(String applicationName, String versionName, StartApplicationParameters params) throws RemoteException,
                  XFMG_CouldNotStartApplication {
    return applicationManagementImpl.startApplicationRemotely(applicationName, versionName, params);
  }

  public String stopApplicationRemotely(String applicationName, String versionName) throws RemoteException,
                  XFMG_CouldNotStopApplication {
    return stopApplicationRemotely(applicationName, versionName, null);
  }

  public String stopApplicationRemotely(String applicationName, String versionName,
                                        EnumSet<OrderEntranceType> onlyOpenOrderEntranceTypes) throws RemoteException,
                  XFMG_CouldNotStopApplication {
    return applicationManagementImpl.stopApplicationRemotely(applicationName, versionName, onlyOpenOrderEntranceTypes);
  }

  public String importApplicationRemotely(boolean force, boolean excludeXynaProperties, boolean excludeCapacities,
                                        boolean importOnlyXynaProperties, boolean importOnlyCapacities,
                                        RemoteInputStream applicationPackageStream) throws RemoteException,
                  XFMG_CouldNotImportApplication, XFMG_DuplicateVersionForApplicationName,
                  XFMG_CouldNotRemoveApplication, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
                  XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    return importApplicationRemotely(force, excludeXynaProperties, excludeCapacities, importOnlyXynaProperties, importOnlyCapacities, false, applicationPackageStream); 
  }

  public String importApplicationRemotely(boolean force, boolean excludeXynaProperties, boolean excludeCapacities,
                                          boolean importOnlyXynaProperties, boolean importOnlyCapacities,
                                          boolean regenerateCode, RemoteInputStream applicationPackageStream)
                  throws RemoteException, XFMG_CouldNotImportApplication, XFMG_DuplicateVersionForApplicationName,
                  XFMG_CouldNotRemoveApplication, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
                  XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    return importApplicationRemotely(force, excludeXynaProperties, excludeCapacities, importOnlyXynaProperties, importOnlyCapacities, false, null, applicationPackageStream); 
  }

  public String importApplicationRemotely(boolean force, boolean excludeXynaProperties, boolean excludeCapacities,
                                          boolean importOnlyXynaProperties, boolean importOnlyCapacities,
                                          boolean regenerateCode, String user, RemoteInputStream applicationPackageStream)
                  throws RemoteException, XFMG_CouldNotImportApplication, XFMG_DuplicateVersionForApplicationName,
                  XFMG_CouldNotRemoveApplication, XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryChangeState,
                  XPRC_ChangeCapacityCardinalityFailedTooManyInuse_TryAgain {
    return applicationManagementImpl.importApplicationRemotely(force, excludeXynaProperties, excludeCapacities,
                                                               importOnlyXynaProperties, importOnlyCapacities,
                                                               regenerateCode, user, applicationPackageStream);
  }

  public String removeApplicationVersionRemotely(String applicationName, String versionName, RemoveApplicationParameters params) throws RemoteException,
                  XFMG_CouldNotRemoveApplication {
    
    return applicationManagementImpl.removeApplicationVersionRemotely(applicationName, versionName, params);
  }
  
  public void notifyRequestIsFinishedRemotly(String requestId, Throwable throwableObject) throws RemoteException {
    applicationManagementImpl.notifyRequestIsFinishedRemotly(requestId, throwableObject);    
  }
  
  public void init(Object... initParameters) {
    applicationManagementImpl = (ApplicationManagementImpl) initParameters[0];
  }

  public OrdersInUse listActiveOrdersRemotely(String applicationName, String versionName, boolean verbose) throws RemoteException {
    return applicationManagementImpl.listActiveOrdersRemotely(applicationName, versionName, verbose);
  }

  public CopyCLOResult copyCronLikeOrders(RuntimeContext source, RuntimeContext target, String id, String[] ordertypes,
                                          boolean move)
                  throws RemoteException, XFMG_CronLikeOrderCopyException {
    return applicationManagementImpl.copyCronLikeOrders(source, target, id, ordertypes, move);
  }

}
