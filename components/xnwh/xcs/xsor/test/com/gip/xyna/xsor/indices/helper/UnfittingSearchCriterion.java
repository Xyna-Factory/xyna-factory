/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xsor.indices.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;


public class UnfittingSearchCriterion extends SearchCriterion {

  public UnfittingSearchCriterion(List<ColumnCriterion> columns) {
    super(columns);
  }

  @Override
  public boolean fits(Object candidate, SearchParameter parameter) {
    return false;
  }
  
  
  public static UnfittingSearchCriterion generateUnfittingSearchCriterion(String[] searchColumns, SearchColumnOperator[] columnOperations, AtomicInteger idGenerator) {
    List<ColumnCriterion> columnCriteria = new ArrayList<ColumnCriterion>();
    for (int i=0; i < searchColumns.length; i++) {
      columnCriteria.add(new ColumnCriterion(searchColumns[i], columnOperations[i], idGenerator.getAndIncrement()));
    }
    return new UnfittingSearchCriterion(columnCriteria);
  }

}
