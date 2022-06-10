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
package com.gip.xyna.xdnc.dhcpv6.db.storables.utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * ParenthesisMatcher sucht in einem String nach zusammengehörigen Klammer-Paaren. 
 * Dabei werden maskierte Klammern "\(" oder "\)" nicht berücksichtigt. 
 * Falls die Klammern nicht paarweise matchen (also "(()", "())", oder ähnliches), 
 * wirft der Konstruktor eine IllegalArgumentException.
 * Anschließend kann mittels findMatchingClose(int open) oder findMatchingOpen(int close)
 * das jeweilige Gegenstücl des Klammer-Paares.
 */
public class ParenthesisMatcher {
  HashMap<Integer,Integer> openClose = new HashMap<Integer,Integer>();
  HashMap<Integer,Integer> closeOpen = new HashMap<Integer,Integer>();
  
  /**
   * @param string
   * @throws IllegalArgumentException, falls Klammern nich matchen
   */
  public ParenthesisMatcher(String string) {
    //System.err.println( string );
    String searchString = string.replace("\\(", "__").replace("\\)", "__");
    //System.err.println( searchString );
    
    ArrayList<Integer> openParentheses = new  ArrayList<Integer>();
    for( int i=0; i< searchString.length(); ++i ) {
      char c = searchString.charAt(i);
      if( c == '(' ) {
        openParentheses.add( i );
      } else if( c == ')' ) {
        //neuer Match
        int lastOpen = openParentheses.size()-1;
        if( lastOpen < 0 ) {
          throw new IllegalArgumentException("Parentheses not matching, unexpected Close at index "+i);
        }
        Integer open = openParentheses.remove(lastOpen);
        Integer close = Integer.valueOf( i );
        openClose.put( open, close );
        closeOpen.put( close, open );
      }
    }
    if( openParentheses.size() > 0 ) {
      throw new IllegalArgumentException("Parentheses not matching, no Close for Open at index "+openParentheses.get(openParentheses.size()-1) );
    }
  }

  /**
   * @param open
   * @return Index der zugehörigen schließenden Klammer
   * @throws IllegalArgumentException, falls open nicht die Position einer öffnenden Klammer angibt
   */
  public int findMatchingClose(int open) {
    Integer close = openClose.get( open );
    if( close == null ) {
      throw new IllegalArgumentException("No Open at index "+open);
    }
    return close.intValue();
  }
  
  /**
   * @param close
   * @return Index der zugehörigen öffnenden Klammer 
   * @throws IllegalArgumentException, falls close nicht die Position einer schließenden Klammer angibt
   */
  public int findMatchingOpen(int close) {
    Integer open = closeOpen.get( close );
    if( open == null ) {
      throw new IllegalArgumentException("No Close at index "+open);
    }
    return open.intValue();
  }
 
}
