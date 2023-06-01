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
package com.gip.xyna.xprc.xbatchmgmt.selectbatch;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.selection.WhereClause;
import com.gip.xyna.xnwh.selection.WhereClauseNumber;
import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClauseStringTransformation;
import com.gip.xyna.xnwh.selection.WhereClausesConnection;
import com.gip.xyna.xnwh.selection.WhereClausesConnection.Connect;
import com.gip.xyna.xprc.xbatchmgmt.beans.BatchProcessStatus;



/**
 * WhereClauses für BatchProzesse. <br>
 * 
 * ACHTUNG: In {@link BatchProcessWhereClausesImpl#addWhereClause(WhereClausesConnection)} werden
 * die WhereClauses verschiedenen Tabellen zugeordnet. Dabei gehen die ursprünglichen Verknüpfungen und Klammern verloren.
 * Beim Erstellen des Select-Strings werden dann immer gleiche Spalten mit "or" verknüpft
 * und unterschiedliche Spalten mit "and" ({@link BatchProcessWhereClausesImpl#getSelectString(BatchProcessTable)}). <br>
 *
 * Beispiel für Zuordnung der whereClauses zu den Tabellen:<br>
 * BatchProcessSelectImpl select = new BatchProcessSelectImpl();<br>
 * select.whereLabel().isEqual("A").or().whereLabel().isEqual("B").and().whereCustom0().isBiggerThan("0")<br>
 * 
 * ergibt folgende Zuordnung:
 * <ul>
 * <li> Archive={label=[label = "A", label = "B"], custom0=[custom0 &gt; "0"]} <br>
 * (die abgeschlossenen Aufträge müssen nach label und custom0 gefiltert werden)</li>
 * <li> ArchiveRunning={label=[label = "A", label = "B"]}<br>
 * (bei laufenden Aufträgen sind die custom-Felder im Archive noch leer, daher nur nach label filtern </li>
 * <li> Custom={counter0=[counter0 &gt; "0"]}<br>
 * (label ist in der Custom-Tabelle nicht vorhanden. Die Counter werden für abgeschlossene
 * Prozesse in die Custom-Feldern im Archiv übertragen (falls sie nicht explizit gesetzt werden).
 * Daher hier nach counter0 filtern.)</li>
 * </ul>
 * 
 */
public class BatchProcessWhereClausesImpl implements BatchProcessWhereClauses<BatchProcessWhereClausesImpl>, Serializable {

  private static final long serialVersionUID = 1L;
  
  //Zuordnung der WhereClauses zu den Tabellen, die von der Filterbedingung betroffen sind.
  //Dabei sind immer die WhereClauses, die eine Spalte betreffen zusammengefasst.
  private EnumMap<BatchProcessTable,Map<String,List<WhereClausesConnection<BatchProcessWhereClausesImpl>>>> whereClauses;


  public BatchProcessWhereClausesImpl() {
    whereClauses = new EnumMap<BatchProcessTable, Map<String,List<WhereClausesConnection<BatchProcessWhereClausesImpl>>>>(BatchProcessTable.class);
  }


  public WhereClausesConnection<BatchProcessWhereClausesImpl> where(WhereClausesConnection<BatchProcessWhereClausesImpl> innerWhereClause) {
    WhereClausesConnection<BatchProcessWhereClausesImpl> wcc = new WhereClausesConnection<BatchProcessWhereClausesImpl>(new WhereClauseBrace(innerWhereClause, this, false));
    addWhereClause(wcc);
    return wcc;
  }


  public WhereClausesConnection<BatchProcessWhereClausesImpl> whereNot(WhereClausesConnection<BatchProcessWhereClausesImpl> innerWhereClause) {
    WhereClausesConnection<BatchProcessWhereClausesImpl> wcc = new WhereClausesConnection<BatchProcessWhereClausesImpl>(new WhereClauseBrace(innerWhereClause, this, true));
    addWhereClause(wcc);
    return wcc;
  }

  
  private static class WhereClauseBrace extends WhereClause<BatchProcessWhereClausesImpl> {
    private static final long serialVersionUID = 1L;

    private BatchProcessWhereClausesImpl innerContainer;
    private boolean negated;


    public WhereClauseBrace(WhereClausesConnection<BatchProcessWhereClausesImpl> child, BatchProcessWhereClausesImpl container, boolean negated) {
      super(container, null);
      innerContainer = child.getCorrespondingContainer();
      this.negated = negated;
    }


