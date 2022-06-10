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

package com.gip.xyna.xnwh.persistence.xml.backup;

import java.util.HashMap;
import java.util.Map;


public class BackupMetaDataStore {

  // map key is tableName
  private Map<String, TableBackupMetaData> _backupMap = 
          new HashMap<String, TableBackupMetaData>();
  
  
  public TableBackupMetaData getOrCreateMetaDataForTablename(String tablename) {    
    TableBackupMetaData ret = _backupMap.get(tablename);
    if (ret == null) {
      ret = new TableBackupMetaData();
      _backupMap.put(tablename, ret);
    }
    return ret;    
  }

  synchronized public long getOldestTransactionIdJournalMustKeep() {
    long tmp = -1L;
    for (TableBackupMetaData meta : _backupMap.values()) {
      long val = meta.getOldestTransactionIdJournalMustKeep();
      if (val > 0) {
        if ((tmp < 0) || (val < tmp)) {
          tmp = val;
        }
      }
    }
    return tmp;
  }
  
}
