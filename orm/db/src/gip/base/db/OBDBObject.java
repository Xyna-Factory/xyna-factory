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
package gip.base.db;

import gip.base.common.*;
import gip.base.db.drivers.OBDriver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

import org.apache.log4j.Logger;

/**
 * OBDBObject
 */
@SuppressWarnings("serial")
public abstract class OBDBObject extends OBObject  {

  /** Datumsformat (Default: ohne Uhrzeit */
  private String format = OBConstants.NLS_DATE_FORMAT;
  
  /** Ausnahmsweise statisch, da application-weit */ 
  public static int maxClobLength=OBAttribute.NULL;
  
  /**
   *  Moegliche Werte fuer die Benutzung von PreparedStatments
   */
  public static interface PreparedStatmentLevelIntf {
    public static final int ALL = 1;
    public static final int NONE =0;
  }

  /** konkreter Wert fuer die Benutzung von PreparedStatments*/
  public static int preparedStatmentLevel = PreparedStatmentLevelIntf.NONE;
  
  private transient static Logger logger = Logger.getLogger(OBDBObject.class);

  // ---------------------------------------------------------------------------
  // -------- Konstruktor ----------------------------------------------------  
  // ---------------------------------------------------------------------------

  /** Standard-Konstruktor */
  public OBDBObject() {
    // ntbd
  }

  /**
   * Liefert i.A. die DataConnection.
   * Kann ueberschrieben werden, wenn z.B. die MesCon gebraucht wird
   * @param context OBContext, aus dem die Connection geholt wird
   * @return Die fuer das Objekt korrekte Connection
   */
  protected OBConnectionInterface getCorrectConnection(OBContext context) {
    return context.getDataConnection();
  }
  
  // ---------------------------------------------------------------------------
  // -------- Selektions-Methoden ----------------------------------------------  
  // ---------------------------------------------------------------------------

  // -------- Selectionsmethoden, die ein Objekt erwarten ----------------------


