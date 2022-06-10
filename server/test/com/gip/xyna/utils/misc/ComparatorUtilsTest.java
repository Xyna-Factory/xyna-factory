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
package com.gip.xyna.utils.misc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;

public class ComparatorUtilsTest {

  public static void main(String[] args) {
    test1();
    test2();
    
  }


  private static void test1() {
    List<String> list = Arrays.asList("A", "D", "B", null, "C");
    Collections.sort(list, ComparatorUtils.compareNullAware(String.class, false) );
    System.out.println( list );
  }


  private static void test2() {
    List<Pair<String,Integer> > list =  Arrays.asList( Pair.of("A",2), Pair.of("A",1), null, Pair.of("B",1) );
    
    Comparator<Pair<String,Integer>> pairComparator = new Comparator<Pair<String,Integer>>() {
      @Override
      public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
        /*
        int comp = ComparatorUtils.compareNullAware( o1.getFirst(), o2.getFirst(), true );
        if( comp == 0 ) {
          comp = ComparatorUtils.compareNullAware( o1.getSecond(), o2.getSecond(), true );
        }
        return comp;
        */
        /*
        ComparatorChain chain = ComparatorUtils.chain();
        if( chain.next() ) chain.compareNullAware( o1.getFirst(), o2.getFirst(), true );
        if( chain.next() ) chain.compareNullAware( o1.getSecond(), o2.getSecond(), true );
        return chain.result();
        */
        return ComparatorUtils.chain().
            compareNullAware( o1.getFirst(), o2.getFirst(), true ).
            compareNullAware( o1.getSecond(), o2.getSecond(), true ).
            result();
      }
    };
        
    
    Collections.sort(list, ComparatorUtils.reverse(ComparatorUtils.nullAware(pairComparator, true)) );
        
        
    
    System.out.println( list );
  }
  
}
