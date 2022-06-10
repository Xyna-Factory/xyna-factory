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

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;



public class WhereClausesConnection<P extends WhereClausesContainerBase<P>> implements Serializable {
  
  private static final long serialVersionUID = 2655224534163988085L;
  private WhereClause<P> wcObject;
  private Connect connect;
  
  public static enum Connect {
    AND("and"), OR("or");
    
    private String sql;
    private Connect(String sql) {
      this.sql = sql;
    }
    public String getSql() {
      return sql;
    }
  }
  
  public WhereClausesConnection(WhereClause<P> wcObject) {
    this.wcObject = wcObject;
  }
  
  public P and() {
    connect = Connect.AND;
    return (P) wcObject.getParent();
  }

  public P or() {
    connect = Connect.OR;
    return wcObject.getParent();
  }

  public <T> T finalizeSelect(Class<T> clazz) {
    if (getCorrespondingContainer() == null ) {
      throw new IllegalArgumentException("can not finalize select on subselect");
    }
    Object o = getCorrespondingContainer();
    if( clazz.isAssignableFrom(o.getClass() ) ) {
      return clazz.cast(o);
    } else {
      throw new IllegalArgumentException("can not finalize select on subselect");
    }
  }
  
  public String getAsSqlString() throws XNWH_InvalidSelectStatementException {
    String s = "";
    s += wcObject.asString();
    if (connect != null) {
      s+=" " + connect.getSql();
    }
    return s;
  }
  
  public P getCorrespondingContainer() {
    return wcObject.getParent();
  }
  
  public void addParameter(List<Object> list) {
    wcObject.addParameter(list);
  }
  
  public WhereClause<P> getConnectedObject() {
    return wcObject;
  }
  
  public Connect getConnection() {
    return connect;
  }

  public void replaceWhereClause(WhereClause<P> wc) {
    wc.changeParent(wcObject.getParent());
    wcObject = wc;
  }
}
