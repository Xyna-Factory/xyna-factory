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
package gip.base.common;

import org.apache.log4j.Level;


/**
 * Definiert eigene Log-Level zu den Standard-Log-Level.
 * Werden die Log-Level geaendert sollte man sich auch LogFormatter.java anschauen.
 * Die verfuegbaren OBLog-Level:
 * <ul>
 * <li>PANIC   (1500) Fehler, wirft eine Exception
 * <li>ERROR   (1100) Fehler, wirft aber keine Exception
 * <li>SEVERE  (1000) (Standard-Log-Level)
 * <li>WARNING  (900) (Standard-Log-Level)
 * <li>INFO     (800) (Standard-Log-Level)
 * <li>CONFIG   (700) (Standard-Log-Level)
 * <li>DDL      (670) DB-Struktur-Aenderungen
 * <li>DML      (650) UPDATE/INSERT-Befehle
 * <li>PLSQL    (620) Aufrufe von PLSQL-Prozeduren/Funktionen
 * <li>SQL      (600) Standard-SQL-Abfragen
 * <li>FINE     (500) (Standard-Log-Level)
 * <li>FINER    (400) (Standard-Log-Level)
 * <li>FINEST   (300) (Standard-Log-Level)
 * <li>DEBUG    (200) 
 * <li>TEST     (100)
 * </ul>
 */
@SuppressWarnings("serial")
public class Log4jLevel extends Level {

  
  public static final Level DDL   = new Log4jLevel(15000, "DDL");  // zwischen DEBUG (10000) und INFO (20000) angesiedelt//$NON-NLS-1$
  public static final Level DML   = new Log4jLevel(14000, "DML"); //$NON-NLS-1$
  public static final Level PLSQL = new Log4jLevel(13000, "PLSQL"); //$NON-NLS-1$
  public static final Level SQL   = new Log4jLevel(12000, "SQL"); //$NON-NLS-1$
  public static final Level TEST  = new Log4jLevel(11000, "TEST"); //$NON-NLS-1$
  
  /**
   * @param name
   * @param value
   */
  private Log4jLevel(int value, String name) {
    super(value, name, 0);
  }
  
}


