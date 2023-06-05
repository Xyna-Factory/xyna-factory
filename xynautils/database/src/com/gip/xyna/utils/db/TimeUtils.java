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
package com.gip.xyna.utils.db;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * TimeUtils sind Hilfsfunktionen, die zum Übertragen von Date und Timestamp 
 * in die DB verwendet werden.
 * 
 * Bei Timestamps wurde darauf geachtet, dass die Zeiten in UTC übertragen 
 * und auch so gespeichert werden. Dies ist nun unabhängig davon, ob der 
 * Timestamp-Typ in der DB nun TIMESTAMP, TIMESTAMP WITH TIME ZONE oder 
 * TIMESTAMP WITH LOCAL TIME ZONE ist. Auch beim Auslesen aus der DB wird 
 * davon ausgegangen, dass der Timestamp in UTC gespeichert ist.
 * 
 * Dabei wird jedoch ein keliner Fehler durch das Gleichsetzen von UTC mit 
 * GMT gemacht, der jedoch vernachlässigbar ist. {@link http://java.sun.com/j2se/1.5.0/docs/api/java/util/Date.html}
 * 
 * Bei Date wird keine derartige Zeitzonenüberprüfung durchgeführt, da DATE
 * nicht für die Verwendung von Zeitzonen gedacht ist.
 * 
 * TimeUtils wird von ExtendedParameter, ResultSetUtils und Timestamp verwendet.
 */
public class TimeUtils {

  public static final String FORMAT_PLSQL_TIMESTAMP = "YYYY.MM.DD HH24:MI:SS.FF3";
  public static final String FORMAT_PLSQL_DATE      = "YYYY.MM.DD HH24:MI:SS";
  public static final String FORMAT_JAVA_TIMESTAMP  = "yyyy.MM.dd HH:mm:ss.SSS z"; //Suffix "z": Ausgabe mit Suffix UTC, Parsen der Angabe GMT+01.00
  public static final String FORMAT_JAVA_DATE       = "yyyy.MM.dd HH:mm:ss";

  /**
   * Parsen des übergebenen Date-Strings
   * @param withMilli
   * @param string
   * @return
   * @throws SQLException
   */
  public static Date parseDate(boolean withMilli, String string) throws SQLException {
    if( string == null || string.length() == 0 ) {
      return null; //Null in der DB muss erlaubt sein
    }
    DateFormat df = withMilli ? getTimestampFormatter() : getDateFormatter();
    try {
      return df.parse( string );
    } catch (ParseException e) {
      throw new SQLException( e.getMessage() );
    }
  }

  /**
   * Umwandlung in DB: String in Date
   * @param paramName
   * @return
   */
  public static String getToDate(String paramName) {
    return "TO_DATE("+paramName+",'"+FORMAT_PLSQL_DATE+"')";
  }
  /**
   * Umwandlung in DB: Date in String
   * @param paramName
   * @return
   */
  public static String getFromDate(String paramName) {
    return "TO_CHAR("+paramName+",'"+FORMAT_PLSQL_DATE+"')";
  }

  /**
   * Umwandlung in DB: String in Timestamp
   * Format so gewählt, dass Angabe "UTC" der SimpleDateFormat-Ausgabe verstanden wird
   * @param paramName
   * @return
   */
  public static String getToTimestamp(String paramName) {
    //Schwierigkeiten ergeb sich dadurch, dass isch SimpleDateFomrat und TO_TIMESTAMP_TZ 
    //nicht richtig verstehen, deswegen die Modifizierungen mit "||'+0'" und "\"UTC\"TZH"
    return "TO_TIMESTAMP_TZ("+paramName+"||'+0','"+FORMAT_PLSQL_TIMESTAMP+" \"UTC\"TZH')";
 }
  
  /**
   * Umwandlung in DB: Timestamp in String
   * Ausgabe in einem Fomrat, so dass SimpleDateFormat die Zeitzone versteht
   * @param paramName
   * @return
   */
  public static String getFromTimestamp(String paramName) {
    //Schwierigkeiten ergeb sich dadurch, dass isch SimpleDateFomrat und TO_TIMESTAMP_TZ 
    //nicht richtig verstehen, deswegen die Modifizierungen mit "\"GMT\"TZR"
    return "TO_CHAR( "+paramName+",'"+TimeUtils.FORMAT_PLSQL_TIMESTAMP+" \"GMT\"TZR')";
  }
  
  /**
   * @return benutztes DateFormat für Date
   */
  public static DateFormat getDateFormatter() {
    SimpleDateFormat dateFormatter = new SimpleDateFormat( FORMAT_JAVA_DATE );
    dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    return dateFormatter;
  }
  
  /**
   * @return benutztes DateFormat für Timestamp
   */
  public static DateFormat getTimestampFormatter() {
    SimpleDateFormat timestampFormatter = new SimpleDateFormat( FORMAT_JAVA_TIMESTAMP );
    timestampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    return timestampFormatter;
  }



  
}
