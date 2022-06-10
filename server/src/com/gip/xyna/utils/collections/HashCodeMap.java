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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;




/**
 * Map, die beliebigen Objects auf Grund deren HashCodes einen Wert zuweist.
 * <br><br>
 * <ul>
 * <li>Diese Map muss mit einer vorgebenen <code>capacity</code> angelegt werden, aus dieser
 * capacity wird u.a. der interne Speicher bestimmt. </li>
 * <li>Dabei bekommt ein Object (dessen HashCode sich nicht ändert!) immer den gleichen Wert 
 * zugewiesen, unterschiedliche Objecte (deren HashCode sich unterscheidet!) bekommen mit
 * hoher Wahrscheinlichkeit (<code>1-1/capacity</code>) einen anderen Wert zugewiesen.</li>
 * <li><code>null</code> ist ebenfalls erlaubt und wird so angesehen, als ob der HashCode 0 ist.</li>
 * <li>Über das Interface {@link com.gip.xyna.utils.collections.HashCodeMap.Constructor Constructor&lt;T&gt;}
 * kann im Konstruktor eine Basisbelegung der Map erzeugt werden.</li>
 * </ul>
 * 
 */
public class HashCodeMap<T> implements Map<Object,T> {
  
  private List<T> content;
  private int capacity;
  private int size; //anzahl von non-null elementen

  /**
   * Eine Implementation dieses Interfaces kann im Konstruktor der HashCodeMap verwendet werden, 
   * um eine Basisbelegung der Map zu erzeugen. Für jeden Eintrag wird die Methode {@link #newInstance() newInstance} gerufen.
   */
  public interface Constructor<T> {
    /**
     * Erzeugen eines neuen Map-Eintrags
     * @return
     */
    T newInstance();
  }
  
  /**
   * Capacity 32, kein Konstruktor für Values
   */
  public HashCodeMap() {
    this(32,null);
  }
  
  /**
   * Kein Konstruktor für Values
   * @param capacity
   */
  public HashCodeMap(int capacity) {
    this(capacity, null);
  }
  
  /**
   * Capacity 32
   * @param constructor
   */
  public HashCodeMap(Constructor<T> constructor) {
    this(32, constructor);
  }
  
  /**
   * Capacity und Constructor werden angegeben, über den Constructor wird eine Basisbelegung der Map erzeugt.
   * @param capacity
   * @param constructor
   */
  public HashCodeMap(int capacity, Constructor<T> constructor) {
    if( capacity <= 0 ) {
      throw new IllegalArgumentException( "invalid capacity <= 0" );
    }
    this.capacity = capacity;
    content = new ArrayList<T>(capacity);
    for (int i = 0; i < capacity; ++i) {
      if (constructor != null) {
        content.add(constructor.newInstance());        
      } else {
        content.add(null);
      }
    }
    size = constructor != null ? capacity : 0;
  }
  

  public T get(Object object) {
    return content.get(index(object));
  }
  
  /**
   * Convenience-Methode zum Zugriff auf bestimmen Value
   * @param index
   * @return
   */
  public T get(int index) {
    return content.get(index);
  }
  
  public T put(int index, T value) {
    T old = content.set(index,value);
    if (old == null && value != null) {
      size++;
    } else if (old != null && value == null) {
      size--;
    }
    return old;
  }
  
  public int size() {
    return size;
  }
  
  public int capacity() {
    return capacity;
  }

  public boolean isEmpty() {
    return false;
  }

  /** 
   * return true (contains every key!)
   * @see java.util.Map#containsKey(java.lang.Object)
   */
  public boolean containsKey(Object key) {
    return true;
  }

  public boolean containsValue(Object value) {
    return content.contains(value);
  }

  public T put(Object key, T value) {
    return put(index(key), value);
  }

  public T remove(Object key) {
    T old =  content.set(index(key),null);
    if (old != null) {
      size --;
    }
    return old;
  }

  public void putAll(Map<? extends Object, ? extends T> t) {
    for( Entry<? extends Object, ? extends T> e : t.entrySet() ) {
      put( e.getKey(), e.getValue() );
    }
  }

  public void clear() {
    for( int c=0; c< capacity; ++c ) {
      content.set(c,null);
    }
    size = 0;
  }

  /**
   * throw new UnsupportedOperationException("keySet is not supported");
   * @see java.util.Map#keySet()
   */
  public Set<Object> keySet() {
    throw new UnsupportedOperationException("keySet is not supported");
  }

  public Collection<T> values() {
    return content;
  }

  /**
   * throw new UnsupportedOperationException("entrySet is not supported");
   * @see java.util.Map#entrySet()
   */
  public Set<java.util.Map.Entry<Object, T>> entrySet() {
    throw new UnsupportedOperationException("entrySet is not supported");
  }
  
  private int index( Object o ) {
    if( o == null ) {
      return 0;
    } else {
      int i = o.hashCode()%capacity;
      return (i < 0) ? -i : i;
    }
  }

  
}
