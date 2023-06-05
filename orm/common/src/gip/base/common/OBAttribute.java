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

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

/** Klasse, die ein Attribut einer DB-Tabelle oder eines Views charakterisiert
*/
@SuppressWarnings("serial")
public class OBAttribute implements Serializable {
  
  private static transient Logger logger = Logger.getLogger(OBAttribute.class);
  
  // bugz 7002
  public interface CaseSensitivity {
    public static final int DEFAULT=0;
    public static final int SENSITIVE=1;
    public static final int INSENSITIVE=2;
  }
  
  /** Name der Spalte */
  public String name;
  /** Typ der Spalte */
  public int type;
  /** Laenge der Spalte */
  public int length;
  /** Ist null als Wert erlaubt? */
  public boolean nullable;
  /** Wert der Spalte, wird immer als String gespeichert */
  public String value;
  /** Wert der Spalte, wird immer als byteArray gespeichert */
  public byte[] bvalue;
  /** Kommentarfeld */
  public String comment;
  /** Eine FomatMaske, die es erlaubt Daten und Zahlen in einem
   * betimmten Format auszugeben (per Default leer (null)) */
  public String formatMask = null;
  /** Ein Feld mit dem der Vergleichs-Operator gesetzt werden kann */
  public String compOp = null;
  /** Muss gesetzt werden, wenn das Feld absichtlich leer gelassen wird bei Aufrufen ans DCI */
  private boolean _ignored;
  /** erlaubte Werte im Interface CaseSensitivity */
  private int caseSensitivity;  // bugz 7002
  
  /** Der null-Wert eines OBAttributes */
  public static final int NULL = OBConstants.IRREGULAR_INT;
  /** Der null-Wert eines OBAttributes */
  public static final int IGNORE = NULL+1;
  
  /** Eine FomatMaske fuer Daten  */
  public String dateFormatMask = "dd.MM.yyyy";  //$NON-NLS-1$
  public String dateFormatMaskWithTime = "dd.MM.yyyy HH:mm:ss";  //$NON-NLS-1$

  
  /** 
   * Konstruktor mit den vier erforderlichen Werten.
   * @param _name Name
   * @param _type Datentyp
   * @param _length maximale Laenge
   * @param _nullable nullable
   */
  public OBAttribute(String _name, int _type, int _length, boolean _nullable) {
    this.name = _name;
    this.type = _type;
    this.length = _length;
    this.nullable = _nullable;
    compOp = CompOperator.defaultOp;
    value = ""; //$NON-NLS-1$
    bvalue = new byte[0];
    comment = ""; //$NON-NLS-1$
    caseSensitivity = CaseSensitivity.DEFAULT;  // bugz 7002
  }


  /** 
   * Konstruktor mit den vier erforderlichen Werten und einem Kommentar.
   * @param _name Name
   * @param _type Datentyp
   * @param _length maximale Laenge
   * @param _nullable nullable
   * @param _comment Kommentar
   */
  public OBAttribute(String _name, int _type, int _length, boolean _nullable, String _comment) {
    this(_name,_type,_length,_nullable);
    this.comment = _comment;
  }
  

  /** 
   * Setzt den Wert des Objektes auf einen definierten Wert NULL.
   */
  public void setNull() {
    this.value = ""; //$NON-NLS-1$
    this.bvalue = new byte[0];
  }


  /** 
   * Liefert TRUE, wenn der Wert den definierten Wert NULL hat
   * @return true, wenn null
   */
  public boolean isNull() {
    if (value.equals("") && bvalue.length==0) { //$NON-NLS-1$
      return true;
    }
    return false;
  }


  /** 
   * Liefert TRUE, wenn der Wert den definierten Wert NULL hat
   * @return true, wenn nicht null
   */
  public boolean isNotNull() {
    if (value.equals("") && bvalue.length==0) { //$NON-NLS-1$
      return false;
    }
    return true;
  }


  /** 
   * Setzt das Attribut zurueck.
   */
  public void clear() {
    setValue("");  //$NON-NLS-1$
    compOp = CompOperator.defaultOp;
    setIgnored(false);
  }


