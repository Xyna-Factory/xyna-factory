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
package com.gip.xyna.xprc.xbatchmgmt.selectbatch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessArchiveStorable;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;


public class BatchProcessSelectImpl extends BatchProcessWhereClausesImpl implements Selection, Serializable {
  private static final long serialVersionUID = 1L;

  private List<BatchProcessWhereClausesImpl> rightWhereClauses; //where-Clause f�r Rechte�berpr�fung

  private static final Pattern RIGHT_PART_SEPERATION_PATTERN = Pattern.compile("(?<!\\\\)[:]");
  
  /**
   * Liefert den select-String f�r die �bergebene BatchProcessTable mit where-Bedingung
   * FIXME hierbei werden die urspr�nglichen connects nicht beachtet!
   * Gleiche Spalten werden mit "or" und unterschiedliche Spalten mit "and"
   * verkn�pft.
   * @param table
   * @return
   * @throws XNWH_InvalidSelectStatementException
   */
  public String getSelectString(BatchProcessTable table) throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select ").append(table.getBatchProcessIdColumn()).append(" from ").append(table.getTableName());
    if (hasWhereClauses(table)) {
      sb.append(" where").append( super.getSelectString(table) );
    }
    
    //f�r Archive entweder die abgeschlossenen oder die laufenden Auftr�ge selektieren
    if (table == BatchProcessTable.Archive || table == BatchProcessTable.ArchiveRunning) {
      sb.append( hasWhereClauses(table) ? " and " : " where ");
      sb.append(table == BatchProcessTable.ArchiveRunning ? "":  "not ");
      appendOrderIsRunning(sb);
      appendRightCondition(table, sb); //Rechte�berpr�fung
    }
    
    return sb.toString();
  }

  private void appendOrderIsRunning(StringBuilder sb) {
    sb.append("(").
       append(BatchProcessArchiveStorable.COL_ORDER_STATUS).append(" = '").
       append(OrderInstanceStatus.SCHEDULING.getName()).append("'").
       append(" or ").
       append(BatchProcessArchiveStorable.COL_ORDER_STATUS).append(" = '").
       append(OrderInstanceStatus.WAITING_FOR_BATCH_PROCESS.getName()).append("'").
       append(")");
  }
  
  /**
   * H�ngt die where-Clause f�r die Rechte�berpr�fung an
   * @param table
   * @param sb
   * @throws XNWH_InvalidSelectStatementException
   */
  private void appendRightCondition(BatchProcessTable table, StringBuilder sb) throws XNWH_InvalidSelectStatementException {
    if (rightWhereClauses != null && rightWhereClauses.size() > 0) {
      sb.append(" and (");
      for (int i=0; i<rightWhereClauses.size(); i++) {
        sb.append("(");
        sb.append(rightWhereClauses.get(i).getSelectString(table));
        sb.append(")");
        if (i < rightWhereClauses.size()-1) {
          sb.append(" or ");
        }
      }
      sb.append(")");
    }
  }


  public Parameter getParameter(BatchProcessTable table) {
    List<Object> list = new ArrayList<Object>();
    super.addParameter(list, table);
    
    //Parameter f�r Rechte�berpr�fung hinzuf�gen
    if (rightWhereClauses != null && rightWhereClauses.size() > 0) {
      if (table == BatchProcessTable.Archive || table == BatchProcessTable.ArchiveRunning) {
        for (BatchProcessWhereClausesImpl right : rightWhereClauses) {
          right.addParameter(list, table);
        }
      }
    }
    
    Parameter paras = new Parameter(list.toArray());
    return paras;
  }

  /**
   * Baut aus den TCO-Rechten die passenden WhereClauses.
   * Dabei muss das Recht wie folgt aufgebaut sein:
   * key:action:slaveordertype:application:version <br>
   * (Doppelpunkte innerhalb der einzelnen Teile m�ssen durch \ escaped sein.)
   * 
   * '*' wird immer als Wildcard interpretiert. <br>
   * @param scopedRights
   * @throws XNWH_WhereClauseBuildException
   */
  public void setRightWhereClauses (List<String> scopedRights) throws XNWH_WhereClauseBuildException {
    if (scopedRights != null && scopedRights.size() > 0) {
      if (scopedRights.size() == 1 &&
          scopedRights.get(0).equals(ScopedRight.TIME_CONTROLLED_ORDER.allAccess())) {
        return;
      }
      rightWhereClauses = new ArrayList<BatchProcessWhereClausesImpl>();
      
      for (String right : scopedRights) {
        BatchProcessWhereClausesImpl select = new BatchProcessWhereClausesImpl();
        String[] rightParts = RIGHT_PART_SEPERATION_PATTERN.split(right, -1);
        prepareRightParts(rightParts);
        if (rightParts[2].length() == 0) {
          select.whereSlaveOrderType().isNull();
        } else {
          select.whereSlaveOrderType().isLike(rightParts[2]);
        }
        if (rightParts[3].length() == 0) {
          select.whereApplication().isNull();
        } else {
          select.whereApplication().isLike(rightParts[3]);
        }
        if (rightParts[4].length() == 0) {
          select.whereVersion().isNull();
        } else {
          select.whereVersion().isLike(rightParts[4]);
        }
        rightWhereClauses.add(select);
      }
    }
  }

  private void prepareRightParts(String[] rightParts) {
    for (int i=0; i<rightParts.length; i++) {
      rightParts[i] = rightParts[i].replace("\\:", ":").replace("*", "%");
    }
  }

  public Parameter getParameter() {
    throw new UnsupportedOperationException();
  }

  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    throw new UnsupportedOperationException();
  }

  public String getSelectCountString() throws XNWH_InvalidSelectStatementException {
    throw new UnsupportedOperationException();
  }

  public boolean containsColumn(Object column) {
    return selectedColumns.contains(column);
  }

  public <T extends Storable<?>> ResultSetReader<T> getReader(Class<T> storableClass) {
    throw new UnsupportedOperationException();
  }
  
  private Set<BatchProcessColumn> selectedColumns = EnumSet.noneOf(BatchProcessColumn.class);

  public void selectConstantInput() {
    selectedColumns.add(BatchProcessColumn.CONSTANT_INPUT);
  }

  
}
