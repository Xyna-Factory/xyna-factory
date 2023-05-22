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


package com.gip.xyna.xnwh.persistence.xml.backup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;


public class TableBackupMetaData {
   
  private static final Logger _logger = CentralFactoryLogging.getLogger(TableBackupMetaData.class); 
  private long _counter = 0;
  private TreeMap<Long, FileBackupMetaData> _fileMap = new TreeMap<Long, FileBackupMetaData>();
    
  private long incAndGetCounter() {
    _counter++;
    return _counter;
  }
  
   
  public FileBackupMetaData getNewestBackup() {
    Map.Entry<Long, FileBackupMetaData> newest = _fileMap.lastEntry();
    if (newest == null) {
      return null;
    }
    return newest.getValue();
  }
  
  
  public FileBackupMetaData buildAndRegisterNewFileMetaData(long txId, String baseFileName) {
    File file = new File(baseFileName + "." + incAndGetCounter() + BackupConfig.BACKUP_SUFFIX);
    FileBackupMetaData fileMeta = new FileBackupMetaData(txId, file);
    _fileMap.put(fileMeta.getTransactionId(), fileMeta);
    return fileMeta;
  }
  
  
  /**
   * registrieren vorhandener Backup-files (z.B. nach Factory-Neustart)
   */
  public void registerDiscoveredBackupFile(long txId, File backupFile) {
    FileBackupMetaData fileMeta = new FileBackupMetaData(txId, backupFile, -1L);
    _fileMap.put(fileMeta.getTransactionId(), fileMeta);
    updateCounterForDiscoveredFile(backupFile);
  }
  
  
  private void updateCounterForDiscoveredFile(File backupFile) {
    String filename = backupFile.getPath();
    long parsed = getCounterForDiscoveredFile(filename);
    if (parsed > _counter) {
      _counter = parsed;
    }
  }
  
  
  public long getCounterForDiscoveredFile(String filename) {
    String withoutSuffix = filename.substring(0, filename.length() - BackupConfig.BACKUP_SUFFIX.length());
    String[] parts = withoutSuffix.split("\\.");
    try {
      return Long.parseLong(parts[parts.length - 1]);
    } catch (Exception e) {
      _logger.info("Could not parse counter in backup file name " + filename);
    }
    return -1L;
  }
  
  
  public List<FileBackupMetaData> getFilesToDelete() {
    List<FileBackupMetaData> ret = new ArrayList<FileBackupMetaData>();    
    int index = 0;
    
    // von neuester TxID abwaerts zaehlen
    for (Long txId : _fileMap.descendingKeySet()) {
      index++;
      if (index == 1) {
        // neuestes backup-File nie loeschen
        continue;
      }
      FileBackupMetaData file = _fileMap.get(txId);
      if (file.isOldEnoughForDeletion()) {
        ret.add(file);
      }
    }
    return ret;
  }
  
  
  public void removeFromStoreAfterFileDeletion(FileBackupMetaData file) {
    _fileMap.remove(file.getTransactionId());
    _logger.trace("Removed backup " + file.getFile().getPath() + " from meta data store.");
  }
  
  public Long getOldestTransactionIdJournalMustKeep() {
    // TxID aufwaerts zaehlen
    for (Long txId : _fileMap.keySet()) {
      FileBackupMetaData file = _fileMap.get(txId);
      if (!file.isOldEnoughForDeletion()) {
        return txId;
      }
    }
    // wenn alle backups alt genug sind
    return new Long(-1L);
  }
    
}
