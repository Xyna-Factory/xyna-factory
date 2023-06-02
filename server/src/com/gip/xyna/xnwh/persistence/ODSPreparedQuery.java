/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.persistence;


public class ODSPreparedQuery<E> implements PreparedQuery<E> {

  private PreparedQuery<E> innerPQ;
  private Query<E> query;
  
  public ODSPreparedQuery(Query<E> query, PreparedQuery<E>  innerPQ) {
    this.innerPQ = innerPQ;
    this.query = query;
  }
  
  public void setInnerPreparedQuery(PreparedQuery<?> newPQ) {
    this.innerPQ = (PreparedQuery<E>) newPQ;
  }
  
  public ResultSetReader<? extends E> getReader() {
    return innerPQ.getReader();
  }

  public String getTable() {
    return innerPQ.getTable();
  }
  
  public PreparedQuery<E>  getInnerPreparedQuery() {
    return innerPQ;
  }

  public Query<E> getQuery() {
    return query;
  }

}
