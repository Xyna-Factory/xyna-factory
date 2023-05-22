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
package com.gip.xyna.utils.concurrent;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * AtomicEnum f�hrt atomare Operationen auf Enums aus.
 *
 */
public class AtomicEnum<E extends Enum<E>> implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private AtomicInteger ai = null;
  private E[] enumConstants;
  
  public AtomicEnum(Class<E> enumType) {
    this.enumConstants = enumType.getEnumConstants();
    this.ai =  new AtomicInteger(0);
  }
  
  public AtomicEnum(Class<E> enumType, E e) {
    this.enumConstants = enumType.getEnumConstants();
    this.ai =  new AtomicInteger(e.ordinal());
  }

  public static <E2 extends Enum<E2>> AtomicEnum<E2> of( E2 e ) {
    @SuppressWarnings("unchecked")
    Class<E2> cl = (Class<E2>)e.getClass();
    return new AtomicEnum<E2>(cl, e);
  }
  
  
  public E get() {
    return enumConstants[ai.get()];
  }

  public void set(E newValue) {
    ai.set(newValue.ordinal());
  }
  
  public E getAndSet(E newValue) {
    return enumConstants[ai.getAndSet(newValue.ordinal())];
  }
  
  public boolean compareAndSet(E expect, E update) {
    return ai.compareAndSet(expect.ordinal(), update.ordinal());
  }
  
  public boolean weakCompareAndSet(E expect, E update) {
    return ai.weakCompareAndSet(expect.ordinal(), update.ordinal());
  }
  
  public String toString() {
    return get().toString();
}

  public boolean is(E expect) {
    return ai.get() == expect.ordinal();
  }
  
  public boolean isNot(E notExpect) {
    return ai.get() != notExpect.ordinal();
  }
  

  public boolean isIn(E ... list) {
    int curr = ai.get();
    for( E e : list ) {
      if( e.ordinal() == curr ) {
        return true;
      }
    }
    return false;
  }

}
