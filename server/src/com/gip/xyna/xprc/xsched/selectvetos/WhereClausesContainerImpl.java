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

package com.gip.xyna.xprc.xsched.selectvetos;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;




public class WhereClausesContainerImpl implements WhereClausesContainer<WhereClausesContainerImpl>, Serializable {

  private static final long serialVersionUID = 3675926935493837745L;
  private List<WhereClausesConnection<WhereClausesContainerImpl>> whereClauses;


  public WhereClausesContainerImpl() {
    whereClauses = new ArrayList<WhereClausesConnection<WhereClausesContainerImpl>>();
  }




  public WhereClausesConnection<WhereClausesContainerImpl> where(WhereClausesConnection<WhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<WhereClausesContainerImpl> wcc = new WhereClausesConnection<WhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, false));
    addWhereClause(wcc);
    return wcc;
  }


  public WhereClausesConnection<WhereClausesContainerImpl> whereNot(WhereClausesConnection<WhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<WhereClausesContainerImpl> wcc = new WhereClausesConnection<WhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, true));
    addWhereClause(wcc);
    return wcc;
  }

  
  private static class WhereClauseBrace extends WhereClause<WhereClausesContainerImpl> {

    private static final long serialVersionUID = 396835642050058815L;

    private WhereClausesContainerImpl innerContainer;
    private boolean negated;


    public WhereClauseBrace(WhereClausesConnection<WhereClausesContainerImpl> child, WhereClausesContainerImpl container, boolean negated) {
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


  public void addWhereClause(WhereClausesConnection<WhereClausesContainerImpl> wcc) {
    whereClauses.add(wcc);
  }


  protected List<WhereClausesConnection<WhereClausesContainerImpl>> getWhereClauses() {
    return whereClauses;
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    if (getWhereClauses().size() > 0) {
      for (WhereClausesConnection<WhereClausesContainerImpl> wcc : getWhereClauses()) {
        sb.append(" ").append(wcc.getAsSqlString());
      }
    }
    return sb.toString();
  }
  
  protected void addParameter(List<Object> list) {
    if (getWhereClauses().size() > 0) {
      for (WhereClausesConnection<WhereClausesContainerImpl> wcc : getWhereClauses()) {
        wcc.addParameter(list);
      }      
    }
  }


  public WhereClauseString<WhereClausesContainerImpl> whereVetoName() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, VetoColumn.VETONAME.getColumnName());
  }


  public WhereClauseNumber<WhereClausesContainerImpl> whereUsingOrderId() {
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, VetoColumn.USINGORDERID.getColumnName());
  }

  public WhereClauseNumber<WhereClausesContainerImpl> whereUsingRootOrderId() {
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, VetoColumn.USINGROOTORDERID.getColumnName());
  }

  public WhereClauseString<WhereClausesContainerImpl> whereUsingOrderType() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, VetoColumn.USINGORDERTYPE.getColumnName());
  }


  public WhereClauseString<WhereClausesContainerImpl> whereDocumentation() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, VetoColumn.DOCUMENTATION.getColumnName());
  }


}
