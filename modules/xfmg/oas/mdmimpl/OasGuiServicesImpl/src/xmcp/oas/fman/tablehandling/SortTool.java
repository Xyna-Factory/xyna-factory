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

package xmcp.oas.fman.tablehandling;

import java.util.List;

import xmcp.oas.fman.datatypes.OasApiDatatypeInfo;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;


public class SortTool {

  public void sort(List<OasApiDatatypeInfo> ret, TableInfo info) {
    TableHelper<OasApiDatatypeInfo, TableInfo> tableHelper =
      TableHelper.<OasApiDatatypeInfo, TableInfo>init(info).sortConfig(SortTool::buildSort).
         addSelectFunction(TableHandlingConstants.Paths.API_DATATYPE, OasApiDatatypeInfo::getApiDatatype).
         addSelectFunction(TableHandlingConstants.Paths.GENERATED_RTC, OasApiDatatypeInfo::getGeneratedRtc).
         addSelectFunction(TableHandlingConstants.Paths.IMPLEMENTATION_DATATYPE, OasApiDatatypeInfo::getImplementationDatatype).
         addSelectFunction(TableHandlingConstants.Paths.IMPLEMENTATION_RTC, OasApiDatatypeInfo::getImplementationRtc).
         addSelectFunction(TableHandlingConstants.Paths.STATUS, OasApiDatatypeInfo::getStatus);
    tableHelper.sort(ret);
  }
  
  
  public static TableHelper.Sort buildSort(TableInfo info) {
    for (TableColumn col : info.getColumns()) {
      TableHelper.Sort sort = TableHelper.createSortIfValid(col.getPath(), col.getSort());
      if (sort != null) { return sort; }
    }
    return new TableHelper.Sort(TableHandlingConstants.Paths.API_DATATYPE, true);
  }
  
}
