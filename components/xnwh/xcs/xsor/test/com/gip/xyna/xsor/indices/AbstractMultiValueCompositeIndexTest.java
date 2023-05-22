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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.junit.Test;

import com.gip.xyna.xsor.indices.CompositeIndex;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.helper.OrderedTestIndexDefinition;
import com.gip.xyna.xsor.indices.helper.TestObject;
import com.gip.xyna.xsor.indices.helper.UnfittingSearchCriterion;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;


public abstract class AbstractMultiValueCompositeIndexTest extends AbstractCompositeIndexTest {

  @Test
  public void testDeletionFromMultipleValues() {
    final int INSERTS = 1000;

    OrderedTestIndexDefinition testDefinition = new OrderedTestIndexDefinition(TestObject.TABLE_NAME, MULTI_COLUMN_NAMES);
    CompositeIndex compositeMultiValueIndex = indexFactory.createIndex(testDefinition);
    
    for (int i=0; i < INSERTS; i++) {
      Map<String, Object> values = new HashMap<String, Object>();
      values.put(MULTI_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
      values.put(MULTI_COLUMN_NAMES[1], uniqueValueGenerator.incrementAndGet());
      values.put(MULTI_COLUMN_NAMES[2], uniqueValueGenerator.incrementAndGet());
      TestObject testObject = new TestObject(values, idGenerator);
      storage.put(testObject.getObjectIndex(), testObject);
      compositeMultiValueIndex.put(testObject, testObject.getObjectIndex());
    }
    
    int[] allValues = compositeMultiValueIndex.values();
    Assert.assertEquals("all 100 unique values should be contained.",INSERTS, allValues.length);
    
    int randomId = RANDOMIZER.nextInt(storage.size()-1)+1;
    TestObject randomTestObject = storage.get(randomId);
    int[] internalIdsForRandomObject = compositeMultiValueIndex.get(randomTestObject);
    
    Assert.assertTrue("HashIndex should have at least returned 1 value", internalIdsForRandomObject.length >= 1);
    List<Integer> internalIds = new ArrayList<Integer>();
    for (Integer integer : internalIdsForRandomObject) {
      internalIds.add(integer);
    }
    Assert.assertTrue("Created object's id should have been contained", internalIds.contains(randomTestObject.getObjectIndex()));
    
    TestObject clone1 = randomTestObject.cloneRestricted(MULTI_COLUMN_NAMES);
    TestObject clone2 = randomTestObject.cloneRestricted(MULTI_COLUMN_NAMES);
    TestObject clone3 = randomTestObject.cloneRestricted(MULTI_COLUMN_NAMES);
    TestObject clone4 = randomTestObject.cloneRestricted(MULTI_COLUMN_NAMES);
    TestObject clone5 = randomTestObject.cloneRestricted(MULTI_COLUMN_NAMES);
    
    compositeMultiValueIndex.put(clone1, idGenerator.incrementAndGet());
    compositeMultiValueIndex.put(clone2, idGenerator.incrementAndGet());
    compositeMultiValueIndex.put(clone3, idGenerator.incrementAndGet());
    compositeMultiValueIndex.put(clone4, idGenerator.incrementAndGet());
    compositeMultiValueIndex.put(clone5, idGenerator.incrementAndGet());
    
    Assert.assertEquals(1005, compositeMultiValueIndex.values().length);
    
    int[] internalIdsAfterAdditions = compositeMultiValueIndex.get(randomTestObject);
    Assert.assertTrue(internalIdsAfterAdditions.length >= 6);
    
    int firstId = internalIdsAfterAdditions[0];
    compositeMultiValueIndex.delete(randomTestObject, firstId);
    int[] internalIdsAfterFirstDeletion = compositeMultiValueIndex.get(randomTestObject);
    Assert.assertTrue(internalIdsAfterFirstDeletion.length < internalIdsAfterAdditions.length);
    Assert.assertFalse(internalIdsAfterFirstDeletion[0] == firstId);
    Arrays.sort(internalIdsAfterFirstDeletion);
    Assert.assertTrue(Arrays.binarySearch(internalIdsAfterFirstDeletion, firstId) < 0);
    
    int lastId = internalIdsAfterFirstDeletion[internalIdsAfterFirstDeletion.length-1];
    compositeMultiValueIndex.delete(randomTestObject, lastId);
    int[] internalIdsAfterSecondDeletion = compositeMultiValueIndex.get(randomTestObject);
    Assert.assertTrue(internalIdsAfterSecondDeletion.length < internalIdsAfterFirstDeletion.length);
    Arrays.sort(internalIdsAfterSecondDeletion);
    Assert.assertTrue(Arrays.binarySearch(internalIdsAfterSecondDeletion, lastId) < 0);
    
    int middleId = internalIdsAfterSecondDeletion[RANDOMIZER.nextInt(internalIdsAfterSecondDeletion.length-2)+1];
    compositeMultiValueIndex.delete(randomTestObject, middleId);
    int[] internalIdsAfterThirdDeletion = compositeMultiValueIndex.get(randomTestObject);
    Assert.assertTrue(internalIdsAfterThirdDeletion.length < internalIdsAfterSecondDeletion.length);
    Arrays.sort(internalIdsAfterThirdDeletion);
    Assert.assertTrue(Arrays.binarySearch(internalIdsAfterThirdDeletion, middleId) < 0);
    
  }
  
  
  @Test
  public void testUpdateIncludingUpdatesInValueWrappers() {
    CompositeIndex compositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    
    final int creationColumnValue1 = 1;
    final int creationColumnValue2 = 1;
    final int creationColumnValue3 = 1;
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(SINGLE_COLUMN_NAMES[0], creationColumnValue1);
    TestObject testObject1 = new TestObject(values);
    final int creationInternalIdForObject1 = 1;
    Assert.assertTrue(compositeIndex.put(testObject1, creationInternalIdForObject1));
    values = new HashMap<String, Object>();
    values.put(SINGLE_COLUMN_NAMES[0], creationColumnValue2);
    TestObject testObject2 = new TestObject(values);
    final int creationInternalIdForObject2 = 2;
    Assert.assertTrue(compositeIndex.put(testObject2, creationInternalIdForObject2));
    values = new HashMap<String, Object>();
    values.put(SINGLE_COLUMN_NAMES[0], creationColumnValue3);
    TestObject testObject3 = new TestObject(values);
    final int creationInternalIdForObject3 = 3;
    Assert.assertTrue(compositeIndex.put(testObject3, creationInternalIdForObject3));
    
    int[] internalIdsAfterCreations = compositeIndex.get(testObject1);
    Assert.assertEquals(3, internalIdsAfterCreations.length);
    Arrays.sort(internalIdsAfterCreations);
    Assert.assertTrue(Arrays.binarySearch(internalIdsAfterCreations, creationInternalIdForObject1) >= 0);
    Assert.assertTrue(Arrays.binarySearch(internalIdsAfterCreations, creationInternalIdForObject2) >= 0);
    Assert.assertTrue(Arrays.binarySearch(internalIdsAfterCreations, creationInternalIdForObject3) >= 0);
    
    final int idToUpdateForTestObject2 = 22; 
    compositeIndex.update(testObject2, testObject2, creationInternalIdForObject2, idToUpdateForTestObject2);
    int[] internalIdsAfterFirstUpdate = compositeIndex.get(testObject2);
    Assert.assertEquals(3, internalIdsAfterFirstUpdate.length);
    Arrays.sort(internalIdsAfterFirstUpdate);
    Assert.assertTrue(Arrays.binarySearch(internalIdsAfterFirstUpdate, creationInternalIdForObject1) >= 0);
    Assert.assertTrue(Arrays.binarySearch(internalIdsAfterFirstUpdate, idToUpdateForTestObject2) >= 0);
    Assert.assertTrue(Arrays.binarySearch(internalIdsAfterFirstUpdate, creationInternalIdForObject3) >= 0);
    
    final int updateColumnValue2 = 222;
    values = new HashMap<String, Object>();
    values.put(SINGLE_COLUMN_NAMES[0], updateColumnValue2);
    TestObject updatedTestObject2 = new TestObject(values);
    compositeIndex.update(testObject2, updatedTestObject2, idToUpdateForTestObject2, idToUpdateForTestObject2);
    
    int[] internalIdsForFirstBucketAfterSecondUpdate = compositeIndex.get(testObject1);
    Assert.assertEquals(2, internalIdsForFirstBucketAfterSecondUpdate.length);
    Arrays.sort(internalIdsForFirstBucketAfterSecondUpdate);
    Assert.assertTrue(Arrays.binarySearch(internalIdsForFirstBucketAfterSecondUpdate, creationInternalIdForObject1) >= 0);
    Assert.assertTrue(Arrays.binarySearch(internalIdsForFirstBucketAfterSecondUpdate, creationInternalIdForObject3) >= 0);
    
    int[] internalIdsForSecondBucketAfterSecondUpdate = compositeIndex.get(updatedTestObject2);
    Assert.assertEquals(1, internalIdsForSecondBucketAfterSecondUpdate.length);
    Assert.assertEquals(idToUpdateForTestObject2, internalIdsForSecondBucketAfterSecondUpdate[0]);
    
    final int updateColumnValue3 = 33;
    values = new HashMap<String, Object>();
    values.put(SINGLE_COLUMN_NAMES[0], updateColumnValue3);
    TestObject updatedTestObject3 = new TestObject(values);
    final int idToUpdateForTestObject3 = 333; 
    compositeIndex.update(testObject3, updatedTestObject3, creationInternalIdForObject3, idToUpdateForTestObject3);
    
    int[] internalIdsForFirstBucketAfterThirdUpdate = compositeIndex.get(testObject1);
    Assert.assertEquals(1, internalIdsForFirstBucketAfterThirdUpdate.length);
    int[] internalIdsForSecondBucketAfterThirdUpdate = compositeIndex.get(updatedTestObject2);
    Assert.assertEquals(1, internalIdsForSecondBucketAfterThirdUpdate.length);
    int[] internalIdsForThirdBucketAfterThirdUpdate = compositeIndex.get(updatedTestObject3);
    Assert.assertEquals(1, internalIdsForThirdBucketAfterThirdUpdate.length);
    
    Assert.assertEquals(idToUpdateForTestObject3, internalIdsForThirdBucketAfterThirdUpdate[0]);
    
  }
  
  
  @Test
  public void testSearchRequestWithEqualComparisonAndMultipleResults() {
    final int INSERTS = 1000;

    CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    
    for (int i=0; i < INSERTS; i++) {
      Map<String, Object> values = new HashMap<String, Object>();
      values.put(MULTI_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
      values.put(MULTI_COLUMN_NAMES[1], uniqueValueGenerator.incrementAndGet());
      values.put(MULTI_COLUMN_NAMES[2], uniqueValueGenerator.incrementAndGet());
      TestObject testObject = new TestObject(values, idGenerator);
      storage.put(testObject.getObjectIndex(), testObject);
      multiColumnCompositeIndex.put(testObject, testObject.getObjectIndex());
    }
    
    int[] allValues = multiColumnCompositeIndex.values();
    Assert.assertEquals("all 100 unique values should be contained.",INSERTS, allValues.length);
    
    SearchCriterion allColumnsEqual = UnfittingSearchCriterion.generateUnfittingSearchCriterion(MULTI_COLUMN_NAMES,new SearchColumnOperator[] {SearchColumnOperator.EQUALS,SearchColumnOperator.EQUALS,SearchColumnOperator.EQUALS},parameterIdGenerator);
    
    int newValueWithMultipleAppearances = uniqueValueGenerator.incrementAndGet();
    final int MULTIPLE_VALUE_APPERANCES = 5;
    TestObject lastOfMultiple = null;
    for (int i=0; i < MULTIPLE_VALUE_APPERANCES; i++) {
      Map<String, Object> values = new HashMap<String, Object>();
      values.put(MULTI_COLUMN_NAMES[0], newValueWithMultipleAppearances);
      values.put(MULTI_COLUMN_NAMES[1], newValueWithMultipleAppearances);
      values.put(MULTI_COLUMN_NAMES[2], newValueWithMultipleAppearances);
      lastOfMultiple = new TestObject(values, idGenerator);
      storage.put(lastOfMultiple.getObjectIndex(), lastOfMultiple);
      multiColumnCompositeIndex.put(lastOfMultiple, lastOfMultiple.getObjectIndex());
    }
    
    allValues = multiColumnCompositeIndex.values();
    Assert.assertEquals("all 105 unique values should be contained.",INSERTS+MULTIPLE_VALUE_APPERANCES, allValues.length);
    
    parameterIdGenerator.set(0);
    SearchParameter params = buildSearchParameterFromTestObject(lastOfMultiple, MULTI_COLUMN_NAMES);
    
    int[] column1EqualsResult = multiColumnCompositeIndex.search(allColumnsEqual, params, -1).getInternalIds();
    Assert.assertEquals(MULTIPLE_VALUE_APPERANCES, column1EqualsResult.length);
  }
  
  
  @Test
  public void testCoverageForMultiValueSingleColumnIndex() {
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
    assertTrue(coverage == 0.5f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME1, SearchColumnOperator.EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME2, SearchColumnOperator.EQUALS, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage <= 1.0/2.9);
    assertTrue(coverage >= 1.0/3.1);
  }
  
  
  @Test
  public void testCoverageResultOnMultiColumnMultiValueIndex() {
    final String NOT_INDEXED_COLUMN_NAME1 = "notIndexedColumn1";
    final String NOT_INDEXED_COLUMN_NAME2 = "notIndexedColumn2";
    
    //CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    IndexDefinition multiColumnCompositeIndex = getIndexDefinition(SINGLE_COLUMN_NAMES);
    
    SearchCriterion crit = buildSearchCriterionForCoverage(MULTI_COLUMN_NAMES, operators("=","=","="));
    float coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 1.0f);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1]}, operators("like","in"));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 0.0f);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1}, operators("=","=","=","="));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage >= 3.0/4.1);
    assertTrue(coverage <= 3.0/3.9);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1}, operators("=","=","=","like"));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage >= 3.0/4.1);
    assertTrue(coverage <= 3.0/3.9);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1, NOT_INDEXED_COLUMN_NAME2}, operators("=","=","=","=","="));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage >= 3.0/5.1);
    assertTrue(coverage <= 3.0/4.9);
  }

  
  @Test
  public void testGetAndSearchOnColumnFunction() {
    CompositeIndex compositeIndexWithColumnFunction = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES_INCLUDING_COLUMN_FUNCTION));
    
    final String COLUMN1_VALUE = "Test";
    final int SMALL_VALUE = 1;
    final int MEDIUM_VALUE = 5;
    final int HIGH_VALUE = 10;
    
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], COLUMN1_VALUE);
    values.put(MULTI_COLUMN_NAMES[1], SMALL_VALUE);
    values.put(MULTI_COLUMN_NAMES[2], HIGH_VALUE);
    TestObject testObject1 = new TestObject(values, idGenerator);
    
    SearchCriterion allColumnsEqual = UnfittingSearchCriterion.generateUnfittingSearchCriterion(MULTI_COLUMN_NAMES,new SearchColumnOperator[] {SearchColumnOperator.EQUALS,SearchColumnOperator.EQUALS,SearchColumnOperator.EQUALS},parameterIdGenerator);
    
    assertTrue(compositeIndexWithColumnFunction.put(testObject1, testObject1.getObjectIndex()));
    int[] internalIds = compositeIndexWithColumnFunction.get(testObject1);
    assertTrue(idsContains(internalIds, testObject1.getObjectIndex()));
    assertEquals(1, internalIds.length);
    
    SearchParameter params = buildSearchParameterFromTestObject(testObject1, MULTI_COLUMN_NAMES);
    internalIds = compositeIndexWithColumnFunction.search(allColumnsEqual, params, -1).getInternalIds();
    assertTrue(idsContains(internalIds, testObject1.getObjectIndex()));
    assertEquals(1, internalIds.length);
    
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], COLUMN1_VALUE);
    values.put(MULTI_COLUMN_NAMES[1], MEDIUM_VALUE);
    values.put(MULTI_COLUMN_NAMES[2], HIGH_VALUE);
    TestObject testObject2 = new TestObject(values, idGenerator);
    
    assertTrue(compositeIndexWithColumnFunction.put(testObject2, testObject2.getObjectIndex()));
    internalIds = compositeIndexWithColumnFunction.get(testObject2);
    assertTrue(idsContains(internalIds, testObject1.getObjectIndex()));
    assertTrue(idsContains(internalIds, testObject2.getObjectIndex()));
    assertEquals(2, internalIds.length);
    
    params = buildSearchParameterFromTestObject(testObject2, MULTI_COLUMN_NAMES);
    internalIds = compositeIndexWithColumnFunction.search(allColumnsEqual, params, -1).getInternalIds();
    assertTrue(idsContains(internalIds, testObject1.getObjectIndex()));
    assertTrue(idsContains(internalIds, testObject2.getObjectIndex()));
    assertEquals(2, internalIds.length);
    
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], COLUMN1_VALUE);
    values.put(MULTI_COLUMN_NAMES[1], HIGH_VALUE);
    values.put(MULTI_COLUMN_NAMES[2], HIGH_VALUE);
    TestObject testObject3 = new TestObject(values, idGenerator);
    
    assertTrue(compositeIndexWithColumnFunction.put(testObject3, testObject3.getObjectIndex()));
    internalIds = compositeIndexWithColumnFunction.get(testObject3);
    assertTrue(idsContains(internalIds, testObject1.getObjectIndex()));
    assertTrue(idsContains(internalIds, testObject2.getObjectIndex()));
    assertTrue(idsContains(internalIds, testObject3.getObjectIndex()));
    assertEquals(3, internalIds.length);
    
    params = buildSearchParameterFromTestObject(testObject3, MULTI_COLUMN_NAMES);
    internalIds = compositeIndexWithColumnFunction.search(allColumnsEqual, params, -1).getInternalIds();
    assertTrue(idsContains(internalIds, testObject1.getObjectIndex()));
    assertTrue(idsContains(internalIds, testObject2.getObjectIndex()));
    assertTrue(idsContains(internalIds, testObject3.getObjectIndex()));
    assertEquals(3, internalIds.length);
    
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], COLUMN1_VALUE);
    values.put(MULTI_COLUMN_NAMES[1], MEDIUM_VALUE);
    values.put(MULTI_COLUMN_NAMES[2], MEDIUM_VALUE);
    TestObject testObject4 = new TestObject(values, idGenerator);
    
    assertTrue(compositeIndexWithColumnFunction.put(testObject4, testObject4.getObjectIndex()));
    internalIds = compositeIndexWithColumnFunction.get(testObject4);
    assertTrue(idsContains(internalIds, testObject4.getObjectIndex()));
    assertEquals(1, internalIds.length);
    
    params = buildSearchParameterFromTestObject(testObject4, MULTI_COLUMN_NAMES);
    internalIds = compositeIndexWithColumnFunction.search(allColumnsEqual, params, -1).getInternalIds();
    internalIds = compositeIndexWithColumnFunction.get(testObject4);
    assertTrue(idsContains(internalIds, testObject4.getObjectIndex()));
    assertEquals(1, internalIds.length);
    
  }
  
  
  @Test
  public void testRefillInSameBucket() {
    CompositeIndex singleColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    
    final String CONSTANT_SINGLE_COLUM_VALUE = "VALUE";
    final int INSERTS = 5;
    
    for (int i = 0; i < INSERTS; i++) {
      Map<String, Object> values = new HashMap<String, Object>();
      values.put(SINGLE_COLUMN_NAMES[0], CONSTANT_SINGLE_COLUM_VALUE);
      TestObject testObject = new TestObject(values, idGenerator);
      storage.put(testObject.getObjectIndex(), testObject);
      singleColumnCompositeIndex.put(testObject, testObject.getObjectIndex());
    }
    
    int[] allValues = singleColumnCompositeIndex.values();
    assertEquals("all "+INSERTS+" values should be contained.",INSERTS, allValues.length);
    assertEquals(storage.size(), allValues.length);
    
    while (storage.size() > 0) {
      Entry<Integer, TestObject> entry = storage.entrySet().iterator().next();
      singleColumnCompositeIndex.delete(entry.getValue(), entry.getKey());
      storage.remove(entry.getKey());
    }
    allValues = singleColumnCompositeIndex.values();
    assertEquals(storage.size(), allValues.length);
    
    for (int i = 0; i < INSERTS; i++) {
      Map<String, Object> values = new HashMap<String, Object>();
      values.put(SINGLE_COLUMN_NAMES[0], CONSTANT_SINGLE_COLUM_VALUE);
      TestObject testObject = new TestObject(values, idGenerator);
      storage.put(testObject.getObjectIndex(), testObject);
      singleColumnCompositeIndex.put(testObject, testObject.getObjectIndex());
    }
    
    allValues = singleColumnCompositeIndex.values();
    assertEquals("all "+INSERTS+" values should be contained.",INSERTS, allValues.length);
    assertEquals(storage.size(), allValues.length);
    
    while (storage.size() > 0) {
      Entry<Integer, TestObject> entry = storage.entrySet().iterator().next();
      singleColumnCompositeIndex.delete(entry.getValue(), entry.getKey());
      storage.remove(entry.getKey());
    }
    
    allValues = singleColumnCompositeIndex.values();
    assertEquals(storage.size(), allValues.length);
    
    
  }

}
