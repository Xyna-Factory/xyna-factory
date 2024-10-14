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
package com.gip.xyna.persistence.xsor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.gip.xyna.xsor.indices.IndexKey;
import com.gip.xyna.xsor.indices.definitions.IndexDefinition;
import com.gip.xyna.xsor.indices.search.ComparisionAlgorithm;
import com.gip.xyna.xsor.indices.search.IndexSearchCriterion;
import com.gip.xyna.xsor.indices.search.SearchParameter;
import com.gip.xyna.xsor.indices.search.SearchRequest;
import com.gip.xyna.xsor.indices.search.SearchValue;
import com.gip.xyna.persistence.xsor.PreparedClusterQuery;
import com.gip.xyna.persistence.xsor.PreparedClusterQueryCreator;
import com.gip.xyna.persistence.xsor.helper.ProjectTestStorable;
import com.gip.xyna.persistence.xsor.helper.TestXCStorable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class TestSearchRequest {

  static PreparedClusterQueryCreator<TestXCStorable> preparedQueryCreator = new PreparedClusterQueryCreator<TestXCStorable>();
  static Random random = new Random();
  
  @Test
  public void testEqualRequests() throws PersistenceLayerException {
    final String EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                                                             .append(TestXCStorable.COL_NAME_BOOLEAN).append("=? AND ")
                                                             .append(TestXCStorable.COL_NAME_DOUBLE).append("=? AND ")
                                                             .append(TestXCStorable.COL_NAME_FLOAT).append("=? AND ")
                                                             .append(TestXCStorable.COL_NAME_INT).append("=? AND ")
                                                             .append(TestXCStorable.COL_NAME_LONG).append("=? AND ")
                                                             .append(TestXCStorable.COL_NAME_PK).append("=? AND ")
                                                             .append(TestXCStorable.COL_NAME_STRING).append("=?").toString();
    List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions = new ArrayList<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>();
    PreparedClusterQuery<TestXCStorable> query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, EQUALS, TestXCStorable.class, indexDefinitions);
    SearchRequest equalsRequest = query.getSearchRequest();
    
    TestXCStorable randomObject = new TestXCStorable(random);
    randomObject.setStringColumn("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque lobortis ipsum at massa aliquet sit amet vehicula velit pharetra. Morbi ultricies venenatis libero nec malesuada."); // generating from random byte array can produce line breaks and other nasty stuff 
    SearchParameter paramsFromRandomObject = new SearchParameter();
    paramsFromRandomObject.addParameter(randomObject.getBooleanColumn());
    paramsFromRandomObject.addParameter(randomObject.getDoubleColumn());
    paramsFromRandomObject.addParameter(randomObject.getFloatColumn());
    paramsFromRandomObject.addParameter(randomObject.getIntColumn());
    paramsFromRandomObject.addParameter(randomObject.getLongColumn());
    paramsFromRandomObject.addParameter(randomObject.getPrimaryKey());
    paramsFromRandomObject.addParameter(randomObject.getStringColumn());

    Assert.assertTrue(equalsRequest.fits(randomObject, paramsFromRandomObject));
    TestXCStorable totallyDifferentObject = TestXCStorable.generateTotallyDifferentTestXCStorable(randomObject, random);
    totallyDifferentObject.setStringColumn("Fusce sit amet tellus et diam hendrerit vehicula. Vivamus ligula elit, lobortis ac facilisis tincidunt, tempor laoreet arcu. Ut et massa dui.");
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, paramsFromRandomObject));
    
    final String BOOLEAN_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                                                                     .append(TestXCStorable.COL_NAME_BOOLEAN).append("=?").toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, BOOLEAN_EQUALS, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    SearchParameter booleanParams = new SearchParameter();
    booleanParams.addParameter(randomObject.getBooleanColumn());
    Assert.assertTrue(equalsRequest.fits(randomObject, booleanParams));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, booleanParams));
    
    final String BOOLEAN_EQUALS_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                                                                           .append(TestXCStorable.COL_NAME_BOOLEAN).append("=")
                                                                           .append(randomObject.getBooleanColumn()).toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, BOOLEAN_EQUALS_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    Assert.assertTrue(equalsRequest.fits(randomObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    
    final String DOUBLE_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                    .append(TestXCStorable.COL_NAME_DOUBLE).append("=?").toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, DOUBLE_EQUALS, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    SearchParameter doubleParams = new SearchParameter();
    doubleParams.addParameter(randomObject.getDoubleColumn());
    Assert.assertTrue(equalsRequest.fits(randomObject, doubleParams));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, doubleParams));
    
    final String DOUBLE_EQUALS_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_DOUBLE).append("=")
                    .append(randomObject.getDoubleColumn()).toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, DOUBLE_EQUALS_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    Assert.assertTrue(equalsRequest.fits(randomObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    
    final String FLOAT_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                    .append(TestXCStorable.COL_NAME_FLOAT).append("=?").toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, FLOAT_EQUALS, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    SearchParameter floatParams = new SearchParameter();
    floatParams.addParameter(randomObject.getFloatColumn());
    Assert.assertTrue(equalsRequest.fits(randomObject, floatParams));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, floatParams));
    
    final String FLOAT_EQUALS_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_FLOAT).append("=")
                    .append(randomObject.getFloatColumn()).toString();
    query = preparedQueryCreator
                    .prepareQuery(TestXCStorable.TABLENAME, FLOAT_EQUALS_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    Assert.assertTrue(equalsRequest.fits(randomObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    
    final String INT_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                    .append(TestXCStorable.COL_NAME_INT).append("=?").toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, INT_EQUALS, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    SearchParameter intParams = new SearchParameter();
    intParams.addParameter(randomObject.getIntColumn());
    Assert.assertTrue(equalsRequest.fits(randomObject, intParams));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, intParams));
    
    final String INT_EQUALS_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_INT).append("=")
                    .append(randomObject.getIntColumn()).toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, INT_EQUALS_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    Assert.assertTrue(equalsRequest.fits(randomObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    
    final String LONG_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                    .append(TestXCStorable.COL_NAME_LONG).append("=?").toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_EQUALS, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    SearchParameter longParams = new SearchParameter();
    longParams.addParameter(randomObject.getLongColumn());
    Assert.assertTrue(equalsRequest.fits(randomObject, longParams));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, longParams));
    
    final String LONG_EQUALS_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_LONG).append("=")
                    .append(randomObject.getLongColumn()).toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_EQUALS_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    Assert.assertTrue(equalsRequest.fits(randomObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    
    final String BYTEARRAY_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_PK).append("=?").toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, BYTEARRAY_EQUALS, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    SearchParameter byteArrayParams = new SearchParameter();
    byteArrayParams.addParameter(randomObject.getPrimaryKey());
    Assert.assertTrue(equalsRequest.fits(randomObject, byteArrayParams));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, byteArrayParams));
    
    final String BYTEARRAY_EQUALS_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_PK).append("=").append("'")
                    .append(Arrays.toString(randomObject.getPrimaryKey())).append("'").toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, BYTEARRAY_EQUALS_FIXEDREQUEST,
                                              TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    Assert.assertTrue(equalsRequest.fits(randomObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    
    final String STRING_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                    .append(TestXCStorable.COL_NAME_STRING).append("=?").toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, STRING_EQUALS, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    SearchParameter stringParams = new SearchParameter();
    stringParams.addParameter(randomObject.getStringColumn());
    Assert.assertTrue(equalsRequest.fits(randomObject, stringParams));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, stringParams));
    
    final String STRING_EQUALS_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_STRING).append("=")
                    .append("'").append(randomObject.getStringColumn()).append("'").toString();
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, STRING_EQUALS_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    equalsRequest = query.getSearchRequest();
    Assert.assertTrue(equalsRequest.fits(randomObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
    Assert.assertFalse(equalsRequest.fits(totallyDifferentObject, new SearchParameter(query.getPrepreparedParameter().get(0))));
  }
  
  
  @Test
  public void testSimpleOrderedRequests() throws PersistenceLayerException {
    TestXCStorable testXcStorable = new TestXCStorable();
    testXcStorable.setLongColumn(10);
    List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions = new ArrayList<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>();
    
    final String LONG_GREATER = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                    .append(TestXCStorable.COL_NAME_LONG).append(">?").toString();
    
    PreparedClusterQuery<TestXCStorable> query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_GREATER, TestXCStorable.class, indexDefinitions);
    SearchRequest greaterRequest = query.getSearchRequest();
    
    Assert.assertTrue(greaterRequest.fits(testXcStorable, new SearchParameter(new SearchValue(-5l))));
    Assert.assertTrue(greaterRequest.fits(testXcStorable, new SearchParameter(new SearchValue(0l))));
    Assert.assertTrue(greaterRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l))));
    Assert.assertTrue(greaterRequest.fits(testXcStorable, new SearchParameter(new SearchValue(9l))));
    Assert.assertFalse(greaterRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l))));
    Assert.assertFalse(greaterRequest.fits(testXcStorable, new SearchParameter(new SearchValue(11l))));
    Assert.assertFalse(greaterRequest.fits(testXcStorable, new SearchParameter(new SearchValue(15l))));
    
    final String LONG_GREATER_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_LONG).append(">=?").toString();

    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_GREATER_EQUALS, TestXCStorable.class, indexDefinitions);
    SearchRequest greaterEqualsRequest = query.getSearchRequest();
    
    Assert.assertTrue(greaterEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(-5l))));
    Assert.assertTrue(greaterEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(0l))));
    Assert.assertTrue(greaterEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l))));
    Assert.assertTrue(greaterEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(9l))));
    Assert.assertTrue(greaterEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l))));
    Assert.assertFalse(greaterEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(11l))));
    Assert.assertFalse(greaterEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(15l))));
    
    final String LONG_SMALLER = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME).append(" where ")
                    .append(TestXCStorable.COL_NAME_LONG).append("<?").toString();

    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_SMALLER, TestXCStorable.class, indexDefinitions);
    SearchRequest smallerRequest = query.getSearchRequest();
    
    Assert.assertFalse(smallerRequest.fits(testXcStorable, new SearchParameter(new SearchValue(-5l))));
    Assert.assertFalse(smallerRequest.fits(testXcStorable, new SearchParameter(new SearchValue(0l))));
    Assert.assertFalse(smallerRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l))));
    Assert.assertFalse(smallerRequest.fits(testXcStorable, new SearchParameter(new SearchValue(9l))));
    Assert.assertFalse(smallerRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l))));
    Assert.assertTrue(smallerRequest.fits(testXcStorable, new SearchParameter(new SearchValue(11l))));
    Assert.assertTrue(smallerRequest.fits(testXcStorable, new SearchParameter(new SearchValue(15l))));
    
    final String LONG_SMALLER_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_LONG).append("<=?").toString();

    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_SMALLER_EQUALS, TestXCStorable.class, indexDefinitions);
    SearchRequest smallerEqualsRequest = query.getSearchRequest();

    Assert.assertFalse(smallerEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(-5l))));
    Assert.assertFalse(smallerEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(0l))));
    Assert.assertFalse(smallerEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l))));
    Assert.assertFalse(smallerEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(9l))));
    Assert.assertTrue(smallerEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l))));
    Assert.assertTrue(smallerEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(11l))));
    Assert.assertTrue(smallerEqualsRequest.fits(testXcStorable, new SearchParameter(new SearchValue(15l))));
    
  }
  
  
  @Test
  public void testValueRangeRequests() throws PersistenceLayerException {
    TestXCStorable testXcStorable = new TestXCStorable();
    testXcStorable.setLongColumn(10);
    
    final String LONG_GREATER_AND_SMALLER = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_LONG).append(">? AND ")
                    .append(TestXCStorable.COL_NAME_LONG).append("<?").toString();
    
    List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions = new ArrayList<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>();
    PreparedClusterQuery<TestXCStorable> query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_GREATER_AND_SMALLER, TestXCStorable.class, indexDefinitions);
    SearchRequest searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(0l),
                                                                             new SearchValue(20l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l),
                                                                             new SearchValue(15l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l),
                                                                              new SearchValue(15l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l),
                                                                              new SearchValue(10l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l),
                                                                              new SearchValue(10l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(15l),
                                                                              new SearchValue(5l))));
    
    final String LONG_GREATER_EQUALS_AND_SMALLER = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_LONG).append(">=? AND ")
                    .append(TestXCStorable.COL_NAME_LONG).append("<?").toString();

    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_GREATER_EQUALS_AND_SMALLER, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(0l),
                                                                             new SearchValue(20l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l),
                                                                             new SearchValue(15l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l),
                                                                              new SearchValue(15l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l),
                                                                              new SearchValue(10l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l),
                                                                              new SearchValue(10l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(15l),
                                                                              new SearchValue(5l))));
    
    final String LONG_GREATER_AND_SMALLER_EQUALS = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_LONG).append(">? AND ")
                    .append(TestXCStorable.COL_NAME_LONG).append("<=?").toString();

    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_GREATER_AND_SMALLER_EQUALS, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(0l),
                                                                             new SearchValue(20l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l),
                                                                             new SearchValue(15l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l),
                                                                              new SearchValue(15l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l),
                                                                              new SearchValue(10l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l),
                                                                              new SearchValue(10l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(15l),
                                                                              new SearchValue(5l))));
    
    final String LONG_GREATER_EQUALS_AND_SMALLER_EQUALS = new StringBuilder("select * from ")
                    .append(TestXCStorable.TABLENAME).append(" where ").append(TestXCStorable.COL_NAME_LONG)
                    .append(">=? AND ").append(TestXCStorable.COL_NAME_LONG).append("<=?").toString();

    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_GREATER_EQUALS_AND_SMALLER_EQUALS, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();

    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(0l),
                                                                             new SearchValue(20l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l),
                                                                             new SearchValue(15l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l),
                                                                              new SearchValue(15l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(5l),
                                                                              new SearchValue(10l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(10l),
                                                                              new SearchValue(10l))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(15l),
                                                                              new SearchValue(5l))));
  }
  
  
  
  @Test
  public void testInRequests() throws PersistenceLayerException {
    TestXCStorable testXcStorable = new TestXCStorable();
    testXcStorable.setStringColumn("Land");
    List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions = new ArrayList<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>();
    
    final String STRING_IN = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_STRING).append(" in ?").toString();
    
    PreparedClusterQuery<TestXCStorable> query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, STRING_IN, TestXCStorable.class, indexDefinitions);
    SearchRequest searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new String[] {"Stadt","Land","Fluss"}))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new String[] {"Wald","Baum"}))));
    
    final String STRING_IN_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_STRING).append(" in (Stadt,Land,Fluss)").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, STRING_IN_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    SearchParameter params = new SearchParameter();
    params.addParameter(query.getPrepreparedParameter().get(0));
    Assert.assertTrue(searchRequest.fits(testXcStorable, params));
    testXcStorable.setStringColumn("Baum");
    Assert.assertFalse(searchRequest.fits(testXcStorable, params));
    
    
    testXcStorable.setBooleanColumn(true);
    
    final String BOOLEAN_IN = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_BOOLEAN).append(" in ?").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, BOOLEAN_IN, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new boolean[] {true}))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new boolean[] {false}))));
    
    final String BOOLEAN_IN_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_BOOLEAN).append(" in (true)").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, BOOLEAN_IN_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    params = new SearchParameter();
    params.addParameter(query.getPrepreparedParameter().get(0));
    Assert.assertTrue(searchRequest.fits(testXcStorable, params));
    testXcStorable.setBooleanColumn(false);
    Assert.assertFalse(searchRequest.fits(testXcStorable, params));
    
    
    testXcStorable.setIntColumn(10);
    
    final String INT_IN = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_INT).append(" in ?").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, INT_IN, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new int[] {1,2,3,4,5,6,7,8,9,10}))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new int[] {1,3,5,7,9,11,13}))));
    
    final String INT_IN_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_INT).append(" in (9,10,11)").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, INT_IN_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    params = new SearchParameter();
    params.addParameter(query.getPrepreparedParameter().get(0));
    Assert.assertTrue(searchRequest.fits(testXcStorable, params));
    testXcStorable.setIntColumn(8);
    Assert.assertFalse(searchRequest.fits(testXcStorable, params));
    
    
    testXcStorable.setLongColumn(10);
    
    final String LONG_IN = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_LONG).append(" in ?").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_IN, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new long[] {1l,2l,3l,4l,5l,6l,7l,8l,9l,10l}))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new long[] {1l,3l,5l,7l,9l,11l,13l}))));
    
    final String LONG_IN_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_LONG).append(" in (9,10,11)").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_IN_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    params = new SearchParameter();
    params.addParameter(query.getPrepreparedParameter().get(0));
    Assert.assertTrue(searchRequest.fits(testXcStorable, params));
    testXcStorable.setLongColumn(8);
    Assert.assertFalse(searchRequest.fits(testXcStorable, params));
    
    
    testXcStorable.setDoubleColumn(12.345);
    
    final String DOUBLE_IN = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_DOUBLE).append(" in ?").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, DOUBLE_IN, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new double[] {1.23, 12.345, 123.4567}))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new double[] {12.343, 12.346}))));
    
    final String DOUBLE_IN_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_DOUBLE).append(" in (1.23, 12.345, 123.4567)").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, DOUBLE_IN_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    params = new SearchParameter();
    params.addParameter(query.getPrepreparedParameter().get(0));
    Assert.assertTrue(searchRequest.fits(testXcStorable, params));
    testXcStorable.setDoubleColumn(0.123);
    Assert.assertFalse(searchRequest.fits(testXcStorable, params));
    
    
    
    testXcStorable.setFloatColumn(12.345f);
    
    final String FLOAT_IN = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_FLOAT).append(" in ?").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, FLOAT_IN, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new float[] {1.23f, 12.345f, 123.4567f}))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new float[] {12.343f, 12.346f}))));
    
    final String FLOAT_IN_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_FLOAT).append(" in (1.23, 12.345, 123.4567)").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, FLOAT_IN_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    params = new SearchParameter();
    params.addParameter(query.getPrepreparedParameter().get(0));
    Assert.assertTrue(searchRequest.fits(testXcStorable, params));
    testXcStorable.setFloatColumn(0.123f);
    Assert.assertFalse(searchRequest.fits(testXcStorable, params));
    
    byte[] pk = new byte[16];
    random.nextBytes(pk);
    testXcStorable.setPrimaryKey(pk);
    
    final String PK_IN = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_PK).append(" in ?").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, PK_IN, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    byte[] notPk = new byte[16];
    random.nextBytes(notPk);
    while (Arrays.equals(pk, notPk)) {
      random.nextBytes(notPk);
    }
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new byte[][] {notPk, pk}))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue(new byte[][] {notPk}))));
    
    final String PK_IN_FIXEDREQUEST = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_PK).append(" in (")
                    .append("'").append(Arrays.toString(pk)).append("')").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, PK_IN_FIXEDREQUEST, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    params = new SearchParameter();
    params.addParameter(query.getPrepreparedParameter().get(0));
    Assert.assertTrue(searchRequest.fits(testXcStorable, params));
    testXcStorable.setPrimaryKey(notPk);
    Assert.assertFalse(searchRequest.fits(testXcStorable, params));
  }
  
  
  @Test
  public void testLikeRequest() throws PersistenceLayerException {
    TestXCStorable testXcStorable = new TestXCStorable();
    testXcStorable.setStringColumn("ApfelBrotBaum");
    List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions = new ArrayList<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>();
    
    final String STRING_LIKE = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_STRING).append(" like ?").toString();
    
    PreparedClusterQuery<TestXCStorable> query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, STRING_LIKE, TestXCStorable.class, indexDefinitions);
    SearchRequest searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%Baum"))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("Apfel%"))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%Brot%"))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%"))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%Apfel"))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("Baum%"))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%x%"))));
    
    
    testXcStorable.setLongColumn(123456789);
    
    final String LONG_LIKE = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_LONG).append(" like ?").toString();
    
    query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, LONG_LIKE, TestXCStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%"))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("1%"))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%9"))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%456%"))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%1"))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("9%"))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%21%"))));
    Assert.assertFalse(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%0%"))));
  }
  
  
  //DisjunctiveNormalForm
  /*
   * { { X AND Y AND Z } OR
   *   { X AND Y AND Z } OR
   *   { X AND Y AND Z } }
   */
  // the way the validation works there will be no OR's, the only way we would have implicit OR's is through IN's
  // IN's will be prepared as IN ? (if they are not already in that form [indices have to treat that appropriately down the line of an actual search])
  
  // WHERE (a = ?1 OR a = ?2 OR a =?3) AND (b = ?4 OR c = ?5)
  // ==> { { a = ?1 AND b = ?4 } OR
  //       { a = ?1 AND c = ?5 } OR
  //       { a = ?2 AND b = ?4 } OR
  //       { a = ?2 AND c = ?5 } OR
  //       { a = ?3 AND b = ?4 } OR
  //       { a = ?3 AND c = ?5 } }
  // This normalization will be rather complicated once the conditions get deep, how would an appropriate algorithm look
  // WHERE (a = ?1 OR (b = ?2 AND (c = ?3 OR a = ?4)))
  // ==> { {a = ?1} OR 
  //       {b = ?2 AND c = ?3} OR
  //       {b = ?2 AND a = ?4} }
  // find inner OR's and multiply them with ANDs
  // repeat recursively
  // WHERE (a = ?1 OR (b = ?2 AND d = ?5 AND (c = ?3 OR a = ?4)))
  // ==> { {a = ?1} OR 
  //       {b = ?2 AND d = ?5 AND c = ?3} OR
  //       {b = ?2 AND d = ?5 AND a = ?4} }
  @Test
  public void testDisjunctiveNormalFormConversion() throws PersistenceLayerException {
    TestXCStorable testXcStorable = new TestXCStorable();
    testXcStorable.setStringColumn("ApfelBrotBaum");
    testXcStorable.setLongColumn(10);

    final String STRING_LIKE_OR_LONG_GREATER = new StringBuilder("select * from ").append(TestXCStorable.TABLENAME)
                    .append(" where ").append(TestXCStorable.COL_NAME_STRING).append(" like ? OR ")
                    .append(TestXCStorable.COL_NAME_LONG).append(" > ?").toString();
    
    List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions = new ArrayList<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>();
    PreparedClusterQuery<TestXCStorable> query = preparedQueryCreator.prepareQuery(TestXCStorable.TABLENAME, STRING_LIKE_OR_LONG_GREATER, TestXCStorable.class, indexDefinitions);
    SearchRequest searchRequest = query.getSearchRequest();
    
    Assert.assertEquals(2, searchRequest.getSearchCriterion().size());
    
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%Brot%"),
                                                                             new SearchValue(20l))));
    Assert.assertTrue(searchRequest.fits(testXcStorable, new SearchParameter(new SearchValue("%Land%"),
                                                                             new SearchValue(5l))));
    
    testXcStorable.setStringColumn("ApfelBrotBaum");
    testXcStorable.setLongColumn(0);
    boolean evaluationOfFirstCriterionWithMatchingStringSuccessfull = searchRequest.getSearchCriterion().get(0).fits(testXcStorable,
                          new SearchParameter(new SearchValue("%Brot%"),
                                              new SearchValue(20l)));
    Assert.assertEquals(!(evaluationOfFirstCriterionWithMatchingStringSuccessfull), searchRequest.getSearchCriterion().get(1).fits(testXcStorable,
                                                                                                                                   new SearchParameter(new SearchValue("%Brot%"),
                                                                                                                                                       new SearchValue(20l))));
    testXcStorable.setStringColumn("");
    testXcStorable.setLongColumn(10);
    boolean evaluationOfFirstCriterionWithMatchingLongSuccessfull = searchRequest.getSearchCriterion().get(0).fits(testXcStorable,
                          new SearchParameter(new SearchValue("%Land%"),
                                              new SearchValue(5l)));
    Assert.assertEquals(!(evaluationOfFirstCriterionWithMatchingLongSuccessfull),
                        searchRequest.getSearchCriterion().get(1).fits(testXcStorable,
                                              new SearchParameter(new SearchValue("%Land%"),
                                                                  new SearchValue(5l))));
    Assert.assertNotSame(evaluationOfFirstCriterionWithMatchingStringSuccessfull, evaluationOfFirstCriterionWithMatchingLongSuccessfull);
  }
  
  
  @Test
  public void testCommonJunoSearchRequests() throws PersistenceLayerException {
    List<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>> indexDefinitions = new ArrayList<IndexDefinition<?, ? extends IndexKey, ? extends IndexSearchCriterion>>();
    
    String sqlStatement = "Select * from " + ProjectTestStorable.TABLENAME + " where "+ProjectTestStorable.COL_SUPERPOOLID + " = ? and " + ProjectTestStorable.COL_EXPIRATIONTIME
    + " < ? and " + ProjectTestStorable.COL_RESERVATIONTIME + " < ? and " + ProjectTestStorable.COL_BINDING + " in (?) for update";
    
    PreparedClusterQuery<TestXCStorable> query = preparedQueryCreator.prepareQuery(ProjectTestStorable.TABLENAME, sqlStatement, ProjectTestStorable.class, indexDefinitions);
    SearchRequest searchRequest = query.getSearchRequest();
    
    final long SUPERPOOLID_PARAM = 1;
    final long MISS_SUPERPOOLID = 2;
    final String[] BINDING_PARAM = new String[] {"1","2"};
    final String BINDING1 = "1";
    final String BINDING2 = "2";
    final String MISS_BINDING = "3";
    final long EXPIRATIONTIME_PARAM = System.currentTimeMillis();
    final long HIGHER_EXPIRATIONTIME = EXPIRATIONTIME_PARAM + 1;
    final long LOWER_EXPIRATIONTIME = EXPIRATIONTIME_PARAM -1;
    final long RESERVATIONTIME_PARAM = System.currentTimeMillis();
    final long HIGHER_RESERVATIONTIME = RESERVATIONTIME_PARAM + 1;
    final long LOWER_RESERVATIONTIME = RESERVATIONTIME_PARAM -1;
    SearchParameter params = new SearchParameter(new SearchValue(SUPERPOOLID_PARAM), new SearchValue(EXPIRATIONTIME_PARAM), new SearchValue(RESERVATIONTIME_PARAM), new SearchValue(BINDING_PARAM));

    ProjectTestStorable hitBinding1 = new ProjectTestStorable(BINDING1, SUPERPOOLID_PARAM, LOWER_EXPIRATIONTIME, LOWER_RESERVATIONTIME);
    Assert.assertTrue(searchRequest.fits(hitBinding1, params));
    ProjectTestStorable hitBinding2 = new ProjectTestStorable(BINDING2, SUPERPOOLID_PARAM, LOWER_EXPIRATIONTIME, LOWER_RESERVATIONTIME);
    Assert.assertTrue(searchRequest.fits(hitBinding2, params));
    ProjectTestStorable missBinding = new ProjectTestStorable(MISS_BINDING, SUPERPOOLID_PARAM, LOWER_EXPIRATIONTIME, LOWER_RESERVATIONTIME);
    Assert.assertFalse(searchRequest.fits(missBinding, params));
    ProjectTestStorable missSuperPoolId = new ProjectTestStorable(BINDING1, MISS_SUPERPOOLID, LOWER_EXPIRATIONTIME, LOWER_RESERVATIONTIME);
    Assert.assertFalse(searchRequest.fits(missSuperPoolId, params));
    ProjectTestStorable missHighReservationTime = new ProjectTestStorable(BINDING1, SUPERPOOLID_PARAM, LOWER_EXPIRATIONTIME, HIGHER_RESERVATIONTIME);
    Assert.assertFalse(searchRequest.fits(missHighReservationTime, params));
    ProjectTestStorable missHighExpirationTime = new ProjectTestStorable(BINDING1, SUPERPOOLID_PARAM, HIGHER_EXPIRATIONTIME, LOWER_RESERVATIONTIME);
    Assert.assertFalse(searchRequest.fits(missHighExpirationTime, params));
    
    
    final long EXPIRATIONTIME_ZERO_PARAM = 0;
    final long RESERVATIONTIME_ZERO_PARAM = 0;
    
    sqlStatement = "Select * from "+ProjectTestStorable.TABLENAME+" where "+ProjectTestStorable.COL_SUPERPOOLID+" = ? and ("+ProjectTestStorable.COL_EXPIRATIONTIME +
    " < ? and "+ProjectTestStorable.COL_EXPIRATIONTIME+" > ?) and ("+ProjectTestStorable.COL_RESERVATIONTIME+" < ? and "+ProjectTestStorable.COL_RESERVATIONTIME+" > ?)"+
    " and " + ProjectTestStorable.COL_BINDING + " in (?) for update";
    
    query = preparedQueryCreator.prepareQuery(ProjectTestStorable.TABLENAME, sqlStatement, ProjectTestStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    params = new SearchParameter(new SearchValue(SUPERPOOLID_PARAM), new SearchValue(EXPIRATIONTIME_PARAM), new SearchValue(EXPIRATIONTIME_ZERO_PARAM),
                                 new SearchValue(RESERVATIONTIME_PARAM), new SearchValue(RESERVATIONTIME_ZERO_PARAM), new SearchValue(BINDING_PARAM));
    
    ProjectTestStorable hit = new ProjectTestStorable(BINDING1, SUPERPOOLID_PARAM, LOWER_EXPIRATIONTIME, LOWER_RESERVATIONTIME);
    Assert.assertTrue(searchRequest.fits(hit, params));
    Assert.assertFalse(searchRequest.fits(missHighExpirationTime, params));
    Assert.assertFalse(searchRequest.fits(missHighReservationTime, params));
    ProjectTestStorable missZeroExpirationTime = new ProjectTestStorable(BINDING1, SUPERPOOLID_PARAM, EXPIRATIONTIME_ZERO_PARAM, LOWER_RESERVATIONTIME);
    Assert.assertFalse(searchRequest.fits(missZeroExpirationTime, params));
    ProjectTestStorable missZeroReservationTime = new ProjectTestStorable(BINDING1, SUPERPOOLID_PARAM, LOWER_EXPIRATIONTIME, RESERVATIONTIME_ZERO_PARAM);
    Assert.assertFalse(searchRequest.fits(missZeroReservationTime, params));
    
    
    sqlStatement = "Select * from "+ProjectTestStorable.TABLENAME+" where "+ProjectTestStorable.COL_MAC+" = ? and "
    +ProjectTestStorable.COL_IAID+" = ? and ("+ProjectTestStorable.COL_EXPIRATIONTIME+" > ? or "+ProjectTestStorable.COL_EXPIRATIONTIME+" = ?) and "
    +ProjectTestStorable.COL_SUPERPOOLID+" = ? limit 1 for update";
    
    query = preparedQueryCreator.prepareQuery(ProjectTestStorable.TABLENAME, sqlStatement, ProjectTestStorable.class, indexDefinitions);
    searchRequest = query.getSearchRequest();
    
    final String MAC_PARAM = "aaaabbbbcccc";
    final String MISS_MAC = "aaaabbbbcccd";
    final String IAID_PARAM = "0x1234";
    final String MISS_IAID = "0x1235";
    
    params = new SearchParameter(new SearchValue(MAC_PARAM), new SearchValue(IAID_PARAM), new SearchValue(EXPIRATIONTIME_PARAM),
                                 new SearchValue(EXPIRATIONTIME_ZERO_PARAM), new SearchValue(SUPERPOOLID_PARAM));
    ProjectTestStorable hitHigExpirationTime = new ProjectTestStorable(MAC_PARAM, IAID_PARAM, HIGHER_EXPIRATIONTIME, SUPERPOOLID_PARAM);
    Assert.assertTrue(searchRequest.fits(hitHigExpirationTime, params));
    ProjectTestStorable hitExpirationTimeZero = new ProjectTestStorable(MAC_PARAM, IAID_PARAM, EXPIRATIONTIME_ZERO_PARAM, SUPERPOOLID_PARAM);
    Assert.assertTrue(searchRequest.fits(hitExpirationTimeZero, params));
    ProjectTestStorable missLowerExpirationTime = new ProjectTestStorable(MAC_PARAM, IAID_PARAM, LOWER_EXPIRATIONTIME, SUPERPOOLID_PARAM);
    Assert.assertFalse(searchRequest.fits(missLowerExpirationTime, params));
    ProjectTestStorable missMac = new ProjectTestStorable(MISS_MAC, IAID_PARAM, HIGHER_EXPIRATIONTIME, SUPERPOOLID_PARAM);
    Assert.assertFalse(searchRequest.fits(missMac, params));
    ProjectTestStorable missIaid = new ProjectTestStorable(MAC_PARAM, MISS_IAID, HIGHER_EXPIRATIONTIME, SUPERPOOLID_PARAM);
    Assert.assertFalse(searchRequest.fits(missIaid, params));
  }
  
}
