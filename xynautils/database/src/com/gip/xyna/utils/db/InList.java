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
package com.gip.xyna.utils.db;

import java.util.ArrayList;
import java.util.List;

/**
 * InList zum Bau von SQL-Statements und dem zugehörigen Parameter-Objekt.
 * Beispiel:<code><pre>
    InList inList = new InList( new Long[]{1L,3L,17L} );
    String sql = "SELECT count(*) FROM TestT WHERE pk=? AND "+inList.getSQL("fk");
    //sql ist nun "SELECT count(*) FROM TestT WHERE pk=? AND fk IN (?,?,?)"
    int n = sqlUtils.query( sql, new Parameter( 123L, inList.getParams() ), ResultSetReaderFactory.getIntReader() ); 
    //mit Parameter( 123, 1, 3, 17 );
 </pre></code>
 * per Default werden bis zu maxLenght=5 Einträge mit Platzhaltern eingetragen, danach 
 * werden die Werte direkt in den SQL-String übernommen:
<code><pre>
    InList inList = new InList( new Long[]{1L,3L,17L,43L,137L,1234L} );
    String sql = "SELECT count(*) FROM TestT WHERE pk=? AND "+inList.getSQL("fk");
    //sql ist nun "SELECT count(*) FROM TestT WHERE pk=? AND fk IN (1,3,17,43,137,1234)"
    int n = sqlUtils.query( sql, new Parameter( 123L, inList.getParams() ), ResultSetReaderFactory.getIntReader() ); 
    //mit Parameter( 123 );
</pre></code>
 * Im Konstruktor kann maxLength verändert werden.
 * <p>
 * Falls mehr als 1000 Werte in der Liste stehen, wird diese automatisch aufgetrennt, 
 * um den Fehler "ORA-01795: maximum number of expressions in a list is 1000" zu umgehen.
 * Dies könnte dann so aussehen: "(col IN (A,B,C) OR col IN (D,E,F) OR col IN (G))"
 * <p>
 * Die Liste kann Strings enthalten, die dann automatisch mit einfachen Anführungszeichen
 * umschlossen werden, bei anderen Typen wird toString aufgerufen und kein Anführungszeichen gesetzt.
 * <p>
 */
public class InList {
  private static final int MAX_LENGTH = 5;
  private static final int MAX_ORACLE_LENGTH = 1000;
  private String sql;
  private Parameter params;
  private boolean listEmpty = true;
  private ArrayList<String> orLists = null;
  
  public InList(Object[] list) throws Exception {
    init( list, MAX_LENGTH );
  }
  
  public InList( List<Object> list) throws Exception {
    init(list.toArray(new Object[]{}), MAX_LENGTH);
  }
  
  public InList(Object[] list, int maxLength ) throws Exception {
    init( list, maxLength );
  }
  
  public InList( List<Object> list, int maxLength ) throws Exception {
    init(list.toArray(new Object[]{}), maxLength);
  }
  
  /**
   * kann überschrieben werden
   */
  protected Parameter createParameter() {
    return new Parameter();
  }
  
  /**
   * kann überschrieben werden
   */
  protected void addParameter(Parameter parameter, Object p) {
    parameter.addParameter(p);
  }
 
  private void init(Object[] list, int maxLength ) throws Exception {
    if( maxLength > MAX_ORACLE_LENGTH ) {
      throw new Exception("Mehr als "+MAX_ORACLE_LENGTH+" Elemente sind in einer IN-Liste nicht erlaubt" );
    }
    listEmpty = list == null || list.length == 0;
    params = createParameter();
    if( listEmpty ) {
      sql = null;
    } 
    else if( list.length == 1 ) {
      sql = " = ?";
      addParameter(params, list[0]);
    }
    else if( list.length <= maxLength ) {
      StringBuilder sb = new StringBuilder();
      sb.append( " IN (");
      for( Object o : list ) {
        addParameter(params, o);
        sb.append( "?,");
      }
      sb.setCharAt(sb.length()-1,')'); //letztes Komma austauschen
      sql = sb.toString();
    }
    else {
      boolean isString = (list[0] instanceof String); //nur erstes Element prüfen, wer mischt ist selbst dran schuld
      StringBuilder sb = null;
      orLists = new ArrayList<String>();
      for( int i=0; i<list.length; ++i ) {
        if( i%MAX_ORACLE_LENGTH == 0 ) {
          if( i != 0 ) {
            sb.append(")");
            orLists.add( sb.toString() );
          }
          sb = new StringBuilder();
          append( isString, sb, "(", list[i] );
        } else {
          append( isString, sb, ",", list[i] );
        }
      }
      if( sb.length() > 1 ) {
        sb.append(")");
        orLists.add( sb.toString() );
      }
      if( orLists.size() == 1 ) {
        sql = " IN "+orLists.get(0);
        orLists = null;
      }
    }
  }

  private void append(boolean isString, StringBuilder sb, String separator, Object object) {
    sb.append(separator);
    if( isString ) sb.append( "'" );
    sb.append( object );
    if( isString ) sb.append( "'" );
  }

  public String getSQL(String colName) {
    if (sql == null) {
      if( orLists == null ) {
        //leere IN-Liste -> ungültige Abfrage
        return colName + " <> " + colName;
      } else {
        //zuviele Parameter in IN-Liste -> mehrere IN-Listen durch OR getrennt
        StringBuilder sb = new StringBuilder();
        sb.append("("); //Einklammern, um OR zu schützen
        for( int i=0; i<orLists.size(); ++i ) {
          if( i != 0 ) sb.append(" OR ");
          sb.append(colName).append(" IN ").append( orLists.get(i) );
        }
        sb.append(")"); //Einklammern, um OR zu schützen
        return sb.toString();
      }
    }
    //Normalfall: IN-Liste gebaut
    return colName + sql;
  }
  
  public Parameter getParams() {
    return params;
  }
  
  public boolean isListEmpty() {
    return listEmpty;
  }
}
