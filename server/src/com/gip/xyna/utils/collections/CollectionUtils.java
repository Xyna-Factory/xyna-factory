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
package com.gip.xyna.utils.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Sammlung von einfachen Funktionen, die auf Collections arbeiten.
 * <br>
 * Transformation:<pre>
 * List&lt;String&gt; strings = Arrays.asList(new String[]{"Alpha","Beta","Gamma"});
 * List&lt;Integer&gt; length = new ArrayList&lt;&gt;();
 * for( String str : strings ) {
 *   length.add( str.length() );
 * }
 * </pre>
 * wird ersetzt durch<pre> 
 * List&lt;String&gt; strings = Arrays.asList(new String[]{"Alpha","Beta","Gamma"});
 * Transformation&lt;String, Integer&gt; getStringLength = new Transformation&lt;&gt;() {
 *   public Integer transform(String from) {
 *     return from.length();
 *   }
 * };
 * List&lt;Integer&gt; length = CollectionUtils.transform( strings, getStringLength );
 * </pre>
 * dies hilft sehr, wenn die Transformation wiederverwendbar ist
 */
public class CollectionUtils {
    
  /**
   * Transformation: die Methode transform berechnet aus dem übergebenen From-Object das To-Object.
   *
   * @param <F> From-Typ
   * @param <T> To-Typ
   */
  public static interface Transformation<F,T> {
    public T transform(F from);
  }
  
  /**
   * Filter: Rückgabe gibt an, ob Value in der Ausgabe erscheint (true)
   *
   * @param <T> type
   */
  public static interface Filter<T> {
    public boolean accept(T value);
  }
 
  
  /**
   * Transformiert alle Elemente aus from in eine neue Liste (ArrayList)
   * @param from
   * @param transformation
   * @return
   */
  public static <F,T> List<T> transform(Collection<F> from, Transformation<F,T> transformation) {
    List<T> to = new ArrayList<T>();
    transform( from, transformation, to );
    return to;
  }
  
  /**
   * Transformiert alle Elemente aus from und trägt diese in to ein
   * @param from
   * @param transformation
   * @param to
   */
  public static <F,T> void transform(Collection<F> from, Transformation<F,T> transformation, Collection<T> to) {
    for( F f :from ) {
      to.add(transformation.transform(f));
    }
  }

  /**
   * Transformiert alle Elemente aus from in eine neue Liste (ArrayList), ohne jedoch null-Objecte einzutragen 
   * @param from
   * @param transformation
   * @return
   */
  public static <F,T> List<T> transformAndSkipNull(Collection<F> from, Transformation<F,T> transformation) {
    List<T> to = new ArrayList<T>();
    transformAndSkipNull( from, transformation, to );
    return to;
  }
 
  /**
   * Transformiert alle Elemente aus from und trägt diese in to ein
   * @param from
   * @param transformation
   * @param to
   */
  public static <F,T> void transformAndSkipNull(Collection<F> from, Transformation<F,T> transformation, Collection<T> to) {
    for( F f :from ) {
      T t = transformation.transform(f);
      if( t != null ) {
        to.add(t);
      }
    }
  }
  
  /**
   * Filtert alle Elemente aus from und trägt diese in to ein
   * @param from
   * @param filter
   * @return
   */
  public static <T> List<T> filter( Collection<T> from, Filter<T> filter ) {
    List<T> to = new ArrayList<T>();
    for( T f :from ) {
      if( filter.accept(f) ) {
        to.add(f);
      }
    }
    return to;
  }
  
  
  /**
   * Gruppiert alle Elemente aus from in die Map to mit dem Gruppierungskriterium grouping
   * @param from
   * @param grouping
   * @param to
   */
  public static <F,T> void group( Collection<? extends F> from, Transformation<F,T> grouping, Map<T,ArrayList<F>> to) {
    for( F f :from ) {
      T t = grouping.transform(f);
      ArrayList<F> list = to.get(t);
      if( list == null ) {
        list = new ArrayList<F>();
        to.put(t,list);
      }
      list.add(f);
    }
  }
  
  /**
   * Gruppiert alle Elemente aus from mit dem Gruppierungskriterium grouping und gibt diese als neue HashMap aus
   * @param from
   * @param grouping
   */
  public static <F,T> Map<T,ArrayList<F>> group( Collection<? extends F> from, Transformation<F,T> grouping) {
    Map<T,ArrayList<F>> grouped = new HashMap<T,ArrayList<F>>();
    group(from, grouping, grouped);
    return grouped;
  }

  
  /**
   * Transformiert die Values der übergebenen Map und liefert eine neue Map zurück.
   * @param from
   * @param transformation
   * @return
   */
  public static <K,F,T> Map<K,T> transformValues(Map<K, F> from, Transformation<F,T> transformation) {
    Map<K,T> to = new HashMap<K,T>();
    transformValues( from, transformation, to );
    return to;
  }

