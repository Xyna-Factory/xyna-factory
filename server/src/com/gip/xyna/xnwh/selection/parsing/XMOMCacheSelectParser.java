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

import java.util.List;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.WhereClausesContainerImpl;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;



public class XMOMCacheSelectParser extends DefaultSelectionParser<WhereClausesContainerImpl> {

  private final static Logger logger = CentralFactoryLogging.getLogger(XMOMCacheSelectParser.class);
  
  XMOMDatabaseSelect xcs;


  public XMOMDatabaseSelect getXMOMCacheSelect() {
    if (xcs == null) {
      xcs = new XMOMDatabaseSelect();
    }
    return xcs;
  }
  
  @Override
  public XMOMDatabaseSelect getSelectImpl() {
    if (xcs == null) {
      xcs = new XMOMDatabaseSelect();
    }
    return xcs;
  }


  @Override
  protected WhereClausesContainerImpl newWhereClauseContainer() {
    return getXMOMCacheSelect().newWhereClause();
  }


  @Override
  protected void parseSelectInternally(String select) {
    try {
      XMOMDatabaseEntryColumn column = XMOMDatabaseEntryColumn.getXMOMColumnByName(select);
      XMOMDatabaseSelect xcs = getXMOMCacheSelect();
      xcs.select(column);
    } catch (IllegalArgumentException e) {
      return;
    }
  }



  @Override
  protected WhereClause<WhereClausesContainerImpl> retrieveColumnWhereClause(String columnName,
                                                                             WhereClausesContainerImpl whereClause) {
    XMOMDatabaseEntryColumn column = XMOMDatabaseEntryColumn.getXMOMColumnByName(columnName);
    return whereClause.where(column);
  }


  @Override
  protected WhereClausesConnection<WhereClausesContainerImpl> parseFilterFor(String filterString,
                                                                           String columnName,
                                                                           WhereClausesConnection<WhereClausesContainerImpl> previousConnection)
                  throws XNWH_WhereClauseBuildException {
    if (filterString != null && !filterString.equals("")) {
      WhereClausesContainerImpl whereClause = getXMOMCacheSelect();
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


  @Override
  protected String getColumnName(String filterEntryKey, String defaultName) {
    try {
      XMOMDatabaseEntryColumn xmomCacheColumn = XMOMDatabaseEntryColumn.getXMOMColumnByName(filterEntryKey);
      defaultName = xmomCacheColumn.getColumnName();
    } catch (Exception e) {
      logger.error("Invalid filtertype received", e);
    }
    return defaultName;
  }


}
