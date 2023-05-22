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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt;



import java.rmi.RemoteException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.BasicRMIInterFactoryLinkProfile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.InterFactoryLink.InterFactoryLinkProfileIdentifier;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;



public class RemoteFileManagementLinkProfile extends BasicRMIInterFactoryLinkProfile<RemoteFileManagementInterface> {


  public InterFactoryLinkProfileIdentifier getIdentifier() {
    return InterFactoryLinkProfileIdentifier.FileManagement;
  }


  protected GenericRMIAdapter<RemoteFileManagementInterface> getAdapter() throws RMIConnectionFailureException {
    return channel.<RemoteFileManagementInterface> getInterface(RemoteFileManagementInterface.BINDING_NAME);
  }


  public FileTransferAcknowledgment startFileTransfer(final XynaCredentials creds, final FileTransfer filetransfer)
      throws XFMG_NodeConnectException, XynaException {
    return exec(new RMICall<RemoteFileManagementInterface, FileTransferAcknowledgment>() {

      @Override
      public FileTransferAcknowledgment exec(RemoteFileManagementInterface rmi) throws RemoteException, XynaException {
        return rmi.startFileTransfer(creds, filetransfer);
      }
    });
  }


  public void sendFilePart(final XynaCredentials creds, final Long id, final byte[] content)
      throws XFMG_NodeConnectException, XynaException {
    exec(new RMICall<RemoteFileManagementInterface, Void>() {

      @Override
      public Void exec(RemoteFileManagementInterface rmi) throws RemoteException, XynaException {
        rmi.sendFilePart(creds, id, content);
        return null;
      }
    });
  }


  public byte[] receiveFilePart(final XynaCredentials creds, final Long id) throws XFMG_NodeConnectException, XynaException {
    return exec(new RMICall<RemoteFileManagementInterface, byte[]>() {

      @Override
      public byte[] exec(RemoteFileManagementInterface rmi) throws RemoteException, XynaException {
        return rmi.receiveFilePart(creds, id);
      }
    });
  }


  public void finishFileTransfer(final XynaCredentials creds, final Long id) throws XFMG_NodeConnectException, XynaException {
    exec(new RMICall<RemoteFileManagementInterface, Void>() {

      @Override
      public Void exec(RemoteFileManagementInterface rmi) throws RemoteException, XynaException {
        rmi.finishFileTransfer(creds, id);
        return null;
      }
    });
  }

}
