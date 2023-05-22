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
import java.io.FilenameFilter;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;


/**
 * Klasse ermoeglicht, dass fuer Dateien der XML-Persistence Backups angelegt werden.
 * 
 * Motivation: Bei Stromausfall in XFS-Dateisystem kann es vorkommen, dass kurz vorher 
 * geschriebene Dateien noch nicht wirklich auf der Festplatte waren und nach Neustart
 * leer sind.
 * Anscheinend passiert das nur, wenn eine vorhandene Datei komplett neu ueberschrieben wird
 * (wie das bisher bei der XML-Persistence passiert ist, das DOM wurde jedesmal komplett neu 
 * rausgeschrieben), aber nicht, wenn an eine vorhandene Datei nur etwas angehaengt wird 
 * (wie bei den Journal-Dateien).
 * 
 * Vorgehen: Bei Aenderungen an einer xml-Datei wird zunaechst die bisherige Datei 
 * umbenannt, d.h. sie erhaelt einen Backup-Suffix mit hochgezaehltem Index. 
 * (Annahme dabei: Umbenennen gefaehrdet die Datei nicht, sofern sie vorher alt genug war,
 * d.h. tatsaechlich auf Platte geschrieben wurde.)
 * Dann wird die Datei komplett mit dem aktuellen DOM neu geschrieben (in XMLPersistenceLayer.MemoryQueue)
 * 
 * Backup-Dateien, die ein konfigurierbares Alter erreicht haben, werden wieder geloescht.
 * Ausnahme: Die aktuell neueste Backup-Datei fuer die jeweilige Tabelle wird nicht geloescht.
 * 
 * Beim Server-Start werden vorhandene Backup-Dateien gesucht und registriert.
 * 
 * Wenn (z.B. eben nach Stromausfall) der Fall eintritt, dass die eigentliche XML-Datei leer
 * ist, wird das Backup eingespielt, indem die Backup-Datei umbenannt wird und die leere Datei
 * ueberschreibt.
 * 
 * Beim Loeschen von Journal-Dateien wird vorher abgefragt, ob die neueste TransactionID in 
 * ihr noch in irgendeiner "gefaehrdeten" Backup-Datei vorkommt, die noch nicht das "sichere"
 * Alter erreicht hat (d.h. ab dem sie dann auch geloescht werden darf).
 * In diesem Fall wird das Journal eventuell noch zum Rekonstruieren der verlorenen Daten bei
 * Stromausfall gebraucht und darf noch nicht geloescht werden.  
 * 
 * Backup-Funktionalitaet wird an-/ausgeschaltet ueber System-Property
 * xnwh.persistence.xml.backup.enabled, gesetzt in Konfig-File black_edition_&lt;n&gt;.properties:
 * jvm.option.xml.backup=-Dxnwh.persistence.xml.backup.enabled=true
 */
public class BackupManager implements BackupManagerIfc {
    
  public interface FilePathBuilder {
    String getFilePathByTableName(String tableName);
  }
  
  protected static class BackupFileFilter implements FilenameFilter {
    private final String _origFileNameWithoutPathPlusDot;
    public BackupFileFilter(File file) {
      this._origFileNameWithoutPathPlusDot = file.getName() + ".";
    }
    public boolean accept(File dir, String name) {
      if (!name.endsWith(BackupConfig.BACKUP_SUFFIX)) {
        return false;
      }
      if (name.startsWith(this._origFileNameWithoutPathPlusDot)) {
        return true;
      }
      return false;
    }    
  }
  
  
  private static final Logger _logger = CentralFactoryLogging.getLogger(BackupManager.class);
  private BackupMetaDataStore _metadataStore = new BackupMetaDataStore();
  private final FilePathBuilder _filePathBuilder;
  
  public BackupManager(FilePathBuilder filePathBuilder) {
    _filePathBuilder = filePathBuilder;
  }
  
