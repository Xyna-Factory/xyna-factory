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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import com.gip.xyna.xsor.indices.CompositeIndex;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.helper.TestObject;
import com.gip.xyna.xsor.indices.helper.UnfittingSearchCriterion;
import com.gip.xyna.xsor.indices.management.BasicIndexFactory;
import com.gip.xyna.xsor.indices.management.IndexFactory;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;


public abstract class AbstractCompositeIndexTest {
  
  protected static final Random RANDOMIZER = new Random();
  protected static final IndexFactory indexFactory = new BasicIndexFactory();
  
  protected ConcurrentMap<Integer, TestObject> storage = new ConcurrentHashMap<Integer, TestObject>();
  protected AtomicInteger idGenerator = new AtomicInteger(0);
  protected AtomicInteger uniqueValueGenerator = new AtomicInteger(0);
  protected AtomicInteger parameterIdGenerator = new AtomicInteger(0);
  
  protected final static String[] SINGLE_COLUMN_NAMES = new String[] {"column"};
  protected final static String[] MULTI_COLUMN_NAMES = new String[] {"column1","column2","column3"};
  protected final static String[] MULTI_COLUMN_NAMES_INCLUDING_COLUMN_FUNCTION = new String[] {"column1","MAX(column2,column3)"};
  
  public abstract IndexDefinition<TestObject, ? extends IndexKey, ? extends IndexSearchCriterion> getIndexDefinition(String[] indexedColumns);
  
  static {
    boolean assertsEnabled = false;
    assert assertsEnabled = true;
    if (!assertsEnabled)
        throw new RuntimeException("Asserts must be enabled!");
  }
  
