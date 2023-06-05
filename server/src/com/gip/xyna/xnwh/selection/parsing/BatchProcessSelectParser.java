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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessColumn;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessSelectImpl;
import com.gip.xyna.xprc.xbatchmgmt.selectbatch.BatchProcessWhereClausesImpl;


public class BatchProcessSelectParser extends DefaultSelectionParser<BatchProcessWhereClausesImpl> {

    private final static Logger logger = CentralFactoryLogging.getLogger(BatchProcessSelectParser.class);

    BatchProcessSelectImpl bps;
    
    public BatchProcessSelectImpl getBPS() {
      if (bps == null) {
        bps = new BatchProcessSelectImpl();
      }
      return bps;
    }

    @Override
    public BatchProcessSelectImpl getSelectImpl() {
      return getBPS();
    }
    

    @Override
  protected void parseSelectInternally(String selectedColumn) {
    BatchProcessSelectImpl bps = getBPS();
    try {
      BatchProcessColumn bpc = BatchProcessColumn.getBatchProcessColumnByName(selectedColumn);
      switch (bpc) {
        case CONSTANT_INPUT :
          bps.selectConstantInput();
          break;
        default : //nicht unterstützt
      }
    } catch (IllegalArgumentException e) {
      //hier werden einige spalten in der gui selektiert, für die selektion serverseitig nicht unterstützt ist
    }
  }
    

    @Override
    protected WhereClausesConnection<BatchProcessWhereClausesImpl> parseFilterFor(
                                                                             String filterString,
                                                                             String columnName,
                                                                             WhereClausesConnection<BatchProcessWhereClausesImpl> previousConnection)
                    throws XNWH_WhereClauseBuildException {
      if (filterString != null && !filterString.equals("")) {
        BatchProcessWhereClausesImpl whereClause = getBPS();
        if (previousConnection != null) {
          whereClause = previousConnection.and();
        }
        List<String> tokens = convertIntoTokens(filterString);
        WhereClausesConnection<BatchProcessWhereClausesImpl> lastConnection = parseTokens(tokens, columnName, whereClause);
        tokens.clear();
        return lastConnection;
      }
      if (previousConnection != null) {
        return previousConnection;
      } else {
        return null;
      }
    }


    protected final WhereClause<BatchProcessWhereClausesImpl> retrieveColumnWhereClause(String columnAsString,
                                                                                        BatchProcessWhereClausesImpl where) {
      BatchProcessColumn bpc = BatchProcessColumn.getBatchProcessColumnByName(columnAsString);
      switch (bpc) {
        case APPLICATION :
          return where.whereApplication();
        case WORKSPACE :
          return where.whereWorkspace();
        case BATCH_PROCESS_ID :
          return where.whereBatchProcessId();
        case COMPONENT :
          return where.whereComponent();
        case LABEL :
          return where.whereLabel();
        case SLAVE_ORDER_TYPE :
          return where.whereSlaveOrderType();
        case VERSION :
          return where.whereVersion();
        case CUSTOM0 :
          return where.whereCustom0();
        case CUSTOM1 :
          return where.whereCustom1();
        case CUSTOM2 :
          return where.whereCustom2();
        case CUSTOM3 :
          return where.whereCustom3();
        case CUSTOM4 :
          return where.whereCustom4();
        case CUSTOM5 :
          return where.whereCustom5();
        case CUSTOM6 :
          return where.whereCustom6();
        case CUSTOM7 :
          return where.whereCustom7();
        case CUSTOM8 :
          return where.whereCustom8();
        case CUSTOM9 :
          return where.whereCustom9();
        case TOTAL :
          return where.whereTotal();
        case STARTED :
          return where.whereStarted();
        case RUNNING :
          return where.whereRunning();
        case FINISHED :
          return where.whereFinished();
        case FAILED :
          return where.whereFailed();
        case CANCELED :
          return where.whereCanceled();
        case STATUS :
          return where.whereStatus();
       default :
          return null;
      }
    }


    @Override
    protected BatchProcessWhereClausesImpl newWhereClauseContainer() {
      return new BatchProcessWhereClausesImpl();
    }


    @Override
    protected String getColumnName(String filterEntryKey, String defaultName) {
      try {
        BatchProcessColumn bpColumn = BatchProcessColumn.getBatchProcessColumnByName(filterEntryKey);
        defaultName = bpColumn.getColumnName();
      } catch (Exception e) {
        logger.error("Invalid filtertype received", e);
      }
      return defaultName;
  }
    
}
