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
import java.util.ArrayList;
import java.util.Iterator;

import com.gip.xyna.utils.db.exception.UnexpectedParameterException;
import com.gip.xyna.utils.db.types.BLOB;
import com.gip.xyna.utils.db.types.CLOBString;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.db.types.Var;

public class Parameter implements Iterable<Object> {
  
  private ArrayList<Object> parameter = new ArrayList<Object>();
  private int maxSizeToString = 100;
  
  public Parameter() {}
  
  public Parameter( Object ... params ) {
    if (params.length == 1 && params[0] instanceof ArrayList<?>) {
      for (Object p : (ArrayList<?>)params[0]) {
        addParameter( p );
      }
    } else {
      for( Object p : params ) {
        addParameter( p );
      }
    } 
  }
        
  /**
   * Eintragen des �bergebenen Objects
   * @param p
   * @throws UnexpectedParameterException, bei unbekanntem Parametertyps
   */
  public void addParameter( Object p ) {
    if( p instanceof Parameter ) { //Verschachtelung erlauben
      for( Object pi : (Parameter)p ) {
        parameter.add( pi );
      }
      return;
    }
    //Basistyp
    if( checkType( p ) ) {
      parameter.add( p );
    } else {
      String message = "Unbekannter Parametertyp";
      if( p != null ) message += " " + p.getClass().getName();
      throw new UnexpectedParameterException( message );
    }
  }  
 
  protected boolean checkType(Object p) {
    if( p == null ) return true;
    if( p instanceof String ) return true;
    if( p instanceof Integer ) return true;
    if( p instanceof Long ) return true;
    if( p instanceof CLOBString ) return true;
    if( p instanceof BLOB ) return true;
    if( p instanceof OutputParam<?> ) return true;
    if( p instanceof java.sql.Blob ) return true;
    if( p instanceof java.sql.Clob ) return true;
    if( p instanceof Float ) return true;
    if( p instanceof Double ) return true;
    if( p instanceof StringSerializable<?> ) return true;
    
    if( p instanceof Var<?> ) return checkType( ((Var<?>)p).get() );
    
    return false;
  }

  public Iterator<Object> iterator() {
    return parameter.iterator();
  }   

  public int size() {
    return parameter.size();
  }
  
  /**
   * @param index, Z�hlung beginnt bei 1
   * @return
   */
  public Object getParameter( int index ) {
    return parameter.get(index-1);
  }
  
  /**
   * @param index, Z�hlung beginnt bei 1
   * @return
   */
  public boolean isOutputParam( int index ) {
    return  parameter.get(index-1) instanceof OutputParam<?>;
  }
  
  //public OutputParam<?> getOutputParam( int index ) {
  //  return OutputParam<?> parameter.get(index);
  //}
  
  public void setMaxSizeParameterToString(int size) {
    maxSizeToString = size;
  }
  
  @Override
  public String toString() {
    StringBuilder ret = new StringBuilder();
    for( Object param : parameter ) {
      ret.append(", ");
      String s = String.valueOf(param);
      if (s.length() > maxSizeToString) {
        String className = param.getClass().getSimpleName();
        int length = Math.max(0, maxSizeToString - className.length()); 
        ret.append(className).append("[").append(s.length());
        if (length > 0) {
          ret.append(":").append(s.substring(0, length)).append("...");
        }
        ret.append("]");
      } else {
        ret.append(s);
      }
    }
    if( parameter.size() > 0 ) {
      ret.delete(0, 1); //erstes ", " abschneiden
    }
    return "Parameter(" + ret.toString() + " )";
  }


  /**
   * Eintragen aller Parameter in das PreparedStatement
   * 
   * @param stmt
   * @param params
   * @throws SQLException
   */
  public void addParameterTo(PreparedStatement stmt)
        throws SQLException {
     if( parameter.size() == 0) {
        return;
     }
     for (int i = 1; i <= parameter.size(); ++i) {
        addParameterTo(stmt, i, parameter.get(i-1));
     }
  }
  
  /**
   * Eintragen eines Parameters in das PreparedStatement
   * 
   * @param stmt
   * @param pos
   * @param param
   * @throws SQLException
   */
  protected void addParameterTo(PreparedStatement stmt, int pos, Object param)
        throws SQLException {
     if ( param == null ) {
       stmt.setNull(pos, Types.VARCHAR ); //VARCHAR sollte �berall funktionieren
       return;
     }
     if (param instanceof String) {
        stmt.setString(pos, (String) param);
        return;
     }
     if (param instanceof Integer) {
        stmt.setInt(pos, (Integer) param);
        return;
     }
     if (param instanceof Long) {
        stmt.setLong(pos, (Long) param);
        return;
     }
     if (param instanceof CLOBString) {
        CLOBString clob = (CLOBString) param;
        clob.setCLOB(stmt, pos);
        return;
     }
     if (param instanceof BLOB) {
        BLOB blob = (BLOB) param;
        blob.setBLOB(stmt, pos);
        return;
     }
     if (param instanceof Var<?> ) {
       addParameterTo(stmt, pos, ((Var<?>)param).get() );
       return;
     }
     if (param instanceof java.sql.Blob) {
       stmt.setBlob(pos, (java.sql.Blob) param);
       return;
     }
     if (param instanceof java.sql.Clob) {
       stmt.setClob(pos, (java.sql.Clob) param);
       return;
     }
     if (param instanceof Float) {
       stmt.setFloat( pos, (Float) param);
       return;
     }
     if (param instanceof Double) {
       stmt.setDouble( pos, (Double) param);
       return;
     }
     if (param instanceof StringSerializable<?>) {
       stmt.setString(pos, ((StringSerializable<?>) param).serializeToString());
       return;
     }
    
     
     // unbekanntes Objekt: Versuch, es trotzdem zu setzen
     stmt.setObject(pos, param);
  }

}
