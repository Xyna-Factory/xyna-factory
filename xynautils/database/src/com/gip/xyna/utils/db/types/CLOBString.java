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

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CLOBString {
  
  String string;
  
  public CLOBString() {
    /* Default-Konstruktor */
  }
  
  public CLOBString( String string ) {
    this.string = string;
  }
  
  public void set(String string) {
    this.string = string;
  }

  public String get() {
    return string;
  }
  
  public void setCLOB( PreparedStatement stmt, int i) throws SQLException {
    if( stmt.getClass().getName().contains("oracle") ) {
      if( stmt instanceof oracle.jdbc.OraclePreparedStatement ) {
        //oracle.jdbc.OraclePreparedStatement
        //nicht oracle.jdbc.driver.OraclePreparedStatement; -> ClassCastException
        ((oracle.jdbc.OraclePreparedStatement)stmt).setStringForClob( i, string );
      } else {
        stmt.setString( i, string );
      }
    } else {
      //keine Oracle-Umgebung
      //TODO bessere Kontrolle der Umgebung
      stmt.setString( i, string );
    }
  }

  public String toString() {
    if( string == null ) {
      return "CLOBString[]";
    }
    return "CLOBString["+string.length()+"]";
  }
  
}
