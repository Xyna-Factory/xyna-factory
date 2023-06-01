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

package com.gip.xyna.xnwh.persistence.memory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.Condition;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ParsedQuery;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.PreparedQueryBuildException;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.PreparedQueryParsingException;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;


public class PreparedQueryCreatorTest extends TestCase {


  @SuppressWarnings("static-access")
  public void testEscapeSQLLikeClause() throws PersistenceLayerException {

    String s = "";

    s = SelectionParser.escapeParams("abc", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Qabc\\E", s);

    s = SelectionParser.escapeParams("abc%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Qabc\\E.*", s);

    s = SelectionParser.escapeParams("%abc%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals(".*\\Qabc\\E.*", s);

    s = SelectionParser.escapeParams("%abc%efg", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals(".*\\Qabc\\E.*\\Qefg\\E", s);

    s = SelectionParser.escapeParams("abc%efg%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Qabc\\E.*\\Qefg\\E.*", s);

    s = SelectionParser.escapeParams("abc\\%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Qabc%\\E", s);

    s = SelectionParser.escapeParams("\\%abc\\%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Q%abc%\\E", s);

    s = SelectionParser.escapeParams("\\%abc\\%efg\\%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Q%abc%efg%\\E", s);

    s = SelectionParser.escapeParams("\\%abc\\\\%efg\\%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Q%abc\\\\E.*\\Qefg%\\E", s);

    s = SelectionParser.escapeParams("\\%abc\\\\\\%efg\\%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Q%abc\\%efg%\\E", s);

    s = SelectionParser.escapeParams("\\\\%abc%efg\\%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Q\\\\E.*\\Qabc\\E.*\\Qefg%\\E", s);

    s = SelectionParser.escapeParams("\\\\\\%abc%efg\\%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals("\\Q\\%abc\\E.*\\Qefg%\\E", s);

    s = SelectionParser.escapeParams("%abc%efg\\\\%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals(".*\\Qabc\\E.*\\Qefg\\\\E.*", s);

    s = SelectionParser.escapeParams("%abc%efg\\\\\\%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals(".*\\Qabc\\E.*\\Qefg\\%\\E", s);

    s = SelectionParser.escapeParams("%%", true, new PreparedQueryForMemory.EscapeForMemory());
    assertEquals(".*.*", s);

  }



  public void test_2_06_WhereClauseParsing() throws PersistenceLayerException, PreparedQueryBuildException, PreparedQueryParsingException {

    PreparedQueryCreator pqc = new PreparedQueryCreator(0);
    ParsedQuery pq = pqc.parse("select * from bla where id>? and trivialId<?");
    CodeBuffer cb = pqc.generateJava(pq, "", "testStorable", new TestTableObject<TestStorable>(TestStorable.class));

    String javaResult = cb.toString();
    System.out.println(javaResult);
    assertTrue(javaResult.contains("private boolean conditionBoolean0(Storable s, Parameter p) {"));
    assertTrue(javaResult
        .contains("return ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getId() != null && ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getId()  >  (java.lang.Long) p.get(0);"));
    assertTrue(javaResult.contains("private boolean conditionBoolean1(Storable s, Parameter p) {"));
    assertTrue(javaResult
        .contains("return ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getTrivialId()  <  ((Number) p.get(1)).longValue();"));

  }


  public void test_2_07_WhereClauseParsing() throws PersistenceLayerException, PreparedQueryBuildException, PreparedQueryParsingException {

    PreparedQueryCreator pqc = new PreparedQueryCreator(0);
    ParsedQuery pq = pqc.parse("select * from bla where id is null and trivialId<?");
    CodeBuffer cb = pqc.generateJava(pq, "", "testStorable", new TestTableObject<TestStorable>(TestStorable.class));

    String javaResult = cb.toString();
    assertTrue(javaResult.contains("private boolean conditionBoolean0(Storable s, Parameter p) {"));
    assertTrue(javaResult
        .contains("return ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getId() == null ;"));
    assertTrue(javaResult.contains("private boolean conditionBoolean1(Storable s, Parameter p) {"));
    assertTrue(javaResult
        .contains("return ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getTrivialId()  <  ((Number) p.get(0)).longValue();"));

  }


  public void test_2_08_WhereClauseParsing() throws PersistenceLayerException, PreparedQueryBuildException, PreparedQueryParsingException {

    PreparedQueryCreator pqc = new PreparedQueryCreator(0);
    ParsedQuery pq = pqc.parse("select * from bla where id  is  not  null and trivialId<?");
    CodeBuffer cb = pqc.generateJava(pq, "", "testStorable", new TestTableObject<TestStorable>(TestStorable.class));

    String javaResult = cb.toString();
    assertTrue(javaResult.contains("private boolean conditionBoolean0(Storable s, Parameter p) {"));
    assertTrue(javaResult
        .contains("return ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getId() != null ;"));
    assertTrue(javaResult.contains("private boolean conditionBoolean1(Storable s, Parameter p) {"));
    assertTrue(javaResult
        .contains("return ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getTrivialId()  <  ((Number) p.get(0)).longValue();"));

  }


  public void test_2_09_WhereClauseParsing() throws PersistenceLayerException, PreparedQueryBuildException, PreparedQueryParsingException {

    PreparedQueryCreator pqc = new PreparedQueryCreator(0);
    ParsedQuery pq = pqc.parse("select * from bla where id like ? and trivialId<?");
    CodeBuffer cb = pqc.generateJava(pq, "", "testStorable", new TestTableObject<TestStorable>(TestStorable.class));

    String javaResult = cb.toString();
    assertTrue(javaResult.contains("private boolean conditionBoolean0(Storable s, Parameter p) {"));
    assertTrue(javaResult
        .contains("p.getParameterAsPattern(0).matcher( ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getId() + \"\" ).matches()"));
    assertTrue(javaResult.contains("private boolean conditionBoolean1(Storable s, Parameter p) {"));
    assertTrue(javaResult
        .contains("return ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getTrivialId()  <  ((Number) p.get(1)).longValue();"));

  }

  public void test_2_10_WhereClauseParsing() throws PersistenceLayerException, PreparedQueryBuildException, PreparedQueryParsingException {

    PreparedQueryCreator pqc = new PreparedQueryCreator(0);
    ParsedQuery pq = pqc.parse("select * from bla where id like 'blabla'");
    CodeBuffer cb = pqc.generateJava(pq, "", "testStorable", new TestTableObject<TestStorable>(TestStorable.class));

    String javaResult = cb.toString();
    assertTrue(javaResult.contains("private boolean conditionBoolean0(Storable s, Parameter p) {"));
    assertTrue(javaResult
        .contains("Pattern.compile(\"\\\\Qblabla\\\\E\").matcher( ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getId() + \"\" ).matches()"));

  }


  public void test_2_11_WhereClauseParsing() throws PersistenceLayerException, PreparedQueryBuildException, PreparedQueryParsingException {

    PreparedQueryCreator pqc = new PreparedQueryCreator(0);
    ParsedQuery pq = pqc.parse("select count(*) from bla where id like 'blabla' order by id");
    CodeBuffer cb =
        pqc.generateJava(pq, "", "testStorable", new TestTableObject<TestStorable>(TestStorable.class));

    String javaResult = cb.toString();
    assertTrue(javaResult.contains("private boolean conditionBoolean0(Storable s, Parameter p) {"));
    assertTrue(javaResult.contains("Pattern.compile(\"\\\\Qblabla\\\\E\").matcher("
        + " ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getId() + \"\" ).matches()"));
    assertTrue(javaResult.contains("public Comparator getComparator() {"));

  }


  public void test_2_12_orderByAsc() throws PersistenceLayerException, SecurityException, NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException,
      UnderlyingDataNotFoundException, PreparedQueryBuildException, PreparedQueryParsingException {

    final String sqlString = "select count(*) from bla where id like 'blabla' order by id asc";

    PreparedQueryCreator pqc = new PreparedQueryCreator(0);
    ParsedQuery pq = pqc.parse(sqlString);
    CodeBuffer cb = pqc.generateJava(pq, "com.gip.xyna.testpackage", "testStorable", new TestTableObject<TestStorable>(TestStorable.class));

    String javaResult = cb.toString();
    assertTrue(javaResult.contains("private boolean conditionBoolean0(Storable s, Parameter p) {"));
    assertTrue(javaResult.contains("Pattern.compile(\"\\\\Qblabla\\\\E\").matcher("
        + " ((com.gip.xyna.xnwh.persistence.memory.TestStorable)s).getId() + \"\" ).matches()"));
    assertTrue(javaResult.contains("public Comparator getComparator() {"));

    Query<TestStorable> query = new Query<TestStorable>(sqlString, TestStorable.reader);
    Class<IPreparedQueryForMemory<TestStorable>> c =
        pqc.createClass(query, new TestTableObject<TestStorable>(TestStorable.class));
    Method getComparatorMethod = c.getMethod("getComparator");

    Comparator<TestStorable> createdComparator =
        (Comparator<TestStorable>) getComparatorMethod.invoke(c.getConstructors()[0].newInstance(query, null), null);

    List<TestStorable> listToBeSorted = new ArrayList<TestStorable>();

    for (int i = 0; i < 40; i++) {
      TestStorable data = new TestStorable(new Random().nextLong());
      listToBeSorted.add(data);
    }

    Collections.sort(listToBeSorted, createdComparator);

    long lastMaxValue = Long.MIN_VALUE;
    for (int i=0; i<listToBeSorted.size(); i++) {
      long nextValue = listToBeSorted.get(i).getId();
      if (nextValue < lastMaxValue) {
        fail();
      }
      lastMaxValue = nextValue;
    }

  }

}
