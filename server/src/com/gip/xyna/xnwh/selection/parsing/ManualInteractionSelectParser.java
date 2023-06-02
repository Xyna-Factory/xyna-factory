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

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xprc.xpce.manualinteraction.ManualInteractionColumn;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.ManualInteractionSelect;
import com.gip.xyna.xprc.xpce.manualinteraction.selectmi.WhereClausesContainerImpl;


public class ManualInteractionSelectParser extends DefaultSelectionParser<WhereClausesContainerImpl> {

    private final static Logger logger = CentralFactoryLogging.getLogger(ManualInteractionSelectParser.class);

    ManualInteractionSelect mis;
    
    public ManualInteractionSelect getMIS() {
      if (mis == null) {
        mis = new ManualInteractionSelect();
      }
      return mis;
    }
    
    @Override
    public ManualInteractionSelect getSelectImpl() {
      if (mis == null) {
        mis = new ManualInteractionSelect();
      }
      return mis;
    }


    @Override
    protected void parseSelectInternally(String select) {
      ManualInteractionSelect mis = getMIS();
      ManualInteractionColumn mic = ManualInteractionColumn.valueOf(select.trim());
      switch (mic) {
        case ID :
          mis.selectId();
          break;
        case reason :
          mis.selectReason();
          break;
        case result :
          mis.selectResult();
          break;
        case todo :
          mis.selectTodo();
          break;
        case type :
          mis.selectType();
          break;
        case userGroup :
          mis.selectUsergroup();
          break;
        case allowedResponses :
          mis.selectAllowedResponses();
          break;
        case xynaorder :
          mis.selectXynaOrder();
          break;
        default :
          //throw new IllegalArgumentException("Unknown OrderInstanceColumn " + oic.toString());
          break;
      }

    }


    @Override
    protected WhereClausesConnection<WhereClausesContainerImpl> parseFilterFor(
                                                                             String filterString,
                                                                             String columnName,
                                                                             WhereClausesConnection<WhereClausesContainerImpl> previousConnection)
                    throws XNWH_WhereClauseBuildException {
      if (filterString != null && !filterString.equals("")) {
        WhereClausesContainerImpl whereClause = getMIS();
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
    protected final WhereClause<WhereClausesContainerImpl> retrieveColumnWhereClause(String columnAsString,
                                                                                     WhereClausesContainerImpl where) {
      ManualInteractionColumn mic = ManualInteractionColumn.valueOf(columnAsString);
      switch (mic) {
        case ID :
          return where.whereId();
        case reason :
          return where.whereReason();
        case todo :
          return where.whereTodo();
        case type :
          return where.whereType();
        case userGroup :
          return where.whereUsergroup();

        default :
          return null;
      }
    }


    @Override
    protected WhereClausesContainerImpl newWhereClauseContainer() {
      return getMIS().newWC();
    }


    @Override
    protected String getColumnName(String filterEntryKey, String defaultName) {
      try {
        ManualInteractionColumn miColumn = ManualInteractionColumn.valueOf(filterEntryKey);
        defaultName = miColumn.toString();
      } catch (Exception e) {
        logger.error("Invalid filtertype received", e);
      }
      return defaultName;
    }

}
