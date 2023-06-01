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
package com.gip.xyna.utils.misc;

import java.util.List;


/**
 * Formatierung für Tabellen bei CLI-Ausgaben
 * 
 * Zur Verwendung muss eine konkrete Subklasse erstellt werden, die die Methoden
 * - List&lt;List&lt;String&gt;&gt; getRows();
 * - List&lt;String&gt; getHeader();
 * implementiert.
 * 
 * Aufgerufen werden dann:
 * StringBuilder output = new StringBuilder();
 * ExampleTableFormatter etf = new ExampleTableFormatter(exampleData);
 * etf.writeTableHeader(output);
 * etf.writeTableRows(output);
 * 
 * prettyPrint = true erzeugt Tabellen Trenner in etwa so:
 *  Spalte1 | Spalte2
 *  ========+========
 *  Value1  | Value2
 *  
 * Der TableRowFormatter ist derzeit fest: MaxCellSizeTableRowFormatter
 * Mit diesem werden die Zellenbreiten an den längsten anzuzeigenden String angepasst. 
 */
public abstract class TableFormatter {
  private TableRowFormatter trf = new MaxCellSizeTableRowFormatter();
  private boolean prettyPrint = true;
  private boolean inspected;
  public interface TableRowFormatter {

    void appendHeaderRowSeparator(StringBuilder output, List<String> header);

    void setPrettyPrint(boolean prettyPrint);

    void appendRow(StringBuilder output, List<String> header);

    void inspect(List<String> header, List<List<String>> rows);
    
  }
  public abstract List<List<String>> getRows();

  public abstract List<String> getHeader(); 

  public void setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
    trf.setPrettyPrint(prettyPrint);
  }
  
  public void writeTableHeader(StringBuilder output) {
    if( !inspected ) {
      trf.inspect( getHeader(), getRows() );
      inspected = true;
    }
    trf.appendRow(output, getHeader());
    if( prettyPrint ) {
      trf.appendHeaderRowSeparator(output, getHeader());
    }
  }

  public String writeTableHeader() {
    StringBuilder sb = new StringBuilder();
    writeTableHeader(sb);
    return sb.toString();
  }
  
  public void writeTableRows(StringBuilder output) {
     if( !inspected ) {
       trf.inspect( getHeader(), getRows() );
       inspected = true;
     }
     for( List<String> row : getRows() ) {
       trf.appendRow(output, row);
     }
  }
  

  
  
  
  public static class MaxCellSizeTableRowFormatter implements TableRowFormatter {
    //TODO Auftrennen in PrettyPrint und MaxCellSize
    private int[] cellSizes;
    private boolean prettyPrint = true;
    private static char ppV = '\u2502'; 
    private static char ppH = '\u2550'; 
    private static char ppC = '\u256a'; 
    
    public static void configurePrettyPrintChars(String ppChars) {
      ppV = ppChars.charAt(0);
      ppH = ppChars.charAt(1);
      ppC = ppChars.charAt(2);
    }
    
    public static String getDefaultPrettyPrintChars() {
      return "\u2502\u2550\u256a";
    }
    

    public void inspect(List<String> header, List<List<String>> rows) {
      cellSizes = new int[header.size()];
      findMaximumCellSizes(header);
      for( List<String> row : rows ) {
        findMaximumCellSizes(row);
      }
    }

    private void findMaximumCellSizes(List<String> row) {
      int size = Math.min(cellSizes.length, row.size());
      for( int i=0; i<size; ++i ) {
        String v = row.get(i);
        cellSizes[i] = Math.max(cellSizes[i], v == null ? 4 : v.length() );
      }
    }

    public void appendRow(StringBuilder output, List<String> row) {
      int size = Math.min(cellSizes.length, row.size());
      for( int i=0; i<size; ++i ) {
        appendCell(output, i, cellSizes[i], row.get(i) );
      }
      output.append("\n");
    }

    private void appendCell(StringBuilder output, int i, int length, String value) {
      if( i != 0 ) {
        if( prettyPrint ) {
          output.append(" ").append(ppV).append(" "); //|
        } else {
          output.append(" ");
        }
      }
      String s = String.valueOf(value);
      output.append(s);
      for( int l = s.length(); l<length; ++l ) {
        output.append(' ');
      }
    }

    public void appendHeaderRowSeparator(StringBuilder output, List<String> header) {
      for( int i=0; i<cellSizes.length; ++i ) {
        if( i != 0 ) {
          output.append(ppH).append(ppC).append(ppH); //=+=
        }
        for( int l = 0; l<cellSizes[i]; ++l ) {
          output.append(ppH); //=
        }
        
      }
      output.append("\n");
    }
    
    public void setPrettyPrint(boolean prettyPrint) {
      this.prettyPrint = prettyPrint;
    }

  }

}