  @Test
  public void testGetInSingleColumnOrderedIndex() {
    final int INSERTS = 1000;
    
    CompositeIndex compositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    
    for (int i=0; i < INSERTS; i++) {
      Map<String, Object> values = new HashMap<String, Object>();
      values.put(SINGLE_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
      TestObject testObject = new TestObject(values, idGenerator);
      storage.put(testObject.getObjectIndex(), testObject);
      compositeIndex.put(testObject, testObject.getObjectIndex());
    }
    
    int[] allValues = compositeIndex.values();
    Assert.assertEquals("all 100 unique values should be contained.",INSERTS, allValues.length);
    
    int randomId = RANDOMIZER.nextInt(storage.size());
    TestObject randomTestObject = storage.get(randomId);
    int[] internalIdsForRandomObject = compositeIndex.get(randomTestObject);
    
    Assert.assertEquals("Index should have returned 1 value", 1, internalIdsForRandomObject.length);
    Assert.assertEquals("Created object's id should have been contained", randomTestObject.getObjectIndex(), internalIdsForRandomObject[0]);
    
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(SINGLE_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
    TestObject uncontainedTestObject = new TestObject(values, idGenerator);
    Assert.assertEquals(0, compositeIndex.get(uncontainedTestObject).length);
    
  }
  
  
  @Test
  public void testGetInMultiColumnOrderedIndex() {
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
    
    int randomId = RANDOMIZER.nextInt(storage.size());
    TestObject randomTestObject = storage.get(randomId);
    int[] internalIdsForRandomObject = multiColumnCompositeIndex.get(randomTestObject);
    
    Assert.assertEquals("Index should have returned 1 value", 1, internalIdsForRandomObject.length);
    Assert.assertEquals("Created object's id should have been contained", randomTestObject.getObjectIndex(), internalIdsForRandomObject[0]);
    
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
    values.put(MULTI_COLUMN_NAMES[1], uniqueValueGenerator.incrementAndGet());
    values.put(MULTI_COLUMN_NAMES[2], uniqueValueGenerator.incrementAndGet());
    TestObject uncontainedTestObject = new TestObject(values, idGenerator);
    Assert.assertEquals(0, multiColumnCompositeIndex.get(uncontainedTestObject).length);
  }
  
  
  @Test
  public void testDeletion() {
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
    
    int randomId = RANDOMIZER.nextInt(storage.size()+1);
    TestObject randomTestObject = storage.get(randomId);
    int[] internalIdsForRandomObject = multiColumnCompositeIndex.get(randomTestObject);
    
    Assert.assertTrue("Index should have at least returned 1 value", internalIdsForRandomObject.length >= 1);
    List<Integer> internalIds = new ArrayList<Integer>();
    for (Integer integer : internalIdsForRandomObject) {
      internalIds.add(integer);
    }
    Assert.assertTrue("Created object's id should have been contained", internalIds.contains(randomTestObject.getObjectIndex()));
    
    multiColumnCompositeIndex.delete(randomTestObject, randomTestObject.getObjectIndex());
    int[] internalIdsForRandomObjectAfterDeletion = multiColumnCompositeIndex.get(randomTestObject);
    Assert.assertTrue("Created object's id should have no longer been contained and result therefore have been shorter",
                      internalIdsForRandomObject.length > internalIdsForRandomObjectAfterDeletion.length);
    Assert.assertFalse("Deleted object's id should not have been contained", idsContains(internalIdsForRandomObjectAfterDeletion, randomTestObject.getObjectIndex()));
  }
  
  
  @Test
  public void testSearchRequestWithEqualComparison() {
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
    
    int randomId = RANDOMIZER.nextInt(storage.size()+1);
    TestObject randomTestObject = storage.get(randomId);
    int[] internalIdsForRandomObject = multiColumnCompositeIndex.get(randomTestObject);
    
    SearchParameter params = buildSearchParameterFromTestObject(randomTestObject, MULTI_COLUMN_NAMES);
    
    int[] allColumnsEqualResult = multiColumnCompositeIndex.search(allColumnsEqual, params, -1).getInternalIds();
    Assert.assertTrue(Arrays.equals(internalIdsForRandomObject, allColumnsEqualResult));
    
    randomTestObject.set(MULTI_COLUMN_NAMES[2], uniqueValueGenerator.incrementAndGet());
    params = buildSearchParameterFromTestObject(randomTestObject, MULTI_COLUMN_NAMES);
    int[] allColumnsEqualResultAfterManipulation = multiColumnCompositeIndex.search(allColumnsEqual, params, -1).getInternalIds();
    Assert.assertEquals(0, allColumnsEqualResultAfterManipulation.length);
  }
  

  @Test
  public void testUpdate() {
    CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
    values.put(MULTI_COLUMN_NAMES[1], uniqueValueGenerator.incrementAndGet());
    values.put(MULTI_COLUMN_NAMES[2], uniqueValueGenerator.incrementAndGet());
    TestObject testObject = new TestObject(values, idGenerator);
    storage.put(testObject.getObjectIndex(), testObject);
    Assert.assertTrue(multiColumnCompositeIndex.put(testObject, testObject.getObjectIndex()));
    
    int[] allValues = multiColumnCompositeIndex.values();
    Assert.assertEquals(1, allValues.length);
    Assert.assertTrue(idsContains(allValues, testObject.getObjectIndex()));
    
    
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
    values.put(MULTI_COLUMN_NAMES[1], uniqueValueGenerator.incrementAndGet());
    values.put(MULTI_COLUMN_NAMES[2], uniqueValueGenerator.incrementAndGet());
    TestObject newTestObject = new TestObject(values, idGenerator);
    
    multiColumnCompositeIndex.update(testObject, newTestObject, testObject.getObjectIndex(), newTestObject.getObjectIndex());
    
    allValues = multiColumnCompositeIndex.values();
    Assert.assertEquals(1, allValues.length);
    Assert.assertTrue(idsContains(allValues, newTestObject.getObjectIndex()));
    
    Assert.assertEquals(0, multiColumnCompositeIndex.get(testObject).length);
    int[] internalIds = multiColumnCompositeIndex.get(newTestObject);
    Assert.assertEquals(1, internalIds.length);
    Assert.assertTrue(idsContains(internalIds, newTestObject.getObjectIndex()));
  }
  
  
  protected boolean idsContains(int[] ids, int id) {
    for (int i : ids) {
      if (i == id) {
        return true;
      }
    }
    return false;
  }
  
  
  protected SearchParameter buildSearchParameterFromTestObject(TestObject testOject, String[] columns) {
    SearchParameter params = new SearchParameter();
    for (int i=0; i < columns.length; i++) {
      params.addParameter(testOject.get(columns[i]));
    }
    return params;
  }
  
  
  @Test
  public void testCoverageResultWithEqualRequestOnSingleColumnIndex() {
    final String NOT_INDEXED_COLUMN_NAME1 = "notIndexedColumn1";
    final String NOT_INDEXED_COLUMN_NAME2 = "notIndexedColumn2";
    
    //CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    IndexDefinition multiColumnCompositeIndex = getIndexDefinition(SINGLE_COLUMN_NAMES);
    
    List<ColumnCriterion> ccs = new ArrayList<ColumnCriterion>();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.EQUALS, -1));
    float coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME1, SearchColumnOperator.EQUALS, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME1, SearchColumnOperator.EQUALS, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage >= 0.5f);
    
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME1, SearchColumnOperator.EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME2, SearchColumnOperator.EQUALS, -1));
    coverage = multiColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage >= 1/3);
  }
  
  
  @Test
  public void testCoverageResultWithEqualRequestOnMultiColumnIndex() {
    final String NOT_INDEXED_COLUMN_NAME1 = "notIndexedColumn1";
    final String NOT_INDEXED_COLUMN_NAME2 = "notIndexedColumn2";
    
    //CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    IndexDefinition multiColumnCompositeIndex = getIndexDefinition(SINGLE_COLUMN_NAMES);
    
    SearchCriterion crit = buildSearchCriterionForCoverage(MULTI_COLUMN_NAMES, operators("=","=","="));
    float coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 1.0f);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1]}, operators("in","="));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 0.0f);
    
    crit = buildSearchCriterionForCoverage(MULTI_COLUMN_NAMES, operators("=","=","like"));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 0.0f);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1}, operators("=","=","=","="));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage >= 3.0/4.0);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1, NOT_INDEXED_COLUMN_NAME2}, operators("=","=","=","=","="));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage >= 3.0/5.0);
  }
  
  
  @Test
  public void testGetValues() {
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
    
    int[] internalIds = multiColumnCompositeIndex.values();
    assertEquals(storage.size(), internalIds.length);
  }
  
  
  protected SearchCriterion buildSearchCriterionForCoverage(String[] columnNames, SearchColumnOperator[] operators) {
    List<ColumnCriterion> ccs = new ArrayList<ColumnCriterion>();
    int iterations = Math.min(columnNames.length, operators.length);
    for (int i=0; i< iterations; i++) {
      ccs.add(new ColumnCriterion(columnNames[i], operators[i], i));
    }
    return new UnfittingSearchCriterion(ccs);
  }
  
  
  protected SearchColumnOperator[] operators(String... identifier) {
    List<SearchColumnOperator> operatorList = new ArrayList<SearchColumnOperator>();
    for (String searchColumnOperator : identifier) {
      operatorList.add(SearchColumnOperator.getSearchColumnOperatorBySqlRepresentation(searchColumnOperator));
    }
    return operatorList.toArray(new SearchColumnOperator[operatorList.size()]);
  }
  

  private final static int OBJECTPOOL_SIZE = 1000;
  
  @Test
  public void testConcurrenctAccessOnSingleColumnIndex() throws InterruptedException, ExecutionException {
    CompositeIndex singleColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    
    List<TestObject> objectPool = Collections.synchronizedList(new ArrayList<TestObject>());
    
    for (int i=0; i < OBJECTPOOL_SIZE; i++) {
      Map<String, Object> values = new HashMap<String, Object>();
      values.put(SINGLE_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
      TestObject testObject = new TestObject(values, idGenerator);
      objectPool.add(testObject);
      singleColumnCompositeIndex.put(testObject, testObject.getObjectIndex());
    }
    
    int workers = 20;
    ExecutorService threadpool = Executors.newFixedThreadPool(workers);
    List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
    for (int i = 0; i < workers; i++) {
      futures.add(threadpool.submit(new ConcurrentIndexAction(objectPool, singleColumnCompositeIndex, uniqueValueGenerator, idGenerator)));
    }
    
    for (Future<Boolean> future : futures) {
      future.get();
    }
    
  }
  
  
  private static class ConcurrentIndexAction implements Callable<Boolean> {

    private final int ITERATIONS = 3000;
    private final int OBJECTPOOL_MINSIZE = 500;
    private Random random = new Random();
    private List<TestObject> objectPool;
    CompositeIndex index;
    AtomicInteger valueGenerator;
    AtomicInteger indexGenerator;
    
    ConcurrentIndexAction(List<TestObject> objectPool, CompositeIndex index, AtomicInteger valueGenerator, AtomicInteger indexGenerator) {
      this.objectPool = objectPool;
      this.index = index;
      this.valueGenerator = valueGenerator;
      this.indexGenerator = indexGenerator;
    }
    
    public Boolean call() throws Exception {
      for (int i = 0; i < ITERATIONS; i++) {
        int actionCode = random.nextInt(10);
        switch (actionCode) {
          case 0 : // get
            //choose a random element from storage
            TestObject obj = getRandomObjectFromStorage();
            // get that element from index
            int[] internalIds = index.get(obj);
            // if not index assert it's not in storage anymore as well (could be possible cause both ids are progressing in one direction)
            if (internalIds.length == 0) {
              if (objectPool.contains(obj)) {
                Thread.yield();
                assertFalse(objectPool.contains(obj));
              }
            } else {
              assertEquals(1, internalIds.length);
              // what more could we assert?
            }
            break;
          case 1 : // delete
            //choose a random element from storage
            int missing = OBJECTPOOL_SIZE - objectPool.size();
            if (missing >= OBJECTPOOL_MINSIZE) {
              for (int j = 0; j<missing; j++) {
                Map<String, Object> values = new HashMap<String, Object>();
                values.put(SINGLE_COLUMN_NAMES[0], valueGenerator.incrementAndGet());
                TestObject testObject = new TestObject(values, indexGenerator);
                index.put(testObject, testObject.getObjectIndex());
                objectPool.add(testObject);
              }
            }
            obj = getRandomObjectFromStorage();
            int objIndex = obj.getObjectIndex();
            if (objectPool.remove(obj)) {
              try {
                index.delete(obj, objIndex);
              } catch (AssertionError e) {
                internalIds = index.get(obj);
                assertEquals(0, internalIds.length);
              }
            }
            break;
          case 2 : // update internalId
            obj = getRandomObjectFromStorage();
            if (objectPool.remove(obj)) {
              int newIndex = indexGenerator.incrementAndGet();
              int oldIndex = obj.getObjectIndex();
              obj.setObjectIndex(newIndex);
              try {
                index.update(obj, obj, oldIndex, newIndex);
              } catch (AssertionError e) {
                try {
                  assertFalse(objectPool.contains(obj));
                } catch (AssertionError ee) {
                  throw ee;
                }
              }
            }
            //assert anything
            break;
          case 3 : // update object value
            obj = getRandomObjectFromStorage();
            Map<String, Object> values = new HashMap<String, Object>();
            values.put(SINGLE_COLUMN_NAMES[0], valueGenerator.incrementAndGet());
            TestObject newValue = new TestObject(values);
            newValue.setObjectIndex(obj.getObjectIndex());
            if (objectPool.remove(obj)) {
              try {
                index.update(obj, newValue, obj.getObjectIndex(), newValue.getObjectIndex());
              } catch (AssertionError e) {
                assertFalse(objectPool.contains(obj));
                assertEquals(0, index.get(newValue).length);
              }
              objectPool.add(newValue);
            }
            //assert anything
            break;
          case 4 : // update object value & internalId
            obj = getRandomObjectFromStorage();
            values = new HashMap<String, Object>();
            values.put(SINGLE_COLUMN_NAMES[0], valueGenerator.incrementAndGet());
            newValue = new TestObject(values, indexGenerator);
            if (objectPool.remove(obj)) {
              try {
                index.update(obj, newValue, obj.getObjectIndex(), newValue.getObjectIndex());
              } catch (AssertionError e) {
                assertFalse(objectPool.contains(obj));
                assertEquals(0, index.get(newValue).length);
              }
              objectPool.add(newValue);
            }
            break;
          case 5 : // getValues
            internalIds = index.values();
            // what to assert?
            break;
          default :
            break;
        }
      }
      return Boolean.TRUE;
    }
    
    private TestObject getRandomObjectFromStorage() {
      int randomId = random.nextInt(objectPool.size());
      TestObject obj =  null;
      try {
        obj = objectPool.get(randomId);
      } catch (IndexOutOfBoundsException e) { }
      if (obj == null) {
        return getRandomObjectFromStorage();
      } else {
        return obj;
      }
    }
    
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
    values.put(MULTI_COLUMN_NAMES[2], SMALL_VALUE);
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
    values.put(MULTI_COLUMN_NAMES[1], SMALL_VALUE);
    values.put(MULTI_COLUMN_NAMES[2], MEDIUM_VALUE);
    TestObject testObject2 = new TestObject(values, idGenerator);
    
    internalIds = compositeIndexWithColumnFunction.get(testObject2);
    assertEquals(0, internalIds.length);
    params = buildSearchParameterFromTestObject(testObject2, MULTI_COLUMN_NAMES);
    internalIds = compositeIndexWithColumnFunction.search(allColumnsEqual, params, -1).getInternalIds();
    assertEquals(0, internalIds.length);
    
    assertTrue(compositeIndexWithColumnFunction.put(testObject2, testObject2.getObjectIndex()));
    internalIds = compositeIndexWithColumnFunction.get(testObject2);
    assertTrue(idsContains(internalIds, testObject2.getObjectIndex()));
    assertEquals(1, internalIds.length);
    internalIds = compositeIndexWithColumnFunction.search(allColumnsEqual, params, -1).getInternalIds();
    assertTrue(idsContains(internalIds, testObject2.getObjectIndex()));
    assertEquals(1, internalIds.length);
    
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], COLUMN1_VALUE);
    values.put(MULTI_COLUMN_NAMES[1], SMALL_VALUE);
    values.put(MULTI_COLUMN_NAMES[2], HIGH_VALUE);
    TestObject testObject3 = new TestObject(values, idGenerator);
    
    internalIds = compositeIndexWithColumnFunction.get(testObject3);
    assertEquals(0, internalIds.length);
    params = buildSearchParameterFromTestObject(testObject3, MULTI_COLUMN_NAMES);
    internalIds = compositeIndexWithColumnFunction.search(allColumnsEqual, params, -1).getInternalIds();
    assertEquals(0, internalIds.length);
    
    assertTrue(compositeIndexWithColumnFunction.put(testObject3, testObject3.getObjectIndex()));
    internalIds = compositeIndexWithColumnFunction.get(testObject3);
    assertTrue(idsContains(internalIds, testObject3.getObjectIndex()));
    assertEquals(1, internalIds.length);
    internalIds = compositeIndexWithColumnFunction.search(allColumnsEqual, params, -1).getInternalIds();
    assertTrue(idsContains(internalIds, testObject3.getObjectIndex()));
    assertEquals(1, internalIds.length);
    
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], COLUMN1_VALUE);
    values.put(MULTI_COLUMN_NAMES[1], MEDIUM_VALUE);
    values.put(MULTI_COLUMN_NAMES[2], HIGH_VALUE);
    TestObject testObject4 = new TestObject(values, idGenerator);
    
    internalIds = compositeIndexWithColumnFunction.get(testObject4);
    assertTrue(idsContains(internalIds, testObject3.getObjectIndex()));
    assertEquals(1, internalIds.length);
    params = buildSearchParameterFromTestObject(testObject3, MULTI_COLUMN_NAMES);
    internalIds = compositeIndexWithColumnFunction.search(allColumnsEqual, params, -1).getInternalIds();
    assertTrue(idsContains(internalIds, testObject3.getObjectIndex()));
    assertEquals(1, internalIds.length);
  }
  
  
}
