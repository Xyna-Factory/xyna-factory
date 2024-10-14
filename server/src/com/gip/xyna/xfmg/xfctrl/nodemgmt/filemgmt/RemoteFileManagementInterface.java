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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;


public interface RemoteFileManagementInterface extends Remote {
  
  public final static String BINDING_NAME = "RemoteFileManagementInterface";

  public FileTransferAcknowledgment startFileTransfer(XynaCredentials creds, FileTransfer transfer) throws RemoteException, XynaException;
  
  public void sendFilePart(XynaCredentials creds, Long transactionId, byte[] content) throws RemoteException, XynaException;
  
  public byte[] receiveFilePart(XynaCredentials creds, Long transactionId) throws RemoteException, XynaException;
  
  public void finishFileTransfer(XynaCredentials creds, Long transactionId) throws RemoteException, XynaException;
  
  
  public static enum FileTransferDirection {
    Upload {

      @SuppressWarnings("unchecked")
      @Override
      public FileTransfer.Upload cast(FileTransfer ft) {
        return FileTransfer.Upload.class.cast(ft);
      }

      @SuppressWarnings("unchecked")
      @Override
      public FileTransferAcknowledgment.Upload castAck(FileTransferAcknowledgment ft) {
        return FileTransferAcknowledgment.Upload.class.cast(ft);
      }
    }, 
    Download {

      @SuppressWarnings("unchecked")
      @Override
      public FileTransfer.Download  cast(FileTransfer ft) {
        return FileTransfer.Download.class.cast(ft);
      }

      @SuppressWarnings("unchecked")
      @Override
      public FileTransferAcknowledgment.Download castAck(FileTransferAcknowledgment ft) {
        return FileTransferAcknowledgment.Download.class.cast(ft);
      }
    };
    
    public abstract <F extends FileTransfer> F cast(FileTransfer ft);
    
    public abstract <F extends FileTransferAcknowledgment> F castAck(FileTransferAcknowledgment ft);
  }
  
}
