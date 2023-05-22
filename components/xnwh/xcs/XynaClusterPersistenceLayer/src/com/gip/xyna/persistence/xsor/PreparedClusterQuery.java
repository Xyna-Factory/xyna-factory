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
package com.gip.xyna.persistence.xsor;



import java.util.Map;

import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.ResultSetReader;



public class PreparedClusterQuery<E> implements PreparedQuery<E> {

  private final ResultSetReader<? extends E> resultSetReader;
  private final boolean isForUpdate;
  private final boolean isCountQuery;
  private final SearchRequest searchRequest;
  private final String[] selection;
  private final Map<Integer, SearchValue> prepreparedParameter;
  private final String sqlString;
  
  public PreparedClusterQuery(ResultSetReader<? extends E> resultSetReader, boolean isForUpdate, boolean isCountQuery, SearchRequest searchRequest, String[] selection, Map<Integer, SearchValue> prepreparedParameter, String sqlString) {
    this.resultSetReader = resultSetReader;
    this.isForUpdate = isForUpdate;
    this.isCountQuery = isCountQuery;
    this.searchRequest = searchRequest;
    this.selection = selection;
    this.prepreparedParameter = prepreparedParameter;
    this.sqlString = sqlString;
  }


  public ResultSetReader<? extends E> getReader() {
    return resultSetReader;
  }


  public boolean isForUpdate() {
    return isForUpdate;
  }
  
  
  public boolean isCountQuery() {
    return isCountQuery;
  }


  public SearchRequest getSearchRequest() {
    return searchRequest;
  }


  public String getTable() {
    return getSearchRequest().getTablename();
  }
  
  
  public String[] getSelection() {
    return selection;
  }
  
  
 public Map<Integer, SearchValue> getPrepreparedParameter() {
   return prepreparedParameter;
 }
 
 
 public String getSqlString() {
   return sqlString;
 }
  

}
