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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;

import com.gip.xyna.xsor.indices.CompositeIndex;
import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.PessimisticOrderedIndex;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.helper.OrderedTestIndexDefinition;
import com.gip.xyna.xsor.indices.helper.TestObject;
import com.gip.xyna.xsor.indices.helper.UnfittingSearchCriterion;
import com.gip.xyna.xsor.indices.search.ColumnCriterion;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchColumnOperator;
import com.gip.xyna.xsor.indices.search.SearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchValue;


public class TestOrderedIndex extends AbstractMultiValueCompositeIndexTest {

  @Override
  public IndexDefinition<TestObject, ? extends IndexKey, ? extends IndexSearchCriterion> getIndexDefinition(String[] indexedColumns) {
    return new OrderedTestIndexDefinition(TestObject.TABLE_NAME, indexedColumns);
  }
  
  
  @Test
  public void testOrderedSearchRequestsOnSingleColumn() {
    final int INSERTS = 1000;

    CompositeIndex singleColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    
    for (int i=0; i < INSERTS; i++) {
      Map<String, Object> values = new HashMap<String, Object>();
      values.put(SINGLE_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
      TestObject testObject = new TestObject(values, idGenerator);
      storage.put(testObject.getObjectIndex(), testObject);
      singleColumnCompositeIndex.put(testObject, testObject.getObjectIndex());
    }
    
    int[] allValues = singleColumnCompositeIndex.values();
    Assert.assertEquals("all 1000 unique values should be contained.",INSERTS, allValues.length);

    // Closed Interval excluding borders [ ]
    SearchCriterion searchCriterion = UnfittingSearchCriterion.generateUnfittingSearchCriterion(new String[] {SINGLE_COLUMN_NAMES[0], SINGLE_COLUMN_NAMES[0]},new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.SMALLER}, parameterIdGenerator);
    
    int lowerBounds = 100;
    int upperBounds = 200;
    SearchParameter searchParams = new SearchParameter(new SearchValue(lowerBounds), new SearchValue(upperBounds));
    
    int[] searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(99, searchResult.length);
    Assert.assertFalse(idsContains(searchResult, lowerBounds-1));
    Assert.assertFalse(idsContains(searchResult, upperBounds+1));
    Assert.assertFalse(idsContains(searchResult, lowerBounds));
    Assert.assertFalse(idsContains(searchResult, upperBounds));
    Assert.assertTrue(idsContains(searchResult, lowerBounds+1));
    Assert.assertTrue(idsContains(searchResult, upperBounds-1));
    
    // read past value end
    lowerBounds = 995;
    upperBounds = 1005;
    searchParams = new SearchParameter(new SearchValue(lowerBounds), new SearchValue(upperBounds));
    searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(5, searchResult.length);
    Assert.assertFalse(idsContains(searchResult, lowerBounds));
    Assert.assertFalse(idsContains(searchResult, upperBounds));
    Assert.assertTrue(idsContains(searchResult, lowerBounds+1));
    Assert.assertFalse(idsContains(searchResult, upperBounds-1));
    Assert.assertTrue(idsContains(searchResult, INSERTS));
    
    // | ]
    parameterIdGenerator.set(0);
    lowerBounds = 100;
    upperBounds = 200;
    
    searchCriterion = UnfittingSearchCriterion.generateUnfittingSearchCriterion(new String[] {SINGLE_COLUMN_NAMES[0], SINGLE_COLUMN_NAMES[0]},new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS, SearchColumnOperator.SMALLER}, parameterIdGenerator);
    searchParams = new SearchParameter(new SearchValue(lowerBounds), new SearchValue(upperBounds));
    searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(100, searchResult.length);
    Assert.assertFalse(idsContains(searchResult, lowerBounds-1));
    Assert.assertFalse(idsContains(searchResult, upperBounds+1));
    Assert.assertTrue(idsContains(searchResult, lowerBounds));
    Assert.assertFalse(idsContains(searchResult, upperBounds));
    Assert.assertTrue(idsContains(searchResult, lowerBounds+1));
    Assert.assertTrue(idsContains(searchResult, upperBounds-1));
    
    // [ |
    parameterIdGenerator.set(0);
    