    @Override
    public String asString() throws XNWH_InvalidSelectStatementException {
      throw new UnsupportedOperationException("asString not implemented");

//      StringBuilder sb = new StringBuilder();
//      if (negated) {
//        sb.append("not ");
//      }
//      sb.append("(").append(innerContainer.getSelectString()).append(")");
//      return sb.toString();
    }


    @Override
    public void addParameter(List<Object> list) {
      throw new UnsupportedOperationException("addParameter not implemented");

      //parameter des inneren containers adden
//      innerContainer.addParameter(list);
    }
  }

  /**
   * Fügt eine neue WhereClauseConnection hinzu. Dabei wird die WhereClause zu jeder Tabelle
   * hinzugefügt, in der nach der entsprechenden Column gefiltert werden soll. <br>
   * FIXME durch das Kopieren der WhereClause (um sie mehreren Tabellen zuzuordnen)
   * geht die Verknüpfung (WhereClausesConnection.connect) und Klammern verloren.
   * Daher werden beim Zusammenbauen des SQL-Strings ({@link BatchProcessWhereClausesImpl#getSelectString(BatchProcessTable)})
   * eigene Verknüpfungen wieder eingefügt.
   */
  public void addWhereClause(WhereClausesConnection<BatchProcessWhereClausesImpl> wcc) {
    String columnName = wcc.getConnectedObject().getColumn();
    
    if( columnName != null ) {
      BatchProcessColumn bpc = BatchProcessColumn.getBatchProcessColumnByName(columnName);
      
      //whereClause für die "Haupttabelle" hinzufügen
      addWhereClauseForTable(wcc, bpc.getTable(), columnName);
      
      if (bpc.getAdditionalTable() != null) {
        //es soll noch in einer weiteren Tabelle gefiltert werden
        //hier könnte der ColumnName anders sein, daher erst einmal umwandeln
        String convertedColumn = bpc.getAdditionalTable().convertColumnName(columnName);
        
        //eine Kopie der WhereClause anlegen und dabei den neuen ColumnName verwenden
        //ACHTUNG: hierdurch gehen die connects verloren, da diese erst später an die 
        //whereClause angehängt werden
        WhereClause<BatchProcessWhereClausesImpl> additionalWc = wcc.getConnectedObject().copy(convertedColumn);
        WhereClausesConnection<BatchProcessWhereClausesImpl> additionalWcc = new WhereClausesConnection<BatchProcessWhereClausesImpl>(additionalWc);

        //whereClause mit angepasstem Spaltenname bei der zusätzlichen Tabelle speichern
        addWhereClauseForTable(additionalWcc, bpc.getAdditionalTable(), convertedColumn);
      }
    } else {
      if( wcc.getConnectedObject() instanceof WhereClauseBrace ) {
        //bei geklammerte Ausdrücke, die enthaltenen WhereClauses übernehmen
        //ACHTUNG: hierdurch gehen die Klammern verloren
        EnumMap<BatchProcessTable,Map<String,List<WhereClausesConnection<BatchProcessWhereClausesImpl>>>> innerWhereClauses = ((WhereClauseBrace)wcc.getConnectedObject()).innerContainer.whereClauses;
        for (BatchProcessTable table : innerWhereClauses.keySet()){
          if (whereClauses.get(table) == null) {
            whereClauses.put(table, innerWhereClauses.get(table));
          } else {
            for (String column : innerWhereClauses.get(table).keySet()) {
              if (whereClauses.get(table).get(column) == null) {
                whereClauses.get(table).put(column,  innerWhereClauses.get(table).get(column));
              } else {
                whereClauses.get(table).get(column).addAll(innerWhereClauses.get(table).get(column));
              }
            }
          }
        }
      }
    }
  }


