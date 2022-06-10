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
package gip.base.common;

import java.io.Serializable;
import java.util.*;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public abstract class OBObject implements OBCheckListener, Serializable {

  private transient static Logger logger = Logger.getLogger(OBObject.class);

  public static final String[] hiddenAtts = {"password", //$NON-NLS-1$
                                             "userPass", //$NON-NLS-1$
                                             "encryptPwd", //$NON-NLS-1$
                                             "tan", //$NON-NLS-1$
                                             "pin", //$NON-NLS-1$
                                             "oldPassword", //$NON-NLS-1$
                                             "otpPin", //$NON-NLS-1$
                                             "voipPin", //$NON-NLS-1$
                                             "passwordRepeat", //$NON-NLS-1$
                                             "connectionPassword", //$NON-NLS-1$
                                             "pwd",  //$NON-NLS-1$
                                             "cruPassword",  //$NON-NLS-1$
                                             "rngPassword",  //$NON-NLS-1$
                                             "certPassword",  //$NON-NLS-1$
                                             "oldCertPassword"}; //$NON-NLS-1$

  public static final String START_PROJECT_SCHEMA = "<##projectSchema:"; //$NON-NLS-1$
  public static final String END_PROJECT_SCHEMA = "##>"; //$NON-NLS-1$

  /** Vektor mit Attributen der Tabelle */
  public OBAttribute[] attArr = null;

  /** Primary Key Attribut */
  public OBAttribute primaryKeyAtt;
  /** Name des Primary Keys */
  public String primaryKey = ""; //$NON-NLS-1$
  /** Kommentar zur Tabelle */
  public String comment = ""; //$NON-NLS-1$
  /** Ergaenzung zum whereClause */
  public String wcModifier = ""; //$NON-NLS-1$
  /** Ordnungsbedingung fuer Selection */
  private String orderClause = ""; //$NON-NLS-1$

  private int _ignoreFirstRows = 0;
  
  private boolean _caseSensitive = true;
  
  /** Name der Tabelle, wird in der abgeleiteten Klasse gesetzt */
  public String tableName;

  /** Trenner fuer getIdentifier */
  protected final String SEPSTRING = ", "; // Trenner fuer getIdentifier //$NON-NLS-1$

  /** Datumsformat (Default: ohne Uhrzeit */
  private String format = OBConstants.NLS_DATE_FORMAT;

  // ---------------------------------------------------------------------------
  // -------- Konstruktor ----------------------------------------------------  
  // ---------------------------------------------------------------------------

  /** Standard-Konstruktor */
  public OBObject() { // ntbd
  }

  // ---------------------------------------------------------------------------
  // ------- Hilfsmethoden -----------------------------------------------------
  // ---------------------------------------------------------------------------
  private String pimpWCModifier(String _wcModifier) {
    if (_wcModifier==null || _wcModifier.trim().length()==0) {
      return ""; //$NON-NLS-1$
    }
    String compStr = _wcModifier.trim().substring(0,4).toUpperCase();
    if (compStr.startsWith("AND ") ||  //$NON-NLS-1$
        compStr.startsWith("OR ")) { //$NON-NLS-1$
      return _wcModifier;
    }
    else {
      return " AND " + _wcModifier; //$NON-NLS-1$
    }
  }
  

  /** Setzt eine zus&auml;tzliche Selektionsbedingung
      @param _wcModifier die zus&auml;tzliche Selektionsbedingung (muss mit AND/OR beginnen, sonst wird automatisch ein AND vorangestellt)
  */
  public void setWCModifier(String _wcModifier) {
    this.wcModifier = pimpWCModifier(_wcModifier);
  }

  /** Erfragt zus&auml;tzliche Selektionsbedingung
      @return die zus&auml;tzliche Selektionsbedingung
  */
  public String getWCModifier() {
    return wcModifier;
  }

  /** Ergaenzt eine zus&auml;tzliche Selektionsbedingung (Die alte wird nicht ueberschrieben) Entspricht dem Aufruf appendWCModifier(_wcModifier, true);
   * @param _wcModifier die zus&auml;tzliche Selektionsbedingung (muss mit AND beginnen)
   * @return String
   */
  public String appendWCModifier(String _wcModifier) {
    return appendWCModifier(_wcModifier, true);
  }

  /** Ergaenzt eine zus&auml;tzliche Selektionsbedingung (Die alte wird nicht ueberschrieben)
   * @param _wcModifier die zus&auml;tzliche Selektionsbedingung (muss mit AND beginnen)
   * @param pimp Soll geprueft werden, ob der wcModifier mit AND/OR beginnt und im Zweifel AND vorangestellt werden 
   * @return String
   */
  public String appendWCModifier(String _wcModifier, boolean pimp) {
    if (pimp) {
      this.wcModifier += pimpWCModifier(_wcModifier);
    }
    else {
      this.wcModifier += _wcModifier;
    }
    return this.wcModifier;
  }

  /** Setzt die Reihenfolge, in der Daten selektiert werden sollen
      @param _orderClause die Ordnungsbedingung (muss mit ORDER BY beginnen)
  */
  public void setOrderClause(String _orderClause) {
    this.orderClause = _orderClause;
  }

  /** Erfragt die Reihenfolge, in der Daten selektiert werden sollen
      @return die Ordnungsbedingung
  */
  public String getOrderClause() {
    return orderClause;
  }

  /** Erfragt den Tabellennamen
      @return Tabellenname
  */
  public String getTableName() {
    return tableName;
  }
  
  /** Erfragt Filter-Option, wie viele Elemente ueberlesen werden 
   * @return int */
  public int getIgnoreFirstRows() { return _ignoreFirstRows;}
  /** Setzt Filter-Option, wie viele Elemente ueberlesen werden 
   * @param ignoreFirstRows Zu ignorierende Zeilen */
  public void setIgnoreFirstRows(int ignoreFirstRows) { this._ignoreFirstRows = ignoreFirstRows;}
  
  /**
   * Fuehrt ein parseInt durch. Im Gegensatz zu Integer.parseInt wird null oder "" zu OBAttribute.NULL geparst.
   * @param toParse zu parsender String
   * @return int Wert oder OBAttribute.NULL
   * @throws OBException NumberFormatException
   */
  public static int parseInt(String toParse) throws OBException {
    if (toParse==null || toParse.trim().length()==0) {
      return OBAttribute.NULL;
    }
    else {
      try {
        return Integer.parseInt(toParse.trim());
      }
      catch (NumberFormatException e) {
        logger.error("error parsing int", e);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.numberFormatException1, new String[] {toParse});
      }
    }
  }


  /**
   * Fuehrt ein parseLong durch. Im Gegensatz zu Long.parseLong wird null oder "" zu OBAttribute.NULL geparst.
   * @param toParse zu parsender String
   * @return long Wert oder OBAttribute.NULL
   * @throws OBException NumberFormatException
   */
  public static long parseLong(String toParse) throws OBException {
    if (toParse==null || toParse.trim().length()==0) {
      return OBAttribute.NULL;
    }
    else {
      try {
        return Long.parseLong(toParse.trim());
      }
      catch (NumberFormatException e) {
        logger.error("error parsing long", e);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.numberFormatException1, new String[] {toParse});
      }
    }
  }


  /** Erfragt den Tabellennamen in SQL
   *  Wird in den abgeleiteten Klassen ueberschrieben 
   *  @return Tabellenname
   */
  public String getSQLName() {
    return "i"+tableName; //$NON-NLS-1$
  }

  /** Erfragt das Projekt des Objektes, aus dem das zugehoerige DB-Schema gesucht wird.
   *  Wird in den abgeleiteten Klassen ueberschrieben 
   *  @return Project
   */
  public String getProjectSchema() {
    return "ipnet"; //$NON-NLS-1$
  }
  
  /** Setzt das Datumsformat.
      @param newFormat Datumsformat in Oracle-Notation
   */
  public void setDateFormat(String newFormat) {
    format = newFormat;
  }

  /** Liefert das Datumsformat.
      @return Datumsformat in Oracle-Notation
   */
  public String getDateFormat() {
    return format;
  }

  // ---------------------------------------------------------------------------
  // ------- Zu ueberschreibende Methoden --------------------------------------
  // ---------------------------------------------------------------------------

  /** Sind in den Gen-Klassen implementiert
      Liefert komma-getrennte Liste der Attribute, wie sie f&uuml;r ein Insert/Update gebraucht werden.
      @return Attribut-Liste
   */
  protected String tableDetails() {
    return ""; //$NON-NLS-1$
  }

  /** Sind in den Gen-Klassen implementiert.
      Liefert komma-getrennte Liste der Attribute, wie sie f&uuml;r ein Select gebraucht werden.
      @return Attribut-Liste
   */
  public String tableSelect() {
    return ""; //$NON-NLS-1$
  }

  /** Von Gen-Klassen implementiert. Liefert die Anzahl der Attribute.
      @return Anzahl der Atttribute
   */
  public int numAttr() {
    return -1;
  }

  // ---------------------------------------------------------------------------
  // ------- Kleinere Methoden -------------------------------------------------
  // ---------------------------------------------------------------------------

  /** Setzt den Prim&auml;rschl&uuml;ssel auf den angegebenen Wert
      @param pk zu setzender Wert des Prim&auml;rschl&uuml;ssels
   * @throws OBException Wenn es keinen PK gibt oder der Wert Null ist
   */
  public void setPrimaryKey(long pk) throws OBException {
    primaryKeyAtt.setValue(pk);
  }

  /** Liefert Wert des Prim&auml;rschl&uuml;ssels.
      @return Wert des Prim&auml;rschl&uuml;ssels
      @throws OBException Es ist kein PK bekannt
   */
  public long getPrimaryKey() throws OBException {
    if (primaryKeyAtt == null || primaryKeyAtt.isNull()) {
      throw new OBException(OBException.OBErrorNumber.pkUnkown);
    }
    return primaryKeyAtt.getLongValue();
  }

  /** Liefert den Namen des Prim&auml;rschl&uuml;ssels.
      @return Name des Prim&auml;rschl&uuml;ssels
   */
  public String getPrimaryKeyName() {
    return primaryKey;
  }
  
  /** Erfragt, ob das PK-Attribute NULL ist
   * @return true, wenn pk null
   * @throws OBException wenn kein PK bekannt ist */
  public boolean isPrimaryKeyNull() throws OBException {
    if (primaryKeyAtt == null) {
      throw new OBException(OBException.OBErrorNumber.pkUnkown);
    }
    return primaryKeyAtt.isNull(); 
  }

  /** Erfragt, ob das PK-Attribute NOT NULL ist
   * @return true, wenn pk not null
   * @throws OBException wenn kein PK bekannt ist */
  public boolean isPrimaryKeyNotNull() throws OBException {
    if (primaryKeyAtt == null) {
      throw new OBException(OBException.OBErrorNumber.pkUnkown);
    }
    return primaryKeyAtt.isNotNull(); 
  }

  /** Loescht die Werte aller Attribute der Tabelle.
   */
  public void clear() {
    OBAttribute a;
    for (int j = 0; j < attArr.length; j++) {
      a = attArr[j];
      a.clear();
      if (primaryKeyAtt != null && a == primaryKeyAtt) {
        a.setIgnored(true);
      }
    }
  }

  /** Dupliziert das Object
   *  @return dupliziertes Objekt
      @throws CloneNotSupportedException Wenn beim Clonen etwas misslingt
   */
  public OBObject clone() throws CloneNotSupportedException {
    Class<? extends OBObject> cl = this.getClass();
    OBObject to;
    try {
      to = cl.newInstance();
      to.copy(this);
    }
    catch (Exception e) {
      throw new CloneNotSupportedException(e.getMessage());
    }
    return to;
  }

  /**
   * Vergleicht das Objekt mit einem anderen (case-insensitive). 
   * Wird nach einer numerischen Spalte sortiert, so werden die Werte erst in eine Zahl verwandelt.
   * @param anotherOBObject Anderes Objekt vom selben Typ
   * @param columnName Name der Vergleichsspalte
   * @return Bei String-Typen das, was String.compareTo auf den Werten der Spalte liefert, bei Zahlen-Typen mit numerischem Vergleich
   * @throws NumberFormatException bei numerischen Spalten und nichtnumerischen Werten
   * @throws OBException Sonstiger Fehler
   */
  public int compareTo(OBObject anotherOBObject, String columnName) throws NumberFormatException, OBException {
    boolean b_numeric = false;
    String s1 = getValue(columnName).toUpperCase();
    String s2 = anotherOBObject.getValue(columnName).toUpperCase();
    int type =  getType(columnName);
    if (type == OBConstants.INTEGER || type == OBConstants.LONG || type == OBConstants.DOUBLE) {
      b_numeric = true;
    }
    if (b_numeric) {
      double d1 = Double.parseDouble(s1);
      double d2 = Double.parseDouble(s2);
      if (d1<d2) { return -1; }
      else if (d1>d2) {return 1; }
      return 0;
    }
    else {
      return s1.compareTo(s2);
    }
  }
  

  /** Kopiert die Werte eines anderen Objektes in dieses.
      Hier werden lockRow, pk, und inDate NICHT mit kopiert.
      @param copy Das zu kopierende Objekt
  */
  public void copy(OBObject copy) {
    OBAttribute cattr;
    OBAttribute attr;
    for (int j = 0; j < attArr.length; j++) {
      attr = attArr[j];
      if (!(attr.name.equals("inDate")) //$NON-NLS-1$
        && !(attr.name.equals(primaryKey))
        && !(attr.name.equals("lockRow")) //$NON-NLS-1$
        && !(attr.name.equals("timestamp"))) { //$NON-NLS-1$
        cattr = copy.attArr[j];
        attr.setValue(cattr.getValue());
        if(attr.type == OBConstants.BLOB) {
          attr.bvalue = (cattr.bvalue.clone());
        }
      }
    }
  }

  
  /**
   * Kopiert die Werte des uebergebenen Objektes in das aufrufende Objekt. 
   * Hier werden u.a. auch lockRow, pk, und inDate kopiert.
   * @param copyTemplate Die Vorlage der Kopie.
   * @throws OBException Fehlermeldung
   */
  public void copyAll(OBObject copyTemplate) throws OBException {
    OBAttribute attr;
    OBAttribute cpAttr;
    for (int j = 0; j < attArr.length; j++) {
      if (!attArr[j].name.equals(copyTemplate.attArr[j].name)) {
        throw new OBException(OBException.OBErrorNumber.noSuchElementException1);
      }
      attr = attArr[j];
      cpAttr = copyTemplate.attArr[j];
      attr.setValue(cpAttr.getValue());
      if(attr.type == OBConstants.BLOB) {
        attr.bvalue = (cpAttr.getByteArrayValue().clone());
      }
    }
  }

  
  /** Kopiert Werte von Attributen eines OBObjects in ein anderes.
      Dies geschieht auf Basis von name-matching, d.h. kopiert die
      Werte, falls die Namen der Attribute uebereinstimmen.
   * @param copyFrom Vorlage zum Kopieren
   */
  public void copyAttribValuesWithSameNameFromOtherObject(OBObject copyFrom) {
    OBAttribute a1,a2;
    for(int i=0; i<this.attArr.length; i++) {
      for(int j=0; j<copyFrom.attArr().length; j++) {
        a1 = this.attArr[i];
        a2 = copyFrom.attArr[j];
        if(a1.name.equals(a2.name)) {
          a1.setValue(a2.value);
          break;
        }
      }
    }
  }
  
  
  /** Zwei Objecte sind gleich, wenn sie in allen Spalten au&szlig;er
      inDate, changeDate und lockRow &uuml;bereinstimmen
   * @param obj2 Vergleichsobjekt
   * @return true, wenn gleich
   */
  public boolean equals(Object obj2) {
    if (!(obj2 instanceof OBObject)) {
      return false;
    }
    OBObject otherObj = (OBObject)obj2;
    OBAttribute cattr;
    OBAttribute attr;
    for (int j = 0; j < attArr.length; j++) {
      attr = attArr[j];
      if (!(attr.name.equals("inDate")) //$NON-NLS-1$
        && !(attr.name.equals(primaryKey))
        && !(attr.name.equals("lockRow")) //$NON-NLS-1$
        && !(attr.name.equals("timestamp")) //$NON-NLS-1$
        && !(attr.name.equals("changeDate"))) { //$NON-NLS-1$
        cattr = otherObj.attArr[j];
        if (!attr.value.equals(cattr.value)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Ueberschreibt die Object-Methode
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    OBAttribute attr;
    int hash = 0;
    for (int j = 0; j < attArr.length; j++) {
      attr = attArr[j];
      if (!("inDate".equals(attr.name)) //$NON-NLS-1$
        && !(primaryKey.equals(attr.name))
        && !("lockRow".equals( attr.name )) //$NON-NLS-1$
        && !("timestamp".equals(attr.name)) //$NON-NLS-1$
        && !("changeDate".equals(attr.name))) { //$NON-NLS-1$
        if (attr.value != null) {
          hash += attr.value.hashCode();
        }
      }
    }
    return hash;
  }
  
  /** Gibt das Attribut-Array zur&uuml;ck.
      @return ARRAY mit Attributen
   */
  public OBAttribute[] attArr() {
    return attArr;
  }

  /** Setzt das Attribut-Array.
      @param aa ARRAY mit Attributen
   */
  public void setAttArr(OBAttribute[] aa) {
    attArr=aa;
  }

  /** Gibt die Attribute als Vector zur&uuml;ck.
      @return Vector mit Attributen
   */
  public Vector<OBAttribute> attVector() {
    Vector<OBAttribute> v = new Vector<OBAttribute>(numAttr());
    for (int j = 0; j < attArr.length; j++) {
      v.addElement(attArr[j]);
    }
    return v;
  }

  // ---------------------------------------------------------------------------
  // ------- Print- und Ausgabe-Methoden ---------------------------------------
  // ---------------------------------------------------------------------------

  /** "Multipliziert" ein Zeichen, z.B. multChar('#', 3) = "###"
      @param c     Zeichen das "multipliziert" werden soll
      @param count Anzahl, wie oft das Zeichen multipliziert werden soll
      @return      String des mutl. Zeichens
  */
  private static String multChar(char c, int count) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < count; i++) {
      sb.append(c);
    }
    return sb.toString();
  }

  /** Liefert eine gut lesbare Repraesentation eines OBObjects 
   * @return String-Repraesentation */
  public String toString() {
    int CUT_AT = 40;
    String indent = multChar(' ', 2);
    String dump;
    OBAttribute att;

    // cout max len of key + value
    int max_key = -1;
    int max_val = "<not set>".length(); //$NON-NLS-1$
    int curr_key, curr_val;
    if (attArr()!=null) {
      for (int i = 0; i < attArr().length; i++) {
        att = attArr()[i];
        curr_key = att.name.length();
        curr_val = att.value.length();
        if (curr_key > max_key) {
          max_key = curr_key;
        }
        if (curr_val > max_val) {
          max_val = curr_val;
        }
      }
    }
    if (max_val > CUT_AT) {
      max_val = CUT_AT;
    }
    // get only last part of the (fully qualified) classname of this object
    StringTokenizer st = new StringTokenizer(getClass().getName(), "."); //$NON-NLS-1$
    String clsname = "<unkown>"; //$NON-NLS-1$
    while (st.hasMoreElements())
      clsname = st.nextToken();

    String header =
      indent
        + "+" //$NON-NLS-1$
        + (multChar('-', max_key + max_val + 5))
        + "+\n" //$NON-NLS-1$
        + indent
        + "| " //$NON-NLS-1$
        + clsname
        + (multChar(' ', max_key + max_val + 3 - clsname.length()))
        + " |\n" //$NON-NLS-1$
        + indent
        + "+" //$NON-NLS-1$
        + (multChar('-', max_key + max_val + 5))
        + "+\n"; //$NON-NLS-1$
    String footer =
      indent + "+" + (multChar('-', max_key + max_val + 5)) + "+\n"; //$NON-NLS-1$ //$NON-NLS-2$

    // dump the object
    dump = ""; //$NON-NLS-1$
    dump = dump + header;
    if (attArr()!=null) {
      for (int i = 0; i < attArr().length; i++) {
        att = attArr()[i];
        if (att.name.equals("lockRow") //$NON-NLS-1$
          || att.name.equals("remark") //$NON-NLS-1$
          || att.name.equals("inDate") //$NON-NLS-1$
          || att.name.equals("changeDate")) { //$NON-NLS-1$
          continue;
        }
        String val = "<unknown>"; //$NON-NLS-1$
        try {
          if (att.value.length() >= CUT_AT - 4) {
            val = att.value.substring(0, CUT_AT - 4) + " ..."; //$NON-NLS-1$
          }
          else {
            val = att.value;
          }
          if (attIsHidden(att.name)) { 
            val = multChar('*', val.length());
          }
          dump = dump + formatLine(indent, max_key, att.name, max_val, val);
        }
        catch (Exception e) {
          dump =
            dump + formatLine(indent, max_key, att.name, max_val, "<not set>"); //$NON-NLS-1$
        }
      }
    }
    else {
      dump += "Keine Attribute vorhanden"; //$NON-NLS-1$
    }
    dump = dump + footer;

    return dump;
  }


  /** 
   * Hilfsmethode fuer printString 
   * @param indent
   * @param max_key
   * @param name
   * @param max_val
   * @param val
   * @return
   */
  private static String formatLine(String indent, int max_key, String name, int max_val, String val) {
    StringBuffer format = new StringBuffer(indent + "| ");  // beginning of line //$NON-NLS-1$
    format.append(name).append(multChar(' ', max_key - name.length())).append(" | "); // name part of line //$NON-NLS-1$
    format.append(multChar(' ', max_val - val.length())).append(val).append(" |\n"); //$NON-NLS-1$
    return format.toString();
  }


  /** Gibt alle Attribute und deren Werte auf der Konsole aus.
      Es wird nicht in den OBLog.log.debug-Channel geschrieben.
      Nur fuer DEBUG-Zwecke der Entwickler gedacht.
   * @param header Kopfzeile
  */
  public void displayForDebug(String header) {
    logger.debug(header+ "\n"+toString()); //$NON-NLS-1$
  }


  /** Ruft displayForDebug("") auf
   */
  public void displayForDebug() {
    displayForDebug(""); //$NON-NLS-1$
  }


  /** Erzeugt die Ausgabe eines Stack-Traces
   */
  public static void printStackTrace() {
    try {
      throw new OBException("Hier bin ich"); //$NON-NLS-1$
    }
    catch (Exception e) {
      logger.error("Hier bin ich", e); //$NON-NLS-1$
    }
  }


  /** Liefert eindeutigen Distinguished-Name eines Objektes.
      Also ein Bezeichnung der das Objekt im gesammten Datenbestand
      eindeutig beschreibt. Moeglichst fuer den Anwender interpretierbar.
      Diese Implementierung hier ist icht fuer den Anwender interpretierbar,
      da der Primary-Key verwendet wird, der nicht an der GUI sichtbar ist.
   * @return Eindeutiger Identifier
   * @throws OBException bei Fehler
  */
  public String getIdentifier() throws OBException {
    try {
      if (primaryKeyAtt.getValue().length() > 0) {
        return tableName + " DB-Id: " + primaryKeyAtt.getValue(); //$NON-NLS-1$
      }
      else {
        String attr = toString();
        return tableName + " Attribute: \n" + attr; //$NON-NLS-1$
      }
    }
    catch (Exception e) {
      logger.debug("error getting identifier", e);//$NON-NLS-1$
      return tableName + "No Identifier created"; //$NON-NLS-1$
    }
  }

  private static boolean attIsHidden(String attName) {
    for (int i = 0; i < hiddenAtts.length; i++) {
      if (attName.equalsIgnoreCase(hiddenAtts[i])) {
        return true;
      }
    }
    return false;
  }
  
  /** Versteckt sicherheitsrelevante Werte der Tabelle in der Ausgabe.
      Diese Werte werden korrekt in der DB abgelegt und
      nur fuer den Ausgabe-String maskiert.
      @param in Ausgabe-String
      @return Maskierter Ausgabe-String
  */
  public String hide(String in) {
    return hide(in, attArr);
  }

  /**
   * Versteckt sicherheitsrelevante Werte der Tabelle in der Ausgabe. Diese
   * Werte werden korrekt in der DB abgelegt und nur fuer den Ausgabe-String
   * maskiert.
   * 
   * @param in Ausgabe-String
   * @param attArr Liste der Attribute, um zu wissen, ob der Wert versteckt werden soll.
   * @return Maskierter Ausgabe-String
   */
  public static String hide(String in, OBAttribute[] attArr) {
    String retVal = in;
    String search;
    StringBuffer replace;
    OBAttribute a;

    for (int j = 0; j < attArr.length; j++) {
      a = (attArr[j]);
      if (a.value.length() > 0 && attIsHidden(a.name)) {
        search = a.value;
        replace = new StringBuffer(50);
        replace.append("'"); //$NON-NLS-1$
        for (int c = 0; c < search.length(); c++) {
          replace.append("*"); //$NON-NLS-1$
        }
        replace.append("'"); //$NON-NLS-1$

        int index = 0;
        while ((index = retVal.indexOf("'" + search + "'")) != -1) { //$NON-NLS-1$ //$NON-NLS-2$
          retVal = retVal.substring(0, index)
                   + replace.toString()
                   + retVal.substring(index + search.length() + 2,
                                      retVal.length());
        }
      }
    }
    return retVal;
  }

  // ---------------------------------------------------------------------------
  // ------- Fuer das Select benoetigte Methoden -------------------------------
  // ---------------------------------------------------------------------------

  // ******* Hilfsfunktionen ************************************************

  /** Liefert ein Array der OBAttribute und fuegt am Schluss noch den PK hinzu.
      @param name Namen der Attribute die als OBAttribute-Array zurueckgeliefert werden soll.
      @return OBAttribute-Array plus PK-OBAttribute (kann dann auch ein NULL-Pointer sein)
   * @throws OBException Fehler
  */
  public OBAttribute[] getOBAttributArrayByName(String[] name) throws OBException {
    OBAttribute[] attrs = new OBAttribute[name.length + 1];
    for (int i = 0; i < name.length; i++) {
      for (int j = 0; j < attArr.length; j++) {
        if (attArr[j].name.equals(name[i])) {
          attrs[i] = attArr[j];
          break;
        }
      }
    }
    if (null==primaryKeyAtt) {
      throw new OBException(OBException.OBErrorNumber.noPrimaryKey1, new String[] {getTableName()});
    }
    attrs[name.length] = primaryKeyAtt;
    return attrs;
  }

  public OBAttribute getOBAttributeByName(String name) {
    for (int j = 0; j < attArr.length; j++) {
      if (attArr[j].name.equals(name)) {
        return attArr[j];
      }
    }
    return null;
  }
  
  /** 
   * Convenience-Methode f&uuml;r getWhereClauseFromFilter(colNames[],searchCaseSensitive)
   * @return Whereclause
   */
  public String getWhereClauseFromFilter() {
    return getWhereClauseFromFilter(null, true);
  }

  /** Baut einen String fuer die Where-Bedingung eines SQL Statements auf.
      Ist der String = "", so wird er im whereClause nicht verwendet,
      da "" in der Datenbank null entspricht.
      @param colNames Die Spalten, die im Where beruecksichtigt werden sollen.
      @param searchCaseSensitive Gibt an, ob Gross/Kleinschreibung wichtig ist
      @return SQL-Where-Clause
  */
  public String getWhereClauseFromFilter(String[] colNames,
                                         boolean searchCaseSensitive) {
    // Umbenannt wegen Default-Access statt public
    //  public String getWhereClause(boolean searchCaseSensitive) {
    String wc = ""; //$NON-NLS-1$
    String tmp = ""; //$NON-NLS-1$
    boolean flag;
    OBAttribute a;

    boolean withANDOR = false;
    String andOrString = " OR "; //$NON-NLS-1$
    boolean withWildcard = false;
    boolean checkForNull = false;
    String tmp1 = ""; //$NON-NLS-1$
    String tmp2 = ""; //$NON-NLS-1$
    int index = -1;
    String tmpCompOp = CompOperator.like;
    String tmp1CompOp = CompOperator.like;
    String tmp2CompOp = CompOperator.like;

    try {
      for (int i = 0; i < attArr.length; i++) {
        flag = false;
        a = attArr[i];
        tmp = a.value;

        // wird attribut in whereclause benutzt?
        if (colNames == null) {
          flag = true;
        }
        else {
          for (int j = 0; j < colNames.length; j++) {
            if (a.name.equals(colNames[j])) {
              flag = true;
            }
          }
        }
        if (flag == true) {
          // Test, ob auf (NOT) NULL abgefragt werden soll
          checkForNull =
            a.getCompOp().equals(CompOperator.isNull)
              || a.getCompOp().equals(CompOperator.isNotNull);
          // "" in Java entspricht null in der DB, is(Not)Null braucht keinen Wert
          if (tmp.equals("!*") || tmp.equals("!%")) { //$NON-NLS-1$ //$NON-NLS-2$
            if (wc.length() == 0)
              wc = wc + " WHERE "; //$NON-NLS-1$
            else
              wc = wc + " AND "; //$NON-NLS-1$

            wc = wc + a.name + " IS NULL "; //$NON-NLS-1$
          }
          else if (tmp.length() > 0 || checkForNull) {
            // wenn auf (NOT) NULL abgefragt werden soll, braucht im Attribut nichts drinzustehen
            if (checkForNull) {
              tmp = ""; //$NON-NLS-1$
            }

            // am anfang where, dann immer and
            if (wc.length() == 0)
              wc = wc + " WHERE "; //$NON-NLS-1$
            else
              wc = wc + " AND "; //$NON-NLS-1$

            withANDOR = false;
            andOrString = " OR "; //$NON-NLS-1$
            withWildcard = false;

            tmp1 = ""; //$NON-NLS-1$
            tmp2 = ""; //$NON-NLS-1$
            index = -1;

            if (a.getCompOp().equals(CompOperator.defaultOp)) {
              tmpCompOp = CompOperator.like;
              tmp1CompOp = CompOperator.like;
              tmp2CompOp = CompOperator.like;
            }
            else {
              tmpCompOp = a.getCompOp();
              tmp1CompOp = a.getCompOp();
              tmp2CompOp = a.getCompOp();
            }
            if (tmpCompOp.equals(CompOperator.like) ||
                tmpCompOp.equals(CompOperator.notLike)) {
              if (a.getValue().indexOf('%')==-1 && 
                  a.getValue().indexOf('*')==-1 && 
                  a.getValue().indexOf('_')==-1) {
                if (tmpCompOp.equals(CompOperator.like)) {
                  tmpCompOp = CompOperator.equal;
                  tmp1CompOp = CompOperator.equal;
                  tmp2CompOp = CompOperator.equal;
                } 
                if (tmpCompOp.equals(CompOperator.notLike)) {
                  tmpCompOp = CompOperator.notEqual;
                  tmp1CompOp = CompOperator.notEqual;
                  tmp2CompOp = CompOperator.notEqual;
                } 
              }
            }
            
            // Suche mit "oder" ermoeglichen
            if ((index = tmp.indexOf("||")) != -1) { //$NON-NLS-1$
              withANDOR = true;
              andOrString = " OR "; //$NON-NLS-1$
              tmp1 = tmp.substring(0, index);
              tmp2 = tmp.substring(index + 2, tmp.length());
              if (tmp1.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmp1CompOp = CompOperator.negateCompOp(tmp1CompOp);
                tmp1 = tmp1.substring(1, tmp1.length()); // ! Zeichen verwerfen
              }
              if (tmp2.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmp2CompOp = CompOperator.negateCompOp(tmp2CompOp);
                tmp2 = tmp2.substring(1, tmp2.length()); // ! Zeichen verwerfen
              }
            }
            else if ((index = tmp.indexOf("&&")) != -1) { //$NON-NLS-1$
              withANDOR = true;
              andOrString = " AND "; //$NON-NLS-1$
              tmp1 = tmp.substring(0, index);
              tmp2 = tmp.substring(index + 2, tmp.length());
              if (tmp1.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmp1CompOp = CompOperator.negateCompOp(tmp1CompOp);
                tmp1 = tmp1.substring(1, tmp1.length()); // ! Zeichen verwerfen
              }
              if (tmp2.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmp2CompOp = CompOperator.negateCompOp(tmp2CompOp);
                tmp2 = tmp2.substring(1, tmp2.length()); // ! Zeichen verwerfen
              }
            }
            else {
              tmp = tmp.trim(); // Leerzeichen verwerfen
              if (tmp.length() > 0
                && tmp.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmpCompOp = CompOperator.negateCompOp(tmpCompOp);
                tmp = tmp.substring(1, tmp.length()); // ! Zeichen verwerfen
              }
            }

            // bei Integers nur mit like % suchen, wenn ein Wildcard-Zeichen drin ist
            if (a.type == OBConstants.INTEGER
              || a.type == OBConstants.LONG
              || a.type == OBConstants.DOUBLE
              || a.type == OBConstants.BOOLEAN) {
              if (tmp.indexOf('%') != -1
                || tmp.indexOf('*') != -1
                || tmp.indexOf('_') != -1) {
                withWildcard = true;
                // Test, ob Wildcards ueberhaupt zulaessig sind
                if (!a.getCompOp().equals(CompOperator.defaultOp)
                  && !a.getCompOp().equals(CompOperator.like)
                  && !a.getCompOp().equals(CompOperator.notLike)) {
                  throw new OBException("Wildcard not allowed"); //$NON-NLS-1$
                }
              }
              else {
                // "like" durch "=" und  "not like" durch "!=" ersetzen,
                // alle anderen Operatoren bleiben !!!!
                if (tmpCompOp.equals(CompOperator.like)) {
                  tmpCompOp = CompOperator.equal;
                }
                else if (tmpCompOp.equals(CompOperator.notLike)) {
                  tmpCompOp = CompOperator.notEqual;
                }

                if (tmp1CompOp.equals(CompOperator.like)) {
                  tmp1CompOp = CompOperator.equal;
                }
                else if (tmp1CompOp.equals(CompOperator.notLike)) {
                  tmp1CompOp = CompOperator.notEqual;
                }

                if (tmp2CompOp.equals(CompOperator.like)) {
                  tmp2CompOp = CompOperator.equal;
                }
                else if (tmp2CompOp.equals(CompOperator.notLike)) {
                  tmp2CompOp = CompOperator.notEqual;
                }
              }
            }

            // unterscheide die verschiedenen typen
            // int, date und string
            if (a.type == OBConstants.INTEGER
              || a.type == OBConstants.BOOLEAN
              || a.type == OBConstants.LONG
              || a.type == OBConstants.DOUBLE) {
              if (checkForNull) {
                // hier kein rechter Operand
                wc = wc + a.name + tmpCompOp;
              }
              else if (withWildcard) {
                // es ist ein SuchZeichen drin
                if (!withANDOR) { // normal
                  wc =
                    wc
                      + a.name
                      + tmpCompOp
                      + "'" //$NON-NLS-1$
                      + transformBadCharForWhere(tmp, tmpCompOp)
                      + "'"; //$NON-NLS-1$
                }
                else {
                  wc =
                    wc
                      + "(" //$NON-NLS-1$
                      + a.name
                      + tmp1CompOp
                      + "'" //$NON-NLS-1$
                      + transformBadCharForWhere(tmp1, tmp1CompOp)
                      + "'" //$NON-NLS-1$
                      + andOrString
                      + a.name
                      + tmp2CompOp
                      + "'" //$NON-NLS-1$
                      + transformBadCharForWhere(tmp2, tmp2CompOp)
                      + "')"; //$NON-NLS-1$
                }
              }
              else {
                // kein Suchzeichen
                if (!withANDOR) { // normal
                  wc = wc + a.name + tmpCompOp + transformBadCharForWhere(tmp, tmpCompOp);
                }
                else {
                  wc =
                    wc
                      + "(" //$NON-NLS-1$
                      + a.name
                      + tmp1CompOp
                      + transformBadCharForWhere(tmp1,tmp1CompOp)
                      + andOrString
                      + a.name
                      + tmp2CompOp
                      + transformBadCharForWhere(tmp2, tmp2CompOp)
                      + ")"; //$NON-NLS-1$
                }
              }
            }
            else if (a.type == OBConstants.DATE) {
              if (checkForNull) {
                // hier kein rechter Operand
                wc = wc + a.name + tmpCompOp;
              }
              else if (!withANDOR) { // normal
                wc =
                  wc
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_DATE_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmpCompOp
                    + "'" //$NON-NLS-1$
                    + transformBadCharForWhere(tmp, tmpCompOp)
                    + "'"; //$NON-NLS-1$
              }
              else {
                wc =
                  wc
                    + "(" //$NON-NLS-1$
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_DATE_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmp1CompOp
                    + "'" //$NON-NLS-1$
                    + transformBadCharForWhere(tmp1, tmp1CompOp)
                    + "'" //$NON-NLS-1$
                    + andOrString
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_DATE_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmp2CompOp
                    + "'" //$NON-NLS-1$
                    + transformBadCharForWhere(tmp2, tmp2CompOp)
                    + "')"; //$NON-NLS-1$
              }
            }
            else if (a.type == OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE) {
              if (checkForNull) {
                // hier kein rechter Operand
                wc = wc + a.name + tmpCompOp;
              }
              else if (!withANDOR) { // normal
                wc =
                  wc
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_TIMESTAMP_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmpCompOp
                    + "'" //$NON-NLS-1$
                    + transformBadCharForWhere(tmp, tmpCompOp)
                    + "'"; //$NON-NLS-1$
              }
              else {
                wc =
                  wc
                    + "(" //$NON-NLS-1$
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_TIMESTAMP_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmp1CompOp
                    + "'" //$NON-NLS-1$
                    + transformBadCharForWhere(tmp1, tmp1CompOp)
                    + "'" //$NON-NLS-1$
                    + andOrString
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_TIMESTAMP_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmp2CompOp
                    + "'" //$NON-NLS-1$
                    + transformBadCharForWhere(tmp2, tmp2CompOp)
                    + "')"; //$NON-NLS-1$
              }
            }
            else if (a.type == OBConstants.STRING) {
              if (checkForNull) {
                // hier kein rechter Operand
                wc = wc + a.name + tmpCompOp;
              }
              else if (!withANDOR) { // normal
                // bugz 7002
                if ((a.getCaseSensitivity() == OBAttribute.CaseSensitivity.DEFAULT && searchCaseSensitive) || 
                    (a.getCaseSensitivity() == OBAttribute.CaseSensitivity.SENSITIVE)) {
                  wc =
                    wc
                      + a.name
                      + tmpCompOp
                      + "'" //$NON-NLS-1$
                      + transformBadCharForWhere(tmp, tmpCompOp)
                      + "'"; //$NON-NLS-1$
                }
                else {
                  wc =
                    wc
                      + "lower(" //$NON-NLS-1$
                      + a.name
                      + ")" //$NON-NLS-1$
                      + tmpCompOp
                      + "lower('" //$NON-NLS-1$
                      + transformBadCharForWhere(tmp,tmpCompOp)
                      + "')"; //$NON-NLS-1$
                }
              }
              else {
                // bugz 7002
                if ((a.getCaseSensitivity() == OBAttribute.CaseSensitivity.DEFAULT && searchCaseSensitive) || 
                    (a.getCaseSensitivity() == OBAttribute.CaseSensitivity.SENSITIVE)) {
                  wc =
                    wc
                      + "(" //$NON-NLS-1$
                      + a.name
                      + tmp1CompOp
                      + "'" //$NON-NLS-1$
                      + transformBadCharForWhere(tmp1, tmp1CompOp)
                      + "'" //$NON-NLS-1$
                      + andOrString
                      + a.name
                      + tmp2CompOp
                      + "'" //$NON-NLS-1$
                      + transformBadCharForWhere(tmp2, tmp2CompOp)
                      + "')"; //$NON-NLS-1$
                }
                else {
                  wc =
                    wc
                      + "(" //$NON-NLS-1$
                      + "lower(" //$NON-NLS-1$
                      + a.name
                      + ")" //$NON-NLS-1$
                      + tmp1CompOp
                      + "lower('" //$NON-NLS-1$
                      + transformBadCharForWhere(tmp1, tmp1CompOp)
                      + "')" //$NON-NLS-1$
                      + andOrString
                      + "lower(" //$NON-NLS-1$
                      + a.name
                      + ")" //$NON-NLS-1$
                      + tmp2CompOp
                      + "lower('" //$NON-NLS-1$
                      + transformBadCharForWhere(tmp2,tmp2CompOp)
                      + "'))"; //$NON-NLS-1$
                }
              }
            }
            else if (a.type == OBConstants.LONGVARCHAR ||
                     a.type == OBConstants.CLOB ||
                     a.type == OBConstants.BLOB) {
              // LONG-Werte in der WHERE clause unberuecksichtigt
              // lassen: Um den WHERE-String abzuschliessen haenge "1=1" an.
              wc += "1 = 1 "; //$NON-NLS-1$
            }
            else {
              logger.error("Type not implemented " + a.type); //$NON-NLS-1$
            }
          }
        }
      }
      if (wc.trim().equals("")) { //$NON-NLS-1$
        wc = " WHERE 1 = 1 " + wcModifier; // NOTE //$NON-NLS-1$
      }
      else {
        wc = wc + " " + wcModifier; //$NON-NLS-1$
      }
      // rownum setzen, netter Ansatz, allerdings geht das nicht gut,
      // da die WHERE-Clause VOR einer OrderBy-Clause ausgefuehrt wird und
      // dadurch kann die erwartete Datenmenge stark abweichen.
      // wc += OBDriver.getMaxRows(OBConfig.maxRows);
    }
    catch (Exception e) {
      logger.error("error getting where clause from filter", e); //$NON-NLS-1$
    }
    return wc;
  }

  /** Baut einen String fuer die Where-Bedingung eines SQL Statements auf.
      Ist der String = "", so wird er im whereClause nicht verwendet,
      da "" in der Datenbank null entspricht.
      @param colNames Die Spalten, die im Where beruecksichtigt werden sollen.
      @param searchCaseSensitive Gibt an, ob Gross/Kleinschreibung wichtig ist
      @param replacementsOut Zu ersetzende Werte
      @return SQL-Where-Clause
    */
  public String getWhereClauseFromFilterForPreparedStatement(String[] colNames,
                                                             boolean searchCaseSensitive,
                                                             ArrayList<String> replacementsOut) {
    String wc = ""; //$NON-NLS-1$
    String tmp = ""; //$NON-NLS-1$
    boolean flag;
    OBAttribute a;
    
    boolean withANDOR = false;
    String andOrString = " OR "; //$NON-NLS-1$
    boolean withWildcard = false;
    boolean checkForNull = false;
    String tmp1 = ""; //$NON-NLS-1$
    String tmp2 = ""; //$NON-NLS-1$
    int index = -1;
    String tmpCompOp = CompOperator.like;
    String tmp1CompOp = CompOperator.like;
    String tmp2CompOp = CompOperator.like;
    
    try {
      for (int i = 0; i < attArr.length; i++) {
        flag = false;
        a = attArr[i];
        tmp = a.value;
    
        // wird attribut in whereclause benutzt?
        if (colNames == null) {
          flag = true;
        }
        else {
          for (int j = 0; j < colNames.length; j++) {
            if (a.name.equals(colNames[j])) {
              flag = true;
            }
          }
        }
        if (flag == true) {
          // Test, ob auf (NOT) NULL abgefragt werden soll
          checkForNull =
            a.getCompOp().equals(CompOperator.isNull)
              || a.getCompOp().equals(CompOperator.isNotNull);
          // "" in Java entspricht null in der DB, is(Not)Null braucht keinen Wert
          if (tmp.equals("!*") || tmp.equals("!%")) { //$NON-NLS-1$ //$NON-NLS-2$
            if (wc.length() == 0)
              wc = wc + " WHERE "; //$NON-NLS-1$
            else
              wc = wc + " AND "; //$NON-NLS-1$
    
            wc = wc + a.name + " IS NULL "; //$NON-NLS-1$
          }
          else if (tmp.length() > 0 || checkForNull) {
            // wenn auf (NOT) NULL abgefragt werden soll, braucht im Attribut nichts drinzustehen
            if (checkForNull) {
              tmp = ""; //$NON-NLS-1$
            }
    
            // am anfang where, dann immer and
            if (wc.length() == 0)
              wc = wc + " WHERE "; //$NON-NLS-1$
            else
              wc = wc + " AND "; //$NON-NLS-1$
    
            withANDOR = false;
            andOrString = " OR "; //$NON-NLS-1$
            withWildcard = false;
    
            tmp1 = ""; //$NON-NLS-1$
            tmp2 = ""; //$NON-NLS-1$
            index = -1;
    
            if (a.getCompOp().equals(CompOperator.defaultOp)) {
              tmpCompOp = CompOperator.like;
              tmp1CompOp = CompOperator.like;
              tmp2CompOp = CompOperator.like;
            }
            else {
              tmpCompOp = a.getCompOp();
              tmp1CompOp = a.getCompOp();
              tmp2CompOp = a.getCompOp();
            }
            if (tmpCompOp.equals(CompOperator.like) ||
                tmpCompOp.equals(CompOperator.notLike)) {
              if (a.getValue().indexOf('%')==-1 && 
                  a.getValue().indexOf('*')==-1 && 
                  a.getValue().indexOf('_')==-1) {
                if (tmpCompOp.equals(CompOperator.like)) {
                  tmpCompOp = CompOperator.equal;
                  tmp1CompOp = CompOperator.equal;
                  tmp2CompOp = CompOperator.equal;
                } 
                if (tmpCompOp.equals(CompOperator.notLike)) {
                  tmpCompOp = CompOperator.notEqual;
                  tmp1CompOp = CompOperator.notEqual;
                  tmp2CompOp = CompOperator.notEqual;
                } 
              }
            }
            
            // Suche mit "oder" ermoeglichen
            if ((index = tmp.indexOf("||")) != -1) { //$NON-NLS-1$
              withANDOR = true;
              andOrString = " OR "; //$NON-NLS-1$
              tmp1 = tmp.substring(0, index);
              tmp2 = tmp.substring(index + 2, tmp.length());
              if (tmp1.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmp1CompOp = CompOperator.negateCompOp(tmp1CompOp);
                tmp1 = tmp1.substring(1, tmp1.length()); // ! Zeichen verwerfen
              }
              if (tmp2.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmp2CompOp = CompOperator.negateCompOp(tmp2CompOp);
                tmp2 = tmp2.substring(1, tmp2.length()); // ! Zeichen verwerfen
              }
            }
            else if ((index = tmp.indexOf("&&")) != -1) { //$NON-NLS-1$
              withANDOR = true;
              andOrString = " AND "; //$NON-NLS-1$
              tmp1 = tmp.substring(0, index);
              tmp2 = tmp.substring(index + 2, tmp.length());
              if (tmp1.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmp1CompOp = CompOperator.negateCompOp(tmp1CompOp);
                tmp1 = tmp1.substring(1, tmp1.length()); // ! Zeichen verwerfen
              }
              if (tmp2.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmp2CompOp = CompOperator.negateCompOp(tmp2CompOp);
                tmp2 = tmp2.substring(1, tmp2.length()); // ! Zeichen verwerfen
              }
            }
            else {
              tmp = tmp.trim(); // Leerzeichen verwerfen
              if (tmp.length() > 0
                && tmp.charAt(0) == '!') { // Ueberpuefung ob Logisch NOT
                tmpCompOp = CompOperator.negateCompOp(tmpCompOp);
                tmp = tmp.substring(1, tmp.length()); // ! Zeichen verwerfen
              }
            }
    
            // bei Integers nur mit like % suchen, wenn ein Wildcard-Zeichen drin ist
            if (a.type == OBConstants.INTEGER
              || a.type == OBConstants.LONG
              || a.type == OBConstants.DOUBLE
              || a.type == OBConstants.BOOLEAN) {
              if (tmp.indexOf('%') != -1
                || tmp.indexOf('*') != -1
                || tmp.indexOf('_') != -1) {
                withWildcard = true;
                // Test, ob Wildcards ueberhaupt zulaessig sind
                if (!a.getCompOp().equals(CompOperator.defaultOp)
                  && !a.getCompOp().equals(CompOperator.like)
                  && !a.getCompOp().equals(CompOperator.notLike)) {
                  throw new OBException("Wildcard not allowed"); //$NON-NLS-1$
                }
              }
              else {
                // "like" durch "=" und  "not like" durch "!=" ersetzen,
                // alle anderen Operatoren bleiben !!!!
                if (tmpCompOp.equals(CompOperator.like)) {
                  tmpCompOp = CompOperator.equal;
                }
                else if (tmpCompOp.equals(CompOperator.notLike)) {
                  tmpCompOp = CompOperator.notEqual;
                }
    
                if (tmp1CompOp.equals(CompOperator.like)) {
                  tmp1CompOp = CompOperator.equal;
                }
                else if (tmp1CompOp.equals(CompOperator.notLike)) {
                  tmp1CompOp = CompOperator.notEqual;
                }
    
                if (tmp2CompOp.equals(CompOperator.like)) {
                  tmp2CompOp = CompOperator.equal;
                }
                else if (tmp2CompOp.equals(CompOperator.notLike)) {
                  tmp2CompOp = CompOperator.notEqual;
                }
              }
            }
    
            // unterscheide die verschiedenen typen
            // int, date und string
            if (a.type == OBConstants.INTEGER
              || a.type == OBConstants.BOOLEAN
              || a.type == OBConstants.LONG
              || a.type == OBConstants.DOUBLE) {
              if (checkForNull) {
                // hier kein rechter Operand
                wc = wc + a.name + tmpCompOp;
              }
              else if (withWildcard) {
                // es ist ein SuchZeichen drin
                if (!withANDOR) { // normal
                  wc = wc + a.name + tmpCompOp + "?"; //$NON-NLS-1$
                  replacementsOut.add(transformBadCharForWhere(tmp, tmpCompOp));
                }
                else {
                  wc =
                    wc
                      + "(" //$NON-NLS-1$
                      + a.name
                      + tmp1CompOp
                      + "?" //$NON-NLS-1$
                      + andOrString
                      + a.name
                      + tmp2CompOp
                      + "?" //$NON-NLS-1$
                      + "')"; //$NON-NLS-1$
                  replacementsOut.add(transformBadCharForWhere(tmp1, tmp1CompOp));
                  replacementsOut.add(transformBadCharForWhere(tmp2, tmp2CompOp));
                }
              }
              else {
                // kein Suchzeichen
                if (!withANDOR) { // normal
                  wc = wc + a.name + tmpCompOp + '?'; //$NON-NLS-1$ 
                  replacementsOut.add(transformBadCharForWhere(tmp, tmpCompOp));
                }
                else {
                  wc = wc
                      + "(" //$NON-NLS-1$
                      + a.name
                      + tmp1CompOp
                      + "?" //$NON-NLS-1$
                      + andOrString
                      + a.name
                      + tmp2CompOp
                      + "?" //$NON-NLS-1$
                      + ")"; //$NON-NLS-1$
                  replacementsOut.add(transformBadCharForWhere(tmp1, tmp1CompOp));
                  replacementsOut.add(transformBadCharForWhere(tmp2, tmp2CompOp));
                }
              }
            }
            else if (a.type == OBConstants.DATE) {
              if (checkForNull) {
                // hier kein rechter Operand
                wc = wc + a.name + tmpCompOp;
              }
              else if (!withANDOR) { // normal
                wc =
                  wc
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_DATE_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmpCompOp
                    + "?"; //$NON-NLS-1$
                replacementsOut.add(transformBadCharForWhere(tmp, tmpCompOp));
              }
              else {
                wc =
                  wc
                    + "(" //$NON-NLS-1$
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_DATE_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmp1CompOp
                    + "?" //$NON-NLS-1$
                    + andOrString
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_DATE_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmp2CompOp
                    + "?)"; //$NON-NLS-1$
                replacementsOut.add(transformBadCharForWhere(tmp1, tmp1CompOp));
                replacementsOut.add(transformBadCharForWhere(tmp2, tmp2CompOp));
              }
            }
            else if (a.type == OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE) {
              if (checkForNull) {
                // hier kein rechter Operand
                wc = wc + a.name + tmpCompOp;
              }
              else if (!withANDOR) { // normal
                wc =
                  wc
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_TIMESTAMP_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmpCompOp
                    + "?"; //$NON-NLS-1$
                replacementsOut.add(transformBadCharForWhere(tmp, tmpCompOp));
              }
              else {
                wc =
                  wc
                    + "(" //$NON-NLS-1$
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_TIMESTAMP_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmp1CompOp
                    + "?" //$NON-NLS-1$
                    + andOrString
                    + "TO_CHAR(" //$NON-NLS-1$
                    + a.name
                    + ",'" //$NON-NLS-1$
                    + (a.getFormatMask()==null || a.getFormatMask().length()==0 ?  OBConstants.NLS_TIMESTAMP_FORMAT : a.getFormatMask())
                    + "')" //$NON-NLS-1$
                    + tmp2CompOp
                    + "?)"; //$NON-NLS-1$
                replacementsOut.add(transformBadCharForWhere(tmp1, tmp1CompOp));
                replacementsOut.add(transformBadCharForWhere(tmp2, tmp2CompOp));
              }
            }
            else if (a.type == OBConstants.STRING) {
              if (checkForNull) {
                // hier kein rechter Operand
                wc = wc + a.name + tmpCompOp;
              }
              else if (!withANDOR) { // normal
                // bugz 7002
                if ((a.getCaseSensitivity() == OBAttribute.CaseSensitivity.DEFAULT && searchCaseSensitive) || 
                    (a.getCaseSensitivity() == OBAttribute.CaseSensitivity.SENSITIVE)) {
                  wc =
                    wc
                      + a.name
                      + tmpCompOp
                      + "?"; //$NON-NLS-1$
                  replacementsOut.add(transformBadCharForWhere(tmp, tmpCompOp));
                }
                else {
                  wc =
                    wc
                      + "lower(" //$NON-NLS-1$
                      + a.name
                      + ")" //$NON-NLS-1$
                      + tmpCompOp
                      + "lower(" //$NON-NLS-1$
                      + "?)"; //$NON-NLS-1$
                  replacementsOut.add(transformBadCharForWhere(tmp, tmpCompOp));
                }
              }
              else {
                // bugz 7002
                if ((a.getCaseSensitivity() == OBAttribute.CaseSensitivity.DEFAULT && searchCaseSensitive) || 
                    (a.getCaseSensitivity() == OBAttribute.CaseSensitivity.SENSITIVE)) {
                  wc =
                    wc
                      + "(" //$NON-NLS-1$
                      + a.name
                      + tmp1CompOp
                      + "?" //$NON-NLS-1$
                      + andOrString
                      + a.name
                      + tmp2CompOp
                      + "?)"; //$NON-NLS-1$
                  replacementsOut.add(transformBadCharForWhere(tmp1, tmp1CompOp));
                  replacementsOut.add(transformBadCharForWhere(tmp2, tmp2CompOp));
                }
                else {
                  wc =
                    wc
                      + "(" //$NON-NLS-1$
                      + "lower(" //$NON-NLS-1$
                      + a.name
                      + ")" //$NON-NLS-1$
                      + tmp1CompOp
                      + "lower(" //$NON-NLS-1$
                      + "?)" //$NON-NLS-1$
                      + andOrString
                      + "lower(" //$NON-NLS-1$
                      + a.name
                      + ")" //$NON-NLS-1$
                      + tmp2CompOp
                      + "lower(" //$NON-NLS-1$
                      + "?))"; //$NON-NLS-1$
                  replacementsOut.add(transformBadCharForWhere(tmp1, tmp1CompOp));
                  replacementsOut.add(transformBadCharForWhere(tmp2, tmp2CompOp));
                }
              }
            }
            else if (a.type == OBConstants.LONGVARCHAR ||
                     a.type == OBConstants.CLOB ||
                     a.type == OBConstants.BLOB) {
              // LONG-Werte in der WHERE clause unberuecksichtigt
              // lassen: Um den WHERE-String abzuschliessen haenge "1=1" an.
              wc += "1 = 1 "; //$NON-NLS-1$
            }
            else {
              logger.error("Type not implemented " + a.type); //$NON-NLS-1$
            }
          }
        }
      }
      if (wc.trim().equals("")) { //$NON-NLS-1$
        wc = " WHERE 1 = 1 " + wcModifier; // NOTE //$NON-NLS-1$
      }
      else {
        wc = wc + " " + wcModifier; //$NON-NLS-1$
      }
      // rownum setzen, netter Ansatz, allerdings geht das nicht gut,
      // da die WHERE-Clause VOR einer OrderBy-Clause ausgefuehrt wird und
      // dadurch kann die erwartete Datenmenge stark abweichen.
      // wc += OBDriver.getMaxRows(OBConfig.maxRows);
    }
    catch (Exception e) {
      logger.error("error getting where clause from filter", e); //$NON-NLS-1$
    }
    return wc;
  }
  // ---------------------------------------------------------------------------
  // ------- SQL-Hilfsmethoden / Exception-Handling ----------------------------
  // ---------------------------------------------------------------------------

  /**
   * Eine Methode, die das replaceAll von Java 1.4 ersetzen soll
   * @param str Ausgangs-String
   * @param rep Zu ersetzender String
   * @param wth Ersetzen mit
   * @return String mit Ersetzungen
   */
  public static final String strReplaceAll(String str,
                                           String rep,
                                           String wth) {
    StringBuffer sb = new StringBuffer();
    int beginIndex = 0;
    int endIndex = 0;

    for (beginIndex = str.indexOf(rep, endIndex);
      beginIndex != -1;
      beginIndex = str.indexOf(rep, endIndex)) {
      sb.append(str.substring(endIndex, beginIndex)).append(wth);
      endIndex = beginIndex + rep.length();
    }
    sb.append(str.substring(endIndex, str.length()));
    return sb.toString();
  }

  /** 
   * Ersetzt in einem String das "'"-Zeichen durch "''" und "*" durch "%".
   * Wird fuer das Bauen der WHERE-Clause verwendet.
   * @param str String, in dem die Ersetzung stattfinden soll
   * @param compOp Vergleichsoperator
   * @return Ersetzten String
   * 
   */
  public static final String transformBadCharForWhere(String str, String compOp) {
    //String outStr = str.replaceAll("\'","''");
    String outStr = strReplaceAll(str, "\'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
    if (compOp.equals(CompOperator.like) || compOp.equals(CompOperator.notLike)) { // Bugz 7030
     outStr = outStr.replace('*', '%');
    } 
    return outStr;
  }

  /** 
   * Ersetzt das "'" Zeichen in einem String durch "''".
   * Wird fuer den Zusammenbau von INSERTs und UPDATEs benoetigt. 
   * @param str String, in dem die Ersetzung stattfinden soll
   * @return Ersetzten String
   * 
   */
  public static final String transformBadCharForDML(String str) {
    //return str.replaceAll("\'","''");
    return strReplaceAll(str, "\'", "''"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /** Ersetzt einen String,falls er = null ist, durch einen Defaultstring
      @param str zu untersuchender String
      @param nullStr Defaultstring (z.B. "")
      @return str, wenn str null war, ansonsten nullStr
  */
  public static final String ifnull(String str, String nullStr) {
    if (str == null) {
      return nullStr;
    }
    else {
      return str;
    }
  }

  /** Wandelt ein OBDBOject in einen Hashtable um (key = attribute.name, value = attribute.value) 
      @return Hashtable, der dem OBDBObject entspricht
  */
  public Hashtable<String,String> convertToHashtable() {
    Hashtable<String,String> retVal = new Hashtable<String,String>();
    for (int i = 0; i < attArr.length; i++) {
      retVal.put(attArr[i].getName(), attArr[i].getValue());
    }
    return retVal;
  }

  /** Wandelt ein OBDBOject in eine HashMap um (key = attribute.name, value = attribute.value) 
      @return HashMap, der dem OBDBObject entspricht
   */
  public HashMap<String,Object> convertToHashMap() {
    HashMap<String,Object> retVal = new HashMap<String,Object>();
    for (int i = 0; i < attArr.length; i++) {
      retVal.put(attArr[i].getName(), attArr[i].getValue());
    }
    return retVal;
  }
  
  /** Wandelt ein OBDBOject in eine HashMap um (key = attribute.name, value = attribute.value) 
   * @param key Schluessel
   * @return HashMap, der dem OBDBObject entspricht
   */
  public HashMap<String,Object> convertToHashMap(String key) {
    HashMap<String,Object> retVal = new HashMap<String,Object>();
    retVal.put(key, convertToHashMap());
    return retVal;
  }

  /** Wandelt einen Hashtable in ein OBDBOject um (key = attribute.name, value = attribute.value) 
      @param ht Hashtable, der dem OBDBObject entspricht
      @throws OBException wenn nicht alle Attribute im Hashtable vorhanden sind.
  */
  public void convertFromHashtable(Hashtable<String,String> ht) throws OBException {
    for (int i = 0; i < attArr.length; i++) {
      if (ht.containsKey(attArr[i].getName())) {
        attArr[i].setValue(ht.get(attArr[i].getName()));
      }
      else {
        throw new OBException(OBException.OBErrorNumber.attribNotFoundInHashtable1, 
                              new String[] {attArr[i].getName()});
      }
    }
  }

  /** Wandelt einen long-Wert in einen String um (noetig fuer xmlrpc)
      @param value der long-Wert
      @return String-Wert
  */
  public static String longToString(long value) {
    return Long.toString(value);
  }

  /** Wandelt einen String-Wert in einen String um (noetig fuer xmlrpc)
      @param value der String-Wert
      @return String-Wert
      @exception NumberFormatException wenn der String falsch ist
  */
  public static long stringToLong(String value) throws NumberFormatException {
    return Long.valueOf(value).longValue();
  }

  // ---------------------------------------------------------------------------
  // ------- Methoden fuer XOBTableModel ---------------------------------------
  // ---------------------------------------------------------------------------

  /** Fuer das Anzeigen in Tabellen */
  protected String[] selectCols = null;
  protected String[] selectColNames = null;

  /** Setzt Select-Spalten und Spalten-Ueberschriften auf einen Default-Wert */
  public void setSelectColsDefault() {
    selectCols = new String[attArr.length];
    selectColNames = new String[attArr.length];
    for (int i = 0; i < attArr.length; i++) {
      selectCols[i] = attArr[i].getName();
      selectColNames[i] = attArr[i].getName();
    }
  }

  /** Setzt Select-Spalten und Spalten-Ueberschriften auf die uebergebenen Werte 
   * @param sc Spaltennamen
   * @param names Ueberschriften
   * @throws OBException Fehler*/
  public void setSelectCols(String[] sc, String[] names) throws OBException {
    if (sc.length != names.length) {
      throw new OBException(OBException.OBErrorNumber.arrayLengthDifferent);
    }
    selectCols = new String[sc.length];
    selectColNames = new String[names.length];
    for (int i = 0; i < sc.length; i++) {
      selectCols[i] = sc[i];
      selectColNames[i] = names[i];
    }
  }

  /** Gibt Select-Spalten zurueck 
   * @return Select-Spalten
  */
  public String[] getSelectCols() {
    return selectCols;
  }

  /** Gibt Select-Spalten-Ueberschriften zurueck 
   * @return SelectColName
   */
  public String[] getSelectColNames() {
    return selectColNames;
  }

  /** Gibt die Spaltenueberschrift zurueck 
   * @param columnIndex Spaltennummer
   * @return Spaltenname der Spalte columnIndex
   */
  public String getColumnName(int columnIndex) {
    String name = ""; //$NON-NLS-1$
    try {
      if (selectColNames.length > columnIndex && columnIndex >= 0) {
        return selectColNames[columnIndex];
      }
    }
    catch (Exception e) {
      logger.debug("error getting column name at index "+columnIndex, e);//$NON-NLS-1$
    }
    return name;
  }

  /** Gibt die Anzahl der gewaehlten Spalten zurueck 
   * @return Anzahl der Spalten 
   */
  public int getColumnCount() {
    return selectCols.length;
  }

  
  /**
   * Testet, ob irgendein Wert in der Zeile gesetzt ist 
   * @return true, wenn leer
   */
  public boolean isEmpty() {
    for (int i = 0; i < selectCols.length; i++) {
      String attName = selectCols[i];
      for (int j = 0; j < attArr.length; j++) {
        if (attArr[j].getName().equals(attName)) {
          if (attArr[j].isNotNull()) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /** Sucht anhand der Select-Spalten-Liste den Wert 
   * @param col Spaltennummer
   * @return Wert in Spalte col 
   */
  public Object getValueAt(int col) {
    Object cell = ""; //$NON-NLS-1$
    try {
      if (selectCols.length > col && col >= 0) {
        String attName = selectCols[col];
        for (int i = 0; i < attArr.length; i++) {
          if (attArr[i].getName().equals(attName)) {
            switch (attArr[i].getType()) {
              case OBConstants.INTEGER :
                cell = ""; //$NON-NLS-1$
                if (!attArr[i].isNull())
                  cell = new Integer(attArr[i].getIntValue());
                break;
              case OBConstants.STRING :
                cell = attArr[i].getValue();
                break;
              case OBConstants.LONG :
                cell = ""; //$NON-NLS-1$
                if (!attArr[i].isNull())
                  cell = new Long(attArr[i].getLongValue());
                break;
              case OBConstants.DATE :
                cell = attArr[i].getValue();
                break;
              case OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE :
                cell = attArr[i].getValue();
                break;
              case OBConstants.BOOLEAN :
                cell = new Boolean(false);
                if (!attArr[i].isNull())
                  cell = new Boolean(attArr[i].getBooleanValue());
                break;
              case OBConstants.DOUBLE :
                cell = new Double(OBConstants.IRREGULAR_INT);
                if (!attArr[i].isNull())
                  cell = new Double(attArr[i].getDoubleValue());
                break;
            }
          }
        }
      }
    }
    catch (Exception e) {
      logger.debug("error getting value at " + col, e);//$NON-NLS-1$
    }
    return cell;
  }
  
  public String getStringValueAt(int col) {
    try {
      if (selectCols.length > col && col >= 0) {
        String attName = selectCols[col];
        for (int i = 0; i < attArr.length; i++) {
          if (attArr[i].getName().equals(attName)) {
            return attArr[i].getValue();
          }
        }
      }
    }
    catch (Exception e) {
      logger.debug("error getting value at " + col, e);//$NON-NLS-1$
    }
    return ""; //$NON-NLS-1$
  }

  /** Gibt die Klasse des Attributes an 
   * @param col Spaltennummer
   * @return Klasse des Attributes
   */
  public Class<?> getColumnClass(int col) {
    Class<?> klass = new Object().getClass();
    try {
      if (selectCols.length > col && col >= 0) {
        String attName = selectCols[col];
        for (int i = 0; i < attArr.length; i++) {
          if (attArr[i].getName().equals(attName)) {
            switch (attArr[i].getType()) {
              case OBConstants.INTEGER :
                klass = new Integer(0).getClass();
                break;
              case OBConstants.STRING :
                klass = new String("").getClass(); //$NON-NLS-1$
                break;
              case OBConstants.LONG :
                klass = new Integer(0).getClass();
                break;
              case OBConstants.DATE :
                klass = new String("").getClass(); //$NON-NLS-1$
                break;
              case OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE :
                klass = new String("").getClass(); //$NON-NLS-1$
                break;
              case OBConstants.DOUBLE :
                klass = new Double(0.0).getClass();
                break;
              case OBConstants.BOOLEAN :
                klass = new Boolean(false).getClass();
                break;
            }
          }
        }
      }
    }
    catch (Exception e) {
      logger.debug("error getting class at " + col, e);//$NON-NLS-1$
    }
    return klass;
  }

  /** Sucht anhand der Select-Spalten-Liste den Wert 
   * @param value Zu setzender Wert
   * @param col Spaltennummer
   */
  public void setValueAt(Object value, int col) {
    try {
      if (selectCols.length > col && col >= 0) {
        String attName = selectCols[col];
        for (int i = 0; i < attArr.length; i++) {
          if (attArr[i].getName().equals(attName)) {
            if (value.toString().equals("")) { //$NON-NLS-1$
              attArr[i].setNull();
            }
            else {
              switch (attArr[i].getType()) {
                case OBConstants.INTEGER :
                  attArr[i].setValue(Integer.parseInt(value.toString()));
                  break;
                case OBConstants.STRING :
                  attArr[i].setValue(value.toString());
                  break;
                case OBConstants.LONG :
                  attArr[i].setValue(Long.parseLong(value.toString()));
                  break;
                case OBConstants.DATE :
                  attArr[i].setValue(value.toString());
                  break;
                case OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE :
                  attArr[i].setValue(value.toString());
                  break;
                case OBConstants.BOOLEAN :
                  attArr[i].setValue(
                    (new Boolean(value.toString())).booleanValue());
                  break;
                case OBConstants.DOUBLE :
                  attArr[i].setValue(Double.parseDouble(value.toString()));
                  break;
              }
            }
          }
        }
      }
    }
    catch (Exception e) {
      logger.debug("error setting value at " + col, e);//$NON-NLS-1$
    }
  }

  /* derzeit nicht eingesetzt
  public String isValueValid(String attribKey, String attribValue) {
    return ""; //$NON-NLS-1$
  }
  */

  /**
   * Liefert den Typ als in gemaess den Konstanten
   * @param attribKey Attributname
   * @return Typ des Attributes
   */
  public int getType(String attribKey)  {
    for (int i=0; i<attArr.length; i++) {
      if (attArr[i].getName().equals(attribKey)) { 
        return attArr[i].getType();
      }
    }
    return OBConstants.IRREGULAR_INT;
  }

  public String getValue(String attribKey) {
    for (int i=0; attArr!=null && i<attArr.length; i++) {
      if (attArr[i].getName().equals(attribKey)) return attArr[i].getValue();
    }
    return null;
  }

  public String getValueIC(String attribKey) {
    for (int i=0; i<attArr.length; i++) {
      if (attArr[i].getName().equalsIgnoreCase(attribKey)) return attArr[i].getValue();
    }
    return null;
  }

  public void setValueIC(String attribKey, String val) {
    for (int i=0; i<attArr.length; i++) {
      if (attArr[i].getName().equalsIgnoreCase(attribKey)) {
        attArr[i].setValue(val); 
        return;
      } 
    }
  }

  protected String hint = ""; //$NON-NLS-1$
  public String getHint() {return hint; }
  public void setHint(String s) { hint=s; }
  
  protected OBAttribute[] attribs = null;
  public OBAttribute[] getAttribs() {return attribs;}
  public void setAttribs(OBAttribute[] attrs) { attribs = attrs; }
  
  /** H&ouml;chstzahl selektierter Datens&auml;tze, wenn nichts anderes angegeben wird */
  public final static int INFINITE_ROWS = Integer.MAX_VALUE;

  protected int maxRowsSelect = INFINITE_ROWS;
  public int getMaxRowsSelect() {return maxRowsSelect; }
  public void setMaxRowsSelect(int i) { maxRowsSelect=i; }
  
 
  public Object getObject() {
    return null;
  }
  
  /* derzeit nicht eingesetzt */
  public String isValueValid(String p_attribKey_p, String p_attribValue_p) {
    return null;
  }
  
  /**
   * Baut aus einem OBObject einen String, der selbiges repraesentiert
   * @param indent Einrueckung
   * @param ignoreList Liste der zu *nenden Werte
   * @return String Repraesentation des Objektes
   * @throws OBException Fehler
   */
  public String convertToString(String indent, String[] ignoreList) throws OBException {
    StringBuilder sb = new StringBuilder();
    for(int i=0; i<attArr.length; i++ ) {
      sb.append(indent).append(attArr[i].getName()).append("=").append(OBUtils.hideValue(attArr[i].getName(), attArr[i].getValue(), ignoreList)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    String details = sb.toString();
    // letztes "\n" weg
    if( details.length() > 1 ) {
      details = details.substring(0,details.length()-1);
    }

    return sb.toString();
  }

  public boolean getCaseSensitive() {
    return _caseSensitive;
  }

  public void setCaseSensitive(boolean _caseSensitive) {
    this._caseSensitive = _caseSensitive;
  }
}
