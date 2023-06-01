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
package com.gip.xyna.utils.collections;

import java.io.Serializable;
import java.util.Comparator;



/**
 * Einfacher Speicherung von zwei zusammengehörigen Objekten.
 * <br><br>
 * Beispiel:
 * <pre>
 * HashMap&lt;String,Pair&lt;String,Long&gt;&gt; map = new HashMap&lt;String,Pair&lt;String,Long&gt;&gt;();
 * map.put("a", Pair.of("A", 1L) );
 * map.put("b", new Pair&lt;String,Long&gt;("B", 2L) );
 * for( Map.Entry&lt;String,Pair&lt;String,Long&gt;&gt; e : map.entrySet() ) {
 *   String lower = e.getKey();
 *   String upper = e.getValue().getFirst();
 *   long ordinal = e.getValue().getSecond();
 * }
 * </pre>
 */
public class Pair<F,S> implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  protected F first;
  protected S second;
  
  public Pair(F first, S second) {
    this.first = first;
    this.second = second;
  }
  
  public static <F,S> Pair<F,S> of(F first, S second) {
    return new Pair<F,S>(first,second);
  }

  public F getFirst() {
    return first;
  }

  
  public void setFirst(F first) {
    this.first = first;
  }

  
  public S getSecond() {
    return second;
  }

  
  public void setSecond(S second) {
    this.second = second;
  }

  /**
   * Comparator, der nach Second sortiert.<br>
   * Aufruf kann leider den Typ &lt;S&gt; nicht ermitteln, daher ist die Angabe nötig.<br>
   * Beispiel: <code>Collections.sort( list, Pair.&lt;Integer&gt;comparatorSecond() )</code>
   * @return
   */
  public static <C extends Comparable<C> > Comparator<Pair<?,C>> comparatorSecond() {
    return new Comparator<Pair<?,C>>() {
      public int compare(Pair<?,C> o1, Pair<?,C> o2) {
        return o1.getSecond().compareTo(o2.getSecond());
      }
    };
  }

  /**
   * Comparator, der nach First sortiert.<br>
   * Aufruf kann leider den Typ &lt;F&gt; nicht ermitteln, daher ist die Angabe nötig.<br>
   * Beispiel: <code>Collections.sort( list, Pair.&lt;Integer&gt;comparatorFirst() )</code>
   * @return
   */
  public static <C extends Comparable<C> > Comparator<Pair<C,?>> comparatorFirst() {
    return new Comparator<Pair<C,?>>() {
      public int compare(Pair<C,?> o1, Pair<C,?> o2) {
        return o1.getFirst().compareTo(o2.getFirst());
      }
    };
  }
  
  /**
   * Comparator, der nach First und Second sortiert. Dabei ist eines der beiden das 
   * Hauptsortierkriterium, bei Gleichheit wird dann das zweite verglichen.<br>
   * Aufruf kann leider den Typ nicht ermitteln, daher ist die Angabe nötig.<br>
   * Beispiel: <code>Collections.sort( list, Pair.&lt;String,Integer&gt;comparator(true) )</code>
   * @param orderFirstSecond <code>true</code>: First ist Haupt-Sortierung
   * @return
   */
  public static <F extends Comparable<F>, S extends Comparable<S> > Comparator<Pair<F,S>> comparator( final boolean orderFirstSecond ) {
    return new Comparator<Pair<F,S>>() {
      public int compare(Pair<F,S> o1, Pair<F,S> o2) {
        if( orderFirstSecond ) {
          int c = o1.getFirst().compareTo(o2.getFirst());
          if( c == 0 ) {
            return o1.getSecond().compareTo(o2.getSecond());
          } else {
            return c;
          }
        } else {
          int c = o1.getSecond().compareTo(o2.getSecond());
          if( c == 0 ) {
            return o1.getFirst().compareTo(o2.getFirst());
          } else {
            return c;
          }
        }
      }
    };
  }
  
  @Override
  public String toString() {
    return "Pair("+first+","+second+")";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Pair<?, ?> other = (Pair<?, ?>) obj;
    if (first == null) {
      if (other.first != null)
        return false;
    } else if (!first.equals(other.first))
      return false;
    if (second == null) {
      if (other.second != null)
        return false;
    } else if (!second.equals(other.second))
      return false;
    return true;
  }

  
}
