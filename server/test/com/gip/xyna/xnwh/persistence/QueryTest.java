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

import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;


public class QueryTest extends TestCase {

  private static class A {
    
  }
  
  private static ResultSetReader<A> reader = new ResultSetReader<A>() {

    public A read(ResultSet rs) throws SQLException {
      return null;
    }
    
  };
  
  public void testSimpleQuery() throws PersistenceLayerException {
    String sqlString = "select bla from mytable";
    Query<A> q = new Query<A>(sqlString, reader);
    assertEquals("mytable", q.getTable());
    assertEquals(sqlString, q.getSqlString());
    assertEquals(reader, q.getReader());
  }
  
  public void testQueryWithOrderBy() throws PersistenceLayerException {
    String sqlString = "select bla from mytable order by bla";
    Query<A> q = new Query<A>(sqlString, reader);
    assertEquals("mytable", q.getTable());
    assertEquals(sqlString, q.getSqlString());
    assertEquals(reader, q.getReader());    
  }
  
  public void testQueryWithWhere() throws PersistenceLayerException {
    String sqlString = "select bla from mytable where bla = 1";
    Query<A> q = new Query<A>(sqlString, reader);
    assertEquals("mytable", q.getTable());
    assertEquals(sqlString, q.getSqlString());
    assertEquals(reader, q.getReader()); 
  }
  
  public void testQueryWithWhereAndOrderBy() throws PersistenceLayerException {
    String sqlString = "select bla from mytable where bla = 1 order by bla";
    Query<A> q = new Query<A>(sqlString, reader);
    assertEquals("mytable", q.getTable());
    assertEquals(sqlString, q.getSqlString());
    assertEquals(reader, q.getReader()); 
  }
}
