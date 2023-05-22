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
package gip.base.common;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class OBException extends Exception implements Serializable {
  
  private transient static Logger logger = Logger.getLogger(OBException.class);

  public static final String MARKABLE_NONE = ""; //$NON-NLS-1$

  /** 
   * The error number is useful when having to react specific on different Exceptions.
   * The error message can than be formulated without thinking on later parsing the
   * message as the Exception is uniquely identified by the error number.
   */
  protected int errorNo = OBErrorNumber.unknownError;
  protected String errorCode = ""; //$NON-NLS-1$
  protected String[] replacements = new String[0];
  private String _hintMsg = ""; // Hinweistext der Exception, zur Fehlerdarstellung setzbar. //$NON-NLS-1$

  
  /** Kann eine verursachende Exception enthalten */
  private Exception _causeExp=null;
  private String _causeExpMsg=""; //$NON-NLS-1$
  private String _causeExpStackTrace=""; //$NON-NLS-1$

  /** Zeitpunkt des Auftretens (im Log) */
  private String _logTimeStamp = ""; //$NON-NLS-1$
  /** Schwere des Fehlers */
  private int _severity = Severity.NormalError; 

  /** Default-Werte fuer die Schwere des Fehlers */
  public interface Severity {
    public static final int CriticalError=0; 
    public static final int NormalError=1; 
    public static final int Warning=2; 
    public static final int Hint=3;
    public static final String[] toString = {"Kritischer Fehler","Fehler","Warnung","Hinweis"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  }

  /** 
   * Angabe der Fehlernummern
   * Bitte nach Arbeitspaketen getrennt schreiben!
   * Konvention: Namen mit Zahl n am Ende bedeuten, das darin n '%' zu ersetzen sind
   */
  public interface OBErrorNumber {

    // hier bitte Nummern eintragen
    // 0000-0999 System
    public static final int noError               = 0;
    public static final int unknownError          = OBAttribute.NULL;
    public static final int unknownError1         = 1;
    public static final int actionAborted         = 2;
    public static final int callback              = 3;    
    public static final int communicationError    = 5;

    // Aus OBAttribute
    public static final int wrongType1            = 10;
    public static final int parseException1       = 11;
    public static final int unkownCompOp1         = 12;


    
    // Aus OBObject
    public static final int pkUnkown                    = 20;
    public static final int attribNotFoundInHashtable1  = 21;
    public static final int arrayLengthDifferent        = 22;
    public static final int noPrimaryKey1               = 23;
    public static final int numberFormatException1      = 24;
    
    // Aus OBDBObject
    public static final int objectNotFound1             = 30;
    public static final int objectNotUnique1            = 31;
    public static final int sqlException1               = 32;
    public static final int sqlFatalException           = 33;
    public static final int permissionDenied            = 34;
    public static final int objectPermissionDenied      = 35;
    public static final int interruptedException1       = 36;
    public static final int classNotFoundException1     = 37;
    public static final int securityException1          = 38;
    public static final int noSuchMethodException1      = 39;
    public static final int noSuchElementException1     = 41;
    public static final int pkValueIrregular1           = 40;
    public static final String insertedValueToLarge     = "ORA-01401"; //$NON-NLS-1$
    public static final String lostConnection           = "ORA-01012"; //$NON-NLS-1$
    public static final String sessionKilled            = "ORA-00028"; //$NON-NLS-1$

    // Aus OBDriver
    public static final int driverNotInitialized        = 50;

    // Aus OBTableObject
    public static final int updateNotSupported          = 51;
    public static final int objectLocked3               = 52;
    public static final int objectNotSet                = 53;
    public static final int deleteNotPossible2          = 54;
    public static final int setLockNotPossibleNoKey     = 55;
    public static final int setLockNotPossible1         = 56;
    public static final int getLockNotPossible1         = 57;
    public static final int pkUnkown1                   = 58;
 
    // Aus OBDTO
    public static final int WRONG_CAPABILTY = 25;
    public static final int capabilityUnknown = 60;
    public static final int capabilityNotSet = 61;
    public static final int parameterNotSet = 65;
    
    // Aus OBStatusChoice
    public static final int invalidStatusChoiceValue = 100;

    // Enums
    public static final String noEnumValue1            = "noEnumValue"; //$NON-NLS-1$
    
    public static final String invalidNumber1          = "invalidNumber"; //$NON-NLS-1$
    
    public static final String timeOut                 = "timeOut"; //$NON-NLS-1$
    
  }


  private static Hashtable<String,String> messages = new Hashtable<String,String>();
  private static Hashtable<String,String> hints    = new Hashtable<String,String>();


  /** In dieser Methode werden die Default-Error-Messages angegeben.
   @return
   */
  static {
    // 0000-0999 System
    addErrorMessage(OBErrorNumber.noError, "kein Fehler aufgetreten."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.unknownError, "unbekannter Fehler aufgetreten."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.unknownError1, "Unbekannter Fehler: %"); //$NON-NLS-1$

    addErrorMessage(OBErrorNumber.actionAborted, "Vorgang auf Benutzerwunsch abgebrochen."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.callback, "Kommunikationsehler bei einer Rueckfrage an den Client."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.communicationError, "Es ist ein Kommunikationsfehler zwischen\n" + //$NON-NLS-1$
                                                      "dem SmartClient und Server aufgetreten.\n" + //$NON-NLS-1$
                                                      "Die aktuelle Aktion wurde abgebrochen.", //$NON-NLS-1$
                                                      "Der Smartclient wird beendet.\nBitte starten Sie den Smartclient erneut."); //$NON-NLS-1$

    // Aus OBAttribute
    addErrorMessage(OBErrorNumber.wrongType1, "Falscher Datentyp %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.parseException1, "ParseException: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.unkownCompOp1, "Unbekannter Vergleichsoperator: %"); //$NON-NLS-1$

    // Aus OBObject
    addErrorMessage(OBErrorNumber.pkUnkown, "Es ist kein PK bekannt!"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.attribNotFoundInHashtable1, "Der Hashtable enthaelt keinen Wert fuer das Attribut %."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.arrayLengthDifferent, "Array-Laengen sind ungleich."); //$NON-NLS-1$

    // Aus OBDBObject
    addErrorMessage(OBErrorNumber.objectNotFound1, "%-Objekt nicht gefunden"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.objectNotUnique1, "%-Objekt nicht eindeutig"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.sqlException1, "SQL-Fehler: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.sqlFatalException, "FATAL: Cannot close statement"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.permissionDenied, "Permission denied"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.objectPermissionDenied, "Object Permission denied"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.interruptedException1, "InterruptedException: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.classNotFoundException1, "ClassNotFoundException: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.securityException1, "SecurityException: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.noSuchMethodException1, "NoSuchMethodException: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.noSuchElementException1, "NoSuchElementException: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.pkValueIrregular1, "Bei der Suche nach % wurde kein Primary Key angegeben."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.insertedValueToLarge, "Es wurde versucht, einen zu langen Wert in eine Spalte einzutragen."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.lostConnection, "Keine gueltige Connection zur Datenbank."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.sessionKilled, "Ihre Connection zur Datenbank wurde geschlossen."); //$NON-NLS-1$

    // Aus OBDriver
    addErrorMessage(OBErrorNumber.driverNotInitialized, "DB-Treiber nicht initialisert."); //$NON-NLS-1$

    // Aus OBTableObject
    addErrorMessage(OBErrorNumber.updateNotSupported, "Update wird derzeit nur in der Version key > 0 und mit leerem whereClause unterstuetzt"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.objectLocked3, "Objekt ist gesperrt: % %\nEs wird derzeit benutzt von Benutzer: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.objectNotSet, "Objekt ist nicht korrekt belegt"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.deleteNotPossible2, "Datensaetze konnten nicht geloescht werden. Objekt: % ID: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.setLockNotPossibleNoKey, "Fehler bei setLock: Weder key noch whereClause gesetzt."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.setLockNotPossible1, "SetLock nicht moeglich: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.getLockNotPossible1, "GetLock nicht moeglich: %"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.pkUnkown1, "Fuer die Tabelle % ist keine PK-Sequenz definiert."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.noPrimaryKey1, "Fuer den View/die Tabelle % ist kein Primaerschluesselattribut definiert."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.numberFormatException1, "{0} ist keine gueltige Zahl oder zu gross."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.WRONG_CAPABILTY, "Es wurde eine undefinierte Capability (%) verwendet."); //$NON-NLS-1$

    // Aus OBDTO
    addErrorMessage(OBErrorNumber.capabilityUnknown, "Die gewaehlte Aktion ist mit den uebergebenen Daten nicht zulaessig."); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.capabilityNotSet, "Keine Capapility gesetzt"); //$NON-NLS-1$
    addErrorMessage(OBErrorNumber.parameterNotSet, "Der Parameter % ist nicht gesetzt"); //$NON-NLS-1$

    // Aus OBStatusChoice
    addErrorMessage(OBErrorNumber.invalidStatusChoiceValue,"Das Feld: % (Datenbank-Attribut: %) hat den ungueltigen Wert: %"); //$NON-NLS-1$
    
    addErrorMessage(OBErrorNumber.invalidNumber1, "Der Wert {0} laesst sich nicht in eine Zahl umwandeln."); //$NON-NLS-1$
  }


  /**
   * @param errorNumber Fehlernummer
   * @param message Meldung
   */
  public static void addErrorMessage(int errorNumber, String message) {
    addErrorMessage(errorNumber, message, null);
  }

  
  /**
   * @param errorNumber Fehlernummer
   * @param message Meldung
   * @param hint Hinweis
   */
  public static void addErrorMessage(int errorNumber, String message, String hint) {
    if (messages.containsKey(String.valueOf(errorNumber))) {
      if (!messages.get(String.valueOf(errorNumber)).equals(message)) {
        logger.error("", new Exception("Doppelte Fehlernummer " + errorNumber)); //$NON-NLS-1$ //$NON-NLS-2$
      }  
    }
    else {
      messages.put(String.valueOf(errorNumber), message);
      if (null != hint) {  // Sonst NPE beim Laden der Klasse
        hints.put(String.valueOf(errorNumber), hint);
      }
    }  
  }

  /**
   * Gibt alle vorhanden Fehlermeldungen auf der Konsole aus.
   */
  public void printAllExceptions() {
    Enumeration<String> e=  messages.keys();
    for (int i = 0; i < messages.size(); i++) {
      Object key=e.nextElement();
      logger.debug(key+" : "+messages.get(key)+" : "+hints.get(key)); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
  }

  
  /**
   * @param errorCode Fehlercode
   * @param message Fehlermeldung
   */
  public static void addErrorMessage(String errorCode, String message) {
    addErrorMessage(errorCode, message, null);
  }  

  
  /**
   * @param errorCode Fehlercode
   * @param message Fehlermeldung
   * @param hint Hinweis
   */
  public static void addErrorMessage(String errorCode, String message, String hint) {
    if (messages.containsKey(errorCode)) {
      if (!messages.get(errorCode).equals(message)) {
        logger.error("", new Exception("Doppelter Fehlercodenummer " + errorCode)); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
    else {
      messages.put(errorCode, message);
      if (null != hint) {  // Sonst NPE beim Laden der Klasse
        hints.put(errorCode, hint);
      }
    }
  }


  /**
   * @param errNo Fehlercode
   * @return Message
   */
  public static String getErrorMessage(int errNo) {
    String tmpStr = messages.get(String.valueOf(errNo));
    if (tmpStr == null) {
      return "Unbekannter Fehler (ErrorNo " + errNo + ")"; //$NON-NLS-1$ //$NON-NLS-2$
    }
    else {
      return tmpStr;
    }
  }


  /**
   * @param errMsg Fehlercode
   * @return ErrorMessage
   */
  public static String getErrorMessage(String errMsg) {
    String tmpStr = messages.get(errMsg);
    if (tmpStr == null) {
      return errMsg;
    }
    else {
      return tmpStr;
    }
  }


  /** 
   * Erzeugt aus einer ErrorMessage und einem String-Array von Ersetzungen eine neue ErrorMessage
   * @param s ErrorMessage, die '%'-Platzhalter enthaelt
   * @param replacements Array von Ersetzungen
   * @return neue ErrorMessage
   */
  protected static String getErrorMessage(String s, String[] replacements) {
    String errorM = getErrorMessage(s);
    String tmpString = ""; //$NON-NLS-1$
    for (int i = 0; i < replacements.length; i++) {
      String toReplace = "{" + i + "}"; //$NON-NLS-1$ //$NON-NLS-2$
      int index = errorM.indexOf(toReplace);
      if (index == -1) {
        toReplace = "%"; //$NON-NLS-1$
        index = errorM.indexOf(toReplace);
      }
      if (index != -1) {
        tmpString = errorM.substring(0, index) + replacements[i] + errorM.substring(index + toReplace.length());
        errorM = tmpString;
      }
    }
    return errorM;
  }


  /** 
   * Erzeugt aus einer ErrorMessage und einem String-Array von Ersetzungen eine neue ErrorMessage
   * @param errNo Error-Nummer
   * @param replacements Array von Ersetzungen
   * @return neue ErrorMessage
   */
  protected static String getErrorMessage(int errNo, String[] replacements) {
    String errorM = getErrorMessage(errNo);
    String tmpString = ""; //$NON-NLS-1$
    for (int i = 0; i < replacements.length; i++) {
      String toReplace = "{" + i + "}"; //$NON-NLS-1$ //$NON-NLS-2$
      int index = errorM.indexOf(toReplace);
      if (index == -1) {
        toReplace = "%"; //$NON-NLS-1$
        index = errorM.indexOf(toReplace);
      }
      if (index != -1) {
        tmpString = errorM.substring(0, index) + replacements[i] + errorM.substring(index + toReplace.length());
        errorM = tmpString;
      }
    }
    return errorM;
  }


  /**
   * Feld, das den Fehler verursacht
   */
  protected String _markable = ""; //$NON-NLS-1$
  
  // --------------------------------------------------------------------------------------------
  // ------- Konstruktoren ----------------------------------------------------------------------
  // --------------------------------------------------------------------------------------------

  /** 
   * Constructs an Exception with the specified detail message.
   * Error number is set to unkownError.
   * @param s detail message.
   */
  public OBException(String s) {
    super(getErrorMessage(s));
    errorNo = OBErrorNumber.unknownError;
    errorCode = s;
    replacements = new String[0];
    _markable = ""; //$NON-NLS-1$
    setLogTimeStamp();
  }

  /** Constructs an Exception with the specified detail message.
   *  Error number is set to unkownError.
   * @param s detail message.
   * @param markable zum Markieren des schuldigen Feldes
   */
  public OBException (String s, String markable) {
    super(getErrorMessage(s));
    errorNo = OBErrorNumber.unknownError;
    errorCode = s;
    replacements=new String[0];
    _markable = markable;
    setLogTimeStamp();
  }

  /** 
   * Constructs an exception with the specified error code.
   * If there is a default detail message for this error, it is set.
   * Otherwise the message is set to 'Unbekannter Fehler' 
   * @param errNo error code.
   */
  public OBException(int errNo) {
    super(getErrorMessage(errNo));
    errorNo = errNo;
    errorCode = "OB-" + errNo; //$NON-NLS-1$
    replacements = new String[0];
    _markable = ""; //$NON-NLS-1$
    _hintMsg = hints.get(String.valueOf(errNo));
    setLogTimeStamp();
  }


  /** 
   * Constructs an exception with the specified error code and replacement text[].
   * @param errNo The code of the error.
   * @param toReplace Array of Strings replacing '%' in the ErrorMessage
   */
  public OBException(int errNo, String toReplace) {
    this(errNo, new String[] {toReplace});
  }


  /**
   * @param errMsg Fehlermeldung
   * @param markable Zu setzendes Feld
   * @param hintMessageText Hinweistext zum Fehler, wird bei der Darstellung ausgewertet
   */
  public OBException(String errMsg, String markable, String hintMessageText) {
    this(errMsg);
    setHintMessage(hintMessageText);
    setMarkable(markable);
  }


  /** 
   * Constructs an exception with the specified error code and replacement text[].
   * @param errNo The code of the error.
   * @param toReplace Array of Strings replacing '%' in the ErrorMessage
   */
  public OBException(int errNo, String[] toReplace) {
    super(getErrorMessage(errNo, toReplace));
    errorNo = errNo;
    errorCode = "OB-" + errNo; //$NON-NLS-1$
    replacements = toReplace;
    _markable = ""; //$NON-NLS-1$
    setLogTimeStamp();
  }


  /** 
   * Constructs an exception with the specified error code, error message
   * and replacement text[].
   * @param s The error message.
   * @param toReplace Array of Strings replacing '%' in the ErrorMessage
   */
  public OBException(String s, String[] toReplace) {
    super(getErrorMessage(s, toReplace));
    errorCode = s;
    errorNo = OBErrorNumber.unknownError;
    replacements = toReplace;
    _markable = ""; //$NON-NLS-1$
    setLogTimeStamp();
  }

  /** Constructs an exception with the specified error code, error message
      and replacement text[].
   * @param s The error message.
   * @param toReplace Array of Strings replacing '%' in the ErrorMessage
   * @param markable zum Markieren des schuldigen Feldes
   */
  public OBException (String s, String[] toReplace, String markable) {
    super(getErrorMessage(s, toReplace));
    errorCode=s;
    errorNo = OBErrorNumber.unknownError;
    replacements=toReplace;
    _markable = markable;
    setLogTimeStamp();
  }
  
  /**
   * @param ex Exception
   */
  public OBException(Exception ex) {
    super(ex.getMessage());
    if (ex instanceof OBException) {
      OBException obe = (OBException) ex;
      errorNo = obe.getErrorNo();
      errorCode = obe.getErrorCode();
      replacements = obe.getReplacements();
      setLogTimeStamp(obe.getLogTimeStamp());
    }
    else {
      errorNo = OBErrorNumber.unknownError1;
      errorCode = "OB-"+OBErrorNumber.unknownError1; //$NON-NLS-1$
      replacements = new String[] {ex.getMessage() };
      setLogTimeStamp();
    }
    setCauseExption(ex);
  }

  
  /** 
   * Gets the error code of the exception.
   * @return error code.
   */
  public int getErrorNo() {
    return errorNo;
  }


  /** 
   * Gets the error code of the exception.
   * @return error code.
   */
  public String getErrorCode() {
    return errorCode;
  }


  /** 
   * Gets the error code replacements of the exception.
   * @return error code replacements.
   */
  public String[] getReplacements() {
    String[] retVal = new String[replacements.length];
    for (int i = 0; i < replacements.length; i++) {
      retVal[i] = replacements[i];
      if (retVal[i]!=null && retVal[i].indexOf("\n") >= 0) { //$NON-NLS-1$
        retVal[i] = retVal[i].replace('\n', ' ');
      }
    }
    return retVal;
  }


  /** 
   * Tests, if the error code of the exception equals the given one.
   * @param errorC ErrorCode
   * @return true, wenn gleich
   */
  public boolean equals(String errorC) {
    return getErrorCode().equals(errorC);
  }


  /** 
   * Tests, if the error code of the exception equals the given one.
   * @param errorN Fehlernummer
   * @return truue, wenn gleicher Code
   */
  public boolean equals(int errorN) {
    return getErrorNo() == errorN;
  }


  /** 
   * Returns the detail message of this Exception object including the error code.
   * @return Message including additional Comment
   */
  public String getMessage() {
    if (errorNo != OBErrorNumber.unknownError) {
      return getSeverityString() +" " + errorNo + ": " + super.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
    }
    else if (super.getMessage()!=null && !super.getMessage().equals(errorCode)) {
      return getSeverityString() +" " + errorCode + ": " + super.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
    }
    else {
      return super.getMessage();
    }
  }


  /** 
   * gibt den Stacktrace einer Exception als String zurueck 
   * @param t beliebiges Throwable
   * @return printStackTrace als String
   */
  static public String getStackTrace(Throwable t) {
    Writer sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    String s = sw.toString();
    return s;
  }

  
  /**
   * Liefert die verursachende Exception.
   * Achtung, bei der Serialisierung geht diese verloren, 
   * deshalb auch die zusaetzlichen String-Members (_causeExpMsg, _causeExpStackTrace).
   * @return causeException
   */
  public Exception getCauseExption() {
    return _causeExp;
  }

  
  /**
   * Liefert die Message der verursachenden Exception.
   * @return Message der verursachenden Exception
   */
  public String getCauseExptionMessage() {
    return _causeExpMsg;
  }

  
  /**
   * Liefert den Stacktrace der verursachenden Exception.
   * @return Stecktrace der verursachenden Exception
   */
  public String getCauseExptionStackTrace() {
    return _causeExpStackTrace;
  }

  
  /**
   * Setzt die verursachende Exception.
   * Dabei werden auch die String-Members _causeExpMsg und _causeExpStackTrace gesetzt.
   * @param cause zugrunde liegenede Exception
   */
  public void setCauseExption(Exception cause) {
    _causeExp=cause;
  }
  
  
  /**
   * @param stackTrace Stacktrace zugrunde liegenede Exception
   */
  public void setCauseStackTrace(String stackTrace) {
    _causeExpStackTrace=stackTrace;
  }


  /**
   * @param causeMsg Meldung zugrunde liegenede Exception
   */
  public void setCauseMessage(String causeMsg) {
    _causeExpMsg=causeMsg;
  }
  
  /**
   * @return the markable-Field
   */
  public String getMarkable() {
    return _markable;
  }


  /**
   * @param markable the markable-field to set
   */
  public void setMarkable(String markable) {
    this._markable = markable;
  }


  /**
   * Ueberschrieben, damit automatisch auch 
   * der Cause-Exception-Stacktrace ausgegeben werden kann.
   * @see java.lang.Throwable#printStackTrace()
   */
  public void printStackTrace() {
    super.printStackTrace();
    logger.error("",this); //$NON-NLS-1$
    if (_causeExpStackTrace!=null && _causeExpStackTrace.length()!=0) {
      logger.error("OBException.printStackTrace() :\nUr-Exception-Stacktrace:\n"+_causeExpStackTrace); //$NON-NLS-1$
    }
  }


  /**
   * Liefert den Hinweis-Text zu einer Exception.
   * @return Hinweis-Text zur Exception
   */
  public String getHintMessage() {
    return _hintMsg;
  }


  /**
   * Setzt den Hinweis-Text zu einer Exception.
   * @param hintMessage Hinweis-Text fuer die Fehlerdarstellung.
   */
  public void setHintMessage(String hintMessage) {
    if (hintMessage==null) {
      _hintMsg=""; //$NON-NLS-1$
    } 
    else {
      _hintMsg=hintMessage;
    }
  }

  /* ------- Zeitpunkt der Initialisierung ----------------------------------- */
  
  /**
   * Liefert den Time-Stamp des Auftretens (entspricht dem des Logs)
   * @return Initialisierungszeitpunkt
   */
  public String getLogTimeStamp() { return _logTimeStamp; }
  /**
   * Setzt den Zeitpunkt des Auftretens (ACHTUNG: Mit Vorsicht zu verwenden!)
   * @param logTimeStamp Zeitpunkt
   */
  public void setLogTimeStamp(String logTimeStamp) { this._logTimeStamp=logTimeStamp; }

  /**
   * Setzt den Zeitstempel auf Now
   */
  public void setLogTimeStamp() { this._logTimeStamp=getTimeStamp(); }
  
  /**
   * Erzeugt den aktuellen Zeitstempel
   * @return aktueller Zeitstempel
   */
  private String getTimeStamp() {
    String retVal = ""; //$NON-NLS-1$
    Calendar cal = Calendar.getInstance();
    retVal  = String.valueOf(cal.get(Calendar.DAY_OF_MONTH)+100).substring(1) + "."; //$NON-NLS-1$
    retVal += String.valueOf(cal.get(Calendar.MONTH)+101).substring(1) + "."; //$NON-NLS-1$
    retVal += String.valueOf(cal.get(Calendar.YEAR)+10000).substring(1) + " "; //$NON-NLS-1$
    retVal += String.valueOf(cal.get(Calendar.HOUR_OF_DAY)+100).substring(1) + ":"; //$NON-NLS-1$
    retVal += String.valueOf(cal.get(Calendar.MINUTE)+100).substring(1) + ":"; //$NON-NLS-1$
    retVal += String.valueOf(cal.get(Calendar.SECOND)+100).substring(1);
    return retVal;
  }
  
  /* ------- Schwere des Fehlers ----------------------------------- */
  
  /** Liefert die Schwere des Fehlers 
   * @return Schwere des Fehlers
   */
  public int getSeverity() { return _severity;}
  
  /** Liefert die String-Version der Schwere des Fehlers 
   * @return Severity als String
   */
  public String getSeverityString() {
    if (_severity>=0 && _severity<Severity.toString.length) {
      return Severity.toString[_severity];
    }
    else {
      return "Fehler"; //$NON-NLS-1$
    }
  }
  
  /** Setzt die Schwere des Fehlers
   * @param severity Schwere des Fehlers 
   */
  public void setSeverity(int severity) {
    _severity=severity;
  }
}
