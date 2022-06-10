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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt.selectorderinputsource;

import java.util.List;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xnwh.selection.parsing.DefaultSelectionParser;


public class OrderInputSourceSelectParser extends DefaultSelectionParser<OrderInputSourceWhereClausesContainerImpl> {

  private final static Logger logger = CentralFactoryLogging.getLogger(OrderInputSourceSelectParser.class);

  OrderInputSourceSelectImpl oigs;
  
  @Override
  protected WhereClause<OrderInputSourceWhereClausesContainerImpl> retrieveColumnWhereClause(String column,
                                                                                                OrderInputSourceWhereClausesContainerImpl whereClause) {
    OrderInputSourceColumn oigc = OrderInputSourceColumn.getColumnByName(column);
    switch (oigc) {
      case ID :
        return whereClause.whereId();
      case NAME :
        return whereClause.whereName();
      case TYPE :
        return whereClause.whereType();
      case ORDERTYPE :
        return whereClause.whereOrderType();
      case APPLICATIONNAME :
        return whereClause.whereApplicationName();
      case VERSIONNAME :
        return whereClause.whereVersionName();
      case WORKSPACENAME :
        return whereClause.whereWorkspaceName();
      case DOCUMENTATION :
        return whereClause.whereDocumentation();
      default :
        return null;
    }
  }

  @Override
  protected OrderInputSourceWhereClausesContainerImpl newWhereClauseContainer() {
    return getSelectImpl().newWC();
  }

  @Override
  protected void parseSelectInternally(String select) {
    OrderInputSourceSelect oigs = getSelectImpl();
    if(select.equals("*")) {
      for(OrderInputSourceColumn oigc : OrderInputSourceColumn.values()) {
        oigs.select(oigc);
      }
    } else {
      OrderInputSourceColumn oigc = OrderInputSourceColumn.getColumnByName(select.trim());
      oigs.select(oigc);
    }
  }

  
  @Override
  protected WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> parseFilterFor(
                                                                                  String filterString,
                                                                                  String columnName,
                                                                                  WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> previousConnection) 
                                                                                      throws XNWH_WhereClauseBuildException
                   {
    if (filterString != null && !filterString.equals("")) {
      OrderInputSourceWhereClausesContainerImpl whereClause = getSelectImpl();
      if (previousConnection != null) {
        whereClause = previousConnection.and();
      }
      List<String> tokens = convertIntoTokens(filterString);
      WhereClausesConnection<OrderInputSourceWhereClausesContainerImpl> lastConnection = parseTokens(tokens, columnName, whereClause);
      tokens.clear();
      return lastConnection;
    }
    if (previousConnection != null) {
      return previousConnection;
    } else {
      return null;
    }
  }
  
  
  @Override
  public OrderInputSourceSelectImpl getSelectImpl() {
    if (oigs == null) {
      oigs = new OrderInputSourceSelectImpl();
    }
    return oigs;
  }


  @Override
  protected String getColumnName(String filterEntryKey, String defaultName) {
    try {
      OrderInputSourceColumn oigColumn = OrderInputSourceColumn.getColumnByName(filterEntryKey);
      defaultName = oigColumn.getColumnName();
    } catch (Exception e) {
      logger.error("Invalid filtertype received", e);
    }
    return defaultName;
  }

}
