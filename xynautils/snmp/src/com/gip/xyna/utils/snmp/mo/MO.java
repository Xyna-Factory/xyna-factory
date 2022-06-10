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
package com.gip.xyna.utils.snmp.mo;

import java.util.Map;

import com.gip.xyna.utils.snmp.exception.NoNumberException;
import com.gip.xyna.utils.snmp.exception.ParamNotSetException;
import com.gip.xyna.utils.snmp.exception.ParameterReadException;


public final class MO {

  private boolean readOnly = false; //for Oids, which can't be written
  private String oid;
  private int type;
  private Restrictions.Restriction restriction;

  private final static int INT = 0;
  private final static int STRING = 1;
  private final static int OCTET  = 2;

  private MO(String oid, boolean readOnly, int type, Restrictions.Restriction restriction ) {
    this.oid = oid;
    this.readOnly = readOnly;
    this.type = type;
    this.restriction = restriction;
  }
  private MO() {/*Konstruktor darf nur intern verwendet werden*/}

  public static MOBuilder typeInt() {
    return new MOBuilder(INT);
  }
  public static MOBuilder typeString() {
    return new MOBuilder(STRING);
  }
  public static MOBuilder typeOctet() {
    return new MOBuilder(OCTET);
  }

  public static class MOBuilder {
    private MO mo = new MO();
    private boolean or = false;
    public MOBuilder(int type) {
      mo.type = type;
    }
    public MOBuilder readOnly() {
      mo.readOnly = true;
      return this;
    }

    public MOBuilder range(int min, int max) {
      return addRestriction( new Restrictions.RangeRestriction(min,max) );
    }
    public MOBuilder positive() {
      return addRestriction( new Restrictions.RangeRestriction(1,Integer.MAX_VALUE) );
    }
    public MOBuilder length(int min, int max) {
      return addRestriction( new Restrictions.RangeRestriction(min,max) );
    }    
    public MOBuilder values(String[] strings) {
      return addRestriction( new Restrictions.ValueRestriction(strings) );
    }
    public MOBuilder value(int value) {
      return addRestriction( new Restrictions.ValueRestriction(value) );
    }
    public MOBuilder mapping(Mapping mapping) {
      return addRestriction( new Restrictions.MappingRestriction(mapping) );
    }
    public MOBuilder pattern(String pattern) {
      return addRestriction( new Restrictions.PatternRestriction(pattern) );
    }
    public MO oid(String oid) {
      mo.oid=oid;
      return mo;
    }
    public MOBuilder or() {
      or = true;
      return this;
    }
    private MOBuilder addRestriction(Restrictions.Restriction restriction) {
      if( ! or || mo.restriction == null ) {
        mo.restriction = restriction;
      } else {
        mo.restriction = new Restrictions.OrRestriction( mo.restriction, restriction );
      }
      return this;
    }
    public MOBuilder unsignedInt32() {
        return addRestriction( new Restrictions.PatternRestriction("\\d+") ); //FIXME better check?
    }
  }

  public String checkValue( String value ) {
    if( type == INT ) {
      return Restrictions.checkIntValue( restriction, value );
    }
    if( type == STRING ) {
      return Restrictions.checkStringValue( restriction, value );      
    }
    return "Invalid type";
  }

  public String checkValue( int value ) {
    if( type == INT ) {
      return Restrictions.checkIntValue( restriction, value );
    }
    if( type == STRING ) {
      return Restrictions.checkStringValue( restriction, value );      
    }
    return "Invalid type";    
  }
  
  public String getOid() {
    return oid;
  }

  public boolean isTypeInt() {
    return type==INT;
  }

  public boolean isTypeString() {
    return type==STRING;
  }

    public int readInt(final Map<String,Object> data, final String key) {
        if (!data.containsKey(key)) {
            throw new ParamNotSetException(key);
        }
        String s = (String) data.get(key);

        int value;
        if( Restrictions.isMapper( restriction ) ) {
            value = Restrictions.map( restriction, s );
        } else {
            try {
                value = Integer.parseInt(s);
            } catch( NumberFormatException e ) {
                throw new NoNumberException(key,s);
            }
        }
        String check = checkValue(value);
        if( check != Restrictions.OK ) {
            throw new ParameterReadException( "Parameter "+key+": "+check );
        }
        return value;
    }

    public String readString(final Map<String,Object> data, final String key) {
        if (!data.containsKey(key)) {
            throw new ParamNotSetException(key);
        }
        String value = (String) data.get(key);
        String check = checkValue(value);
        if( check != Restrictions.OK ) {
            throw new ParameterReadException( "Parameter "+key+": "+check );
        }
        return value;
    }

  public String setString(String value) {
    String check = checkValue(value);
    if( check != Restrictions.OK ) {
      throw new IllegalArgumentException( "set: "+check );
    }
    return value;
  }

  public int setInt(int value) {
    String check = checkValue(value);
    if( check != Restrictions.OK ) {
      throw new IllegalArgumentException( "set: "+check );
    }
    return value;
  }

  public String map(int value) {
    return Restrictions.map( restriction, value );
  }

  public int map(String value) {
    return Restrictions.map( restriction, value );
  }
  
}
