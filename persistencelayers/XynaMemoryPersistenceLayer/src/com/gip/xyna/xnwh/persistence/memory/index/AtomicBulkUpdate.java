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
package com.gip.xyna.xnwh.persistence.memory.index;

public interface AtomicBulkUpdate<E extends Comparable<E>, F> {

  void add(E e, F f);


  void remove(E e, F f);


  void update(E oldKey, E newKey, F f);

  /**
   * achtung, bulk commit funktioniert nur f�r daten, die bereits "gemerged" sind.
   * d.h. die reihenfolge der bulk-teile muss vertauschbar sein (bis auf remove/add, die werden automatisch in die richtige reihenfolge gebracht) 
   */  
  void commit();


  int size();

}
