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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.Arrays;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementInterface.FileTransferDirection;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;

public class RemoteFileManagementUtils {

  public static String upload(String nodename, String localFileId, RemoteFileManagementLinkProfile remoteFileMgmt,
                       XynaCredentials creds)
                  throws RemoteException, XynaException {
    FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    TransientFile file = fm.retrieve(localFileId);
    String originalName = file.getOriginalFilename();

    FileTransferAcknowledgment ack = remoteFileMgmt.startFileTransfer(creds, FileTransfer.upload(originalName));
    FileTransferAcknowledgment.Upload upAck = FileTransferDirection.Upload.castAck(ack);
    InputStream is = file.openInputStream();
    try {
      byte[] buffer = new byte[RemoteFileManagementLanding.BUFFERSIZE];
      int bytes = is.read(buffer);
      while (bytes >= 0) {
        byte[] subBytes = Arrays.copyOfRange(buffer, 0, bytes);
        remoteFileMgmt.sendFilePart(creds, upAck.getTransactionId(), subBytes);
        bytes = is.read(buffer);
      }
    } catch (IOException e) {
      throw new Ex_FileAccessException(originalName, e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        throw new Ex_FileAccessException(originalName, e);
      }
    }

    remoteFileMgmt.finishFileTransfer(creds, upAck.getTransactionId());
    return upAck.getRemoteFileId();
  }
  
  
  public static String download(String nodename, String remoteFileId, RemoteFileManagementLinkProfile remoteFileMgmt, XynaCredentials creds) throws RemoteException, XynaException {
    
    FileManagement fm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement();
    FileTransferAcknowledgment ack = remoteFileMgmt.startFileTransfer(creds, FileTransfer.download(remoteFileId));
    FileTransferAcknowledgment.Download downAck = FileTransferDirection.Download.castAck(ack);
    
    Triple<String, OutputStream, String> triple = fm.store(RemoteFileManagementLanding.REMOTE_FILE_UPLOAD_LOCATION, downAck.getOriginalFileName());
    try {
      byte[] part = remoteFileMgmt.receiveFilePart(creds, downAck.getTransactionId());
      while (part.length > 0) {
        try {
          triple.getSecond().write(part);
        } catch (IOException e) {
          throw new Ex_FileAccessException(downAck.getOriginalFileName(), e);
        }
        part = remoteFileMgmt.receiveFilePart(creds, downAck.getTransactionId());
      }
    } finally {
      try {
        triple.getSecond().close();
      } catch (IOException e) {
        throw new Ex_FileAccessException(downAck.getOriginalFileName(), e);
      }
    }
    
    return triple.getFirst();
  }
  
}
