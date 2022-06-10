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
package com.gip.xyna.xprc.xsched.selectvetos;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;




public class VetoSelectImpl extends WhereClausesContainerImpl implements Selection, Serializable, VetoSelect {

  private static final long serialVersionUID = 762195134074810249L;

  private List<VetoColumn> selected;


  public VetoSelectImpl() {
    super();
    selected = new ArrayList<VetoColumn>();
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    if (selected.size() == 0) {
      throw new XNWH_NoSelectGivenException();
    }
    for (int i = 0; i < selected.size() - 1; i++) {
      sb.append(selected.get(i).getColumnName()).append(", ");
    }
    sb.append(selected.get(selected.size() - 1).getColumnName());

    sb.append(" from ").append(VetoInformationStorable.TABLE_NAME);
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
    sb.append(VetoInformationStorable.TABLE_NAME);
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }
    return sb.toString() + super.getSelectString();
  }


  public WhereClausesContainerImpl newWC() {
    return new WhereClausesContainerImpl();
  }


  public ResultSetReader<VetoInformationStorable> getReader() {
    return new VetoInformationStorable.DynamicVetoReader(selected);
  }



  public VetoSelect selectVetoName() {
    if(!selected.contains(VetoColumn.VETONAME)) {
      selected.add(VetoColumn.VETONAME);
    }
    return this;
  }


  public VetoSelect selectUsingOrderId() {
    if(!selected.contains(VetoColumn.USINGORDERID)) {
      selected.add(VetoColumn.USINGORDERID);
    }
    return this;
  }

  public VetoSelect selectUsingRootOrderId() {
    if(!selected.contains(VetoColumn.USINGROOTORDERID)) {
      selected.add(VetoColumn.USINGROOTORDERID);
    }
    return this;
  }

  public VetoSelect selectUsingOrdertype() {
    if(!selected.contains(VetoColumn.USINGORDERTYPE)) {
      selected.add(VetoColumn.USINGORDERTYPE);
    }
    return this;
  }


  public VetoSelect selectDocumentation() {
    if(!selected.contains(VetoColumn.DOCUMENTATION)) {
      selected.add(VetoColumn.DOCUMENTATION);
    }
    return this;
  }


  public VetoSelect select(VetoColumn column) {
    if(!selected.contains(column)) {
      selected.add(column);
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
