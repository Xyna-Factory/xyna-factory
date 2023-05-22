/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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



/**
 * Einfacher Speicherung von drei zusammengeh�rigen Objekten.
 * <br><br>
 * Beispiel:
 * <pre>
 * HashMap&lt;String,Triple&lt;String,Long,Boolean&gt;&gt; map = new HashMap&lt;String,Triple&lt;String,Long,Boolean&gt;&gt;();
 * map.put("a", Triple.of("A", 1L, true) );
 * map.put("b", new Triple&lt;String,Long,Boolean&gt;("B", 2L, false) );
 * for( Map.Entry&lt;String,Triple&lt;String,Long,Boolean&gt;&gt; e : map.entrySet() ) {
 *   String lower = e.getKey();
 *   String upper = e.getValue().getFirst();
 *   long ordinal = e.getValue().getSecond();
 *   boolean vocal = e.getValue().getThird();
 * }
 * </pre>
 */
public class Triple<F,S,T> implements Serializable {
  
  private static final long serialVersionUID = -144794521022105783L;
  
  private F first;
  private S second;
  private T third;

  public Triple(F first, S second, T third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }
  
  public static <F,S,T> Triple<F,S,T> of(F first, S second, T third) {
    return new Triple<F,S,T>(first,second,third);
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

  public T getThird() {
    return third;
  }
  
  public void setThird(T third) {
    this.third = third;
  }
  
  @Override
  public String toString() {
    return "Triple("+first+","+second+","+third+")";
  }

  @Override
  public int hashCode() {
    int h = 1;
    if (first != null) {
      h += first.hashCode() + 31*h;
    }
    h += 17; 
    if (second != null) {
      h += second.hashCode() + 31*h;
    }
    h += 17; 
    if (third != null) {
      h += third.hashCode() + 31*h;
    }
    return h;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Triple)) {
      return false;
    }
    Triple<?, ?, ?> o = (Triple<?, ?, ?>) obj;
    if (!eq(first, o.first)) {
      return false;
    }
    if (!eq(second, o.second)) {
      return false;
    }
    if (!eq(third, o.third)){
      return false;
    }
    return true;
  }

  private static boolean eq(Object o, Object o2) {
    if (o == null) {
      if (o2 == null) {
        return true;
      }
    } else {
      return o.equals(o2);
    }
    return false;
  }
  
}
