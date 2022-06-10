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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt.selectorderinputsource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;


public class OrderInputSourceWhereClausesContainerImpl implements OrderInputSourceWhereClausesContainer<OrderInputSourceWhereClausesContainerImpl>, Serializable {

  private static final long serialVersionUID = 1L;
  private List<WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl>> whereClauses;

  public OrderInputSourceWhereClausesContainerImpl() {
    this.whereClauses = new ArrayList<WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl>>() ;
  }

  public WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> where(WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> wcc = new WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, false));
    addWhereClause(wcc);
    return wcc;
  }

  public WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> whereNot(WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> wcc = new WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, true));
    addWhereClause(wcc);
    return wcc;
  }

  public void addWhereClause(WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> wcc) {
    whereClauses.add(wcc);
  }

  private static class WhereClauseBrace extends WhereClause<OrderInputSourceWhereClausesContainerImpl> {

    private static final long serialVersionUID = 396835642050058815L;

    private OrderInputSourceWhereClausesContainerImpl innerContainer;
    private boolean negated;


    public WhereClauseBrace(WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> child, OrderInputSourceWhereClausesContainerImpl container, boolean negated) {
      super(container, null);
      innerContainer = child.getCorrespondingContainer();
      this.negated = negated;
    }


    @Override
    public String asString() throws XNWH_InvalidSelectStatementException {
      StringBuilder sb = new StringBuilder();
      if (negated) {
        sb.append("not ");
      }
      sb.append("(").append(innerContainer.getSelectString()).append(")");
      return sb.toString();
    }


    @Override
    public void addParameter(List<Object> list) {
      //parameter des inneren containers adden
      innerContainer.addParameter(list);
    }

  }

  
  
  protected List<WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl>> getWhereClauses() {
    return whereClauses;
  }
  
  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    if (getWhereClauses().size() > 0) {
      for (WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> wcc : getWhereClauses()) {
        sb.append(" ").append(wcc.getAsSqlString());
      }
    }
    return sb.toString();
  }
  
  protected void addParameter(List<Object> list) {
    if (getWhereClauses().size() > 0) {
      for (WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> wcc : getWhereClauses()) {
        wcc.addParameter(list);
      }
    }
  }
  
  public WhereClauseNumber<OrderInputSourceWhereClausesContainerImpl> whereId() {
    return new WhereClauseNumber<OrderInputSourceWhereClausesContainerImpl>(this, OrderInputSourceColumn.ID.getColumnName());
  }

  public WhereClauseString<OrderInputSourceWhereClausesContainerImpl> whereName() {
    return new WhereClauseString<OrderInputSourceWhereClausesContainerImpl>(this, OrderInputSourceColumn.NAME.getColumnName());
  }

  public WhereClauseString<OrderInputSourceWhereClausesContainerImpl> whereType() {
    return new WhereClauseString<OrderInputSourceWhereClausesContainerImpl>(this, OrderInputSourceColumn.TYPE.getColumnName());
  }

  public WhereClauseString<OrderInputSourceWhereClausesContainerImpl> whereOrderType() {
    return new WhereClauseString<OrderInputSourceWhereClausesContainerImpl>(this, OrderInputSourceColumn.ORDERTYPE.getColumnName());
  }

  public WhereClauseString<OrderInputSourceWhereClausesContainerImpl> whereApplicationName() {
    return new WhereClauseString<OrderInputSourceWhereClausesContainerImpl>(this, OrderInputSourceColumn.APPLICATIONNAME.getColumnName());
  }

  public WhereClauseString<OrderInputSourceWhereClausesContainerImpl> whereVersionName() {
    return new WhereClauseString<OrderInputSourceWhereClausesContainerImpl>(this, OrderInputSourceColumn.VERSIONNAME.getColumnName());
  }

  public WhereClauseString<OrderInputSourceWhereClausesContainerImpl> whereWorkspaceName() {
    return new WhereClauseString<OrderInputSourceWhereClausesContainerImpl>(this, OrderInputSourceColumn.WORKSPACENAME.getColumnName());
  }

  public WhereClauseString<OrderInputSourceWhereClausesContainerImpl> whereDocumentation() {
    return new WhereClauseString<OrderInputSourceWhereClausesContainerImpl>(this, OrderInputSourceColumn.DOCUMENTATION.getColumnName());
  }

}
