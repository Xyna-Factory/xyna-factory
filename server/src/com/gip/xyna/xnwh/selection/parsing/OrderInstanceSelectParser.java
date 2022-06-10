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
package com.gip.xyna.xnwh.selection.parsing;



import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceColumn;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.WhereClausesContainerImpl;



public class OrderInstanceSelectParser extends SelectionParser<WhereClausesContainerImpl> {

  private final static Logger logger = CentralFactoryLogging.getLogger(OrderInstanceSelectParser.class);

  OrderInstanceSelect ois;
  private Set<WhereClausesConnection<WhereClausesContainerImpl>> whereClauses;
  
  public OrderInstanceSelect getOIS() {
    if (ois == null) {
      ois = new OrderInstanceSelect();
    }
    return ois;
  }
  
  @Override
  public OrderInstanceSelect getSelectImpl() {
    if (ois == null) {
      ois = new OrderInstanceSelect();
    }
    return ois;
  }


  @Override
  protected void parseSelectInternally(String select) {
    try {
      OrderInstanceColumn column = OrderInstanceColumn.getByColumnName(select);
      OrderInstanceSelect ois = getOIS();
      ois.select(column);
    } catch (IllegalArgumentException e) {
      return;
    }
  }


  @Override
  protected final WhereClausesConnection<WhereClausesContainerImpl> parseFilterInternally(Map<String, String> filters)
                  throws XNWH_WhereClauseBuildException {

    whereClauses = new HashSet<WhereClausesConnection<WhereClausesContainerImpl>>();
    WhereClausesConnection<WhereClausesContainerImpl> previousConnection = null;
    WhereClausesConnection<WhereClausesContainerImpl> connection = null;

    if (filters == null || filters.size() == 0) {
      return null;
    }

    Set<Entry<String, String>> filterEntries = filters.entrySet();

    for (Entry<String, String> entry : filterEntries) {
      if (connection != null) {
        previousConnection = connection;
      }
      OrderInstanceColumn orderColumn = null;
      if (entry.getKey() == null || entry.getKey().equals("")) {
        logger.warn("Ignoring untyped filtervalue (OrderInstance Select).");
        continue;
      } else {
        try {
          orderColumn = OrderInstanceColumn.getByColumnName(entry.getKey());
        } catch (Throwable e) {
          Department.handleThrowable(e);
          logger.warn("Ignoring invalid filtertype (OrderInstance Select): " + entry.getKey(), e);
          continue;
        }
      }
      if (previousConnection == null) {
        previousConnection = parseFilterFor(entry.getValue(), orderColumn.getColumnName(), null);
        whereClauses.add(previousConnection);
      } else {
        connection = parseFilterFor(entry.getValue(), orderColumn.getColumnName(), previousConnection);
        whereClauses.add(connection);
      }
    }

    return connection;
  }


  private WhereClausesConnection<WhereClausesContainerImpl> parseFilterFor(String filterString,
                                                                           String columnName,
                                                                           WhereClausesConnection<WhereClausesContainerImpl> previousConnection)
                  throws XNWH_WhereClauseBuildException {
    if (filterString != null && !filterString.equals("")) {
      WhereClausesContainerImpl whereClause = getOIS();
      if (previousConnection != null) {
        whereClause = previousConnection.and();
      }
      List<String> tokens = convertIntoTokens(filterString);
      WhereClausesConnection<WhereClausesContainerImpl> lastConnection = parseTokens(tokens, columnName, whereClause);
      tokens.clear();
      return lastConnection;
    }
    if (previousConnection != null) {
      return previousConnection;
    } else {
      return null;
    }
  }


  protected final WhereClause<WhereClausesContainerImpl> retrieveColumnWhereClause(String columnName,
                                                                                   WhereClausesContainerImpl where) {
    OrderInstanceColumn column = OrderInstanceColumn.getByColumnName(columnName);
    return where.where(column);
  }


  @Override
  protected WhereClausesContainerImpl newWhereClauseContainer() {
    return getOIS().newWC();
  }


  @Override
  protected Set<WhereClausesConnection<WhereClausesContainerImpl>> getWhereClausesConnections() {
    return whereClauses;
  }

}
