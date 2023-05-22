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

package com.gip.xyna.xnwh.selection;



import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;



public class WhereClauseString<P extends WhereClausesContainerBase<P>> extends WhereClause<P> {

  private static final long serialVersionUID = -397504443992552763L;
  private String value;
  private String[] inValues;
  private Operator operator;


  public static enum Operator {
    EQUALS("="), BIGGERTHAN(">"), SMALLERTHAN("<"), LIKE("LIKE"), ISNULL("IS NULL"), IN("IN");

    private String sql;


    private Operator(String sql) {
      this.sql = sql;
    }


    public String getSQL() {
      return sql;
    }
  }


  public WhereClauseString(P parent, String col) {
    super(parent, col);
  }

  @Override
  public WhereClauseString<P> copy(String col) {
    WhereClauseString<P> copy = new WhereClauseString<P>(getParent(), col);
    copy.value = value;
    copy.operator = operator;
    return copy;
  }
  
  public WhereClausesConnection<P> isEqual(String value) throws XNWH_WhereClauseBuildException {
    checkLocked("isEqual");
    return createWhereClausesConnection(value, Operator.EQUALS);
  }


  public WhereClausesConnection<P> isLike(String value) throws XNWH_WhereClauseBuildException {
    checkLocked("isLike");
    return createWhereClausesConnection(value, Operator.LIKE);
  }

  public WhereClausesConnection<P> isIn(String... values) throws XNWH_WhereClauseBuildException {
    checkLocked("isIn");
    this.inValues = values;
    this.operator = Operator.IN;
    WhereClausesConnection<P> wcc = new WhereClausesConnection<P>(this);
    getParent().addWhereClause(wcc);
    return wcc;
  }

  public WhereClausesConnection<P> isBiggerThan(String value) throws XNWH_WhereClauseBuildException {
    checkLocked("isBiggerThan");
    return createWhereClausesConnection(value, Operator.BIGGERTHAN);
  }


  public WhereClausesConnection<P> isSmallerThan(String value) throws XNWH_WhereClauseBuildException {
    checkLocked("isSmallerThan");
    return createWhereClausesConnection(value, Operator.SMALLERTHAN);
  }

  
  public WhereClausesConnection<P> isNull() throws XNWH_WhereClauseBuildException {
    checkLocked("isNull");
    return createWhereClausesConnection(null, Operator.ISNULL);
  }


  private WhereClausesConnection<P> createWhereClausesConnection(String value, Operator operator) {
    this.value = value;
    this.operator = operator;
    WhereClausesConnection<P> wcc = new WhereClausesConnection<P>(this);
    getParent().addWhereClause(wcc);
    return wcc;
  }


  @Override
  public String asString() {
    switch(operator) {
      case IN:
        StringBuilder sb = new StringBuilder(getColumn());
        sb.append(" IN (");        
        for (int i = 0; i<inValues.length; i++) {
          if (i > 0) {
            sb.append(", ");
          }
          sb.append("?");
        }
        sb.append(")");
        return sb.toString();
      case ISNULL:
        return getColumn() + " IS NULL ";
      default:
        return getColumn() + " " + operator.getSQL() + " ?";
    }    
  }


  @Override
  public void addParameter(List<Object> list) {
    if (operator == Operator.IN) {
      for (String v : inValues) {
        list.add(v);
      }
    } else if (operator != Operator.ISNULL) {
      list.add(value);
    }
  }


  public Operator getOperator() {
    return operator;
  }


  public String getParameterValue() {
    return value;
  }

  
  public void setParameterValue(String value) {
    this.value = value;
  }

}
