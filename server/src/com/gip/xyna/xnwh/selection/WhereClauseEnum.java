/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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



public class WhereClauseEnum<P extends WhereClausesContainerBase<P>, E extends Enum<E>> extends WhereClause<P> {

  private static final long serialVersionUID = -397504443992552763L;
  private E value;
  private Operator operator;
  private Class<E> enumType;


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


  public WhereClauseEnum(P parent, Class<E> enumType, String col) {
    super(parent, col);
    this.enumType = enumType;
  }

  @Override
  public WhereClauseEnum<P,E> copy(String col) {
    WhereClauseEnum<P,E> copy = new WhereClauseEnum<P,E>(getParent(), enumType, col);
    copy.value = value;
    copy.operator = operator;
    return copy;
  }

  public WhereClausesConnection<P> isEqual(E value) throws XNWH_WhereClauseBuildException {
    checkLocked("isEqual");
    return createWhereClausesConnection(value, Operator.EQUALS);
  }
  
  public WhereClausesConnection<P> isEqual(String value) throws XNWH_WhereClauseBuildException {
    checkLocked("isEqual");
    return createWhereClausesConnection(Enum.valueOf(enumType, value), Operator.EQUALS);
  }
  
  public WhereClausesConnection<P> isNull() throws XNWH_WhereClauseBuildException {
    checkLocked("isNull");
    return createWhereClausesConnection(null, Operator.ISNULL);
  }


  private WhereClausesConnection<P> createWhereClausesConnection(E value, Operator operator) {
    this.value = value;
    this.operator = operator;
    WhereClausesConnection<P> wcc = new WhereClausesConnection<P>(this);
    getParent().addWhereClause(wcc);
    return wcc;
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


  public E getParameterValue() {
    return value;
  }

}
