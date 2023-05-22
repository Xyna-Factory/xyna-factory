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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder;
import com.gip.xyna.xprc.xsched.cronlikescheduling.CronLikeOrder.DynamicCronLikeOrderReader;




public class CronLikeOrderSelectImpl extends CronLikeOrderWhereClausesContainerImpl implements Selection, Serializable, CronLikeOrderSelect {

  private static final long serialVersionUID = 762195134074810249L;

  private Set<CronLikeOrderColumn> selected;


  public CronLikeOrderSelectImpl() {
    super();
    selected = new HashSet<CronLikeOrderColumn>();
    selected.add(CronLikeOrderColumn.ID);
    selected.add(CronLikeOrderColumn.CREATIONPARAMETER);
  }


  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    if (selected.size() == 0) {
       throw new XNWH_NoSelectGivenException();
    }
    Iterator<CronLikeOrderColumn> iter = selected.iterator();
    while(iter.hasNext()) {
      sb.append(iter.next().getColumnName());
      if(iter.hasNext()) {
        sb.append(", ");
      }
    }
    sb.append(" from ").append(CronLikeOrder.TABLE_NAME);
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
    sb.append(CronLikeOrder.TABLE_NAME);
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }
    return sb.toString() + super.getSelectString();
  }


  public CronLikeOrderWhereClausesContainerImpl newWC() {
    return new CronLikeOrderWhereClausesContainerImpl();
  }


  public ResultSetReader<CronLikeOrder> getReader() {
    return new DynamicCronLikeOrderReader(selected);
  }

  
  

  public CronLikeOrderSelect selectId() {
    selected.add(CronLikeOrderColumn.ID);
    return this;
  }


  public CronLikeOrderSelect selectLabelName() {
    selected.add(CronLikeOrderColumn.LABEL);
    return this;
  }


  public CronLikeOrderSelect selectOrdertype() {
    selected.add(CronLikeOrderColumn.ORDERTYPE);
    return this;
  }


  public CronLikeOrderSelect selectStarttime() {
    selected.add(CronLikeOrderColumn.STARTTIME);
    return this;
  }

  
  public CronLikeOrderSelect selectNextExecution() {
    selected.add(CronLikeOrderColumn.NEXTEXECUTION);
    return this;
  }
  

  public CronLikeOrderSelect selectInterval() {
    selected.add(CronLikeOrderColumn.INTERVAL);
    return this;
  }


  public CronLikeOrderSelect selectStatus() {
    selected.add(CronLikeOrderColumn.STATUS);
    return this;
  }


  public CronLikeOrderSelect selectOnError() {
    selected.add(CronLikeOrderColumn.ONERROR);
    return this;
  }


  public CronLikeOrderSelect selectApplicationName() {
    selected.add(CronLikeOrderColumn.APPLICATIONNAME);
    return this;
  }


  public CronLikeOrderSelect selectVersionName() {
    selected.add(CronLikeOrderColumn.VERSIONNAME);
    return this;
  }


  public CronLikeOrderSelect selectWorkspaceName() {
    selected.add(CronLikeOrderColumn.WORKSPACENAME);
    return this;
  }


  public CronLikeOrderSelect select(CronLikeOrderColumn column) {
    selected.add(column);
    return this;
  }


  public CronLikeOrderSelect selectEnabled() {
    selected.add(CronLikeOrderColumn.ENABLED);
    return this;
  }


  public CronLikeOrderSelect selectTimeZoneID() {
    selected.add(CronLikeOrderColumn.TIMEZONEID);
    return this;
  }


  public CronLikeOrderSelect selectConsiderDaylightSaving() {
    selected.add(CronLikeOrderColumn.CONSIDERDAYLIGHTSAVING);
    return this;
  }


  public CronLikeOrderSelect selectCustom0() {
    selected.add(CronLikeOrderColumn.CUSTOM0);
    return this;
  }


  public CronLikeOrderSelect selectCustom1() {
    selected.add(CronLikeOrderColumn.CUSTOM1);
    return this;
  }


  public CronLikeOrderSelect selectCustom2() {
    selected.add(CronLikeOrderColumn.CUSTOM2);
    return this;
  }


  public CronLikeOrderSelect selectCustom3() {
    selected.add(CronLikeOrderColumn.CUSTOM3);
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
