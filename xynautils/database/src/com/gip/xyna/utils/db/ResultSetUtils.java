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
package com.gip.xyna.utils.db;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import com.gip.xyna.utils.db.types.Timestamp;
import com.gip.xyna.utils.db.types.XMLType;

/**
 * Hilfsfunktionen zum Lesen aus ResultSets
 *
 * Nicht alle Leseoperationen aus ResultSets sind so einfach wie ein rs.getString(1).
 * Diese Methoden-Sammlung soll helfen, das Auslesen zu vereinfachen.
 * Es gibt dabei immer zwei Methoden getXXX( ResultSet rs, int i )
 * und getXXX( ResultSet rs, String colName ), wie dies auch im ResultSet selbst
 * angeboten wird. F�r manche Ausleseoperationen ist es n�tig, dass vorher der
 * SELECT-String ver�ndert wird, dazu dienen die selectXXX-Methoden.
 * <p>
 * Beispiel-Aufruf:
 * <code><pre>
   private static final String VIEW_COL_STARTDATE = "start_date";
   String sql = "SELECT "+ResultSetUtils.selectTimestamp(VIEW_COL_STARTDATE) + " FROM VCronLST";
   sqlUtils.query( sql, null, new ResultSetReaderFunction(){
     public boolean read(ResultSet rs) throws SQLException {
     System.out.println( ResultSetUtils.getTimestamp( rs, VIEW_COL_STARTDATE ).getTime() );
     return false;
   }} );
   </pre></code>
 * <p>
 * Derzeit existieren die Hilfsfunktionen
 * <ul>
 *   <li> getClob
 *   <li> getDate
 *   <li> getTimestamp
 *   <li> getCalendar
 *   <li> getLong
 *   <li> getInteger
 *   <li> getXMLType
 * </ul>
 *
 *
 *
 */
public class ResultSetUtils {

  
  /**
   * Lesen eines CLOBs aus dem ResultSet
   * @param rs
   * @param i
   * @return
   * @throws SQLException
   */
  public static String getClob(ResultSet rs, int i) throws SQLException {
    Clob clob = rs.getClob(i);
    return clob.getSubString(1, (int) clob.length());
  }
  /**
   * Lesen eines CLOBs aus dem ResultSet
   * @param rs
   * @param i
   * @return
   * @throws SQLException
   */
  public static String getClob(ResultSet rs, String colName) throws SQLException {
    Clob clob = rs.getClob(colName);
    return clob.getSubString(1, (int) clob.length());
  }
  
  /**
   * Lesen eines Longs unter Ber�cksichtigung der NULL
   * @param rs
   * @param i
   * @return
   * @throws SQLException
   */
  public static Long getLong(ResultSet rs, int i) throws SQLException {
    long l = rs.getLong(i);
    return longToLong( l, rs.wasNull() );
  }
  /**
   * Lesen eines Longs unter Ber�cksichtigung der NULL
   * @param rs
   * @param colName
   * @return
   * @throws SQLException
   */
  public static Long getLong(ResultSet rs, String colName) throws SQLException {
    long l = rs.getLong(colName);
    return longToLong( l, rs.wasNull() );
  }
  private static Long longToLong(long l, boolean wasNull) {
    return wasNull ? null : Long.valueOf(l);
  }
  
  /**
   * Lesen eines Integers unter Ber�cksichtigung der NULL
   * @param rs
   * @param i
   * @return
   * @throws SQLException
   */
  public static Integer getInteger(ResultSet rs, int i) throws SQLException {
    int j = rs.getInt(i);
    return intToInteger( j, rs.wasNull() );
  }
  /**
   * Lesen eines Integers unter Ber�cksichtigung der NULL
   * @param rs
   * @param colName
   * @return
   * @throws SQLException
   */
  public static Integer getInteger(ResultSet rs, String colName) throws SQLException {
    int j = rs.getInt(colName);
    return intToInteger( j, rs.wasNull() );
  }
  private static Integer intToInteger(int i, boolean wasNull) {
    return wasNull ? null : Integer.valueOf(i);
  }

  
  
  
  
