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

package com.gip.xyna.xfmg.xfctrl.xmomdatabase.search;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClauseBoolean;
import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;




public class WhereClausesContainerImpl implements WhereClausesContainer<WhereClausesContainerImpl>, Serializable {

  private static final long serialVersionUID = 3675926935493837745L;
  private List<WhereClausesConnection<WhereClausesContainerImpl>> whereClauses;
  private Set<XMOMDatabaseEntryColumn> columnsWithinWhereClauses;
  


  public WhereClausesContainerImpl() {
    whereClauses = new ArrayList<WhereClausesConnection<WhereClausesContainerImpl>>();
    columnsWithinWhereClauses = new HashSet<XMOMDatabaseEntryColumn>();
  }


  public WhereClausesConnection<WhereClausesContainerImpl> where(WhereClausesConnection<WhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<WhereClausesContainerImpl> wcc = new WhereClausesConnection<WhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause,
                                                                                                                                       this,
                                                                                                                                       false));
    addWhereClause(wcc);
    return wcc;
  }


  public WhereClausesConnection<WhereClausesContainerImpl> whereNot(WhereClausesConnection<WhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<WhereClausesContainerImpl> wcc = new WhereClausesConnection<WhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause,
                                                                                                                                       this,
                                                                                                                                       true));
    addWhereClause(wcc);
    return wcc;
  }


  public WhereClausesConnection<WhereClausesContainerImpl> and(WhereClausesConnection<WhereClausesContainerImpl> additionalWhereClause) {
    if (whereClauses.size() == 0) {
      return where(additionalWhereClause);
    }
    WhereClausesConnection<WhereClausesContainerImpl> last = whereClauses.get(whereClauses.size() - 1);
    return last.and().where(additionalWhereClause);
  }


  private static class WhereClauseBrace extends WhereClause<WhereClausesContainerImpl> {

    private static final long serialVersionUID = 396835642050058815L;

    private WhereClausesContainerImpl innerContainer;
    private boolean negated;


    public WhereClauseBrace(WhereClausesConnection<WhereClausesContainerImpl> child,
                            WhereClausesContainerImpl container, boolean negated) {
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
    try {
      if (wcc.getConnectedObject() instanceof WhereClauseBrace) {
        columnsWithinWhereClauses.addAll(((WhereClauseBrace)wcc.getConnectedObject()).innerContainer.getColumnsWithinWhereClauses());
      }
    } catch (Throwable e) {
      ;
    }
    if (wcc.getConnectedObject() != null && wcc.getConnectedObject().getColumn() != null && !wcc.getConnectedObject().getColumn().equals("")) {
      XMOMDatabaseEntryColumn column = XMOMDatabaseEntryColumn.valueOf(wcc.getConnectedObject().getColumn());
      columnsWithinWhereClauses.add(column);
    }
    
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


  public WhereClauseString<WhereClausesContainerImpl> where(XMOMDatabaseEntryColumn column) {
    columnsWithinWhereClauses.add(column);
    return new WhereClauseString<WhereClausesContainerImpl>(this, column.toString());
  }
  
  public WhereClauseNumber<WhereClausesContainerImpl> whereNumber(XMOMDatabaseEntryColumn column) {
    columnsWithinWhereClauses.add(column);
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, column.toString());
  }
  
  public WhereClauseBoolean<WhereClausesContainerImpl> whereBoolean(XMOMDatabaseEntryColumn column) {
    columnsWithinWhereClauses.add(column);
    return new WhereClauseBoolean<WhereClausesContainerImpl>(this, column.toString());
  }
  
  
  public Set<XMOMDatabaseEntryColumn> getColumnsWithinWhereClauses() {
    return columnsWithinWhereClauses;
  }
  

}
