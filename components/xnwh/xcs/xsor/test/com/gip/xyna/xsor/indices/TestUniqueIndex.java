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
package com.gip.xyna.xsor.indices;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.gip.xyna.xsor.indices.CompositeIndex;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.UniqueKeyValueMappingIndex;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.helper.TestObject;
import com.gip.xyna.xsor.indices.helper.UnfittingSearchCriterion;
import com.gip.xyna.xsor.indices.helper.UniqueTestIndexDefinition;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.protocol.XSORPayload;


public class TestUniqueIndex extends AbstractCompositeIndexTest {

  @Override
  public IndexDefinition<TestObject, ? extends IndexKey, ? extends IndexSearchCriterion> getIndexDefinition(String[] indexedColumns) {
    return new UniqueTestIndexDefinition(TestObject.TABLE_NAME, indexedColumns);
  }

  
  @Test
  public void testUniqueKeyConstraintViolationWithMultipleColumns() {
    final String STATIC_COLUMN1_VALUE = "baum";
    final boolean STATIC_COLUMN2_VALUE = true;
    
    CompositeIndex multiColumnIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], STATIC_COLUMN1_VALUE);
    values.put(MULTI_COLUMN_NAMES[1], STATIC_COLUMN2_VALUE);
    values.put(MULTI_COLUMN_NAMES[2], 1);
    TestObject testObject = new TestObject(values);
    Assert.assertTrue("Object is not contained and should have been succesfully inserted!", multiColumnIndex.put(testObject, testObject.getObjectIndex()));
    
    Assert.assertFalse("Object is already contained and should not have been created succesfully!",
                       multiColumnIndex.put(testObject, testObject.getObjectIndex()));
    
    Map<String, Object> slightlyDifferentValues = new HashMap<String, Object>();
    slightlyDifferentValues.put(MULTI_COLUMN_NAMES[0], STATIC_COLUMN1_VALUE);
    slightlyDifferentValues.put(MULTI_COLUMN_NAMES[1], STATIC_COLUMN2_VALUE);
    slightlyDifferentValues.put(MULTI_COLUMN_NAMES[2], 2);
    TestObject slightlyDifferentTestObject = new TestObject(slightlyDifferentValues);
    
    Assert.assertTrue("Different Key but same Value should be possible!", multiColumnIndex.put(slightlyDifferentTestObject, slightlyDifferentTestObject.getObjectIndex()));
  }
  
  
  @Test
  public void testDeletionOnUniqueIndex() {
    final String STATIC_COLUMN1_VALUE = "baum";
    final boolean STATIC_COLUMN2_VALUE = true;
    
    CompositeIndex multiColumnUniqueIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], STATIC_COLUMN1_VALUE);
    values.put(MULTI_COLUMN_NAMES[1], STATIC_COLUMN2_VALUE);
    values.put(MULTI_COLUMN_NAMES[2], 1);
    TestObject testObject = new TestObject(values);
    Assert.assertTrue("Object is not contained and should have been succesfully inserted!", multiColumnUniqueIndex.put(testObject, testObject.getObjectIndex()));
    
    int[] internalIds = multiColumnUniqueIndex.get(testObject);
    Assert.assertTrue("Object should be contained", internalIds.length == 1);
    Assert.assertTrue("Retrieved id should be the one of the object", internalIds[0] == testObject.getObjectIndex());
    
    multiColumnUniqueIndex.delete(testObject, testObject.getObjectIndex());
    
    internalIds = multiColumnUniqueIndex.get(testObject);
    Assert.assertTrue("Object should be contained", internalIds.length == 0);
  }
  
  
  @Test
  public void testUniqueMappingRetrievalAndReturnValue() {
    final int STATIC_COLUMN_VALUE = 123;
    CompositeIndex singleColumnUniqueIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    
    Assert.assertTrue(singleColumnUniqueIndex instanceof UniqueKeyValueMappingIndex);
    UniqueKeyValueMappingIndex<XSORPayload> uniqueIndex =  (UniqueKeyValueMappingIndex)singleColumnUniqueIndex;
    
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(SINGLE_COLUMN_NAMES[0], STATIC_COLUMN_VALUE);
    TestObject testObject = new TestObject(values);
    
    int internalId = uniqueIndex.getUniqueValueForKey(testObject);
    Assert.assertEquals("UniqueKeyValueMappingIndex.NO_VALUE should be returned if the object is not contained", UniqueKeyValueMappingIndex.NO_VALUE, internalId);
    
    singleColumnUniqueIndex.put(testObject, STATIC_COLUMN_VALUE);
    internalId = uniqueIndex.getUniqueValueForKey(testObject);
    Assert.assertEquals("The internal id should now have been found", STATIC_COLUMN_VALUE, internalId);
    
    singleColumnUniqueIndex.delete(testObject, STATIC_COLUMN_VALUE);
    internalId = uniqueIndex.getUniqueValueForKey(testObject);
    Assert.assertEquals("UniqueKeyValueMappingIndex.NO_VALUE should be returned if the object is not contained", UniqueKeyValueMappingIndex.NO_VALUE, internalId);
    
  }
  
  
  // updates
  @Test
  public void testUpdateForUniqueIndex() {
    CompositeIndex singleColumnUniqueIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    UniqueKeyValueMappingIndex<XSORPayload> singleColumnUniqueIndexAsUniqueMapper = (UniqueKeyValueMappingIndex<XSORPayload>)singleColumnUniqueIndex;
    
    Map<String, Object> values = new HashMap<String, Object>();
    int valueOfCreationColumn = 123;
    values.put(SINGLE_COLUMN_NAMES[0], valueOfCreationColumn);
    TestObject testObject = new TestObject(values);
    int idOfCreatedObject = 1;
    Assert.assertTrue("Object is not contained and should have been succesfully inserted!", singleColumnUniqueIndex.put(testObject, idOfCreatedObject));
    
    int internalId = singleColumnUniqueIndexAsUniqueMapper.getUniqueValueForKey(testObject);
    Assert.assertEquals(idOfCreatedObject, internalId);
    
    int idToUpdateTo = 2;
    TestObject clonedObject = testObject.cloneRestricted(SINGLE_COLUMN_NAMES);
    singleColumnUniqueIndex.update(testObject, clonedObject, idOfCreatedObject, idToUpdateTo);
    int internalIdAfterUpdateWithTestObjectKey = singleColumnUniqueIndexAsUniqueMapper.getUniqueValueForKey(testObject);
    Assert.assertEquals("Key was not changed", idToUpdateTo, internalIdAfterUpdateWithTestObjectKey);
    int internalIdAfterUpdateWithClonedKey = singleColumnUniqueIndexAsUniqueMapper.getUniqueValueForKey(clonedObject);
    Assert.assertEquals("Key was not changed", idToUpdateTo, internalIdAfterUpdateWithClonedKey);
    
    Map<String, Object> updateValues = new HashMap<String, Object>();
    int valueOfUpdatedColumn = 1234;
    updateValues.put(SINGLE_COLUMN_NAMES[0], valueOfUpdatedColumn);
    TestObject updateObject = new TestObject(updateValues);
    
    singleColumnUniqueIndex.update(clonedObject, updateObject, idToUpdateTo, idToUpdateTo);
    int internalIdAfterSecondUpdateWithTestObjectKey = singleColumnUniqueIndexAsUniqueMapper.getUniqueValueForKey(testObject);
    Assert.assertEquals(-1, internalIdAfterSecondUpdateWithTestObjectKey);
    int internalIdAfterSecondUpdateWithUpdatedKey = singleColumnUniqueIndexAsUniqueMapper.getUniqueValueForKey(updateObject);
    Assert.assertEquals(idToUpdateTo, internalIdAfterSecondUpdateWithUpdatedKey);
    
    Map<String, Object> nextUpdateValues = new HashMap<String, Object>();
    int valueOfNextUpdatedColumn = 12345;
    nextUpdateValues.put(SINGLE_COLUMN_NAMES[0], valueOfNextUpdatedColumn);
    TestObject nextUpdateObject = new TestObject(nextUpdateValues);
    
    int nextIdToUpdateTo = 3;
    singleColumnUniqueIndex.update(updateObject, nextUpdateObject, idToUpdateTo, nextIdToUpdateTo);
    int internalIdAfterThirdUpdateWithTestObjectKey = singleColumnUniqueIndexAsUniqueMapper.getUniqueValueForKey(updateObject);
    Assert.assertEquals(-1, internalIdAfterThirdUpdateWithTestObjectKey);
    int internalIdAfterThridUpdateWithUpdatedKey = singleColumnUniqueIndexAsUniqueMapper.getUniqueValueForKey(nextUpdateObject);
    Assert.assertEquals(nextIdToUpdateTo, internalIdAfterThridUpdateWithUpdatedKey);
  }
  
  
  @Test
  public void testCoverageForUniqueIndexOnSingleColumnIndex() {
    final String NOT_INDEXED_COLUMN_NAME1 = "notIndexedColumn1";
    final String NOT_INDEXED_COLUMN_NAME2 = "notIndexedColumn2";
    
    //CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    IndexDefinition multiColumnCompositeIndex = getIndexDefinition(SINGLE_COLUMN_NAMES);
    
    List<ColumnCriterion> ccs = new ArrayList<ColumnCriterion>();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.EQUALS, -1));
    float coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME1, SearchColumnOperator.EQUALS, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME1, SearchColumnOperator.EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME2, SearchColumnOperator.EQUALS, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.GREATER, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
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
  
  
  @Test
  public void testCoverageResultOnUniqueMultiColumnIndex() {
    final String NOT_INDEXED_COLUMN_NAME1 = "notIndexedColumn1";
    
    //CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    IndexDefinition multiColumnCompositeIndex = getIndexDefinition(MULTI_COLUMN_NAMES);
    
    SearchCriterion crit = buildSearchCriterionForCoverage(MULTI_COLUMN_NAMES, operators("=","=","="));
    float coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 1.0f);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1]}, operators("=","="));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 0.0f);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1}, operators("=","=","=","="));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 1.0f);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1}, operators("=","=","=","like"));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 1.0f);
    
  }
  
  // TODO test searches
  
  
}