  /**
   * Gefundene backup-files werden hier noch nicht registriert;
   * (dafuer muss erst die TxId aus dem geparsten xml geholt werden)
   */
  synchronized public File[] searchBackupFilesOfTable(String tableName) {
    String baseFileName = _filePathBuilder.getFilePathByTableName(tableName);
    File baseFile = new File(baseFileName);
    BackupFileFilter filter = new BackupFileFilter(baseFile);
    File dir = baseFile.getParentFile().getAbsoluteFile();
    if ((dir == null) || (!dir.exists())) {
      _logger.trace("Could not find directory for xml persistence file: " + baseFileName);
      return new File[0];
    }
    return dir.listFiles(filter);    
  }
  
  
  synchronized public void registerDiscoveredBackupFile(String tablename, long txId, File backupFile) { 
    _logger.trace("Registering backup file " + backupFile.getPath());   
    TableBackupMetaData metaForTable = this._metadataStore.getOrCreateMetaDataForTablename(tablename);
    metaForTable.registerDiscoveredBackupFile(txId, backupFile);
  }
  
    
  synchronized public void deleteObsoleteBackupFilesOfTable(String tablename) {
    _logger.trace("Entering deleteObsoleteBackupFilesOfTable for " + tablename);
    TableBackupMetaData metaForTable = this._metadataStore.getOrCreateMetaDataForTablename(tablename);
    List<FileBackupMetaData> list = metaForTable.getFilesToDelete();
    for (FileBackupMetaData metaForFile : list) {
      boolean deleted = false;
      try {
        _logger.debug("Deleting backup file " + metaForFile.getFile().getPath());
        deleted = metaForFile.getFile().delete();
      } catch (Exception e) {
        _logger.warn("Error trying to delete backup file " + metaForFile.getFile().getPath() + ": " + 
                     e.getMessage(), e);
      }
      if (deleted) {
        metaForTable.removeFromStoreAfterFileDeletion(metaForFile);
      } else {
        _logger.warn("Could not delete backup file: " + metaForFile.getFile().getPath());
      }
    }
  }
  
      
  synchronized public boolean isDeletionOfJournalFileAllowed(long lastTxIdInJournal) {
    long oldest = this._metadataStore.getOldestTransactionIdJournalMustKeep();
    if (oldest < 0) {
      return true;
    }
    return (lastTxIdInJournal < oldest);
  }
  
  
  synchronized public void backupCurrentFile(String tablename, long txId) {    
    String baseFileName = _filePathBuilder.getFilePathByTableName(tablename);
    TableBackupMetaData metaForTable = this._metadataStore.getOrCreateMetaDataForTablename(tablename);
    FileBackupMetaData metaForFile = metaForTable.buildAndRegisterNewFileMetaData(txId, baseFileName);
    boolean success = false;
    try {
      File orig = new File(baseFileName);
      if (!orig.exists()) {
        _logger.warn("File not found: " + orig.getPath());
        return;
      }
      _logger.debug("Creating backup file " + metaForFile.getFile().getPath());
      success = orig.renameTo(metaForFile.getFile());
    } catch (Exception e) {
      _logger.warn("Error trying to create file " + metaForFile.getFile().getPath() + ": " + e.getMessage(), e); 
    }
    if (!success) {
      _logger.warn("Could not create file " + metaForFile.getFile().getPath());
    }
  }
  
  
  synchronized public void tryRestoreFromBackup(String tablename, File baseFile) {
    _logger.trace("Entering tryRestoreFromBackup for " + baseFile.getPath());
    TableBackupMetaData metaForTable = this._metadataStore.getOrCreateMetaDataForTablename(tablename);
    FileBackupMetaData newest = metaForTable.getNewestBackup();
    if (newest == null) {
      return;
    }
    File file = newest.getFile();
    boolean success = false;
    try {
      _logger.debug("Will try to restore file from backup file: " + file.getPath());
      success = file.renameTo(baseFile);
      if (!success) {
        _logger.warn("Trying to restore file from backup file failed: " + file.getPath()); 
      }
    } catch (Exception e) {
      _logger.warn("Error trying to restore file from backup file: " + file.getPath() + ": " + e.getMessage(), e); 
    }
    if (success) {
      metaForTable.removeFromStoreAfterFileDeletion(newest);
    }
  }
  
  
  public boolean newBackupDoesClearTable() {
    return true;
  }
  
}
