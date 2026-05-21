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

import java.util.Arrays;

/**
 * NextOID ist ein einfacher Zaehler, mit dem Tabellen ueber getNBext durchlaufen werden koennen.
 * 
 * NextOID bestimmt die jeweils naechste OID, zu der Eintraege vorhanden sind, in dem die einzelnen
 * Indexe in der OID inkrementiert werden. Dabei werden die Ueberlaeufe korrekt behandelt. Dazu muessen
 * im Konstruktor die jeweiligen Intervalle angegeben werden.
 * 
 * Beispiel NextOID(2,3): "" -> .1.1 -> .1.2 -> .1.3 -> .2.1 -> .2.2 -> .2.3 -> NoNextOIDException
 * 
 *
 */
public class NextOID {
  
  /**
   * NoNextOIDException wird geworfen, wenn die OID nicht weiter inkrementiert werden kann.
   */
  public static class NoNextOIDException extends Exception {
    private static final long serialVersionUID = 1L;

  }

  int[] sizes;
  
  /**
   * @param sizes
   */
  public NextOID( int ... sizes ) {
    this.sizes = sizes.clone();
  }

  /**
   * liefert die naechste OID nach der uebergebenen
   * @param oid
   * @return
   * @throws NoNextOIDException
   */
  public OID getNext(OID oid) throws NoNextOIDException {
    String[] current = new String[sizes.length];
    for( int i=0; i<sizes.length; ++i ) {
      current[i] = oid.getIndex(i);
    }
    
    boolean start = false;
    for( int i=0; i<sizes.length; ++i ) {
      if( current[i] == null ) {
        current[i] = "1";
        start = true;        
      }
    }
    
    if( start ) {
      //OID war noch nicht komplett, ist nun ergänzt und daher fertig
      return new OID(current);
    }
    
    //OID war bereits komplett, daher nun inkrementieren
    for( int len = sizes.length-1; len>=0; --len ) {
      int c = Integer.parseInt( current[len] );
      ++c; //inkrementieren
      if( c <= sizes[len] ) {
        current[len] = String.valueOf(c);
        break; //fertig
      } else {
        current[len] = "1";
        //naechsten Index erhoehen
        if( len == 0 ) {
          //es gibt keinen hoeheren Index mehr!
          throw new NoNextOIDException();
        }
      }
    }
    
    return new OID(current);
  }

  /**
   * Liefert die Laenge des angegebenen Index
   * @param i
   * @return
   */
  public int size(int i) {
    return sizes[i];
  }
  
  @Override
  public String toString() {
    return "NextOID"+Arrays.toString(sizes);
  }
  
}
