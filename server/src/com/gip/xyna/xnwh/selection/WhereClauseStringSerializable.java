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

import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;



public class WhereClauseStringSerializable<P extends WhereClausesContainerBase<P>, S extends StringSerializable<S>> extends WhereClause<P> {

  private static final long serialVersionUID = -397504443992552763L;
  private S value;
  private Operator operator;
  private S example;

  
  public static enum Operator {
    EQUALS("="), ISNULL("IS NULL");

    private String sql;


    private Operator(String sql) {
      this.sql = sql;
    }


    public String getSQL() {
      return sql;
    }
  }


  public WhereClauseStringSerializable(P parent, S example, String col) {
    super(parent, col);
    if( example == null ) {
      throw new IllegalArgumentException("example is null ");
    }
    this.example = example;
  }
  
  public static <P extends WhereClausesContainerBase<P>, S extends StringSerializable<S>> WhereClauseStringSerializable<P,S> construct(P parent, S example, String col) {
    return new WhereClauseStringSerializable<P,S>(parent,example,col);
  }

  @Override
  public WhereClauseStringSerializable<P,S> copy(String col) {
    WhereClauseStringSerializable<P,S> copy = new WhereClauseStringSerializable<P,S>(getParent(), example, col);
    copy.value = value;
    copy.operator = operator;
    return copy;
  }

  public WhereClausesConnection<P> isEqual(S value) throws XNWH_WhereClauseBuildException {
    checkLocked("isEqual");
    return createWhereClausesConnection(value, Operator.EQUALS);
  }
  
  public WhereClausesConnection<P> isEqual(String value) throws XNWH_WhereClauseBuildException {
    checkLocked("isEqual");
    return createWhereClausesConnection( deserializeFromString(value), Operator.EQUALS);
  }

  public WhereClausesConnection<P> isNull() throws XNWH_WhereClauseBuildException {
    checkLocked("isNull");
    return createWhereClausesConnection(null, Operator.ISNULL);
  }


  private WhereClausesConnection<P> createWhereClausesConnection(S value, Operator operator) {
    this.value = value;
    this.operator = operator;
    WhereClausesConnection<P> wcc = new WhereClausesConnection<P>(this);
    getParent().addWhereClause(wcc);
    return wcc;
  }

  private S deserializeFromString(String value) {
    if( example != null ) {
      return example.deserializeFromString(value);
    } else {
      throw new IllegalArgumentException("example is null ");
    }
  }

  

  @Override
  public String asString() {
    switch(operator) {
      case ISNULL:
        return getColumn() + " IS NULL ";
      default:
        return getColumn() + " " + operator.getSQL() + " ?";
    }    
  }


  @Override
  public void addParameter(List<Object> list) {
    if(operator != Operator.ISNULL) {
      list.add(value);
    }
  }


  public Operator getOperator() {
    return operator;
  }


  public S getParameterValue() {
    return value;
  }

}
