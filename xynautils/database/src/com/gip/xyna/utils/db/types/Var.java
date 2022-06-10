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
package com.gip.xyna.utils.db.types;

/**
 * Var<T> macht den Typen variabel: Über get und set kann der Inhalt
 * ausgelesen und verändert werden. 
 * 
 * Beispiel: ein Integer kann nur einen festen Wert haben, der 
 * zugrundeliegende int kann nicht verändert werden. Mit dem 
 * Var<Integer> ist dies nun möglich. Damit kann der Var<Integer> an 
 * verschiedenen Stellen gespeichert werden, und dann gleichzeitg an 
 * allen Stellen den Wert ändern.
 * 
 * Achtung: nicht als Key in einer HashMap speichern, da hashCode variabel ist!
 * 
 * @param <T>
 */
public class Var<T> {
  private T t;

  /**
   * Default-Konstruktor
   */
  public Var() {
  }
  
  /**
   * Konstruktor zur Initialisierung
   */
  public Var( T t ) {
    this.t = t;
  }
  
 
  /**
   * Setter
   * @param t
   */
  public void set(T t) {
    this.t = t;
  }

  /**
   * Getter
   * @return
   */
  public T get() {
    return t;
  }
  
  @Override
  public boolean equals(Object obj) {
    if( obj == null ) {
      return false;
    }
    if( t == null ) {
      return false;
    }
    if( obj instanceof Var<?> ) {
      return t.equals( ((Var<?>)obj).get() );
    } else {
      if( t.getClass().equals( obj.getClass() ) ) {
        return t.equals( obj );
      } else {
        return false;
      }
    }
  }
 
  @Override
  public int hashCode() {
    if( t == null ) {
      return super.hashCode();
    } else {
      return t.hashCode();
    }
  }
  
}
