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
package com.gip.xyna.xdev;



import java.rmi.Remote;
import java.rmi.RemoteException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringActionParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.refactoring.RefactoringResult;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.parameters.InstantiateRepositoryAccessParameters;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyAccess;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;



public interface XynaDevelopmentFactoryRMI extends Remote {

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public RefactoringResult refactorXMOM(XynaCredentials credentials, RefactoringActionParameter action) throws RemoteException, XynaException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public boolean lockXMOM(XynaCredentials credentials, String creator, String path, String type) throws RemoteException, XynaException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public boolean lockXMOM(XynaCredentials credentials, String creator, String path, String type, RuntimeContext runtimeContext) throws RemoteException, XynaException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public boolean unlockXMOM(XynaCredentials credentials, String creator, String path, String type) throws RemoteException, XynaException;
  
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public boolean unlockXMOM(XynaCredentials credentials, String creator, String path, String type, RuntimeContext runtimeContext) throws RemoteException, XynaException;
  
  @Deprecated
  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void publishXMOM(XynaCredentials credentials, String creator, String path, String type, String payload, Long autosaveCounter) throws RemoteException, XynaException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void publishXMOM(XynaCredentials credentials, String creator, String path, String type, String payload, Long autosaveCounter, RuntimeContext runtimeContext) throws RemoteException, XynaException;

  @ProxyAccess(right = ProxyRight.EDIT_MDM)
  public void instantiateRepositoryAccessInstance(XynaCredentials credentials, InstantiateRepositoryAccessParameters parameters, RuntimeContext runtimeContext) throws RemoteException, XynaException;

}