    searchCriterion = UnfittingSearchCriterion.generateUnfittingSearchCriterion(new String[] {SINGLE_COLUMN_NAMES[0], SINGLE_COLUMN_NAMES[0]},new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.SMALLER_EQUALS}, parameterIdGenerator);
    searchParams = new SearchParameter(new SearchValue(lowerBounds), new SearchValue(upperBounds));
    searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(100, searchResult.length);
    Assert.assertFalse(idsContains(searchResult, lowerBounds-1));
    Assert.assertFalse(idsContains(searchResult, upperBounds+1));
    Assert.assertFalse(idsContains(searchResult, lowerBounds));
    Assert.assertTrue(idsContains(searchResult, upperBounds));
    Assert.assertTrue(idsContains(searchResult, lowerBounds+1));
    Assert.assertTrue(idsContains(searchResult, upperBounds-1));
    
    // | |
    parameterIdGenerator.set(0);
    
    searchCriterion = UnfittingSearchCriterion.generateUnfittingSearchCriterion(new String[] {SINGLE_COLUMN_NAMES[0], SINGLE_COLUMN_NAMES[0]},new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS, SearchColumnOperator.SMALLER_EQUALS}, parameterIdGenerator);
    searchParams = new SearchParameter(new SearchValue(lowerBounds), new SearchValue(upperBounds));
    searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(101, searchResult.length);
    Assert.assertFalse(idsContains(searchResult, lowerBounds-1));
    Assert.assertFalse(idsContains(searchResult, upperBounds+1));
    Assert.assertTrue(idsContains(searchResult, lowerBounds));
    Assert.assertTrue(idsContains(searchResult, upperBounds));
    Assert.assertTrue(idsContains(searchResult, lowerBounds+1));
    Assert.assertTrue(idsContains(searchResult, upperBounds-1));
  }
  
  
  @Test
  public void testHalfOpenOrderedSearchRequestsOnSingleColumn() {
    final int INSERTS = 1000;

    CompositeIndex singleColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    
    for (int i=0; i < INSERTS; i++) {
      Map<String, Object> values = new HashMap<String, Object>();
      values.put(SINGLE_COLUMN_NAMES[0], uniqueValueGenerator.incrementAndGet());
      TestObject testObject = new TestObject(values, idGenerator);
      storage.put(testObject.getObjectIndex(), testObject);
      singleColumnCompositeIndex.put(testObject, testObject.getObjectIndex());
    }
    
    int[] allValues = singleColumnCompositeIndex.values();
    Assert.assertEquals("all 1000 unique values should be contained.",INSERTS, allValues.length);
    
    // [ ->
    SearchCriterion searchCriterion = UnfittingSearchCriterion.generateUnfittingSearchCriterion(SINGLE_COLUMN_NAMES,new SearchColumnOperator[] {SearchColumnOperator.GREATER}, parameterIdGenerator);
    
    int boundary = 500;
    SearchParameter searchParams = new SearchParameter(new SearchValue(boundary));
    
    int[] searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(500, searchResult.length);
    Assert.assertFalse(idsContains(searchResult, boundary-1));
    Assert.assertFalse(idsContains(searchResult, boundary));
    Assert.assertTrue(idsContains(searchResult, boundary+1));
    Assert.assertTrue(idsContains(searchResult, INSERTS));
    
    // | ->
    parameterIdGenerator.set(0);
    searchCriterion = UnfittingSearchCriterion.generateUnfittingSearchCriterion(SINGLE_COLUMN_NAMES,new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS}, parameterIdGenerator);
        
    searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(501, searchResult.length);
    Assert.assertFalse(idsContains(searchResult, boundary-1));
    Assert.assertTrue(idsContains(searchResult, boundary));
    Assert.assertTrue(idsContains(searchResult, boundary+1));
    Assert.assertTrue(idsContains(searchResult, INSERTS));
    
    //  (...) | ->
    searchParams = new SearchParameter(new SearchValue(INSERTS+1));
    searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(0, searchResult.length);
    
    // <- ]
    parameterIdGenerator.set(0);
    searchCriterion = UnfittingSearchCriterion.generateUnfittingSearchCriterion(SINGLE_COLUMN_NAMES,new SearchColumnOperator[] {SearchColumnOperator.SMALLER}, parameterIdGenerator);
    searchParams = new SearchParameter(new SearchValue(boundary));
    
    searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(499, searchResult.length);
    Assert.assertTrue(idsContains(searchResult, boundary-1));
    Assert.assertFalse(idsContains(searchResult, boundary));
    Assert.assertFalse(idsContains(searchResult, boundary+1));
    Assert.assertTrue(idsContains(searchResult, 1));
    
    // <- |
    parameterIdGenerator.set(0);
    searchCriterion = UnfittingSearchCriterion.generateUnfittingSearchCriterion(SINGLE_COLUMN_NAMES,new SearchColumnOperator[] {SearchColumnOperator.SMALLER_EQUALS}, parameterIdGenerator);
    
    searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(500, searchResult.length);
    Assert.assertTrue(idsContains(searchResult, boundary-1));
    Assert.assertTrue(idsContains(searchResult, boundary));
    Assert.assertFalse(idsContains(searchResult, boundary+1));
    Assert.assertTrue(idsContains(searchResult, 1));
    
    //  <- | (...)
    searchParams = new SearchParameter(new SearchValue(0));
    searchResult = singleColumnCompositeIndex.search(searchCriterion, searchParams, -1).getInternalIds();
    Assert.assertEquals(0, searchResult.length);
  }
  
  
  @Test
  public void testCreateSomeElementsAndSearch() {
    final String[] MULTI_COLUMN_NAMES = new String[] {"ip","leasetime"}; 
    
    CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    
    Map<String, Object> valueMap1 = new HashMap<String, Object>();
    valueMap1.put(MULTI_COLUMN_NAMES[0], "1.2.3.4"); valueMap1.put(MULTI_COLUMN_NAMES[1], 4124123L);
    TestObject testObject1 = new TestObject(valueMap1, idGenerator);
    multiColumnCompositeIndex.put(testObject1, testObject1.getObjectIndex());
    Map<String, Object> valueMap2 = new HashMap<String, Object>();
    valueMap2.put(MULTI_COLUMN_NAMES[0], "1.2.3.4"); valueMap2.put(MULTI_COLUMN_NAMES[1], 4124125L);
    TestObject testObject2 = new TestObject(valueMap2, idGenerator);
    multiColumnCompositeIndex.put(testObject2, testObject2.getObjectIndex());
    Map<String, Object> valueMap3 = new HashMap<String, Object>();
    valueMap3.put(MULTI_COLUMN_NAMES[0], "1.2.3.4"); valueMap3.put(MULTI_COLUMN_NAMES[1], 4124127L);
    TestObject testObject3 = new TestObject(valueMap3, idGenerator);
    multiColumnCompositeIndex.put(testObject3, testObject3.getObjectIndex());
    Map<String, Object> valueMap4 = new HashMap<String, Object>();
    valueMap4.put(MULTI_COLUMN_NAMES[0], "1.2.3.4"); valueMap4.put(MULTI_COLUMN_NAMES[1], 4124129L);
    TestObject testObject4 = new TestObject(valueMap4, idGenerator);
    multiColumnCompositeIndex.put(testObject4, testObject4.getObjectIndex());
    

    /*
     * suchintervalle von links nach rechts
     * 
     * testfall | intervallgrenzen | erwartetes suchergebnis
     *   ------------- 1 --- 2 --- 3 --- 4 ----------
     *  1[           ]leer
     *  2[           )leer
     *  3[             ]1
     *  4[             )leer
     *  5[                ]1
     *  6[                )1
     *  7              [  ]1
     *  8              [  )1
     *  9              (  ]leer
     * 10              (  )leer
     * 11[                   ]12
     * 12[                   )1
     * 13              [     ]12
     * 14              [     )1
     * 15              (     ]2
     * 16              (     )leer
     * 17                 [  ]2
     * 18                 [  )leer
     * 19                 (  ]2
     * 20                 (  )leer
     * 21[                      ]12
     * 22[                      )12
     * 23              [        ]12
     * 24              [        )12
     * 25              (        ]2
     * 26              (        )2
     * 27                 [     ]2
     * 28                 [     )2
     * 29                 (     ]2
     * 30                 (     )2
     * 31[                               ]1234
     * 32[                               )123
     * 33              [                 ]1234
     * 34              [                 )123
     * 35              (                 ]234
     * 36              (                 )23
     * 37                 [              ]234
     * 38                 [              )23
     * 39                 (              ]234
     * 40                 (              )23
     * 41                    [                 ]234
     * 42                    (                 ]34
     * 43                                [     ]4
     * 44                                (     ]leer
     * 45start==end, aber () => leer
     * 46start==end, []      => start
     * 46start>end, []     => leer
     */
    
    //1
    int[] result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 1L, true),
                                                    buildSearchParameter("1", 1L, true, "1.2.3.4", 1L, true), -1).getInternalIds();
    assertEquals(0, result.length);
    //2
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 1L, false),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 1L, false), -1).getInternalIds();
    assertEquals(0, result.length);
    //3
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124123L, true),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124123L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    //4
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124123L, false),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124123L, false), -1).getInternalIds();
    assertEquals(0, result.length);
    //5
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124124L, true),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124124L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    //6
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124124L, false),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124124L, false), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    //7
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, true, "1.2.3.4", 4124124L, true),
                                              buildSearchParameter("1.2.3.4", 4124123L, true, "1.2.3.4", 4124124L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    //8
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, true, "1.2.3.4", 4124124L, false),
                                              buildSearchParameter("1.2.3.4", 4124123L, true, "1.2.3.4", 4124124L, false), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    //9
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, false, "1.2.3.4", 4124124L, true),
                                              buildSearchParameter("1.2.3.4", 4124123L, false, "1.2.3.4", 4124124L, true), -1).getInternalIds();
    assertEquals(0, result.length);
    //10
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, false, "1.2.3.4", 4124124L, false),
                                              buildSearchParameter("1.2.3.4", 4124123L, false, "1.2.3.4", 4124124L, false), -1).getInternalIds();
    assertEquals(0, result.length);
    //11
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124125L, true),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124125L, true), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //12
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124125L, false),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124125L, false), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    //13
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, true, "1.2.3.4", 4124125L, true),
                                              buildSearchParameter("1.2.3.4", 4124123L, true, "1.2.3.4", 4124125L, true), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //14
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, true, "1.2.3.4", 4124125L, false),
                                              buildSearchParameter("1.2.3.4", 4124123L, true, "1.2.3.4", 4124125L, false), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    //15
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, false, "1.2.3.4", 4124125L, true),
                                              buildSearchParameter("1.2.3.4", 4124123L, false, "1.2.3.4", 4124125L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //16
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, false, "1.2.3.4", 4124125L, false),
                                              buildSearchParameter("1.2.3.4", 4124123L, false, "1.2.3.4", 4124125L, false), -1).getInternalIds();
    assertEquals(0, result.length);
    //17
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, true, "1.2.3.4", 4124125L, true),
                                              buildSearchParameter("1.2.3.4", 4124124L, true, "1.2.3.4", 4124125L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //18
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, true, "1.2.3.4", 4124125L, false),
                                              buildSearchParameter("1.2.3.4", 4124124L, true, "1.2.3.4", 4124125L, false), -1).getInternalIds();
    assertEquals(0, result.length);
    //19
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, false, "1.2.3.4", 4124125L, true),
                                              buildSearchParameter("1.2.3.4", 4124124L, false, "1.2.3.4", 4124125L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //20
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, false, "1.2.3.4", 4124125L, false),
                                              buildSearchParameter("1.2.3.4", 4124124L, false, "1.2.3.4", 4124125L, false), -1).getInternalIds();
    assertEquals(0, result.length);
    //21
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124126L, true),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124126L, true), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //22
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124126L, false),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124126L, false), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //23
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, true, "1.2.3.4", 4124126L, true),
                                              buildSearchParameter("1.2.3.4", 4124123L, true, "1.2.3.4", 4124126L, true), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //24
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, true, "1.2.3.4", 4124126L, false),
                                              buildSearchParameter("1.2.3.4", 4124123L, true, "1.2.3.4", 4124126L, false), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //25
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, false, "1.2.3.4", 4124126L, true),
                                              buildSearchParameter("1.2.3.4", 4124123L, false, "1.2.3.4", 4124126L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //26
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, false, "1.2.3.4", 4124126L, false),
                                              buildSearchParameter("1.2.3.4", 4124123L, false, "1.2.3.4", 4124126L, false), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //27
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, true, "1.2.3.4", 4124126L, true),
                                              buildSearchParameter("1.2.3.4", 4124124L, true, "1.2.3.4", 4124126L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //28
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, true, "1.2.3.4", 4124126L, false),
                                              buildSearchParameter("1.2.3.4", 4124124L, true, "1.2.3.4", 4124126L, false), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //29
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, false, "1.2.3.4", 4124126L, true),
                                              buildSearchParameter("1.2.3.4", 4124124L, false, "1.2.3.4", 4124126L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //30
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, false, "1.2.3.4", 4124126L, false),
                                              buildSearchParameter("1.2.3.4", 4124124L, false, "1.2.3.4", 4124126L, false), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    //31
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124129L, true),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124129L, true), -1).getInternalIds();
    assertEquals(4, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    assertTrue(idsContains(result, testObject4.getObjectIndex()));
    //32
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1", 1L, true, "1.2.3.4", 4124129L, false),
                                              buildSearchParameter("1", 1L, true, "1.2.3.4", 4124129L, false), -1).getInternalIds();
    assertEquals(3, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    //33
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, true, "1.2.3.4", 4124129L, true),
                                              buildSearchParameter("1.2.3.4", 4124123L, true, "1.2.3.4", 4124129L, true), -1).getInternalIds();
    assertEquals(4, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    assertTrue(idsContains(result, testObject4.getObjectIndex()));
    //34
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, true, "1.2.3.4", 4124129L, false),
                                              buildSearchParameter("1.2.3.4", 4124123L, true, "1.2.3.4", 4124129L, false), -1).getInternalIds();
    assertEquals(3, result.length);
    assertTrue(idsContains(result, testObject1.getObjectIndex()));
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    //35
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, false, "1.2.3.4", 4124129L, true),
                                              buildSearchParameter("1.2.3.4", 4124123L, false, "1.2.3.4", 4124129L, true), -1).getInternalIds();
    assertEquals(3, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    assertTrue(idsContains(result, testObject4.getObjectIndex()));
    //36
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124123L, false, "1.2.3.4", 4124129L, false),
                                              buildSearchParameter("1.2.3.4", 4124123L, false, "1.2.3.4", 4124129L, false), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    //37
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, true, "1.2.3.4", 4124129L, true),
                                              buildSearchParameter("1.2.3.4", 4124124L, true, "1.2.3.4", 4124129L, true), -1).getInternalIds();
    assertEquals(3, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    assertTrue(idsContains(result, testObject4.getObjectIndex()));
    //38
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, true, "1.2.3.4", 4124129L, false),
                                              buildSearchParameter("1.2.3.4", 4124124L, true, "1.2.3.4", 4124129L, false), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    //39
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, false, "1.2.3.4", 4124129L, true),
                                              buildSearchParameter("1.2.3.4", 4124124L, false, "1.2.3.4", 4124129L, true), -1).getInternalIds();
    assertEquals(3, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    assertTrue(idsContains(result, testObject4.getObjectIndex()));
    //40
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124124L, false, "1.2.3.4", 4124129L, false),
                                              buildSearchParameter("1.2.3.4", 4124124L, false, "1.2.3.4", 4124129L, false), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    //41
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124125L, true, "2", 1L, true),
                                              buildSearchParameter("1.2.3.4", 4124125L, true, "2", 1L, true), -1).getInternalIds();
    assertEquals(3, result.length);
    assertTrue(idsContains(result, testObject2.getObjectIndex()));
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    assertTrue(idsContains(result, testObject4.getObjectIndex()));
    //42
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124125L, false, "2", 1L, false),
                                              buildSearchParameter("1.2.3.4", 4124125L, false, "2", 1L, false), -1).getInternalIds();
    assertEquals(2, result.length);
    assertTrue(idsContains(result, testObject3.getObjectIndex()));
    assertTrue(idsContains(result, testObject4.getObjectIndex()));
    //43
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124129L, true, "2", 1L, true),
                                              buildSearchParameter("1.2.3.4", 4124129L, true, "2", 1L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject4.getObjectIndex()));
    //44
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124129L, false, "2", 1L, false),
                                              buildSearchParameter("1.2.3.4", 4124129L, false, "2", 1L, false), -1).getInternalIds();
    assertEquals(0, result.length);
    //45
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124129L, false, "1.2.3.4", 4124129L, false),
                                              buildSearchParameter("1.2.3.4", 4124129L, false, "1.2.3.4", 4124129L, false), -1).getInternalIds();
    assertEquals(0, result.length);
    //46
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124129L, true, "1.2.3.4", 4124129L, true),
                                              buildSearchParameter("1.2.3.4", 4124129L, true, "1.2.3.4", 4124129L, true), -1).getInternalIds();
    assertEquals(1, result.length);
    assertTrue(idsContains(result, testObject4.getObjectIndex()));
    //47
    result = multiColumnCompositeIndex.search(buildSearchCriterion("1.2.3.4", 4124130L, false, "1.2.3.4", 4124129L, false),
                                              buildSearchParameter("1.2.3.4", 4124130L, false, "1.2.3.4", 4124129L, false), -1).getInternalIds();
    assertEquals(0, result.length);
  }
  
  
  private SearchCriterion buildSearchCriterion(String ipStart, long leaseTimeStart, boolean includingStart, String ipEnd, long leaseTimeEnd, boolean includingEnd) {
    List<ColumnCriterion> list = new ArrayList<ColumnCriterion>();
    SearchColumnOperator startKeyOperator;
    if (includingStart) {
      startKeyOperator = SearchColumnOperator.GREATER_EQUALS;
    } else {
      startKeyOperator = SearchColumnOperator.GREATER;
    }
    ColumnCriterion ipStartCC = new ColumnCriterion("ip", startKeyOperator, 0);
    //ColumnCriterion ipStartCC = new ColumnCriterion("ip", SearchColumnOperator.GREATER, 0);
    list.add(ipStartCC);
    ColumnCriterion leaseTimeStartCC = new ColumnCriterion("leasetime", startKeyOperator, 1);
    list.add(leaseTimeStartCC);
    SearchColumnOperator stopKeyOperator;
    if (includingEnd) {
      stopKeyOperator = SearchColumnOperator.SMALLER_EQUALS;
    } else {
      stopKeyOperator = SearchColumnOperator.SMALLER;
    }
    ColumnCriterion ipEndCC = new ColumnCriterion("ip", stopKeyOperator, 2);
    //ColumnCriterion ipEndCC = new ColumnCriterion("ip", SearchColumnOperator.SMALLER, 2);
    list.add(ipEndCC);
    ColumnCriterion leaseTimeStopCC = new ColumnCriterion("leasetime", stopKeyOperator, 3);
    list.add(leaseTimeStopCC);
    return new UnfittingSearchCriterion(list);
  }
  
  private SearchParameter buildSearchParameter(String ipStart, long leaseTimeStart, boolean includingStart, String ipEnd, long leaseTimeEnd, boolean includingEnd) {
    return new SearchParameter(new SearchValue(ipStart), new SearchValue(leaseTimeStart), new SearchValue(ipEnd), new SearchValue(leaseTimeEnd));
  }
  
  /*
    scenarios
    cols: boolean, string, long
    to1: true, baum, 3
    to2: true, baum, 5
    to3: true, baum, 7
    to4: false, baum, 4
    to5: false, baum, 6
    ----------------------------
    > true, baum, 7 : []
    >= true, baum, 7 : [3]
    > true, baum, 6 : [3]
    > true, baum, 4 : [2,3]
    > true, baum, 2 : [1,2,3]
    >= true, baum, 0 : [1,2,3]
    > false, baum, 7: [1,2,3]
    >= false, baum, 7: [1,2,3]
    > false, baum, 6: [1,2,3]
    > false, baum, 4: [1,2,3,5]
    > false, baum, 2: [1,2,3,4,5]
    >= false, baum, 0: [1,2,3,4,5]
    
    < true, baum, 0: [4,5]
    < true, baum, 3: [4,5]
    <= true, baum, 3: [1,4,5]
    < true, baum, 5: [1,4,5]
    < true, baum, 7: [1,2,4,5]
    < true, baum, 8: [1,2,3,4,5]
    < false, baum, 4: []
    <= false, baum, 4: [4]
    < false, baum, 5: [4]
    < false, baum, 7: [4,5]
   */
  
  @Test
  public void testHalfOpenSearchRequestsOnMultipleColumns() {
    final String STATIC_COL2_VALUE = "baum";
    CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    TestObject to1 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(true, STATIC_COL2_VALUE, 3, 1);
    TestObject to2 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(true, STATIC_COL2_VALUE, 5, 2);
    TestObject to3 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(true, STATIC_COL2_VALUE, 7, 3);
    TestObject to4 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(false, STATIC_COL2_VALUE, 4, 4);
    TestObject to5 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(false, STATIC_COL2_VALUE, 6, 5);
    
    assertTrue(multiColumnCompositeIndex.put(to1, to1.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to2, to2.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to3, to3.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to4, to4.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to5, to5.getObjectIndex()));
    
    int[] internalIds;
    SearchParameter searchParameter;
    SearchCriterion searchCriterion;
    
// > true, baum, 7 : []
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 7);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[0], internalIds);
    
