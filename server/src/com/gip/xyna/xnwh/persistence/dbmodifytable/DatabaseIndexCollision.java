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

package com.gip.xyna.xnwh.persistence.dbmodifytable;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.Storable;


public class DatabaseIndexCollision {
  
  public static enum IndexModification {
    CREATE, DELETE, MODIFY;
  }
  
  private final Persistable persi;
  private final Column column;
  private final Class<? extends Storable> klass;
  // String reason?
  private final IndexModification modification;
  
  public DatabaseIndexCollision(Persistable persi, Column column, Class<? extends Storable> klass, IndexModification modification) {
    this.persi = persi;
    this.column = column;
    this.klass = klass;
    this.modification = modification;
  }
  
  
  public Class<? extends Storable> getKlass() {
    return klass;
  }

  
  public Persistable getPersi() {
    return persi;
  }


  public Column getColumn() {
    return column;
  }
  
  
  public IndexModification getIndexModification() {
    return modification;
  }
  

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof DatabaseIndexCollision)) {
      return false;
    }
    DatabaseIndexCollision dic = (DatabaseIndexCollision)obj;
    return column.name().equals(dic.column.name()) &&
           persi.tableName().equals(dic.persi.tableName());
  }

  
  @Override
  public int hashCode() {
    return column.name().hashCode() + persi.tableName().hashCode();
  }
  
  

}
