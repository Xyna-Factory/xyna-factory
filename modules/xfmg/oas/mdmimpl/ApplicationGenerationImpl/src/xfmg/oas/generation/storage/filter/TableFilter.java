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

package xfmg.oas.generation.storage.filter;

import java.util.List;

import com.gip.xyna.xnwh.persistence.Parameter;


public class TableFilter {

  private static final String SQL_WHERE = " WHERE ";
  private static final String SQL_AND = " AND ";
  private static final String SQL_WILDCARD_CONDITION = " LIKE ?";
  
  private final List<FilterColumn> _filterColumns;

  
  public TableFilter(List<FilterColumn> filterColumns) {
    this._filterColumns = filterColumns;
  }
  
  
  public String buildWhereClause() {
    if (_filterColumns.size() < 1) { return ""; }
    StringBuilder str = new StringBuilder();
    str.append(SQL_WHERE);
    boolean isfirst = true;
    for (FilterColumn col : _filterColumns) {
      if (isfirst) { isfirst = false; }
      else { str.append(SQL_AND); }
      str.append(col.getSqlColumnName());
      str.append(SQL_WILDCARD_CONDITION);
    }
    return str.toString();
  }
  
  
  public Parameter buildParameter() {
    Parameter ret = new Parameter();
    for (FilterColumn col : _filterColumns) {
      ret.add(col.getValue());
    }
    return ret;
  }
  
}
