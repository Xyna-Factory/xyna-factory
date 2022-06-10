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
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserColumns;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserSelect;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.WhereClausesContainerImpl;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;



public class UserSelectParser extends DefaultSelectionParser<WhereClausesContainerImpl> {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(UserSelectParser.class);
  
  private List<String> tokens;
  
  UserSelect us;
  
  public UserSelect getUS() {
    if (us == null) {
      us = new UserSelect();
    }
    return us;
  }
  
  @Override
  public UserSelect getSelectImpl() {
    if (us == null) {
      us = new UserSelect();
    }
    return us;
  }
  

  @Override
  protected WhereClausesContainerImpl newWhereClauseContainer() {
    return getUS().newWC();
  }


  @Override
  protected void parseSelectInternally(String select) {
    UserSelect us = getUS();
    UserColumns uc = UserColumns.valueOf(select);
    switch (uc) {
      case name :
        us.selectId();
        break;
      case creationDate :
        us.selectCreationDate();
        break;
      case locked :
        us.selectLocked();
        break;
      case authmode :
        us.selectAuthmode();
        break;
      case domains :
        us.selectDomains();
        break;
      case description :
        us.selectDescription();
        break;
      case role :
        us.selectRole();
        break;
        
      default :
        break;
    }

  }



  @Override
  protected WhereClause<WhereClausesContainerImpl> retrieveColumnWhereClause(String columnName,
                                                                             WhereClausesContainerImpl whereClause) {
    UserColumns column = UserColumns.valueOf(columnName);
    switch (column) {
      case name :
        return whereClause.whereId();
      case creationDate :
        return whereClause.whereCreationDate();
      case role :
        return whereClause.whereRole();
      case domains :
        return whereClause.whereDomains();
      default :
        return null;
    }
  }
  
  @Override
  protected WhereClausesConnection<WhereClausesContainerImpl> parseFilterFor(String filterString,
                                                                           String columnName,
                                                                           WhereClausesConnection<WhereClausesContainerImpl> previousConnection)
                  throws XNWH_WhereClauseBuildException {
    if (filterString != null && !filterString.equals("")) {
      WhereClausesContainerImpl whereClause = getUS();
      if (previousConnection != null) {
        whereClause = previousConnection.and();
      }
      tokens = convertIntoTokens(filterString);
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
      UserColumns userColumn = UserColumns.valueOf(filterEntryKey);
      defaultName = userColumn.toString();
    } catch (Exception e) {
      logger.error("Invalid filtertype received", e);
    }
    return defaultName;
  }

}