// >= true, baum, 7 : [3]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER_EQUALS);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 7);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {3}, internalIds);
    
// > true, baum, 6 : [3]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 6);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {3}, internalIds);
    
// > true, baum, 4 : [2,3]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 4);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {2,3}, internalIds);
    
// > true, baum, 2 : [1,2,3]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 2);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
// >= true, baum, 0 : [1,2,3]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER_EQUALS);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 0);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
// > false, baum, 7: [1,2,3]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 7);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
// >= false, baum, 7: [1,2,3]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER_EQUALS);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 7);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
// > false, baum, 6: [1,2,3]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 6);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
// > false, baum, 4: [1,2,3,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 4);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,5}, internalIds);
    
// > false, baum, 2: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 2);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
// >= false, baum, 0: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.GREATER_EQUALS);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 0);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
    
// < true, baum, 0: [4,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 0);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4,5}, internalIds);
    
// < true, baum, 3: [4,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 3);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4,5}, internalIds);
    
// <= true, baum, 3: [1,4,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER_EQUALS);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 3);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,4,5}, internalIds);
    
// < true, baum, 5: [1,4,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 5);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,4,5}, internalIds);
    
// < true, baum, 7: [1,2,4,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 7);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,4,5}, internalIds);
    
// < true, baum, 8: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(true, STATIC_COL2_VALUE, 8);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
    
