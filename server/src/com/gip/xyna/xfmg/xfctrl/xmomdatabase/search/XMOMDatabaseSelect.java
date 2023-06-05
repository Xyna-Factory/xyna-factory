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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;


public class XMOMDatabaseSelect extends WhereClausesContainerImpl implements Selection, Serializable, IXMOMDatabaseSelect {

  private static final long serialVersionUID = -2778715338567128995L;

  public static String ARCHIVEPLACEHOLDER = "ArchiveContextPlaceholder";

  private Set<XMOMDatabaseEntryColumn> selected;
  private Set<XMOMDatabaseType> desiredResultTypes;
  private boolean selectedAll;


  public XMOMDatabaseSelect() {
    selected = new HashSet<XMOMDatabaseEntryColumn>();
    select(XMOMDatabaseEntryColumn.FQNAME);
    select(XMOMDatabaseEntryColumn.NAME);
    select(XMOMDatabaseEntryColumn.PATH);
    select(XMOMDatabaseEntryColumn.METADATA);
    select(XMOMDatabaseEntryColumn.REVISION);
    desiredResultTypes = new HashSet<XMOMDatabaseType>();
    selectedAll = false;
  }


  public void selectAll() {
    for (XMOMDatabaseEntryColumn column : XMOMDatabaseEntryColumn.values()) {
      if (!selected.contains(column)) {
        selected.add(column);
      }
    }
    selectedAll = true;
  }


  public XMOMDatabaseSelect select(XMOMDatabaseEntryColumn column) {
    selected.add(column);
    return this;
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {

    if (selected.size() == 0) {
      throw new XNWH_NoSelectGivenException();
    }

    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    if(selectedAll) {
      sb.append("* ");
    } else {
      Iterator<XMOMDatabaseEntryColumn> columnIter = selected.iterator();
      while (columnIter.hasNext()) {
        XMOMDatabaseEntryColumn current = columnIter.next();
        sb.append(current.getColumnName());
        if (columnIter.hasNext()) {
          sb.append(",");
        }
      }
    }
    sb.append(" from ").append(XMOMDatabaseSelect.ARCHIVEPLACEHOLDER);
    if (getWhereClauses().size() > 0) {
      sb.append(" where ");
      sb.append(super.getSelectString());
    }
    return sb.toString();

  }


  public String getSelectCountString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select count(*) from ");
    sb.append(XMOMDatabaseSelect.ARCHIVEPLACEHOLDER);
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
      sb.append(super.getSelectString());
    }
    return sb.toString();
  }


  public WhereClausesContainerImpl newWhereClause() {
    return new WhereClausesContainerImpl();
  }


  public Parameter getParameter() {
    List<Object> list = new ArrayList<Object>();
    super.addParameter(list);
    Parameter paras = new Parameter(list.toArray());
    return paras;
  }
  
  
  public Set<XMOMDatabaseEntryColumn> getSelection() {
    return selected;
  }
  
  
  public Set<XMOMDatabaseType> getDesiredResultTypes() {
    return desiredResultTypes;
  }
  
  
  public void addDesiredResultTypes(XMOMDatabaseType newType) {
    desiredResultTypes.add(newType);
  }
  
  public void addAllDesiredResultTypes(Collection<XMOMDatabaseType> newTypes) {
    desiredResultTypes.addAll(newTypes);
  }


  
  public boolean isSelectedAll() {
    return selectedAll;
  }
  
  public boolean containsColumn(Object column) {
    return selected.contains(column);
  }
  
  public <T extends Storable<?>> ResultSetReader<T> getReader(Class<T> storableClass) {
    throw new UnsupportedOperationException();
    //return (ResultSetReader<T>) getReader();
  }

}
