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
package com.gip.xyna.xnwh.selection.parsing;

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskInfoColumn;
import com.gip.xyna.xprc.xfqctrl.search.FrequencyControlledTaskSelect;
import com.gip.xyna.xprc.xfqctrl.search.WhereClausesContainerImpl;



public class FrequencyControlledTaskSelectParser extends DefaultSelectionParser<WhereClausesContainerImpl> {

  private final static Logger logger = CentralFactoryLogging.getLogger(FrequencyControlledTaskSelectParser.class);
  
  FrequencyControlledTaskSelect fcts;


  public FrequencyControlledTaskSelect getFrequencyControlledTaskSelect() {
    if (fcts == null) {
      fcts = new FrequencyControlledTaskSelect();
    }
    return fcts;
  }
  
  @Override
  public FrequencyControlledTaskSelect getSelectImpl() {
    if (fcts == null) {
      fcts = new FrequencyControlledTaskSelect();
    }
    return fcts;
  }


  @Override
  protected WhereClausesContainerImpl newWhereClauseContainer() {
    return getFrequencyControlledTaskSelect().newWhereClause();
  }


  @Override
  protected void parseSelectInternally(String select) {
    try {
      FrequencyControlledTaskInfoColumn column = FrequencyControlledTaskInfoColumn.getByColumnName(select);
      FrequencyControlledTaskSelect us = getFrequencyControlledTaskSelect();
      us.select(column);
    } catch (IllegalArgumentException e) {
      return;
    }
  }



  @Override
  protected WhereClause<WhereClausesContainerImpl> retrieveColumnWhereClause(String columnName,
                                                                             WhereClausesContainerImpl whereClause) {
    FrequencyControlledTaskInfoColumn column = FrequencyControlledTaskInfoColumn.getByColumnName(columnName);
    return whereClause.where(column);
  }


  @Override
  protected WhereClausesConnection<WhereClausesContainerImpl> parseFilterFor(String filterString,
                                                                           String columnName,
                                                                           WhereClausesConnection<WhereClausesContainerImpl> previousConnection)
                  throws XNWH_WhereClauseBuildException {
    if (filterString != null && !filterString.equals("")) {
      WhereClausesContainerImpl whereClause = getFrequencyControlledTaskSelect();
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
      FrequencyControlledTaskInfoColumn fqTaskColumn = FrequencyControlledTaskInfoColumn.getByColumnName(filterEntryKey);
      defaultName = fqTaskColumn.getColumnName();
    } catch (Exception e) {
      logger.error("Invalid filtertype received", e);
    }
    return defaultName;
  }


}
