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

import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetReaderFactory {

  /**
   * ResultSetReader, der length ints in ein Array schreibt 
   * @param length Anzahl der zu lesenden ints
   * @return
   */
  public static ResultSetReader<int[]> getIntArrayReader( final int length ) {
    return new ResultSetReader<int[]>() {
      public int[] read(ResultSet rs) throws SQLException {
        int[] ret = new int[length];
        for( int i=0; i< length; ++i ) {
          ret[i] = rs.getInt(i+1);
        }
        return ret;
      }};
  }

  /**
   * ResultSetReader, der length ints in ein Array schreibt, falls die DB ein null liefert,
   * wird stattdessen nullValue verwendet
   * @param length
   * @param nullValue
   * @return
   */
  public static ResultSetReader<int[]> getIntArrayReaderCheckNull( final int length, final int nullValue ) {
    return new ResultSetReader<int[]>() {
      public int[] read(ResultSet rs) throws SQLException {
        int[] ret = new int[length];
        for( int i=0; i< length; ++i ) {
          ret[i] = rs.getInt(i+1);
          if( rs.wasNull() ) {
            ret[i] = nullValue;
          }
        }
        return ret;
      }};
  }
  
  /**
   * ResultSetReader, der einen String liest
   * @return
   */
  public static ResultSetReader<String> getStringReader() {
    return new ResultSetReader<String>() {
      public String read(ResultSet rs) throws SQLException {
        return rs.getString(1);
      }};
  }
  
  /**
   * ResultSetReader, der length Strings in ein Array schreibt 
   * @param length Anzahl der zu lesenden Strings
   * @return
   */
  public static ResultSetReader<String[]> getStringArrayReader( final int length ) {
    return new ResultSetReader<String[]>() {
      public String[] read(ResultSet rs) throws SQLException {
        String[] ret = new String[length];
        for( int i=0; i< length; ++i ) {
          ret[i] = rs.getString(i+1);
        }
        return ret;
      }};
  }
  
  /**
   * ResultSetReader, der n Strings in ein Array schreibt 
   * @return
   */
  public static ResultSetReader<String[]> getStringArrayReader() {
    return new ResultSetReader<String[]>() {
      private int length = -1;
      public String[] read(ResultSet rs) throws SQLException {
        if( length < 0 ) {
          length = rs.getMetaData().getColumnCount();
        }
        String[] ret = new String[length];
        for( int i=0; i< length; ++i ) {
          ret[i] = rs.getString(i+1);
        }
        return ret;
      }};
  }
 
  /**
   * ResultSetReader, der einen Long liest
   * @return Long oder null
   */
  public static ResultSetReader<Long> getLongReader() {
    return new ResultSetReader<Long>() {
      public Long read(ResultSet rs) throws SQLException {
        long v = rs.getLong(1);
        if( rs.wasNull() ) {
          return null;
        }
      return Long.valueOf( v );
      }};
  }
  
  /**
   * ResultSetReader, der length Longs in ein Array schreibt 
   * @param length Anzahl der zu lesenden Longs
   * @return
   */
  public static ResultSetReader<Long[]> getLongArrayReader( final int length ) {
    return new ResultSetReader<Long[]>() {
      public Long[] read(ResultSet rs) throws SQLException {
        Long[] ret = new Long[length];
        for( int i=0; i< length; ++i ) {
          ret[i] = Long.valueOf( rs.getLong(i+1) );
          if( rs.wasNull() ) {
            ret[i] = null;
          }
        }
        return ret;
      }};
  }

  
  /**
   * ResultSetReader, der einen Enum ausliest
   * Achtung: noch nicht genügend getestet
   * @return
   */
  public static <E extends Enum<E>> ResultSetReader<E> getEnumReader( final Class<E> clazz ) {
    return new ResultSetReader<E>() {
      public E read(ResultSet rs) throws SQLException {
        try {
          return Enum.valueOf( clazz, rs.getString(1) );
        } catch( IllegalArgumentException iae ) {
          throw new SQLException(iae.getMessage());
        }
      }
    };
  }
  
  
  /**
   * ResultSetReader der ein Array beliebiger Strings, Longs, Integers und Enums ausliest
   * Achtung: noch nicht genügend getestet
   * @param types
   * @return
   */
  public static ResultSetReader<Object[]> getObjectReader( final Class<?> ... types ) {
    return new ResultSetReader<Object[]>() {
      public Object[] read(ResultSet rs) throws SQLException {
        Object[] ret = new Object[types.length];
        for( int pos=0; pos<types.length; ++pos ) {
          ret[pos] = read(rs, pos+1, types[pos] );
        }
        return ret;
      }
      @SuppressWarnings({ "rawtypes", "unchecked" })
      private Object read(ResultSet rs, int pos, Class<?> type) throws SQLException {
        if( type == String.class ) {
          return rs.getString(pos);
        }
        if( type == Integer.class ) {
          int i = rs.getInt(pos);
          if( rs.wasNull() ) {
            return null;
          } else {
            return Integer.valueOf(i);
          }
        }
        if( type == Long.class ) {
          long l = rs.getLong(pos);
          if( rs.wasNull() ) {
            return null;
          } else {
            return Long.valueOf(l);
          }
        }
        if( type.isEnum() ) {
          try {
            return Enum.valueOf( (Class<Enum>)type, rs.getString(pos) );
          } catch( IllegalArgumentException iae ) {
            throw new SQLException(iae.getMessage());
          }
        }
        return null;
      }
    };
    
  }

}
