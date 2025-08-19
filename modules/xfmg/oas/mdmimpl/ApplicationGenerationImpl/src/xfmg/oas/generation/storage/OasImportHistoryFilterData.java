/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xfmg.oas.generation.storage;

import java.util.Optional;

import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;


public class OasImportHistoryFilterData {

  
  // filename, type, date, importstatus
  
  private String filename = "%";
  private String type = "%";
  private String date = "%";
  private String importStatus = "%";
  
  // to parameter (xyna db utils)
  // constr(tableinfo)
  
  public OasImportHistoryFilterData(TableInfo info) {
    init(info);
  }
  
  
  private void init(TableInfo info) {
    if (info == null) { return; }
    if (info.getColumns() == null) { return; }
    for (TableColumn col : info.getColumns()) {
      Optional<String> path = getTrimmedOrEmpty(col.getPath());
      if (path.isEmpty()) { continue; }
      Optional<String> filter = getTrimmedOrEmpty(col.getFilter());
      if (filter.isEmpty()) { continue; }
      
      if (OasImportHistoryConstants.PATH_FILENAME.equals(path.get())) { filename = filter.get(); }
      if (OasImportHistoryConstants.PATH_TYPE.equals(path.get())) { type = filter.get(); }
      if (OasImportHistoryConstants.PATH_DATE.equals(path.get())) { date = filter.get(); }
      if (OasImportHistoryConstants.PATH_IMPORTSTATUS.equals(path.get())) { importStatus = filter.get(); }
    }
  }
  
  
  // buildfilter (replace * %)
  
  
  private Optional<String> getTrimmedOrEmpty(String val) {
    if (val == null) { return Optional.empty(); }
    val = val.trim();
    if (val.isEmpty()) { return Optional.empty(); }
    return Optional.of(val);
  }

  
  public String getFilename() {
    return filename;
  }

  
  public String getType() {
    return type;
  }

  
  public String getDate() {
    return date;
  }

  
  public String getImportStatus() {
    return importStatus;
  }
  
}
