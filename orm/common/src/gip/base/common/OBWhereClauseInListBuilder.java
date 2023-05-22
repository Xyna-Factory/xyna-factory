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

import java.util.ArrayList;
import java.util.Arrays;


public class OBWhereClauseInListBuilder {
  private static final int MAX_SIZE = 900;
  static int count = 0;

  /**
   * @param <V> EnumInterface
   * @param name Spaltenname
   * @param values Werte ...
   * @return whereClause-Teil
   * @throws OBException Fehler
   */
  public static <V> String build(String name, V ... values) throws OBException {
    return buildInternal(name, Arrays.asList(values), true );
  }
  /**
   * @param <V> EnumInterface
   * @param name Spaltenname
   * @param values Werte ...
   * @return whereClause-Teil
   * @throws OBException Fehler
   */
  public static <V> String build(String name, Iterable<V> values) throws OBException {
    return buildInternal(name, values, true );
  }

  /** Hier werden Strings als long angesehen, sprich die ' ' weggelassen.
   * @param <V> EnumInterface
   * @param name Spaltenname
   * @param values Werte ...
   * @return whereClause-Teil
   * @throws OBException Fehler
   */
  public static <V> String buildLongFromString(String name, V ... values) throws OBException {
    return buildInternal(name, Arrays.asList(values), false);
  }

  /**
   * @param name Spaltenname
   * @param values Werte ...
   * @return whereClause-Teil
   * @throws OBException Fehler
   */
  public static String buildInt(String name, Integer ... values) throws OBException {
    return buildInternal(name, Arrays.asList(values), false );
  }

  /**
   * @param name Spaltenname
   * @param values Werte ...
   * @return whereClause-Teil
   * @throws OBException Fehler
   */
  public static String buildInt(String name, Iterable<Integer> values) throws OBException {
    return buildInternal(name, values, false );
  }

  /**
   * @param name Spaltenname
   * @param values Werte ...
   * @return whereClause-Teil
   * @throws OBException Fehler
   */
  public static String buildLong(String name, Long ... values) throws OBException {
    return buildLong(name, Arrays.asList(values) );
  }

  /**
   * @param name Spaltenname
   * @param values Werte ...
   * @return whereClause-Teil
   * @throws OBException Fehler
   */
  public static String buildLong(String name, Iterable<Long> values) throws OBException {
    return buildInternal(name, values, false );
  }


  /**
   * @param name Spaltenname
   * @param values Werte ...
   * @param quote mit Anfuehrungszeichen?
   * @return whereClause-Teil
   * @throws OBException Fehler
   */
  private static <V> String buildInternal(String name, Iterable<V> values, boolean quote) throws OBException {
    StringBuilder sb = new StringBuilder();
    sb.append(" (");//$NON-NLS-1$
    int cnt = 0;
    String sep = "";//$NON-NLS-1$
    String sepOr = "";//$NON-NLS-1$
    for( V v : values) {
      ++cnt;
      if( cnt == 1 ) {
        sb.append(sepOr);
        sb.append(name).append(" IN (");//$NON-NLS-1$
        sepOr = ") OR ";//$NON-NLS-1$
      }
      sb.append(sep);
      fillInternal( sb, v, quote );
      sep = ", ";//$NON-NLS-1$
      if( cnt == MAX_SIZE ) {
        cnt = 0;
        sep = "";//$NON-NLS-1$
      }
    }
    sb.append(") ) ");//$NON-NLS-1$
    return sb.toString();
  }

  private static <V> void fillInternal(StringBuilder sb, V value, boolean quote) throws OBException {
    if( quote ) {
      sb.append("'").append( OBObject.transformBadCharForWhere(value.toString(),CompOperator.equal) ).append("'");//$NON-NLS-1$//$NON-NLS-2$
    } 
    else {
      try {
        Double.parseDouble(value.toString().trim()); // Check, dass es wirklich eine Zahl ist!
      }
      catch (Exception e) {
        throw new OBException(OBException.OBErrorNumber.invalidNumber1, new String[] {value.toString()});
      }
      sb.append( value );
    }
  }

  /**
   * @param name Name der Spalte
   * @param values Werte-Liste (Objekt)
   * @param columnName  Spaltenname im Objekt
   * @return whereClause-Teil
   * @throws OBException Fehler
   */
  public static String buildFromOBList(String name, OBListObject<? extends OBObject> values, String columnName) throws OBException {
    ArrayList<String> data = new  ArrayList<String>();
    boolean isString = false;
    for(int i=0; i< values.size(); i++) {
      OBAttribute a = values.elementAt(i).getOBAttributeByName(columnName);
      if (a!=null) {
        data.add(a.getValue());
        // isString setzen
        switch (a.getType()) {
          case OBConstants.INTEGER :
          case OBConstants.LONG :
          case OBConstants.BOOLEAN :
          case OBConstants.DOUBLE :
            isString |= false;
            break;
          case OBConstants.STRING :
          case OBConstants.CLOB :
          case OBConstants.LONGVARCHAR :
          case OBConstants.DATE :
          case OBConstants.TIMESTAMP_WITH_LOCAL_TIME_ZONE :
            isString = true;
            break;
        }
      }
    }
    return buildInternal(name, data, isString);
  }

  /**
   * @param <V> EnumInterface
   * @param name Spaltenname
   * @param values Werte
   * @return whereClause-Teil
   * @throws OBException Fehler 
   */
  public static <V extends OBEnumInterface> String build(String name,V ... values) throws OBException {
    ArrayList<String> data = new  ArrayList<String>();
    boolean isString = false;
    for(V v : values) {
       data.add(v.getStringValue());
       isString |= v.getUseQuotes();
    }
    return buildInternal(name, data, isString);
  }

}
