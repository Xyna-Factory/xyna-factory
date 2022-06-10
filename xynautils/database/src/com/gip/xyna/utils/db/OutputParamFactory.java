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
package com.gip.xyna.utils.db;

import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;

import oracle.sql.CLOB;

public class OutputParamFactory {
  
  /**
   * Anlegen des OutputParam<Integer>
   * @return OutputParam<Integer>
   */
  public static OutputParam<Integer> createInteger() {
    return new OutputParamInteger();
  }
  
  /**
   * Anlegen des OutputParam<Long>
   * @return OutputParam<Long>
   */
  public static OutputParam<Long> createLong() {
    return new OutputParamLong();
  }
  
  /**
   * Anlegen des OutputParam<String>
   * @return OutputParam<String>
   */
  public static OutputParam<String> createString() {
    return new OutputParamString();
  }
  
  /**
   * Anlegen des OutputParam<Clob>
   * @return OutputParam<String>
   */
  public static OutputParam<String> createClob() {
    return new OutputParamClob();
  }
  
  

  public static class OutputParamInteger implements OutputParam<Integer> {
    private Integer object;
    public Integer get() {
      return object;
    }
    public int getSQLType() {
      return Types.INTEGER;
    }
    public void set(Object object) {
      this.object = (Integer)object;
    }
    public String toString() {
      return "OutputParam<Integer>";
    }
  }
  
  public static class OutputParamLong implements OutputParam<Long> {
    private Long object;
    public Long get() {
      return object;
    }
    public int getSQLType() {
      return Types.BIGINT;
    }
    public void set(Object object) {
      this.object = (Long)object;
    }
    public String toString() {
      return "OutputParam<Long>";
    }
  }
  
  public static class OutputParamString implements OutputParam<String> {
    private String object;
    public String get() {
      return object;
    }
    public int getSQLType() {
      return Types.VARCHAR;
    }
    public void set(Object object) {
      this.object = (String)object;
    }
    public String toString() {
      return "OutputParam<String>";
    }
  }

  public static class OutputParamClob implements OutputParam<String> {
    private String object;
    public String get() {
      return object;
    }
    public int getSQLType() {
      return Types.CLOB;
    }
    public void set(Object object) throws SQLException {
      if( object instanceof Clob ) {
        Clob clob = null;
        try {
          clob = ((Clob)object);
          this.object = clob.getSubString(1,(int)clob.length() );
        } finally {
          if( clob instanceof CLOB ) {
            CLOB cl = (CLOB)clob;
            try {
              if( cl.isTemporary() ) {
                cl.freeTemporary();
              }
            } catch( SQLException e) {
              //Logger.getLogger(OutputParamClob.class).warn( "Could not free temp CLOB ", e );
            }
          }
        }
      } else {
        if( object == null ) {
          throw new SQLException( "Could not read CLOB: got null" );
        } else {
          throw new SQLException( "Could not read CLOB: unexpected Type: "+ object.getClass().getSimpleName() );
        }
      }
    }
    public String toString() {
      return "OutputParam<Clob>";
    }
  }

}
