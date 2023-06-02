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
package com.gip.xyna.xnwh.persistence.memory;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidObjectForTableException;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.Storable;


public abstract class MemoryRowData<T extends Storable> implements RowDataInterface {

  private TableObject<T, ? extends MemoryRowData<T>> parent;

  private boolean commited;
  
  public MemoryRowData(TableObject<T, ? extends MemoryRowData<T>> parent, boolean commited) {
    this.parent = parent;
    this.commited = commited;
  }


  /**
   * füllt das übergebene objekt
   */
  public void fillFromData(PersistenceLayer pl, Storable<? super T> storable) throws UnderlyingDataNotFoundException {
    storable.setAllFieldsFromData(getData(pl));
  }


  /**
   * @return the previous value
   */
  public T setData(PersistenceLayer pl, T storable) throws XNWH_InvalidObjectForTableException  {

    Class<? extends Storable> tableClazz = getTableClass();
    Class<? extends Storable> storableClass = storable.getClass();

    if (!tableClazz.isAssignableFrom(storableClass)) {

      String tableClassName = tableClazz.getCanonicalName();
      String storableClassName = storableClass.getCanonicalName();

      if (tableClassName.equals(storableClassName)) {
        // in this case the problem is probably classloading related and is probably a bug
        throw new RuntimeException("Storable " + storableClassName + " for table <" + parent.getName()
            + "> was loaded inconsistently: " + tableClazz.getClassLoader() + ", " + storableClass.getClassLoader()
            + ".");
      } else {
        // probably configuration problems?
        // TODO fehlermeldung besser, eigener exception type. 
        throw new XNWH_InvalidObjectForTableException(parent.getName(), storableClassName, tableClassName);
      }
    }
    T ret = setDataInternally(pl, storable);
    deleted = false;
    return ret;
  }


  public abstract MemoryRowLock getLock(PersistenceLayer pl) throws UnderlyingDataNotFoundException ;


  public Object getPKValue(PersistenceLayer pl) throws UnderlyingDataNotFoundException {
    return getData(pl).getPrimaryKey();
  }


  public abstract T getData(PersistenceLayer pl) throws UnderlyingDataNotFoundException;


  protected abstract T setDataInternally(PersistenceLayer pl, T storable);


  public Class<T> getTableClass() {
    return parent.getBackingClass();
  }


  public abstract long getUniqueID();


  public final boolean isCommitted() {
    return commited;
  }


  public final void setIsCommitted(boolean commited) {
    this.commited = commited;
  }
  
  private boolean deleted = false;

  public void deleted() {
    deleted = true;
  }

  public boolean isDeleted() {
    return deleted;
  }

}