  /** 
   * Setzt den Wert des Objektes.
   * @param _value Zu setzender Wert
   * @throws OBException Fehler bei falschem Format
   */
  public void setValue(boolean _value) throws OBException {
    if(type != OBConstants.BOOLEAN) {
      throw new OBException(OBException.OBErrorNumber.wrongType1, new String[] {getName()});
    }
    else {
      if (_value) {
        this.value="1";  //$NON-NLS-1$
      }
      else {
        this.value="0";  //$NON-NLS-1$
      }
      setIgnored(true);
    }
  }

  
  /** 
   * Setzt den Wert des Objektes
   * @param _value Zu setzender Wert
   * @throws OBException Fehler
   */
  public void setValue(int _value) throws OBException {
    if(type != OBConstants.INTEGER && type != OBConstants.LONG) {
      // Nullsetzen auch fuer Doubles erlaubt!
      if (!(type == OBConstants.DOUBLE) || _value!=OBAttribute.NULL) {
        throw new OBException(OBException.OBErrorNumber.wrongType1, new String[] {getName()});
      }
    }
    else {
      if (_value==OBAttribute.NULL) {
        setNull();
      }
      else {
        this.value = Integer.toString(_value);
      }
      setIgnored(true);
    }
  }


  /** 
   * Setzt den Wert des Objektes
   * @param _value Zu setzender Wert
   * @throws OBException Fehler
   */
  public void setValue(long _value) throws OBException {
    if(type != OBConstants.INTEGER && type != OBConstants.LONG) {
      throw new OBException(OBException.OBErrorNumber.wrongType1, new String[] {getName()});
    }
    else {
      if (_value==OBAttribute.NULL) {
        setNull();
      }
      else {
        this.value = Long.toString(_value);
      }
      setIgnored(true);
    }
  }


  /** 
   * Setzt den Wert des Objektes
   * @param _value Zu setzender Wert
   * @throws OBException Fehler
   */
  public void setValue(double _value) throws OBException {
    if(type != OBConstants.DOUBLE) {
      throw new OBException(OBException.OBErrorNumber.wrongType1, new String[] {getName()});
    }
    else {
      if (_value==OBAttribute.NULL) {
        setNull();
      }
      else {
        this.value = Double.toString(_value);
      }
      setIgnored(true);
    }
  }
  

  /** 
   * Setzt den Wert des Objektes
   * @param _value Zu setzender Wert
   * @param format Format
   * @throws OBException Fehler
   */
  public void setValue(Calendar _value, String format) throws OBException {
    if(type != OBConstants.DATE && type != OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE) {
      throw new OBException(OBConstants.WRONG_TYPE + " " + getName()); //$NON-NLS-1$
    }
    else {
      Date date = _value.getTime();
      SimpleDateFormat sdf = new SimpleDateFormat(format);
      this.value = sdf.format(date);
      setIgnored(true);
    }
  }


  /** 
   * Setzt den Wert des Objektes, 
   * dabei werden alle fuehrenden und nachfolgenden Leerzeichen abgeschnitten
   * @param value Zu setzender Wert
   */
  public void setValue(byte[] value) {  
    if (value != null) {
      if(type == OBConstants.BLOB) {
        this.bvalue = value;
        this.value = ""; //$NON-NLS-1$
      }
      else {
        this.value = new String(value);
        this.bvalue = new byte[0];
      }
    }
    else {
      this.value = ""; //$NON-NLS-1$
      this.bvalue = new byte[0];
    }
  }

  /** 
   * Setzt den Wert des Objektes, 
   * dabei werden alle fuehrenden und nachfolgenden Leerzeichen abgeschnitten
   * @param _value Zu setzender Wert
   */
  public void setValue(Object _value) {
    if (_value != null) {
      this.value = _value.toString();
    }
    else {
      this.value = "";  //$NON-NLS-1$
    }
    setIgnored(true);
  }


  /**
   * Liefert den Wert des Attributes als String
   * @return String-Wert
   */
  public String getValue() {
    return value;
  }


  /** 
   * Gibt den Wert des Objektes als long zurueck, wenn es ein Integer ist
   * @return long-Wert
   * @throws OBException Fehler
   */
  public long getLongValue() throws OBException {
    if(type != OBConstants.INTEGER && type != OBConstants.LONG) {
      throw new OBException(OBException.OBErrorNumber.wrongType1, new String[] {getName()});
    }
    else {
      if (value.length() == 0) {
        return NULL;
      }
      else {
        return Long.parseLong(value);
      }
    }
  }


