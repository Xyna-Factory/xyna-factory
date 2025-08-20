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
import java.util.stream.Collectors;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;

import xfmg.oas.generation.storage.tools.FilterColumnConfig;
import xfmg.oas.generation.storage.tools.TableFilter;
import xfmg.oas.generation.storage.tools.TableFilterBuilder;
import xmcp.oas.fman.storables.OAS_ImportHistory;
import xmcp.tables.datatypes.TableInfo;


public class SearchOasImportHistory implements WarehouseRetryExecutableNoException<List<OAS_ImportHistory>> {

  private static final String SELECT_BASE = "SELECT * FROM " + OasImportHistoryStorable.TABLE_NAME;
  /*
  private static final String CONDITION_BASE = " WHERE ";
  private static final String CONDITION_AND = " AND ";
  
  private static final String QUERY_OAS_IMPORT_HISTORY =
      "SELECT * FROM " + OasImportHistoryStorable.TABLE_NAME + " WHERE " +
      OasImportHistoryStorable.COL_FILE_NAME + " LIKE ? AND " +
      OasImportHistoryStorable.COL_TYPE + " LIKE ? AND " +
      OasImportHistoryStorable.COL_DATE + " LIKE ? AND " +
      OasImportHistoryStorable.COL_IMPORT_STATUS + " LIKE ?";
  */
  
  private final static TableFilterBuilder _filterBuilder = new TableFilterBuilder(List.of(
     FilterColumnConfig.builder().xmomPath(OasImportHistoryConstants.PATH_FILENAME).
                                  sqlColumnName(OasImportHistoryStorable.COL_FILE_NAME).build(),
     FilterColumnConfig.builder().xmomPath(OasImportHistoryConstants.PATH_TYPE).
                                  sqlColumnName(OasImportHistoryStorable.COL_TYPE).build(),
     FilterColumnConfig.builder().xmomPath(OasImportHistoryConstants.PATH_DATE).
                                  sqlColumnName(OasImportHistoryStorable.COL_DATE).build(),
     FilterColumnConfig.builder().xmomPath(OasImportHistoryConstants.PATH_IMPORTSTATUS).
                                  sqlColumnName(OasImportHistoryStorable.COL_IMPORT_STATUS).build()));
  
  private final TableFilter filter;
  
  
  public SearchOasImportHistory(TableInfo info) {
    super();
    this.filter = _filterBuilder.build(info);
  }


  @Override
  public List<OAS_ImportHistory> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
    String sql = getQuerySql();
    PreparedQuery<OasImportHistoryStorable> query = OasImportHistoryStorage.getQueryCache().getQueryFromCache(sql, con,
                                                      OasImportHistoryStorable.getOasImportHistoryStorableReader());
    List<OasImportHistoryStorable> result = con.query(query, filter.buildParameter(), -1);
    return result.stream().map(x -> adapt(x)).collect(Collectors.toList());
  }
  
  
  private String getQuerySql() {
    return SELECT_BASE + filter.buildWhereClause();
  }
  
  
  private OAS_ImportHistory adapt(OasImportHistoryStorable input) {
    OAS_ImportHistory ret = new OAS_ImportHistory();
    ret.setUniqueIdentifier(input.getUniqueIdentifier());
    ret.setType(input.getType());
    ret.setDate0(input.getDate());
    ret.setFileName(input.getFileName());
    ret.setSpecificationFile(input.getSpecificationFile());
    ret.setImportStatus(input.getImportStatus());
    ret.setErrorMessage(input.getErrorMessage());
    return ret;
  }
  
}
