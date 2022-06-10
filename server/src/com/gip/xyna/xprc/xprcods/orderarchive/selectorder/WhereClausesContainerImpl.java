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

package com.gip.xyna.xprc.xprcods.orderarchive.selectorder;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClauseBoolean;
import com.gip.xyna.xnwh.selection.WhereClauseEnum;
import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClauseStringSerializable;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;



public class WhereClausesContainerImpl implements WhereClausesContainer<WhereClausesContainerImpl>, Serializable {

  private static final long serialVersionUID = 3675926935493837745L;
  private List<WhereClausesConnection<WhereClausesContainerImpl>> whereClauses;


  public WhereClausesContainerImpl() {
    whereClauses = new ArrayList<WhereClausesConnection<WhereClausesContainerImpl>>();
  }
  

  public WhereClause<WhereClausesContainerImpl> where(OrderInstanceColumn column) {
    switch (column.getColumnType()) {
      case NUMBER :
        return new WhereClauseNumber<WhereClausesContainerImpl>(this, column.getColumnName());
      case BOOLEAN :
        return new WhereClauseBoolean<WhereClausesContainerImpl>(this, column.getColumnName());
      case STRING :
        return new WhereClauseString<WhereClausesContainerImpl>(this, column.getColumnName());
      case STRING_SERIALIZABLE :
        switch( column ) {
          case C_STATUS:
            return WhereClauseStringSerializable.construct( this, OrderInstanceStatus.CANCELED, column.getColumnName() );
          default:
            throw new IllegalArgumentException("Unexpected StringSerializable cloumn " + column.getColumnName());
        }
      default :
        throw new IllegalArgumentException("Can not query complex row " + column.getColumnName());
    }
  }


  public WhereClauseString<WhereClausesContainerImpl> whereOrderType() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_ORDER_TYPE.getColumnName());
  }


  public WhereClausesConnection<WhereClausesContainerImpl> where(WhereClausesConnection<WhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<WhereClausesContainerImpl> wcc =
        new WhereClausesConnection<WhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, false));
    addWhereClause(wcc);
    return wcc;
  }


  public WhereClausesConnection<WhereClausesContainerImpl> whereNot(WhereClausesConnection<WhereClausesContainerImpl> innerWhereClause) {
    WhereClausesConnection<WhereClausesContainerImpl> wcc =
        new WhereClausesConnection<WhereClausesContainerImpl>(new WhereClauseBrace(innerWhereClause, this, true));
    addWhereClause(wcc);
    return wcc;
  }


  public WhereClauseNumber<WhereClausesContainerImpl> whereId() {
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_ID.getColumnName());
  }


  //FIXME codeduplizierung an anderen stellen, wo genau so eine klasse verwendet wird (suche nach whereclausebrace)
  public static class WhereClauseBrace extends WhereClause<WhereClausesContainerImpl> {

    private static final long serialVersionUID = 396835642050058815L;

    private WhereClausesContainerImpl innerContainer;
    private boolean negated;


    public WhereClauseBrace(WhereClausesConnection<WhereClausesContainerImpl> child,
                            WhereClausesContainerImpl container, boolean negated) {
      super(container, null);
      innerContainer = (WhereClausesContainerImpl) child.getCorrespondingContainer();
      this.negated = negated;
    }


    @Override
    public String asString() throws XNWH_InvalidSelectStatementException {
      StringBuffer sb = new StringBuffer();
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
    
    public boolean isNegated() {
      return negated;
    }
    
    public WhereClausesContainerImpl getInnerWC() {
      return innerContainer;
    }

  }


  public void addWhereClause(WhereClausesConnection<WhereClausesContainerImpl> wcc) {
    whereClauses.add(wcc);
  }


  public List<WhereClausesConnection<WhereClausesContainerImpl>> getWhereClauses() {
    return Collections.unmodifiableList(whereClauses);
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuffer sb = new StringBuffer();
    List<WhereClausesConnection<WhereClausesContainerImpl>> wcs = getWhereClauses();
    if (wcs.size() > 0) {
      for (WhereClausesConnection<WhereClausesContainerImpl> wcc : wcs) {
        sb.append(" ").append(wcc.getAsSqlString());
      }
    }
    return sb.toString();
  }
  
  protected void addParameter(List<Object> list) {
    List<WhereClausesConnection<WhereClausesContainerImpl>> wcs = getWhereClauses();
    if (wcs.size() > 0) {
      for (WhereClausesConnection<WhereClausesContainerImpl> wcc : wcs) {
        wcc.addParameter(list);
      }      
    }
  }


  public WhereClauseString<WhereClausesContainerImpl> whereCustom0() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_CUSTOM0.getColumnName());
  }


  public WhereClauseString<WhereClausesContainerImpl> whereCustom1() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_CUSTOM1.getColumnName());
  }


  public WhereClauseString<WhereClausesContainerImpl> whereCustom2() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_CUSTOM2.getColumnName());
  }


  public WhereClauseString<WhereClausesContainerImpl> whereCustom3() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_CUSTOM3.getColumnName());
  }


  public WhereClauseString<WhereClausesContainerImpl> whereExecutionType() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_EXECUTION_TYPE.getColumnName());
  }


  public WhereClauseNumber<WhereClausesContainerImpl> whereLastUpdate() {
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_LAST_UPDATE.getColumnName());
  }


  public WhereClauseNumber<WhereClausesContainerImpl> whereMonitoringLevel() {
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_MONITORING_LEVEL.getColumnName());
  }


  public WhereClauseNumber<WhereClausesContainerImpl> whereParentId() {
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_PARENT_ID.getColumnName());
  }


  public WhereClauseNumber<WhereClausesContainerImpl> wherePriority() {
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_PRIORITY.getColumnName());
  }


  public WhereClauseNumber<WhereClausesContainerImpl> whereStartTime() {
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_START_TIME.getColumnName());
  }
  
  
  public WhereClauseNumber<WhereClausesContainerImpl> whereStopTime() {
    return new WhereClauseNumber<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_STOP_TIME.getColumnName());
  }


  public WhereClauseString<WhereClausesContainerImpl> whereStatus() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_STATUS.getColumnName());
  }
  
  
  public WhereClauseEnum<WhereClausesContainerImpl,OrderInstanceStatus> whereStatusEnum() {
    return new WhereClauseEnum<WhereClausesContainerImpl,OrderInstanceStatus>(this, OrderInstanceStatus.class, OrderInstanceColumn.C_STATUS.getColumnName());   
  }

  
  public WhereClauseString<WhereClausesContainerImpl> whereStatusCompensate() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_STATUS_COMPENSATE.getColumnName());
  }


  public WhereClauseString<WhereClausesContainerImpl> whereSuspended() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_SUSPENSION_STATUS.getColumnName());
  }


  public WhereClauseString<WhereClausesContainerImpl> whereSuspensionCause() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_SUSPENSION_CAUSE.getColumnName());
  }


  public WhereClauseString<WhereClausesContainerImpl> whereSessionId() {
    return new WhereClauseString<WhereClausesContainerImpl>(this, OrderInstanceColumn.C_SESSION_ID.getColumnName());
  }
}