  /** 
   * Gibt den Wert des Objektes als int zurueck, wenn es ein Integer ist
   * @return int-Wert
   * @throws OBException Fehler
   */
  public int getIntValue() throws OBException {
    if(type != OBConstants.INTEGER) {
      throw new OBException(OBException.OBErrorNumber.wrongType1, new String[] {getName()});
    }
    else {
      if (value.length() == 0) {
        return NULL;
      }
      return Integer.parseInt(value);
    }
  }


  /** 
   * Gibt den Wert des Objektes als double zurueck, wenn es ein Double ist
   * @return Wert als double
   * @throws OBException Fehler
   */
  public double getDoubleValue() throws OBException {
    if(type != OBConstants.INTEGER && 
       type != OBConstants.LONG && 
       type != OBConstants.DOUBLE) {
      throw new OBException(OBException.OBErrorNumber.wrongType1, new String[] {getName()});
    }
    else {
      if (value.length() == 0) {
        return NULL;
      }
      else {
        return Double.parseDouble(value);
      }
    }
  }

  
  /** 
   * Gibt den Wert des Objektes als int zurueck, wenn es ein Integer ist
   * @return Wert als Boolean
   * @throws OBException Fehler
   */
  public boolean getBooleanValue() throws OBException {
    if(type != OBConstants.BOOLEAN) {
      throw new OBException(OBException.OBErrorNumber.wrongType1, new String[] {getName()});
    }
    else {
      if (value.length() == 0) {
        return false;
      }
      return (value.equals("1"));  //$NON-NLS-1$
    }
  }

  
  /**
   * @param format Format
   * @return Wert als Calendar
   * @throws OBException Fehler
   */
  public Calendar getCalendarValue(String format) throws OBException {
    SimpleDateFormat date = new SimpleDateFormat(format);
    date.setCalendar(new GregorianCalendar());
    try {
      date.parse(value);
    }
    catch (ParseException e) {
      logger.error("error parsing Calendar", e); //$NON-NLS-1$
      throw new OBException(OBException.OBErrorNumber.parseException1, new String[] {e.getMessage()});
    }
    return date.getCalendar();
  }

  /** gibt den Wert des Objektes als int zurueck, wenn es ein Integer ist
   * @return Wert in byte[] bei CLOB und BLOB
   * @throws OBException Wenn falscher Typ
   */
  public byte[] getByteArrayValue() throws OBException {
    if(type == OBConstants.BLOB) {
      return bvalue;
    }
    else if(type == OBConstants.CLOB) {
      return value.getBytes();
    }
    throw new OBException(OBException.OBErrorNumber.wrongType1, new String[] {getName()});
  }

  /** Liefert den Namen
      @return Name des Attributes
  */
  public String getName() {
    return name;
  }


  /** 
   * Liefert den Type des Attributes
   * @return Type des Attributes
   */
  public int getType() {
    return type;
  }


