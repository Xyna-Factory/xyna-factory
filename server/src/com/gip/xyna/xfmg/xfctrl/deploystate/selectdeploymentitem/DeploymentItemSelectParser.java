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
package com.gip.xyna.xfmg.xfctrl.deploystate.selectdeploymentitem;

import java.util.Map;
import java.util.Set;

import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;


public class DeploymentItemSelectParser extends SelectionParser<DeploymentItemWhereClausesContainerImpl> {

  DeploymentItemSelectImpl dis;
  
  
  @Override
  protected WhereClause<DeploymentItemWhereClausesContainerImpl> retrieveColumnWhereClause(String column,
                                                                                           DeploymentItemWhereClausesContainerImpl whereClause) {
    throw new UnsupportedOperationException("retrieveColumnWhereClause is not supported");
  }

  @Override
  protected DeploymentItemWhereClausesContainerImpl newWhereClauseContainer() {
    return getSelectImpl().newWC();
  }

  @Override
  protected void parseSelectInternally(String select) {
    DeploymentItemSelect dis = getSelectImpl();
    if (select.equals("*")) {
      for (DeploymentItemColumn dic : DeploymentItemColumn.values()) {
        dis.select(dic);
      }
    } else {
      DeploymentItemColumn dic = DeploymentItemColumn.getColumnByName(select.trim());
      dis.select(dic);
    }
  }

  @Override
  protected WhereClausesConnection<DeploymentItemWhereClausesContainerImpl> parseFilterInternally(Map<String, String> filters)
                  throws XNWH_WhereClauseBuildException {
    return null;
  }

  @Override
  public DeploymentItemSelectImpl getSelectImpl() {
    if (dis == null) {
      dis = new DeploymentItemSelectImpl();
    }
    return dis;
  }

  @Override
  //returns null, like parseFilterInternally
  protected Set<WhereClausesConnection<DeploymentItemWhereClausesContainerImpl>> getWhereClausesConnections() {
    return null;
  }

}