  /**
   * Vorbereitung des Select-Statements, um einen Timestamp zu lesen
   * Timestamps werden in der DB in UTC erwartet
   * @param paramName
   * @return
   */
  public static String selectTimestamp(String paramName) {
    return TimeUtils.getFromTimestamp( paramName )+" AS "+paramName;
  }
  /**
   * Lesen des Timestamp aus dem ResultSet
   * Timestamps werden in der DB in UTC erwartet
   * @param rs
   * @param i
   * @return
   * @throws SQLException
   */
  public static Timestamp getTimestamp( ResultSet rs, int i) throws SQLException {
    return new Timestamp( TimeUtils.parseDate( true, rs.getString(i) ) );
  }
  /**
   * Lesen des Timestamp aus dem ResultSet
   * Timestamps werden in der DB in UTC erwartet
   * @param rs
   * @param colName
   * @return
   * @throws SQLException
   */
  public static Timestamp getTimestamp(ResultSet rs, String colName) throws SQLException {
    return new Timestamp( TimeUtils.parseDate( true, rs.getString(colName) ) );
  }

  
  /**
   * Vorbereitung des Select-Statements, um ein Date zu lesen
   * @param paramName
   * @return
   */
  public static String selectDate(String paramName) {
    return TimeUtils.getFromDate( paramName )+" AS "+paramName;
  }
  /**
   * Lesen des Date aus dem ResultSet
   * @param rs
   * @param i
   * @return
   * @throws SQLException
   */
  public static Date getDate( ResultSet rs, int i) throws SQLException {
    return TimeUtils.parseDate( false, rs.getString(i) );
  }
  /**
   * Lesen des Date aus dem ResultSet
   * @param rs
   * @param colName
   * @return
   * @throws SQLException
   */
  public static Date getDate(ResultSet rs, String colName) throws SQLException {
    return TimeUtils.parseDate( false, rs.getString(colName) );
  }
  
  /**
   * Vorbereitung des Select-Statements, um ein Calendar zu lesen
   * @param paramName
   * @param withMilli Sollen auch Millisekunden gelesen werden
   * @return
   */
  public static String selectCalendar(String paramName, boolean withMilli ) {
    return withMilli ? selectTimestamp(paramName) : selectDate(paramName);
  }
  /**
   * Lesen des Calendar aus dem ResultSet
   * @param rs
   * @param withMilli Auslesen der MilliSekunden (erfordert selectTimestamp oder selectCalendar(withMilli)
   * @param i
   * @return
   * @throws SQLException
   */
  public static Calendar getCalendar( ResultSet rs, boolean withMilli, int i) throws SQLException {
    return dateToClendar( TimeUtils.parseDate( withMilli, rs.getString(i) ) );
  }
  /**
   * Lesen des Calendar aus dem ResultSet
   * @param rs
   * @param withMilli Auslesen der MilliSekunden (erfordert selectTimestamp oder selectCalendar(withMilli)
   * @param colName
   * @return
   * @throws SQLException
   */
  public static Calendar getCalendar( ResultSet rs, boolean withMilli, String colName) throws SQLException {
    return dateToClendar( TimeUtils.parseDate( withMilli, rs.getString(colName) ) );
  }
  
  /**
   * Lesen eines XMLTypes aus dem ResultSet
   * @param rs
   * @param i
   * @return
   * @throws SQLException
   */
  public static XMLType getXMLType( ResultSet rs, int i ) throws SQLException {
    return new XMLType(getClob(rs, i));
  }

  /**
   * Lesen eines XMLTypes aus dem ResultSet
   * @param rs
   * @param colName
   * @return
   * @throws SQLException
   */
  public static XMLType getXMLType( ResultSet rs, String colName ) throws SQLException {
    return new XMLType(getClob(rs, colName));
  }

  /**
   * erzeugt &lt;tableIdentifier&gt;.&lt;colName&gt;.getCLOBVal() as &lt;colName&gt;
   * @param tableIdentifier prefix der tabelle, welches die spalte colName vom Type XMLType hat. <br>
   * Beispiel: In<br>
   *  <code>select b.a from myTable b</code><br>
   * ist b der tableIdentifier von myTable.
   * @param colName name der spalte/des parameters
   * @return
   */
  public static String selectXMLType(String tableIdentifier, String colName) {
    return tableIdentifier + "." + colName + ".getCLOBVal() AS "+colName;
  }
  
  /**
   * Umwandlung Date in Calendar
   * @param date
   * @return
   */
  private static Calendar dateToClendar(Date date) {
    if( date == null ) {
      return null;
    } else {
      Calendar cal = Calendar.getInstance();
      cal.setTime( date );
      return cal;
    }
  }
  
}
