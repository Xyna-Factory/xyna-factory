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
package com.gip.xyna.utils.snmp;

import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Klasse zum Speichern einer OID, gedacht fuer Zugriffe auf einzelne Indexe der OID
 *
 */
public class OID implements Comparable {

  private final String oid;
  private String[] oidParts;
  private transient int[] oidIntParts;
  private static final Pattern splitter = Pattern.compile("\\.");
  
  /**
   * @param oid
   */
  public OID(String oid ) {
    if( oid == null ) {
      throw new IllegalArgumentException( "OID is null");
    }
    String unprocessedOid = oid.trim();
    if (!unprocessedOid.startsWith(".")) {
      this.oid = "." + unprocessedOid;
    } else {
      this.oid = unprocessedOid;
    }
  }
  
  /**
   * @param oidParts
   */
  public OID(String[] oidParts ) {
    this.oidParts = oidParts.clone();
    StringBuilder sb = new StringBuilder();
    Pattern p = Pattern.compile("[0-9]+");
    for( String op : oidParts ) {
      if( ! p.matcher(op).matches() ) {
        throw new IllegalArgumentException( "oidPart "+op+" ist not a number" );
      }
      sb.append('.').append(op);
    }
    this.oid = sb.toString();
  }
 
  private OID( OID o, int start, int end ) {
    String oid = null;
    try {
      if( start == 0 && end == o.oidParts.length ) {
        oid = o.oid;
        this.oidParts = o.oidParts;
      } else {
        this.oidParts = new String[end-start];
        StringBuffer sb = new StringBuffer();
        for( int i=start; i<end; ++i ) {
          this.oidParts[i-start] = o.oidParts[i];
          sb.append(".").append(o.oidParts[i]);
        }
        oid=sb.toString();
      }
    } catch( RuntimeException e ) {
      e.printStackTrace();
    }
    this.oid = oid;
  }
  
  /**
   * @param intParts
   */
  public OID(int[] intParts) {
    this(toStringArray(intParts));
    oidIntParts = intParts;
  }
  
  private static String[] toStringArray(int[] arr) {
    String[] parts = new String[arr.length];
    for (int i = 0; i<arr.length; i++) {
      parts[i] = String.valueOf(arr[i]);
    }
    return parts;
  }

  @Override
  public String toString() {
    return getOid();
  }

  /**
   * Startet die Oid mit dem angegebenen Prefix
   * @param prefix
   * @return
   */
  public boolean startsWith(String prefix) {
    return oid.startsWith(prefix);
  }
  
  /**
  * Startet die Oid mit dem angegebenen Prefix
   * @param oidPrefix
   * @return
   */
  public boolean startsWith(OID oidPrefix) {
    return oid.startsWith(oidPrefix.oid);
  }

  /**
   * Liefert den Index an der angegebenen Position
   * @param pos
   * @return
   */
  public String getIndex(int pos) {
    split();
    if( pos >= oidParts.length ) {
      return null;
    }
    return oidParts[pos];
  }
  
  /**
   * Liefert den Index an der angegebenen Position
   * @param pos
   * @return
   */
  public int getIntIndex(int pos) {
    if (oidIntParts == null) {
      split();
      oidIntParts = new int[oidParts.length];
      for (int i = 0; i < oidParts.length; i++) {
        oidIntParts[i] = Integer.parseInt(getIndex(i));
      }
    }
    return oidIntParts[pos];
  }

  /**
   * Liefert die Anzahl der OID-Indexe
   * @return
   */
  public int length() {
    split();
    return oidParts.length;
  }

  /**
   * Liefert oid als String
   * @return
   */
  public String getOid() {
    return oid;
  }

  /**
   * Gibt subOid ab Index start beginnend aus
   * @param start
   * @return subOid
   */
  public OID subOid(int start) {
    if( start <0 || start > length() ) {
      throw new IllegalArgumentException( "invalid interval ("+start+"-)" );
    }
    return new OID( this, start, length() );
  }

  /**
   * Gibt subOid zwischen Index start und end aus
   * @param start
   * @param end
   * @return subOid
   */
  public OID subOid(int start, int end) {
    if( start <0 || start > length() ) {
      throw new IllegalArgumentException( "invalid interval ("+start+"-)" );
    }
    if( end<= start || end > length() ) {
      throw new IllegalArgumentException( "invalid interval (-"+end+")");
    }
    return new OID( this, start, end );
  }

