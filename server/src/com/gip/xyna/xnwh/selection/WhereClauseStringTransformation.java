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



import java.io.Serializable;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;



public class WhereClauseStringTransformation<P extends WhereClausesContainerBase<P>> extends WhereClause<P> {

  private static final long serialVersionUID = -397504443992552763L;
  private List<String> transformedValues;
  private Operator operator;
  private Transformation transformation;
  
  public static interface Transformation extends Serializable {

    List<String> transform(String value);
    
  }

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


  public WhereClauseStringTransformation(P parent, String col, Transformation transformation) {
    super(parent, col);
    this.transformation = transformation;
  }

  @Override
  public WhereClauseStringTransformation<P> copy(String col) {
    WhereClauseStringTransformation<P> copy = new WhereClauseStringTransformation<P>(getParent(), col, transformation);
    copy.transformedValues = transformedValues;
    copy.operator = operator;
    return copy;
  }
  
  public WhereClausesConnection<P> isEqual(String value) throws XNWH_WhereClauseBuildException {
    checkLocked("isEqual");
    return createWhereClausesConnection(value, Operator.EQUALS);
  }
  
  public WhereClausesConnection<P> isNull() throws XNWH_WhereClauseBuildException {
    checkLocked("isNull");
    return createWhereClausesConnection(null, Operator.ISNULL);
  }


  private WhereClausesConnection<P> createWhereClausesConnection(String value, Operator operator) {
    transformedValues = transformation.transform(value);
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
      case EQUALS:
        String colEqualsVal = getColumn() + " " + operator.getSQL() + " ?";
        if( transformedValues== null || transformedValues.size() == 0 ) {
          return getColumn() + " IS NULL ";
        } else if( transformedValues.size() == 1 ) {
          return colEqualsVal;
        } else {
          StringBuilder sb = new StringBuilder();
          String sep = "(";
          for(int i=0; i<transformedValues.size(); ++i ) {
            sb.append(sep).append(colEqualsVal);
            sep = " or ";
          }
          sb.append(")");
          return sb.toString();
        }
      default:
        return ""; //FIXME weitere implementieren
    }    
  }


  @Override
  public void addParameter(List<Object> list) {
    if(operator != Operator.ISNULL) {
      if( transformedValues != null ) {
        list.addAll( transformedValues );
      }
    }
  }


  public Operator getOperator() {
    return operator;
  }

/*
  public String getParameterValue() {
    return value;
  }
*/
}