// < false, baum, 4: []
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 4);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[0], internalIds);
    
// <= false, baum, 4: [4]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER_EQUALS);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 4);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4}, internalIds);
    
// < false, baum, 5: [4]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 5);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4}, internalIds);
    
// < false, baum, 7: [4,5]
    searchCriterion = buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator.SMALLER);
    searchParameter = buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(false, STATIC_COL2_VALUE, 7);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4,5}, internalIds);
    
  }
  
  private TestObject generateTestObjectForScenarioBasedTestsOnMultipleColumns(boolean col1, String col2, long col3, int objectId) {
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], col1);
    values.put(MULTI_COLUMN_NAMES[1], col2);
    values.put(MULTI_COLUMN_NAMES[2], col3);
    return new TestObject(values, objectId);
  }
  
  private void assertSuccess(int[] expectedIds, int[] recievedIds) {
    assertEquals(expectedIds.length, recievedIds.length);
    for (int i : expectedIds) {
      assertTrue(idsContains(recievedIds, i));
    }
  }
  
  private SearchCriterion buildSearchCriterionForTestHalfOpenSearchRequestsOnMultipleColumns(SearchColumnOperator operator) {
    List<ColumnCriterion> ccs = new ArrayList<ColumnCriterion>();
    ccs.add(new ColumnCriterion(MULTI_COLUMN_NAMES[0], operator, 0));
    ccs.add(new ColumnCriterion(MULTI_COLUMN_NAMES[1], SearchColumnOperator.EQUALS, 1));
    ccs.add(new ColumnCriterion(MULTI_COLUMN_NAMES[2], operator, 2));
    return new UnfittingSearchCriterion(ccs);
  }
  
  private SearchParameter buildSearchParameterForTestHalfOpenSearchRequestsOnMultipleColumns(boolean col1, String col2, long col3) {
    return new SearchParameter(new SearchValue(col1), new SearchValue(col2), new SearchValue(col3)); 
  }
  
  
  /*
   * scenarios
    cols: boolean, string, long
    to1: true, baum, 3
    to2: true, baum, 5
    to3: true, wald, 7
    to4: false, baum, 4
    to5: false, baum, 6
    ----------------------------
    > false baum 0 & < true: [4,5]
    > false baum 0 & < true wald: [1,2,4,5]
    > false baum 0 & <= true wald: [1,2,3,4,5]
    > false baum 4 & <= true wald: [1,2,3,5]
    > false baum 4 & < true wald: [1,2,5]
    > false baum 6 & < true wald: [1,2]
    >= false baum 6 & < true baum: [5]
    > false baum 6 & < true: []
    
    
    > false & < true baum 0: []
    > false & < true wald 8: [1,2,3]
    >= false & < true wald 8: [1,2,3,4,5]
    >= false baum & < true wald 8: [1,2,3,4,5]
    > false baum & < true wald 8: [1,2,3]
    >= false & < true wald 7: [1,2,4,5]
   */
  @Test
  public void testPartialOpenSearchRequests() {
    CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    TestObject to1 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(true, "baum", 3, 1);
    TestObject to2 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(true, "baum", 5, 2);
    TestObject to3 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(true, "wald", 7, 3);
    TestObject to4 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(false, "baum", 4, 4);
    TestObject to5 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(false, "baum", 6, 5);
    
    assertTrue(multiColumnCompositeIndex.put(to1, to1.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to2, to2.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to3, to3.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to4, to4.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to5, to5.getObjectIndex()));
    
    int[] internalIds;
    SearchParameter searchParameter;
    SearchCriterion searchCriterion;
    
    
//    > false baum 0 & < true: [4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", 0L, true, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4,5}, internalIds);
    
//    > false baum 0 & < true wald: [1,2,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", 0L, true, "wald", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,4,5}, internalIds);
    
//    > false baum 0 & <= true wald: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER_EQUALS, SearchColumnOperator.SMALLER_EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", 0L, true, "wald", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
//    > false baum 4 & <= true wald: [1,2,3,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER_EQUALS, SearchColumnOperator.SMALLER_EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", 4L, true, "wald", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,5}, internalIds);
    
//    > false baum 4 & < true wald: [1,2,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", 4L, true, "wald", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,5}, internalIds);
    
//    > false baum 5 & < true wald: [1,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", 6L, true, "wald", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2}, internalIds);
    
//    >= false baum 6 & < true baum: [5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS, SearchColumnOperator.GREATER_EQUALS, SearchColumnOperator.GREATER_EQUALS},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", 6L, true, "baum", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {5}, internalIds);
    
//    > false baum 6 & < true: []
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", 6L, true, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {}, internalIds);

    
//    > false & < true baum 0: []
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", 6L, true, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {}, internalIds);
    
//    > false & < true wald 8: [1,2,3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, null, null, true, "wald", 8L);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
//    >= false & < true wald 8: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, null, null, true, "wald", 8L);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
//    >= false baum & < true wald 8: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS, SearchColumnOperator.GREATER_EQUALS},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", null, true, "wald", 8L);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
//    > false baum & < true wald 8: [1,2,3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", null, true, "wald", 8L);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
//    >= false & < true wald 7: [1,2,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, null, null, true, "wald", 7L);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,4,5}, internalIds);
  }
  
  
  private SearchCriterion buildSearchCriterionForPartialOpenSearchRequests(SearchColumnOperator[] startcomps, SearchColumnOperator[] stopcomps) {
    List<ColumnCriterion> ccs = new ArrayList<ColumnCriterion>();
    int mappingId = 0;
    for (int i = 0; i < startcomps.length; i++) {
      ccs.add(new ColumnCriterion(MULTI_COLUMN_NAMES[i], startcomps[i], mappingId));
      mappingId++;
    }
    for (int i = 0; i < stopcomps.length; i++) {
      ccs.add(new ColumnCriterion(MULTI_COLUMN_NAMES[i], stopcomps[i], mappingId));
      mappingId++;
    }    
    return new UnfittingSearchCriterion(ccs);
  }
  
  
  private SearchParameter buildSearchParameterForPartialOpenSearchRequests(Boolean col1_1, String col2_1, Long col3_1, Boolean col1_2, String col2_2, Long col3_2) {
    SearchParameter sp = new SearchParameter();
    if (col1_1 != null) {
      sp.addParameter(col1_1);
    }
    if (col2_1 != null) {
      sp.addParameter(col2_1);
    }
    if (col3_1 != null) {
      sp.addParameter(col3_1);
    }
    if (col1_2 != null) {
      sp.addParameter(col1_2);
    }
    if (col2_2 != null) {
      sp.addParameter(col2_2);
    }
    if (col3_2 != null) {
      sp.addParameter(col3_2);
    }
    return sp;
  }
  
  private SearchParameter buildSearchParameterForColumnFunctionSearchRequests(String col1_1, Integer col2_1, Integer col3_1, String col1_2, Integer col2_2, Integer col3_2) {
    SearchParameter sp = new SearchParameter();
    if (col1_1 != null) {
      sp.addParameter(col1_1);
    }
    if (col2_1 != null) {
      sp.addParameter(col2_1);
    }
    if (col3_1 != null) {
      sp.addParameter(col3_1);
    }
    if (col1_2 != null) {
      sp.addParameter(col1_2);
    }
    if (col2_2 != null) {
      sp.addParameter(col2_2);
    }
    if (col3_2 != null) {
      sp.addParameter(col3_2);
    }
    return sp;
  }
  
  
  @Test
  public void testCoverageForOrderedIndexOnSingleColumnIndex() {
    final String NOT_INDEXED_COLUMN_NAME1 = "notIndexedColumn1";
    final String NOT_INDEXED_COLUMN_NAME2 = "notIndexedColumn2";
    
    //CompositeIndex singleColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(SINGLE_COLUMN_NAMES));
    IndexDefinition singleColumnCompositeIndex = getIndexDefinition(SINGLE_COLUMN_NAMES);
    
    List<ColumnCriterion> ccs = new ArrayList<ColumnCriterion>();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.EQUALS, -1));
    float coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);

    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.GREATER, -1));
    coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.GREATER_EQUALS, -1));
    coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.SMALLER, -1));
    coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.SMALLER_EQUALS, -1));
    coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.LIKE, -1));
    coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.IN, -1));
    coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.SMALLER_EQUALS, -1));
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.GREATER_EQUALS, -1));
    coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 1.0f);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.SMALLER_EQUALS, -1));
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.GREATER_EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME1, SearchColumnOperator.EQUALS, -1));
    coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage <= 2.0/2.9);
    assertTrue(coverage >= 2.0/3.1);
    
    ccs.clear();
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.SMALLER_EQUALS, -1));
    ccs.add(new ColumnCriterion(SINGLE_COLUMN_NAMES[0], SearchColumnOperator.GREATER_EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME1, SearchColumnOperator.EQUALS, -1));
    ccs.add(new ColumnCriterion(NOT_INDEXED_COLUMN_NAME2, SearchColumnOperator.EQUALS, -1));
    coverage = singleColumnCompositeIndex.coverage(new UnfittingSearchCriterion(ccs));
    assertTrue(coverage == 0.5f);
  }
  
  
  @Test
  public void testCoverageForOrderedIndexOnMultiColumnIndex() {
    final String NOT_INDEXED_COLUMN_NAME1 = "notIndexedColumn1";
    final String NOT_INDEXED_COLUMN_NAME2 = "notIndexedColumn2";
    
    //CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    IndexDefinition multiColumnCompositeIndex = getIndexDefinition(MULTI_COLUMN_NAMES);
    
    SearchCriterion crit = buildSearchCriterionForCoverage(MULTI_COLUMN_NAMES, operators(">",">",">"));
    float coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage == 1.0f);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], NOT_INDEXED_COLUMN_NAME1}, operators(">",">",">"));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage <= (PessimisticOrderedIndex.MAX_COVERAGE_FOR_PARTIALCOVERAGE / 3.0) * 2.1);
    assertTrue(coverage >= (PessimisticOrderedIndex.MAX_COVERAGE_FOR_PARTIALCOVERAGE / 3.0) * 1.9);
    

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1},
                                           operators(">",">",">",">"));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage <= 3.0/3.9);
    assertTrue(coverage >= 3.0/4.1);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], MULTI_COLUMN_NAMES[0], NOT_INDEXED_COLUMN_NAME1},
                                           operators(">",">",">","<",">"));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage <= 4.0/4.9);
    assertTrue(coverage >= 4.0/5.1);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1},
                                           operators(">",">",">","<","<","<","="));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage <= 6.0/6.9);
    assertTrue(coverage >= 6.0/7.1);
    
    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], NOT_INDEXED_COLUMN_NAME1, NOT_INDEXED_COLUMN_NAME2},
                                           operators(">",">",">","<","<","<","=","="));
    coverage = multiColumnCompositeIndex.coverage(crit);
    assertTrue(coverage <= 6.0/7.9);
    assertTrue(coverage >= 6.0/8.1);
  }
  
  
  /*
   * scenarios
    cols: boolean, string, long
    to1: true, baum, 3
    to2: true, baum, 5
    to3: true, wald, 7
    to4: false, baum, 4
    to5: false, baum, 6
    ----------------------------
    > true: [1,2,3]
    > false: [1,2,3]
    >= false: [1,2,3,4,5]
    >= false wald : [1,2,3]
    >= true: [1,2,3]
    >= true baum: [1,2,3]
    > true baum: [3]
    >= true wald: [3]
    > true wald: []
    
    < false: []
    < true: [4,5]
    <= true: [1,2,3,4,5]
    <= true, xyz: [1,2,3,4,5]
    <= true wald: [1,2,3,4,5]
    < true wald: [1,2,4,5]
    <= false: [4,5]
    <= false baum: [4,5]
    < false baum: []
    
    == true: [4,5]
    == false: [1,2,3]
    == true baum: [1,2]
    == true wald: [3]
    == false baum: [4,5]

   */
  @Test
  public void wayOpenSearchRequest() {
    CompositeIndex multiColumnCompositeIndex = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES));
    TestObject to1 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(true, "baum", 3, 1);
    TestObject to2 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(true, "baum", 5, 2);
    TestObject to3 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(true, "wald", 7, 3);
    TestObject to4 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(false, "baum", 4, 4);
    TestObject to5 = generateTestObjectForScenarioBasedTestsOnMultipleColumns(false, "baum", 6, 5);
    
    assertTrue(multiColumnCompositeIndex.put(to1, to1.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to2, to2.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to3, to3.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to4, to4.getObjectIndex()));
    assertTrue(multiColumnCompositeIndex.put(to5, to5.getObjectIndex()));
    
    int[] internalIds;
    SearchParameter searchParameter;
    SearchCriterion searchCriterion;
    
    
