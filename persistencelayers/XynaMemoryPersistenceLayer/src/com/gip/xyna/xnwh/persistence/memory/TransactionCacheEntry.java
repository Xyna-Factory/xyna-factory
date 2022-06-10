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

package com.gip.xyna.xnwh.persistence.memory;



import com.gip.xyna.xnwh.persistence.Storable;



public class TransactionCacheEntry<T extends Storable> {

  private final Integer order;
  private MemoryRowData<T> rowdata;
  private T newContent;


  public TransactionCacheEntry(Integer order, MemoryRowData<T> rowdata, T newContent) {
    this.order = order;
    this.rowdata = rowdata;
    this.newContent = newContent;
  }


  public Integer getOrder() {
    return this.order;
  }


  public MemoryRowData<T> getRowData() {
    return this.rowdata;
  }


  public T getNewContent() {
    return this.newContent;
  }

}