  /** Suche nach einem Objekt mit Hilfe des Primaerschluessels
   * @param context
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param pk Prim&auml;rschl&uuml;sselwert
      @param hint Hint an den Optimizer
      @return Das gefundene Objekt
   * @throws OBException
      @throws OBException wenn der Datensatz nicht gefunden wurde oder nicht eindeutig war (bei Views)
  */
  public static OBObject find(OBContext context, 
                                OBObject example, 
                                long pk,
                                String hint) throws OBException {
    if (pk==OBConstants.IRREGULAR_INT) {
      throw new OBException(OBException.OBErrorNumber.pkValueIrregular1, new String[]{example.getSQLName(), pk+""}); //$NON-NLS-1$
    }
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      String whereClause = " WHERE " + example.getPrimaryKeyName() + " = " + pk + " "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      return find(context,example,whereClause,null,hint);
    }
    else {
      String whereClause = " WHERE " + example.getPrimaryKeyName() + " = ? "; //$NON-NLS-1$ //$NON-NLS-2$
      ArrayList<String> replArr = new ArrayList<String>();
      replArr.add(String.valueOf(pk));
      return find(context,example,whereClause,replArr,hint);
    }
  }

  /** Suche nach einem Objekt mit Hilfe eines Beispiel-Objektes
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param filter Ein Beispiel-Objekt, wie das zu suchende aussehen soll
      @return Das gefundene Objekt
      @throws OBException wenn der Datensatz nicht gefunden wurde oder nicht eindeutig war
  */
  public static OBObject find(OBContext context, 
                              OBObject example, 
                              OBObject filter) throws OBException {

    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      String whereClause = " " + filter.getWhereClauseFromFilter(null, filter.getCaseSensitive()) + " "; //$NON-NLS-1$ //$NON-NLS-2$
      return find(context,example, whereClause, filter.getHint());
    }
    else {
      ArrayList<String> replArray = new ArrayList<String>();
      String whereClause = " " + filter.getWhereClauseFromFilterForPreparedStatement(null, filter.getCaseSensitive(), replArray);
      return find(context, example, whereClause, replArray, filter.getHint());
    }
  }

                                   
  /** Suche nach einem Objekt mit Hilfe einer SQL-Bedingung
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param hint Hint an den Optimizer
      @return Das gefundene Objekt
      @throws OBException wenn der Datensatz nicht gefunden wurde oder nicht eindeutig war
  */
  public static OBObject find(OBContext context, 
                                OBObject example, 
                                String whereClause,
                                String hint) throws OBException {
    return find(context,example,whereClause,null,hint);
  }

  /** Suche nach einem Objekt mit Hilfe einer SQL-Bedingung
    @param context Durchzureichendes Context-Objekt
    @param example Beispielobjekt, damit die Methode statisch sein kann.
    @param whereClause Bedingung, muss mit WHERE beginnen
    @param hint Hint an den Optimizer
    @return Das gefundene Objekt
    @throws OBException wenn der Datensatz nicht gefunden wurde oder nicht eindeutig war
   */
  public static OBObject find(OBContext context, 
                              OBObject example, 
                              String whereClause,
                              ArrayList<String> replArray,
                              String hint) throws OBException {
    String[] sqlString = getStatement(context,example, 0, whereClause, hint, null); 
    OBListObject<OBObject> list = null;
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      list = executeSqlStatement(context, example, 
                                 sqlString, 
                                 "",    // orderBy macht bei einem Datensatz keinen Sinn //$NON-NLS-1$
                                 0,     // der erste
                                 2,     // Gebraucht wird einer, wenn es einen 2. gibt->Fehler
                                 null); // Attribut-Liste, bisher nur default noetig
    }
    else {
      list = _executePreparedSqlStatement(context, example, 
                                          sqlString, 
                                          replArray,
                                          "",    // orderBy macht bei einem Datensatz keinen Sinn //$NON-NLS-1$
                                          0,     // der erste
                                          2,     // Gebraucht wird einer, wenn es einen 2. gibt->Fehler
                                          null); // Attribut-Liste, bisher nur default noetig
      
    }
    String name = example.getClass().getName();
    name = name.substring(name.lastIndexOf('.')+1, name.length());
                                
    // Es kann nur einen geben ...
    int count = list.size();
    if (count==0) {
      throw new OBException (OBException.OBErrorNumber.objectNotFound1, new String[] {name});
    }
    else if (count>1) {
      throw new OBException (OBException.OBErrorNumber.objectNotUnique1, new String[] {name});
    }

    return list.elementAt(0);
  }


  // -------- Selectionsmethoden, die eine Liste von Objekten erwarten ---------


  /** Suche nach mehreren Objekten mit Hilfe der PKs und einer Ordnungsbedingung
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param pks ARRAY der Prim&auml;rschl&uuml;ssel
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @param hint Hint an den Optimizer
      @return List-Objekt, das die gefundenen Objekte enth&auml;lt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static <E extends OBObject> OBListObject<E> findAll (OBContext context, 
                                                E example, 
                                                long[] pks, 
                                                String orderBy, 
                                                String hint) throws OBException {
    if (pks.length==0) return new OBListObject<E>();

    final int max = 1000;
    
    ArrayList<String> replArray = new ArrayList<String>();
    
    if (pks.length<max) {
      String whereClause = " WHERE " + example.getPrimaryKeyName() + " IN ("; //$NON-NLS-1$ //$NON-NLS-2$
      for (int i=0; i<pks.length; i++) {
        if (i!=0) {
          whereClause += ", "; //$NON-NLS-1$
        }
        if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
          whereClause += pks[i];
        }
        else {
          whereClause+="?";
          replArray.add(String.valueOf(pks[i]));
        }
      }
      whereClause += ") "; //$NON-NLS-1$
      return findAll(context,example,whereClause,replArray, orderBy, hint,0,INFINITE_ROWS,null);
    }
    else {
      String whereClause = " WHERE " + example.getPrimaryKeyName() + " IN ("; //$NON-NLS-1$ //$NON-NLS-2$
      for (int i=0; i<pks.length; i++) {
        if (i>0 && i%max==0) {
          whereClause += ") OR "+ example.getPrimaryKeyName() + " IN ("; //$NON-NLS-1$ //$NON-NLS-2$
        }
        else if (i%max!=0) {
          whereClause += ", "; //$NON-NLS-1$
        }
        if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
          whereClause += pks[i];
        }
        else {
          whereClause+="?";
          replArray.add(String.valueOf(pks[i]));
        }
      }
      whereClause += ") "; //$NON-NLS-1$
      return findAll(context,example,whereClause, replArray, orderBy, hint,0,INFINITE_ROWS,null);
    }
  }

  /** Suche nach mehreren Objekten mit Hilfe eines Beispielobjektes
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param filter Ein Beispiel-Objekt, wie die zu suchenden aussehen sollen
      @return List-Objekt, das die gefundenen Objekte enth&auml;lt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static <E extends OBObject> OBListObject<E> findAll (OBContext context, 
                                                              E example, 
                                                              OBObject filter) throws OBException {
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      String whereClause = " " + filter.getWhereClauseFromFilter(null, filter.getCaseSensitive()) + " "; //$NON-NLS-1$ //$NON-NLS-2$
      return findAll (context,example, whereClause,filter.getOrderClause(),filter.getHint(), filter.getIgnoreFirstRows(), filter.getMaxRowsSelect(),filter.getAttribs());
    }
    else {
      ArrayList<String> replArray = new ArrayList<String>();
      String whereClause = " " + filter.getWhereClauseFromFilterForPreparedStatement(null,filter.getCaseSensitive(), replArray) + " "; //$NON-NLS-1$ //$NON-NLS-2$
      return findAll (context,example, whereClause, replArray, filter.getOrderClause(),filter.getHint(), filter.getIgnoreFirstRows(), filter.getMaxRowsSelect(),filter.getAttribs());
      
    }
  }


  /** Suche nach mehreren Objekten mit Hilfe einer SQL-Bedingung
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @param hint Hint an den Optimizer
      @return List-Objekt, das die gefundenen Objekte enth&auml;lt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static <E extends OBObject> OBListObject<E> findAll (OBContext context, 
                                                              E example, 
                                                              String whereClause,
                                                              String orderBy,
                                                              String hint) throws OBException {
    return findAll(context, example, 
                   whereClause, orderBy, hint,
                   INFINITE_ROWS, // maxRows
                   null); // attribs
  }

  /** Suche nach mehreren Objekten mit Hilfe einer SQL-Bedingung
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @param hint Hint an den Optimizer
      @param maxRows H&ouml;chstzahl dr gelieferten Datens&auml;tze
      @param attribs zu selektierende Spalten
      @return List-Objekt, das die gefundenen Objekte enth&auml;lt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static <E extends OBObject> OBListObject<E> findAll (OBContext context, 
                                                              E example, 
                                                              String whereClause,
                                                              String orderBy,
                                                              String hint,
                                                              int maxRows,
                                                              OBAttribute[] attribs) throws OBException {
    return findAll(context, example, 
                   whereClause, orderBy, hint, 
                   0, maxRows, attribs);
  }

  /** Suche nach mehreren Objekten mit Hilfe einer SQL-Bedingung
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @param hint Hint an den Optimizer
      @param ignoreFirstLines Nummer der ersten Zeile (Zaehlung beginnt mit 0)
      @param maxRows H&ouml;chstzahl dr gelieferten Datens&auml;tze
      @param attribs zu selektierende Spalten
      @return List-Objekt, das die gefundenen Objekte enth&auml;lt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static <E extends OBObject> OBListObject<E> findAll (OBContext context, 
                                                              E example, 
                                                              String whereClause,
                                                              String orderBy,
                                                              String hint,
                                                              int ignoreFirstLines,
                                                              int maxRows,
                                                              OBAttribute[] attribs) throws OBException {
    return findAll(context, example, whereClause, null, orderBy, hint, ignoreFirstLines, maxRows, attribs);
  }

  /** Suche nach mehreren Objekten mit Hilfe einer SQL-Bedingung
    @param context Durchzureichendes Context-Objekt
    @param example Beispielobjekt, damit die Methode statisch sein kann.
    @param whereClause Bedingung, muss mit WHERE beginnen
    @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
    @param hint Hint an den Optimizer
    @param ignoreFirstLines Nummer der ersten Zeile (Zaehlung beginnt mit 0)
    @param maxRows H&ouml;chstzahl dr gelieferten Datens&auml;tze
    @param attribs zu selektierende Spalten
    @return List-Objekt, das die gefundenen Objekte enth&auml;lt
    @throws OBException Auftretende SQL-Exceptions
  */
  public static <E extends OBObject> OBListObject<E> findAll (OBContext context, 
                                                              E example, 
                                                              String whereClause,
                                                              ArrayList<String> replArray,
                                                              String orderBy,
                                                              String hint,
                                                              int ignoreFirstLines,
                                                              int maxRows,
                                                              OBAttribute[] attribs) throws OBException {
    String[] sqlString = getStatement(context,example, 0, whereClause, hint, attribs);
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      return executeSqlStatement(context, example, 
                                 sqlString, orderBy, 
                                 ignoreFirstLines, maxRows, attribs);
    }
    else {
      return _executePreparedSqlStatement(context, example, 
                                          sqlString,
                                          replArray,
                                          orderBy, 
                                          ignoreFirstLines, maxRows, attribs);
    }
  }

  // -------- Selectionsmethoden, die die Anzahl der gefundenen Objekte liefern ----

  /** Suche mit Hilfe der PKs
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param pks ARRAY der Prim&auml;rschl&uuml;ssel
      @return Anzahl der gefundenen Objekte
      @throws OBException Auftretende SQL-Exceptions
  */
  public static int count (OBContext context, 
                           OBObject example, 
                           long[] pks) throws OBException {
    if (pks.length==0) return 0;

    final int max = 1000;
    
    ArrayList<String> replArray = new ArrayList<String>();
    
    if (pks.length<max) {
      String whereClause = " WHERE " + example.getPrimaryKeyName() + " IN ("; //$NON-NLS-1$ //$NON-NLS-2$
      for (int i=0; i<pks.length; i++) {
        if (i!=0) {
          whereClause += ", "; //$NON-NLS-1$
        }
        if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
          whereClause += pks[i];
        }
        else {
          whereClause+="?";
          replArray.add(String.valueOf(pks[i]));
        }
      }
      whereClause += ") "; //$NON-NLS-1$
      return count(context,example,whereClause,replArray);
    }
    else {
      String whereClause = " WHERE " + example.getPrimaryKeyName() + " IN ("; //$NON-NLS-1$ //$NON-NLS-2$
      for (int i=0; i<pks.length; i++) {
        if (i>0 && i%max==0) {
          whereClause += ") OR "+ example.getPrimaryKeyName() + " IN ("; //$NON-NLS-1$ //$NON-NLS-2$
        }
        else if (i%max!=0) {
          whereClause += ", "; //$NON-NLS-1$
        }
        if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
          whereClause += pks[i];
        }
        else {
          whereClause+="?";
          replArray.add(String.valueOf(pks[i]));
        }
      }
      whereClause += ") "; //$NON-NLS-1$
      return count(context,example,whereClause,replArray);
    }
  }

  /** Suche mit Hilfe eines Beispielobjektes
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param filter Ein Beispiel-Objekt, wie die zu suchenden aussehen sollen
      @return Anzahl der gefundenen Objekte
      @throws OBException Auftretende SQL-Exceptions
  */
  public static int count (OBContext context, 
                           OBObject example, 
                           OBObject filter) throws OBException {
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      String whereClause = " " + filter.getWhereClauseFromFilter(null, filter.getCaseSensitive()) + " "; //$NON-NLS-1$ //$NON-NLS-2$
      return count(context,example, whereClause);
    }
    else {
      ArrayList<String> replArray = new ArrayList<String>();
      String whereClause = filter.getWhereClauseFromFilterForPreparedStatement(null, filter.getCaseSensitive(), replArray);
      return count(context,example,whereClause,replArray);
    }
  }

  /** Suche mit Hilfe einer SQL-Bedingung
      @param context Durchzureichendes Context-Objekt
      @param example Beispielobjekt, damit die Methode statisch sein kann.
      @param whereClause Bedingung, muss mit WHERE beginnen
      @return Anzahl der gefundenen Objekte
      @throws OBException Auftretende SQL-Exceptions
  */
  public static int count (OBContext context, 
                           OBObject example, 
                           String whereClause) throws OBException {
    return count(context,example,whereClause,null);
  }

  /** Suche mit Hilfe einer SQL-Bedingung
    @param context Durchzureichendes Context-Objekt
    @param example Beispielobjekt, damit die Methode statisch sein kann.
    @param whereClause Bedingung, muss mit WHERE beginnen
    @return Anzahl der gefundenen Objekte
    @throws OBException Auftretende SQL-Exceptions
  */
  public static int count (OBContext context, 
                           OBObject example, 
                           String whereClause,
                           ArrayList<String> replArray) throws OBException {
    String[] sqlString = getStatement(context,example, 0, whereClause, "", null); //$NON-NLS-1$
    if (preparedStatmentLevel==PreparedStatmentLevelIntf.NONE) {
      return executeCountStatement(context, example, sqlString[1]);
    }
    else {
      return executePreparedCountStatement(context, example, sqlString[1],replArray);
    }
  }

  /** Gibt die Lock-Connection frei
   * @param con
   */
  public static void giveBackLockConnection(OBConnectionInterface con) {
    logger.debug("" + con); //$NON-NLS-1$
  }

  /** Setzt das Datumsformat.
      @param newFormat Datumsformat in Oracle-Notation
   */
  public void setDateFormat(String newFormat) {
    format=newFormat;
  }

  /** Liefert das Datumsformat.
      @return Datumsformat in Oracle-Notation
   */
  public String getDateFormat() {
    return format;
  }

  // ---------------------------------------------------------------------------
  // ------- Kleinere Methoden -------------------------------------------------
  // ---------------------------------------------------------------------------

  /** Liefert das Objekt aus der Datenbank, das den primary Key des
      aktuellen Objektes besitzt, auch wenn im aktuellen Objekt Attribute geaendert worden sind.
      Kann im update benutzt werden, um die alten Werte des zu aendernden Objektes zu bekommen.
      Die Tabelle, aus der das Objekt gelesen wird, ergibt sich aus dem tableName von this
   * @param context
   * @return Das Objektz, wie es in der DB steht
   * @throws OBException
  */
  public OBObject getObjectFromDB(OBContext context) throws OBException {
    return find(context, this, getPrimaryKey(),""); //$NON-NLS-1$
  }

  protected String getPreparedStatementStringForDebug(String sql,
                                                      ArrayList<String> replArray,
                                                      ArrayList<OBAttribute> hasLOBs,
                                                      long key, long lock) {
    return getPreparedStatementStringForDebug(sql, replArray, hasLOBs, key, lock, attArr);
  }
  
  protected static String getPreparedStatementStringForDebug(String sql,
                                                             ArrayList<String> replArray,
                                                             ArrayList<OBAttribute> hasLOBs,
                                                             long key, long lock,
                                                             OBAttribute[] attArr) {
    StringBuffer retVal = new StringBuffer(sql);
    int countRepl=0;
    if (replArray!=null && replArray.size()>0) {
      retVal.append(" [");
      for (int i = 0; i < replArray.size(); i++) {
        if (i>0) retVal.append(",");
        retVal.append(replArray.get(i));
        countRepl++;
      }
      retVal.append("]");
    }
    if (hasLOBs!=null && hasLOBs.size()>0) {
      retVal.append(", LOBS [");
      for (int i = 0; i < hasLOBs.size(); i++) {
        if (i>0) retVal.append(",");
        if (hasLOBs.get(i).type==OBConstants.CLOB) {
          retVal.append(hasLOBs.get(i).name).append(":");
          if (hasLOBs.get(i).value.length()>100) {
            retVal.append(hasLOBs.get(i).value.substring(0, 100));
          }
          else {
            retVal.append(hasLOBs.get(i).value);
          }
        }
        if (hasLOBs.get(i).type==OBConstants.BLOB) {
          retVal.append(hasLOBs.get(i).name).append(":").append(hasLOBs.get(i).bvalue.length);
        }
        countRepl++;
      }
      retVal.append("]");
    }
    if (key!=OBAttribute.NULL) {
      retVal.append(", key:").append(key);
      countRepl++;
    }
    if (lock!=OBAttribute.NULL) {
      retVal.append(", lock:").append(lock);
      countRepl++;
    }
    retVal.append(" Total: ").append(countRepl);
    return hide(retVal.toString(),attArr);
  }

  /**
   * Liefert den naechsten Wert der Sequenz zur angegebenen Tabelle
   * @param context
   * @param projectSchema Projekt, zu dem die Sequenz gehoert, z.B. "ipnet"
   * @param sequenceName
   * @return Nextval der Sequenz
   * @throws OBException
   */
  public static long getNextKeyVal(OBContext context, String projectSchema, String sequenceName) throws OBException {
    String query = OBDriver.getNextKeyValStatement(context,context.getSchema(projectSchema),sequenceName);
    long value;
    Statement stmt = null;
    try {
      stmt = context.getDataConnection().createStatement();
      ResultSet rs = stmt.executeQuery(query);
      rs.next();
      value = rs.getLong(1);
      rs.close();
    }
    catch (SQLException e) {
      logger.debug(XynaContextFactory.getSessionData(context) + 
                   "Error getting nextval of sequence", e);//$NON-NLS-1$
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[]{e.getMessage()});
    }
    finally {
      try {
        if (stmt!=null) stmt.close();
      }
      catch (Exception e) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closin statement", e);//$NON-NLS-1$
      }
    }
    return value;
  }


  /**
   * Liefert den aktuellen Wert der Sequenz zur angegebenen Tabelle
   * @param context
   * @param projectSchema Projekt, zu dem die Sequenz gehoert, z.B. "ipnet"
   * @param sequenceName
   * @return currVal der Sequenz
   * @throws OBException
   */
  public static long getCurrentKeyVal(OBContext context, String projectSchema, String sequenceName) throws OBException {
    String query = OBDriver.getCurrentKeyValStatement(context,context.getSchema(projectSchema),sequenceName);
    long value;
    Statement stmt = null;
    try {
      stmt = context.getDataConnection().createStatement();
      ResultSet rs = stmt.executeQuery(query);
      rs.next();
      value = rs.getLong(1);
      rs.close();
    }
    catch (SQLException e) {
      logger.debug(XynaContextFactory.getSessionData(context) + 
                   "error getting currentval of sequence",e);//$NON-NLS-1$
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[]{e.getMessage()});
    }
    finally {
      try {
        if (stmt!=null) stmt.close();
      }
      catch (Exception e) {
        logger.error(XynaContextFactory.getSessionData(context) + "error closing statement", e);//$NON-NLS-1$
      }
    }
    return value;
  }

  // ---------------------------------------------------------------------------
  // ------- Fuer das Select benoetigte Methoden -------------------------------
  // ---------------------------------------------------------------------------

  /** Baut den kompletten SQL-Befehl (au&szlig;er ORDER BY) zusammen.
   * @param context
   * @param example
   * @param key
   * @param whereClause
   * @param hint
   * @param attribs
   * @return Kompleter SQL-Befehl in den Varianten select [0] und count [1]
   * @throws OBException
  */
  private static String[] getStatement(OBContext context,
                                       OBObject example, 
                                       long key, String whereClause, 
                                       String hint,
                                       OBAttribute[] attribs) throws OBException {
    String attribSelect = ""; //$NON-NLS-1$
    String sqlString = ""; //$NON-NLS-1$
    attribSelect = "SELECT "; //$NON-NLS-1$
    
    if (hint!=null && hint.length()>0) {
      attribSelect += hint + " "; //$NON-NLS-1$
    }

    if (attribs != null) {
      for (int i=0; i<attribs.length-1; i++) {
        // try-catch zur Fehlersuche (falsch geschriebenes Attribut ist klar, aber welches!)
        // Eventuell fehlt aber auch eine Primary-Key-Definition in einem View, wenn dieser in einer Select-Maske dargestellt werden soll
        try { 
          attribSelect = attribSelect + attribs[i].getSelectName() + ", "; //$NON-NLS-1$
        }
        catch (NullPointerException e) {
          logger.error(XynaContextFactory.getSessionData(context) + "error constructing attriblist " + i,e);//$NON-NLS-1$
          throw e;
          
        }
      }
      attribSelect = attribSelect + attribs[attribs.length-1].getSelectName();
    }
    else {
      attribSelect += example.tableSelect();
    }
    
    attribSelect = attribSelect + " FROM " + OBDriver.getTableName(context,context.getSchema(example.getProjectSchema()), //$NON-NLS-1$
                                                                   example.getSQLName());
    
    String sqlStringCount = "SELECT COUNT(*) FROM " + OBDriver.getTableName(context,context.getSchema(example.getProjectSchema()), //$NON-NLS-1$
                                                                     example.getSQLName());
    if (key > 0) {
      sqlString = attribSelect + " WHERE " + example.getPrimaryKeyName() + " = " + key; //$NON-NLS-1$ //$NON-NLS-2$
      sqlStringCount +=  " WHERE " + example.getPrimaryKeyName() + " = " + key; //$NON-NLS-1$ //$NON-NLS-2$
    }
    else {
      sqlString = attribSelect + " " + whereClause; //$NON-NLS-1$
      sqlStringCount += " " + whereClause; //$NON-NLS-1$
    }
    return new String[] {sqlString,sqlStringCount};
  }

  
  /**
   * @param context
   * @param sqlString
   * @return ersetztes Projekt-Schema
   * @throws OBException
   */
  protected static String replaceProjectSchemata(OBContext context, String sqlString) throws OBException {
    String retVal = sqlString;
    Enumeration<String> en = context.getSchemata();
    while (en.hasMoreElements()) {
      String project = en.nextElement();
      retVal = retVal.replaceAll(START_PROJECT_SCHEMA + project + END_PROJECT_SCHEMA, 
                                       context.getSchema(project));
    }
    return retVal;
  }
  
  
  /** Methode, die ein SQL-Select ausfuehrt
   * @param context
      @param example Beispielobjekt, benoetigt fuer Typ-Informationen
      @param sqlStringArray SQL-Statement ohne ORDER BY-Clause fuer select[0] und count[1]
      @param orderBy ORDER BY-Clause
   * @param ignoreFirstLines Anzahl der Zeilen, die am Anfang übersprungen werden sollen
      @param maxRows Maximale Anzahl selektierter Zeilen
      @param attribs Array zu Selektierender Spalten - null bedeutet: Nimm default
      @return ListenObject mit gefundenen Werten
   * @throws OBException
   */
  @SuppressWarnings("unchecked")
  public static <E extends OBObject> OBListObject<E> _executeSqlStatement(OBContext context,
                                                                          E example, 
                                                                          String[] sqlStringArray,
                                                                          String orderBy,
                                                                          int ignoreFirstLines,
                                                                          int maxRows,
                                                                          OBAttribute[] attribs) throws OBException {
    if (attribs != null) {
      example.attArr = attribs;
    }
    OBListObject<E> res = new OBListObject<E>();
    res.setFirstLine(ignoreFirstLines);
    int count;
    String sqlString = replaceProjectSchemata(context,sqlStringArray[0] + " " + orderBy); //$NON-NLS-1$
    logger.debug(XynaContextFactory.getSessionData(context) + 
                 example.getTableName()+": "+sqlString); //$NON-NLS-1$
    
    Statement stmt=null;
    OBConnectionInterface corCon= context.getDataConnection();
    try {
      if (example instanceof OBDBObject) {
        corCon = ((OBDBObject)example).getCorrectConnection(context);
      }
      stmt = corCon.createStatement();
      OBAttribute a;
      count = 0;
      Class<? extends OBObject> cl = example.getClass();
      ResultSet rs = stmt.executeQuery(sqlString);
      String value = ""; //$NON-NLS-1$
      E to;
      // die ignoreFirstLiness ueberlesen
      while(!corCon.isClosed() && 
            count<ignoreFirstLines && rs.next()
            ) {
        count++;
      }
      while(!corCon.isClosed() && 
            rs.next() && count<maxRows
            ) {
        // hier wird reflection benutzt!
        to = (E) cl.newInstance();
        
        if (attribs != null) {
          to.attArr = new OBAttribute[attribs.length];
        }
        count++;

        for (int j=0; j<example.attArr().length; j++) {
          if (attribs != null) {
            to.attArr[j] = new OBAttribute(attribs[j].name, attribs[j].type, attribs[j].length,attribs[j].nullable);
          }
          a = to.attArr[j];
          
          if (a.type!=OBConstants.BLOB) {
            value = ifnull(rs.getString(j+1),""); //$NON-NLS-1$
          }
          switch(a.type) {
          case OBConstants.INTEGER:
          case OBConstants.LONG:
          case OBConstants.BOOLEAN:
            // getString liefert 0.0 statt 0 bei int-Wert 0
            if (value.equals("0.0")) value = "0"; //$NON-NLS-1$ //$NON-NLS-2$
            break;
          case OBConstants.DOUBLE:
          case OBConstants.DATE:
          case OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE:
          case OBConstants.STRING:
            break;
          case OBConstants.LONGVARCHAR:
            break;
          case OBConstants.CLOB:
            Clob clob = rs.getClob(j+1);
            if (clob!=null) {
              BufferedReader br = new BufferedReader(clob.getCharacterStream());
              String aux;
              StringBuffer strb = new StringBuffer();
              while ((aux = br.readLine())!= null) {
                // Sicherung gegen OutOfMemory
                if (maxClobLength!=OBAttribute.NULL && strb.length()+aux.length()>maxClobLength) {
                  strb.append(aux.substring(0,maxClobLength-strb.length()));
                  break;
                }
                strb.append(aux).append('\n'); // Zeilenumbruch!  
              }
              br.close();

              value= strb.toString();

             // Sicherung gegen OutOfMemory
//              int cloblength = (int)clob.length();
//              if (maxClobLength!=OBAttribute.NULL && cloblength>maxClobLength) {
//                cloblength=maxClobLength;
//              }
//              byte[] rawBuf = new byte[cloblength];
//              is.read(rawBuf);
//              value = new String(rawBuf);
//              is.close();
            }
            break;
          case OBConstants.BLOB:
            Blob blob = rs.getBlob(j+1);
            if (blob!=null) {
              InputStream is = blob.getBinaryStream();
              byte[] rawBuf = new byte[(int)blob.length()];
              is.read(rawBuf);
              is.close();
              a.setValue(rawBuf);
            }
            else {
              a.bvalue=new byte[0];
            }
            break;
          default:
            throw new SQLException(OBConstants.WRONG_ATTRIBUTE_TYPE);
          }
          
          a.setValue(value);
        }
        res.add(to);
      } // while(rs.next())
      stmt.close();
      res.setTotalLines(count);
      try {
        if (count>=maxRows && 
            sqlStringArray.length>1 && 
            sqlStringArray[1]!=null && 
            sqlStringArray[1].length()>0) {
          res.setTotalLines(executeCountStatement(context,example,sqlStringArray[1]));      
        }
      } 
      catch (Exception e) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error executing statement", e);//$NON-NLS-1$
      }
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error executing statement", e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      String tmpStr = handleSQLException(context,e, corCon);
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[] {e.getMessage() + ": "+ tmpStr}); //$NON-NLS-1$
    }
    catch( Exception e ) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error executing statement", e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[]{e.getMessage()});
    }
    return res;
  }

  /** Methode, die ein SQL-Select ausfuehrt
   * @param context
      @param example Beispielobjekt, benoetigt fuer Typ-Informationen
      @param sqlStringArray SQL-Statement ohne ORDER BY-Clause fuer select[0] und count[1]
      @param orderBy ORDER BY-Clause
   * @param ignoreFirstLines Anzahl der Zeilen, die am Anfang übersprungen werden sollen
      @param maxRows Maximale Anzahl selektierter Zeilen
      @param attribs Array zu Selektierender Spalten - null bedeutet: Nimm default
      @return ListenObject mit gefundenen Werten
   * @throws OBException
   */
  @SuppressWarnings("unchecked")
  public static <E extends OBObject> OBListObject<E> _executePreparedSqlStatement(OBContext context,
                                                                                  E example, 
                                                                                  String[] sqlStringArray,
                                                                                  ArrayList<String> replArray,
                                                                                  String orderBy,
                                                                                  int ignoreFirstLines,
                                                                                  int maxRows,
                                                                                  OBAttribute[] attribs) throws OBException {
    if (attribs != null) {
      example.attArr = attribs;
    }
    OBListObject<E> res = new OBListObject<E>();
    res.setFirstLine(ignoreFirstLines);
    int count;
    String sqlString = replaceProjectSchemata(context,sqlStringArray[0] + " " + orderBy); //$NON-NLS-1$
    logger.debug(XynaContextFactory.getSessionData(context) + 
                 example.getTableName()+": "+getPreparedStatementStringForDebug(sqlString, replArray, null, OBAttribute.NULL, -1, example.attArr)); //$NON-NLS-1$
    
    PreparedStatement stmt=null;
    OBConnectionInterface corCon= context.getDataConnection();
    try {
      if (example instanceof OBDBObject) {
        corCon = ((OBDBObject)example).getCorrectConnection(context);
      }
      stmt = corCon.prepareStatement(sqlString);
      for (int i = 0; replArray!=null && i < replArray.size(); i++) {
        stmt.setString(i+1, replArray.get(i));
      }
      OBAttribute a;
      count = 0;
      Class<? extends OBObject> cl = example.getClass();
      ResultSet rs = stmt.executeQuery();
      String value = ""; //$NON-NLS-1$
      E to;
      // die ignoreFirstLiness ueberlesen
      while(!corCon.isClosed() && 
            count<ignoreFirstLines && rs.next()
            ) {
        count++;
      }
      while(!corCon.isClosed() && 
            rs.next() && count<maxRows
            ) {
        // hier wird reflection benutzt!
        to = (E) cl.newInstance();
        
        if (attribs != null) {
          to.attArr = new OBAttribute[attribs.length];
        }
        count++;

        for (int j=0; j<example.attArr().length; j++) {
          if (attribs != null) {
            to.attArr[j] = new OBAttribute(attribs[j].name, attribs[j].type, attribs[j].length,attribs[j].nullable);
          }
          a = to.attArr[j];
          
          if (a.type!=OBConstants.BLOB) {
            value = ifnull(rs.getString(j+1),""); //$NON-NLS-1$
          }
          switch(a.type) {
          case OBConstants.INTEGER:
          case OBConstants.LONG:
          case OBConstants.BOOLEAN:
            // getString liefert 0.0 statt 0 bei int-Wert 0
            if (value.equals("0.0")) value = "0"; //$NON-NLS-1$ //$NON-NLS-2$
            break;
          case OBConstants.DOUBLE:
          case OBConstants.DATE:
          case OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE:
          case OBConstants.STRING:
            break;
          case OBConstants.LONGVARCHAR:
            break;
          case OBConstants.CLOB:
            Clob clob = rs.getClob(j+1);
            if (clob!=null) {
              BufferedReader br = new BufferedReader(clob.getCharacterStream());
              String aux;
              StringBuffer strb = new StringBuffer();
              while ((aux = br.readLine())!= null) {
                // Sicherung gegen OutOfMemory
                if (maxClobLength!=OBAttribute.NULL && strb.length()+aux.length()>maxClobLength) {
                  strb.append(aux.substring(0,maxClobLength-strb.length()));
                  break;
                }
                strb.append(aux).append('\n'); // Zeilenumbruch!  
              }
              br.close();

              value= strb.toString();

             // Sicherung gegen OutOfMemory
//              int cloblength = (int)clob.length();
//              if (maxClobLength!=OBAttribute.NULL && cloblength>maxClobLength) {
//                cloblength=maxClobLength;
//              }
//              byte[] rawBuf = new byte[cloblength];
//              is.read(rawBuf);
//              value = new String(rawBuf);
//              is.close();
            }
            break;
          case OBConstants.BLOB:
            Blob blob = rs.getBlob(j+1);
            if (blob!=null) {
              InputStream is = blob.getBinaryStream();
              byte[] rawBuf = new byte[(int)blob.length()];
              is.read(rawBuf);
              is.close();
              a.setValue(rawBuf);
            }
            else {
              a.bvalue=new byte[0];
            }
            break;
          default:
            throw new SQLException(OBConstants.WRONG_ATTRIBUTE_TYPE);
          }
          
          a.setValue(value);
        }
        res.add(to);
      } // while(rs.next())
      stmt.close();
      res.setTotalLines(count);
      try {
        if (count>=maxRows && 
            sqlStringArray.length>1 && 
            sqlStringArray[1]!=null && 
            sqlStringArray[1].length()>0) {
          res.setTotalLines(executePreparedCountStatement(context,example,sqlStringArray[1],replArray));
        }
      } 
      catch (Exception e) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error executing statement", e);//$NON-NLS-1$
      }
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error executing statement", e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      String tmpStr = handleSQLException(context,e, corCon);
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[] {e.getMessage() + ": "+ tmpStr}); //$NON-NLS-1$
    }
    catch( Exception e ) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error executing statement", e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[]{e.getMessage()});
    }
    return res;
  }

  /** Methode, die ein SQL-Select ausfuehrt, um die vorhandenen Anfangsbuchstaben zu finden
   * @param context 
   * @param nameGroupByAttr Von diesem Attribut werden die Initialen gesucht
   * @param filter Aus diesem View wird selektiert (Tabelle tut es auch), dabei wird es auch als Filter benutzt.
   * @return Liste der vorhandenen Anfangsbuchstaben 
   * @throws OBException
   */
  public static HashSet<String> findInitials(OBContext context,
                                             String nameGroupByAttr,
                                             OBObject filter) throws OBException {
    return findInitials(context, nameGroupByAttr, filter.getWhereClauseFromFilter(null, filter.getCaseSensitive()), filter);
    
  }
  
  /**
   * Liefert die SQL-Technisch korrekte Substitution, um einen Anfangsbuchsteben in A-Z zu erhalten
   * @param nameGroupByAttr Apalte, deren Anfangsbuchstabe gesucht wird
   * @return Komplizierten sql-Teilstring.
   */
  public static String getFirstLetterSQL(String nameGroupByAttr) {
    return "UPPER(SUBSTR(utl_raw.cast_to_varchar2(nlssort(SUBSTR(" + nameGroupByAttr + ",1,1), 'nls_sort=''binary_ai''')),1,1))";//$NON-NLS-1$//$NON-NLS-2$
  }
  
  /** Methode, die ein SQL-Select ausfuehrt, um die vorhandenen Anfangsbuchstaben zu finden
   * @param context 
   * @param nameGroupByAttr Von diesem Attribut werden die Initialen gesucht
   * @param wcClause Einschraenkung
   * @param view Aus diesem View wird selektiert (Tabelle tut es auch)
   * @return Liste der vorhandenen Anfangsbuchstaben 
   * @throws OBException
   */
  public static HashSet<String> findInitials(OBContext context,
                                             String nameGroupByAttr,
                                             String wcClause,
                                             OBObject view) throws OBException {
    String sqlStr = "SELECT DISTINCT " + getFirstLetterSQL(nameGroupByAttr) + " AS ini FROM " +  //$NON-NLS-1$  //$NON-NLS-2$ 
                    OBDriver.getTableName(context,context.getSchema(view.getProjectSchema()),
                                          view.getSQLName()) +
                    " " + wcClause +  //$NON-NLS-1$
                    " ORDER BY 1";//$NON-NLS-1$ 
    HashSet<String> res = new HashSet<String>();
    String sqlString = replaceProjectSchemata(context,sqlStr ); 
    logger.debug(XynaContextFactory.getSessionData(context) + 
                 view.getTableName()+": "+sqlString); //$NON-NLS-1$
    // FIXME Das muesste es auch mit Prepared Statements geben
    Statement stmt=null;
    OBConnectionInterface corCon= context.getDataConnection();
    try {
      if (view instanceof OBDBObject) {
        corCon = ((OBDBObject) view).getCorrectConnection(context);
      }
      stmt = corCon.createStatement();
      ResultSet rs = stmt.executeQuery(sqlString);
      while(!corCon.isClosed() && 
            rs.next()) {
        String initial = rs.getString(1);
        // nur A-Z 
        if (initial!=null && initial.compareTo("A")>=0 && initial.compareTo("Z")<=0) {//$NON-NLS-1$//$NON-NLS-2$
          res.add(initial);
        }
      }
      stmt.close();
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error finding initials", e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      String tmpStr = handleSQLException(context,e, corCon);
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[] {e.getMessage() + ": "+ tmpStr}); //$NON-NLS-1$
    }
    catch( Exception e ) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error finding initials", e);//$NON-NLS-1$
      try {
        if (stmt!=null) stmt.close();
      }
      catch(Exception exp) {
        //System.exit(1);
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[]{e.getMessage()});
    }
    return res;
  }

  // ---------------------------------------------------------------------------
  // ------- Methoden speziell fuer die count-Abfragen -------------------------
  // ---------------------------------------------------------------------------

  /** Methode, die ein SQL-Select count(*) ausfuehrt
   * @param context
      @param example Beispielobjekt, benoetigt fuer Typ-Informationen
      @param sqlStringIn SQL-Statement
      @return Anzahl der gefundenen Datensaetze
   * @throws OBException
   */
  private static int executeCountStatement(OBContext context, 
                                           OBObject example,
                                           String sqlStringIn) throws OBException {
    String sqlString = replaceProjectSchemata(context,sqlStringIn);
    logger.debug(XynaContextFactory.getSessionData(context) + 
                 example.getTableName()+" : "+sqlString); //$NON-NLS-1$ 
    Statement stmt = null;
    OBConnectionInterface corCon = context.getDataConnection();
    try {
      if (example instanceof OBDBObject) {
        corCon = ((OBDBObject) example).getCorrectConnection(context);
      }
      stmt = corCon.createStatement();
      ResultSet rs = stmt.executeQuery(sqlString);
      int count = -1;
      if (rs.next()) {
        count = rs.getInt(1);
      }
      stmt.close();
      return count;
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error executing count", e);//$NON-NLS-1$
      try {
        if (stmt!=null) {
          stmt.close();
        }
      }
      catch (Exception exp) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, 
                            new String[]{handleSQLException(context,e, corCon)});
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error executing count", e);//$NON-NLS-1$
      try {
        if (stmt!=null) {
          stmt.close();
        }
      }
      catch (Exception exp) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[]{e.getMessage()});
    }
  }

  /** Methode, die ein SQL-Select count(*) ausfuehrt
   * @param context
      @param example Beispielobjekt, benoetigt fuer Typ-Informationen
      @param sqlStringIn SQL-Statement
      @return Anzahl der gefundenen Datensaetze
   * @throws OBException
   */
  private static int executePreparedCountStatement(OBContext context, 
                                                   OBObject example,
                                                   String sqlStringIn,
                                                   ArrayList<String> replArray) throws OBException {
    String sqlString = replaceProjectSchemata(context,sqlStringIn);
    logger.debug(XynaContextFactory.getSessionData(context) + 
                 example.getTableName()+" : "+getPreparedStatementStringForDebug(sqlString, replArray, null, OBAttribute.NULL, -1, new OBAttribute[0])); //$NON-NLS-1$ 
    PreparedStatement stmt = null;
    OBConnectionInterface corCon = context.getDataConnection();
    try {
      if (example instanceof OBDBObject) {
        corCon = ((OBDBObject) example).getCorrectConnection(context);
      }
      stmt = corCon.prepareStatement(sqlString);
      for (int i = 0; replArray!=null &&  i < replArray.size(); i++) {
        stmt.setString(i+1, replArray.get(i));
      }
      ResultSet rs = stmt.executeQuery();
      int count = -1;
      if (rs.next()) {
        count = rs.getInt(1);
      }
      stmt.close();
      return count;
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error executing count", e);//$NON-NLS-1$
      try {
        if (stmt!=null) {
          stmt.close();
        }
      }
      catch (Exception exp) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, 
                            new String[]{handleSQLException(context,e, corCon)});
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error executing count", e);//$NON-NLS-1$
      try {
        if (stmt!=null) {
          stmt.close();
        }
      }
      catch (Exception exp) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[]{e.getMessage()});
    }
  }

  /** Methode, die das Aktuelle Datum der DB erfragt
   * @param context
     @param format Format, in dem die Zeit geliefert werden soll
     @return Anzahl der gefundenen Datensaetze
   * @throws OBException
  */
  public static String getCurrentTime(OBContext context, 
                                      String format) throws OBException {
    Statement stmt = null;
    String sqlString = "SELECT TO_CHAR(sysdate,'"+format+"') FROM DUAL"; //$NON-NLS-1$ //$NON-NLS-2$
    try {
      stmt = context.getDataConnection().createStatement();
      ResultSet rs = stmt.executeQuery(sqlString);
      String ct = ""; //$NON-NLS-1$
      if (rs.next()) {
        ct = rs.getString(1);
      }
      stmt.close();
      return ct;
    }
    catch (SQLException e) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error getting current time", e);//$NON-NLS-1$
      try {
        if (stmt!=null) {
          stmt.close();
        }
      }
      catch (Exception exp) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, 
                            new String[]{handleSQLException(context,e, context.getDataConnection())});
    }
    catch (Exception e) {
      logger.error(XynaContextFactory.getSessionData(context) + 
                   "error getting current time", e);//$NON-NLS-1$
      try {
        if (stmt!=null) {
          stmt.close();
        }
      }
      catch (Exception exp) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error closing statement", exp);//$NON-NLS-1$
        throw new OBException(OBException.OBErrorNumber.sqlFatalException); 
      }
      throw new OBException(OBException.OBErrorNumber.sqlException1, new String[]{e.getMessage()});
    }
  }

 
  // ---------------------------------------------------------------------------
  // ------- SQL-Hilfsmethoden / Exception-Handling ----------------------------
  // ---------------------------------------------------------------------------
  
  
  /** 
   * Methode zur globalen Fehlerbehandlung. 
   * Hier werden die Oracle-Fehlermeldungen in eine lesbarere Form uebersetzt.
   * @param context Durchzureichendes Context-Objekt
   * @param e Die zu behandelnde Exception
   * @param con Connection zur Datenbank
   * @return String fuer die Ausgabe in einer umgewandelten Form
   * @throws OBException
   */
  public static final String handleSQLException(OBContext context, SQLException e, OBConnectionInterface con) throws OBException {
    String back = ""; //$NON-NLS-1$
    if (e.getErrorCode()==1) {
      // unique constraint abfangen
      back = handleUnique(context, e, con);
    }
    else if (e.getErrorCode()==1400) {
      // not null constraint abfangen
      back = handleNotNull(context, e, con);
    }
    else if (e.getErrorCode()==1401 || e.getErrorCode()==12899) {
      // insertet value too large for column
      back = context.getMessageGenerator().generateOraMessage(OBException.OBErrorNumber.insertedValueToLarge);
    }
    else if (e.getErrorCode()==2091) {
      // Transaktion wurde zurückgesetzt, kommt bei Deferable-Constraints vor
      back = handleTransaktionsRollback(context, e, con);
    }
    else if (e.getErrorCode()==2290) {
      // check Constraint
      back = handleCheck(context, e, con);
    }
    else if (e.getErrorCode()==2292) {
      // child Record found
      back = handleChildRecord(context, e, con);
    }
    else if (e.getErrorCode()==2291) {
      // parent Record not found
      back = handleParentRecord(context, e, con);
    }
    else if (e.getErrorCode()==28 || e.getErrorCode()==1012) {
      // session killed
      back = handleConnectionLost(e);
    }
    else if (e.getErrorCode()==1031) {
      back = "Missing rights"; //$NON-NLS-1$
    }
    else {
      back = e.getMessage() + "\nStatus: " + e.getSQLState() + "\nCode: " + e.getErrorCode() + back; //$NON-NLS-1$ //$NON-NLS-2$
    }
    return back;
  }


  /** Wird von handleSQLException fuer ORA-00001 - Meldungen benutzt. Ermittelt die Namen der verletzten
      Constraints und baut diese in eine Fehlermeldung ein.
   * @param context Durchzureichendes Context-Objekt
   * @param e Die zu behandelnde Exception
   * @param con Connection zur Datenbank
      @return String fuer die Ausgabe in einer umgewandelten Form
   * @throws OBException
  */
  private static String handleUnique(OBContext context, SQLException e, OBConnectionInterface con) throws OBException {
    String help = e.getMessage();
    String constrName = help.substring(help.indexOf(".") + 1, help.indexOf(")")); //$NON-NLS-1$ //$NON-NLS-2$

    String back = context.getMessageGenerator().generateMessage(constrName, con);

    // try to get the column_names
    if (back.equals("")) { //$NON-NLS-1$
      Statement stmt = null;
      try {
        stmt = con.createStatement();
        // TODO : der SELECT muss noch fuer ein Schema eingegrenzt werden
        String sqlString = "SELECT column_name FROM all_cons_columns WHERE constraint_name = '"+constrName+ "'"; //$NON-NLS-1$ //$NON-NLS-2$
        ResultSet rs = stmt.executeQuery(sqlString);
        String helpBack = ""; //$NON-NLS-1$
        int count = 0;
        while (rs.next()) {
          if (count != 0) {
            helpBack = helpBack + "," + rs.getString(1); //$NON-NLS-1$
          }
          else {
            helpBack = rs.getString(1);
          }
          count++;
        }
        if (count==0) {
          back = OBConstants.UNIQUE_CONSTRAINT + "\n"; //$NON-NLS-1$
        }
        else if (count==1) {
          back = OBConstants.FIELD_NOT_UNIQUE + " " + helpBack; //$NON-NLS-1$
        }
        else {
          back = OBConstants.FIELDS_NOT_UNIQUE + " " + helpBack; //$NON-NLS-1$
        }
        back= back+"\n"+context.getMessageGenerator().generateMessage(constrName); //$NON-NLS-1$
      }
      catch (Exception ex) {
        logger.error(XynaContextFactory.getSessionData(context) + 
                     "error handleUnique", ex);//$NON-NLS-1$
      }
      finally {
        try {
          if (stmt!=null) stmt.close();
        }
        catch (Exception ex) {
          logger.error(XynaContextFactory.getSessionData(context) + 
                       "error closing statement", ex);//$NON-NLS-1$
        }
      }
    }
    return back;
  }
  

  /** Wird von handleSQLException fuer ORA-01400 - Meldungen benutzt. Ermittelt die Namen des verletzten
      Constraints und baut diesen in eine Fehlermeldung ein.
   * @param context Durchzureichendes Context-Objekt
   * @param e Die zu behandelnde Exception
   * @param con Connection zur Datenbank
      @return String fuer die Ausgabe in einer umgewandelten Form
   * @throws OBException
  */
  private static String handleNotNull(OBContext context, SQLException e, OBConnectionInterface con)  throws OBException {
    String msg = e.getMessage(); 
    String help = msg.substring(msg.indexOf(".") + 2, msg.indexOf(")")-1); //$NON-NLS-1$ //$NON-NLS-2$
    // constrName hat jetzt die Form TABLE"."COLUMN
    String constrName = help.substring(0,help.indexOf("\""))+"_" + //$NON-NLS-1$ //$NON-NLS-2$
                        help.substring(help.indexOf("\"")+3);  //$NON-NLS-1$

    String back = context.getMessageGenerator().generateMessage(constrName, con);

    // try to get the column_names
    if (back.equals("")) { //$NON-NLS-1$
      back = OBConstants.FIELD_NOT_NULL +":  "+ help.replace('"', ' '); //$NON-NLS-1$
    }
    return back;
  }

 
  /** Wird von handleSQLException fuer ORA-01400 - Meldungen benutzt. Ermittelt die Namen des verletzten
      Constraints und baut diesen in eine Fehlermeldung ein.
      @param e Die zu behandelnde Exception
      @param con Connection zur Datenbank
      @return String fuer die Ausgabe in einer umgewandelten Form
   * @throws OBException
  */
  private static String handleConnectionLost(SQLException e)  throws OBException {
    logger.debug("handleConnectionLost",e);//$NON-NLS-1$
    return OBConstants.CONNECTION_LOST;
  }

  /** 
   * Wird von handleSQLException fuer ORA-02091 - Meldungen benutzt. 
   * Ermittelt die Namen des verletzten Constraints und baut diesen in eine Fehlermeldung ein.
   * @param context Durchzureichendes Context-Objekt
   * @param e Die zu behandelnde Exception
   * @param con Connection zur Datenbank
   * @return String fuer die Ausgabe in einer umgewandelten Form
   * @throws OBException
   */
  private static String handleTransaktionsRollback(OBContext context, SQLException e, OBConnectionInterface con)  throws OBException {
    String s = e.getMessage();
    int secORA=s.indexOf("ORA-", 5); //$NON-NLS-1$
    String secOraMsg=s.substring(secORA);

    int vendorNr=Integer.parseInt(secOraMsg.replaceFirst("ORA-", "").substring(0,5)); //$NON-NLS-1$ //$NON-NLS-2$
    secOraMsg=secOraMsg.replaceFirst("ORA-......", ""); //$NON-NLS-1$ //$NON-NLS-2$
    
    SQLException secExp=new SQLException(secOraMsg, "", vendorNr); //$NON-NLS-1$
    String back = "DB-Transaktion wird zurückgesetzt.\n" + handleSQLException(context, secExp, con); //$NON-NLS-1$

    return back;
  }


  /** Wird von handleSQLException fuer ORA-02290 - Meldungen benutzt. Ermittelt die Namen des verletzten
      Constraints und baut diesen in eine Fehlermeldung ein.
   * @param context Durchzureichendes Context-Objekt
   * @param e Die zu behandelnde Exception
   * @param con Connection zur Datenbank
      @return String fuer die Ausgabe in einer umgewandelten Form
   * @throws OBException
  */
  private static String handleCheck(OBContext context, SQLException e, OBConnectionInterface con)  throws OBException {
    String s = e.getMessage();
    String constrName = s.substring(s.indexOf(".") + 1 , s.indexOf(")")); //$NON-NLS-1$ //$NON-NLS-2$
    String back = context.getMessageGenerator().generateMessage(constrName, con);

    if (back.equals("")) { //$NON-NLS-1$
      back = context.getMessageGenerator().generateMessage(constrName);
    }
    return back;
  }

  /** Wird von handleSQLException fuer ORA-02292 - Meldungen benutzt. Ermittelt die Namen des verletzten
      Constraints und baut diesen in eine Fehlermeldung ein.
   * @param context Durchzureichendes Context-Objekt
   * @param e Die zu behandelnde Exception
   * @param con Connection zur Datenbank
      @return String fuer die Ausgabe in einer umgewandelten Form
   * @throws OBException
  */
  private static String handleChildRecord(OBContext context, SQLException e, OBConnectionInterface con)  throws OBException {
    String s = e.getMessage();
    String constrName = s.substring(s.indexOf(".") + 1 , s.indexOf(")")); //$NON-NLS-1$ //$NON-NLS-2$
    String back = context.getMessageGenerator().generateMessage(constrName, con);

    if (back.equals("")) { //$NON-NLS-1$
      back = context.getMessageGenerator().generateMessage(constrName);
    }
    return back;
  }

  /** Wird von handleSQLException fuer ORA-02291 - Meldungen benutzt. Ermittelt die Namen des verletzten
      Constraints und baut diesen in eine Fehlermeldung ein.
   * @param context Durchzureichendes Context-Objekt
   * @param e Die zu behandelnde Exception
   * @param con Connection zur Datenbank
      @return String fuer die Ausgabe in einer umgewandelten Form
   * @throws OBException
  */
  private static String handleParentRecord(OBContext context, SQLException e, OBConnectionInterface con)  throws OBException {
    String s = e.getMessage();
    String constrName = s.substring(s.indexOf(".") + 1 , s.indexOf(")")); //$NON-NLS-1$ //$NON-NLS-2$
    String back = context.getMessageGenerator().generateMessage(constrName, con);

    if (back.equals("")) { //$NON-NLS-1$
      back = context.getMessageGenerator().generateMessage(constrName);
    }
    return back;
  }
  
  /**
   *
   * Die urspruengliche Methode wurde in _executeSqlStatement umbenannt.
   * @param context
   * @param example
   * @param sqlString
   * @param orderBy
   * @param ignoreFirstLines
   * @param maxRows
   * @param attribs
   * @return Liste von OBObjects
   * @throws OBException
   */
  public static <E extends OBObject> OBListObject<E> executeSqlStatement(OBContext context,
                                                                         E example, 
                                                                         String[] sqlString,
                                                                         String orderBy,
                                                                         int ignoreFirstLines,
                                                                         int maxRows,
                                                                         OBAttribute[] attribs) throws OBException {
    return _executeSqlStatement(context, example, sqlString, orderBy, ignoreFirstLines, maxRows, attribs);
  }
}

