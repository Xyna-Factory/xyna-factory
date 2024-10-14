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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.selectdatamodel;

import java.util.List;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xnwh.selection.parsing.DefaultSelectionParser;

public class DataModelSelectParser extends DefaultSelectionParser<DataModelWhereClausesContainerImpl > {

    private final static Logger logger = CentralFactoryLogging.getLogger(DataModelSelectParser.class);

    DataModelSelectImpl dms;
    
    @Override
    public DataModelSelectImpl getSelectImpl() {
      if (dms == null) {
        dms = new DataModelSelectImpl();
      }
      return dms;
    }

    @Override
    protected void parseSelectInternally(String select) {
      DataModelSelect dms = getSelectImpl();
      if( select.equals("*") ) {
        for( DataModelColumn dmc : DataModelColumn.values() ) {
          dms.select(dmc);
        }
      } else {
        DataModelColumn dmc = DataModelColumn.getColumnByName(select.trim());     
        dms.select(dmc);
      }
    }


    @Override
    protected WhereClausesConnection<DataModelWhereClausesContainerImpl> parseFilterFor(
                                                                             String filterString,
                                                                             String columnName,
                                                                             WhereClausesConnection<DataModelWhereClausesContainerImpl> previousConnection) 
                                                                                 throws XNWH_WhereClauseBuildException
                     {
      if (filterString != null && !filterString.equals("")) {
        DataModelWhereClausesContainerImpl whereClause = getSelectImpl();
        if (previousConnection != null) {
          whereClause = previousConnection.and();
        }
        List<String> tokens = convertIntoTokens(filterString);
        WhereClausesConnection<DataModelWhereClausesContainerImpl> lastConnection = parseTokens(tokens, columnName, whereClause);
        tokens.clear();
        return lastConnection;
      }
      if (previousConnection != null) {
        return previousConnection;
      } else {
        return null;
      }
    }


    protected final WhereClause<DataModelWhereClausesContainerImpl> retrieveColumnWhereClause(String columnAsString,
                                                                                     DataModelWhereClausesContainerImpl where) {
      DataModelColumn dmc = DataModelColumn.getColumnByName(columnAsString);
      switch (dmc) {
        case LABEL :
          return where.whereLabel();
       
        default :
          return null;
      }
    }


    @Override
    protected DataModelWhereClausesContainerImpl newWhereClauseContainer() {
      return getSelectImpl().newWC();
    }


    @Override
    protected String getColumnName(String filterEntryKey, String defaultName) {
      try {
        DataModelColumn cloColumn = DataModelColumn.getColumnByName(filterEntryKey);
        defaultName = cloColumn.getColumnName();
      } catch (Exception e) {
        logger.error("Invalid filtertype received", e);
      }
      return defaultName;
    }
    
}
