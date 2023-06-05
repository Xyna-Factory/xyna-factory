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

import java.io.Serializable;

import com.gip.xyna.xfmg.xfctrl.nodemgmt.filemgmt.RemoteFileManagementInterface.FileTransferDirection;

public class FileTransfer implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private FileTransferDirection direction;
  
  public FileTransfer(FileTransferDirection direction) {
    this.direction = direction;
  }

  public FileTransferDirection getDirection() {
    return direction;
  }
  
  
  public static FileTransfer upload(String originalFileName) {
    return new Upload(originalFileName);
  }
  
  public static FileTransfer download(String remoteFileId) {
    return new Download(remoteFileId);
  }
  
  public static class Upload extends FileTransfer {

    private static final long serialVersionUID = 1L;
    
    private String originalFileName;
    
    public Upload(String originalFileName) {
      super(FileTransferDirection.Upload);
      this.originalFileName = originalFileName;
    }

    public String getOriginalFileName() {
      return originalFileName;
    }

  }
  
  public static class Download extends FileTransfer {
    
    private static final long serialVersionUID = 1L;
    
    private String fileId;
    
    public Download(String fileId) {
      super(FileTransferDirection.Download);
      this.fileId = fileId;
    }

    public String getFileId() {
      return fileId;
    }
    
  }
  
}