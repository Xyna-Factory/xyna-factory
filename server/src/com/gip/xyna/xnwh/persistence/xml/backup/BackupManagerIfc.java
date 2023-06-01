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


public interface BackupManagerIfc {

  File[] searchBackupFilesOfTable(String baseFileName);
    
  void registerDiscoveredBackupFile(String tablename, long txId, File backupFile);
      
  void deleteObsoleteBackupFilesOfTable(String tablename);
        
  boolean isDeletionOfJournalFileAllowed(long lastTxIdInJournal);
    
  void backupCurrentFile(String tablename, long txId);
  
  void tryRestoreFromBackup(String tablename, File baseFile);
  
  /**
   * Flag, ob Aufruf backupCurrentFile() bewirkt, dass der bisherige Inhalt der Tabelle verschwindet 
   * (persistiert in Datei, aber nicht in memoryQueue) und somit Aufruf von con.deleteAll(clazz) 
   * unnotetig wird;
   * dies in BackupManager dadurch, dass die bisherige XML-Datei beim backup umbenannt wird und
   * folglich kurzzeitig keine "richtige" Datei fuer die Tabelle vorhanden ist, d.h. die Tabelle effektiv 
   * leer ist   
   */
  boolean newBackupDoesClearTable();
}