//             > true: [1,2,3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[0]);
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, null, null, null, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
//             > false: [1,2,3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[0]);
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, null, null, null, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
//             >= false: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS},
                                                                       new SearchColumnOperator[0]);
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, null, null, null, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
//             >= false wald : [1,2,3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS, SearchColumnOperator.GREATER_EQUALS},
                                                                       new SearchColumnOperator[0]);
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "wald", null, null, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
//             >= true: [1,2,3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS},
                                                                       new SearchColumnOperator[0]);
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(true, null, null, null, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
//             >= true baum: [1,2,3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS, SearchColumnOperator.GREATER_EQUALS},
                                                                       new SearchColumnOperator[0]);
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(true, "baum", null, null, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
//             > true baum: [3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[0]);
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(true, "baum", null, null, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {3}, internalIds);
    
//             >= true wald: [3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER_EQUALS, SearchColumnOperator.GREATER_EQUALS},
                                                                       new SearchColumnOperator[0]);
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(true, "wald", null, null, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {3}, internalIds);
    
//             > true wald: []
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.GREATER, SearchColumnOperator.GREATER},
                                                                       new SearchColumnOperator[0]);
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(true, "wald", null, null, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[0], internalIds);
    
    
//             < false: []
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[0],
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(null, null, null, false, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[0], internalIds);
    
