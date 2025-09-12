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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;

import xmcp.oas.fman.storables.OAS_ImportHistory;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.storage.generic.filter.FilterColumnConfig;
import xmcp.zeta.storage.generic.filter.FilterColumnInput;
import xmcp.zeta.storage.generic.filter.TableFilter;
import xmcp.zeta.storage.generic.filter.TableFilterBuilder;


public class SearchOasImportHistory implements WarehouseRetryExecutableNoException<List<OAS_ImportHistory>> {

  private static final String SELECT_BASE = "SELECT " +
    OasImportHistoryStorable.COL_UNIQUE_ID + ", " +
    OasImportHistoryStorable.COL_FILE_NAME + ", " +
    OasImportHistoryStorable.COL_IMPORT_TYPE + ", " +
    OasImportHistoryStorable.COL_IMPORT_DATE + ", " +
    OasImportHistoryStorable.COL_IMPORT_STATUS +
    " FROM " + OasImportHistoryStorable.TABLE_NAME;
  private static final OasImportHistoryAdapter _adapter = new OasImportHistoryAdapter();
  
  
  private final static TableFilterBuilder _filterBuilder = new TableFilterBuilder(List.of(
     FilterColumnConfig.builder().xmomPath(OasImportHistoryConstants.PATH_FILENAME).
                                  sqlColumnName(OasImportHistoryStorable.COL_FILE_NAME).build(),
     FilterColumnConfig.builder().xmomPath(OasImportHistoryConstants.PATH_TYPE).
                                  sqlColumnName(OasImportHistoryStorable.COL_IMPORT_TYPE).build(),
     FilterColumnConfig.builder().xmomPath(OasImportHistoryConstants.PATH_DATE).
                                  sqlColumnName(OasImportHistoryStorable.COL_IMPORT_DATE).build(),
     FilterColumnConfig.builder().xmomPath(OasImportHistoryConstants.PATH_IMPORTSTATUS).
                                  sqlColumnName(OasImportHistoryStorable.COL_IMPORT_STATUS).build()));
  
  private final List<FilterColumnInput> _filterColumnInputList;
  private int _queryLength = 200;
  
  
  public SearchOasImportHistory(TableInfo info) {
    super();
    List<FilterColumnInput> adaptedColumns = new ArrayList<>();
    for (TableColumn tc : info.getColumns()) {
      adaptedColumns.add(adapt(tc));
    }
    if ((info.getLength() != null) && (info.getLength() > 0)) {
      _queryLength = info.getLength();
    }
    _filterColumnInputList = adaptedColumns;
  }


  @Override
  public List<OAS_ImportHistory> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
    QueryGenerator qg = ODSImpl.getInstance().getQueryGenerator(con.getConnectionType(), OasImportHistoryStorable.TABLE_NAME);
    TableFilter filter = _filterBuilder.build(_filterColumnInputList, qg.escape);
    String sql = SELECT_BASE + filter.getWhereClause();
    PreparedQuery<OasImportHistoryStorable> query = OasImportHistoryStorage.getQueryCache().getQueryFromCache(sql, con,
                                                      OasImportHistoryStorable.getOasImportHistoryMultiLineReader());
    List<OasImportHistoryStorable> result = con.query(query, filter.buildParameter(), _queryLength);
    return result.stream().map(x -> _adapter.adapt(x)).collect(Collectors.toList());
  }
  
  
  private FilterColumnInput adapt(TableColumn tc) {
    FilterColumnInput ret = new FilterColumnInput();
    ret.setFilter(tc.getFilter());
    ret.setPath(tc.getPath());
    return ret;
  }
  
}