  /** 
   * Liefert den Namen fuer SQL-Befehle.
   * Bei Datums-Attribute wird ein TO_CHAR() drumgebaut.
   * @return Name fuers SQL
   */
  public String getSelectName() {
    if (type == OBConstants.DATE || type == OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE) {
      if (formatMask != null) {
        return "TO_CHAR(" + name + ", '" + formatMask + "')";    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
      else {
        return "TO_CHAR(" + name + ", '" + OBConstants.NLS_DATE_FORMAT + "')";    //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }
    }
    else {
      return name;
    }
  }


  /** 
   * Liefert die max. Laenge.
   * @return maximale Laenge
   */
  public int getMaxLength() {
    return length;
  }

  
  /** 
   * Setzt caseSensitivity 
   * @param caseSensitivity Zu setzender Wert
   */
  public void setCaseSensitivity(int caseSensitivity) {
    this.caseSensitivity = caseSensitivity;
  }


  /**
   * Liefert caseSensitivity
   * @return caseSensitivity
   */
  public int getCaseSensitivity() {
    return caseSensitivity;
  }
  
  /** 
   * Gibt das aktuelle OBAttribute auf der Konsole aus, nur fuer DEBUG-Zwecke
   * @param header Optionaler Header-String
   */
  public void displayForDebug(String header) {
    logger.debug(" *** ["+name+"] "+header+" ***");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    logger.debug(" * name         : "+name);  //$NON-NLS-1$
    logger.debug(" * hashCode     : "+hashCode());  //$NON-NLS-1$
    logger.debug(" * type         : "+type);  //$NON-NLS-1$
    logger.debug(" * length       : "+length);  //$NON-NLS-1$
    logger.debug(" * nullable     : "+nullable);  //$NON-NLS-1$
    logger.debug(" * comment      : "+comment);  //$NON-NLS-1$
    logger.debug(" * value.length : "+value.length()); //$NON-NLS-1$
    logger.debug(" * value        : "+value);  //$NON-NLS-1$
    logger.debug(" * compOp       : "+compOp);  //$NON-NLS-1$
    logger.debug(" * isIgnored    : "+_ignored);  //$NON-NLS-1$
    logger.debug(" * caseSensitivity : "+caseSensitivity); //$NON-NLS-1$

  }


  /** 
   * Gibt das aktuelle OBAttribute auf der Konsole aus, nur fuer DEBUG-Zwecke.
   */
  public void displayForDebug() {
    displayForDebug("");  //$NON-NLS-1$
  }


  /** 
   * Setzen der Formatmaske
   * @param fm Formatmaske
   * funktioniert mit den find-Methoden nicht mehr 
   */
  public void setFormatMask(String fm) {
    formatMask = fm;
  }

  /** 
   * Setzen der Formatmaske
   * @return Formatmaske
   * funktioniert mit den find-Methoden nicht mehr 
   */
  public String getFormatMask() {
    return formatMask;
  }


  /** 
   * Setzt einen Vergleichsoperator
   * @return Der gesetzte Vergleichsoperator
   */
  public String getCompOp() {
    return compOp;
  }

  
  /** 
   * Setzt einen Vergleichsoperator.
   * @param op Der zu setzende Vergleichsoperator
   * @throws OBException Unbekannter Vergleichsoperator
   * @see CompOperator
   */
  public void setCompOp(String op) throws OBException {
    if (op.equals(CompOperator.defaultOp) ||
        op.equals(CompOperator.equal) ||
        op.equals(CompOperator.notEqual) ||
        op.equals(CompOperator.less) ||
        op.equals(CompOperator.lessEqual) || 
        op.equals(CompOperator.greater) || 
        op.equals(CompOperator.greaterEqual) || 
        op.equals(CompOperator.isNull) || 
        op.equals(CompOperator.isNotNull) || 
        op.equals(CompOperator.like) || 
        op.equals(CompOperator.notLike)) { 
      compOp = op;
    }
    else {
      throw new OBException(OBException.OBErrorNumber.unkownCompOp1, new String[]{op});
    }
  }
  
  
  /** 
   * Muss entsprechend gesetzt werden, 
   * wenn eine entsprechende Behandlung im DCI erwuenscht ist.
   * @param b true : im DCI wird das Attribut nicht auf Null-Werte geprueft.
   */
  public void setIgnored(boolean b) {
    this._ignored = b;
  }
  

  /** 
   * Entsprechende Behandlung im DCI.
   * @return true, wenn ignoriert
   */
  public boolean isIgnored() {
    return this._ignored;
  }
  
//  /** 
//   * Wandelt einen Byte-Array in einen String um
//   * @param arrByte das zu konvertierende byte[]
//   * @return String
//   */
//  private static String byteArrayToString(byte[] arrByte) {
//    if (arrByte == null) {
//        return null;
//    }
//
//    String ret = "";
//    for (int i = 0; i < arrByte.length; i++) {
//      char b = (char) (arrByte[i] & 0xFF);
//      if (b < 0x10) {
//        ret = ret + "0";
//      }
//      ret = ret + (Integer.toHexString(b)).toUpperCase();
//    }
//
//    return ret;
//  }
//
//  /**
//   * Konvertiert einen String in ein Byte-Array 
//   * @param strValue zu konvertierender String
//   * @return
//   */
//  private static byte[] stringToByteArray(String strValue) {
//    if (strValue == null) {
//        return new byte[]{};
//    } else {
//        byte[] bts = new BigInteger(strValue, 16).toByteArray();
//
//        return bts;
//    }
//  }

}