//             < true: [4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[0],
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(null, null, null, true, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4,5}, internalIds);
    
//             <= true: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[0],
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER_EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(null, null, null, true, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
//             <= true, xyz: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[0],
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER_EQUALS, SearchColumnOperator.SMALLER_EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(null, null, null, true, "xyz", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
//             <= true wald: [1,2,3,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[0],
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER_EQUALS, SearchColumnOperator.SMALLER_EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(null, null, null, true, "wald", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3,4,5}, internalIds);
    
//             < true wald: [1,2,4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[0],
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(null, null, null, true, "wald", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,4,5}, internalIds);
    
//             <= false: [4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[0],
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER_EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(null, null, null, false, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4,5}, internalIds);
    
//             <= false baum: [4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[0],
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER_EQUALS, SearchColumnOperator.SMALLER_EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(null, null, null, false, "baum", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4,5}, internalIds);
    
//             < false baum: []
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[0],
                                                                       new SearchColumnOperator[] {SearchColumnOperator.SMALLER, SearchColumnOperator.SMALLER});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(null, null, null, false, "baum", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[0], internalIds);
    
    
//      == true: [1,2,3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.EQUALS},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(true, null, null, true, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);
    
//      == false: [4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.EQUALS},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, null, null, false, null, null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4,5}, internalIds);
    
    
//      == true baum: [1,2]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.EQUALS, SearchColumnOperator.EQUALS},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.EQUALS, SearchColumnOperator.EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(true, "baum", null, true, "baum", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    //assertSuccess(new int[0], internalIds);
    
