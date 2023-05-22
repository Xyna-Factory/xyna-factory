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

package com.gip.xyna.xnwh.persistence.dbmodifytable;

import java.util.Collection;
import java.util.Set;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;



public interface DatabasePersistenceLayerConnectionWithAlterTableSupport {

  public boolean doesTableExist(Persistable persistable);


  public <T extends Storable> void createTable(Persistable persistable, Class<T> klass, Column[] cols);


   /**
   * wird von addTable aufgerufen, falls tabelle existiert
   */
  public <T extends Storable> Set<DatabaseIndexCollision> checkColumns(Persistable persistable, Class<T> klass, Column[] columns) throws PersistenceLayerException;
  
  
  public <T extends Storable> void alterColumns(Set<DatabaseIndexCollision> columns) throws PersistenceLayerException;


  public <T extends Storable> String getDefaultColumnTypeString(Column col, Class<T> klass);


  public <T extends Storable> boolean areColumnsCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo);


  public <T extends Storable> boolean areBaseTypesCompatible(Column col, Class<T> klass, DatabaseColumnInfo colInfo);


  public <T extends Storable> boolean isTypeDependentOnSizeSpecification(Column col, Class<T> klass);


  public <T extends Storable> void modifyColumnsCompatible(Column col, Class<T> klass, String tableName);


  public <T extends Storable> void widenColumnsCompatible(Column col, Class<T> klass, String tableName);


  public <T extends Storable> String getCompatibleColumnTypesAsString(Column col, Class<T> klass);


  public <T extends Storable> String getTypeAsString(Column col, Class<T> klass);
  
  public long getPersistenceLayerInstanceId();

}
