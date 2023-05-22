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
package com.gip.xyna.xsor.indices;


import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gip.xyna.xsor.indices.CompositeIndex;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.helper.HashTestIndexDefinition;
import com.gip.xyna.xsor.indices.helper.TestObject;
import com.gip.xyna.xsor.indices.helper.UnfittingSearchCriterion;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;


public class TestHashIndex extends AbstractMultiValueCompositeIndexTest {

  @Override
  public IndexDefinition<TestObject, ? extends IndexKey, ? extends IndexSearchCriterion> getIndexDefinition(String[] indexedColumns) {
    return new HashTestIndexDefinition(TestObject.TABLE_NAME, indexedColumns);
  }
  
  @Test
  public void testCoverageForHashIndexWithDifferentOperators() {
    //CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    IndexDefinition multiColumnCompositeIndex = getIndexDefinition(SINGLE_COLUMN_NAMES);
    
    List<ColumnCriterion> ccs = new ArrayList<ColumnCriterion>();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.GREATER, -1));
    float coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.GREATER_EQUALS, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.SMALLER, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.SMALLER_EQUALS, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.LIKE, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.IN, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.0f);
  }
  
  
}
