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
package com.gip.xyna.utils.snmp.agent.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import com.gip.xyna.utils.snmp.OID;

/**
 * SparseSnmpTable ist ein Tool, um recht einfach eine statische Tabelle ueber SNMP abfragbar zu machen.
 * Der Unterschied zu SnmpTable besteht darin, dass auch lueckenhafte Indexe erlaubt sind.
 * 
 * Statisch heisst, dass derzeit nur GET und GET_NEXT unterstuetzt sind.
 *
 * Die angzeigten Daten werden ueber eine Implementierung des {@link SparseSnmpTableModel} geliefert. 
 */
public class SparseSnmpTable extends AbstractSnmpTable {
  private SparseSnmpTableModel sparseSnmpTableModel;
  private NextSparseOID nextSparseOID;
  
  /**
   * Konstruktor
   * @param sparseSnmpTableModel
   * @param oidBase
   */
  public SparseSnmpTable( SparseSnmpTableModel sparseSnmpTableModel, OID oidBase) {
    this(sparseSnmpTableModel,oidBase,WALK_END);
  }
  
  /**
   * Konstruktor
   * @param sparseSnmpTableModel
   * @param oidBase
   * @param nextOHS
   */
  public SparseSnmpTable(SparseSnmpTableModel sparseSnmpTableModel, OID oidBase, ChainedOidSingleHandler nextOHS) {
    super(sparseSnmpTableModel, oidBase, nextOHS );
    this.sparseSnmpTableModel = sparseSnmpTableModel;
    nextSparseOID = new NextSparseOID( oidBase, sparseSnmpTableModel.getColumnCount(), sparseSnmpTableModel.getRowIndexSet() );
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.AbstractSnmpTable#getNextOID(com.gip.xyna.utils.snmp.OID)
   */
  @Override
  protected OID getNextOID(OID oid) {
    return nextSparseOID.getNextOID(oid);
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.agent.utils.AbstractSnmpTable#refreshTableModel()
   */
  @Override
  public void refreshTableModel() {
    nextSparseOID = new NextSparseOID( oidBase, sparseSnmpTableModel.getColumnCount(), sparseSnmpTableModel.getRowIndexSet() );
  }
  
  /**
   * Hilfsklasse zum Erzeugen der NextOID fuer getNext-Requests
   * Speichert alle dafuer benoetigten Informationen.
   *
   */
  private static class NextSparseOID {

    private OID oidBase;
    private int columnCount;
    private int rowIndex;
    private ArrayList<Integer> rowIndexList;

    /**
     * @param oidBase 
     * @param columnCount
     * @param rowIndexList
     */
    public NextSparseOID(OID oidBase, int columnCount, Set<Integer> rowIndexSet) {
      this.oidBase = oidBase;
      this.columnCount = columnCount;
      rowIndex = oidBase.length() +1;
      if( rowIndexSet == null ) {
        this.rowIndexList = new ArrayList<Integer>();
      } else {
        this.rowIndexList = new ArrayList<Integer>(rowIndexSet);
      }
      Collections.sort(this.rowIndexList);
    }

    /**
     * @param oid
     * @return
     */
    public OID getNextOID(OID oid) {
      if( oid.length() < oidBase.length()+2 ) {
        return startOID( oid );
      }
      
      int lastRow = oid.getIntIndex( oidBase.length()+1 );
      int nextRow = nextRow(lastRow);
      if( nextRow == Integer.MAX_VALUE ) {
        nextRow = nextRow(Integer.MIN_VALUE);
        int lastColumn = oid.getIntIndex( oidBase.length() ); //Column-Index
        if( lastColumn == columnCount ) {
          return null; //Keine weitere Spalte mehr, Ende des Walks
        }
        return oidBase.append( lastColumn+1, nextRow );
      }
      
      return oid.setIndex( rowIndex, nextRow);
    }

    /**
     * @param oid
     * @return
     */
    private OID startOID(OID oid) {
      OID start = oid;
      if( oid.length() == oidBase.length()  ) {
        start = start.append(1); //erste Entry-Spalte anhaengen
      }
      int nextRow = nextRow(Integer.MIN_VALUE);
      if( nextRow == Integer.MAX_VALUE ) {
        return null; //sparseTableModel enthaelt keine Daten!
      }
      start = start.append( nextRow ); //erste Entry-Row anhaengen
      return start;
    }

    /**
     * @param lastRow
     * @return
     */
    private int nextRow(int lastRow) {
      if( lastRow == Integer.MIN_VALUE ) {
        return rowIndexList.get(0); //allererster Eintrag
      }
      int pos = Collections.binarySearch(rowIndexList, lastRow);
      if( pos < 0 ) {
        //gesuchte Zeile ist verschwunden
        pos = -pos-1; //naechsthoehere Zeile 
      } else {
        ++pos; //naechste Zeile
      }
      if( pos >= rowIndexList.size() ) {
        return Integer.MAX_VALUE;
      }
      return rowIndexList.get(pos);
    }
    
  }  
  
}
  