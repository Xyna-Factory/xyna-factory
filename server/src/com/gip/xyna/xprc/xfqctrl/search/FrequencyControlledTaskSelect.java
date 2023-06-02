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

package com.gip.xyna.xprc.xfqctrl.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskInformation.DynamicFrequencyControlledTaskInfoReader;


public class FrequencyControlledTaskSelect extends WhereClausesContainerImpl implements Selection, Serializable, IFrequencyControlledTaskSelect {

  private static final long serialVersionUID = -2778715338567128995L;


  private List<FrequencyControlledTaskInfoColumn> selected;


  public FrequencyControlledTaskSelect() {
    selected = new ArrayList<FrequencyControlledTaskInfoColumn>();
  }


  public void selectAll() {
    for (FrequencyControlledTaskInfoColumn column : FrequencyControlledTaskInfoColumn.values()) {
      if (!selected.contains(column)) {
        selected.add(column);
      }
    }
  }


  public FrequencyControlledTaskSelect selectId() {
    if (!selected.contains(FrequencyControlledTaskInfoColumn.ID)) {
      selected.add(FrequencyControlledTaskInfoColumn.ID);
    }
    return this;
  }


  public FrequencyControlledTaskSelect selectTaskLabel() {
    if (!selected.contains(FrequencyControlledTaskInfoColumn.TASK_LABEL)) {
      selected.add(FrequencyControlledTaskInfoColumn.TASK_LABEL);
    }
    return this;
  }


  public FrequencyControlledTaskSelect selectTaskStatus() {
    if (!selected.contains(FrequencyControlledTaskInfoColumn.STATUS)) {
      selected.add(FrequencyControlledTaskInfoColumn.STATUS);
    }
    return this;
  }


  public FrequencyControlledTaskSelect select(FrequencyControlledTaskInfoColumn column) {
    if (!selected.contains(column)) {
      selected.add(column);
    }
    return this;
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {

    if (selected.size() == 0) {
      throw new XNWH_NoSelectGivenException();
    }

    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    for (int i = 0; i < selected.size() - 1; i++) {
      sb.append(selected.get(i).getColumnName()).append(", ");
    }
    sb.append(selected.get(selected.size() - 1).getColumnName());

    sb.append(" from ").append(FrequencyControlledTaskInformation.TABLE_NAME);
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }

    sb.append(super.getSelectString());

    return sb.toString();

  }


  public String getSelectCountString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select count(*) from ");
    sb.append(FrequencyControlledTaskInformation.TABLE_NAME);
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }
    sb.append(super.getSelectString());
    return sb.toString();
  }


  public WhereClausesContainerImpl newWhereClause() {
    return new WhereClausesContainerImpl();
  }


  public ResultSetReader<FrequencyControlledTaskInformation> getDynamicReader() {
    return new DynamicFrequencyControlledTaskInfoReader(selected);
  }


  public Parameter getParameter() {
    List<Object> list = new ArrayList<Object>();
    super.addParameter(list);
    Parameter paras = new Parameter(list.toArray());
    return paras;
  }

  public boolean containsColumn(Object column) {
    return selected.contains(column);
  }

  @SuppressWarnings("unchecked")
  public <T extends Storable<?>> ResultSetReader<T> getReader(Class<T> storableClass) {
    return (ResultSetReader<T>) getDynamicReader();
  }

}
