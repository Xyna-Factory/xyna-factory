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
package com.gip.xyna.xnwh.selection;

import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;


public class WhereClauseNumber<P extends WhereClausesContainerBase<P>>  extends WhereClause<P> {


  private static final long serialVersionUID = 2807077617036942111L;

  private Object value;
  private Operator operator;
  
  private static enum Operator {
    EQUALS("="), BIGGERTHAN(">"), SMALLERTHAN("<"), LIKE("LIKE");
    
    private String sql;
    private Operator(String sql) {
      this.sql = sql;
    }
    public String getSQL() {
      return sql;
    }
  }

  public WhereClauseNumber(P parent, String col) {
    super(parent, col);
  }

  @Override
  public WhereClauseNumber<P> copy(String col) {
    WhereClauseNumber<P> copy = new WhereClauseNumber<P>(getParent(), col);
    if (value instanceof Number || value instanceof String) {
      copy.value = value;
    } else {
      throw new IllegalArgumentException("value must be a Number or String, but found: " + value.getClass().getName());
    }
    
    copy.operator = operator;
    
    return copy;
  }

  public WhereClausesConnection<P> isEqual(long value) throws XNWH_WhereClauseBuildException {
    checkLocked("isEqual");
    return createWhereClausesConnection(value, Operator.EQUALS);
  }


  public WhereClausesConnection<P> isBiggerThan(long value) throws XNWH_WhereClauseBuildException {
    checkLocked("isBiggerThan");
    return createWhereClausesConnection(value, Operator.BIGGERTHAN);
  }


  public WhereClausesConnection<P> isSmallerThan(long value) throws XNWH_WhereClauseBuildException {
    checkLocked("isSmallerThan");
    return createWhereClausesConnection(value, Operator.SMALLERTHAN);
  }


  public WhereClausesConnection<P> isLike(String value) throws XNWH_WhereClauseBuildException {
    checkLocked("isLike");
    return createWhereClausesConnection(value, Operator.LIKE);
  }


  private WhereClausesConnection<P> createWhereClausesConnection(Object value, Operator operator) {
    this.value = value;
    this.operator = operator;
    WhereClausesConnection<P> wcc = new WhereClausesConnection<P>(this);
    getParent().addWhereClause(wcc);
    return wcc;
  }


  @Override
  public String asString() {
    return getColumn() + " " + operator.getSQL() + " ?";
  }


  @Override
  public void addParameter(List<Object> list) {
    list.add(value);
  }

}