  private void split() {
    if( oidParts == null ) {
      if( oid.startsWith(".") ) {
        oidParts = splitter.split(oid.substring(1));
      } else {
        oidParts = splitter.split(oid);
      }
    }
  }
  
  /**
   * haengt an die OID den angegebenen Appendix an (z.B einen Index)
   * @param appendix
   * @return neue OID
   */
  public OID append( OID appendix ) {
    split();
    appendix.split();
    String[] op = new String[oidParts.length+appendix.oidParts.length];
    for( int i=0; i<oidParts.length; ++i ) {
      op[i] = oidParts[i];
    }
    for( int i=0; i<appendix.oidParts.length; ++i ) {
      op[i+oidParts.length] = appendix.oidParts[i];
    }
    return new OID(op);
  }
  
  /**
   * hängt an die OID den angegebenen Index an
   * @param index
   * @return neue OID
   */
  public OID append( int index ) {
    split();
    String[] op = new String[oidParts.length+1];
    for( int i=0; i<oidParts.length; ++i ) {
      op[i] = oidParts[i];
    }
    op[oidParts.length] = String.valueOf(index);
    return new OID(op);
  }
  
  /**
   * haengt an die OID die angegebenen Indexe an
   * @param indexes
   * @return
   */
  public OID append(int ... indexes ) {
    split();
    String[] op = new String[oidParts.length+indexes.length];
    for( int i=0; i<oidParts.length; ++i ) {
      op[i] = oidParts[i];
    }
    for( int i=0; i<indexes.length; ++i ) {
      op[oidParts.length+i] = String.valueOf(indexes[i]);
    }
    return new OID(op);
  }

  /**
   * Setzt den (existierenden!) Index an Position pos auf Wert index
   * @param pos
   * @param index
   * @return
   */
  public OID setIndex(int pos, int index) {
    split();
    String[] op = oidParts.clone();
    op[pos] = String.valueOf(index);
    return new OID(op);
  }

  /**
   * gibt hallo zurueck fuer oid = 104.97.108.108.111.
   * Falls OID nach dem String noch weitere Parts enthaelt, kann man diese ermitteln,
   * indem man oid.subOid(string.length()+1) aufruft.
   * @return
   */
  public String decodeToString() {
    split();
    StringBuilder sb = new StringBuilder();
    if (length() < 1) {
      throw new IllegalStateException("Cannot decode OID with length 0.");
    }
    int stringLength = getIntIndex(0);
    if (stringLength+1 > length()) {
      throw new IllegalStateException("Cannot decode OID: OID too short.");
    }
    for (int i = 0; i <stringLength; i++) {
      sb.append((char)getIntIndex(i+1));
    }
    return sb.toString();
  }

  /**
   * erzeugt oid, in der die oidParts ascii codes der zeichen sind.
   * hallo => 104.97.108.108.111
   * @param s
   * @return
   */
  public static OID encodeFromString(String s) {
    int[] intParts = new int[s.length()+1];
    intParts[0] = s.length();
    for (int i = 0; i<s.length(); i++) {
      intParts[i+1] = s.charAt(i);
    }
    OID oid = new OID(intParts);
    return oid;
  }

  
  /**
   * Prueft die Gleichheit einer OID mit einer anderen OID
   * this == o : true
   * this != o : false
   * @param o
   * @return
   */
  public boolean equals(Object o) {
    return ( 0 == this.compareTo(o) );
  }
  
  
  /**
   * vergleicht die OID mit einer anderen OID
   * this < o  : -1
   * this > o  : +1
   * this == o :  0
   * @param o
   * @return
   */
  public int compareTo(Object o) {
    OID cmp = null;
    if (o == null) {
      return 1;
    }
    
    if ( o instanceof OID ) {
      cmp = (OID) o;
    } else if ( o instanceof String ) {
      cmp = new OID((String) o);
    } else {
      return 1;
    }

    int l = Math.min(this.length(), cmp.length());

    // comparing by value. part by part
    for ( int i = 0; i < l; i++ ) {
      int i1 = this.getIntIndex(i);
      int i2 = cmp.getIntIndex(i);

      if ( i1 != i2 ) {
        return i1 - i2;
      }
    }

    // if both parts are identical, one might be longer than the other. the shorter is smaller.
    int l1 = this.length();
    int l2 = cmp.length();

    if (l1 != l2) {
      return l1 - l2;
    }

    // both are identical
    return 0;
  }
}
