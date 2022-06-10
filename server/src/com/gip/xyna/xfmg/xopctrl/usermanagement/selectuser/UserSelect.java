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
package com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User.DynamicUserReader;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;


public class UserSelect extends WhereClausesContainerImpl implements Selection, IUserSelect, Serializable {

  private static final long serialVersionUID = 7097366216015138467L;
  
  private List<UserColumns> selected;
  
  public UserSelect() {
    super();
    selected = new ArrayList<UserColumns>();
  }
  
  
  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    sb.append("select ");
    if (selected.size() == 0) {
      throw new XNWH_NoSelectGivenException();
    }
    for (int i = 0; i < selected.size() - 1; i++) {
      sb.append(selected.get(i).toString()).append(", ");
    }
    sb.append(selected.get(selected.size() - 1).toString()).append(" from ").append(User.TABLENAME);
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
    sb.append(User.TABLENAME);
    if (getWhereClauses().size() > 0) {
      sb.append(" where");
    }
    return sb.toString() + super.getSelectString();
  }
  
  
  public WhereClausesContainerImpl newWC() {
    return new WhereClausesContainerImpl();
  }
 
  
  public ResultSetReader<User> getReader() {
    return new DynamicUserReader(selected);
  }
  
  
  public UserSelect selectCreationDate() {
    selected.add(UserColumns.creationDate);
    return this;
  }


  public UserSelect selectId() {
    selected.add(UserColumns.name);
    return this;
  }


  public UserSelect selectLocked() {
    selected.add(UserColumns.locked);
    return this;
  }
  
  
  public UserSelect selectRole() {
    selected.add(UserColumns.role);
    return this;
  }


  public UserSelect selectPassword() {
    selected.add(UserColumns.password);
    return this;
  }
  
  
  public UserSelect selectAuthmode() {
    selected.add(UserColumns.authmode);
    return this;
  }
  
  
  public UserSelect selectDomains() {
    selected.add(UserColumns.domains);
    return this;
  }
  
  
  public UserSelect selectDescription() {
    selected.add(UserColumns.description);
    return this;
  }

  
  public UserSelect selectAllForUser() {
    selectId().selectCreationDate().selectLocked().selectPassword().selectRole().selectAuthmode().selectDomains().selectDescription();
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
