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
import com.gip.xyna.xprc.xsched.selectvetos.VetoColumn;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelect;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;
import com.gip.xyna.xprc.xsched.selectvetos.WhereClausesContainerImpl;


public class VetoSelectParser extends DefaultSelectionParser<WhereClausesContainerImpl> {

    private final static Logger logger = CentralFactoryLogging.getLogger(VetoSelectParser.class);

    VetoSelectImpl vs;
    
    public VetoSelectImpl getVS() {
      if (vs == null) {
        vs = new VetoSelectImpl();
      }
      return vs;
    }
    
    @Override
    public VetoSelectImpl getSelectImpl() {
      if (vs == null) {
        vs = new VetoSelectImpl();
      }
      return vs;
    }


    @Override
    protected void parseSelectInternally(String select) {
      VetoSelect vs = getVS();
      VetoColumn vc = VetoColumn.getVetoColumnByName(select.trim());
      switch (vc) {
        case VETONAME :
          vs.selectVetoName();
          break;
        case USINGORDERID :
          vs.selectUsingOrderId();
          break;
        case USINGORDERTYPE :
          vs.selectUsingOrdertype();
          break;
        case DOCUMENTATION :
          vs.selectDocumentation();
          break;
        default :
          //throw new IllegalArgumentException("Unknown OrderInstanceColumn " + oic.toString());
          break;
      }

    }
    

    protected WhereClausesConnection<WhereClausesContainerImpl> parseFilterFor(
                                                                             String filterString,
                                                                             String columnName,
                                                                             WhereClausesConnection<WhereClausesContainerImpl> previousConnection)
                    throws XNWH_WhereClauseBuildException {
      if (filterString != null && !filterString.equals("")) {
        WhereClausesContainerImpl whereClause = getVS();
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


    protected final WhereClause<WhereClausesContainerImpl> retrieveColumnWhereClause(String columnAsString,
                                                                                     WhereClausesContainerImpl where) {
      VetoColumn vc = VetoColumn.getVetoColumnByName(columnAsString);
      switch (vc) {
        case VETONAME :
          return where.whereVetoName();
        case USINGORDERID :
          return where.whereUsingOrderId();
        case USINGORDERTYPE :
          return where.whereUsingOrderType();
        case DOCUMENTATION :
          return where.whereDocumentation();

        default :
          return null;
      }
    }


    @Override
    protected WhereClausesContainerImpl newWhereClauseContainer() {
      return getVS().newWC();
    }
    

    @Override
    protected String getColumnName(String filterEntryKey, String defaultName) {
      try {
        VetoColumn vColumn = VetoColumn.getVetoColumnByName(filterEntryKey);
        defaultName = vColumn.getColumnName();
      } catch (Exception e) {
        logger.error("Invalid filtertype received", e);
      }
      
      return defaultName;
    }
    
}
