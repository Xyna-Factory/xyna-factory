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

package xmcp.zeta.storage.generic.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.log4j.Logger;

import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator;

import xmcp.zeta.storage.generic.filter.elems.FilterElement;
import xmcp.zeta.storage.generic.filter.parser.FilterInputParser;
import xmcp.zeta.storage.generic.filter.shared.SqlWhereClauseData;


public class TableFilter {

  private static Logger _logger = Logger.getLogger(TableFilter.class);
  
  private static final String SQL_WHERE = " WHERE ";
  private static final String SQL_AND = " AND ";
  
  private final List<FilterColumn> _filterColumns;
  private final Function<String, String> _escape;
  private String _whereClause = "";
  private List<String> _parameters = new ArrayList<>();

  
  public TableFilter(List<FilterColumn> filterColumns, QueryGenerator queryGenerator) {
    this(filterColumns, queryGenerator.escape);
  }
  
  public TableFilter(List<FilterColumn> filterColumns, Function<String, String> escape) {
    this._filterColumns = filterColumns;
    this._escape = escape;
    init();
  }
  
  
  private void init() {
    if (_filterColumns.size() < 1) { return; }
    FilterInputParser parser = new FilterInputParser();
    StringBuilder str = new StringBuilder();
    str.append(SQL_WHERE);
    boolean isfirst = true;
    for (FilterColumn col : _filterColumns) {
      try {
        FilterElement elem = parser.parse(col.getValue());
        SqlWhereClauseData sql = new SqlWhereClauseData(_escape);
        elem.writeSql(col.getSqlColumnName(), sql);
        if (isfirst) { isfirst = false; }
        else { str.append(SQL_AND); }
        str.append(sql.getSql());
        _parameters.addAll(sql.getParameters());
      } catch (Exception e) {
        _logger.error("Error parsing filter input: " + e.getMessage(), e);
      }
    }
    _whereClause = str.toString();
  }
  
  
  public Parameter buildParameter() {
    Parameter ret = new Parameter();
    for (String val : _parameters) {
      ret.add(val);
    }
    return ret;
  }


  public String getWhereClause() {
    return _whereClause;
  }
  
  
  public static TableFilterBuilder builder(List<FilterColumnConfig> list) {
    return new TableFilterBuilder(list);
  }
  
}