//      == true wald: [3]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.EQUALS, SearchColumnOperator.EQUALS},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.EQUALS, SearchColumnOperator.EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(true, "wald", null, true, "wald", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {3}, internalIds);
    
//      == false baum: [4,5]
    searchCriterion = buildSearchCriterionForPartialOpenSearchRequests(new SearchColumnOperator[] {SearchColumnOperator.EQUALS, SearchColumnOperator.EQUALS},
                                                                       new SearchColumnOperator[] {SearchColumnOperator.EQUALS, SearchColumnOperator.EQUALS});
    searchParameter = buildSearchParameterForPartialOpenSearchRequests(false, "baum", null, false, "baum", null);
    internalIds = multiColumnCompositeIndex.search(searchCriterion, searchParameter, -1).getInternalIds();
    assertSuccess(new int[] {4,5}, internalIds);
  }
  
    
  @Test
  public void testSearchWithColumnFunction() {
    
    CompositeIndex compositeIndexWithColumnFunction = indexFactory.createIndex(getIndexDefinition(MULTI_COLUMN_NAMES_INCLUDING_COLUMN_FUNCTION));
    
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], "wald");
    values.put(MULTI_COLUMN_NAMES[1], 1);
    values.put(MULTI_COLUMN_NAMES[2], 5);
    TestObject testObject1 = new TestObject(values, idGenerator);
    assertTrue(compositeIndexWithColumnFunction.put(testObject1, testObject1.getObjectIndex()));
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], "wald");
    values.put(MULTI_COLUMN_NAMES[1], 10);
    values.put(MULTI_COLUMN_NAMES[2], 5);
    TestObject testObject2 = new TestObject(values, idGenerator);
    assertTrue(compositeIndexWithColumnFunction.put(testObject2, testObject2.getObjectIndex()));
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], "wald");
    values.put(MULTI_COLUMN_NAMES[1], 1);
    values.put(MULTI_COLUMN_NAMES[2], 10);
    TestObject testObject3 = new TestObject(values, idGenerator);
    assertTrue(compositeIndexWithColumnFunction.put(testObject3, testObject3.getObjectIndex()));
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], "baum");
    values.put(MULTI_COLUMN_NAMES[1], 1);
    values.put(MULTI_COLUMN_NAMES[2], 5);
    TestObject testObject4 = new TestObject(values, idGenerator);
    assertTrue(compositeIndexWithColumnFunction.put(testObject4, testObject4.getObjectIndex()));
    values = new HashMap<String, Object>();
    values.put(MULTI_COLUMN_NAMES[0], "baum");
    values.put(MULTI_COLUMN_NAMES[1], 10);
    values.put(MULTI_COLUMN_NAMES[2], 1);
    TestObject testObject5 = new TestObject(values, idGenerator);
    assertTrue(compositeIndexWithColumnFunction.put(testObject5, testObject5.getObjectIndex()));
    
    SearchCriterion crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0]},
                                                           operators(">"));
    SearchParameter param = buildSearchParameterForColumnFunctionSearchRequests("baum", null, null, null, null, null);
    int[] internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1]},
                                                           operators(">", ">"));
    param = buildSearchParameterForColumnFunctionSearchRequests("wald", 1, null, null, null, null);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1]},
                                           operators(">=", ">="));
    param = buildSearchParameterForColumnFunctionSearchRequests("wald", 5, null, null, null, null);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1, 2, 3}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1]},
                                           operators(">", ">"));
    param = buildSearchParameterForColumnFunctionSearchRequests("baum", 5, null, null, null, null);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1, 2, 3, 5}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2]},
                                           operators(">", ">", ">"));
    param = buildSearchParameterForColumnFunctionSearchRequests("baum", 5, 1, null, null, null);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1, 2, 3, 5}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2]},
                                           operators(">=", ">=", ">="));
    param = buildSearchParameterForColumnFunctionSearchRequests("baum", 5, 10, null, null, null);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1, 2, 3, 5}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2]},
                                           operators(">", ">", ">"));
    param = buildSearchParameterForColumnFunctionSearchRequests("baum", 1, 10, null, null, null);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1, 2, 3}, internalIds);
    

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1]},
                                           operators(">", ">"));
    param = buildSearchParameterForColumnFunctionSearchRequests("wald", 5, null, null, null, null);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {2,3}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1]},
                                           operators(">=", ">="));
    param = buildSearchParameterForColumnFunctionSearchRequests("wald", 5, null, null, null, null);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1,2,3}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2]},
                                           operators(">=", ">=", ">=", "<", "<", "<"));
    param = buildSearchParameterForColumnFunctionSearchRequests("baum", 1, 5, "wald", 1, 10);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1,4,5}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2]},
                                           operators(">", ">", ">", "<", "<", "<"));
    param = buildSearchParameterForColumnFunctionSearchRequests("baum", 1, 5, "wald", 10, 1);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {1,5}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2]},
                                           operators(">", ">", ">", "<", "<", "<"));
    param = buildSearchParameterForColumnFunctionSearchRequests("baum", 5, 1, "wald", 1, 5);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {5}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2]},
                                           operators(">", ">", ">", "<", "<", "<"));
    param = buildSearchParameterForColumnFunctionSearchRequests("baum", 5, 9, "wald", 5, 1);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {5}, internalIds);

    crit = buildSearchCriterionForCoverage(new String[] {MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2], MULTI_COLUMN_NAMES[0], MULTI_COLUMN_NAMES[1], MULTI_COLUMN_NAMES[2]},
                                           operators(">", ">", ">", "<", "<", "<"));
    param = buildSearchParameterForColumnFunctionSearchRequests("baum", 5, 10, "wald", 5, 1);
    internalIds = compositeIndexWithColumnFunction.search(crit, param, -1).getInternalIds();
    assertSuccess(new int[] {}, internalIds);
    
  }
  
}
