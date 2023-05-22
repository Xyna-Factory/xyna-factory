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

package com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClauseBoolean;
import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;




public class CronLikeOrderWhereClausesContainerImpl implements CronLikeOrderWhereClausesContainer, Serializable {

  private static final long serialVersionUID = 3675926935493837745L;
  private List<WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl>> whereClauses;


  public CronLikeOrderWhereClausesContainerImpl() {
    whereClauses = new ArrayList<WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl>>();
  }




  public WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> where(WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> wcc = new WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, false));
    addWhereClause(wcc);
    return wcc;
  }


  public WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> whereNot(WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> wcc = new WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, true));
    addWhereClause(wcc);
    return wcc;
  }

  
  private static class WhereClauseBrace extends WhereClause<CronLikeOrderWhereClausesContainerImpl> {

    private static final long serialVersionUID = 396835642050058815L;

    private CronLikeOrderWhereClausesContainerImpl innerContainer;
    private boolean negated;


    public WhereClauseBrace(WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> child, CronLikeOrderWhereClausesContainerImpl container, boolean negated) {
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


  public void addWhereClause(WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> wcc) {
    whereClauses.add(wcc);
  }


  protected List<WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl>> getWhereClauses() {
    return whereClauses;
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    if (getWhereClauses().size() > 0) {
      for (WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> wcc : getWhereClauses()) {
        sb.append(" ").append(wcc.getAsSqlString());
      }
    }
    return sb.toString();
  }
  
  protected void addParameter(List<Object> list) {
    if (getWhereClauses().size() > 0) {
      for (WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> wcc : getWhereClauses()) {
        wcc.addParameter(list);
      }      
    }
  }


  public WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl> whereId() {
    return new WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.ID.getColumnName());
  }

  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereLabel() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.LABEL.getColumnName());
  }

  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereOrdertype() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.ORDERTYPE.getColumnName());
  }

  public WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl> whereStartTime() {
    return new WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.STARTTIME.getColumnName());
  }
  
  public WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl> whereNextExecution() {
    return new WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.NEXTEXECUTION.getColumnName());
  }

  public WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl> whereInterval() {
    return new WhereClauseNumber<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.INTERVAL.getColumnName());
  }

  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereStatus() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.STATUS.getColumnName());
  }

  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereOnError() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.ONERROR.getColumnName());
  }

  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereApplicationname() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.APPLICATIONNAME.getColumnName());
  }

  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereVersionname() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.VERSIONNAME.getColumnName());
  }

  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereWorkspacename() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.WORKSPACENAME.getColumnName());
  }

  public WhereClauseBoolean<CronLikeOrderWhereClausesContainerImpl> whereEnabled() {
    return new WhereClauseBoolean<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.ENABLED.getColumnName());
  }
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereTimeZoneID() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.TIMEZONEID.getColumnName());
  }
  
  public WhereClauseBoolean<CronLikeOrderWhereClausesContainerImpl> whereConsiderDaylightSaving() {
    return new WhereClauseBoolean<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.CONSIDERDAYLIGHTSAVING.getColumnName());
  }
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereCustom0() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.CUSTOM0.getColumnName());
  }

  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereCustom1() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.CUSTOM1.getColumnName());
  }
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereCustom2() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.CUSTOM2.getColumnName());
  }
  
  public WhereClauseString<CronLikeOrderWhereClausesContainerImpl> whereCustom3() {
    return new WhereClauseString<CronLikeOrderWhereClausesContainerImpl>(this, CronLikeOrderColumn.CUSTOM3.getColumnName());
  }
}
