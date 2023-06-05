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

import java.lang.ref.WeakReference;


/**
 * StrongWeakReference kann zwischen einer starken Referenz und WeakReference umgeschaltet werden.
 * 
 * {@link #convertToWeak()} funktioniert dabei immer, {@link #convertToStrong()} nur, 
 * wenn der referenzierte Wert noch vorhanden ist. 
 * Mit {@link #replace(Object)} kann der referenzierte Wert durch einen anderen ersetzt werden. 
 * 
 */
public class StrongWeakReference<T> {

  private T strongRef;
  private WeakReference<T> weakRef;
  private boolean isStrong;

  public StrongWeakReference(T ref) {
    this.strongRef = ref;
    this.weakRef = new WeakReference<T>(ref);
    this.isStrong = true;
  }
  
  
  public T get() {
    if( isStrong ) {
      return strongRef;
    } else {
      return weakRef.get();
    }
  }

  public void clear() {
    this.strongRef = null;
    this.weakRef.clear();
  }

  public void convertToWeak() {
    this.strongRef = null;
    this.isStrong = false;
  }
  
  /**
   * @return true, wenn refence wieder strong ist
   */
  public boolean convertToStrong() {
    T ref = weakRef.get();
    if( ref != null ) {
      this.strongRef = ref;
      this.isStrong = true;
    } else {
      //klappt nicht mehr
    }
    return this.isStrong;
  }
  
  /**
   * Ersetzt den referenzierten Wert durch einen neuen
   * @param ref
   * @return alter Wert oder null
   */
  public T replace(T ref) {
    T old = get();
    if( isStrong ) {
      this.strongRef = ref;
      this.weakRef = new WeakReference<T>(ref);
    } else {
      this.weakRef = new WeakReference<T>(ref);
    }
    return old;
  }
  
  
  public boolean isStrong() {
    return isStrong;
  }
  
  public WeakReference<T> getWeakReference() {
    return weakRef;
  }
  
}
