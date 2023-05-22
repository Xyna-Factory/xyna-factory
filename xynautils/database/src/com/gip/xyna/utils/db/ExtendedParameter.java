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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.util.Date;

import com.gip.xyna.utils.db.types.BooleanWrapper;
import com.gip.xyna.utils.db.types.Timestamp;
import com.gip.xyna.utils.db.types.XMLType;

/**
 * ExtendedParameter erweitert Parameter, so dass einige Datentypen verwendet werden k�nnen, die
 * nicht direkt verwendet werden k�nnen, sondern einen �nderung des SQL-String n�tig machen.
 * <p>
 * Aufruf-Beispiel:
 * <code><pre>
  OutputParam<String> ops = OutputParamFactory.createString();
  ExtendedParameter ep = new ExtendedParameter(ops,"Hallo",new Date(),false );
  String sql = "{? = call boolDateTest(?,?,?)}";
  sql = ep.prepareSQLString(sql);
  //sql ist nun "{? = call boolDateTest(?,TO_DATE(?,'YYYY.MM.DD HH24:MI:SS'),?=1)}"
  sqlUtils.executeCall(sql, ep );
  System.out.println( ops.get() );
 </pre></code>
 mit der PLSQL-Funktion boolDateTest
 <code><pre>
 create or replace FUNCTION boolDateTest( string VARCHAR2, datum DATE, was BOOLEAN )
RETURN VARCHAR2
IS
BEGIN
  IF was THEN
    RETURN string;
  ELSE
    RETURN datum;
  END IF;
END;
 </pre></code>
 * <p>
 * Momentan werden die Typen
 * <ul>
 *   <li> Boolean
 *   <li> Date
 *   <li> Timestamp
 * <ul>
 * unterst�tzt.
 * <p>
 *
 * Timestamps werden in die DB als UTC eingetragen.
 *
 * Achtung bei der �bergabe von NULL: der Typ der NULL kann nicht erkannt werden, daher k�nnen die
 * n�tigen Umwandlungen in prepareSQLString() dann nicht durchgef�hrt werden. Dies �u�ert sich evtl.
 * in einem Fehler "PLS-00306: Falsche Anzahl oder Typen von Argumenten in Aufruf von...".
 * Dies ist insbesondere beim Typ Boolean der Fall: Hier muss der BooleanWrapper verwendet werden,
 * wenn der einzutragende Boolean NULL sein kann. Beispiel:
 * Boolean b = null; ExtendedParameter ep = new ExtendedParameter( new BooleanWrapper(b) );
 */
public class ExtendedParameter extends Parameter {

  private DateFormat dateFormatter;
  private DateFormat timestampFormatter;

  public ExtendedParameter() {
    super();
  }
  
  public ExtendedParameter( Object ... params ) {
    super( params );
  }
 
  protected boolean checkType(Object p) {
    if( super.checkType(p) ) {
      return true;
    }
    if( p instanceof Boolean ) return true;
    if( p instanceof BooleanWrapper ) return true;
    if( p instanceof Date ) return true;
    if( p instanceof Timestamp ) return true;
    if( p instanceof XMLType ) return true;
    return false;
  }

  public String prepareSQLString( String sql ) {
    String[] sqlSplit = sql.split("\\?",-1);
    StringBuffer sb = new StringBuffer();
    sb.append( sqlSplit[0] );
    for( int i=1; i<sqlSplit.length; ++i ) {
      sb.append( getPlaceHolder(i) ).append( sqlSplit[i] );
    }
    return sb.toString();
  }
  
  public String getPlaceHolder(int index) {
    Object param = getParameter(index);
    if( param instanceof Boolean ) {
      return "?=1";
    }
    if( param instanceof BooleanWrapper ) {
      return "?=1";
    }
    if( param instanceof Date ) {
      return TimeUtils.getToDate("?");
    }
    if( param instanceof Timestamp ) {
      return TimeUtils.getToTimestamp("?");
    }
/*    if( param instanceof XMLType ) {
      return "XMLType(?)";
    }*/
    return "?";
  }

  protected void addParameterTo( PreparedStatement stmt, int pos, Object param) throws SQLException {
    if( param == null ) {
      stmt.setNull(pos, Types.VARCHAR );
      return;
    }
    if( param instanceof Boolean ) {
      stmt.setInt(pos, ((Boolean)param)?1:0 );
      return;
    }
    if( param instanceof BooleanWrapper ) {
      BooleanWrapper bw = (BooleanWrapper) param;
      if( bw.isNull() ) {
        stmt.setNull(pos, Types.INTEGER );
      } else {
        stmt.setInt(pos, bw.booleanValue()?1:0 );
      }
      return;
    }
    if( param instanceof Date ) {
      String d = null;
      if( param != null ) {
        d = getDateFormatter().format( (Date)param );
      }
      stmt.setString(pos, d);
      return;
    }
    if( param instanceof Timestamp ) {
      String ts = null;
      if( ((Timestamp)param).getDate() != null ) {
        ts = getTimestampFormatter().format( ((Timestamp)param).getDate() );
      }
      stmt.setString(pos, ts);
      return;
    }
    if (param instanceof XMLType) {
       oracle.xdb.XMLType xt = oracle.xdb.XMLType.createXML(stmt.getConnection(), ((XMLType)param).getXMLString());
       stmt.setObject(pos, xt); 
   /*    CLOBString clob = ((XMLType)param).toCLOBString();
       clob.setCLOB(stmt, pos);*/
       return;
    }    
    super.addParameterTo(stmt,pos,param);
  }

  private DateFormat getDateFormatter() {
    if( dateFormatter == null ) {
      dateFormatter = TimeUtils.getDateFormatter();
    }
    return dateFormatter;
  }
  
  private DateFormat getTimestampFormatter() {
    if( timestampFormatter == null ) {
      timestampFormatter = TimeUtils.getTimestampFormatter();
    }
    return timestampFormatter;
  }
  
}
