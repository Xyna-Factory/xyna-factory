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

import java.util.List;

import xmcp.oas.fman.storables.OAS_ImportHistory;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;


public class OasImportHistorySortTool {
  
  public void sort(List<OAS_ImportHistory> ret, TableInfo info) {
    TableHelper<OAS_ImportHistory, TableInfo> tableHelper =
      TableHelper.<OAS_ImportHistory, TableInfo>init(info).sortConfig(OasImportHistorySortTool::buildSort).
         addSelectFunction(OasImportHistoryConstants.PATH_FILENAME, OAS_ImportHistory::getFileName).
         addSelectFunction(OasImportHistoryConstants.PATH_TYPE, OAS_ImportHistory::getFileName).
         addSelectFunction(OasImportHistoryConstants.PATH_DATE, OAS_ImportHistory::getDate0).
         addSelectFunction(OasImportHistoryConstants.PATH_IMPORTSTATUS, OAS_ImportHistory::getImportStatus);
    tableHelper.sort(ret);
  }
  
  
  public static TableHelper.Sort buildSort(TableInfo info) {
    for (TableColumn col : info.getColumns()) {
      TableHelper.Sort sort = TableHelper.createSortIfValid(col.getPath(), col.getSort());
      if (sort != null) { return sort; }
    }
    return new TableHelper.Sort(OasImportHistoryConstants.PATH_FILENAME, true);
  }
  
}
