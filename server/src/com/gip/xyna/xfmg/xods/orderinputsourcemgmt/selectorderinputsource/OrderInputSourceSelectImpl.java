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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt.selectorderinputsource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable.DynamicOrderInputGeneratorReader;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;


public class OrderInputSourceSelectImpl extends OrderInputSourceWhereClausesContainerImpl implements Serializable, OrderInputSourceSelect, Selection {

  private static final long serialVersionUID = 1L;

  private Set<OrderInputSourceColumn> selected;
  
  
  public OrderInputSourceSelectImpl() {
    super();
    selected = new HashSet<OrderInputSourceColumn>();
  }
  
  
  public Parameter getParameter() {
    List<Object> list = new ArrayList<Object>();
    super.addParameter(list);
    Parameter paras = new Parameter(list.toArray());
    return paras;
  }

  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    if (selected.size() == 0) {
      throw new XNWH_NoSelectGivenException();
    }
    Iterator<OrderInputSourceColumn> iter = selected.iterator();
    while(iter.hasNext()) {
      sb.append(iter.next().getColumnName());
      if(iter.hasNext()) {
        sb.append(", ");
      }
    }
    sb.append(" from ").append(OrderInputSourceStorable.TABLENAME);
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }
    return sb.toString() + super.getSelectString();
  }

  public String getSelectCountString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select count(*) from ");
    sb.append(OrderInputSourceStorable.TABLENAME);
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }
    return sb.toString() + super.getSelectString();
  }

  public boolean containsColumn(Object column) {
    return selected.contains(column);
  }

  public OrderInputSourceWhereClausesContainerImpl newWC() {
    return new OrderInputSourceWhereClausesContainerImpl();
  }
  
  
  public ResultSetReader<OrderInputSourceStorable> getReader() {
    return new DynamicOrderInputGeneratorReader(selected);
  }

  @SuppressWarnings("unchecked")
  public <T extends Storable<?>> ResultSetReader<T> getReader(Class<T> storableClass) {
    return (ResultSetReader<T>) getReader();
  }
  
  
  public OrderInputSourceSelect selectId() {
    selected.add(OrderInputSourceColumn.ID);
    return this;
  }

  public OrderInputSourceSelect selectName() {
    selected.add(OrderInputSourceColumn.NAME);
    return this;
  }

  public OrderInputSourceSelect selectType() {
    selected.add(OrderInputSourceColumn.TYPE);
    return this;
  }

  public OrderInputSourceSelect selectOrderType() {
    selected.add(OrderInputSourceColumn.ORDERTYPE);
    return this;
  }

  public OrderInputSourceSelect selectApplicationName() {
    selected.add(OrderInputSourceColumn.APPLICATIONNAME);
    return this;
  }

  public OrderInputSourceSelect selectVersionName() {
    selected.add(OrderInputSourceColumn.VERSIONNAME);
    return this;
  }

  public OrderInputSourceSelect selectWorkspaceName() {
    selected.add(OrderInputSourceColumn.WORKSPACENAME);
    return this;
  }

  public OrderInputSourceSelect selectDocumentation() {
    selected.add(OrderInputSourceColumn.DOCUMENTATION);
    return this;
  }

  public OrderInputSourceSelect selectParameter() {
    selected.add(OrderInputSourceColumn.PARAMETER);
    return this;
  }

  public OrderInputSourceSelect select(OrderInputSourceColumn column) {
    selected.add(column);
    return this;
  }

}
