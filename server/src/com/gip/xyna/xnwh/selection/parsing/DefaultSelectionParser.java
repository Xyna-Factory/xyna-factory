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

package com.gip.xyna.xnwh.selection.parsing;



import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xnwh.selection.WhereClausesContainerBase;



public abstract class DefaultSelectionParser<P extends WhereClausesContainerBase<P>> extends SelectionParser<P> {

  private final static Logger logger = CentralFactoryLogging.getLogger(DefaultSelectionParser.class);

  protected Set<WhereClausesConnection<P>> whereClauses;


  @Override
  protected WhereClausesConnection<P> parseFilterInternally(Map<String, String> filters) throws XNWH_WhereClauseBuildException {
    whereClauses = new HashSet<WhereClausesConnection<P>>();
    WhereClausesConnection<P> previousConnection = null;
    WhereClausesConnection<P> connection = null;

    if (filters == null || filters.size() == 0) {
      return null;
    }

    Set<Entry<String, String>> filterEntries = filters.entrySet();

    for (Entry<String, String> entry : filterEntries) {
      if (connection != null) {
        previousConnection = connection;
      }
      String columnName = "unknown";
      if (entry.getKey() == null || entry.getKey().equals("")) {
        logger.warn("Untyped filtervalue received");
        continue;
      } else {
        columnName = getColumnName(entry.getKey(), columnName);
      }
      if (previousConnection == null) {
        previousConnection = parseFilterFor(entry.getValue(), columnName, null);
        whereClauses.add(previousConnection);
      } else {
        connection = parseFilterFor(entry.getValue(), columnName, previousConnection);
        whereClauses.add(connection);
      }
    }

    return connection;
  }


  protected abstract WhereClausesConnection<P> parseFilterFor(String filterString, String columnName,
                                                              WhereClausesConnection<P> previousConnection)
      throws XNWH_WhereClauseBuildException;


  protected abstract String getColumnName(String filterEntryKey, String defaultName);
  
  @Override
  protected Set<WhereClausesConnection<P>> getWhereClausesConnections() {
    return whereClauses;
  }
}
