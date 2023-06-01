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
package com.gip.xyna.utils.misc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Vereinfacht den Bau von Comparatoren.
 * Ermöglicht auch Vergleich null mit Objekt mit definierter Sortierung.
 *
 */
public class ComparatorUtils {
  
  private ComparatorUtils() {}
  
  public static <T> Comparator<? super T> nullAware(final Comparator<T> comparator, final boolean nullFirst) {
    return new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        if( o1 == null ) {
          return compareNull(o2, nullFirst);
        }
        if( o2 == null ) {
          return nullFirst? 1 : -1;
        }
        return comparator.compare(o1,o2);
      }
    };
  }
  
  private static int compareNull(Object o2, boolean nullFirst) {
    if( o2 == null ) {
      return 0;
    }
    return (nullFirst? -1 : 1 );
  }
  
  public static <T> Comparator<? super T> reverse(final Comparator<T> comparator) {
    return new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        return - comparator.compare(o1,o2);
      }};
  }

  public static <T extends Comparable<T>> Comparator<? super T> compare(Class<T> type) {
    return new Comparator<T>(){
      @Override
      public int compare(T o1, T o2) {
        return o1.compareTo(o2); 
      }};
  }
  
  public static <T extends Comparable<T>> Comparator<? super T> compareNullAware(Class<T> type, final boolean nullFirst) {
    return new Comparator<T>(){
      @Override
      public int compare(T o1, T o2) {
        return compareNullAware(o1, o2, nullFirst);
      }};
  }

  public static <T extends Comparable<T>> int compareNullAware(T o1, T o2, boolean nullFirst) {
    if( o1 == null ) {
      return compareNull(o2, nullFirst);
    }
    if( o2 == null ) {
      return nullFirst? 1 : -1;
    }
    return o1.compareTo(o2);
  }

  public static int compareIgnoreCaseNullAware(String o1, String o2, boolean nullFirst) {
    if( o1 == null ) {
      return compareNull(o2, nullFirst);
    }
    if( o2 == null ) {
      return nullFirst? 1 : -1;
    }
    return o1.compareToIgnoreCase(o2);
  }

  /**
   *
   *Manuell
   <pre>
        int comp = ComparatorUtils.compareNullAware( o1.getFirst(), o2.getFirst(), true );
        if( comp == 0 ) {
          comp = ComparatorUtils.compareNullAware( o1.getSecond(), o2.getSecond(), true );
        }
        return comp;
   </pre>
   oder einfach
   <pre>
        return ComparatorUtils.chain().
            compareNullAware( o1.getFirst(), o2.getFirst(), true ).
            compareNullAware( o1.getSecond(), o2.getSecond(), true ).
            result();
   </pre>
   oder optimiert bei teuren Gettern
   <pre>
        ComparatorChain chain = ComparatorUtils.chain();
        if( chain.next() ) chain.compareNullAware( o1.getFirst(), o2.getFirst(), true );
        if( chain.next() ) chain.compareNullAware( o1.getSecond(), o2.getSecond(), true );
        return chain.result();
   </pre>
   
   *
   */
  public static class ComparatorChain {
    int comp = 0;
    
    public boolean next() {
      return comp == 0;
    }
    
    public <T extends Comparable<T>> ComparatorChain compareNullAware(T o1, T o2, boolean nullFirst) {
      if( comp == 0 ) {
        comp = ComparatorUtils.compareNullAware(o1, o2, nullFirst);
      }
      return this;
    }

    public int result() {
      return comp;
    }
    
  }
  
  public static ComparatorChain chain() {
    return new ComparatorChain();
  }
  
  /**
   * Comparator, der zwei Objects miteinander vergleicht, wenn diese Comparable sind.
   * Ansonsten wird die (notwendige) Implementierung von NotComparableHandler ausgeführt.
   *
   */
  public static class ObjectComparator implements Comparator<Object> {

    public static interface NotComparableHandler {
      
      public int compareNotComparable(Object o1, Object o2);
    }

    private NotComparableHandler notComparableHandler;
    
    public ObjectComparator(NotComparableHandler notComparableHandler) {
      this.notComparableHandler = notComparableHandler;
    }
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public int compare(Object o1, Object o2) {
      if( o1 instanceof Comparable ) {
        return ((Comparable)o1).compareTo(o2);
      } else {
        return notComparableHandler.compareNotComparable(o1,o2);
      }
    }
    
  }
  
  /**
   * ComparatorList ist ein Comparator, der nacheinander mehrere Comparatoren auf die
   * zu vergleichenden T-Objekte anwendet, bis diese einen Wert != 0 zurückgeben.
   * Damit kann recht einfach eine Comparator-Kette realisiert werden.
   *
   */
  public static class ComparatorList<T> implements Comparator<T> {
    private List<Comparator<T>> subComparators;
    
    public ComparatorList(int size) {
      subComparators = new ArrayList<>(size);
    }

    public void add(Comparator<T> comparator) {
      this.subComparators.add(comparator);
    }

    @Override
    public int compare(T o1, T o2) {
      int comp = 0;
      for( Comparator<T> sub : subComparators ) {
        comp = sub.compare(o1, o2);
        if( comp != 0 ) {
          return comp;
        }
      }
      return 0;
    }
  
  }

}
