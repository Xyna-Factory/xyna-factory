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
package com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt;

import java.io.Serializable;

import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementInterface.FileTransferDirection;


public class FileTransferAcknowledgment implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private final FileTransferDirection direction;
  private final Long transactionId;
  
  public FileTransferAcknowledgment(FileTransferDirection direction, Long transactionId) {
    this.direction = direction;
    this.transactionId = transactionId;
  }

  public FileTransferDirection getDirection() {
    return direction;
  }
  
  public Long getTransactionId() {
    return transactionId;
  }
  
  public static FileTransferAcknowledgment download(String originalFileName, Long transactionId) {
    return new Download(originalFileName, transactionId);
  }
  
  public static FileTransferAcknowledgment upload(String fileId, Long transactionId) {
    return new Upload(fileId, transactionId);
  }
  
  public static class Upload extends FileTransferAcknowledgment {

    private static final long serialVersionUID = 1L;
    
    private final String remoteFileId;
    
    public Upload(String fileId, Long transactionId) {
      super(FileTransferDirection.Upload, transactionId);
      this.remoteFileId = fileId;
    }
    
    public String getRemoteFileId() {
      return remoteFileId;
    }
    
  }
  
  
  public static class Download extends FileTransferAcknowledgment {
    
    private static final long serialVersionUID = 1L;
    
    private final String originalFileName;
    
    public Download(String originalFileName, Long transactionId) {
      super(FileTransferDirection.Download, transactionId);
      this.originalFileName = originalFileName;
    }

    public String getOriginalFileName() {
      return originalFileName;
    }
    
  }


  

}
