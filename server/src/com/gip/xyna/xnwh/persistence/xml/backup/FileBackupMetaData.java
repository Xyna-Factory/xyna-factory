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

package com.gip.xyna.xnwh.persistence.xml.backup;

import java.io.File;


public class FileBackupMetaData {

  private final Long _transactionId;
  private final File _file;
  private final long _timestamp;  
  private boolean _oldEnoughForDeletion = false;
  
  public FileBackupMetaData(long transactionId, File file) {
    _transactionId = new Long(transactionId);
    _file = file;
    _timestamp = System.currentTimeMillis();
  }
  
  public FileBackupMetaData(long transactionId, File file, long timestamp) {
    _transactionId = new Long(transactionId);
    _file = file;
    _timestamp = timestamp;
  }
  
  public Long getTransactionId() {
    return _transactionId;
  }
  
  public File getFile() {
    return _file;
  }
  
  public long getTimestamp() {
    return _timestamp;
  }
  
  synchronized public boolean isOldEnoughForDeletion() {    
    if (_oldEnoughForDeletion) {
      return true;
    }
    int minLifetime = BackupConfig.XynaProperty.BACKUP_FILE_MIN_LIFETIME.get();
    _oldEnoughForDeletion = (_timestamp + minLifetime*1000 < System.currentTimeMillis());    
    return _oldEnoughForDeletion;
  }
  
}