  /**
   * Fügt eine WhereClause zu einer Tabelle hinzu und legt sie bei der entsprechenden column ab
   * @param wcc
   * @param table
   * @param columnName
   */
  private void addWhereClauseForTable(WhereClausesConnection<BatchProcessWhereClausesImpl> wcc, BatchProcessTable table, String columnName) {
    List<WhereClausesConnection<BatchProcessWhereClausesImpl>> wcList = null;
    
    if (whereClauses.get(table) != null) {
      wcList = whereClauses.get(table).get(columnName);
    } else {
      // es gibt bisher noch keine whereClauses für diese Tabelle
      whereClauses.put(table, new HashMap<String, List<WhereClausesConnection<BatchProcessWhereClausesImpl>>>());
    }

    if (wcList == null) {
      // es gibt noch keine whereClauses für diese Spalte
      wcList = new ArrayList<WhereClausesConnection<BatchProcessWhereClausesImpl>>();
    }
    
    wcList.add(wcc);
    
    whereClauses.get(table).put(columnName, wcList);
  }
  
  protected boolean hasWhereClauses(BatchProcessTable table) {
    return whereClauses.get(table) != null;
  }

  /**
   * Liefert den sql-String für die where-Bedingung <br>
   * FIXME hierbei werden die ursprünglichen connects nicht beachtet!
   * Gleiche Spalten werden mit "or" und unterschiedliche Spalte mit "and" verknüpft.
   * @param table
   * @return
   * @throws XNWH_InvalidSelectStatementException
   */
  public String getSelectString(BatchProcessTable table) throws XNWH_InvalidSelectStatementException {
    StringBuilder sb = new StringBuilder();
    
    if (hasWhereClauses(table)) {
      for (String column : whereClauses.get(table).keySet()) {
        if (sb.length() > 0) {
          sb.append(" " + Connect.AND.getSql());
        }
        sb.append(" (");
        for (int i=0; i<whereClauses.get(table).get(column).size(); i++) {
          if (i > 0) {
            sb.append(" ").append(Connect.OR.getSql()).append(" ");
          }
          sb.append(whereClauses.get(table).get(column).get(i).getConnectedObject().asString());
        }
        sb.append(")");
      }
    }
    
    return sb.toString();
  }
  
  protected void addParameter(List<Object> list, BatchProcessTable table) {
    if (hasWhereClauses(table)) {
      for (String column : whereClauses.get(table).keySet()) {
        for (WhereClausesConnection<BatchProcessWhereClausesImpl> wcc : whereClauses.get(table).get(column)) {
          wcc.addParameter(list);
        }
      }
    }
  }

  public WhereClauseNumber<BatchProcessWhereClausesImpl> whereBatchProcessId() {
    return new WhereClauseNumber<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.BATCH_PROCESS_ID.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereLabel() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.LABEL.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereApplication() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.APPLICATION.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereVersion() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.VERSION.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereWorkspace() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.WORKSPACE.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereComponent() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.COMPONENT.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereSlaveOrderType() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.SLAVE_ORDER_TYPE.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom0() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM0.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom1() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM1.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom2() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM2.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom3() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM3.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom4() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM4.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom5() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM5.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom6() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM6.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom7() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM7.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom8() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM8.getColumnName());
  }

  public WhereClauseString<BatchProcessWhereClausesImpl> whereCustom9() {
    return new WhereClauseString<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CUSTOM9.getColumnName());
  }

  public WhereClauseNumber<BatchProcessWhereClausesImpl> whereTotal() {
    return new WhereClauseNumber<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.TOTAL.getColumnName());
  }

  public WhereClauseNumber<BatchProcessWhereClausesImpl> whereStarted() {
    return new WhereClauseNumber<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.STARTED.getColumnName());
  }

  public WhereClauseNumber<BatchProcessWhereClausesImpl> whereRunning() {
    return new WhereClauseNumber<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.RUNNING.getColumnName());
  }

  public WhereClauseNumber<BatchProcessWhereClausesImpl> whereFinished() {
    return new WhereClauseNumber<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.FINISHED.getColumnName());
  }

  public WhereClauseNumber<BatchProcessWhereClausesImpl> whereFailed() {
    return new WhereClauseNumber<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.FAILED.getColumnName());
  }

  public WhereClauseNumber<BatchProcessWhereClausesImpl> whereCanceled() {
    return new WhereClauseNumber<BatchProcessWhereClausesImpl>(this, BatchProcessColumn.CANCELED.getColumnName());
  }

  public WhereClauseStringTransformation<BatchProcessWhereClausesImpl> whereStatus() {
    return new WhereClauseStringTransformation<BatchProcessWhereClausesImpl>(this, 
        BatchProcessColumn.STATUS.getColumnName(), BatchProcessStatus.getStatusTransformation() );
  }
  
  
}
