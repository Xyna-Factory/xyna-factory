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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.selectdatamodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelStorable;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.storables.DataModelStorable.DynamicDataModelReader;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;

public class DataModelSelectImpl extends DataModelWhereClausesContainerImpl implements Serializable, DataModelSelect, Selection {

  private static final long serialVersionUID = 762195134074810249L;

  private Set<DataModelColumn> selected;


  public DataModelSelectImpl() {
    super();
    selected = new HashSet<DataModelColumn>();
    selected.add(DataModelColumn.FQNAME);
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    if (selected.size() == 0) {
      throw new XNWH_NoSelectGivenException();
    }
    Iterator<DataModelColumn> iter = selected.iterator();
    while(iter.hasNext()) {
      sb.append(iter.next().getColumnName());
      if(iter.hasNext()) {
        sb.append(", ");
      }
    }
    sb.append(" from ").append(DataModelStorable.TABLENAME);
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
    sb.append(DataModelStorable.TABLENAME);
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }
    return sb.toString() + super.getSelectString();
  }


  public DataModelWhereClausesContainerImpl newWC() {
    return new DataModelWhereClausesContainerImpl();
  }


  public ResultSetReader<DataModelStorable> getReader() {
    return new DynamicDataModelReader(selected);
  }  
  
  public DataModelSelect selectFqName() {
    selected.add(DataModelColumn.FQNAME);
    return this;
  }

  public DataModelSelect selectLabelName() {
    selected.add(DataModelColumn.LABEL);
    return this;
  }

  public DataModelSelect selectBaseTypeFqName() {
    selected.add(DataModelColumn.BASETYPEFQNAME);
    return this;
  }
  
  public DataModelSelect selectBaseTypeLabel() {
    selected.add(DataModelColumn.BASETYPELABEL);
    return this;
  }
  
  public DataModelSelect selectDataModelType() {
    selected.add(DataModelColumn.DATAMODELTYPE);
    return this;
  }
  
  public DataModelSelect selectDataModelPrefix() {
    selected.add(DataModelColumn.DATAMODELPREFIX);
    return this;
  }
  
  public DataModelSelect selectVersion() {
    selected.add(DataModelColumn.VERSION);
    return this;
  }
  
  public DataModelSelect selectDocumentation() {
    selected.add(DataModelColumn.DOCUMENTATION);
    return this;
  }
  
  public DataModelSelect selectParameter() {
    selected.add(DataModelColumn.PARAMETER);
    return this;
  }

  public DataModelSelect select(DataModelColumn column) {
    selected.add(column);
    return this;
  }
  
  public boolean selectedContains(DataModelColumn column) {
    return selected.contains(column);
  }

  public boolean containsColumn(Object column) {
    return selected.contains(column);
  }
  
  @SuppressWarnings("unchecked")
  public <T extends Storable<?>> ResultSetReader<T> getReader(Class<T> storableClass) {
    return (ResultSetReader<T>) getReader();
  }
  
}
