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

package xmcp.zeta.storage.generic.filter.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator;


public class SqlWhereClauseData {

  private final Function<String, String> _escape;
  private StringBuilder sql = new StringBuilder();
  private List<String> parameters = new ArrayList<>();
  
  
  public SqlWhereClauseData(QueryGenerator queryGenerator) {
    this._escape = queryGenerator.escape;
  }
  
  public SqlWhereClauseData(Function<String, String> escape) {
    this._escape = escape;
  }

  public void appendToSql(String str) {
    sql.append(str);
  }
  
  public void appendToSqlEscaped(String str) {
    sql.append(_escape.apply(str));
  }
  
  public void addQueryParameter(String param) {
    if (param == null) { throw new IllegalArgumentException("Query parameter is null."); }
    if (param.isEmpty()) { throw new IllegalArgumentException("Query parameter is empty."); }
    parameters.add(param);
  }
  
  public String getSql() {
    return sql.toString();
  }
  
  public List<String> getParameters() {
    return parameters;
  }
 
  @Override
  public String toString() {
    return getSql();
  }
  
}
