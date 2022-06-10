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
package com.gip.xyna.xprc.xpce.manualinteraction.selectmi;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionColumn;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionEntry.DynamicManualInteractionReader;



/**
 * definition einer gueltigen suchabfrage an das orderarchive oder orderdb. beispiel: OrderInstanceSelect ois = new
 * OrderInstanceSelect();
 * ois.selectAllForOrderInstance().whereId().isBiggerThan(3).and().where(ois.newWC().whereOrderType().isEqual("ot"));
 */
public class ManualInteractionSelect extends WhereClausesContainerImpl implements Selection, Serializable, MiSelect {

  private static final long serialVersionUID = 762195134074810249L;

  private List<ManualInteractionColumn> selected;


  public ManualInteractionSelect() {
    super();
    selected = new ArrayList<ManualInteractionColumn>();
  }



  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    if (selected.size() == 0) {
      throw new XNWH_NoSelectGivenException();
    }
    for (int i = 0; i < selected.size() - 1; i++) {
      if (selected.get(i) == ManualInteractionColumn.xynaorder) {
        sb.append(ManualInteractionEntry.MI_COL_XYNAORDER_ID).append(", ");
        sb.append(ManualInteractionEntry.MI_COL_XYNAORDER_MON_LVL).append(", ");
        sb.append(ManualInteractionEntry.MI_COL_XYNAORDER_PARENT_ID).append(", ");
        sb.append(ManualInteractionEntry.MI_COL_XYNAORDER_PARENT_ORDERTYPE).append(", ");
        sb.append(ManualInteractionEntry.MI_COL_XYNAORDER_PRIORITY).append(", ");
        sb.append(ManualInteractionEntry.MI_COL_XYNAORDER_SESSION_ID).append(", ");
      } else {
        sb.append(selected.get(i).toString()).append(", ");
      }
    }
    sb.append(selected.get(selected.size() - 1).toString());

    sb.append(" from ").append(new ManualInteractionEntry().getTableName());
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }
    return sb.toString() + super.getSelectString();
  }


  public Parameter getParameter() {
    List<Object> list = new ArrayList<Object>();
    super.addParameter(list);
    Parameter paras = new Parameter(list.toArray());
    return paras;
  }


  public String getSelectCountString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select count(*) from ");
    sb.append(new ManualInteractionEntry().getTableName());
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }
    return sb.toString() + super.getSelectString();
  }


  public WhereClausesContainerImpl newWC() {
    return new WhereClausesContainerImpl();
  }


  public ResultSetReader<ManualInteractionEntry> getReader() {
    return new DynamicManualInteractionReader(selected);
  }


  public ManualInteractionSelect selectId() {
    if(!selected.contains(ManualInteractionColumn.ID)) {
      selected.add(ManualInteractionColumn.ID);
    }
    return this;
  }


  public ManualInteractionSelect selectTodo() {
    if(!selected.contains(ManualInteractionColumn.todo)) {
      selected.add(ManualInteractionColumn.todo);
    }
    return this;
  }


  public ManualInteractionSelect selectType() {
    if(!selected.contains(ManualInteractionColumn.type)) {
      selected.add(ManualInteractionColumn.type);
    }
    return this;
  }


  public ManualInteractionSelect selectReason() {
    if(!selected.contains(ManualInteractionColumn.reason)) {
      selected.add(ManualInteractionColumn.reason);
    }
    return this;
  }


  public ManualInteractionSelect selectUsergroup() {
    if(!selected.contains(ManualInteractionColumn.userGroup)) {
      selected.add(ManualInteractionColumn.userGroup);
    }
    return this;
  }


  public ManualInteractionSelect selectResult() {
    if(!selected.contains(ManualInteractionColumn.result)) {
      selected.add(ManualInteractionColumn.result);
    }
    return this;
  }


  public ManualInteractionSelect selectRevision() {
    if(!selected.contains(ManualInteractionColumn.revision)) {
      selected.add(ManualInteractionColumn.revision);
    }
    return this;
  }

  public ManualInteractionSelect selectXynaOrder() {
    if(!selected.contains(ManualInteractionColumn.xynaorder)) {
      selected.add(ManualInteractionColumn.xynaorder);
    }
    return this;
  }
  
  
  public ManualInteractionSelect selectAllowedResponses() {
    if(!selected.contains(ManualInteractionColumn.allowedResponses)) {
      selected.add(ManualInteractionColumn.allowedResponses);
    }
    return this;
  }

  public boolean containsColumn(Object column) {
    return selected.contains(column);
  }
  
  @SuppressWarnings("unchecked")
  public <T extends Storable<?>> ResultSetReader<T> getReader(Class<T> storableClass) {
    return (ResultSetReader<T>) getReader();
  }

}
