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
package com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem;

import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.selection.parsing.Selection;


public class DeploymentItemSelectImpl extends DeploymentItemWhereClausesContainerImpl implements DeploymentItemSelect, Selection{

  private Set<DeploymentItemColumn> selected;
  
  public DeploymentItemSelectImpl() {
    selected = new HashSet<DeploymentItemColumn>();
    selected.add(DeploymentItemColumn.FQNAME);
    selected.add(DeploymentItemColumn.TYPE);
  }
  
  public Parameter getParameter() {
    throw new UnsupportedOperationException("getParameter is not supported");
  }

  public String getSelectString() throws XNWH_InvalidSelectStatementException {
    throw new UnsupportedOperationException("getSelectString is not supported");
  }

  public String getSelectCountString() throws XNWH_InvalidSelectStatementException {
    throw new UnsupportedOperationException("getSelectCountString is not supported");
  }

  public boolean containsColumn(Object column) {
    return selected.contains(column);
  }

  public <T extends Storable<?>> ResultSetReader<T> getReader(Class<T> storableClass) {
    throw new UnsupportedOperationException("getReader is not supported");
  }

  public DeploymentItemWhereClausesContainerImpl newWC() {
    return new DeploymentItemWhereClausesContainerImpl();
  }

  public DeploymentItemSelect select(DeploymentItemColumn column) {
    selected.add(column);
    return this;
  }
}
