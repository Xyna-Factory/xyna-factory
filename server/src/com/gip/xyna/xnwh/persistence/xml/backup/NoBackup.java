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


public class NoBackup implements BackupManagerIfc {

  public File[] searchBackupFilesOfTable(String tn) {
    return new File[0];
  }

  public void registerDiscoveredBackupFile(String tablename, long txId, File backupFile) {}

  public void deleteObsoleteBackupFilesOfTable(String tablename) {}

  public boolean isDeletionOfJournalFileAllowed(long lastTxIdInJournal) {
    return true;
  }

  public void backupCurrentFile(String tablename, long txId) {}

  public void tryRestoreFromBackup(String tablename, File baseFile) {}

  public boolean newBackupDoesClearTable() {
    return false;
  }
}
