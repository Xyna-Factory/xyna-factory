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
package com.gip.xyna.xprc.xbatchmgmt.selectbatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.PreparedQueryCache;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcess;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessInformation;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessArchiveStorable;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRestartInformationStorable;


public class BatchProcessSearch {
  
  private static Logger logger = CentralFactoryLogging.getLogger(BatchProcessSearch.class);

  private static PreparedQueryCache cache = new PreparedQueryCache();
  
  /**
   * Sucht alle Batch Prozesse, die die übergebenen Filterkriterien erfüllen und
   * sortiert das Ergebnis absteigend nach der batchProcessId
   * @param select Filterkriterien
   * @param maxRows maximale Anzahl an Ergebnissen
   * @return
   * @throws PersistenceLayerException
   */
  public BatchProcessSearchResult searchBatchProcesses(BatchProcessSelectImpl select, int maxRows) throws PersistenceLayerException {
    BatchProcessSearchResult result;
    List<Long> sort = new ArrayList<Long>();
    
    ODSConnection con = ODSImpl.getInstance().openConnection(ODSConnectionType.DEFAULT);
    try {
      //die Ids der Batch Prozesse suchen, die die Filterkriterien erfüllen
      Set<Long> found = searchBatchProcessIds(con, select);
      
      //das Ergebnis absteigend sortieren
      sort.addAll(found);
      Comparator<Long> comparator = Collections.<Long>reverseOrder();
      Collections.sort(sort, comparator);
      
      //auf maximale Anzahl beschränken
      if (sort.size() > maxRows) {
        List<Long> tmp = new ArrayList<Long>();
        for (int i = 0; i<maxRows; i++) {
          tmp.add(sort.get(i));
        }
        sort = tmp;
      }
      
      //zu den BatchProcessIds die zugehörigen BatchProcessInformations holen
      List<BatchProcessInformation> bpis = getBatchProcessInformations(con, sort, select);
      result = new BatchProcessSearchResult(bpis, found.size());
    } finally {
      finallyClose(con);
    }
    
    return result;
  }
  

  /**
   * Liefert die Ids der Batch Prozesse, die die übergebenen Filterkriterien erfüllen
   * @param con
   * @param select Filterkriterien
   * @return
   * @throws PersistenceLayerException
   */
  private Set<Long> searchBatchProcessIds(ODSConnection con, BatchProcessSelectImpl select) throws PersistenceLayerException {
    Set<Long> found = new HashSet<Long>();
    
    //zunächst werden alle laufenden Prozesse aus dem Archive gesucht, die die Filter-Kriterien erfüllen
    List<Long> archiveRunningIds = searchBatchProcessIds(con, select, BatchProcessTable.ArchiveRunning);
    found.addAll(archiveRunningIds);
    
    //dann nur die behalten, die auch die Filter-Kriterien in Customization und RuntimeInformation 
    //erfüllen
    filter( found, con, select, BatchProcessTable.Custom );
    filter( found, con, select, BatchProcessTable.Runtime );
    
    //zu den gefundenen laufenden Prozessen müssen nun noch die abgeschlossenen hinzugefügt werden
    List<Long> archiveIds = searchBatchProcessIds(con, select, BatchProcessTable.Archive);
    found.addAll(archiveIds);
    
    //als letztes muss noch die Schnittmenge mit den Suchergebnissen aus der RestartInformation
    //gebildet werden, um die Filter-Kriterien aus der RestartInformation zu berücksichtigen
    filter( found, con, select, BatchProcessTable.Restart );
    
    return found;
  }
  
  private void filter(Set<Long> found, ODSConnection con, BatchProcessSelectImpl select, BatchProcessTable table) throws PersistenceLayerException {
    if( select.hasWhereClauses(table) ) {
      List<Long> filteredIds = searchBatchProcessIds(con, select, table);
      found.retainAll(filteredIds);
    } else {
      //logger.debug("no where clause condition for "+table);
    }
  }


  private List<Long> searchBatchProcessIds(ODSConnection con, BatchProcessSelectImpl select, BatchProcessTable table) throws PersistenceLayerException {
    try {
      return searchBatchProcessIdsInternally(con, select, table);
    } catch (XNWH_IncompatiblePreparedObjectException e) {
      cache.clear();
      return searchBatchProcessIdsInternally(con, select, table);
    }
  }
  
  
  private List<Long> searchBatchProcessIdsInternally(ODSConnection con, BatchProcessSelectImpl select, BatchProcessTable table) throws PersistenceLayerException {
    String selectString;
    
    try {
      selectString = select.getSelectString(table);
    } catch (XNWH_InvalidSelectStatementException e) {
      throw new RuntimeException("Problem with select statement: " + e.getMessage(), e);
    }
    
    ResultSetReader<Long> idReader = table.getIdReader();
    PreparedQuery<Long> archiveQuery = cache.getQueryFromCache(selectString, con, idReader);
    Parameter params = select.getParameter(table);
    
    List<Long> result = con.query(archiveQuery, params, -1);
    //logger.debug("Select for "+table+": " + selectString+" -> "+result.size()+" "+result );
    return result;
  }
  
  /**
   * Liefert die BatchProcessInformations zu den übergebenen BatchProcessIds (für laufende
   * aus dem Memory, für abgeschlossenen aus dem Warehouse)
   * @param con
   * @param batchProcessIds
   * @param select 
   * @return
   * @throws PersistenceLayerException
   */
  private List<BatchProcessInformation> getBatchProcessInformations(ODSConnection con, List<Long> batchProcessIds, BatchProcessSelectImpl select) throws PersistenceLayerException {
    List<BatchProcessInformation> bpis = new ArrayList<BatchProcessInformation>();
    BatchProcessManagement bpm = XynaFactory.getInstance().getProcessing().getBatchProcessManagement();

    boolean removeConstantInput = !select.containsColumn(BatchProcessColumn.CONSTANT_INPUT);
    for (Long batchProcessId : batchProcessIds) {
      BatchProcess batchProcess = bpm.getBatchProcess(batchProcessId);
      BatchProcessInformation bpi;
      
      if (batchProcess != null) {
        //Batch Process läuft noch
        bpi = batchProcess.getBatchProcessInformation();
      } else {
        //der Process läuft nicht mehr, daher Archive und RestartInformation
        //aus dem Warehouse holen
        try {
          BatchProcessArchiveStorable archive = new BatchProcessArchiveStorable(batchProcessId);
          con.queryOneRow(archive);
          bpi = new BatchProcessInformation(archive, true);

          BatchProcessRestartInformationStorable restartInfo = new BatchProcessRestartInformationStorable(batchProcessId);
          con.queryOneRow(restartInfo);
          bpi.setRestartInformation(restartInfo);
        }
        catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          // sollte eigentlich nicht auftreten, da aus dem Archive keine Daten gelöscht werden
          logger.warn("BatchProcess disappeared", e);
          continue;
        }
      }
      
      //selection
      if (removeConstantInput) {
        bpi.getRestartInformation().setConstantInput(null);
      }
      
      bpis.add(bpi);
    }
    
    return bpis;
  }
  
  
  
  private void finallyClose(ODSConnection con) {
    if( con != null ) {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Failed to close connection", e);
      }
    }
  }
}
