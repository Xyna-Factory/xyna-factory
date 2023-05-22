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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.selectdatamodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;

public class DataModelWhereClausesContainerImpl implements DataModelWhereClausesContainer<DataModelWhereClausesContainerImpl>, Serializable {

  private static final long serialVersionUID = 3675926935493837745L;
  private List<WhereClausesConnection<DataModelWhereClausesContainerImpl>> whereClauses;

  public DataModelWhereClausesContainerImpl() {
    whereClauses = new ArrayList<WhereClausesConnection<DataModelWhereClausesContainerImpl>>();
  }

  public WhereClausesConnection<DataModelWhereClausesContainerImpl> where(WhereClausesConnection<DataModelWhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<DataModelWhereClausesContainerImpl> wcc = new WhereClausesConnection<DataModelWhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, false));
    addWhereClause(wcc);
    return wcc;
  }

  public WhereClausesConnection<DataModelWhereClausesContainerImpl> whereNot(WhereClausesConnection<DataModelWhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<DataModelWhereClausesContainerImpl> wcc = new WhereClausesConnection<DataModelWhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, true));
    addWhereClause(wcc);
    return wcc;
  }

  private static class WhereClauseBrace extends WhereClause<DataModelWhereClausesContainerImpl> {

    private static final long serialVersionUID = 396835642050058815L;

    private DataModelWhereClausesContainerImpl innerContainer;
    private boolean negated;


    public WhereClauseBrace(WhereClausesConnection<DataModelWhereClausesContainerImpl> child, DataModelWhereClausesContainerImpl container, boolean negated) {
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


  public void addWhereClause(WhereClausesConnection<DataModelWhereClausesContainerImpl> wcc) {
    whereClauses.add(wcc);
  }


  protected List<WhereClausesConnection<DataModelWhereClausesContainerImpl>> getWhereClauses() {
    return whereClauses;
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    if (getWhereClauses().size() > 0) {
      for (WhereClausesConnection<DataModelWhereClausesContainerImpl> wcc : getWhereClauses()) {
        sb.append(" ").append(wcc.getAsSqlString());
      }
    }
    return sb.toString();
  }
  
  protected void addParameter(List<Object> list) {
    if (getWhereClauses().size() > 0) {
      for (WhereClausesConnection<DataModelWhereClausesContainerImpl> wcc : getWhereClauses()) {
        wcc.addParameter(list);
      }      
    }
  }

  public WhereClauseString<DataModelWhereClausesContainerImpl> whereFqName() {
    return new WhereClauseString<DataModelWhereClausesContainerImpl>(this, DataModelColumn.FQNAME.getColumnName());
  }

  public WhereClauseString<DataModelWhereClausesContainerImpl> whereLabel() {
    return new WhereClauseString<DataModelWhereClausesContainerImpl>(this, DataModelColumn.LABEL.getColumnName());
  }

  public WhereClauseString<DataModelWhereClausesContainerImpl> whereDataModelType() {
    return new WhereClauseString<DataModelWhereClausesContainerImpl>(this, DataModelColumn.DATAMODELTYPE.getColumnName());
  }

  public WhereClauseString<DataModelWhereClausesContainerImpl> whereBaseFqName() {
    return new WhereClauseString<DataModelWhereClausesContainerImpl>(this, DataModelColumn.BASETYPEFQNAME.getColumnName());
  }

  public WhereClauseString<DataModelWhereClausesContainerImpl> whereBaseLabel() {
    return new WhereClauseString<DataModelWhereClausesContainerImpl>(this, DataModelColumn.BASETYPELABEL.getColumnName());
  }

  public WhereClauseString<DataModelWhereClausesContainerImpl> whereVersion() {
    return new WhereClauseString<DataModelWhereClausesContainerImpl>(this, DataModelColumn.VERSION.getColumnName());
  }

  public WhereClauseString<DataModelWhereClausesContainerImpl> whereDocumentation() {
    return new WhereClauseString<DataModelWhereClausesContainerImpl>(this, DataModelColumn.DOCUMENTATION.getColumnName());
  }

}
