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


/** 
 * Definiert die Konstanten, die fuer die Basis-Klassen gebraucht werden.
 */
public class OBConstants {

  public static boolean checkParams = true; 
  
  public final static String PATH_SEPARATOR = "\\";  //$NON-NLS-1$
  public final static String UNIQUE_CONSTRAINT = "Ein Feld ist nicht eindeutig! (UNIQUE-CONSTRAINT VIOLATED)";//$NON-NLS-1$
  public final static String WRONG_TYPE = "Falscher Datentyp";//$NON-NLS-1$
  public final static String NO_DATA_FOUND = "Es wurden keine Daten gefunden";//$NON-NLS-1$
  public final static String DATA_INSERTED = "Daten wurden eingefuegt";//$NON-NLS-1$
  public final static String DATA_UPDATED = "Daten wurden geaendert";//$NON-NLS-1$
  public final static String DATA_NOT_UPDATED = "Daten wurden nicht geaendert/eingefuegt! (Wurde Lock gestohlen?)";//$NON-NLS-1$
  public final static String DATA_NOT_UNIQUE = "Daten nicht eindeutig. Einfuegen nicht moeglich";//$NON-NLS-1$
  public final static String ERROR = "Fehler";  //$NON-NLS-1$
  public final static String SUCCESS = "Erfolg";//$NON-NLS-1$
  public final static String INFO = "Info";//$NON-NLS-1$
  public final static String WARNING = "Warnung";//$NON-NLS-1$
  public final static String FILL_ERROR = "Fehler beim Fuellen der Felder";  //$NON-NLS-1$
  public final static String DATE_FILL = "Datum muss ausgefuellt sein";  //$NON-NLS-1$
  public final static String INCONSISTANT_DATABASE = "Rollback nicht moeglich!\nDatenbank koennte in einem\ninkonsistenten Zustand sein";  //$NON-NLS-1$    
  public final static String INTRANET_HOSTS_INSERT = "Fehler bei Einfuegen von Intranet Hosts";//$NON-NLS-1$
  public final static String PRINT_MAIL = "Alle Felder muessen ausgefuellt sein";//$NON-NLS-1$
  public final static String DATE_FORMAT = "Datumsformat nicht korrekt";  //$NON-NLS-1$
  public final static String NO_REGION = "Keine Region gefunden";//$NON-NLS-1$
  public final static String ACTION_TYPE_NOT_IN_DATABASE = "Kein action typ gefunden";//$NON-NLS-1$
  public final static String OBJECT_TYPE_NOT_IN_DATABASE = "Kein objekt typ gefunden";//$NON-NLS-1$

  public final static String DELETE_NOT_POSSIBLE = " Datensaetze konnten nicht geloescht werden";//$NON-NLS-1$
  public final static String SETLOCK_NOT_POSSIBLE = "SetLock nicht moeglich";//$NON-NLS-1$
  public final static String GETLOCK_NOT_POSSIBLE = "GetLock nicht moeglich";//$NON-NLS-1$
  public final static String DATA_LOCKED_REMOVE_LOCK = "Der Datensatz ist gesperrt! Sperre aufheben?";//$NON-NLS-1$
  public final static String WRONG_ATTRIBUTE_TYPE = "Attribute typ nicht unterstuetzt";//$NON-NLS-1$
  public final static String HOSTNAMETOOLONG = "Hostname mit Ziffern wuerde zu lang";//$NON-NLS-1$
  public final static String JAVA_ERROR = "Interner Java-Fehler";//$NON-NLS-1$
  public final static String OBJECT_INVALID = "Fehler bei Validierung des Objektes";//$NON-NLS-1$
  public final static String OBJECT_NOT_SET = "Objekt ist nicht korrekt belegt";//$NON-NLS-1$
  public final static String NLS_DATE_FORMAT = "DD.MM.YYYY";//$NON-NLS-1$
  public final static String NLS_DATETIME_FORMAT = "DD.MM.YYYY HH24:MI:SS";//$NON-NLS-1$
  public final static String NLS_DATETIME_FORMAT2 = "DD.MM.YYYY HH24:MI";//$NON-NLS-1$
  public static final String NLS_TIMESTAMP_FORMAT = "YYYY.MM.DD HH24:MI:SS.FF3";//$NON-NLS-1$
  public final static String TIMESTAMP_FORMAT = "YYYY-MM-DD HH24:MI:SS";//$NON-NLS-1$
  public final static String MAIL_SENT = "E-mail wurde gesendet";//$NON-NLS-1$
  public final static String NOT_SUPPORTED_METHOD = "Methode kann nicht mehr unterstuetzt werden.";//$NON-NLS-1$
  public final static String IMG_LOCATION = "/gip/base/gui/images/";//$NON-NLS-1$
  public final static String CONNECTION_LOST = "Keine Verbindung zur Datenbank";//$NON-NLS-1$
  public final static String FIELD_NOT_NULL = "Feld darf nicht NULL sein";//$NON-NLS-1$
  public final static String FIELD_NOT_UNIQUE = "Feld nicht eindeutig";//$NON-NLS-1$
  public final static String FIELDS_NOT_UNIQUE = "Felder nicht eindeutig";//$NON-NLS-1$

  public final static int DISPLAY = 1;
  public final static int UPDATE = 2;
  public final static int INSERT = 3;
  public final static int SUBINSERT = 4;
  public final static int INTEGER = 0;
  public final static int STRING = 1;
  public final static int DOUBLE = 2;
  public final static int DATE = 3;
  public final static int RAW = 4;
  public final static int LONGVARCHAR = 5;
  public final static int CLOB = 6;
  public final static int BLOB = 7;
  public final static int LONG = 8;
  public final static int BOOLEAN = 9;
  public final static int TIMESTAMP_WITH_LOCAL_TIME_ZONE = 10;
  public final static int IRREGULAR_INT = Integer.MIN_VALUE;
  public final static int MAX_TF_LENGTH = 35;
  public final static int TF_LENGTH_SMART = 25;
  public final static int MAX_MLTF_LENGTH = 350;
  public final static int DEFAULT_COLUMN_SIZE = 110;
  public final static int MAILSTRINGLABELSIZE = 30; 
  public final static int READ_ONLY = 0;
  public final static int SETMODE_INSERT = 1;
  public final static int SETMODE_UPDATE = 2;
  public final static int SETMODE_INSERT_OR_UPDATE = 3;
  public final static int MAX_CLOB_LENGTH = 32760;

  public final static boolean INT = false;
  public final static boolean STR = true;

  // Extra nicht final, dann koennte es theoretisch via client.properties ueberschrieben werden.
  public final static double FIELDWIDTH_RATIO = 0.8; 
  public final static int    LMESSAGE_CLEAR = 3000; 

  
}


