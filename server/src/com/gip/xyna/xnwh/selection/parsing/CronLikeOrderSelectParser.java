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
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderColumn;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelect;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderSelectImpl;
import com.gip.xyna.xprc.xsched.cronlikescheduling.selectcrons.CronLikeOrderWhereClausesContainerImpl;


public class CronLikeOrderSelectParser extends DefaultSelectionParser<CronLikeOrderWhereClausesContainerImpl > {

    private final static Logger logger = CentralFactoryLogging.getLogger(CronLikeOrderSelectParser.class);

    CronLikeOrderSelectImpl clos;
    
    public CronLikeOrderSelectImpl getCLOS() {
      if (clos == null) {
        clos = new CronLikeOrderSelectImpl();
      }
      return clos;
    }
    
    @Override
    public CronLikeOrderSelectImpl getSelectImpl() {
      if (clos == null) {
        clos = new CronLikeOrderSelectImpl();
      }
      return clos;
    }


    @Override
    protected void parseSelectInternally(String select) {
      CronLikeOrderSelect clos = getCLOS();
      CronLikeOrderColumn cloc = CronLikeOrderColumn.getCLOColumnByName(select.trim());
      switch (cloc) {
        case APPLICATIONNAME :
          clos.selectApplicationName();
          break;
        case WORKSPACENAME :
          clos.selectWorkspaceName();
        case INTERVAL :
          clos.selectInterval();
          break;
        case LABEL :
          clos.selectLabelName();
          break;
        case ONERROR :
          clos.selectOnError();
          break;
        case ORDERTYPE :
          clos.selectOrdertype();
          break;
        case STARTTIME :
          clos.selectStarttime();
          break;
        case NEXTEXECUTION :
          clos.selectNextExecution();
          break;
        case STATUS :
          clos.selectStatus();
          break;
        case VERSIONNAME :
          clos.selectVersionName();
          break;
        case ID:
          clos.selectId();
          break;
        case CREATIONPARAMETER:
          break;
        case ENABLED:
          clos.selectEnabled();
          break;
        case TIMEZONEID :
          clos.selectTimeZoneID();
          break;
        case CONSIDERDAYLIGHTSAVING :
          clos.selectConsiderDaylightSaving();
          break;
        case CUSTOM0 :
          clos.selectCustom0();
          break;
        case CUSTOM1 :
          clos.selectCustom1();
          break;
        case CUSTOM2 :
          clos.selectCustom2();
          break;
        case CUSTOM3 :
          clos.selectCustom3();
          break;
        default :
          //throw new IllegalArgumentException("Unknown OrderInstanceColumn " + oic.toString());
          break;
      }

    }
    

    @Override
    protected WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> parseFilterFor(
                                                                             String filterString,
                                                                             String columnName,
                                                                             WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> previousConnection)
                    throws XNWH_WhereClauseBuildException {
      if (filterString != null && !filterString.equals("")) {
        CronLikeOrderWhereClausesContainerImpl whereClause = getCLOS();
        if (previousConnection != null) {
          whereClause = previousConnection.and();
        }
        List<String> tokens = convertIntoTokens(filterString);
        WhereClausesConnection<CronLikeOrderWhereClausesContainerImpl> lastConnection = parseTokens(tokens, columnName, whereClause);
        tokens.clear();
        return lastConnection;
      }
      if (previousConnection != null) {
        return previousConnection;
      } else {
        return null;
      }
    }


    protected final WhereClause<CronLikeOrderWhereClausesContainerImpl> retrieveColumnWhereClause(String columnAsString,
                                                                                     CronLikeOrderWhereClausesContainerImpl where) {
      CronLikeOrderColumn vc = CronLikeOrderColumn.getCLOColumnByName(columnAsString);
      switch (vc) {
        case APPLICATIONNAME :
          return where.whereApplicationname();
        case WORKSPACENAME :
          return where.whereWorkspacename();
        case INTERVAL :
          return where.whereInterval();
        case ID :
          return where.whereId();
        case LABEL :
          return where.whereLabel();
        case ONERROR :
          return where.whereOnError();
        case ORDERTYPE :
          return where.whereOrdertype();
        case STARTTIME :
          return where.whereStartTime();
        case NEXTEXECUTION :
          return where.whereNextExecution();
        case STATUS :
          return where.whereStatus();
        case VERSIONNAME :
          return where.whereVersionname();
        case ENABLED :
          return where.whereEnabled();
        case TIMEZONEID :
          return where.whereTimeZoneID();
        case CONSIDERDAYLIGHTSAVING :
          return where.whereConsiderDaylightSaving();
        case CUSTOM0 :
          return where.whereCustom0();
        case CUSTOM1 :
          return where.whereCustom1();
        case CUSTOM2 :
          return where.whereCustom2();
        case CUSTOM3 :
          return where.whereCustom3();
        default :
          return null;
      }
    }


    @Override
    protected CronLikeOrderWhereClausesContainerImpl newWhereClauseContainer() {
      return getCLOS().newWC();
    }


    @Override
    protected String getColumnName(String filterEntryKey, String defaultName) {
      try {
        CronLikeOrderColumn cloColumn = CronLikeOrderColumn.getCLOColumnByName(filterEntryKey);
        defaultName = cloColumn.getColumnName();
      } catch (Exception e) {
        logger.error("Invalid filtertype received", e);
      }
      return defaultName;
    }
    
}
