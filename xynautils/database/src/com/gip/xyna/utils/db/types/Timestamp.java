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
package com.gip.xyna.utils.db.types;

import java.util.Calendar;
import java.util.Date;

import com.gip.xyna.utils.db.TimeUtils;

/**
 * �bertragung eines Timestamps in die Datenbank.
 *
 * Diese Klasse ist nur ein Wrapper f�r ein Date. Anhand des Klassennamens kann dann
 * jedoch festgestellt werden, dass der Date als Timestamp in die DB eingetragen werden
 * soll.
 * 
 * Timestamps werden in die DB als UTC eingetragen und beim Auslesen auch so erwartet.
 *
 * Dies ist unabh�ngig davon, ob der Timestamp-Typ in der DB nun "TIMESTAMP", 
 * "TIMESTAMP WITH TIME ZONE" oder "TIMESTAMP WITH LOCAL TIME ZONE" ist.
 * 
 * Die unterschiedliche Behandlung der Timestamps in der DB hat zur Folge, dass bei 
 * Verwendung von "TIMESTAMP WITH {,LOCAL} TIME ZONE" die Zeit in der DB korrekt 
 * gespeichert wird und von anderen korrekt ausgelesen werden kann. Bei Verwendung
 * eines "TIMESTAMP" in der DB wird die Zeit jedoch als UTC eingetragen und keine 
 * Zeitzoneninformation gespeichert, so dass Clients, die nicht wissen, dass der 
 * Timestamp in UTC gespeichert ist, diesen falsch auslesen.
 */
public class Timestamp {
  
  private Date date = null;
  
  /**
   * Default-Konstruktor: aktuelles Date
   */
  public Timestamp() {
    this.date = new Date();
  }
  
  /**
   * Konstruktor, der gegebenes Date �bernimmt 
   * @param date
   */
  public Timestamp( Date date) {
    this.date = date;
  }
  
  /**
   * Konstruktor, der gegebenes Datum aus den Millisekunden seit 1970 �bernimmt. 
   * @param milli
   */
  public Timestamp( long milli ) {
    this.date = new Date(milli);
  }

  /**
   * Konstruktor, der gegebenen Calendar �bernimmt 
   * @param date
   */
  public Timestamp( Calendar calendar ) {
    if( calendar != null ) {
      this.date = calendar.getTime();
    }
  }

  /**
   * Ausgabe des gespeicherten Date
   * @return
   */
  public Date getDate() {
    return date;
  }
  
  @Override
  public String toString() {
    if( date == null ) {
      return "NULL";
    }
    return TimeUtils.getTimestampFormatter().format( date );
  }

  /**
   * Returns the number of milliseconds since January 1, 1970, 00:00:00 GMT
   * represented by this <tt>Timestamp</tt> object.
   *
   * @return  the number of milliseconds since January 1, 1970, 00:00:00 GMT
   *          represented by this timestamp.
   */
  public long getTime() {
    return date.getTime();
  }

}
