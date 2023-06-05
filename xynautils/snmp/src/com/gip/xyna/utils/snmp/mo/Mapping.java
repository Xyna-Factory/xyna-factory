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
package com.gip.xyna.utils.snmp.mo;

import java.util.ArrayList;


public class Mapping {
  private ArrayList<Entry> map;
  private static class Entry {
    String string;
    int i;
    private Entry(String string, int i) {
      this.string = string;
      this.i = i;
    }
    private boolean is(String value) {
      return string.equals(value);
    }
    private boolean is(int value) {
      return i==value;
    }
    private int asInt() {
      return i;
    }
    private String asString() {
      return string;
    }
  }
  
  public static MappingBuilder add(String string, int i) {
    return new MappingBuilder().add(string,i);
  }
  
  public String mapToString( int value ) {
    int index = firstIndexWith(value);
    if( index == -1 ) {
      throw new IllegalArgumentException( "Illegal Mapping-Value "+value+" for "+this.toString());
    }
    return map.get(index).asString();
  }
  public int mapToInt( String value ) {
    int index = firstIndexWith(value);
    if( index == -1 ) {
      throw new IllegalArgumentException( "Illegal Mapping-Value "+value+" for "+this.toString());
    }
    return map.get(index).asInt();
  }
 
  public boolean contains(int value) {
    return firstIndexWith(value) != -1;
  }
  public boolean contains(String value) {
    return firstIndexWith(value) != -1;
  }
  
  private int firstIndexWith(int value) {
    for( int i=0,n=map.size(); i<n; ++i ) {
      if( map.get(i).is(value) ) {
        return i;
      }
    }
    return -1;
  }
  private int firstIndexWith( String value) {
    for( int i=0,n=map.size(); i<n; ++i ) {
      if( map.get(i).is(value) ) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public String toString() {
    if( true ) {
      StringBuffer sb = new StringBuffer();
      sb.append("Mapping[").append(map.size()).append("]");
      sb.append("(");
      for( int i=0,n=map.size(); i<n; ++i ) {
        Entry e = map.get(i);
        sb.append(e.asString()).append("(").append(e.asInt()).append("),");
      }
      sb.append(")");
      return sb.toString();
    } else {
      return "Mapping["+map.size()+"]";
    }
  }
  
  
  public static class MappingBuilder {
    
    Mapping mapping = null;
    private MappingBuilder() {
      mapping = new Mapping();
      mapping.map = new ArrayList<Entry>();
    }
    
    public MappingBuilder add(String string, int i) {
      mapping.map.add( new Entry(string,i) );
      return this;
    }

    public Mapping build() {
      return mapping;
    }
    
  }


}