  /**
   * Transformiert die Values der übergebenen Map in die zweite übergebene Map.
   * @param from
   * @param transformation
   * @param to
   */
  public static <K,F,T> void transformValues(Map<K, F> from, Transformation<F,T> transformation, Map<K,T> to) {
    if( from == null ) {
      return; //nichts zu tun
    }
    for( Map.Entry<K,F> entry : from.entrySet() ) {
      to.put( entry.getKey(), transformation.transform(entry.getValue() ) );
    }
  }
  
  /**
   * Transformiert die Values der übergebenen Map und liefert eine neue Map zurück; die neue Map enthält 
   * nur die Einträge, für die die Transformation als Ergebnis keine null liefert.
   * @param from
   * @param transformation
   * @return
   */
  public static <K,F,T> Map<K,T> transformValuesAndSkipNull(Map<K, F> from, Transformation<F,T> transformation) {
    Map<K,T> to = new HashMap<K,T>();
    transformValuesAndSkipNull( from, transformation, to );
    return to;
  }
  
  /**
   * Transformiert die Values der übergebenen Map in die zweite übergebene Map; die zweite Map erhält 
   * nur die Einträge, für die die Transformation als Ergebnis keine null liefert.
   * @param from
   * @param transformation
   * @param to
   */
  public static <K,F,T> void transformValuesAndSkipNull(Map<K, F> from, Transformation<F,T> transformation, Map<K,T> to) {
    if( from == null ) {
      return; //nichts zu tun
    }
    for( Map.Entry<K,F> entry : from.entrySet() ) {
      T toVal = transformation.transform(entry.getValue() );
      if( toVal != null ) {
        to.put( entry.getKey(), toVal );
      }
    }
  }

  /**
   * Gibt die Mengen-Differenz Menge(all) ohne Menge(without) zurück.
   * @param all
   * @param without
   * @return
   */
  public static <T> Set<T> differenceSet(Collection<T> all, Collection<T> without) {
    Set<T> difference = new HashSet<T>(all);
    difference.removeAll(without);
    return difference;
  }

  /**
   * Liefert die Werte aus der Map für die angegeben Keys.
   * @param map
   * @param keys
   * @return
   */
  public static <K,V> List<V> valuesForKeys(Map<K,V> map, Collection<K> keys) {
    List<V> values = new ArrayList<V>();
    for( K key : keys ) {
      values.add( map.get(key) );
    }      
    return values;
  }


  public static <L, R, K, J> List<J> join(Collection<L> left, Collection<R> right, final Join<L, R, K, J> join, JoinType jointype) {
    Set<K> allKeys = new HashSet<K>();
    Map<K,ArrayList<L> > lefts = group(left, new Transformation<L,K>() {
      public K transform(L from) {
        return join.leftKey(from);
      }
    });
    allKeys.addAll(lefts.keySet() );
    Map<K,ArrayList<R> > rights = group(right, new Transformation<R,K>() {
      public K transform(R from) {
        return join.rightKey(from);
      }
    });
    allKeys.addAll(rights.keySet() );
    
    ArrayList<J> joins = new ArrayList<J>();
    for( K key : allKeys ) {
      ArrayList<L> ls = lefts.get(key);
      ArrayList<R> rs = rights.get(key);
      if( jointype.canJoin(ls != null, rs != null) ) {
        joins.add( join.join(key, ls, rs) );
      }
    }
    return joins;
  }
  
  public enum JoinType {
    Inner() {
      public boolean canJoin(boolean leftExists, boolean rightExists ) {
        return leftExists && rightExists;
      }
    },
    LeftOuter() {
      public boolean canJoin(boolean leftExists, boolean rightExists ) {
        return leftExists;
      }
    },
    RightOuter() {
      public boolean canJoin(boolean leftExists, boolean rightExists ) {
        return rightExists;
      }
    },
    FullOuter() {
      public boolean canJoin(boolean leftExists, boolean rightExists ) {
        return true;
      }
    },
    Anti() {
      public boolean canJoin(boolean leftExists, boolean rightExists ) {
        return leftExists != rightExists;
      }
    };
    
    public abstract boolean canJoin(boolean leftExists, boolean rightExists );
  }
  
  
  public interface Join<L, R, K, J> {
    
    public K leftKey(L left);
    
    public K rightKey(R right);
    
    public J join(K key, List<L> lefts, List<R> rights);
  }

}
